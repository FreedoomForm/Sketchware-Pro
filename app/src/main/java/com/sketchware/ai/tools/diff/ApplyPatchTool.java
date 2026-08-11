package com.sketchware.ai.tools.diff;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.ToolResultFormatter;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.utility.FileUtil;

/**
 * apply_patch - apply a multi-file unified diff to the project's files/
 * directory. Port of Cline's {@code apply_patch} tool.
 *
 * <p>Enables single-call editing of multiple files: add new files, update
 * existing files (with hunk-based line replacement), and delete files.
 *
 * <h2>Format</h2>
 * <pre>
 * *** Begin Patch
 * *** Add File: path/to/new.txt
 * +line 1
 * +line 2
 * *** End File
 * *** Update File: path/to/existing.txt
 * @@ context
 * -old line
 * +new line
 *  unchanged context
 * *** End File
 * *** Delete File: path/to/old.txt
 * *** End Patch
 * </pre>
 *
 * <h2>Atomicity</h2>
 * The tool is NOT atomic: if file 3 of 5 fails, files 1-2 are already applied.
 * The tool returns a per-file result list so the LLM can see which succeeded
 * and which failed, then retry the failures.
 *
 * <p>This matches Cline's behaviour — atomic multi-file patches would require
 * a transactional filesystem which Android doesn't provide.
 */
public final class ApplyPatchTool implements SketchwareTool {

    @Override public String name() { return "apply_patch"; }
    @Override public String category() { return "java"; }
    @Override public boolean isReadOnly() { return false; }
    @Override public boolean isAutoApprovedByDefault() { return false; }

    @Override public String description() {
        return "Apply a multi-file unified-diff patch to the project's files/ directory. "
                + "Supports adding new files, updating existing files (hunk-based line replacement), "
                + "and deleting files in a single tool call. Use this when you need to edit multiple "
                + "files at once or when SEARCH/REPLACE blocks aren't sufficient. Format:\n"
                + "*** Begin Patch\n*** Add File: <path>\n+<content>\n*** End File\n"
                + "*** Update File: <path>\n@@<context>\n-<remove>\n+<add>\n*** End File\n"
                + "*** Delete File: <path>\n*** End Patch";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject patch = new JsonObject();
        patch.addProperty("type", "string");
        patch.addProperty("description",
                "Multi-file patch starting with '*** Begin Patch' and ending with '*** End Patch'. "
                + "See tool description for format details.");
        props.add("patch", patch);

        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("patch");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String patch = args.has("patch") && !args.get("patch").isJsonNull()
                ? args.get("patch").getAsString() : null;
        if (patch == null || patch.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.missingArgument(
                    name(), "patch", "specify the patch to apply"));
        }

        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");

        File base = new File(Environment.getExternalStorageDirectory(), ".sketchware/data/" + scId + "/files");

        List<PatchParser.PatchOp> ops;
        try {
            ops = PatchParser.parse(patch);
        } catch (PatchParser.PatchException e) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "patch parse error: " + e.getMessage(),
                    "Ensure the patch starts with '*** Begin Patch' and ends with '*** End Patch'."));
        }
        if (ops.isEmpty()) {
            return ToolResult.error(ToolResultFormatter.toolError(name(),
                    "no operations found in patch",
                    "Add at least one Add/Update/Delete file operation."));
        }

        List<String> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (PatchParser.PatchOp op : ops) {
            String result = applyOp(base, op);
            results.add(result);
            if (result.startsWith("OK:")) succeeded++;
            else failed++;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Patch applied: ").append(succeeded).append(" succeeded, ")
               .append(failed).append(" failed, out of ").append(ops.size()).append(" ops.\n");
        for (int i = 0; i < results.size(); i++) {
            summary.append("  [").append(i + 1).append("] ").append(results.get(i)).append("\n");
        }
        return ToolResult.success(summary.toString());
    }

    private String applyOp(File base, PatchParser.PatchOp op) {
        File target = new File(base, op.path);
        try {
            switch (op.type) {
                case ADD: {
                    if (FileUtil.isExistFile(target.getAbsolutePath())) {
                        return "FAIL: " + op.path + " already exists (use Update File instead)";
                    }
                    File parent = target.getParentFile();
                    if (parent != null && !parent.exists()) {
                        if (!parent.mkdirs()) {
                            return "FAIL: " + op.path + " could not create parent directory";
                        }
                    }
                    FileUtil.writeFile(target.getAbsolutePath(), op.addContent == null ? "" : op.addContent);
                    int lines = op.addContent == null ? 0 : countLines(op.addContent);
                    return "OK: " + op.path + " added (" + lines + " lines)";
                }
                case UPDATE: {
                    if (!FileUtil.isExistFile(target.getAbsolutePath())) {
                        return "FAIL: " + op.path + " not found (use Add File to create)";
                    }
                    String original = FileUtil.readFile(target.getAbsolutePath());
                    String updated = PatchParser.applyUpdateHunk(original, op.updateHunk, op.path);
                    if (original.equals(updated)) {
                        return "OK: " + op.path + " no changes (hunk matched but produced identical content)";
                    }
                    FileUtil.writeFile(target.getAbsolutePath(), updated);
                    int delta = countLines(updated) - countLines(original);
                    return "OK: " + op.path + " updated (" + (delta >= 0 ? "+" : "") + delta + " lines)";
                }
                case DELETE: {
                    if (!FileUtil.isExistFile(target.getAbsolutePath())) {
                        return "FAIL: " + op.path + " not found (already deleted?)";
                    }
                    if (!target.delete()) {
                        return "FAIL: " + op.path + " could not delete (File.delete() returned false)";
                    }
                    return "OK: " + op.path + " deleted";
                }
                default:
                    return "FAIL: " + op.path + " unknown op type " + op.type;
            }
        } catch (Throwable t) {
            return "FAIL: " + op.path + " " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private static int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int n = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') n++;
        return n;
    }
}
