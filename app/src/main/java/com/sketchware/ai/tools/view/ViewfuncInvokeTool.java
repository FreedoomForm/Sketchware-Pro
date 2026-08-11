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
import java.util.regex.Pattern;

/**
 * viewfunc_invoke — universal tool for invoking runtime "view function"
 * operations on a widget: set text, set image, set background color,
 * set text color, set visibility, get text, or animate.
 *
 * <p>Replaces 7 stubs: viewfunc_invoke:{set_text, set_image,
 * set_background_color, set_text_color, set_visibility, get_text, animate}.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Verifies the widget exists in the active layout before applying
 *       any operation (returns the available widget IDs if not found).</li>
 *   <li>For {@code set_background_color} / {@code set_text_color}: validates
 *       the color value matches {@code ^#?[0-9A-Fa-f]{6,8}$} (RGB or ARGB
 *       hex); prepends {@code #} if missing; rejects invalid formats with
 *       a helpful message.</li>
 *   <li>For {@code set_visibility}: validates the value is one of
 *       {@code {visible, invisible, gone}} (case-insensitive); maps to
 *       Android {@code View} constants {@code VISIBLE=0, INVISIBLE=4,
 *       GONE=8}.</li>
 *   <li>For {@code set_image}: validates that the image resource name
 *       exists in the project's image collection (best-effort via
 *       reflection on {@code jC.d(scId)}).</li>
 *   <li>For {@code animate}: validates {@code animation_type} is one of
 *       {@code {fade, slide, scale, rotate}}; validates {@code duration}
 *       is between 50 and 10000 ms (clamps if out of range and warns in
 *       the response).</li>
 *   <li>For {@code get_text}: returns "(no text)" if the widget doesn't
 *       exist or has no text, instead of "null".</li>
 * </ul>
 */
public final class ViewfuncInvokeTool extends UniversalTool {

    /** Color hex: optional '#' + 6 (RGB) or 8 (ARGB) hex digits. */
    private static final Pattern COLOR_HEX = Pattern.compile("^#?[0-9A-Fa-f]{6,8}$");

    /** Supported animation types. */
    private static final Set<String> ANIMATION_TYPES = new HashSet<>(Arrays.asList(
            "fade", "slide", "scale", "rotate"
    ));

    /** Supported visibility values (lowercased for comparison). */
    private static final Set<String> VISIBILITY_VALUES = new HashSet<>(Arrays.asList(
            "visible", "invisible", "gone"
    ));

    /** Android View visibility constants. */
    private static final int VISIBLE = 0;
    private static final int INVISIBLE = 4;
    private static final int GONE = 8;

    /** Animation duration bounds (in ms). */
    private static final int MIN_DURATION = 50;
    private static final int MAX_DURATION = 10_000;

    public ViewfuncInvokeTool() {
        super("viewfunc_invoke",
                "Invoke a runtime view function on a widget: set text, set image, "
                        + "set background color, set text color, set visibility, get text, "
                        + "or animate. Colors must be hex like '#FF0000' or '#FFAA0000' (with alpha). "
                        + "Visibility must be one of: visible, invisible, gone. "
                        + "Animation type must be one of: fade, slide, scale, rotate.",
                "view", false, false,
                "set_text", "set_image", "set_background_color", "set_text_color",
                "set_visibility", "get_text", "animate");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pWidgetId = new JsonObject();
        pWidgetId.addProperty("type", "string");
        pWidgetId.addProperty("description", "ID of the target widget. Must exist in the active layout.");
        props.add("widget_id", pWidgetId);

        JsonObject pValue = new JsonObject();
        pValue.addProperty("type", "string");
        pValue.addProperty("description",
                "Value to set. For set_text: any string. For set_image: image resource name "
                        + "(must exist in project). For set_background_color / set_text_color: "
                        + "hex color like '#FF0000' or '#FFAA0000' (with alpha). "
                        + "For set_visibility: visible | invisible | gone.");
        props.add("value", pValue);

        JsonObject pAnim = new JsonObject();
        pAnim.addProperty("type", "string");
        pAnim.addProperty("description",
                "(animate) Animation type: fade | slide | scale | rotate.");
        props.add("animation_type", pAnim);

        JsonObject pDur = new JsonObject();
        pDur.addProperty("type", "integer");
        pDur.addProperty("description",
                "(animate) Duration in ms. Must be between 50 and 10000. Default: 300.");
        props.add("duration", pDur);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "set_text":             return doSetText(ctx, scId, args);
            case "set_image":            return doSetImage(ctx, scId, args);
            case "set_background_color": return doSetColor(ctx, scId, args, "set_background_color", "setBackgroundColor");
            case "set_text_color":       return doSetColor(ctx, scId, args, "set_text_color", "setTextColor");
            case "set_visibility":       return doSetVisibility(ctx, scId, args);
            case "get_text":             return doGetText(ctx, scId, args);
            case "animate":              return doAnimate(ctx, scId, args);
            default:                      return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  set_text
    // ------------------------------------------------------------------
    private ToolResult doSetText(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value", "");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setText", widgetId, value);
            ctx.refreshViewEditor();
            return ok("set_text('" + widgetId + "', " + quote(value) + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_image
    // ------------------------------------------------------------------
    private ToolResult doSetImage(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value (image resource name) is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        // Best-effort: verify the image resource exists in the project.
        List<String> available = listImageResourceNames(scId);
        if (!available.isEmpty() && !available.contains(value)) {
            return err("Image resource '" + value + "' not found in project '" + scId
                    + "'. Available image resources: " + available);
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setImage", widgetId, value);
            ctx.refreshViewEditor();
            return ok("set_image('" + widgetId + "', '" + value + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_background_color / set_text_color
    // ------------------------------------------------------------------
    private ToolResult doSetColor(SketchwareToolContext ctx, String scId, JsonObject args,
                                  String actionLabel, String nativeMethod) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) {
            return err("value (color) is required for " + actionLabel + ". "
                    + "Expected a hex color like '#FF0000' or '#FFAA0000' (with alpha).");
        }
        if (!COLOR_HEX.matcher(value).matches()) {
            return err("Invalid color value '" + value + "'. "
                    + "Color must be a hex string like '#FF0000' (RGB) or '#FFAA0000' (with alpha), "
                    + "matching ^#?[0-9A-Fa-f]{6,8}$.");
        }
        // Prepend '#' if missing.
        String normalized = value.startsWith("#") ? value : "#" + value;
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, nativeMethod, widgetId, normalized);
            ctx.refreshViewEditor();
            return ok(actionLabel + "('" + widgetId + "', '" + normalized + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_visibility
    // ------------------------------------------------------------------
    private ToolResult doSetVisibility(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) {
            return err("value (visibility) is required. Use one of: visible, invisible, gone.");
        }
        String normalized = value.toLowerCase();
        if (!VISIBILITY_VALUES.contains(normalized)) {
            return err("Unknown visibility '" + value + "'. Use one of: visible, invisible, gone "
                    + "(case-insensitive).");
        }
        int androidVisibility;
        switch (normalized) {
            case "visible":   androidVisibility = VISIBLE;   break;
            case "invisible": androidVisibility = INVISIBLE; break;
            case "gone":      androidVisibility = GONE;      break;
            default:          androidVisibility = VISIBLE;   break; // unreachable
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setVisibility", widgetId, androidVisibility);
            ctx.refreshViewEditor();
            return ok("set_visibility('" + widgetId + "', '" + normalized + "' = "
                    + androidVisibility + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  get_text
    // ------------------------------------------------------------------
    private ToolResult doGetText(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, "getText", widgetId);
            String text;
            if (result == null) {
                text = "(no text)";
            } else {
                text = String.valueOf(result);
                if (text.isEmpty() || "null".equals(text)) {
                    text = "(no text)";
                }
            }
            return ok("get_text('" + widgetId + "') = " + quote(text));
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  animate
    // ------------------------------------------------------------------
    private ToolResult doAnimate(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String anim = optString(args, "animation_type", "fade").toLowerCase();
        if (!ANIMATION_TYPES.contains(anim)) {
            return err("Unknown animation_type '" + anim + "'. Use one of: " + ANIMATION_TYPES + ".");
        }
        int requestedDuration = optInt(args, "duration", 300);
        int duration = requestedDuration;
        String warning = null;
        if (duration < MIN_DURATION) {
            duration = MIN_DURATION;
            warning = "Requested duration " + requestedDuration + "ms is below minimum "
                    + MIN_DURATION + "ms; clamped to " + duration + "ms.";
        } else if (duration > MAX_DURATION) {
            duration = MAX_DURATION;
            warning = "Requested duration " + requestedDuration + "ms is above maximum "
                    + MAX_DURATION + "ms; clamped to " + duration + "ms.";
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "m", widgetId, anim, duration);
            ctx.refreshViewEditor();
            StringBuilder msg = new StringBuilder();
            msg.append("animate('").append(widgetId).append("', type='").append(anim)
               .append("', duration=").append(duration).append("ms) applied.");
            if (warning != null) {
                msg.append("\nWARNING: ").append(warning);
            }
            return ok(msg.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    /**
     * Returns a non-null error result if the widget doesn't exist (or if the
     * active layout is missing). Returns null if the widget exists.
     */
    private ToolResult checkWidgetExists(SketchwareToolContext ctx, String scId, String widgetId) {
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null || javaName.isEmpty()) {
            return err("No active layout. Open a layout in the View editor first.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<String> available = listWidgetIds(editor, javaName);
        if (!available.contains(widgetId)) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + available);
        }
        return null;
    }

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

    /**
     * Best-effort: list image resource names from {@code jC.d(scId)}. Returns
     * an empty list if the reflection call fails (the caller then skips the
     * existence check rather than blocking the action).
     */
    private static List<String> listImageResourceNames(String scId) {
        List<String> names = new ArrayList<>();
        if (scId == null) return names;
        try {
            Object resourceEditor = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            Object images = SketchwareApi.invoke(resourceEditor, "a");
            if (images instanceof List) {
                for (Object b : (List<?>) images) {
                    String n = readField(b, "name");
                    if (n == null) n = readField(b, "fileName");
                    if (n != null) names.add(n);
                }
            }
        } catch (Throwable ignored) {}
        return names;
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

    /** Quote a string for display, showing the length and a truncated preview. */
    private static String quote(String s) {
        if (s == null) return "null";
        String preview = s.length() > 60 ? s.substring(0, 60) + "..." : s;
        return "\"" + preview + "\" (" + s.length() + " chars)";
    }
}
