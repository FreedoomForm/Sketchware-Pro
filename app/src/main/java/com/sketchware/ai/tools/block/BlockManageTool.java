package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * block_manage — universal tool for block operations.
 *
 * <p>Replaces 10 stubs: block_manage:delete, block_manage:duplicate, block_manage:import_from_collection, block_manage:move, block_manage:redo, block_manage:save, block_manage:save_to_collection, block_manage:set_java_method, block_manage:set_parameter, block_manage:undo
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class BlockManageTool extends UniversalTool {

    public BlockManageTool() {
        super("block_manage",
                "Manage blocks inside event handlers: delete, duplicate, import from collection, move, redo, save, save to collection, set java method, set parameter, undo.",
                "block", false, false,
"delete",
                "duplicate",
                "import_from_collection",
                "move",
                "redo",
                "save",
                "save_to_collection",
                "set_java_method",
                "set_parameter",
                "undo");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID containing the block.");
        props.add("event_id", p_event_id);
        JsonObject p_block_id = new JsonObject();
        p_block_id.addProperty("type", "string");
        p_block_id.addProperty("description", "ID of the target block.");
        props.add("block_id", p_block_id);
        JsonObject p_new_id = new JsonObject();
        p_new_id.addProperty("type", "string");
        p_new_id.addProperty("description", "(duplicate/move) New block ID.");
        props.add("new_id", p_new_id);
        JsonObject p_param_name = new JsonObject();
        p_param_name.addProperty("type", "string");
        p_param_name.addProperty("description", "(set_parameter) Parameter name.");
        props.add("param_name", p_param_name);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(set_parameter/set_java_method) Value to set.");
        props.add("value", p_value);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(save_to_collection/import_from_collection) File system path.");
        props.add("file_path", p_file_path);
        JsonObject p_new_index = new JsonObject();
        p_new_index.addProperty("type", "integer");
        p_new_index.addProperty("description", "(move) New index in the block list.");
        props.add("new_index", p_new_index);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "delete": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                if (eventId == null || blockId == null) return err("event_id and block_id required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "n", eventId, blockId);
                                    ctx.refreshLogicEditor();
                                    return ok("Deleted block '" + blockId + "' from event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "duplicate": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                String newId = optString(args, "new_id", blockId + "_copy");
                                if (eventId == null || blockId == null) return err("event_id and block_id required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "o", eventId, blockId, newId);
                                    ctx.refreshLogicEditor();
                                    return ok("Duplicated block '" + blockId + "' → '" + newId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import_from_collection": {
                String eventId = optString(args, "event_id");
                                String path = optString(args, "file_path");
                                if (eventId == null || path == null) return err("event_id and file_path required");
                                try {
                                    String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "p", eventId, content);
                                    ctx.refreshLogicEditor();
                                    return ok("Imported block into event '" + eventId + "' from " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "move": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                int newIndex = optInt(args, "new_index", -1);
                                if (eventId == null || blockId == null) return err("event_id and block_id required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "q", eventId, blockId, newIndex);
                                    ctx.refreshLogicEditor();
                                    return ok("Moved block '" + blockId + "' to index " + newIndex + " in event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "redo": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "r", eventId);
                                    ctx.refreshLogicEditor();
                                    return ok("Redo on event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "save": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "s", eventId);
                                    return ok("Saved blocks of event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "save_to_collection": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                String path = optString(args, "file_path");
                                if (eventId == null || blockId == null || path == null) return err("event_id, block_id, file_path required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object def = SketchwareApi.invoke(editor, "t", eventId, blockId);
                                    java.nio.file.Files.write(java.nio.file.Paths.get(path),
                                            java.util.Collections.singletonList(String.valueOf(def)));
                                    return ok("Saved block '" + blockId + "' to collection at " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_java_method": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                String value = optString(args, "value");
                                if (eventId == null || blockId == null || value == null) return err("event_id, block_id, value required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "u", eventId, blockId, value);
                                    ctx.refreshLogicEditor();
                                    return ok("Set java method of block '" + blockId + "' to '" + value + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_parameter": {
                String eventId = optString(args, "event_id");
                                String blockId = optString(args, "block_id");
                                String paramName = optString(args, "param_name");
                                String value = optString(args, "value");
                                if (eventId == null || blockId == null || paramName == null || value == null)
                                    return err("event_id, block_id, param_name, value required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "v", eventId, blockId, paramName, value);
                                    ctx.refreshLogicEditor();
                                    return ok("Set parameter '" + paramName + "' = '" + value + "' on block '" + blockId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "undo": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "w", eventId);
                                    ctx.refreshLogicEditor();
                                    return ok("Undo on event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
