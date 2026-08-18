package pro.sketchware.creator.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable JSON codec for the first Creator Runtime Project IR schema. */
public final class CreatorProjectDocumentCodec {
    private CreatorProjectDocumentCodec() { }

    public static String encode(CreatorProjectDocument document) {
        if (document == null) throw new IllegalArgumentException("document");
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", document.getSchemaVersion());
        root.addProperty("projectId", document.getProjectId());
        root.addProperty("revision", document.getRevision());
        root.addProperty("name", document.getName());
        if (document.getEntryScreenId() == null) root.add("entryScreenId", JsonNull.INSTANCE);
        else root.addProperty("entryScreenId", document.getEntryScreenId());

        JsonObject entry = new JsonObject();
        entry.addProperty("visible", document.getEntryControl().isVisible());
        entry.addProperty("label", document.getEntryControl().getLabel());
        entry.addProperty("placement", document.getEntryControl().getPlacement());
        root.add("entryControl", entry);

        JsonArray screens = new JsonArray();
        for (CreatorScreen screen : document.getScreens().values()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", screen.getId());
            json.addProperty("route", screen.getRoute());
            json.addProperty("rootWidgetId", screen.getRootWidgetId());
            screens.add(json);
        }
        root.add("screens", screens);

        JsonArray widgets = new JsonArray();
        for (CreatorWidget widget : document.getWidgets().values()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", widget.getId());
            json.addProperty("type", widget.getType());
            if (widget.getParentId() == null) json.add("parentId", JsonNull.INSTANCE);
            else json.addProperty("parentId", widget.getParentId());
            JsonArray children = new JsonArray();
            for (String child : widget.getChildren()) children.add(child);
            json.add("children", children);
            json.add("properties", toJsonObject(widget.getProperties()));
            widgets.add(json);
        }
        root.add("widgets", widgets);
        root.add("state", toJsonObject(document.getState()));
        JsonArray events = new JsonArray();
        for (CreatorEventBinding binding : document.getEvents().values()) {
            JsonObject event = new JsonObject();
            event.addProperty("id", binding.getId());
            event.addProperty("targetWidgetId", binding.getTargetWidgetId());
            event.addProperty("eventName", binding.getEventName());
            JsonArray blocks = new JsonArray();
            for (CreatorRuntimeBlock block : binding.getBlocks()) {
                JsonObject encoded = new JsonObject();
                encoded.addProperty("type", block.getType().name());
                encoded.add("payload", toJsonObject(block.getPayload()));
                blocks.add(encoded);
            }
            event.add("blocks", blocks);
            events.add(event);
        }
        root.add("events", events);
        return root.toString();
    }

    public static CreatorProjectDocument decode(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) throw new IllegalArgumentException("serialized");
        JsonObject root = JsonParser.parseString(serialized).getAsJsonObject();
        int schemaVersion = required(root, "schemaVersion").getAsInt();
        String projectId = required(root, "projectId").getAsString();
        long revision = required(root, "revision").getAsLong();
        String name = required(root, "name").getAsString();
        String entryScreenId = root.has("entryScreenId") && !root.get("entryScreenId").isJsonNull()
                ? root.get("entryScreenId").getAsString() : null;

        JsonObject entry = required(root, "entryControl").getAsJsonObject();
        CreatorEntryControl entryControl = new CreatorEntryControl(
                required(entry, "visible").getAsBoolean(),
                required(entry, "label").getAsString(),
                required(entry, "placement").getAsString());

        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        for (JsonElement item : arrayOrEmpty(root, "screens")) {
            JsonObject json = item.getAsJsonObject();
            CreatorScreen screen = new CreatorScreen(required(json, "id").getAsString(),
                    required(json, "route").getAsString(), required(json, "rootWidgetId").getAsString());
            screens.put(screen.getId(), screen);
        }

        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        for (JsonElement item : arrayOrEmpty(root, "widgets")) {
            JsonObject json = item.getAsJsonObject();
            List<String> children = new ArrayList<>();
            for (JsonElement child : arrayOrEmpty(json, "children")) children.add(child.getAsString());
            String parentId = json.has("parentId") && !json.get("parentId").isJsonNull()
                    ? json.get("parentId").getAsString() : null;
            Map<String, Object> properties = json.has("properties") && json.get("properties").isJsonObject()
                    ? fromJsonObject(json.getAsJsonObject("properties")) : new LinkedHashMap<String, Object>();
            CreatorWidget widget = new CreatorWidget(required(json, "id").getAsString(),
                    required(json, "type").getAsString(), parentId, children, properties);
            widgets.put(widget.getId(), widget);
        }
        Map<String, Object> state = root.has("state") && root.get("state").isJsonObject()
                ? fromJsonObject(root.getAsJsonObject("state")) : new LinkedHashMap<String, Object>();
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        for (JsonElement item : arrayOrEmpty(root, "events")) {
            JsonObject event = item.getAsJsonObject();
            List<CreatorRuntimeBlock> blocks = new ArrayList<>();
            for (JsonElement blockItem : arrayOrEmpty(event, "blocks")) {
                JsonObject block = blockItem.getAsJsonObject();
                CreatorRuntimeBlock.Type type = CreatorRuntimeBlock.Type.valueOf(required(block, "type").getAsString());
                Map<String, Object> payload = block.has("payload") && block.get("payload").isJsonObject()
                        ? fromJsonObject(block.getAsJsonObject("payload")) : new LinkedHashMap<String, Object>();
                blocks.add(new CreatorRuntimeBlock(type, payload));
            }
            CreatorEventBinding binding = new CreatorEventBinding(required(event, "id").getAsString(),
                    required(event, "targetWidgetId").getAsString(), required(event, "eventName").getAsString(), blocks);
            events.put(binding.getId(), binding);
        }
        return new CreatorProjectDocument(schemaVersion, projectId, revision, name, entryScreenId,
                screens, widgets, entryControl, state, events);
    }

    private static JsonElement required(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing required document field: " + key);
        }
        return object.get(key);
    }

    private static JsonArray arrayOrEmpty(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static JsonObject toJsonObject(Map<String, Object> source) {
        JsonObject target = new JsonObject();
        for (Map.Entry<String, Object> entry : source.entrySet()) target.add(entry.getKey(), toJson(entry.getValue()));
        return target;
    }

    @SuppressWarnings("unchecked")
    private static JsonElement toJson(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof String) return new JsonPrimitive((String) value);
        if (value instanceof Number) return new JsonPrimitive((Number) value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
        if (value instanceof Map) return toJsonObject((Map<String, Object>) value);
        if (value instanceof List) {
            JsonArray array = new JsonArray();
            for (Object element : (List<Object>) value) array.add(toJson(element));
            return array;
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static Map<String, Object> fromJsonObject(JsonObject source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) target.put(entry.getKey(), fromJson(entry.getValue()));
        return target;
    }

    private static Object fromJson(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonObject()) return fromJsonObject(value.getAsJsonObject());
        if (value.isJsonArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonElement element : value.getAsJsonArray()) values.add(fromJson(element));
            return values;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }
}
