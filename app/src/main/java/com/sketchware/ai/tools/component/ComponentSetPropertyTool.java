package com.sketchware.ai.tools.component;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * component_set_property — universal tool for component operations.
 *
 * <p>Replaces 21 stubs: component_set_property:set_animator_duration, component_set_property:set_animator_property, component_set_property:set_animator_target, component_set_property:set_camera_facing, component_set_property:set_dialog_message, component_set_property:set_dialog_title, component_set_property:set_firebase_url, component_set_property:set_intent_action, component_set_property:set_intent_data, component_set_property:set_intent_extra, component_set_property:set_intent_flag, component_set_property:set_location_updates, component_set_property:set_mediaresource, component_set_property:set_param, component_set_property:set_requestnetwork_header, component_set_property:set_requestnetwork_method, component_set_property:set_requestnetwork_param, component_set_property:set_requestnetwork_url, component_set_property:set_sharedpref_key, component_set_property:set_timer_interval, component_set_property:set_tts_language
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ComponentSetPropertyTool extends UniversalTool {

    public ComponentSetPropertyTool() {
        super("component_set_property",
                "Set a typed property on a component. Covers all component_set_* operations (animator, camera, dialog, firebase, intent, location, media, requestnetwork, sharedpref, timer, tts, etc.).",
                "component", false, false,
"set_animator_duration",
                "set_animator_property",
                "set_animator_target",
                "set_camera_facing",
                "set_dialog_message",
                "set_dialog_title",
                "set_firebase_url",
                "set_intent_action",
                "set_intent_data",
                "set_intent_extra",
                "set_intent_flag",
                "set_location_updates",
                "set_mediaresource",
                "set_param",
                "set_requestnetwork_header",
                "set_requestnetwork_method",
                "set_requestnetwork_param",
                "set_requestnetwork_url",
                "set_sharedpref_key",
                "set_timer_interval",
                "set_tts_language");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_component_id = new JsonObject();
        p_component_id.addProperty("type", "string");
        p_component_id.addProperty("description", "Target component ID.");
        props.add("component_id", p_component_id);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "Property value to set.");
        props.add("value", p_value);
        JsonObject p_extra_key = new JsonObject();
        p_extra_key.addProperty("type", "string");
        p_extra_key.addProperty("description", "Key (for intent_extra, requestnetwork_header, requestnetwork_param).");
        props.add("extra_key", p_extra_key);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "set_animator_duration": {
                return setComponentProp(ctx, args, "animator", "duration");
            }
            case "set_animator_property": {
                return setComponentProp(ctx, args, "animator", "property");
            }
            case "set_animator_target": {
                return setComponentProp(ctx, args, "animator", "target");
            }
            case "set_camera_facing": {
                return setComponentProp(ctx, args, "camera", "facing");
            }
            case "set_dialog_message": {
                return setComponentProp(ctx, args, "dialog", "message");
            }
            case "set_dialog_title": {
                return setComponentProp(ctx, args, "dialog", "title");
            }
            case "set_firebase_url": {
                return setComponentProp(ctx, args, "firebase", "url");
            }
            case "set_intent_action": {
                return setComponentProp(ctx, args, "intent", "action");
            }
            case "set_intent_data": {
                return setComponentProp(ctx, args, "intent", "data");
            }
            case "set_intent_extra": {
                return setComponentPropKV(ctx, args, "intent", "extra");
            }
            case "set_intent_flag": {
                return setComponentProp(ctx, args, "intent", "flag");
            }
            case "set_location_updates": {
                return setComponentProp(ctx, args, "location", "updates");
            }
            case "set_mediaresource": {
                return setComponentProp(ctx, args, "mediaresource", "resource");
            }
            case "set_param": {
                return setComponentProp(ctx, args, "param", "value");
            }
            case "set_requestnetwork_header": {
                return setComponentPropKV(ctx, args, "requestnetwork", "header");
            }
            case "set_requestnetwork_method": {
                return setComponentProp(ctx, args, "requestnetwork", "method");
            }
            case "set_requestnetwork_param": {
                return setComponentPropKV(ctx, args, "requestnetwork", "param");
            }
            case "set_requestnetwork_url": {
                return setComponentProp(ctx, args, "requestnetwork", "url");
            }
            case "set_sharedpref_key": {
                return setComponentProp(ctx, args, "sharedpref", "key");
            }
            case "set_timer_interval": {
                return setComponentProp(ctx, args, "timer", "interval");
            }
            case "set_tts_language": {
                return setComponentProp(ctx, args, "tts", "language");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult setComponentProp(SketchwareToolContext ctx, JsonObject args, String group, String propKey) {
        String compId = optString(args, "component_id");
        if (compId == null) return err("component_id is required");
        String value = optString(args, "value");
        if (value == null) return err("value is required");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
            SketchwareApi.invoke(editor, "i", compId, group + ":" + propKey, value);
            ctx.refreshComponentList();
            return ok("Set " + group + "." + propKey + " = '" + value + "' on component '" + compId + "'.");
        } catch (Throwable t) { return ToolResult.error(t); }
    }

    private ToolResult setComponentPropKV(SketchwareToolContext ctx, JsonObject args, String group, String propKey) {
        String compId = optString(args, "component_id");
        if (compId == null) return err("component_id is required");
        String key = optString(args, "extra_key");
        String value = optString(args, "value");
        if (key == null || value == null) return err("extra_key and value are required");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
            SketchwareApi.invoke(editor, "i", compId, group + ":" + propKey, key, value);
            ctx.refreshComponentList();
            return ok("Set " + group + "." + propKey + "[" + key + "] = '" + value + "' on component '" + compId + "'.");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
