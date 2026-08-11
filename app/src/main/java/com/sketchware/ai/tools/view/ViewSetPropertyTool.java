package com.sketchware.ai.tools.view;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.TextBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_set_property - set a property on an existing widget.
 *
 * <p>Uses reflection for both lookup and write. The previous implementation
 * had multiple bugs that caused every property set to fail with
 * {@code NoSuchFieldException} or {@code IllegalArgumentException}:
 *
 * <ul>
 *   <li><b>Wrong field names</b>: used {@code paddingL/paddingT/paddingR/paddingB}
 *       and {@code marginL/marginT/marginR/marginB} — these fields do NOT
 *       exist on {@link LayoutBean} (which extends obfuscated {@code a.a.a.nA}).
 *       The actual field names are {@code paddingLeft/paddingTop/paddingRight/
 *       paddingBottom} and {@code marginLeft/marginTop/marginRight/marginBottom}
 *       (see LayoutBean.java lines 64-69, 54-60).</li>
 *   <li><b>Wrong types</b>: passed {@code String} values to {@code int} fields
 *       ({@code width}, {@code height}, {@code weight}, {@code weightSum},
 *       {@code backgroundColor}, {@code textColor}, {@code textSize}, etc.)
 *       — reflection's {@code Field.set} rejects type mismatches.</li>
 *   <li><b>Wrong boolean encoding</b>: Sketchware uses {@code int -1 = true,
 *       0 = false} (see {@link LayoutBean#VALUE_TRUE}/{@code VALUE_FALSE}),
 *       NOT Java {@code boolean}.</li>
 * </ul>
 *
 * <p>This implementation performs the correct type conversions:
 * <ul>
 *   <li>{@code layout_width}/{@code layout_height}: accept {@code "match_parent"},
 *       {@code "wrap_content"}, or a numeric pixel value.</li>
 *   <li>{@code *_color}: accept {@code "#RRGGBB"} or {@code "#AARRGGBB"} hex
 *       strings, converted to ARGB int.</li>
 *   <li>{@code gravity}/{@code layout_gravity}: accept pipe-separated tokens
 *       like {@code "right|center_vertical"}, converted to Android gravity
 *       bitmask.</li>
 *   <li>{@code orientation}: accept {@code "vertical"}/{@code "horizontal"}.</li>
 *   <li>{@code text_style}: accept {@code "normal"}/{@code "bold"}/
 *       {@code "italic"}/{@code "bold_italic"}.</li>
 *   <li>{@code input_type}: accept named constants ({@code "text"}/{@code "number"}/
 *       {@code "phone"}/{@code "password"}).</li>
 *   <li>{@code enabled}/{@code checked}/{@code single_line}: accept
 *       {@code "true"}/{@code "false"} (mapped to -1/0).</li>
 * </ul>
 */
public final class ViewSetPropertyTool implements SketchwareTool {

    @Override public String name() { return "view_set_property"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Set a property on an existing widget. "
                + "REQUIRED: widget_id, property_key, value. "
                + "widget_id can be a specific widget ID (e.g. 'button1') or 'root' to target "
                + "the layout's root container. "
                + "Property_key MUST be one of the enum values listed below. "
                + "Common keys: id, layout_width, layout_height, padding, padding_left, padding_top, "
                + "padding_right, padding_bottom, margin, margin_left, margin_top, margin_right, margin_bottom, "
                + "orientation, weight_sum, gravity, layout_gravity, weight, text, text_size, text_style, "
                + "text_color, hint, hint_color, single_line, lines, input_type, ime_option, image, "
                + "scale_type, background_resource, background_color, enabled, rotate, alpha, translation_x, "
                + "translation_y, scale_x, scale_y, inject, convert, spinner_mode, divider_height, "
                + "custom_view_listview, checked, max, progress, progressbar_style, indeterminate, "
                + "first_day_of_week, ad_size. "
                + "'padding' and 'margin' are shortcuts that set all 4 sides at once. "
                + "Value semantics: layout_width/height accept 'match_parent'/'wrap_content'/number; "
                + "colors accept '#RRGGBB' or '#AARRGGBB'; gravity accepts 'left|center_vertical' etc; "
                + "orientation accepts 'vertical'/'horizontal'; booleans accept 'true'/'false'.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject widgetId = new JsonObject();
        widgetId.addProperty("type", "string");
        widgetId.addProperty("description", "Widget ID (e.g. 'button1')");
        props.add("widget_id", widgetId);
        JsonObject key = new JsonObject();
        key.addProperty("type", "string");
        key.addProperty("description", "Property key. MUST be one of the enum values.");
        JsonArray enumArr = new JsonArray();
        String[] keys = {
            "id", "layout_width", "layout_height",
            "padding", "padding_left", "padding_top", "padding_right", "padding_bottom",
            "margin", "margin_left", "margin_top", "margin_right", "margin_bottom",
            "orientation", "weight_sum", "gravity", "layout_gravity", "weight",
            "text", "text_size", "text_style", "text_color",
            "hint", "hint_color", "single_line", "lines", "input_type", "ime_option",
            "image", "scale_type", "background_resource", "background_color",
            "enabled", "rotate", "alpha", "translation_x", "translation_y",
            "scale_x", "scale_y", "inject", "convert",
            "spinner_mode", "divider_height", "custom_view_listview",
            "checked", "max", "progress", "progressbar_style", "indeterminate",
            "first_day_of_week", "ad_size"
        };
        for (String k : keys) enumArr.add(k);
        key.add("enum", enumArr);
        props.add("property_key", key);
        JsonObject value = new JsonObject();
        value.addProperty("type", "string");
        value.addProperty("description", "Property value as a string. Will be converted to the target field's type.");
        props.add("value", value);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("widget_id");
        required.add("property_key");
        required.add("value");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        // Accept "property_key" as the canonical name, but also accept "property"
        // as an alias — some LLMs (Z.AI GLM-4.6) hallucinate "property" despite
        // the schema explicitly requiring "property_key". Treating it as an
        // alias avoids a hard validation failure that would cascade into
        // "Not the same number of function calls and responses" when multiple
        // tool calls fail validation in the same turn.
        String widgetId = args.has("widget_id") && !args.get("widget_id").isJsonNull()
                ? args.get("widget_id").getAsString() : null;
        String key = null;
        if (args.has("property_key") && !args.get("property_key").isJsonNull()) {
            key = args.get("property_key").getAsString();
        } else if (args.has("property") && !args.get("property").isJsonNull()) {
            key = args.get("property").getAsString();
        }
        String value = args.has("value") && !args.get("value").isJsonNull()
                ? args.get("value").getAsString() : null;
        if (widgetId == null || key == null || value == null) {
            return ToolResult.error("widget_id, property_key, and value are all required");
        }
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        // Normalise to .xml-suffixed name — eC's HashMap is keyed by "main.xml".
        String xmlName = javaName.endsWith(".xml") ? javaName : javaName + ".xml";
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object target = null;
            // Special case: widget_id="root" targets the layout's root
            // ViewBean (the container itself). eC.h(xmlName) returns the
            // root ViewBean. Without this, the AI couldn't set orientation
            // or gravity on the root LinearLayout — it would always get
            // "Widget 'root' not found" because the root isn't in the
            // regular widget list (eC.d(xmlName)).
            if ("root".equals(widgetId)) {
                try {
                    target = SketchwareApi.invoke(eC, "h", xmlName);
                } catch (Throwable ignored) {}
                if (target == null) {
                    return ToolResult.error("Root container not found for layout '" + xmlName + "'. "
                            + "The layout may not be initialised. Try view_manage_layout create first.");
                }
            } else {
                Object widgets = SketchwareApi.invoke(eC, "d", xmlName);
                if (widgets instanceof List) {
                    for (Object b : (List<?>) widgets) {
                        Object id = getFieldValue(b, "id");
                        if (id != null && widgetId.equals(id.toString())) { target = b; break; }
                    }
                }
            }
            if (target == null) return ToolResult.error("Widget '" + widgetId + "' not found "
                    + "in layout '" + xmlName + "'. Use view_list_widgets to see available IDs.");
            applyProperty(target, key, value);
            // For non-root widgets, persist the change via eC.a(xmlName, viewBean).
            // For the root widget, eC.a(xmlName, viewBean) might duplicate it,
            // so skip the persist call — the root is already in eC.c.
            if (!"root".equals(widgetId)) {
                SketchwareApi.invoke(eC, "a", xmlName, target);
            }
            // Persist the property change to disk so the editor and tool stay in sync.
            ctx.persistViewToDisk();
            ctx.refreshViewEditor();
            return ToolResult.success("Set " + key + " = " + value + " on " + widgetId + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private void applyProperty(Object bean, String key, String value) throws Exception {
        Object layout = getFieldValue(bean, "layout");
        Object text = getFieldValue(bean, "text");
        Object image = getFieldValue(bean, "image");

        switch (key) {
            // ---- id ----
            case "id": setField(bean, "id", value); break;

            // ---- layout dimensions (int: -1=match, -2=wrap, >=0 = px) ----
            case "layout_width":
                if (layout != null) setIntField(layout, "width", parseDimension(value));
                break;
            case "layout_height":
                if (layout != null) setIntField(layout, "height", parseDimension(value));
                break;

            // ---- padding (int px) — CORRECT field names are paddingLeft/Top/Right/Bottom ----
            // 'padding' is a shortcut that sets all 4 sides at once.
            case "padding": {
                if (layout != null) {
                    int px = Integer.parseInt(value);
                    setIntField(layout, "paddingLeft", px);
                    setIntField(layout, "paddingTop", px);
                    setIntField(layout, "paddingRight", px);
                    setIntField(layout, "paddingBottom", px);
                }
                break;
            }
            case "padding_left":  if (layout != null) setIntField(layout, "paddingLeft",  Integer.parseInt(value)); break;
            case "padding_top":   if (layout != null) setIntField(layout, "paddingTop",   Integer.parseInt(value)); break;
            case "padding_right": if (layout != null) setIntField(layout, "paddingRight", Integer.parseInt(value)); break;
            case "padding_bottom":if (layout != null) setIntField(layout, "paddingBottom",Integer.parseInt(value)); break;

            // ---- margin (int px) ----
            // 'margin' is a shortcut that sets all 4 sides at once.
            case "margin": {
                if (layout != null) {
                    int px = Integer.parseInt(value);
                    setIntField(layout, "marginLeft", px);
                    setIntField(layout, "marginTop", px);
                    setIntField(layout, "marginRight", px);
                    setIntField(layout, "marginBottom", px);
                }
                break;
            }
            case "margin_left":   if (layout != null) setIntField(layout, "marginLeft",  Integer.parseInt(value)); break;
            case "margin_top":    if (layout != null) setIntField(layout, "marginTop",   Integer.parseInt(value)); break;
            case "margin_right":  if (layout != null) setIntField(layout, "marginRight", Integer.parseInt(value)); break;
            case "margin_bottom": if (layout != null) setIntField(layout, "marginBottom",Integer.parseInt(value)); break;

            // ---- layout container props (int-encoded enums) ----
            case "orientation":
                if (layout != null) setIntField(layout, "orientation", parseOrientation(value));
                break;
            case "weight_sum":
                if (layout != null) setIntField(layout, "weightSum", Integer.parseInt(value));
                break;
            case "gravity":
                if (layout != null) setIntField(layout, "gravity", parseGravity(value));
                break;
            case "layout_gravity":
                if (layout != null) setIntField(layout, "layoutGravity", parseGravity(value));
                break;
            case "weight":
                // LayoutBean.weight is int (not float as previously assumed).
                if (layout != null) setIntField(layout, "weight", (int) Float.parseFloat(value));
                break;

            // ---- text properties (TextBean) ----
            case "text":
                if (text != null) setField(text, "text", value);
                break;
            case "text_size":
                // TextBean.textSize is int (sp), not float.
                if (text != null) setIntField(text, "textSize", (int) Float.parseFloat(value));
                break;
            case "text_style":
                // TextBean.textType is int-encoded enum.
                if (text != null) setIntField(text, "textType", parseTextStyle(value));
                break;
            case "text_color":
                // TextBean.textColor is int ARGB.
                if (text != null) setIntField(text, "textColor", parseColor(value));
                break;
            case "hint":
                if (text != null) setField(text, "hint", value);
                break;
            case "hint_color":
                if (text != null) setIntField(text, "hintColor", parseColor(value));
                break;
            case "single_line":
                // TextBean.singleLine is int (-1=true, 0=false), NOT boolean.
                if (text != null) setIntField(text, "singleLine", parseSketchwareBool(value));
                break;
            case "lines":
                if (text != null) setIntField(text, "line", Integer.parseInt(value));
                break;
            case "input_type":
                if (text != null) setIntField(text, "inputType", parseInputType(value));
                break;
            case "ime_option":
                if (text != null) setIntField(text, "imeOption", parseImeOption(value));
                break;

            // ---- image properties (ImageBean) ----
            case "image":
                if (image != null) setField(image, "resName", value);
                break;
            case "scale_type":
                if (image != null) setField(image, "scaleType", value);
                break;

            // ---- background (LayoutBean) ----
            case "background_resource":
                if (layout != null) setField(layout, "backgroundResource", value);
                break;
            case "background_color":
                // LayoutBean.backgroundColor is int ARGB.
                if (layout != null) setIntField(layout, "backgroundColor", parseColor(value));
                break;

            // ---- ViewBean-level props ----
            case "enabled":
                // ViewBean.enabled is int (-1=true, 0=false), NOT boolean.
                setIntField(bean, "enabled", parseSketchwareBool(value));
                break;
            case "rotate":
                // ImageBean.rotate is int (degrees), not float.
                if (image != null) setIntField(image, "rotate", (int) Float.parseFloat(value));
                break;
            case "alpha":
                // ViewBean.alpha IS float.
                setFloatField(bean, "alpha", Float.parseFloat(value));
                break;
            case "translation_x": setFloatField(bean, "translationX", Float.parseFloat(value)); break;
            case "translation_y": setFloatField(bean, "translationY", Float.parseFloat(value)); break;
            case "scale_x":       setFloatField(bean, "scaleX",       Float.parseFloat(value)); break;
            case "scale_y":       setFloatField(bean, "scaleY",       Float.parseFloat(value)); break;
            case "inject":        setField(bean, "inject", value); break;
            case "convert":       setField(bean, "convert", value); break;
            case "spinner_mode":
                // ViewBean.spinnerMode is int (0=dialog, 1=dropdown).
                setIntField(bean, "spinnerMode", parseSpinnerMode(value));
                break;
            case "divider_height":
                setIntField(bean, "dividerHeight", Integer.parseInt(value));
                break;
            case "custom_view_listview":
                setField(bean, "customView", value);
                break;
            case "checked":
                // ViewBean.checked is int (-1=true, 0=false), NOT boolean.
                setIntField(bean, "checked", parseSketchwareBool(value));
                break;
            case "max":
                setIntField(bean, "max", Integer.parseInt(value));
                break;
            case "progress":
                setIntField(bean, "progress", Integer.parseInt(value));
                break;
            case "progressbar_style":
                setField(bean, "progressStyle", value);
                break;
            case "indeterminate":
                // ViewBean.indeterminate is String "true"/"false".
                setField(bean, "indeterminate", value);
                break;
            case "first_day_of_week":
                setIntField(bean, "firstDayOfWeek", Integer.parseInt(value));
                break;
            case "ad_size":
                setField(bean, "adSize", value);
                break;
            default:
                throw new IllegalArgumentException("Unknown property key: " + key
                        + ". Valid keys are listed in the tool description.");
        }
    }

    // ==================================================================
    //  Type conversion helpers
    // ==================================================================

    /** Parse layout dimension: "match_parent" → -1, "wrap_content" → -2, else int px. */
    private static int parseDimension(String v) {
        if (v == null) return LayoutBean.LAYOUT_WRAP_CONTENT;
        switch (v.toLowerCase().trim()) {
            case "match_parent":
            case "fill_parent":   return LayoutBean.LAYOUT_MATCH_PARENT;
            case "wrap_content":  return LayoutBean.LAYOUT_WRAP_CONTENT;
            default:              return Integer.parseInt(v.trim());
        }
    }

    /** Parse orientation: "vertical" → 1, "horizontal" → 0, "none" → -1. */
    private static int parseOrientation(String v) {
        if (v == null) return LayoutBean.ORIENTATION_NONE;
        switch (v.toLowerCase().trim()) {
            case "vertical":   return LayoutBean.ORIENTATION_VERTICAL;
            case "horizontal": return LayoutBean.ORIENTATION_HORIZONTAL;
            case "none":       return LayoutBean.ORIENTATION_NONE;
            default:           return Integer.parseInt(v.trim());
        }
    }

    /** Parse gravity tokens like "right|center_vertical" into Android gravity bitmask. */
    private static int parseGravity(String v) {
        if (v == null || v.isEmpty()) return LayoutBean.GRAVITY_NONE;
        int result = 0;
        for (String token : v.toLowerCase().split("\\|")) {
            switch (token.trim()) {
                case "left":             result |= LayoutBean.GRAVITY_LEFT;             break;
                case "right":            result |= LayoutBean.GRAVITY_RIGHT;            break;
                case "top":              result |= LayoutBean.GRAVITY_TOP;              break;
                case "bottom":           result |= LayoutBean.GRAVITY_BOTTOM;           break;
                case "center":           result |= LayoutBean.GRAVITY_CENTER;           break;
                case "center_horizontal":result |= LayoutBean.GRAVITY_CENTER_HORIZONTAL;break;
                case "center_vertical":  result |= LayoutBean.GRAVITY_CENTER_VERTICAL;  break;
                case "none":             result |= LayoutBean.GRAVITY_NONE;             break;
                default:
                    // Allow numeric gravity too.
                    try { result |= Integer.parseInt(token.trim()); }
                    catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    /** Parse text style: "normal"→0, "bold"→1, "italic"→2, "bold_italic"→3. */
    private static int parseTextStyle(String v) {
        if (v == null) return TextBean.TEXT_TYPE_NORMAL;
        switch (v.toLowerCase().trim()) {
            case "bold":        return TextBean.TEXT_TYPE_BOLD;
            case "italic":      return TextBean.TEXT_TYPE_ITALIC;
            case "bold_italic": return TextBean.TEXT_TYPE_BOLDITALIC;
            case "normal":      return TextBean.TEXT_TYPE_NORMAL;
            default:            return Integer.parseInt(v.trim());
        }
    }

    /** Parse input type named constants. */
    private static int parseInputType(String v) {
        if (v == null) return TextBean.INPUT_TYPE_TEXT;
        switch (v.toLowerCase().trim()) {
            case "text":                 return TextBean.INPUT_TYPE_TEXT;
            case "number":               return TextBean.INPUT_TYPE_NUMBER_SIGNED;
            case "number_decimal":       return TextBean.INPUT_TYPE_NUMBER_DECIMAL;
            case "number_signed":        return TextBean.INPUT_TYPE_NUMBER_SIGNED;
            case "number_signed_decimal":return TextBean.INPUT_TYPE_NUMBER_SIGNED_DECIMAL;
            case "phone":                return TextBean.INPUT_TYPE_PHONE;
            case "password":             return TextBean.INPUT_TYPE_PASSWORD;
            default:                     return Integer.parseInt(v.trim());
        }
    }

    /** Parse IME option named constants. */
    private static int parseImeOption(String v) {
        if (v == null) return TextBean.IME_OPTION_NORMAL;
        switch (v.toLowerCase().trim()) {
            case "done":   return TextBean.IME_OPTION_DONE;
            case "go":     return TextBean.IME_OPTION_GO;
            case "next":   return TextBean.IME_OPTION_NEXT;
            case "none":   return TextBean.IME_OPTION_NONE;
            case "normal": return TextBean.IME_OPTION_NORMAL;
            case "search": return TextBean.IME_OPTION_SEARCH;
            case "send":   return TextBean.IME_OPTION_SEND;
            default:       return Integer.parseInt(v.trim());
        }
    }

    /** Parse spinner mode: "dialog"→0, "dropdown"→1. */
    private static int parseSpinnerMode(String v) {
        if (v == null) return 1;
        switch (v.toLowerCase().trim()) {
            case "dialog":   return 0;
            case "dropdown": return 1;
            default:         return Integer.parseInt(v.trim());
        }
    }

    /**
     * Parse Sketchware boolean encoding: -1 = true, 0 = false.
     * Accepts "true"/"false" (case-insensitive) or numeric.
     */
    private static int parseSketchwareBool(String v) {
        if (v == null) return 0;
        switch (v.toLowerCase().trim()) {
            case "true":  return LayoutBean.VALUE_TRUE;   // -1
            case "false": return LayoutBean.VALUE_FALSE;  // 0
            default:      return Integer.parseInt(v.trim());
        }
    }

    /**
     * Parse color string. Accepts "#RRGGBB" (alpha forced to FF) or "#AARRGGBB".
     * Returns ARGB int. Falls back to Integer.decode for numeric inputs like
     * "0xFFE0E0E0".
     */
    private static int parseColor(String v) {
        if (v == null) return 0xFF000000;
        String s = v.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 6) {
            // RRGGBB → FFRRGGBB
            return 0xFF000000 | Integer.parseInt(s, 16);
        } else if (s.length() == 8) {
            // AARRGGBB
            return (int) Long.parseLong(s, 16);
        }
        // Numeric like "0xFFE0E0E0" or "-1"
        return Integer.decode(v.trim());
    }

    // ==================================================================
    //  Reflection helpers
    // ==================================================================

    private Object getFieldValue(Object obj, String name) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(name); }
            catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) { return null; }
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        java.lang.reflect.Field f;
        try { f = obj.getClass().getDeclaredField(name); }
        catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void setIntField(Object obj, String name, int value) throws Exception {
        java.lang.reflect.Field f;
        try { f = obj.getClass().getDeclaredField(name); }
        catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private void setFloatField(Object obj, String name, float value) throws Exception {
        java.lang.reflect.Field f;
        try { f = obj.getClass().getDeclaredField(name); }
        catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
        f.setAccessible(true);
        f.setFloat(obj, value);
    }
}
