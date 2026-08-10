package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * viewfunc_invoke — universal tool for view operations.
 *
 * <p>Replaces 7 stubs: viewfunc_invoke:set_text, viewfunc_invoke:set_image, viewfunc_invoke:set_background_color, viewfunc_invoke:set_text_color, viewfunc_invoke:set_visibility, viewfunc_invoke:get_text, viewfunc_invoke:animate
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ViewfuncInvokeTool extends UniversalTool {

    public ViewfuncInvokeTool() {
        super("viewfunc_invoke",
                "Invoke a runtime view function on a widget: set text, set image, set background color, set visibility, get text, or animate.",
                "view", false, false,
"set_text",
                "set_image",
                "set_background_color",
                "set_text_color",
                "set_visibility",
                "get_text",
                "animate");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_widget_id = new JsonObject();
        p_widget_id.addProperty("type", "string");
        p_widget_id.addProperty("description", "Target widget ID.");
        props.add("widget_id", p_widget_id);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "Value to set (text, color hex, image resource name, visibility: visible|invisible|gone).");
        props.add("value", p_value);
        JsonObject p_animation_type = new JsonObject();
        p_animation_type.addProperty("type", "string");
        p_animation_type.addProperty("description", "(animate) Animation type: fade|slide|scale|rotate.");
        props.add("animation_type", p_animation_type);
        JsonObject p_duration = new JsonObject();
        p_duration.addProperty("type", "integer");
        p_duration.addProperty("description", "(animate) Duration in ms (default 300).");
        props.add("duration", p_duration);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "set_text": {
                return applyViewFunc(ctx, args, "setText", "value");
            }
            case "set_image": {
                return applyViewFunc(ctx, args, "setImage", "value");
            }
            case "set_background_color": {
                return applyViewFunc(ctx, args, "setBackgroundColor", "value");
            }
            case "set_text_color": {
                return applyViewFunc(ctx, args, "setTextColor", "value");
            }
            case "set_visibility": {
                return applyViewFunc(ctx, args, "setVisibility", "value");
            }
            case "get_text": {
                return applyViewFunc(ctx, args, "getText", null);
            }
            case "animate": {
                String widgetId = optString(args, "widget_id");
                                if (widgetId == null) return err("widget_id is required");
                                String anim = optString(args, "animation_type", "fade");
                                int dur = optInt(args, "duration", 300);
                                try {
                                    Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
                                    SketchwareApi.invoke(editor, "m", widgetId, anim, dur);
                                    ctx.refreshViewEditor();
                                    return ok("Animated widget '" + widgetId + "' with " + anim + " for " + dur + "ms.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult applyViewFunc(SketchwareToolContext ctx, JsonObject args, String method, String valueKey) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null) return err("widget_id is required");
        String value = valueKey != null ? optString(args, valueKey) : null;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", ctx.getScId());
            Object result;
            if (value != null) {
                result = SketchwareApi.invoke(editor, method, widgetId, value);
            } else {
                result = SketchwareApi.invoke(editor, method, widgetId);
            }
            ctx.refreshViewEditor();
            return ok(method + "('" + widgetId + "'" + (value != null ? ", " + value : "") + ") = " + result);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
