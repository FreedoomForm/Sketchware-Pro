package com.sketchware.ai.release;

import java.util.List;

/**
 * Immutable representation of a GitHub Release as returned by
 * {@code GET /repos/{owner}/{repo}/releases}.
 *
 * <p>Only the fields needed by the in-app Versions screen and the
 * UpdateChecker are modeled. The download URL points at the first
 * {@code .apk} asset in the release — Sketchware Pro releases are
 * single-APK, so this is sufficient.
 *
 * <p>Field naming mirrors the JSON keys produced by
 * <a href="https://docs.github.com/en/rest/releases/releases">GitHub's
 * REST API</a> so the parser is a straightforward Gson {@code fromJson}
 * call.
 */
public final class GitHubRelease {

    /** HTML URL of the release page on github.com (used by the "Open in browser" button). */
    public final String html_url;

    /** Release name (e.g. "Sketchware Pro v7.0.1+740f984"). May be null for drafts. */
    public final String name;

    /** Tag name (e.g. "v7.0.1+740f984"). Always present for published releases. */
    public final String tag_name;

    /** Whether the release is a prerelease (excluded from update checks by default). */
    public final boolean prerelease;

    /** ISO-8601 timestamp string (e.g. "2026-08-12T09:30:00Z"). */
    public final String published_at;

    /** Markdown body of the release notes. May be null/empty. */
    public final String body;

    /** APK assets attached to the release. */
    public final List<Asset> assets;

    public GitHubRelease(String html_url, String name, String tag_name,
                         boolean prerelease, String published_at,
                         String body, List<Asset> assets) {
        this.html_url = html_url;
        this.name = name;
        this.tag_name = tag_name;
        this.prerelease = prerelease;
        this.published_at = published_at;
        this.body = body;
        this.assets = assets;
    }

    /** Asset attached to a release. We only care about APK assets. */
    public static final class Asset {
        public final String name;
        public final String browser_download_url;
        public final long size;
        public final String content_type;

        public Asset(String name, String browser_download_url, long size, String content_type) {
            this.name = name;
            this.browser_download_url = browser_download_url;
            this.size = size;
            this.content_type = content_type;
        }
    }

    /** Return the first APK asset, or {@code null} if the release has none. */
    public GitHubRelease.Asset firstApkAsset() {
        if (assets == null) return null;
        for (GitHubRelease.Asset a : assets) {
            if (a.name != null && a.name.toLowerCase().endsWith(".apk")) return a;
        }
        return null;
    }

    /**
     * Extract the numeric versionCode encoded in this release's tag.
     *
     * <p>Tag format produced by the release workflow:
     * {@code v<versionName>+<shortSha>}, where {@code <versionName>} is
     * {@code v<major>.<minor>.<patch>}. There is no versionCode in the
     * tag itself — versionCode is recovered from the release notes body
     * (the workflow emits a {@code **versionCode:** N} line).
     *
     * @return the versionCode, or {@code -1} if it cannot be parsed
     *         (e.g. for releases published before this convention)
     */
    public int extractVersionCode() {
        if (body == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\*\\*versionCode:\\*\\*\\s*(\\d+)")
                .matcher(body);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /** Human-readable label for list display (prefers name, falls back to tag). */
    public String displayName() {
        if (name != null && !name.isEmpty()) return name;
        if (tag_name != null) return tag_name;
        return "Unknown release";
    }
}
