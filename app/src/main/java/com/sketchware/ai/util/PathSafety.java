package com.sketchware.ai.util;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Filesystem path-traversal protection for AI tools.
 *
 * <p>All AI tools that accept a user/LLM-supplied relative path and resolve it
 * against a project root MUST run the path through {@link #resolveUnderRoot}
 * before constructing a {@link File}. Without this check, an LLM (or an
 * attacker via prompt injection in a shared {@code .sc} project) can supply
 * {@code ../../../../etc/passwd} or {@code /sdcard/Android/data/...} and read
 * or write files outside the project directory.
 *
 * <p>This is the canonical implementation extracted from
 * {@code ResourceFileManageTool}; the original private copy remains there for
 * backwards compatibility, but new code should call this class instead.
 *
 * <p>The {@link #SAFE_REL_PATH} regex is intentionally strict: only
 * alphanumeric, underscore, slash, hyphen, and dot are allowed. This matches
 * everything Sketchware project files legitimately contain (resource paths
 * like {@code values/strings.xml}, Java names like
 * {@code com/example/MainActivity}, etc.) while rejecting path separators,
 * null bytes, colons, and other shell-injection vectors.
 */
public final class PathSafety {

    private PathSafety() {}

    /** Allowed relative-path charset: letters, digits, _, /, -, .. */
    public static final Pattern SAFE_REL_PATH =
            Pattern.compile("^[A-Za-z0-9_][A-Za-z0-9_/\\-.]*$");

    /**
     * Validate that a relative path is safe to resolve under a project root.
     *
     * <p>Rules:
     * <ul>
     *   <li>Non-null, non-empty.</li>
     *   <li>Matches {@link #SAFE_REL_PATH} (no shell metachars, no leading dot).</li>
     *   <li>Contains no {@code ".."} segments (the regex already forbids a
     *       leading {@code ..} but a path like {@code a/../b} would still
     *       pass the regex; this check catches it).</li>
     * </ul>
     */
    public static boolean isSafeRelPath(String rel) {
        if (rel == null || rel.isEmpty()) return false;
        if (!SAFE_REL_PATH.matcher(rel).matches()) return false;
        String[] parts = rel.split("/");
        for (String p : parts) {
            if ("..".equals(p)) return false;
        }
        return true;
    }

    /**
     * Resolve {@code rel} against {@code root} and return the canonical
     * absolute path, <em>only if</em> the resolved target is the root itself
     * or a descendant of it.
     *
     * @return the canonical absolute path, or {@code null} if {@code rel} is
     *         unsafe or escapes the root.
     */
    public static String resolveUnderRoot(String root, String rel) {
        if (root == null || rel == null) return null;
        if (!isSafeRelPath(rel)) return null;
        try {
            File rootFile = new File(root).getCanonicalFile();
            File target = new File(rootFile, rel).getCanonicalFile();
            String targetPath = target.getAbsolutePath();
            String rootPath = rootFile.getAbsolutePath();
            if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
                return null;
            }
            return target.getAbsolutePath();
        } catch (Exception t) {
            return null;
        }
    }

    /**
     * Variant of {@link #resolveUnderRoot} that returns a {@link File} (or
     * {@code null}) for callers that prefer the File API.
     */
    public static File resolveFileUnderRoot(String root, String rel) {
        String abs = resolveUnderRoot(root, rel);
        return abs == null ? null : new File(abs);
    }
}
