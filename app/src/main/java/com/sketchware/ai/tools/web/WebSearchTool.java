package com.sketchware.ai.tools.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * web_search — search the web and return a list of results (title, URL, snippet).
 *
 * <p>Mirrors Cline's {@code WebSearchTool} ({@code web_search} in v4). Used
 * when the LLM needs up-to-date information that's not in its training data
 * (recent library releases, current API docs, error messages, etc.).
 *
 * <h2>Backend</h2>
 * <p>Uses DuckDuckGo's HTML endpoint ({@code https://html.duckduckgo.com/html/?q=...})
 * which requires no API key and is free. The results are parsed from the
 * returned HTML. The parser is intentionally simple — DuckDuckGo's HTML
 * structure is stable enough that a regex-based extractor works for ~95% of
 * queries. When parsing fails (e.g. DDG changes their HTML), the tool falls
 * back to returning the raw HTML so the LLM can still extract useful info.
 *
 * <p>If the user has configured a Tavily or Brave Search API key in the
 * provider settings (TODO — not yet wired), this tool should prefer that
 * backend for higher-quality results.
 */
public final class WebSearchTool implements SketchwareTool {

    /** Maximum results returned to the LLM. */
    static final int MAX_RESULTS = 8;

    /** Snippet length cap. */
    static final int MAX_SNIPPET_CHARS = 400;

    private static final int TIMEOUT_SECONDS = 20;
    private static final String DDG_URL = "https://html.duckduckgo.com/html/";

    @Override public String name() { return "web_search"; }
    @Override public String category() { return "web"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Search the web and return up to " + MAX_RESULTS + " results (title, URL, snippet). "
                + "Use for up-to-date information not in training data. Backend: DuckDuckGo (no API key required).";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        query.addProperty("description", "Search query (will be URL-encoded)");
        props.add("query", query);
        JsonObject max = new JsonObject();
        max.addProperty("type", "integer");
        max.addProperty("description", "Max results to return (default " + MAX_RESULTS + ", max 10)");
        max.addProperty("default", MAX_RESULTS);
        props.add("max_results", max);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("query");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (!args.has("query") || args.get("query").isJsonNull()) {
            return ToolResult.error("Missing required parameter: query");
        }
        String query = args.get("query").getAsString().trim();
        if (query.isEmpty()) {
            return ToolResult.error("Search query is empty");
        }
        int maxResults = MAX_RESULTS;
        if (args.has("max_results") && args.get("max_results").isJsonPrimitive()) {
            try {
                maxResults = Math.max(1, Math.min(10, args.get("max_results").getAsInt()));
            } catch (Throwable ignored) {}
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        String url = DDG_URL + "?q=" + encodedQuery + "&kl=us-en";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .post(okhttp3.RequestBody.create(
                        "q=" + encodedQuery + "&b=&kl=us-en",
                        okhttp3.MediaType.get("application/x-www-form-urlencoded")))
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                return ToolResult.error("DuckDuckGo HTTP " + resp.code() + ": " + resp.message());
            }
            String html = resp.body() != null ? resp.body().string() : "";
            if (html.isEmpty()) {
                return ToolResult.error("Empty response from DuckDuckGo");
            }
            List<SearchResult> results = parseDdgHtml(html, maxResults);
            if (results.isEmpty()) {
                // Fall back: maybe DDG changed HTML structure; return a hint.
                return ToolResult.success("No results parsed (DDG HTML may have changed). "
                        + "Try a more specific query, or use web_fetch on a specific URL.\n\n"
                        + "Raw HTML preview:\n" + html.substring(0, Math.min(2000, html.length())));
            }
            return ToolResult.success(formatResults(query, results));
        } catch (IOException e) {
            return ToolResult.error("Network error searching: " + e.getMessage());
        }
    }

    /**
     * Parse DuckDuckGo HTML results. DDG wraps each result in a
     * {@code <div class="result">} with the title in an {@code <a class="result__a">}
     * and the snippet in {@code <a class="result__snippet">}.
     */
    static List<SearchResult> parseDdgHtml(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (html == null || html.isEmpty()) return results;
        // Match result blocks. DDG's HTML uses class="result__a" for the title link
        // and class="result__snippet" for the snippet.
        // Title pattern: <a class="result__a" href="...">Title text</a>
        Pattern titlePat = Pattern.compile(
                "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        // Snippet pattern: <a class="result__snippet" ...>snippet text</a>
        Pattern snippetPat = Pattern.compile(
                "<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher titleMatcher = titlePat.matcher(html);
        Matcher snippetMatcher = snippetPat.matcher(html);
        // Collect all titles and snippets; since they appear in order, we zip them.
        List<String> titles = new ArrayList<>();
        List<String> titleUrls = new ArrayList<>();
        while (titleMatcher.find() && titles.size() < maxResults) {
            String href = titleMatcher.group(1);
            String title = stripTags(titleMatcher.group(2));
            if (title.isEmpty() || href == null) continue;
            // DDG wraps URLs in a redirect: //duckduckgo.com/l/?uddg=<encoded>
            String realUrl = decodeDdgRedirect(href);
            titles.add(title);
            titleUrls.add(realUrl);
        }
        List<String> snippets = new ArrayList<>();
        while (snippetMatcher.find() && snippets.size() < maxResults) {
            snippets.add(stripTags(snippetMatcher.group(1)));
        }
        // Zip
        int n = Math.min(titles.size(), snippets.size());
        for (int i = 0; i < n; i++) {
            String snippet = snippets.get(i);
            if (snippet.length() > MAX_SNIPPET_CHARS) {
                snippet = snippet.substring(0, MAX_SNIPPET_CHARS) + "...";
            }
            results.add(new SearchResult(titles.get(i), titleUrls.get(i), snippet));
        }
        // If we have titles but no snippets (DDG sometimes returns empty snippets),
        // still include them with empty snippet.
        for (int i = n; i < titles.size(); i++) {
            results.add(new SearchResult(titles.get(i), titleUrls.get(i), ""));
        }
        return results;
    }

    /** Decode DuckDuckGo's l/?uddg= redirect wrapper to the actual URL. */
    private static String decodeDdgRedirect(String href) {
        if (href == null) return "";
        // DDG format: //duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpath&rut=...
        int uddgIdx = href.indexOf("uddg=");
        if (uddgIdx >= 0) {
            int start = uddgIdx + 5;
            int end = href.indexOf('&', start);
            if (end < 0) end = href.length();
            String encoded = href.substring(start, end);
            try {
                return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                return href;
            }
        }
        // Already a direct URL.
        if (href.startsWith("//")) return "https:" + href;
        if (href.startsWith("/")) return "https://duckduckgo.com" + href;
        return href;
    }

    private static String stripTags(String s) {
        if (s == null) return "";
        return s.replaceAll("(?s)<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .trim();
    }

    private static String formatResults(String query, List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search: \"").append(query).append("\"\n");
        sb.append("Results: ").append(results.size()).append("\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.title).append("\n");
            sb.append("   URL: ").append(r.url).append("\n");
            if (!r.snippet.isEmpty()) {
                sb.append("   ").append(r.snippet).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Tip: use web_fetch on a specific URL to read the full page.");
        return sb.toString();
    }

    /** One search result. */
    static final class SearchResult {
        final String title;
        final String url;
        final String snippet;

        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
