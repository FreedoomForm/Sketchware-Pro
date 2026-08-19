package com.sketchware.ai.tools.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * project_set_app_name - change the project's app name via reflection.
 */
public final class ProjectSetAppNameTool implements SketchwareTool {

    @Override public String name() { return "project_set_app_name"; }
    @Override public String category() { return "project"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Set the project's app name. Updates the project metadata.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject name = new JsonObject();
        name.addProperty("type", "string");
        name.addProperty("minLength", 1);
        name.addProperty("maxLength", 50);
        props.add("app_name", name);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("app_name");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String appName = args.has("app_name") ? args.get("app_name").getAsString() : null;
        if (appName == null || appName.isEmpty()) return ToolResult.error("app_name is required");
        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");
        try {
            // Read existing project metadata via lC.b(scId)
            Object data = SketchwareApi.invokeStatic("a.a.a.lC", "b", scId);
            if (data instanceof java.util.HashMap) {
                @SuppressWarnings("unchecked")
                java.util.HashMap<String, Object> map = (java.util.HashMap<String, Object>) data;
                map.put("my_app_name", appName);
                SketchwareApi.invokeStatic("a.a.a.lC", "b", scId, map);
            }
            return ToolResult.success("App name set to '" + appName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
