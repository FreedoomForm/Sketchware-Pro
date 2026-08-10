package com.sketchware.ai.tools.event;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * event_list - read-only list of events attached to the current Java file.
 */
public final class EventListTool implements SketchwareTool {

    @Override public String name() { return "event_list"; }
    @Override public String category() { return "event"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "List all events attached to the current Java file.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object events = SketchwareApi.invoke(eC, "g", javaName);
            StringBuilder sb = new StringBuilder();
            sb.append("Events for ").append(javaName).append(":");
            if (events instanceof List) {
                List<?> list = (List<?>) events;
                sb.append(" (").append(list.size()).append(")\n");
                for (Object e : list) {
                    Object eventName = getFieldValue(e, "eventName");
                    Object targetId = getFieldValue(e, "targetId");
                    Object eventType = getFieldValue(e, "eventType");
                    String typeName;
                    if (eventType instanceof Integer) {
                        switch ((Integer) eventType) {
                            case 1: typeName = "VIEW"; break;
                            case 2: typeName = "COMPONENT"; break;
                            case 3: typeName = "ACTIVITY"; break;
                            case 4: typeName = "DRAWER"; break;
                            default: typeName = "ETC"; break;
                        }
                    } else typeName = "?";
                    sb.append("- [").append(typeName).append("] ");
                    if (targetId != null && !targetId.toString().isEmpty()) sb.append(targetId).append(".");
                    sb.append(eventName).append("\n");
                }
            }
            return ToolResult.success(sb.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private Object getFieldValue(Object obj, String name) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(name); }
            catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) { return null; }
    }
}
