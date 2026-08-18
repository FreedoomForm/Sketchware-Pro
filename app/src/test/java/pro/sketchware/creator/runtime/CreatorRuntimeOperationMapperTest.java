package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import org.junit.Test;

public class CreatorRuntimeOperationMapperTest {

    @Test public void mapsAiWidgetOperationToTheSameTypedOperationUsedByTheVisualEditor() {
        JsonObject args = new JsonObject();
        args.addProperty("action", "add_widget");
        args.addProperty("widget_id", "title");
        args.addProperty("widget_type", "text");
        args.addProperty("parent_id", "root");
        JsonObject properties = new JsonObject();
        properties.addProperty("text", "Hello");
        args.add("properties", properties);

        CreatorProjectOperation operation = CreatorRuntimeOperationMapper.map(args,
                CreatorProjectDocument.empty("project", "Demo"), CreatorProjectOperation.ActorKind.AI);

        assertThat(operation.getActorKind()).isEqualTo(CreatorProjectOperation.ActorKind.AI);
        assertThat(operation.getType()).isEqualTo(CreatorProjectOperation.Type.WIDGET_ADD);
        assertThat(operation.getPayload().get("widgetId")).isEqualTo("title");
        assertThat(operation.getPayload().get("parentId")).isEqualTo("root");
        assertThat(((java.util.Map<?, ?>) operation.getPayload().get("properties")).get("text")).isEqualTo("Hello");
    }

    @Test public void mapsAiStateOperationAndNestedConditionalBlocks() {
        JsonObject stateArgs = new JsonObject();
        stateArgs.addProperty("action", "set_state");
        stateArgs.addProperty("state_id", "status");
        stateArgs.addProperty("value", "approved");
        CreatorProjectOperation state = CreatorRuntimeOperationMapper.map(stateArgs,
                CreatorProjectDocument.empty("project", "Demo"), CreatorProjectOperation.ActorKind.AI);
        assertThat(state.getType()).isEqualTo(CreatorProjectOperation.Type.STATE_SET);
        assertThat(state.getPayload().get("stateId")).isEqualTo("status");

        JsonObject eventArgs = new JsonObject();
        eventArgs.addProperty("action", "attach_event");
        eventArgs.addProperty("binding_id", "tap");
        eventArgs.addProperty("target_widget_id", "button");
        eventArgs.addProperty("event_name", "click");
        JsonObject conditional = new JsonObject();
        conditional.addProperty("type", "if_state_equals");
        conditional.addProperty("stateId", "status");
        conditional.addProperty("equals", "approved");
        JsonObject message = new JsonObject();
        message.addProperty("type", "show_message");
        message.addProperty("message", "Approved");
        JsonArray thenBlocks = new JsonArray();
        thenBlocks.add(message);
        conditional.add("then_blocks", thenBlocks);
        JsonArray blocks = new JsonArray();
        blocks.add(conditional);
        eventArgs.add("blocks", blocks);

        CreatorProjectOperation event = CreatorRuntimeOperationMapper.map(eventArgs,
                CreatorProjectDocument.empty("project", "Demo"), CreatorProjectOperation.ActorKind.AI);
        CreatorRuntimeBlock branch = ((java.util.List<CreatorRuntimeBlock>) event.getPayload().get("blocks")).get(0);
        assertThat(branch.getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_STATE_EQUALS);
        assertThat(branch.getThenBlocks().get(0).getPayload().get("message")).isEqualTo("Approved");
    }
}
