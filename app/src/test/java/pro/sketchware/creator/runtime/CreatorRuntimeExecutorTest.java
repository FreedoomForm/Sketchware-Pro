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

    @Test public void repeatExecutesBodyWithinTypedExecutionBudget() {
        CreatorRuntimeEngine engine = engineWithButton();
        CreatorRuntimeBlock repeat = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.REPEAT,
                map("count", "3"),
                java.util.Collections.singletonList(new CreatorRuntimeBlock(
                        CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "tick"))),
                java.util.Collections.<CreatorRuntimeBlock>emptyList());
        attach(engine, java.util.Collections.singletonList(repeat), 2);

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(effects).hasSize(3);
        assertThat(effects.get(2).getValue()).isEqualTo("tick");
    }

    @Test public void foreverStopsAtBreakInsteadOfRunningIndefinitely() {
        CreatorRuntimeEngine engine = engineWithButton();
        CreatorRuntimeBlock forever = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.FOREVER,
                java.util.Collections.<String, Object>emptyMap(),
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "once")),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.BREAK, java.util.Collections.<String, Object>emptyMap())),
                java.util.Collections.<CreatorRuntimeBlock>emptyList());
        attach(engine, java.util.Collections.singletonList(forever), 2);

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getValue()).isEqualTo("once");
    }

    @Test public void typedDataOperationsMutateMapsAndListsWithoutSourceExecution() {
        CreatorRuntimeEngine engine = engineWithButton();
        engine.apply(op("map", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "scores", "value", new LinkedHashMap<String, Object>())));
        engine.apply(op("list", 3, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "keys", "value", new java.util.ArrayList<Object>())));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.DATA_OPERATION,
                        map("operation", "map_put", "target", "scores", "key", "\"alice\"", "value", "7")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.DATA_OPERATION,
                        map("operation", "map_keys", "source", "scores", "target", "keys")),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.DATA_OPERATION,
                        map("operation", "list_add", "target", "keys", "value", "\"tail\"")));
        attach(engine, blocks, 4);

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("scores")).isEqualTo(
                Collections.<String, Object>singletonMap("alice", 7.0d));
        @SuppressWarnings("unchecked") List<Object> keys = (List<Object>) engine.getCurrent().getState().get("keys");
        assertThat(keys).containsExactly("alice", "tail").inOrder();
    }

    @Test public void serviceArgumentsResolveStateReferencesBeforeDispatch() {
        final Map<String, Object> received = new LinkedHashMap<>();
        CreatorRuntimeServiceDispatcher services = new CreatorRuntimeServiceDispatcher().register(new CreatorRuntimeService() {
            @Override public String getId() { return "capture"; }
            @Override public Result execute(Map<String, Object> arguments) {
                received.putAll(arguments);
                return new Result(Status.SUCCEEDED, java.util.Collections.<String, Object>emptyMap(), null);
            }
        });
        CreatorRuntimeEngine engine = engineWithButton();
        engine.apply(op("state", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "url", "value", "https://example.test")));
        Map<String, Object> args = map("url", "state:url", "nested", map("token", "@url"));
        attachWithServices(engine, new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                map("serviceId", "capture", "arguments", args)), services, 3);

        new CreatorRuntimeExecutor(services).dispatch(engine, "button", "click");

        assertThat(received.get("url")).isEqualTo("https://example.test");
        @SuppressWarnings("unchecked") Map<String, Object> nested = (Map<String, Object>) received.get("nested");
        assertThat(nested.get("token")).isEqualTo("https://example.test");
    }

    @Test public void typedExpressionsAndIncrementUpdateRuntimeState() {
        CreatorRuntimeEngine engine = engineWithButton();
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.STATE_INCREMENT, map("stateId", "counter", "delta", 1)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "counter", "expression", "counter + 2")));
        attach(engine, blocks, 2);

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("counter")).isEqualTo(3.0d);
    }

    @Test public void typedConditionChoosesThenBranchUsingRuntimeState() {
        CreatorRuntimeEngine engine = engineWithButton();
        engine.apply(op("state", 2, CreatorProjectOperation.Type.STATE_SET, map("stateId", "status", "value", "approved")));
        Map<String, Object> condition = map("operator", "equals", "left", "state:status", "right", "approved");
        CreatorRuntimeBlock branch = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_CONDITION, condition,
                java.util.Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "yes"))),
                java.util.Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "no"))));
        attach(engine, java.util.Collections.singletonList(branch), 3);

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getValue()).isEqualTo("yes");
    }

    private static CreatorRuntimeEngine engineWithButton() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        return engine;
    }

    private static void attachWithServices(CreatorRuntimeEngine engine, CreatorRuntimeBlock block,
                                            CreatorRuntimeServiceDispatcher services, long revision) {
        engine.apply(op("event-service-" + revision, revision, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_service_" + revision, "targetWidgetId", "button", "eventName", "click",
                        "blocks", java.util.Collections.singletonList(block))));
    }

    private static void attach(CreatorRuntimeEngine engine, List<CreatorRuntimeBlock> blocks, long revision) {
        engine.apply(op("event-" + revision, revision, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click_" + revision, "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));
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
