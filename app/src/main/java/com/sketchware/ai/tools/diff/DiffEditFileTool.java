package com.sketchware.ai.tools.diff;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.ToolResultFormatter;
import com.sketchware.ai.util.PathSafety;

import android.os.Environment;

import java.io.File;
import java.util.List;

import pro.sketchware.utility.FileUtil;

/**
 * diff_edit_file - apply SEARCH/REPLACE blocks to a file in the project's
 * files/ directory. Port of Cline's {@code replace_in_file} tool.
 *
 * <p>Unlike {@link com.sketchware.ai.tools.java.JavaEditFileTool} which
 * rewrites the entire file, this tool takes a series of SEARCH/REPLACE
 * blocks and applies them in-place. This is far more token-efficient for
 * small edits to large files: a 1000-line Java file with a 5-line change
 * needs only ~10 lines of diff instead of 1000 lines of full-file content.
 *
 * <h2>Format</h2>
 * The {@code diff} parameter contains one or more SEARCH/REPLACE blocks:
 * <pre>
 * &lt;&lt;&lt;&lt;&lt;&lt;&lt; SEARCH
 *  lines to find (must match exactly OR line-trimmed OR block-anchor)
 * =======
 *  replacement lines
 * &gt;&gt;&gt;&gt;&gt;&gt;&gt; REPLACE
 * </pre>
 *
 * <p>Multiple blocks can be chained in a single call; they're applied in order.
 *
 * <h2>Matching</h2>
 * Uses {@link DiffParser}'s 3-tier matching:
 * <ol>
 *   <li>Exact match.</li>
 *   <li>Line-trimmed match (whitespace per line ignored).</li>
 *   <li>Block-anchor match (first+last non-blank lines as anchors).</li>
 * </ol>
 *
 * <p>If a block fails all three tiers, the tool returns an error with the
 * block index and the search snippet so the LLM can correct and retry.
 */
public final class DiffEditFileTool implements SketchwareTool {

    @Override public String name() { return "diff_edit_file"; }
    @Override public String category() { return "java"; }
    @Override public boolean isReadOnly() { return false; }
    @Override public boolean isAutoApprovedByDefault() { return false; }

    @Override public String description() {
        return "Apply SEARCH/REPLACE diff blocks to an existing file in the project's files/ directory. "
                + "Prefer this over java_edit_file for small edits to large files - it saves tokens. "
                + "The diff parameter must contain one or more blocks delimited by '<<<<<<< SEARCH' / "
                + "'=======' / '>>>>>>> REPLACE' markers. Matching is forgiving: exact, line-trimmed, "
                + "and block-anchor matches are tried in order.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject filePath = new JsonObject();
        filePath.addProperty("type", "string");
        filePath.addProperty("description", "Path of the file to edit, relative to project files/.");
        props.add("file_path", filePath);

        JsonObject diff = new JsonObject();
        diff.addProperty("type", "string");
        diff.addProperty("description",
                "One or more SEARCH/REPLACE blocks. Each block:\n"
                + "<<<<<<< SEARCH\n lines to find\n=======\n replacement lines\n>>>>>>> REPLACE");
        props.add("diff", diff);

        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("file_path");
        required.add("diff");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String relativePath = args.has("file_path") && !args.get("file_path").isJsonNull()
                ? args.get("file_path").getAsString() : null;
        String diff = args.has("diff") && !args.get("diff").isJsonNull()
                ? args.get("diff").getAsString() : null;

        if (relativePath == null || relativePath.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.missingArgument(
                    name(), "file_path", "locate the file to edit"));
        }
        if (diff == null || diff.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.missingArgument(
                    name(), "diff", "specify the SEARCH/REPLACE blocks to apply"));
        }

        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");

        File base = new File(Environment.getExternalStorageDirectory(), ".sketchware/data/" + scId + "/files");
        // Path-traversal guard (see JavaReadFileTool for rationale).
        String fullPath = PathSafety.resolveUnderRoot(base.getAbsolutePath(), relativePath);
        if (fullPath == null) {
            return ToolResult.error("file_path '" + relativePath + "' is not a safe "
                    + "project-relative path. Path traversal (..) and absolute paths "
                    + "are not allowed.");
        }
        if (!FileUtil.isExistFile(fullPath)) {
            return ToolResult.error(ToolResultFormatter.notFound(
                    "File", relativePath, "project files/",
                    null));
        }

        String original;
        try {
            original = FileUtil.readFile(fullPath);
        } catch (Throwable t) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "could not read file: " + t.getMessage(),
                    "Check file permissions and retry."));
        }

        List<DiffParser.Block> blocks;
        try {
            blocks = DiffParser.parse(diff);
        } catch (DiffParser.DiffApplyException e) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "diff parse error: " + e.getMessage(),
                    "Ensure each block has '<<<<<<< SEARCH', '=======', and '>>>>>>> REPLACE' markers."));
        }
        if (blocks.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "no SEARCH/REPLACE blocks found in diff",
                    "Add at least one block delimited by '<<<<<<< SEARCH' / '>>>>>>> REPLACE'."));
        }

        String updated;
        try {
            updated = DiffParser.apply(original, blocks);
        } catch (DiffParser.DiffApplyException e) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "block " + e.blockIndex + " failed to match: " + e.getMessage(),
                    "Verify the SEARCH block content matches the file. Search snippet:\n"
                            + e.searchSnippet));
        }

        if (original.equals(updated)) {
            // No-op diff: warn but don't fail (could be intentional).
            return ToolResult.success(ToolResultFormatter.toolSuccess(name(),
                    "diff applied but produced no changes (file content identical). "
                    + "Blocks: " + blocks.size() + "."));
        }

        try {
            FileUtil.writeFile(fullPath, updated);
        } catch (Throwable t) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "could not write file: " + t.getMessage(),
                    "Check storage permissions."));
        }

        int addedLines = countLines(updated) - countLines(original);
        return ToolResult.success(ToolResultFormatter.toolSuccess(name(),
                "applied " + blocks.size() + " SEARCH/REPLACE block(s) to " + relativePath
                        + ". File now " + countLines(updated) + " lines ("
                        + (addedLines >= 0 ? "+" : "") + addedLines + ")."));
    }

    private static int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int n = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') n++;
        return n;
    }
}
