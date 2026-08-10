package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * view_manage_custom_widget — universal tool for view operations.
 *
 * <p>Replaces 5 stubs: view_manage_custom_widget:create, view_manage_custom_widget:edit, view_manage_custom_widget:delete, view_manage_custom_widget:export, view_manage_custom_widget:import
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ViewManageCustomWidgetTool extends UniversalTool {

    public ViewManageCustomWidgetTool() {
        super("view_manage_custom_widget",
                "Manage custom widget definitions: create, edit, delete, export, or import.",
                "view", false, false,
"create",
                "edit",
                "delete",
                "export",
                "import");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Custom widget name.");
        props.add("name", p_name);
        JsonObject p_definition = new JsonObject();
        p_definition.addProperty("type", "string");
        p_definition.addProperty("description", "(create/edit) JSON definition of the widget structure.");
        props.add("definition", p_definition);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(export/import) File system path.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                String definition = optString(args, "definition", "{}");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "a", "_custom_widget:" + name, definition);
                                    ctx.refreshViewEditor();
                                    return ok("Created custom widget '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "edit": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                String definition = optString(args, "definition", "{}");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "b", "_custom_widget:" + name, definition);
                                    ctx.refreshViewEditor();
                                    return ok("Edited custom widget '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String name = optString(args, "name");
                                if (name == null) return err("name is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "c", "_custom_widget:" + name);
                                    ctx.refreshViewEditor();
                                    return ok("Deleted custom widget '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "export": {
                String name = optString(args, "name");
                                String path = optString(args, "file_path");
                                if (name == null || path == null) return err("name and file_path are required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    Object def = SketchwareApi.invoke(editor, "d", "_custom_widget:" + name);
                                    java.nio.file.Files.write(java.nio.file.Paths.get(path),
                                            java.util.Collections.singletonList(String.valueOf(def)));
                                    return ok("Exported custom widget '" + name + "' to " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
                                    String name = optString(args, "name", java.nio.file.Paths.get(path).getFileName().toString().replace(".json", ""));
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "a", "_custom_widget:" + name, content);
                                    ctx.refreshViewEditor();
                                    return ok("Imported custom widget from " + path + " as '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
