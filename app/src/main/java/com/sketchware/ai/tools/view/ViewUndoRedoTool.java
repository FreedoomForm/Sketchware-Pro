package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * view_undo / view_redo - undo or redo the last action in the View editor.
 *
 * <p><b>FIX-A-VIEW</b>: this previously returned a confirmation string
 * without actually calling any Sketchware API. It now invokes the real
 * undo/redo API:
 * <ul>
 *   <li>{@code cC.c(scId).i(xmlName)} — undo (returns the
 *       {@code HistoryViewBean} that was reverted, or null if the undo
 *       stack was empty).</li>
 *   <li>{@code cC.c(scId).h(xmlName)} — redo (returns the
 *       {@code HistoryViewBean} that was re-applied, or null if the redo
 *       stack was empty).</li>
 * </ul>
 *
 * <p>These are exactly the calls that {@code ViewEditorFragment.onUndo()}
 * and {@code onRedo()} make ({@code a.a.a.ViewEditorFragment} lines 316/382).
 * The {@code xmlName} is taken from {@link SketchwareToolContext#getCurrentJavaName()}
 * — the active layout/file being edited.
 */
public final class ViewUndoRedoTool implements SketchwareTool {

    private final boolean undo;

    public ViewUndoRedoTool(boolean undo) { this.undo = undo; }

    @Override public String name() { return undo ? "view_undo" : "view_redo"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return undo
                ? "Undo the last action in the View editor (add/delete/move/property change). "
                        + "Calls cC.c(scId).i(xmlName)."
                : "Redo the previously undone action in the View editor. "
                        + "Calls cC.c(scId).h(xmlName).";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String scId = ctx.getScId();
        if (scId == null) {
            return ToolResult.error("[" + name() + "] No active project (sc_id is null).");
        }
        String xmlName = ctx.getCurrentJavaName();
        if (xmlName == null || xmlName.isEmpty()) {
            return ToolResult.error("[" + name() + "] No active layout. "
                    + "Open a layout in the View editor before calling "
                    + name() + ".");
        }
        // cC.c(scId) returns the per-project undo/redo history singleton.
        Object history;
        try {
            history = SketchwareApi.invokeStatic("a.a.a.cC", "c", scId);
        } catch (Throwable t) {
            return ToolResult.error("[" + name() + "] Failed to obtain cC history singleton: "
                    + t.getMessage());
        }
        if (history == null) {
            return ToolResult.error("[" + name() + "] cC.c(scId) returned null.");
        }

        // Best-effort pre-check: cC.c(scId).g(xmlName) reports whether an undo
        // is available; .f(xmlName) reports whether a redo is available. If
        // these return false, we return a friendly error rather than calling
        // the actual undo/redo (which would just return null).
        String checkMethod = undo ? "g" : "f";
        String opMethod    = undo ? "i" : "h";
        String opLabel     = undo ? "undo" : "redo";
        try {
            Object canDo = SketchwareApi.invoke(history, checkMethod, xmlName);
            if (Boolean.FALSE.equals(canDo)) {
                return ToolResult.error("[" + name() + "] No " + opLabel
                        + " available for layout '" + xmlName + "'.");
            }
        } catch (Throwable ignored) {
            // Pre-check is best-effort: continue and call the actual op.
        }

        try {
            Object historyBean = SketchwareApi.invoke(history, opMethod, xmlName);
            if (historyBean == null) {
                return ToolResult.error("[" + name() + "] " + opLabel
                        + " returned no history entry for layout '" + xmlName
                        + "' — the " + opLabel + " stack may be empty.");
            }
            // Refresh the View editor so the change is visible.
            ctx.refreshViewEditor();
            String actionType = readIntField(historyBean, "actionType");
            return ToolResult.success("[" + name() + "] " + opLabel
                    + " applied to layout '" + xmlName + "'"
                    + (actionType == null ? "" : " (actionType=" + actionType + ")")
                    + ". The View editor has been refreshed.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Read an int field reflectively (best-effort). */
    private static String readIntField(Object bean, String fieldName) {
        if (bean == null) return null;
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
}
