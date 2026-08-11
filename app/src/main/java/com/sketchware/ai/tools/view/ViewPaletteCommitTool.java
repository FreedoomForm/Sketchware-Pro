package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_palette_commit — tool for committing pending property changes in
 * the View editor. Mutating operation; NOT auto-approved.
 *
 * <p><b>FIX-A-VIEW</b>: this was split out of {@link ViewPaletteActionTool}
 * because the previous combined tool was marked {@code isReadOnly() = true}
 * and {@code isAutoApprovedByDefault() = true}, but the
 * {@code commit_property_changes} action mutates pending property edits —
 * a safety bug flagged in {@code COVERAGE_REPORT.md} §4.3. By moving the
 * mutating action into its own tool, the {@link ViewPaletteActionTool}
 * can remain read-only and auto-approved, while this tool requires
 * explicit user approval (in ACT mode).
 *
 * <p>The single action {@code commit_property_changes} calls
 * {@code jC.a(scId).l()} and returns the change count if extractable.
 */
public final class ViewPaletteCommitTool extends UniversalTool {

    public ViewPaletteCommitTool() {
        super("view_palette_commit",
                "Commit pending property changes in the View editor. "
                        + "This is a mutating operation that persists in-progress property "
                        + "edits to the project; it requires user approval in ACT mode. "
                        + "Returns the change count if extractable.",
                "view", /* readOnly */ false, /* autoApproved */ false,
                "commit_property_changes");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        // No extra properties — the action enum is mandatory and sufficient.
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");
        switch (action) {
            case "commit_property_changes": return doCommitPropertyChanges(ctx, scId);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  commit_property_changes
    // ------------------------------------------------------------------
    private ToolResult doCommitPropertyChanges(SketchwareToolContext ctx, String scId) {
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, "l");
            ctx.refreshViewEditor();
            // Best-effort: try to extract a count of changed properties from the return value.
            int changedCount = -1;
            if (result != null) {
                changedCount = extractChangeCount(result);
            }
            if (changedCount >= 0) {
                return ok("Committed " + changedCount + " property change(s) to project '"
                        + scId + "'. The View editor has been refreshed.");
            }
            return ok("Committed pending property changes to project '" + scId + "'. "
                    + "The View editor has been refreshed and changes are now persisted.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /**
     * Best-effort: extract an integer "change count" from the return value of
     * the {@code l} method. Sketchware's internals may return a Boolean,
     * Integer, List, or void. Returns -1 if no count could be extracted.
     */
    private static int extractChangeCount(Object result) {
        if (result == null) return -1;
        if (result instanceof Integer) return (Integer) result;
        if (result instanceof Number) return ((Number) result).intValue();
        if (result instanceof Boolean) return ((Boolean) result) ? 1 : 0;
        if (result instanceof List) return ((List<?>) result).size();
        // Some beans expose a "count" or "size" field — try reflection.
        String s = readField(result, "count");
        if (s == null) s = readField(result, "size");
        if (s != null) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private static String readField(Object bean, String fieldName) {
        if (bean == null) return null;
        try {
            Object v = SketchwareApi.invoke(bean,
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
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
