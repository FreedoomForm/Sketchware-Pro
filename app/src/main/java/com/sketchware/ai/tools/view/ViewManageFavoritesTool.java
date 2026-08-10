package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * view_manage_favorites — universal tool for view operations.
 *
 * <p>Replaces 4 stubs: view_manage_favorites:save_widget, view_manage_favorites:add_collection, view_manage_favorites:delete_collection, view_manage_favorites:add_image_resource_inline
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ViewManageFavoritesTool extends UniversalTool {

    public ViewManageFavoritesTool() {
        super("view_manage_favorites",
                "Manage the widget favorites collection: save a widget, add a collection, delete a collection, or add an inline image resource.",
                "view", false, false,
"save_widget",
                "add_collection",
                "delete_collection",
                "add_image_resource_inline");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_widget_id = new JsonObject();
        p_widget_id.addProperty("type", "string");
        p_widget_id.addProperty("description", "ID of the widget (save_widget).");
        props.add("widget_id", p_widget_id);
        JsonObject p_collection_name = new JsonObject();
        p_collection_name.addProperty("type", "string");
        p_collection_name.addProperty("description", "Name of the favorites collection.");
        props.add("collection_name", p_collection_name);
        JsonObject p_image_name = new JsonObject();
        p_image_name.addProperty("type", "string");
        p_image_name.addProperty("description", "(add_image_resource_inline) Image resource name.");
        props.add("image_name", p_image_name);
        JsonObject p_image_data = new JsonObject();
        p_image_data.addProperty("type", "string");
        p_image_data.addProperty("description", "(add_image_resource_inline) Base64-encoded image data or path.");
        props.add("image_data", p_image_data);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "save_widget": {
                String widgetId = optString(args, "widget_id");
                                String collection = optString(args, "collection_name", "default");
                                if (widgetId == null) return err("widget_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "e", widgetId, collection);
                                    return ok("Saved widget '" + widgetId + "' to favorites collection '" + collection + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_collection": {
                String collection = optString(args, "collection_name");
                                if (collection == null) return err("collection_name is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "f", collection);
                                    return ok("Created favorites collection '" + collection + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete_collection": {
                String collection = optString(args, "collection_name");
                                if (collection == null) return err("collection_name is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "g", collection);
                                    return ok("Deleted favorites collection '" + collection + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_image_resource_inline": {
                String name = optString(args, "image_name");
                                String data = optString(args, "image_data");
                                if (name == null || data == null) return err("image_name and image_data are required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "h", name, data);
                                    return ok("Added inline image resource '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
