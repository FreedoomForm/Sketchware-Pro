package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreatorRuntimeExecutorTest {
    @Test public void clickBindingAppliesTypedStateAndWidgetUpdatesThenReturnsVisibleEffects() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("label", 2, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "label", "widgetType", "text", "parentId", "root")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "clicked", "value", true)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, map("widgetId", "label", "property", "text", "value", "Done")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "Saved")));
        engine.apply(op("event", 3, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("clicked")).isEqualTo(true);
        assertThat(engine.getCurrent().getWidgets().get("label").getProperties().get("text")).isEqualTo("Done");
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getType()).isEqualTo("message");
        assertThat(effects.get(0).getValue()).isEqualTo("Saved");
    }

    @Test public void incrementStateBlocksTreatMissingStateAsZeroAndApplySignedDelta() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE, map("stateId", "count", "delta", 1L)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE, map("stateId", "count", "delta", -1L)));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("count")).isEqualTo(0L);
    }

    @Test public void listMutationBlocksApplyCollectionEditsToRuntimeState() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("source", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "source", "value", Arrays.asList("two", "three"))));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, map("stateId", "items", "action", "add", "value", "one")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, map("stateId", "items", "action", "insert", "index", 0, "value", "zero")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, map("stateId", "items", "action", "remove_at", "index", 1)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, map("stateId", "items", "action", "add_all", "sourceStateId", "source")));
        engine.apply(op("event", 3, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat((java.util.List<?>) engine.getCurrent().getState().get("items"))
                .containsExactly("zero", "two", "three").inOrder();
    }

    @Test public void mapMutationBlocksCreateUpdateAndRemoveLiveStateEntries() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, map("stateId", "profile", "action", "create")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, map("stateId", "profile", "action", "put", "key", "name", "value", "Ada")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, map("stateId", "profile", "action", "put", "key", "city", "value", "London")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, map("stateId", "profile", "action", "remove", "key", "city")));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        @SuppressWarnings("unchecked") Map<String, Object> profile =
                (Map<String, Object>) engine.getCurrent().getState().get("profile");
        assertThat(profile).containsExactly("name", "Ada");
    }

    @Test public void executesBooleanConditionAndBoundedRepeatWithoutLegacyCodeExecution() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("enabled", 2, CreatorProjectOperation.Type.STATE_SET, map("stateId", "enabled", "value", true)));
        engine.apply(op("counter", 3, CreatorProjectOperation.Type.STATE_SET, map("stateId", "counter", "value", 0L)));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_BOOLEAN, map("stateId", "enabled"),
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                                map("stateId", "counter", "delta", 1L))),
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                                map("stateId", "counter", "delta", -1L)))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.REPEAT, map("count", "3"),
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                                map("stateId", "counter", "delta", 1L))), Collections.<CreatorRuntimeBlock>emptyList()));
        engine.apply(op("event", 4, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("counter")).isEqualTo(4L);
    }

    private static CreatorProjectOperation op(String id, long revision, CreatorProjectOperation.Type type, Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "p", revision, CreatorProjectOperation.ActorKind.USER, type, payload, 0);
    }
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
