package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class CreatorProjectDocumentCodecTest {

    @Test public void codecPreservesAProjectDocumentWithWidgetsAndEditableEntryControl() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(
                CreatorProjectDocument.empty("project", "Demo"), 10, new CreatorRuntimeEventLog(20));
        engine.apply(operation("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(operation("widget", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root",
                        "properties", map("text", "Open"))));
        engine.apply(operation("entry", 2, CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE,
                map("label", "Edit", "placement", "top_end")));

        CreatorProjectDocument decoded = CreatorProjectDocumentCodec.decode(
                CreatorProjectDocumentCodec.encode(engine.getCurrent()));

        assertThat(decoded.getRevision()).isEqualTo(3L);
        assertThat(decoded.getEntryControl().getLabel()).isEqualTo("Edit");
        assertThat(decoded.getEntryControl().getPlacement()).isEqualTo("top_end");
        assertThat(decoded.getWidgets().get("button").getProperties().get("text")).isEqualTo("Open");
    }

    private static CreatorProjectOperation operation(String id, long revision,
                                                      CreatorProjectOperation.Type type, Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "project", revision, CreatorProjectOperation.ActorKind.USER,
                type, payload, 1L);
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
