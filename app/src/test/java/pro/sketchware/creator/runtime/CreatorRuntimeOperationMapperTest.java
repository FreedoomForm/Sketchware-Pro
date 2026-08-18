package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;

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
}
