package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * map_manage — universal tool for block operations.
 *
 * <p>Replaces 3 stubs: map_manage:create, map_manage:get, map_manage:put
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class MapManageTool extends UniversalTool {

    public MapManageTool() {
        super("map_manage",
                "Manage project-level maps: create, get, put.",
                "block", false, false,
"create",
                "get",
                "put");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Map name.");
        props.add("name", p_name);
        JsonObject p_key = new JsonObject();
        p_key.addProperty("type", "string");
        p_key.addProperty("description", "(get/put) Map key.");
        props.add("key", p_key);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(put) Map value.");
        props.add("value", p_value);
        JsonObject p_key_type = new JsonObject();
        p_key_type.addProperty("type", "string");
        p_key_type.addProperty("description", "(create) Key type: String|Number.");
        props.add("key_type", p_key_type);
        JsonObject p_value_type = new JsonObject();
        p_value_type.addProperty("type", "string");
        p_value_type.addProperty("description", "(create) Value type: String|Number|Map|List.");
        props.add("value_type", p_value_type);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "name");
                                String kt = optString(args, "key_type", "String");
                                String vt = optString(args, "value_type", "String");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "a", "map:" + kt + "," + vt + " " + name);
                                    return ok("Created map '" + name + "' (" + kt + "→" + vt + ").");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "get": {
                String name = optString(args, "name");
                                String key = optString(args, "key");
                                if (name == null || key == null) return err("name and key required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object val = SketchwareApi.invoke(pf, "j", "map:" + name, key);
                                    return ok("Map '" + name + "'[" + key + "] = " + val);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "put": {
                String name = optString(args, "name");
                                String key = optString(args, "key");
                                String val = optString(args, "value");
                                if (name == null || key == null || val == null) return err("name, key, value required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "k", "map:" + name, key, val);
                                    return ok("Set map '" + name + "'[" + key + "] = '" + val + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
