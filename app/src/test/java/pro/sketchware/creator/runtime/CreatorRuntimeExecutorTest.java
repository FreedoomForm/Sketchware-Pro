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

    @Test public void evaluatesNestedLegacyReporterExpressionsForIfAndRepeat() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("name", 2, CreatorProjectOperation.Type.STATE_SET, map("stateId", "name", "value", "Ada")));
        engine.apply(op("counter", 3, CreatorProjectOperation.Type.STATE_SET, map("stateId", "counter", "value", 0L)));
        Map<String, Object> equal = map("kind", "reporter", "opCode", "stringequals", "arguments", Arrays.asList(
                map("kind", "literal", "value", "name"), map("kind", "literal", "value", "Ada")));
        Map<String, Object> addition = map("kind", "reporter", "opCode", "+", "arguments", Arrays.asList(
                map("kind", "literal", "value", "1"), map("kind", "literal", "value", "2")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_BOOLEAN, map("expression", equal),
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                                map("stateId", "counter", "delta", 1L))), Collections.<CreatorRuntimeBlock>emptyList()),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.REPEAT, map("countExpression", addition),
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                                map("stateId", "counter", "delta", 1L))), Collections.<CreatorRuntimeBlock>emptyList()));
        engine.apply(op("event", 4, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("counter")).isEqualTo(4L);
    }

    @Test public void assignsTypedNestedReporterResultToRuntimeState() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        Map<String, Object> sum = map("kind", "reporter", "opCode", "+", "arguments", Arrays.asList(
                map("kind", "literal", "value", "4"), map("kind", "literal", "value", "5")));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks",
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                                map("stateId", "total", "expression", sum))))));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("total")).isEqualTo(9d);
    }

    @Test public void appliesTypedNestedReporterResultToLiveWidgetProperty() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("label", 2, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "label", "widgetType", "text", "parentId", "root")));
        engine.apply(op("name", 3, CreatorProjectOperation.Type.STATE_SET, map("stateId", "name", "value", "Ada")));
        Map<String, Object> join = map("kind", "reporter", "opCode", "stringjoin", "arguments", Arrays.asList(
                map("kind", "literal", "value", "Hello "), map("kind", "literal", "value", "name")));
        engine.apply(op("event", 4, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks",
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY,
                                map("widgetId", "label", "property", "text", "expression", join))))));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getWidgets().get("label").getProperties().get("text")).isEqualTo("Hello Ada");
    }

    @Test public void appliesNestedReporterResultsToListAndMapMutations() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        Map<String, Object> sum = map("kind", "reporter", "opCode", "+", "arguments", Arrays.asList(
                map("kind", "literal", "value", "2"), map("kind", "literal", "value", "3")));
        Map<String, Object> length = map("kind", "reporter", "opCode", "stringlength", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "Ada")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE,
                        map("stateId", "items", "action", "add", "valueExpression", sum)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE,
                        map("stateId", "profile", "action", "put", "key", "score", "valueExpression", length)));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat((java.util.List<?>) engine.getCurrent().getState().get("items")).containsExactly(5d);
        @SuppressWarnings("unchecked") Map<String, Object> profile = (Map<String, Object>) engine.getCurrent().getState().get("profile");
        assertThat(profile).containsExactly("score", 3d);
    }

    @Test public void evaluatesCanonicalStringAndMathReporterFamiliesWithoutGeneratedJava() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        Map<String, Object> substring = map("kind", "reporter", "opCode", "stringsub", "arguments", Arrays.asList(
                map("kind", "literal", "value", "Creator"), map("kind", "literal", "value", "1"),
                map("kind", "literal", "value", "4")));
        Map<String, Object> power = map("kind", "reporter", "opCode", "mathpow", "arguments", Arrays.asList(
                map("kind", "literal", "value", "2"), map("kind", "literal", "value", "3")));
        Map<String, Object> replacement = map("kind", "reporter", "opCode", "stringreplace", "arguments", Arrays.asList(
                map("kind", "literal", "value", "book"), map("kind", "literal", "value", "o"),
                map("kind", "literal", "value", "a")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "fragment", "expression", substring)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "power", "expression", power)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "replacement", "expression", replacement)));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState()).containsEntry("fragment", "rea");
        assertThat(engine.getCurrent().getState()).containsEntry("power", 8d);
        assertThat(engine.getCurrent().getState()).containsEntry("replacement", "baak");
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
