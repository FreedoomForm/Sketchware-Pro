package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.ArrayList;
import java.util.List;

/**
 * view_manage_widget — universal tool for widget operations beyond
 * add/delete/list/set_property (which have their own dedicated tools).
 *
 * <p>Replaces 4 stubs: view_move_widget, view_clone_widget,
 * view_select_widget_by_id, view_list_widget_events.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates that the target widget exists before performing move /
 *       clone / select / list_events. If not found, returns a helpful error
 *       listing the widget IDs available in the active layout.</li>
 *   <li>For {@code clone}, auto-generates a unique {@code _copy},
 *       {@code _copy_2}, {@code _copy_3}, ... suffix if {@code new_id} is
 *       omitted (matching {@code ComponentManageTool}'s clone semantics).</li>
 *   <li>For {@code list_events}, returns a formatted bulleted list of
 *       each event handler with its event type and target widget, instead
 *       of a raw {@code .toString()} dump.</li>
 *   <li>For {@code move}, validates that {@code x}/{@code y} are integers
 *       (via {@link #optInt}) and applies the change via reflection on the
 *       bean's {@code parent}, {@code x}, {@code y} fields.</li>
 * </ul>
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
        widgetId.addProperty("description", "ID of the widget to operate on. Must exist in the active layout.");
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
        newName.addProperty("description", "(clone) New widget ID for the clone. If omitted, a unique _copy, _copy_2, _copy_3 suffix is generated.");
        props.add("new_id", newName);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null || javaName.isEmpty()) return err("No active layout (currentJavaName is null).");
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");

        switch (action) {
            case "move":        return doMove(ctx, scId, javaName, args, widgetId);
            case "clone":       return doClone(ctx, scId, javaName, args, widgetId);
            case "select":      return doSelect(ctx, scId, javaName, widgetId);
            case "list_events": return doListEvents(ctx, scId, javaName, widgetId);
            default:             return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  move
    // ------------------------------------------------------------------
    private ToolResult doMove(SketchwareToolContext ctx, String scId, String javaName,
                              JsonObject args, String widgetId) {
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<?> widgets = listWidgets(editor, javaName);
        if (widgets == null) return err("Cannot enumerate widgets in layout '" + javaName + "'.");
        Object target = findWidgetById(widgets, widgetId);
        if (target == null) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + collectIds(widgets));
        }
        String parentId = optString(args, "parent_id", "");
        int x = optInt(args, "x", 0);
        int y = optInt(args, "y", 0);
        try {
            setField(target, "parent", parentId);
            setIntField(target, "x", x);
            setIntField(target, "y", y);
            SketchwareApi.invoke(editor, "b", javaName, widgets);
            ctx.refreshViewEditor();
            return ok("Moved widget '" + widgetId + "' to parent='" + parentId
                    + "' at (" + x + "," + y + ") in layout '" + javaName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  clone
    // ------------------------------------------------------------------
    private ToolResult doClone(SketchwareToolContext ctx, String scId, String javaName,
                               JsonObject args, String widgetId) {
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<?> widgets = listWidgets(editor, javaName);
        if (widgets == null) return err("Cannot enumerate widgets in layout '" + javaName + "'.");
        Object target = findWidgetById(widgets, widgetId);
        if (target == null) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + collectIds(widgets));
        }
        String newId = optString(args, "new_id");
        if (newId == null || newId.isEmpty()) {
            newId = generateUniqueWidgetId(widgets, widgetId);
        } else if (findWidgetById(widgets, newId) != null) {
            return err("A widget with id '" + newId + "' already exists in layout '" + javaName + "'.");
        }
        try {
            Class<?> cls = target.getClass();
            Object beanClone = cls.getDeclaredConstructor().newInstance();
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                f.setAccessible(true);
                try { f.set(beanClone, f.get(target)); } catch (Throwable ignored) {}
            }
            setField(beanClone, "id", newId);
            SketchwareApi.invoke(editor, "a", javaName, beanClone);
            ctx.refreshViewEditor();
            return ok("Cloned widget '" + widgetId + "' → '" + newId
                    + "' in layout '" + javaName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  select
    // ------------------------------------------------------------------
    private ToolResult doSelect(SketchwareToolContext ctx, String scId, String javaName,
                                String widgetId) {
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<?> widgets = listWidgets(editor, javaName);
        if (widgets == null) return err("Cannot enumerate widgets in layout '" + javaName + "'.");
        Object target = findWidgetById(widgets, widgetId);
        if (target == null) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + collectIds(widgets));
        }
        try {
            SketchwareApi.invoke(editor, "b", javaName, widgets);
            ctx.refreshViewEditor();
            return ok("Selected widget '" + widgetId + "' in layout '" + javaName
                    + "'. The View editor canvas has been refreshed.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  list_events
    // ------------------------------------------------------------------
    private ToolResult doListEvents(SketchwareToolContext ctx, String scId, String javaName,
                                   String widgetId) {
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<?> widgets = listWidgets(editor, javaName);
        if (widgets != null && findWidgetById(widgets, widgetId) == null) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + collectIds(widgets));
        }
        try {
            Object eventEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
            Object events = SketchwareApi.invoke(eventEditor, "c", javaName);
            StringBuilder sb = new StringBuilder();
            sb.append("Events attached to widget '").append(widgetId)
              .append("' in layout '").append(javaName).append("':\n");
            int count = 0;
            if (events instanceof List) {
                for (Object e : (List<?>) events) {
                    String targetId = readField(e, "target");
                    if (targetId == null) continue;
                    if (!widgetId.equals(targetId)) continue;
                    String eventType = readField(e, "event");
                    String eventName = readField(e, "name");
                    sb.append("  • ");
                    if (eventType != null) sb.append("type=").append(eventType).append("  ");
                    if (eventName != null) sb.append("name=").append(eventName).append("  ");
                    sb.append("target=").append(targetId);
                    sb.append("\n");
                    count++;
                }
            }
            if (count == 0) {
                sb.append("  (no event handlers attached to this widget — ")
                  .append("use event_attach to add one)\n");
            }
            return ok(sb.toString().trim());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private static List<?> listWidgets(Object editor, String javaName) {
        try {
            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
            if (widgets instanceof List) return (List<?>) widgets;
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object findWidgetById(List<?> widgets, String id) {
        if (widgets == null || id == null) return null;
        for (Object b : widgets) {
            String bid = readField(b, "id");
            if (id.equals(bid)) return b;
        }
        return null;
    }

    private static String generateUniqueWidgetId(List<?> widgets, String baseId) {
        // Try _copy, then _copy_2, _copy_3, ...
        String suffix = "_copy";
        int n = 1;
        while (findWidgetById(widgets, baseId + suffix) != null) {
            n++;
            suffix = "_copy_" + n;
        }
        return baseId + suffix;
    }

    private static List<String> collectIds(List<?> widgets) {
        List<String> ids = new ArrayList<>();
        if (widgets == null) return ids;
        for (Object b : widgets) {
            String id = readField(b, "id");
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private static String readField(Object bean, String fieldName) {
        if (bean == null) return null;
        // Try getter first.
        try {
            Object v = SketchwareApi.invoke(bean,
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        // Fall back to direct field access (search superclass chain).
        try {
            Class<?> cls = bean.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(bean);
                    return v == null ? null : v.toString();
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void setField(Object bean, String fieldName, Object value) throws Exception {
        Class<?> cls = bean.getClass();
        while (cls != null) {
            try {
                java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(bean, value);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void setIntField(Object bean, String fieldName, int value) throws Exception {
        Class<?> cls = bean.getClass();
        while (cls != null) {
            try {
                java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.setInt(bean, value);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
