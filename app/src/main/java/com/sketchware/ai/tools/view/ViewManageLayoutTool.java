package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * view_manage_layout — universal tool for layout (XML file) management.
 *
 * <p>Replaces 4 stubs: view_create_layout, view_delete_layout,
 * view_rename_layout, view_switch_active_layout.
 */
public final class ViewManageLayoutTool extends UniversalTool {

    public ViewManageLayoutTool() {
        super("view_manage_layout",
                "Manage layout XML files in the current project: create, delete, "
                        + "rename, or switch the active layout shown in the View editor.",
                "view", false, false,
                "create", "delete", "rename", "switch_active");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject name = new JsonObject();
        name.addProperty("type", "string");
        name.addProperty("description", "Layout file name (without .xml extension, e.g. 'main').");
        props.add("name", name);

        JsonObject rootTag = new JsonObject();
        rootTag.addProperty("type", "string");
        rootTag.addProperty("description", "(create only) Root view tag: LinearLayout, RelativeLayout, or ConstraintLayout. Default: LinearLayout.");
        props.add("root_tag", rootTag);

        JsonObject newName = new JsonObject();
        newName.addProperty("type", "string");
        newName.addProperty("description", "(rename only) New layout file name.");
        props.add("new_name", newName);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project.");
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");

        switch (action) {
            case "create": {
                String rootTag = optString(args, "root_tag", "LinearLayout");
                try {
                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
                    // Create a new layout: jC.a(scId).a(javaName) creates the layout file
                    // bean if missing. We use a unique name and seed an empty XML.
                    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                            + "<" + rootTag + " xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\"\n"
                            + "    android:orientation=\"vertical\">\n\n</" + rootTag + ">\n";
                    SketchwareApi.invoke(editor, "a", name, xml);
                    ctx.refreshViewEditor();
                    return ok("Created layout '" + name + "' with root <" + rootTag + ">.");
                } catch (Throwable t) {
                    return ToolResult.error(t);
                }
            }
            case "delete": {
                try {
                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
                    SketchwareApi.invoke(editor, "b", name);
                    ctx.refreshViewEditor();
                    return ok("Deleted layout '" + name + "'.");
                } catch (Throwable t) {
                    return ToolResult.error(t);
                }
            }
            case "rename": {
                String newName = optString(args, "new_name");
                if (newName == null || newName.isEmpty()) return err("new_name is required for rename.");
                try {
                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
                    SketchwareApi.invoke(editor, "a", name, newName);
                    ctx.refreshViewEditor();
                    return ok("Renamed layout '" + name + "' → '" + newName + "'.");
                } catch (Throwable t) {
                    return ToolResult.error(t);
                }
            }
            case "switch_active": {
                try {
                    // Switch the active layout in the editor: jC.a(scId).b(javaName) returns the
                    // ViewBeans collection for that layout, which becomes the active editing target.
                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
                    SketchwareApi.invoke(editor, "b", name);
                    ctx.refreshViewEditor();
                    return ok("Switched active layout to '" + name + "'.");
                } catch (Throwable t) {
                    return ToolResult.error(t);
                }
            }
            default:
                return err("Unknown action: " + action);
        }
    }
}
