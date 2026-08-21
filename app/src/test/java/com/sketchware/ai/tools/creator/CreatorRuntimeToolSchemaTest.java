package com.sketchware.ai.tools.creator;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectOperation;
import pro.sketchware.creator.runtime.CreatorRuntimeOperationMapper;

public final class CreatorRuntimeToolSchemaTest {
    @Test public void declaresEveryTopLevelCreatorRuntimeOperation() {
        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
        JsonArray actions = schema.getAsJsonObject("properties")
                .getAsJsonObject("action").getAsJsonArray("enum");
        assertThat(jsonStrings(actions)).containsExactly(
                "create_screen", "add_widget", "set_widget_property", "remove_widget", "set_state",
                "update_entry_control", "attach_event", "replace_event", "detach_event", "restore_revision");
    }

    @Test public void declaresEveryTypedRuntimeBlockInstrumentForAi() {
        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
        JsonArray blockTypes = schema.getAsJsonObject("properties")
                .getAsJsonObject("blocks")
                .getAsJsonObject("items")
                .getAsJsonObject("properties")
                .getAsJsonObject("type")
                .getAsJsonArray("enum");
        assertThat(jsonStrings(blockTypes)).containsExactly(
                "set_widget_property", "set_state", "increment_state", "list_mutate",
                "map_mutate", "attach_event", "replace_event", "detach_event", "show_message", "navigate",
                "runtime_service_call", "custom_function_call", "return", "if_state_equals",
                "if_boolean", "repeat", "forever", "break");
    }

    @Test public void mapsEveryTopLevelActionToOneTypedVisualEditorOperation() {
        CreatorProjectDocument document = CreatorProjectDocument.empty("project", "Demo");
        Map<String, JsonObject> fixtures = new LinkedHashMap<>();
        fixtures.put("create_screen", object("screen_id", "settings", "route", "/settings", "root_widget_id", "settings_root"));
        fixtures.put("add_widget", object("widget_id", "title", "widget_type", "text", "parent_id", "root"));
        fixtures.put("set_widget_property", object("widget_id", "title", "property", "text", "value", "Hello"));
        fixtures.put("remove_widget", object("widget_id", "title"));
        fixtures.put("set_state", object("state_id", "status", "value", "ready"));
        fixtures.put("update_entry_control", object("visible", true));
        fixtures.put("attach_event", object("binding_id", "tap", "target_widget_id", "title", "event_name", "click"));
        fixtures.put("replace_event", object("binding_id", "tap", "target_widget_id", "title", "event_name", "click"));
        fixtures.put("detach_event", object("binding_id", "tap"));
        fixtures.put("restore_revision", object("target_revision", 0));
        Set<CreatorProjectOperation.Type> mapped = new HashSet<>();
        for (Map.Entry<String, JsonObject> fixture : fixtures.entrySet()) {
            fixture.getValue().addProperty("action", fixture.getKey());
            mapped.add(CreatorRuntimeOperationMapper.map(fixture.getValue(), document,
                    CreatorProjectOperation.ActorKind.AI).getType());
        }
        assertThat(mapped).containsExactly(
                CreatorProjectOperation.Type.SCREEN_CREATE,
                CreatorProjectOperation.Type.WIDGET_ADD,
                CreatorProjectOperation.Type.WIDGET_SET_PROPERTY,
                CreatorProjectOperation.Type.WIDGET_REMOVE,
                CreatorProjectOperation.Type.STATE_SET,
                CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE,
                CreatorProjectOperation.Type.EVENT_ATTACH,
                CreatorProjectOperation.Type.EVENT_REPLACE,
                CreatorProjectOperation.Type.EVENT_DETACH,
                CreatorProjectOperation.Type.REVISION_RESTORE);
    }

    @Test public void mapsEveryDeclaredBlockInstrumentThroughTheRuntimeMapper() {
        JsonObject args = object("action", "attach_event", "binding_id", "instrumented",
                "target_widget_id", "button", "event_name", "click");
        JsonArray blocks = new JsonArray();
        String[] types = {
                "set_widget_property", "set_state", "increment_state", "list_mutate",
                "map_mutate", "attach_event", "replace_event", "detach_event", "show_message", "navigate",
                "runtime_service_call", "custom_function_call", "return", "if_state_equals",
                "if_boolean", "repeat", "forever", "break"};
        for (String type : types) blocks.add(object("type", type));
        args.add("blocks", blocks);
        CreatorProjectOperation operation = CreatorRuntimeOperationMapper.map(args,
                CreatorProjectDocument.empty("project", "Demo"), CreatorProjectOperation.ActorKind.AI);
        Set<String> mapped = new HashSet<>();
        for (pro.sketchware.creator.runtime.CreatorRuntimeBlock block
                : (java.util.List<pro.sketchware.creator.runtime.CreatorRuntimeBlock>) operation.getPayload().get("blocks")) {
            mapped.add(block.getType().name().toLowerCase(java.util.Locale.ROOT));
        }
        assertThat(mapped).containsExactlyElementsIn(Arrays.asList(types));
    }

    @Test public void schemaRequiresOnlyActionAtTopLevelAndKeepsNestedBlocksTyped() {
        JsonObject schema = new CreatorRuntimeTool().jsonSchema();
        assertThat(jsonStrings(schema.getAsJsonArray("required"))).containsExactly("action");
        JsonObject blockItems = schema.getAsJsonObject("properties")
                .getAsJsonObject("blocks").getAsJsonObject("items");
        assertThat(blockItems.get("type").getAsString()).isEqualTo("object");
        assertThat(blockItems.getAsJsonObject("properties").getAsJsonObject("type")
                .get("type").getAsString()).isEqualTo("string");
    }

    private static JsonObject object(Object... pairs) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            if (value instanceof Boolean) result.addProperty(String.valueOf(pairs[i]), (Boolean) value);
            else if (value instanceof Number) result.addProperty(String.valueOf(pairs[i]), (Number) value);
            else result.addProperty(String.valueOf(pairs[i]), String.valueOf(value));
        }
        return result;
    }

    private static Set<String> jsonStrings(JsonArray values) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < values.size(); i++) result.add(values.get(i).getAsString());
        return result;
    }
}
