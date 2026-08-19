package com.sketchware.ai.tools.manifest;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * appcompat_manage — universal tool for manifest operations.
 *
 * <p>Replaces 3 stubs: appcompat_manage:add_attribute, appcompat_manage:delete_attribute, appcompat_manage:reset_to_defaults
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class AppcompatManageTool extends UniversalTool {

    public AppcompatManageTool() {
        super("appcompat_manage",
                "Manage AppCompat attributes: add_attribute, delete_attribute, reset_to_defaults.",
                "manifest", false, false,
"add_attribute",
                "delete_attribute",
                "reset_to_defaults");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_attribute_name = new JsonObject();
        p_attribute_name.addProperty("type", "string");
        p_attribute_name.addProperty("description", "Attribute name.");
        props.add("attribute_name", p_attribute_name);
        JsonObject p_attribute_value = new JsonObject();
        p_attribute_value.addProperty("type", "string");
        p_attribute_value.addProperty("description", "(add_attribute) Attribute value.");
        props.add("attribute_value", p_attribute_value);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_attribute": {
                String attr = optString(args, "attribute_name");
                                String val = optString(args, "attribute_value");
                                if (attr == null || val == null) return err("attribute_name and attribute_value required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "m", attr, val);
                                    return ok("Added AppCompat attribute '" + attr + "' = '" + val + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete_attribute": {
                String attr = optString(args, "attribute_name");
                                if (attr == null) return err("attribute_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "n", attr);
                                    return ok("Deleted AppCompat attribute '" + attr + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "reset_to_defaults": {
                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "o");
                                    return ok("Reset AppCompat attributes to defaults.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
