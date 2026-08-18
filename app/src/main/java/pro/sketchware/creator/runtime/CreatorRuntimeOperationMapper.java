package pro.sketchware.creator.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/** Pure mapper from a normalized AI tool-call payload to a typed Creator operation. */
public final class CreatorRuntimeOperationMapper {
    private CreatorRuntimeOperationMapper() { }

    public static CreatorProjectOperation map(JsonObject args, CreatorProjectDocument document,
                                              CreatorProjectOperation.ActorKind actorKind) {
        if (args == null || document == null) throw new IllegalArgumentException("args and document are required");
        String action = requiredString(args, "action");
        Map<String, Object> payload = new LinkedHashMap<>();
        CreatorProjectOperation.Type type;
        switch (action) {
            case "create_screen":
                type = CreatorProjectOperation.Type.SCREEN_CREATE;
                copy(args, payload, "screen_id", "screenId");
                copy(args, payload, "route", "route");
                copy(args, payload, "root_widget_id", "rootWidgetId");
                copy(args, payload, "root_widget_type", "rootWidgetType");
                break;
            case "add_widget":
                type = CreatorProjectOperation.Type.WIDGET_ADD;
                copy(args, payload, "widget_id", "widgetId");
                copy(args, payload, "widget_type", "widgetType");
                copy(args, payload, "parent_id", "parentId");
                copy(args, payload, "index", "index");
                if (args.has("properties") && args.get("properties").isJsonObject()) {
                    payload.put("properties", jsonObjectToMap(args.getAsJsonObject("properties")));
                }
                break;
            case "set_widget_property":
                type = CreatorProjectOperation.Type.WIDGET_SET_PROPERTY;
                copy(args, payload, "widget_id", "widgetId");
                copy(args, payload, "property", "property");
                copy(args, payload, "value", "value");
                break;
            case "update_entry_control":
                type = CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE;
                copy(args, payload, "visible", "visible");
                copy(args, payload, "label", "label");
                copy(args, payload, "placement", "placement");
                break;
            case "restore_revision":
                type = CreatorProjectOperation.Type.REVISION_RESTORE;
                copy(args, payload, "target_revision", "targetRevision");
                break;
            case "attach_event":
                type = CreatorProjectOperation.Type.EVENT_ATTACH;
                copy(args, payload, "binding_id", "bindingId");
                copy(args, payload, "target_widget_id", "targetWidgetId");
                copy(args, payload, "event_name", "eventName");
                payload.put("blocks", blocks(args));
                break;
            default:
                throw new IllegalArgumentException("Unsupported Creator Runtime action: " + action);
        }
        return new CreatorProjectOperation("ai-" + UUID.randomUUID(), document.getProjectId(),
                document.getRevision(), actorKind, type, payload, System.currentTimeMillis());
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) throw new IllegalArgumentException(key + " is required");
        String value = object.get(key).getAsString();
        if (value.trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static void copy(JsonObject source, Map<String, Object> target, String from, String to) {
        if (source.has(from) && !source.get(from).isJsonNull()) target.put(to, jsonToValue(source.get(from)));
    }

    private static Map<String, Object> jsonObjectToMap(JsonObject source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) result.put(entry.getKey(), jsonToValue(entry.getValue()));
        return result;
    }

    private static java.util.List<CreatorRuntimeBlock> blocks(JsonObject args) {
        java.util.List<CreatorRuntimeBlock> result = new ArrayList<>();
        if (!args.has("blocks") || !args.get("blocks").isJsonArray()) return result;
        for (JsonElement element : args.getAsJsonArray("blocks")) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("each block must be an object");
            JsonObject block = element.getAsJsonObject();
            String type = requiredString(block, "type");
            Map<String, Object> payload = jsonObjectToMap(block);
            payload.remove("type");
            result.add(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.valueOf(
                    type.toUpperCase(java.util.Locale.ROOT)), payload));
        }
        return result;
    }

    private static Object jsonToValue(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) return jsonObjectToMap(element.getAsJsonObject());
        if (element.isJsonArray()) return element.toString(); // R1 arrays are introduced with the list/state slice.
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }
}
