package com.sketchware.ai.tools.java;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import android.os.Environment;
import java.io.File;

import pro.sketchware.utility.FileUtil;

/**
 * java_read_file - read a file's content from the project files/ directory.
 */
public final class JavaReadFileTool implements SketchwareTool {

    @Override public String name() { return "java_read_file"; }
    @Override public String category() { return "java"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Read a file's content from the project files/ directory. file_path is relative.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject filePath = new JsonObject();
        filePath.addProperty("type", "string");
        props.add("file_path", filePath);
        schema.add("properties", props);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("file_path");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String relativePath = args.has("file_path") ? args.get("file_path").getAsString() : null;
        if (relativePath == null) return ToolResult.error("file_path is required");
        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");
        try {
            File base = new File(Environment.getExternalStorageDirectory(), ".sketchware/data/" + scId + "/files");
            String fullPath = new File(base, relativePath).getAbsolutePath();
            if (!FileUtil.isExistFile(fullPath)) {
                return ToolResult.error("File not found: " + relativePath);
            }
            String content = FileUtil.readFile(fullPath);
            return ToolResult.success(content);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
