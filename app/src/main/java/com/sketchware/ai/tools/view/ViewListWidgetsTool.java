package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_list_widgets - read-only list of all widgets in the current layout.
 *
 * <p>CRITICAL: Sketchware's {@code eC} stores ViewBeans in a HashMap keyed
 * by the <b>full XML name</b> (e.g. {@code "main.xml"}). If the context's
 * currentJavaName lacks the {@code .xml} suffix (a regression we fixed in
 * {@code SketchwareToolContext.setCurrentJavaName}), the lookup would
 * silently return an empty list — the AI would think there are 0 widgets
 * even after adding 20. This tool now normalises the name to always end
 * with {@code .xml} before calling {@code eC.d(name)}.
 */
public final class ViewListWidgetsTool implements SketchwareTool {

    @Override public String name() { return "view_list_widgets"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "List all widgets in the current layout file. Returns widget ID, type, parent, "
                + "and layout dimensions. Also shows the root container info. Use this to verify "
                + "what widgets exist before calling view_set_property or view_add_widget.";
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
        // Normalise to .xml-suffixed name (eC requires it).
        String xmlName = javaName.endsWith(".xml") ? javaName : javaName + ".xml";
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object widgets = SketchwareApi.invoke(eC, "d", xmlName);
            StringBuilder sb = new StringBuilder();
            sb.append("Layout: ").append(xmlName).append("\n");
            // Also fetch the root ViewBean (the layout's root container).
            // eC.h(xmlName) returns the root ViewBean — useful for the AI
            // to know what type the root is (LinearLayout, etc.) so it can
            // set orientation / gravity on it via view_set_property with
            // widget_id="root".
            try {
                Object root = SketchwareApi.invoke(eC, "h", xmlName);
                if (root != null) {
                    Object rootType = getField(root, "type");
                    Object rootLayout = getField(root, "layout");
                    sb.append("Root: type=").append(rootType);
                    if (rootLayout != null) {
                        Object w = getField(rootLayout, "width");
                        Object h = getField(rootLayout, "height");
                        Object orient = getField(rootLayout, "orientation");
                        sb.append(" size=").append(w).append("x").append(h)
                          .append(" orientation=").append(orient);
                    }
                    sb.append("\n");
                }
            } catch (Throwable ignored) {}

            if (widgets instanceof List) {
                List<?> list = (List<?>) widgets;
                sb.append("Widgets (").append(list.size()).append("):\n");
                int rootCount = 0;
                for (Object b : list) {
                    String id = str(getField(b, "id"));
                    Object type = getField(b, "type");
                    Object parent = getField(b, "parent");
                    Object layout = getField(b, "layout");
                    String parentStr = parent == null ? "(null)" : parent.toString();
                    boolean isRoot = "root".equals(parentStr);
                    if (isRoot) rootCount++;
                    sb.append("- id=").append(id);
                    sb.append(" type=").append(type);
                    sb.append(" parent=").append(parentStr);
                    sb.append(isRoot ? " [root-level]" : " [nested]");
                    if (layout != null) {
                        Object w = getField(layout, "width");
                        Object h = getField(layout, "height");
                        sb.append(" size=").append(w).append("x").append(h);
                    }
                    sb.append("\n");
                }
                sb.append("Summary: ").append(list.size()).append(" total, ")
                  .append(rootCount).append(" root-level (visible on canvas), ")
                  .append(list.size() - rootCount).append(" nested.\n");
            } else {
                sb.append("Widgets: (unable to enumerate)\n");
            }
            return ToolResult.success(sb.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }

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
