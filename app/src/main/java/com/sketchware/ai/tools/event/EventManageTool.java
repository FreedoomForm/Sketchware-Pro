package com.sketchware.ai.tools.event;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * event_manage — universal tool for event operations.
 *
 * <p>Replaces 10 stubs: event_manage:delete, event_manage:duplicate, event_manage:list_available, event_manage:open_in_logic_editor, event_manage:reset_blocks, event_manage:search, event_manage:set_activity_event, event_manage:set_drawer_event, event_manage:set_target, event_manage:sort
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class EventManageTool extends UniversalTool {

    public EventManageTool() {
        super("event_manage",
                "Manage event handlers in the current project: delete, duplicate, list available event types, open in logic editor, reset blocks, search handlers, set activity-level event, set drawer event, set target, or sort.",
                "event", false, false,
"delete",
                "duplicate",
                "list_available",
                "open_in_logic_editor",
                "reset_blocks",
                "search",
                "set_activity_event",
                "set_drawer_event",
                "set_target",
                "sort");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "ID of the event handler (for delete/duplicate/open/reset/set_target).");
        props.add("event_id", p_event_id);
        JsonObject p_target_id = new JsonObject();
        p_target_id.addProperty("type", "string");
        p_target_id.addProperty("description", "Widget ID to attach event to (for set_target).");
        props.add("target_id", p_target_id);
        JsonObject p_event_name = new JsonObject();
        p_event_name.addProperty("type", "string");
        p_event_name.addProperty("description", "Event type name (for set_activity_event/set_drawer_event/search).");
        props.add("event_name", p_event_name);
        JsonObject p_drawer_id = new JsonObject();
        p_drawer_id.addProperty("type", "string");
        p_drawer_id.addProperty("description", "Drawer ID (for set_drawer_event).");
        props.add("drawer_id", p_drawer_id);
        JsonObject p_activity_event = new JsonObject();
        p_activity_event.addProperty("type", "string");
        p_activity_event.addProperty("description", "Activity lifecycle event name (onCreate, onStart, onResume, onPause, onStop, onDestroy).");
        props.add("activity_event", p_activity_event);
        JsonObject p_query = new JsonObject();
        p_query.addProperty("type", "string");
        p_query.addProperty("description", "(search) Search query.");
        props.add("query", p_query);
        JsonObject p_new_id = new JsonObject();
        p_new_id.addProperty("type", "string");
        p_new_id.addProperty("description", "(duplicate) New event handler ID.");
        props.add("new_id", p_new_id);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "delete": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "d", eventId);
                                    ctx.refreshEventList();
                                    return ok("Deleted event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "duplicate": {
                String eventId = optString(args, "event_id");
                                String newId = optString(args, "new_id", eventId + "_copy");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "e", eventId, newId);
                                    ctx.refreshEventList();
                                    return ok("Duplicated event '" + eventId + "' → '" + newId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "list_available": {
                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object events = SketchwareApi.invoke(editor, "f");
                                    return ok("Available event types: " + events);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "open_in_logic_editor": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "g", eventId);
                                    ctx.refreshLogicEditor();
                                    return ok("Opened event '" + eventId + "' in logic editor.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "reset_blocks": {
                String eventId = optString(args, "event_id");
                                if (eventId == null) return err("event_id is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "h", eventId);
                                    ctx.refreshLogicEditor();
                                    return ok("Reset blocks of event '" + eventId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "search": {
                String q = optString(args, "query", "");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object results = SketchwareApi.invoke(editor, "i", q);
                                    return ok("Search results for '" + q + "': " + results);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_activity_event": {
                String activityEvent = optString(args, "activity_event");
                                if (activityEvent == null) return err("activity_event is required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "j", activityEvent);
                                    ctx.refreshEventList();
                                    return ok("Set activity event '" + activityEvent + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_drawer_event": {
                String drawerId = optString(args, "drawer_id");
                                String eventName = optString(args, "event_name");
                                if (drawerId == null || eventName == null) return err("drawer_id and event_name required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "k", drawerId, eventName);
                                    ctx.refreshEventList();
                                    return ok("Set drawer event '" + eventName + "' for drawer '" + drawerId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_target": {
                String eventId = optString(args, "event_id");
                                String targetId = optString(args, "target_id");
                                if (eventId == null || targetId == null) return err("event_id and target_id required");
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "l", eventId, targetId);
                                    ctx.refreshEventList();
                                    return ok("Set target of event '" + eventId + "' to '" + targetId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "sort": {
                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(editor, "m");
                                    ctx.refreshEventList();
                                    return ok("Sorted events.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
