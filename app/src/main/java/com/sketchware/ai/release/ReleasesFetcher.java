package com.sketchware.ai.release;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.llm.http.HttpClient;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches the list of GitHub Releases for the Sketchware-Pro fork.
 *
 * <p>Uses the public GitHub REST endpoint
 * {@code GET /repos/{owner}/{repo}/releases?per_page=30} — no auth token
 * required for public repos (rate-limited to 60 req/hour per IP, which
 * is plenty for an in-app version check).
 *
 * <p>Runs on a background thread — the caller MUST NOT invoke
 * {@link #fetchReleases()} from the UI thread (OkHttp blocks).
 */
public final class ReleasesFetcher {

    /** Hard-coded fork coordinates. The user's releases live here. */
    public static final String OWNER = "FreedoomForm";
    public static final String REPO  = "Sketchware-Pro";

    private static final String ENDPOINT =
            "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases?per_page=30";

    private static final Gson GSON = new Gson();

    private ReleasesFetcher() {}

    /**
     * Fetch and parse the list of published releases, newest first.
     *
     * @return a non-null, possibly-empty list of {@link GitHubRelease}
     * @throws Exception on network or parse error
     */
    public static List<GitHubRelease> fetchReleases() throws Exception {
        Request req = new Request.Builder()
                .url(ENDPOINT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Sketchware-Pro-Android")
                .get()
                .build();

        try (Response resp = HttpClient.getClient().newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("GitHub HTTP " + resp.code() + ": "
                        + (resp.body() != null ? resp.body().string() : ""));
            }
            String body = resp.body() != null ? resp.body().string() : "[]";
            return parse(body);
        }
    }

    /** Parse the JSON array returned by the releases endpoint. */
    static List<GitHubRelease> parse(String json) {
        List<GitHubRelease> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        JsonArray arr;
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) return out;
            arr = el.getAsJsonArray();
        } catch (Exception ignored) {
            return out;
        }
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String htmlUrl = str(o, "html_url");
            String name    = str(o, "name");
            String tag     = str(o, "tag_name");
            boolean pre    = bool(o, "prerelease");
            String pubAt   = str(o, "published_at");
            String body    = str(o, "body");
            List<GitHubRelease.Asset> assets = new ArrayList<>();
            if (o.has("assets") && o.get("assets").isJsonArray()) {
                for (JsonElement ae : o.getAsJsonArray("assets")) {
                    if (!ae.isJsonObject()) continue;
                    JsonObject ao = ae.getAsJsonObject();
                    assets.add(new GitHubRelease.Asset(
                            str(ao, "name"),
                            str(ao, "browser_download_url"),
                            ao.has("size") ? ao.get("size").getAsLong() : 0L,
                            str(ao, "content_type")));
                }
            }
            out.add(new GitHubRelease(htmlUrl, name, tag, pre, pubAt, body, assets));
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return null;
        return o.get(key).getAsString();
    }

    private static boolean bool(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return false;
        return o.get(key).getAsBoolean();
    }
}
