package com.sketchware.ai.tools.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_set_property - set a property on an existing widget.
 * Uses reflection for both lookup and write.
 */
public final class ViewSetPropertyTool implements SketchwareTool {

    @Override public String name() { return "view_set_property"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Set a property on an existing widget. "
                + "Property keys: id, layout_width, layout_height, padding_left, padding_top, "
                + "padding_right, padding_bottom, margin_left, margin_top, margin_right, margin_bottom, "
                + "orientation, weight_sum, gravity, layout_gravity, weight, text, text_size, text_style, "
                + "text_color, hint, hint_color, single_line, lines, input_type, ime_option, image, "
                + "scale_type, background_resource, background_color, enabled, rotate, alpha, translation_x, "
                + "translation_y, scale_x, scale_y, inject, convert, spinner_mode, divider_height, "
                + "custom_view_listview, checked, max, progress, progressbar_style, indeterminate, "
                + "first_day_of_week, ad_size.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject widgetId = new JsonObject();
        widgetId.addProperty("type", "string");
        props.add("widget_id", widgetId);
        JsonObject key = new JsonObject();
        key.addProperty("type", "string");
        props.add("property_key", key);
        JsonObject value = new JsonObject();
        value.addProperty("type", "string");
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
        String widgetId = args.has("widget_id") ? args.get("widget_id").getAsString() : null;
        String key = args.has("property_key") ? args.get("property_key").getAsString() : null;
        String value = args.has("value") && !args.get("value").isJsonNull()
                ? args.get("value").getAsString() : null;
        if (widgetId == null || key == null || value == null) {
            return ToolResult.error("widget_id, property_key, and value are all required");
        }
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
            applyProperty(target, key, value);
            SketchwareApi.invoke(eC, "a", javaName, target);
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
            case "id": setField(bean, "id", value); break;
            case "layout_width": if (layout != null) setField(layout, "width", value); break;
            case "layout_height": if (layout != null) setField(layout, "height", value); break;
            case "padding_left": if (layout != null) setField(layout, "paddingL", Integer.parseInt(value)); break;
            case "padding_top": if (layout != null) setField(layout, "paddingT", Integer.parseInt(value)); break;
            case "padding_right": if (layout != null) setField(layout, "paddingR", Integer.parseInt(value)); break;
            case "padding_bottom": if (layout != null) setField(layout, "paddingB", Integer.parseInt(value)); break;
            case "margin_left": if (layout != null) setField(layout, "marginL", Integer.parseInt(value)); break;
            case "margin_top": if (layout != null) setField(layout, "marginT", Integer.parseInt(value)); break;
            case "margin_right": if (layout != null) setField(layout, "marginR", Integer.parseInt(value)); break;
            case "margin_bottom": if (layout != null) setField(layout, "marginB", Integer.parseInt(value)); break;
            case "orientation": if (layout != null) setField(layout, "orientation", value); break;
            case "weight_sum": if (layout != null) setField(layout, "weightSum", value); break;
            case "gravity": if (layout != null) setField(layout, "gravity", value); break;
            case "layout_gravity": if (layout != null) setField(layout, "layoutGravity", value); break;
            case "weight": if (layout != null) setField(layout, "weight", Float.parseFloat(value)); break;
            case "text": if (text != null) setField(text, "text", value); break;
            case "text_size": if (text != null) setField(text, "textSize", Float.parseFloat(value)); break;
            case "text_style": if (text != null) setField(text, "textType", value); break;
            case "text_color": if (text != null) setField(text, "textColor", value); break;
            case "hint": if (text != null) setField(text, "hint", value); break;
            case "hint_color": if (text != null) setField(text, "hintColor", value); break;
            case "single_line": if (text != null) setField(text, "singleLine", Boolean.parseBoolean(value)); break;
            case "lines": if (text != null) setField(text, "line", Integer.parseInt(value)); break;
            case "input_type": if (text != null) setField(text, "inputType", value); break;
            case "ime_option": if (text != null) setField(text, "imeOption", value); break;
            case "image": if (image != null) setField(image, "resName", value); break;
            case "scale_type": if (image != null) setField(image, "scaleType", value); break;
            case "background_resource": if (layout != null) setField(layout, "backgroundResource", value); break;
            case "background_color": if (layout != null) setField(layout, "backgroundColor", value); break;
            case "enabled": setField(bean, "enabled", Boolean.parseBoolean(value)); break;
            case "rotate": if (image != null) setField(image, "rotate", Float.parseFloat(value)); break;
            case "alpha": setField(bean, "alpha", Float.parseFloat(value)); break;
            case "translation_x": setField(bean, "translationX", Float.parseFloat(value)); break;
            case "translation_y": setField(bean, "translationY", Float.parseFloat(value)); break;
            case "scale_x": setField(bean, "scaleX", Float.parseFloat(value)); break;
            case "scale_y": setField(bean, "scaleY", Float.parseFloat(value)); break;
            case "inject": setField(bean, "inject", value); break;
            case "convert": setField(bean, "convert", value); break;
            case "spinner_mode": setField(bean, "spinnerMode", value); break;
            case "divider_height": setField(bean, "dividerHeight", value); break;
            case "custom_view_listview": setField(bean, "customView", value); break;
            case "checked": setField(bean, "checked", Boolean.parseBoolean(value)); break;
            case "max": setField(bean, "max", Integer.parseInt(value)); break;
            case "progress": setField(bean, "progress", Integer.parseInt(value)); break;
            case "progressbar_style": setField(bean, "progressStyle", value); break;
            case "indeterminate": setField(bean, "indeterminate", Boolean.parseBoolean(value)); break;
            case "first_day_of_week": setField(bean, "firstDayOfWeek", Integer.parseInt(value)); break;
            case "ad_size": setField(bean, "adSize", value); break;
            default:
                throw new IllegalArgumentException("Unknown property key: " + key);
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

    private void setField(Object obj, String name, Object value) throws Exception {
        java.lang.reflect.Field f;
        try { f = obj.getClass().getDeclaredField(name); }
        catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
        f.setAccessible(true);
        Class<?> t = f.getType();
        if (t == int.class && value instanceof Integer) f.setInt(obj, (Integer) value);
        else if (t == boolean.class && value instanceof Boolean) f.setBoolean(obj, (Boolean) value);
        else if (t == float.class && value instanceof Float) f.setFloat(obj, (Float) value);
        else f.set(obj, value);
    }
}
