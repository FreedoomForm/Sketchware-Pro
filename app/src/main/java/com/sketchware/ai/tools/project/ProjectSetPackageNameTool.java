package com.sketchware.ai.tools.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * project_set_package_name - change the project's package name.
 */
public final class ProjectSetPackageNameTool implements SketchwareTool {

    @Override public String name() { return "project_set_package_name"; }
    @Override public String category() { return "project"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Set the project's Java package name (e.g. 'com.example.myapp').";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject pkg = new JsonObject();
        pkg.addProperty("type", "string");
        pkg.addProperty("pattern", "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
        props.add("package_name", pkg);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("package_name");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String pkg = args.has("package_name") ? args.get("package_name").getAsString() : null;
        if (pkg == null || pkg.isEmpty()) return ToolResult.error("package_name is required");
        if (!pkg.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")) {
            return ToolResult.error("Invalid package name format.");
        }
        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");
        try {
            Object data = SketchwareApi.invokeStatic("a.a.a.lC", "b", scId);
            if (data instanceof java.util.HashMap) {
                @SuppressWarnings("unchecked")
                java.util.HashMap<String, Object> map = (java.util.HashMap<String, Object>) data;
                map.put("my_sc_pkg_name", pkg);
                SketchwareApi.invokeStatic("a.a.a.lC", "b", scId, map);
            }
            return ToolResult.success("Package name set to '" + pkg + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
