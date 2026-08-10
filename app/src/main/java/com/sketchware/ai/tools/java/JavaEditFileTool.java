package com.sketchware.ai.tools.java;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import android.os.Environment;

import java.io.File;

import pro.sketchware.utility.FileUtil;

/**
 * java_edit_file - write content to a Java/Kotlin/XML file in the project's
 * files/ directory.
 */
public final class JavaEditFileTool implements SketchwareTool {

    @Override public String name() { return "java_edit_file"; }
    @Override public String category() { return "java"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Write content to a Java/Kotlin/XML source file. file_path is relative to "
                + "the project's files/ directory (e.g. 'java/com/example/MainActivity.java', "
                + "'resource/layout/my_layout.xml'). The file is created if it doesn't exist.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject filePath = new JsonObject();
        filePath.addProperty("type", "string");
        props.add("file_path", filePath);
        JsonObject content = new JsonObject();
        content.addProperty("type", "string");
        props.add("content", content);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("file_path");
        required.add("content");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String relativePath = args.has("file_path") ? args.get("file_path").getAsString() : null;
        String content = args.has("content") && !args.get("content").isJsonNull()
                ? args.get("content").getAsString() : "";
        if (relativePath == null) return ToolResult.error("file_path is required");
        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");
        try {
            File base = new File(Environment.getExternalStorageDirectory(), ".sketchware/data/" + scId + "/files");
            String fullPath = new File(base, relativePath).getAbsolutePath();
            FileUtil.writeFile(fullPath, content);
            return ToolResult.success("Wrote " + content.length() + " chars to " + relativePath + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
