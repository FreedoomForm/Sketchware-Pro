package com.sketchware.ai.tools.resource;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * values_xml_manage — universal tool for resource operations.
 *
 * <p>Replaces 12 stubs: values_xml_manage:add_array, values_xml_manage:add_bool, values_xml_manage:add_color, values_xml_manage:add_dimen, values_xml_manage:add_id, values_xml_manage:add_integer, values_xml_manage:add_string, values_xml_manage:add_style, values_xml_manage:delete_entry, values_xml_manage:import_from_default, values_xml_manage:list_entries, values_xml_manage:switch_variant
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ValuesXmlManageTool extends UniversalTool {

    public ValuesXmlManageTool() {
        super("values_xml_manage",
                "Manage values XML resources: add_array, add_bool, add_color, add_dimen, add_id, add_integer, add_string, add_style, delete_entry, import_from_default, list_entries, switch_variant.",
                "resource", false, false,
"add_array",
                "add_bool",
                "add_color",
                "add_dimen",
                "add_id",
                "add_integer",
                "add_string",
                "add_style",
                "delete_entry",
                "import_from_default",
                "list_entries",
                "switch_variant");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Resource name.");
        props.add("name", p_name);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "Resource value (string for add_string, hex color for add_color, etc.).");
        props.add("value", p_value);
        JsonObject p_variant = new JsonObject();
        p_variant.addProperty("type", "string");
        p_variant.addProperty("description", "(switch_variant) Variant name (default|night|rtl).");
        props.add("variant", p_variant);
        JsonObject p_items = new JsonObject();
        p_items.addProperty("type", "string");
        p_items.addProperty("description", "(add_array) Comma-separated array items.");
        props.add("items", p_items);
        JsonObject p_parent = new JsonObject();
        p_parent.addProperty("type", "string");
        p_parent.addProperty("description", "(add_style) Parent style name.");
        props.add("parent", p_parent);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_array": {
                return addValuesEntry(ctx, args, "array");
            }
            case "add_bool": {
                return addValuesEntry(ctx, args, "bool");
            }
            case "add_color": {
                return addValuesEntry(ctx, args, "color");
            }
            case "add_dimen": {
                return addValuesEntry(ctx, args, "dimen");
            }
            case "add_id": {
                return addValuesEntry(ctx, args, "id");
            }
            case "add_integer": {
                return addValuesEntry(ctx, args, "integer");
            }
            case "add_string": {
                return addValuesEntry(ctx, args, "string");
            }
            case "add_style": {
                return addValuesEntry(ctx, args, "style");
            }
            case "delete_entry": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object rm = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(rm, "l", "values:" + name);
                                    return ok("Deleted values entry '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import_from_default": {
                try {
                                    Object rm = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(rm, "m", "values:default");
                                    return ok("Imported default values XML.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "list_entries": {
                try {
                                    Object rm = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object entries = SketchwareApi.invoke(rm, "n", "values:");
                                    return ok("Values entries: " + entries);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "switch_variant": {
                String variant = optString(args, "variant", "default");
                                try {
                                    Object rm = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(rm, "o", "values:variant", variant);
                                    return ok("Switched values XML variant to '" + variant + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult addValuesEntry(SketchwareToolContext ctx, JsonObject args, String type) {
        String name = optString(args, "name");
        String value = optString(args, "value", "");
        if (name == null) return err("name is required");
        String extra = "";
        if (type.equals("array")) extra = optString(args, "items", "");
        if (type.equals("style")) extra = optString(args, "parent", "");
        try {
            Object rm = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
            SketchwareApi.invoke(rm, "p", "values:" + type + ":" + name, value, extra);
            return ok("Added values entry " + type + " '" + name + "' = '" + value + "'.");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
