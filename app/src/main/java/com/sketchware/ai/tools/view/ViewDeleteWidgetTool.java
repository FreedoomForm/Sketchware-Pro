package com.sketchware.ai.tools.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_delete_widget - delete a widget from the canvas via reflection.
 */
public final class ViewDeleteWidgetTool implements SketchwareTool {

    @Override public String name() { return "view_delete_widget"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Delete a widget (and its children) from the current layout.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject widgetId = new JsonObject();
        widgetId.addProperty("type", "string");
        props.add("widget_id", widgetId);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("widget_id");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String widgetId = args.has("widget_id") ? args.get("widget_id").getAsString() : null;
        if (widgetId == null) return ToolResult.error("widget_id is required");
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object widgets = SketchwareApi.invoke(eC, "d", javaName);
            Object target = null;
            if (widgets instanceof List) {
                for (Object b : (List<?>) widgets) {
                    Object id = getFieldValue(b, "id");
                    if (id != null && widgetId.equals(id.toString())) { target = b; break; }
                }
            }
            if (target == null) return ToolResult.error("Widget '" + widgetId + "' not found.");
            SketchwareApi.invoke(eC, "b", javaName, target);
            return ToolResult.success("Deleted widget '" + widgetId + "'.");
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
