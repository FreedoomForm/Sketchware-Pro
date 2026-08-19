package com.sketchware.ai.tools.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * web_fetch — fetch content from a URL and return it as text for the LLM.
 *
 * <p>Mirrors Cline's {@code WebFetchTool} ({@code fetch_web} in v4). Used
 * when the LLM needs to read documentation, look up an API reference, or
 * inspect a webpage's content.
 *
 * <p>The fetched content is truncated to {@link #MAX_CONTENT_CHARS} to
 * avoid blowing the context window. HTML is stripped to plain text via a
 * simple tag-removal pass (production-quality HTML → text conversion is
 * out of scope; the goal is to give the LLM readable content, not a
 * pixel-perfect rendering).
 *
 * <p>Only {@code http://} and {@code https://} URLs are supported.
 * Redirects are followed (up to OkHttp's default of 20). File:// and other
 * schemes are rejected to prevent SSRF-style access to local resources.
 */
public final class WebFetchTool implements SketchwareTool {

    /** Maximum content length returned to the LLM (in chars). */
    static final int MAX_CONTENT_CHARS = 16_000;

    /** Maximum content length fetched from the server (in bytes). */
    private static final long MAX_CONTENT_BYTES = 1_500_000L;

    /** Connect / read / write timeouts in seconds. */
    private static final int TIMEOUT_SECONDS = 30;

    @Override public String name() { return "web_fetch"; }
    @Override public String category() { return "web"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Fetch content from a URL (HTTP/HTTPS) and return it as text. "
                + "Use to read documentation, look up API references, or inspect webpages. "
                + "HTML is stripped to plain text and truncated to " + (MAX_CONTENT_CHARS / 1000) + "K chars.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject url = new JsonObject();
        url.addProperty("type", "string");
        url.addProperty("description", "Absolute http:// or https:// URL to fetch");
        url.addProperty("format", "uri");
        props.add("url", url);
        JsonObject raw = new JsonObject();
        raw.addProperty("type", "boolean");
        raw.addProperty("description", "If true, return the raw HTML/XML without stripping tags (default false)");
        props.add("raw", raw);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("url");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (!args.has("url") || args.get("url").isJsonNull()) {
            return ToolResult.error("Missing required parameter: url");
        }
        String urlStr = args.get("url").getAsString().trim();
        if (urlStr.isEmpty()) {
            return ToolResult.error("URL is empty");
        }
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return ToolResult.error("Only http:// and https:// URLs are supported. Got: " + urlStr);
        }
        boolean raw = args.has("raw") && args.get("raw").isJsonPrimitive()
                && args.get("raw").getAsBoolean();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        Request req = new Request.Builder()
                .url(urlStr)
                .header("User-Agent", "Sketchware-Pro-AI/1.0 (Android; +https://github.com/FreedoomForm/Sketchware-Pro)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml,text/plain,application/json;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                return ToolResult.error("HTTP " + resp.code() + " " + resp.message() + " fetching " + urlStr);
            }
            String contentType = resp.header("Content-Type", "");
            long contentLength = resp.body() != null ? resp.body().contentLength() : -1;
            if (contentLength > MAX_CONTENT_BYTES) {
                return ToolResult.error("Content too large (" + contentLength + " bytes). Max: " + MAX_CONTENT_BYTES);
            }
            // Bound the read regardless of Content-Length. For chunked
            // responses (HTTP/1.1 Transfer-Encoding: chunked) contentLength
            // is -1, so the check above is bypassed. Without an explicit
            // cap on the stream itself, resp.body().string() would buffer
            // the entire body into a Java String — potentially gigabytes
            // for a malicious or misconfigured server, OOM-crashing the
            // agent process.
            String body;
            if (resp.body() == null) {
                body = "";
            } else {
                // Note: 'rawStream' (not 'raw') — the method already has a
                // boolean 'raw' parameter at line 86 for the "raw HTML" flag.
                java.io.InputStream rawStream = resp.body().byteStream();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(64 * 1024);
                byte[] buf = new byte[16 * 1024];
                long total = 0;
                int n;
                boolean tooBig = false;
                while ((n = rawStream.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_CONTENT_BYTES) {
                        tooBig = true;
                        break;
                    }
                    baos.write(buf, 0, n);
                }
                if (tooBig) {
                    return ToolResult.error("Content too large (> " + MAX_CONTENT_BYTES
                            + " bytes streamed). Max: " + MAX_CONTENT_BYTES);
                }
                body = baos.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            }
            if (body.isEmpty()) {
                return ToolResult.success("(empty body)");
            }
            String result;
            if (raw || isTextContentType(contentType)) {
                String processed = raw ? body : stripHtml(body);
                result = truncate(processed);
            } else {
                result = "(Non-text content type: " + contentType + ", " + body.length() + " bytes)";
            }
            return ToolResult.success(formatResult(urlStr, resp.code(), contentType, result));
        } catch (IOException e) {
            return ToolResult.error("Network error fetching " + urlStr + ": " + e.getMessage());
        }
    }

    private static boolean isTextContentType(String contentType) {
        if (contentType == null) return true; // assume text if unknown
        String ct = contentType.toLowerCase();
        return ct.contains("text") || ct.contains("json") || ct.contains("xml") || ct.contains("javascript");
    }

    /**
     * Strip HTML tags to plain text. Preserves line breaks from block-level
     * tags and decodes common entities. This is a minimal stripper — for
     * complex pages, the result will be readable but not perfectly formatted.
     */
    static String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        // Remove script and style blocks entirely (including content).
        String s = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        s = s.replaceAll("(?is)<!--.*?-->", " ");
        // Convert block-level closing tags to newlines.
        s = s.replaceAll("(?i)</(p|div|br|h[1-6]|li|tr|table|hr|blockquote|pre)>", "\n");
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        // Remove all remaining tags.
        s = s.replaceAll("(?s)<[^>]+>", "");
        // Decode common entities.
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
             .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
             .replace("&apos;", "'");
        // Collapse whitespace.
        s = s.replaceAll("[ \\t]+", " ");
        s = s.replaceAll("(?:\\r?\\n[ \\t]*){3,}", "\n\n");
        return s.trim();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= MAX_CONTENT_CHARS ? s : s.substring(0, MAX_CONTENT_CHARS)
                + "\n\n... (truncated, " + s.length() + " chars total)";
    }

    private static String formatResult(String url, int code, String contentType, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("URL: ").append(url).append("\n");
        sb.append("HTTP: ").append(code).append("\n");
        sb.append("Content-Type: ").append(contentType).append("\n");
        sb.append("Body length: ").append(body.length()).append(" chars\n");
        sb.append("---\n");
        sb.append(body);
        return sb.toString();
    }
}
