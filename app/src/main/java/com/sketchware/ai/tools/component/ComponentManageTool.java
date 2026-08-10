package com.sketchware.ai.tools.component;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * component_manage — universal tool for component operations.
 *
 * <p>Replaces 8 stubs: component_manage:attach_event, component_manage:clone, component_manage:delete, component_manage:export_to_collection, component_manage:import_from_collection, component_manage:list, component_manage:open_event, component_manage:rename
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ComponentManageTool extends UniversalTool {

    public ComponentManageTool() {
        super("component_manage",
                "Manage components (non-UI widgets like Timer, RequestNetwork, Firebase, Intent, SharedPreferences, etc.): attach event, clone, delete, export, import, list, open, or rename.",
                "component", false, false,
"attach_event",
                "clone",
                "delete",
                "export_to_collection",
                "import_from_collection",
                "list",
                "open_event",
                "rename");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_component_id = new JsonObject();
        p_component_id.addProperty("type", "string");
        p_component_id.addProperty("description", "ID of the component.");
        props.add("component_id", p_component_id);
        JsonObject p_event_name = new JsonObject();
        p_event_name.addProperty("type", "string");
        p_event_name.addProperty("description", "(attach_event) Event name.");
        props.add("event_name", p_event_name);
        JsonObject p_new_id = new JsonObject();
        p_new_id.addProperty("type", "string");
        p_new_id.addProperty("description", "(clone/rename) New component ID.");
        props.add("new_id", p_new_id);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(export/import) File system path.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "attach_event": {
                String compId = optString(args, "component_id");
                                String eventName = optString(args, "event_name");
                                if (compId == null || eventName == null) return err("component_id and event_name required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "a", compId, eventName);
                                    ctx.refreshComponentList();
                                    return ok("Attached event '" + eventName + "' to component '" + compId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "clone": {
                String compId = optString(args, "component_id");
                                String newId = optString(args, "new_id", compId + "_copy");
                                if (compId == null) return err("component_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "b", compId, newId);
                                    ctx.refreshComponentList();
                                    return ok("Cloned component '" + compId + "' → '" + newId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String compId = optString(args, "component_id");
                                if (compId == null) return err("component_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "c", compId);
                                    ctx.refreshComponentList();
                                    return ok("Deleted component '" + compId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "export_to_collection": {
                String compId = optString(args, "component_id");
                                String path = optString(args, "file_path");
                                if (compId == null || path == null) return err("component_id and file_path required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    Object def = SketchwareApi.invoke(editor, "d", compId);
                                    java.nio.file.Files.write(java.nio.file.Paths.get(path),
                                            java.util.Collections.singletonList(String.valueOf(def)));
                                    return ok("Exported component '" + compId + "' to " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import_from_collection": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path required");
                                try {
                                    String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "e", content);
                                    ctx.refreshComponentList();
                                    return ok("Imported component from " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "list": {
                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    Object list = SketchwareApi.invoke(editor, "f");
                                    return ok("Components: " + list);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "open_event": {
                String compId = optString(args, "component_id");
                                if (compId == null) return err("component_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "g", compId);
                                    ctx.refreshEventList();
                                    return ok("Opened events for component '" + compId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "rename": {
                String compId = optString(args, "component_id");
                                String newId = optString(args, "new_id");
                                if (compId == null || newId == null) return err("component_id and new_id required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(editor, "h", compId, newId);
                                    ctx.refreshComponentList();
                                    return ok("Renamed component '" + compId + "' → '" + newId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
