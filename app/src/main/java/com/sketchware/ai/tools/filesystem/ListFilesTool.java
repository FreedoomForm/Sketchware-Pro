package com.sketchware.ai.tools.filesystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

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
                + MAX_DEPTH + " levels deep). Returns a tree view. Use to explore project structure.";
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
            return ToolResult.error("Project root not found: " + (projectRoot == null ? "null" : projectRoot.getAbsolutePath()));
        }
        File target = relPath.isEmpty() ? projectRoot : new File(projectRoot, relPath);
        if (!target.exists()) {
            return ToolResult.error("Path does not exist: " + relPath);
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
     * Resolve the project root directory. Tries multiple known Sketchware
     * project data paths in order.
     */
    static File resolveProjectRoot(SketchwareToolContext ctx) {
        if (ctx == null) return null;
        String scId = ctx.getScId();
        if (scId == null || scId.isEmpty()) return null;
        // Try the standard Sketchware data paths in order of preference.
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
        // Fallback: use the app's files directory.
        android.content.Context c = ctx.getContext();
        if (c != null) {
            File f = new File(c.getFilesDir(), ".sketchware/data/" + scId);
            if (f.exists() && f.isDirectory()) return f;
        }
        return null;
    }
}
