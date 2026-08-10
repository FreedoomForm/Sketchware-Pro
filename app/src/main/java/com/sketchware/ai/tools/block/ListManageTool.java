package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * list_manage — universal tool for block operations.
 *
 * <p>Replaces 6 stubs: list_manage:add_item, list_manage:clear, list_manage:create, list_manage:delete, list_manage:remove_item, list_manage:size
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ListManageTool extends UniversalTool {

    public ListManageTool() {
        super("list_manage",
                "Manage project-level lists: create, delete, add_item, remove_item, clear, size.",
                "block", false, false,
"add_item",
                "clear",
                "create",
                "delete",
                "remove_item",
                "size");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "List name.");
        props.add("name", p_name);
        JsonObject p_item_type = new JsonObject();
        p_item_type.addProperty("type", "string");
        p_item_type.addProperty("description", "(create) Item type: String|Map|Number.");
        props.add("item_type", p_item_type);
        JsonObject p_index = new JsonObject();
        p_index.addProperty("type", "integer");
        p_index.addProperty("description", "(remove_item) Index of item to remove.");
        props.add("index", p_index);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(add_item) Item value.");
        props.add("value", p_value);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_item": {
                String name = optString(args, "name");
                                String value = optString(args, "value");
                                if (name == null || value == null) return err("name and value required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "f", "list:" + name, value);
                                    return ok("Added item '" + value + "' to list '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "clear": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "h", "list:" + name);
                                    return ok("Cleared list '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "create": {
                String name = optString(args, "name");
                                String type = optString(args, "item_type", "String");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "a", "list:" + type + " " + name);
                                    return ok("Created list '" + name + "' of " + type + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "b", "list:" + name);
                                    return ok("Deleted list '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove_item": {
                String name = optString(args, "name");
                                int idx = optInt(args, "index", -1);
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "g", "list:" + name, idx);
                                    return ok("Removed item at index " + idx + " from list '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "size": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object size = SketchwareApi.invoke(pf, "i", "list:" + name);
                                    return ok("List '" + name + "' size: " + size);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
