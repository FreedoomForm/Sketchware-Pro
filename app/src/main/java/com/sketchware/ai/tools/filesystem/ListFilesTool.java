package com.sketchware.ai.tools.filesystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.PathSafety;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * list_files — list files and directories under the Sketchware project.
 *
 * <p>Mirrors Cline's {@code list_files} tool. Walks the project directory
 * tree (rooted at the project's data dir) and returns the names of files
 * and directories up to {@link #MAX_DEPTH} levels deep.
 *
 * <p>Used by the LLM to orient itself in the project structure: see what
 * source files exist, what resources are present, etc. The result is a
 * plain-text tree (not JSON) for token efficiency.
 *
 * <p>Paths are returned relative to the project root. Hidden files and
 * directories (starting with {@code .}) are excluded to avoid cluttering
 * the output with {@code .git/}, {@code .gradle/}, etc.
 */
public final class ListFilesTool implements SketchwareTool {

    /** Maximum directory depth to walk. */
    static final int MAX_DEPTH = 3;

    /** Maximum number of entries to return. */
    static final int MAX_ENTRIES = 200;

    @Override public String name() { return "list_files"; }
    @Override public String category() { return "filesystem"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "List files and directories under the project root (recursive, up to "
                + MAX_DEPTH + " levels deep). Returns a tree view. Use to explore project structure. "
                + "NOTE: Sketchware projects store data as FILES, not directories — "
                + "'view' (all widget data), 'logic' (all blocks), 'file' (project file list), "
                + "'library' (lib config), 'resource' (resource zip), 'project_config', "
                + "'permission'. There is NO 'resource/layout' directory. To list layouts, "
                + "use view_manage_layout with action='list' instead.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject path = new JsonObject();
        path.addProperty("type", "string");
        path.addProperty("description", "Relative path from project root to list (default: root). "
                + "Example: 'java', 'resource', 'assets'. Use '' for root.");
        props.add("path", path);
        JsonObject recursive = new JsonObject();
        recursive.addProperty("type", "boolean");
        recursive.addProperty("description", "If true (default), list recursively; if false, only direct children.");
        recursive.addProperty("default", true);
        props.add("recursive", recursive);
        schema.add("properties", props);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String relPath = args.has("path") && !args.get("path").isJsonNull()
                ? args.get("path").getAsString().trim() : "";
        boolean recursive = !args.has("recursive") || args.get("recursive").isJsonNull()
                || args.get("recursive").getAsBoolean();

        File projectRoot = resolveProjectRoot(ctx);
        if (projectRoot == null || !projectRoot.exists()) {
            return ToolResult.error("Project root not found for sc_id='" + ctx.getScId() + "'. "
                    + "Tried wq.b(sc_id) and fallback paths. Make sure the project is open in the editor.");
        }
        // Path-traversal guard: reject `..` segments and any path that
        // resolves outside the project root. Without this, an LLM (or an
        // attacker via prompt injection in a shared .sc project) can read
        // arbitrary files like `/sdcard/Android/data/.../shared_prefs`.
        File target;
        if (relPath.isEmpty()) {
            target = projectRoot;
        } else {
            String abs = PathSafety.resolveUnderRoot(projectRoot.getAbsolutePath(), relPath);
            if (abs == null) {
                return ToolResult.error("Path '" + relPath + "' is not a safe project-relative "
                        + "path. Path traversal (..) and absolute paths are not allowed.");
            }
            target = new File(abs);
        }
        if (!target.exists()) {
            // Helpful error: show what DOES exist at the root.
            StringBuilder hint = new StringBuilder();
            File[] children = projectRoot.listFiles();
            if (children != null) {
                hint.append(" Available entries at root: ");
                for (int i = 0; i < children.length; i++) {
                    if (i > 0) hint.append(", ");
                    hint.append(children[i].getName());
                }
                hint.append(". NOTE: Sketchware stores data as FILES not directories. "
                        + "Use view_manage_layout action='list' to see layouts.");
            }
            return ToolResult.error("Path does not exist: '" + relPath + "'." + hint);
        }
        if (!target.isDirectory()) {
            // It's a file — just return its name.
            return ToolResult.success(target.getName() + " (file, " + target.length() + " bytes)");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Path: ").append(relPath.isEmpty() ? "/" : relPath).append("\n\n");
        List<String> entries = new ArrayList<>();
        int depth = recursive ? MAX_DEPTH : 1;
        walk(target, "", depth, entries);
        for (String entry : entries) {
            sb.append(entry).append("\n");
            if (entries.size() > MAX_ENTRIES && entries.indexOf(entry) == MAX_ENTRIES - 1) {
                sb.append("... (").append(entries.size() - MAX_ENTRIES).append(" more entries truncated)\n");
                break;
            }
        }
        sb.append("\nTotal: ").append(Math.min(entries.size(), MAX_ENTRIES))
          .append(" entries (capped at ").append(MAX_ENTRIES).append(")");
        return ToolResult.success(sb.toString());
    }

    private void walk(File dir, String prefix, int depth, List<String> out) {
        if (depth <= 0 || out.size() >= MAX_ENTRIES * 2) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        // Sort: directories first, then files; alphabetically within each.
        List<File> sorted = new ArrayList<>(Arrays.asList(children));
        Collections.sort(sorted, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File child : sorted) {
            if (child.getName().startsWith(".")) continue;
            String line = prefix + (child.isDirectory() ? "[DIR]  " : "       ") + child.getName();
            if (!child.isDirectory()) {
                line += "  (" + formatSize(child.length()) + ")";
            }
            out.add(line);
            if (child.isDirectory()) {
                walk(child, prefix + "  ", depth - 1, out);
            }
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "K";
        return (bytes / (1024 * 1024)) + "M";
    }

    /**
     * Resolve the project root directory using Sketchware's own path
     * resolver {@code wq.b(sc_id)}, which returns
     * {@code <external_storage>/.sketchware/data/<sc_id>}.
     *
     * <p>Previous implementation used hardcoded paths that didn't match
     * modern Android scoped storage. Using {@code wq.b(sc_id)} ensures we
     * get the exact same path Sketchware itself uses to read/write project
     * data.
     */
    static File resolveProjectRoot(SketchwareToolContext ctx) {
        if (ctx == null) return null;
        String scId = ctx.getScId();
        if (scId == null || scId.isEmpty()) return null;
        // Primary: use Sketchware's own path resolver.
        try {
            String path = (String) SketchwareApi.invokeStatic("a.a.a.wq", "b", scId);
            if (path != null) {
                File f = new File(path);
                if (f.exists() && f.isDirectory()) return f;
            }
        } catch (Throwable ignored) {}
        // Fallback candidates for unusual installations.
        String[] candidates = {
            "/data/data/pro.sketchware/files/.sketchware/data/" + scId,
            "/sdcard/.sketchware/data/" + scId,
            "/storage/emulated/0/.sketchware/data/" + scId
        };
        for (String path : candidates) {
            if (path == null) continue;
            File f = new File(path);
            if (f.exists() && f.isDirectory()) return f;
        }
        // Last resort: use the app's files directory.
        android.content.Context c = ctx.getContext();
        if (c != null) {
            File f = new File(c.getFilesDir(), ".sketchware/data/" + scId);
            if (f.exists() && f.isDirectory()) return f;
        }
        return null;
    }
}
