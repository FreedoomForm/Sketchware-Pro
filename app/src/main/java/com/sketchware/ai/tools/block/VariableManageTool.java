package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * variable_manage — universal tool for block operations.
 *
 * <p>Replaces 5 stubs: variable_manage:create, variable_manage:delete, variable_manage:get_value, variable_manage:rename, variable_manage:set_value
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class VariableManageTool extends UniversalTool {

    public VariableManageTool() {
        super("variable_manage",
                "Manage project-level variables: create, delete, get_value, rename, set_value.",
                "block", false, false,
"create",
                "delete",
                "get_value",
                "rename",
                "set_value");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Variable name.");
        props.add("name", p_name);
        JsonObject p_type = new JsonObject();
        p_type.addProperty("type", "string");
        p_type.addProperty("description", "(create) Variable type: Boolean|Double|String|Map|List.");
        props.add("type", p_type);
        JsonObject p_new_name = new JsonObject();
        p_new_name.addProperty("type", "string");
        p_new_name.addProperty("description", "(rename) New variable name.");
        props.add("new_name", p_new_name);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(set_value) Variable value as string.");
        props.add("value", p_value);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "name");
                                String type = optString(args, "type", "String");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "a", "var:" + type + " " + name);
                                    return ok("Created variable '" + name + "' of type " + type + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "b", "var:" + name);
                                    return ok("Deleted variable '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "get_value": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object val = SketchwareApi.invoke(pf, "c", "var:" + name);
                                    return ok("Variable '" + name + "' value: " + val);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "rename": {
                String name = optString(args, "name");
                                String newName = optString(args, "new_name");
                                if (name == null || newName == null) return err("name and new_name required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "d", "var:" + name, "var:" + newName);
                                    return ok("Renamed variable '" + name + "' → '" + newName + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_value": {
                String name = optString(args, "name");
                                String value = optString(args, "value");
                                if (name == null || value == null) return err("name and value required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "e", "var:" + name, value);
                                    return ok("Set variable '" + name + "' = '" + value + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
