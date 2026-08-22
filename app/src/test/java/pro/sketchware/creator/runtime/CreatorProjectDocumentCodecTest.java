package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
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

    @Test public void codecPreservesLockedEditorScreen() {
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("main", new CreatorScreen("main", "/main", "root_main", false));
        screens.put("editor", new CreatorScreen("editor", "/editor", "root_editor", true));
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                "project", 1L, "Demo", "main", screens, new LinkedHashMap<>(),
                CreatorEntryControl.defaultControl());

        CreatorProjectDocument decoded = CreatorProjectDocumentCodec.decode(
                CreatorProjectDocumentCodec.encode(document));

        assertThat(decoded.getScreens().get("main").isLocked()).isFalse();
        assertThat(decoded.getScreens().get("editor").isLocked()).isTrue();

    }

    @Test public void codecPreservesImportedRuntimeStateAndTypedEventBlocks() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null, Arrays.asList("button"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root", null, null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        Map<String, Object> state = map("legacy.components", map("camera1", map("serviceId", "camera")));
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("tap", new CreatorEventBinding("tap", "button", "click", Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "Live")))));
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                "project", 4L, "Demo", "home", screens, widgets, CreatorEntryControl.defaultControl(), state, events);

        CreatorProjectDocument decoded = CreatorProjectDocumentCodec.decode(CreatorProjectDocumentCodec.encode(document));

        assertThat(decoded.getState()).containsKey("legacy.components");
        assertThat(decoded.getEvents()).containsKey("tap");
        assertThat(decoded.getEvents().get("tap").getBlocks().get(0).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(decoded.getEvents().get("tap").getBlocks().get(0).getPayload().get("message")).isEqualTo("Live");
    }

    @Test public void codecPreservesTypedCollectionMutationBlocksAndNestedValues() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null, Arrays.asList("button"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root", null, null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("tap", new CreatorEventBinding("tap", "button", "click", Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE,
                        map("stateId", "items", "action", "add", "value", Arrays.asList("one", "two"))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE,
                        map("stateId", "profile", "action", "put", "key", "name", "value", "Ada")))));
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                "project", 4L, "Demo", "home", screens, widgets, CreatorEntryControl.defaultControl(), null, events);

        CreatorProjectDocument decoded = CreatorProjectDocumentCodec.decode(CreatorProjectDocumentCodec.encode(document));

        assertThat(decoded.getEvents().get("tap").getBlocks().get(0).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(decoded.getEvents().get("tap").getBlocks().get(1).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.MAP_MUTATE);
        assertThat(decoded.getEvents().get("tap").getBlocks().get(0).getPayload().get("value"))
                .isInstanceOf(java.util.List.class);
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
