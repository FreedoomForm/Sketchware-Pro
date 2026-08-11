package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * view_palette_action — universal tool for <b>read-only</b> operations on
 * the View editor palette/UI: switch the palette group, switch the
 * property group, or open the property editor for a widget.
 *
 * <p>Replaces 3 stubs: view_palette_action:{switch_palette_group,
 * switch_property_group, open_property_editor}.
 *
 * <p><b>FIX-A-VIEW</b>: this tool was previously a 4-action tool that
 * included {@code commit_property_changes} (a mutating action). Because
 * the entire tool was marked {@code isReadOnly() = true} and
 * {@code isAutoApprovedByDefault() = true}, the {@code commit_property_changes}
 * action was being auto-approved without user confirmation — a safety bug
 * flagged in {@code COVERAGE_REPORT.md} §4.3. The mutating action has been
 * split out into a separate {@link ViewPaletteCommitTool} which is neither
 * read-only nor auto-approved.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates {@code group} against the supported set
 *       {@code {basic, layout, media, advanced, widget, custom}} — returns
 *       a helpful error listing the supported values if invalid.</li>
 *   <li>For {@code open_property_editor}: verifies the widget exists in
 *       the active layout before opening (returns the available widget
 *       IDs if not found).</li>
 * </ul>
 */
public final class ViewPaletteActionTool extends UniversalTool {

    /** Supported palette/property group names. */
    private static final Set<String> SUPPORTED_GROUPS = new HashSet<>(Arrays.asList(
            "basic", "layout", "media", "advanced", "widget", "custom"
    ));

    public ViewPaletteActionTool() {
        super("view_palette_action",
                "Read-only operations on the View editor palette/UI: switch palette group, "
                        + "switch property group, or open the property editor for a widget. "
                        + "Group must be one of: basic, layout, media, advanced, widget, custom. "
                        + "For committing pending property changes, use the separate "
                        + "view_palette_commit tool (which requires user approval).",
                "view", /* readOnly */ true, /* autoApproved */ true,
                "switch_palette_group", "switch_property_group", "open_property_editor");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pGroup = new JsonObject();
        pGroup.addProperty("type", "string");
        pGroup.addProperty("description",
                "(switch_palette_group / switch_property_group) Group name. "
                        + "Must be one of: basic, layout, media, advanced, widget, custom. "
                        + "Default: basic.");
        props.add("group", pGroup);

        JsonObject pWidgetId = new JsonObject();
        pWidgetId.addProperty("type", "string");
        pWidgetId.addProperty("description",
                "(open_property_editor) ID of widget whose properties to edit. "
                        + "Must exist in the active layout.");
        props.add("widget_id", pWidgetId);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "switch_palette_group":    return doSwitchPaletteGroup(ctx, scId, args);
            case "switch_property_group":   return doSwitchPropertyGroup(ctx, scId, args);
            case "open_property_editor":    return doOpenPropertyEditor(ctx, scId, args);
            default:                         return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  switch_palette_group
    // ------------------------------------------------------------------
    private ToolResult doSwitchPaletteGroup(SketchwareToolContext ctx, String scId, JsonObject args) {
        String group = optString(args, "group", "basic");
        if (!SUPPORTED_GROUPS.contains(group)) {
            return err("Unknown group '" + group + "'. Supported: " + SUPPORTED_GROUPS + ".");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "i", group);
            ctx.refreshViewEditor();
            return ok("Switched palette group to '" + group + "' in project '" + scId + "'. "
                    + "The View editor palette now displays the '" + group + "' category.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  switch_property_group
    // ------------------------------------------------------------------
    private ToolResult doSwitchPropertyGroup(SketchwareToolContext ctx, String scId, JsonObject args) {
        String group = optString(args, "group", "basic");
        if (!SUPPORTED_GROUPS.contains(group)) {
            return err("Unknown group '" + group + "'. Supported: " + SUPPORTED_GROUPS + ".");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "j", group);
            ctx.refreshViewEditor();
            return ok("Switched property group to '" + group + "' in project '" + scId + "'. "
                    + "The property panel now displays the '" + group + "' category.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  open_property_editor
    // ------------------------------------------------------------------
    private ToolResult doOpenPropertyEditor(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        // Verify widget exists in active layout.
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null || javaName.isEmpty()) {
            return err("No active layout. Open a layout in the View editor first.");
        }
        List<String> available = listWidgetIds(editor, javaName);
        if (!available.contains(widgetId)) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + available);
        }
        try {
            SketchwareApi.invoke(editor, "k", widgetId);
            ctx.refreshViewEditor();
            return ok("Opened property editor for widget '" + widgetId + "' in layout '"
                    + javaName + "'. The property panel is now showing this widget's properties.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private static List<String> listWidgetIds(Object editor, String javaName) {
        List<String> ids = new ArrayList<>();
        if (editor == null) return ids;
        try {
            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
            if (widgets instanceof List) {
                for (Object b : (List<?>) widgets) {
                    String id = readField(b, "id");
                    if (id != null) ids.add(id);
                }
            }
        } catch (Throwable ignored) {}
        return ids;
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
