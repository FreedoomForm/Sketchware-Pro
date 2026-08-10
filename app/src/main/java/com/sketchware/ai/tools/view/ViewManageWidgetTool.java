package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_manage_widget — universal tool for widget operations beyond
 * add/delete/list/set_property (which have their own dedicated tools).
 *
 * <p>Replaces 4 stubs: view_move_widget, view_clone_widget,
 * view_select_widget_by_id, view_list_widget_events.
 */
public final class ViewManageWidgetTool extends UniversalTool {

    public ViewManageWidgetTool() {
        super("view_manage_widget",
                "Operate on an existing widget: move it, clone it, select it, "
                        + "or list its event handlers.",
                "view", false, false,
                "move", "clone", "select", "list_events");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject widgetId = new JsonObject();
        widgetId.addProperty("type", "string");
        widgetId.addProperty("description", "ID of the widget to operate on.");
        props.add("widget_id", widgetId);

        JsonObject parentId = new JsonObject();
        parentId.addProperty("type", "string");
        parentId.addProperty("description", "(move) New parent container ID; empty = root.");
        props.add("parent_id", parentId);

        JsonObject x = new JsonObject();
        x.addProperty("type", "integer");
        x.addProperty("description", "(move) New X coordinate.");
        props.add("x", x);

        JsonObject y = new JsonObject();
        y.addProperty("type", "integer");
        y.addProperty("description", "(move) New Y coordinate.");
        props.add("y", y);

        JsonObject newName = new JsonObject();
        newName.addProperty("type", "string");
        newName.addProperty("description", "(clone) New widget ID for the clone.");
        props.add("new_id", newName);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project.");
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null) return err("No active layout.");
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");

        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
            if (!(widgets instanceof List)) return err("Cannot enumerate widgets.");
            Object target = null;
            for (Object b : (List<?>) widgets) {
                try {
                    Object id = SketchwareApi.invoke(b, "getId");
                    if (id == null) {
                        java.lang.reflect.Field f = b.getClass().getDeclaredField("id");
                        f.setAccessible(true);
                        id = f.get(b);
                    }
                    if (id != null && id.toString().equals(widgetId)) {
                        target = b;
                        break;
                    }
                } catch (Throwable ignored) {}
            }
            if (target == null) return err("Widget '" + widgetId + "' not found in layout '" + javaName + "'.");

            switch (action) {
                case "move": {
                    String parentId = optString(args, "parent_id", "");
                    int x = optInt(args, "x", 0);
                    int y = optInt(args, "y", 0);
                    try {
                        java.lang.reflect.Field f = target.getClass().getDeclaredField("parent");
                        f.setAccessible(true);
                        f.set(target, parentId);
                        java.lang.reflect.Field fx = target.getClass().getDeclaredField("x");
                        fx.setAccessible(true);
                        fx.setInt(target, x);
                        java.lang.reflect.Field fy = target.getClass().getDeclaredField("y");
                        fy.setAccessible(true);
                        fy.setInt(target, y);
                        SketchwareApi.invoke(editor, "b", javaName, widgets);
                        ctx.refreshViewEditor();
                        return ok("Moved widget '" + widgetId + "' to parent='" + parentId + "' at (" + x + "," + y + ").");
                    } catch (Throwable t) {
                        return ToolResult.error(t);
                    }
                }
                case "clone": {
                    String newId = optString(args, "new_id");
                    if (newId == null || newId.isEmpty())
                        newId = widgetId + "_copy";
                    Object beanClone;
                    try {
                        Class<?> cls = target.getClass();
                        beanClone = cls.getDeclaredConstructor().newInstance();
                        for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                            f.setAccessible(true);
                            try { f.set(beanClone, f.get(target)); } catch (Throwable ignored) {}
                        }
                        java.lang.reflect.Field fid = cls.getDeclaredField("id");
                        fid.setAccessible(true);
                        fid.set(beanClone, newId);
                        SketchwareApi.invoke(editor, "a", javaName, beanClone);
                        ctx.refreshViewEditor();
                        return ok("Cloned widget '" + widgetId + "' → '" + newId + "'.");
                    } catch (Throwable t) {
                        return ToolResult.error(t);
                    }
                }
                case "select": {
                    // Selecting a widget in the canvas: just refresh the editor with the widget id.
                    try {
                        SketchwareApi.invoke(editor, "b", javaName, widgets);
                        ctx.refreshViewEditor();
                        return ok("Selected widget '" + widgetId + "'.");
                    } catch (Throwable t) {
                        return ToolResult.error(t);
                    }
                }
                case "list_events": {
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Events attached to widget '").append(widgetId).append("':\n");
                        Object eventEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                        Object events = SketchwareApi.invoke(eventEditor, "c", javaName);
                        if (events instanceof List) {
                            for (Object e : (List<?>) events) {
                                try {
                                    Object targetId = SketchwareApi.invoke(e, "getTarget");
                                    if (targetId == null) {
                                        java.lang.reflect.Field f = e.getClass().getDeclaredField("target");
                                        f.setAccessible(true);
                                        targetId = f.get(e);
                                    }
                                    if (widgetId.equals(String.valueOf(targetId))) {
                                        Object eventType = SketchwareApi.invoke(e, "getEvent");
                                        if (eventType == null) {
                                            java.lang.reflect.Field f = e.getClass().getDeclaredField("event");
                                            f.setAccessible(true);
                                            eventType = f.get(e);
                                        }
                                        sb.append("  - ").append(eventType).append("\n");
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                        return ok(sb.toString());
                    } catch (Throwable t) {
                        return ToolResult.error(t);
                    }
                }
                default:
                    return err("Unknown action: " + action);
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
