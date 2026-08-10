package com.sketchware.ai.tools.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_list_widgets - read-only list of all widgets in the current layout.
 */
public final class ViewListWidgetsTool implements SketchwareTool {

    @Override public String name() { return "view_list_widgets"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "List all widgets in the current layout file. Returns widget ID, type, parent.";
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
            Object widgets = SketchwareApi.invoke(eC, "d", javaName);
            StringBuilder sb = new StringBuilder();
            sb.append("Layout: ").append(javaName).append("\n");
            if (widgets instanceof List) {
                List<?> list = (List<?>) widgets;
                sb.append("Widgets (").append(list.size()).append("):\n");
                for (Object b : list) {
                    sb.append("- id=").append(getField(b, "id"));
                    sb.append(" type=").append(getField(b, "type"));
                    Object parent = getField(b, "parent");
                    if (parent != null && !parent.toString().isEmpty()) sb.append(" parent=").append(parent);
                    sb.append("\n");
                }
            } else {
                sb.append("Widgets: (unable to enumerate)\n");
            }
            return ToolResult.success(sb.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private Object getField(Object obj, String name) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(name); }
            catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) { return null; }
    }
}
