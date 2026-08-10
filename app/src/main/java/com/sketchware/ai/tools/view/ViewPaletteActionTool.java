package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * view_palette_action — universal tool for view operations.
 *
 * <p>Replaces 4 stubs: view_palette_action:switch_palette_group, view_palette_action:switch_property_group, view_palette_action:open_property_editor, view_palette_action:commit_property_changes
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ViewPaletteActionTool extends UniversalTool {

    public ViewPaletteActionTool() {
        super("view_palette_action",
                "Operate on the View editor palette/UI: switch palette group, switch property group, open the property editor for a widget, or commit pending property changes.",
                "view", true, true,
"switch_palette_group",
                "switch_property_group",
                "open_property_editor",
                "commit_property_changes");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_group = new JsonObject();
        p_group.addProperty("type", "string");
        p_group.addProperty("description", "(switch_palette_group/switch_property_group) Group name: basic/layout/media/advanced.");
        props.add("group", p_group);
        JsonObject p_widget_id = new JsonObject();
        p_widget_id.addProperty("type", "string");
        p_widget_id.addProperty("description", "(open_property_editor) ID of widget whose properties to edit.");
        props.add("widget_id", p_widget_id);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "switch_palette_group": {
                String g = optString(args, "group", "basic");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "i", g);
                                    ctx.refreshViewEditor();
                                    return ok("Switched palette group to '" + g + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "switch_property_group": {
                String g = optString(args, "group", "basic");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "j", g);
                                    ctx.refreshViewEditor();
                                    return ok("Switched property group to '" + g + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "open_property_editor": {
                String widgetId = optString(args, "widget_id");
                                if (widgetId == null) return err("widget_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "k", widgetId);
                                    ctx.refreshViewEditor();
                                    return ok("Opened property editor for widget '" + widgetId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "commit_property_changes": {
                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "l");
                                    ctx.refreshViewEditor();
                                    return ok("Committed property changes to project.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
