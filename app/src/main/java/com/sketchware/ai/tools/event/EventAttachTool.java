package com.sketchware.ai.tools.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * event_attach - attach an event handler via reflection.
 */
public final class EventAttachTool implements SketchwareTool {

    @Override public String name() { return "event_attach"; }
    @Override public String category() { return "event"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Attach an event handler to a target. target_type: view/component/activity/drawer/moreblock. "
                + "Common event_name values: onClick, onTouch, onCreate, onResume, onPause, onStart, onStop, "
                + "onDestroy, onActivityResult, onBackPressed, onResponse, onErrorResponse.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject targetType = new JsonObject();
        targetType.addProperty("type", "string");
        JsonArray typeEnum = new JsonArray();
        typeEnum.add("view"); typeEnum.add("component"); typeEnum.add("activity");
        typeEnum.add("drawer"); typeEnum.add("moreblock");
        targetType.add("enum", typeEnum);
        props.add("target_type", targetType);
        JsonObject targetId = new JsonObject();
        targetId.addProperty("type", "string");
        props.add("target_id", targetId);
        JsonObject eventName = new JsonObject();
        eventName.addProperty("type", "string");
        props.add("event_name", eventName);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("target_type");
        required.add("event_name");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String targetType = args.has("target_type") ? args.get("target_type").getAsString() : "view";
        String targetId = args.has("target_id") && !args.get("target_id").isJsonNull()
                ? args.get("target_id").getAsString() : "";
        String eventName = args.has("event_name") ? args.get("event_name").getAsString() : null;
        if (eventName == null || eventName.isEmpty()) return ToolResult.error("event_name is required");
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        try {
            int eventType;
            switch (targetType) {
                case "view": eventType = 1; break;
                case "component": eventType = 2; break;
                case "activity": eventType = 3; break;
                case "drawer": eventType = 4; break;
                default: eventType = 5; break;
            }
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Class<?> eventBeanClass = Class.forName("com.besome.sketch.beans.EventBean");
            Object bean = eventBeanClass.getDeclaredConstructor(int.class, int.class, String.class, String.class)
                    .newInstance(eventType, 0, targetId, eventName);
            SketchwareApi.invoke(eC, "a", javaName, bean);
            SketchwareApi.invoke(eC, "k");
            return ToolResult.success("Attached event " + eventName + " to " + targetType + " '" + targetId + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
