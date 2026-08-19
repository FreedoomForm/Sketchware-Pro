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

    @Test public void executesForeverUntilBreakThenContinuesTheOuterBlockChain() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        List<CreatorRuntimeBlock> foreverBody = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE, map("stateId", "count", "delta", 1L)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.BREAK, Collections.<String, Object>emptyMap()));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.FOREVER, Collections.<String, Object>emptyMap(),
                        foreverBody, Collections.<CreatorRuntimeBlock>emptyList()),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE, map("stateId", "count", "delta", 1L)));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("count")).isEqualTo(2L);
        assertThat(effects).isEmpty();
    }

    @Test public void boundsForeverWithoutBreakAtTenThousandIterations() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 10_020,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeBlock increment = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                map("stateId", "count", "delta", 1L));
        CreatorRuntimeBlock forever = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.FOREVER,
                Collections.<String, Object>emptyMap(), Collections.singletonList(increment),
                Collections.<CreatorRuntimeBlock>emptyList());
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click",
                        "blocks", Collections.singletonList(forever))));

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("count")).isEqualTo(10_000L);
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getType()).isEqualTo("forever");
        assertThat(effects.get(0).getValue()).isEqualTo("capped:10000");
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

    @Test public void evaluatesCanonicalListAndMapReportersAndMutatesMapRowWithoutGeneratedJava() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("numbers", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "numbers", "value", Arrays.asList(7d, 9d))));
        engine.apply(op("names", 3, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "names", "value", Arrays.asList("Ada", "Lin"))));
        engine.apply(op("rows", 4, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "rows", "value", Collections.singletonList(map("name", "Ada", "age", "42")))));
        engine.apply(op("profile", 5, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "profile", "value", map("city", "Tashkent", "score", 2d))));
        Map<String, Object> numberAt = reporter("getatlistint", literal("1"), literal("numbers"));
        Map<String, Object> nameAt = reporter("getatliststr", literal("0"), literal("names"));
        Map<String, Object> mapAt = reporter("getatlistmap", literal("0"), literal("name"), literal("rows"));
        Map<String, Object> containsRowKey = reporter("containlistmap", literal("rows"), literal("0"), literal("name"));
        Map<String, Object> listSize = reporter("lengthlist", literal("names"));
        Map<String, Object> mapValue = reporter("mapget", literal("profile"), literal("city"));
        Map<String, Object> containsMapKey = reporter("mapcontainkey", literal("profile"), literal("score"));
        Map<String, Object> mapSize = reporter("mapsize", literal("profile"));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "numberAt", "expression", numberAt)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "nameAt", "expression", nameAt)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "mapAt", "expression", mapAt)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "containsRowKey", "expression", containsRowKey)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "listSize", "expression", listSize)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "mapValue", "expression", mapValue)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "containsMapKey", "expression", containsMapKey)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "mapSize", "expression", mapSize)),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE,
                        map("stateId", "rows", "action", "map_put_at", "key", "age", "value", "43", "index", 0)));
        engine.apply(op("event", 6, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("numberAt")).isEqualTo(9d);
        assertThat(engine.getCurrent().getState().get("nameAt")).isEqualTo("Ada");
        assertThat(engine.getCurrent().getState().get("mapAt")).isEqualTo("Ada");
        assertThat(engine.getCurrent().getState().get("containsRowKey")).isEqualTo(true);
        assertThat(engine.getCurrent().getState().get("listSize")).isEqualTo(2d);
        assertThat(engine.getCurrent().getState().get("mapValue")).isEqualTo("Tashkent");
        assertThat(engine.getCurrent().getState().get("containsMapKey")).isEqualTo(true);
        assertThat(engine.getCurrent().getState().get("mapSize")).isEqualTo(2d);
        @SuppressWarnings("unchecked") List<Map<String, Object>> rows =
                (List<Map<String, Object>>) (List<?>) engine.getCurrent().getState().get("rows");
        assertThat(rows.get(0)).containsExactly("name", "Ada", "age", "43");
    }

    @Test public void evaluatesCalendarTimeFormatAndDiffReportersThroughRuntimeNativeService() {
        CreatorRuntimeServiceDispatcher services = new CreatorRuntimeServiceDispatcher().register(new CreatorCalendarService());
        services.dispatch("calendar", map("componentId", "start", "action", "set_time", "timestamp", 20_000L));
        services.dispatch("calendar", map("componentId", "end", "action", "set_time", "timestamp", 5_000L));
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "time", "expression", reporter("calendargettime", literal("start")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "formatted", "expression", reporter("calendarformat", literal("start"), literal("HH:mm:ss.SSS")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "diff", "expression", reporter("calendardiff", literal("start"), literal("end")))));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor(services).dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("time")).isEqualTo(20_000L);
        assertThat(engine.getCurrent().getState().get("formatted")).isEqualTo(
                new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(new java.util.Date(20_000L)));
        assertThat(engine.getCurrent().getState().get("diff")).isEqualTo(15_000d);
    }

    @Test public void evaluatesCanonicalWidgetAndStateGetterReportersFromTypedRuntimeProperties() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 40,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("widget", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "widget", "widgetType", "text", "parentId", "root")));
        String[] properties = {"text", "enabled", "checked", "alpha", "rotation", "translationX", "translationY", "scaleX", "scaleY", "max", "progress", "selectedIndex"};
        Object[] values = {"Creator", false, true, 0.5d, 45d, 3d, 4d, 1.5d, 2d, 80d, 12d, 2d};
        for (int index = 0; index < properties.length; index++) {
            engine.apply(op("property" + index, index + 2, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY,
                    map("widgetId", "widget", "property", properties[index], "value", values[index])));
        }
        engine.apply(op("answer", 14, CreatorProjectOperation.Type.STATE_SET, map("stateId", "answer", "value", "42")));
        String[] opCodes = {"gettext", "getenable", "getchecked", "getalpha", "getrotate", "gettranslationx", "gettranslationy", "getscalex", "getscaley", "seekbargetmax", "seekbargetprogress", "spngetselection", "getvar"};
        List<CreatorRuntimeBlock> blocks = new java.util.ArrayList<>();
        for (String opCode : opCodes) {
            String argument = "getvar".equals(opCode) ? "answer" : "widget";
            blocks.add(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                    map("stateId", opCode, "expression", reporter(opCode, literal(argument)))));
        }
        engine.apply(op("event", 15, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "widget_click", "targetWidgetId", "widget", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "widget", "click");

        assertThat(engine.getCurrent().getState().get("gettext")).isEqualTo("Creator");
        assertThat(engine.getCurrent().getState().get("getenable")).isEqualTo(false);
        assertThat(engine.getCurrent().getState().get("getchecked")).isEqualTo(true);
        assertThat(engine.getCurrent().getState().get("getalpha")).isEqualTo(0.5d);
        assertThat(engine.getCurrent().getState().get("getrotate")).isEqualTo(45d);
        assertThat(engine.getCurrent().getState().get("gettranslationx")).isEqualTo(3d);
        assertThat(engine.getCurrent().getState().get("gettranslationy")).isEqualTo(4d);
        assertThat(engine.getCurrent().getState().get("getscalex")).isEqualTo(1.5d);
        assertThat(engine.getCurrent().getState().get("getscaley")).isEqualTo(2d);
        assertThat(engine.getCurrent().getState().get("seekbargetmax")).isEqualTo(80d);
        assertThat(engine.getCurrent().getState().get("seekbargetprogress")).isEqualTo(12d);
        assertThat(engine.getCurrent().getState().get("spngetselection")).isEqualTo(2d);
        assertThat(engine.getCurrent().getState().get("getvar")).isEqualTo("42");
    }

    @Test public void evaluatesExtendedCollectionAccessorsAndSetAtMutationWithoutGeneratedJava() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("names", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "names", "value", Arrays.asList("Ada", "Lin"))));
        engine.apply(op("rows", 3, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "rows", "value", Collections.singletonList(map("name", "Ada")))));
        engine.apply(op("profile", 4, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "profile", "value", map("score", 4d, "active", true, "city", "Tashkent",
                        "meta", map("rank", "gold"), "labels", Arrays.asList("one", "two")))));
        List<CreatorRuntimeBlock> blocks = Arrays.asList(
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "row", "expression", reporter("getmapatposlistmap", literal("0"), literal("rows")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "score", "expression", reporter("hashmapgetnumber", literal("profile"), literal("score")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "active", "expression", reporter("hashmapgetboolean", literal("profile"), literal("active")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "meta", "expression", reporter("hashmapgetmap", literal("profile"), literal("meta")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "labels", "expression", reporter("hashmapliststr", literal("profile"), literal("labels")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                        map("stateId", "hasCity", "expression", reporter("mapcontainvalue", literal("profile"), literal("Tashkent")))),
                new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE,
                        map("stateId", "names", "action", "set_at", "value", "Grace", "index", 1)));
        engine.apply(op("event", 5, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", blocks)));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) engine.getCurrent().getState().get("row");
        assertThat(row).containsExactly("name", "Ada");
        assertThat(engine.getCurrent().getState().get("score")).isEqualTo(4d);
        assertThat(engine.getCurrent().getState().get("active")).isEqualTo(true);
        @SuppressWarnings("unchecked") Map<String, Object> meta = (Map<String, Object>) engine.getCurrent().getState().get("meta");
        assertThat(meta).containsExactly("rank", "gold");
        assertThat((List<?>) engine.getCurrent().getState().get("labels")).containsExactly("one", "two").inOrder();
        assertThat(engine.getCurrent().getState().get("hasCity")).isEqualTo(true);
        assertThat((List<?>) engine.getCurrent().getState().get("names")).containsExactly("Ada", "Grace").inOrder();
    }

    @Test public void evaluatesFirebasePushKeyReporterThroughItsImportedComponentBasePath() {
        final Map<String, Object> captured = new LinkedHashMap<>();
        CreatorRuntimeService firebase = new CreatorRuntimeService() {
            @Override public String getId() { return "firebase"; }
            @Override public Result execute(Map<String, Object> arguments) {
                captured.putAll(arguments);
                return new Result(Status.SUCCEEDED, map("key", "generated-key"), null);
            }
        };
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 30,
                new CreatorRuntimeEventLog(30));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("components", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "legacy.components", "value", map("firebase1", map("param1", "/users/")))));
        CreatorRuntimeBlock setKey = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                map("stateId", "pushKey", "expression", reporter("firebasegetpushkey", literal("firebase1"))));
        engine.apply(op("event", 3, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click",
                        "blocks", Collections.singletonList(setKey))));

        new CreatorRuntimeExecutor(new CreatorRuntimeServiceDispatcher().register(firebase)).dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("pushKey")).isEqualTo("generated-key");
        assertThat(captured).containsEntry("action", "push_key");
        assertThat(captured).containsEntry("componentId", "firebase1");
        assertThat(captured).containsEntry("path", "users");
    }

    @Test public void evaluatesObjectAnimatorIsRunningReporterThroughRuntimeNativeService() {
        CreatorRuntimeService animator = new CreatorRuntimeService() {
            @Override public String getId() { return "animator"; }
            @Override public Result execute(Map<String, Object> arguments) {
                return new Result(Status.SUCCEEDED, map("value", true), null);
            }
        };
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeBlock setState = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                map("stateId", "running", "expression", reporter("objectanimatorisrunning", literal("animator1"))));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click",
                        "blocks", Collections.singletonList(setState))));

        new CreatorRuntimeExecutor(new CreatorRuntimeServiceDispatcher().register(animator)).dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("running")).isEqualTo(true);
    }

    @Test public void evaluatesGetResStrFromImportedTypedStringResourceState() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        engine.apply(op("resources", 2, CreatorProjectOperation.Type.STATE_SET,
                map("stateId", "legacy.stringResources", "value", map("", map("welcome", "Creator Runtime")))));
        CreatorRuntimeBlock setText = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                map("stateId", "message", "expression", reporter("getresstr", literal("welcome"))));
        engine.apply(op("event", 3, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click",
                        "blocks", Collections.singletonList(setText))));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState().get("message")).isEqualTo("Creator Runtime");
    }

    @Test public void evaluatesRandomReporterWithinLegacyInclusiveIntegerBounds() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeBlock setState = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                map("stateId", "value", "expression", reporter("random", literal("5"), literal("9"))));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click",
                        "blocks", Collections.singletonList(setState))));

        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        double value = ((Number) engine.getCurrent().getState().get("value")).doubleValue();
        assertThat(value).isAtLeast(5d);
        assertThat(value).isAtMost(9d);
        assertThat(value).isEqualTo(Math.rint(value));
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

    @Test public void storesConfiguredServiceOutputInTypedRuntimeState() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeService service = new CreatorRuntimeService() {
            @Override public String getId() { return "devices"; }
            @Override public Result execute(Map<String, Object> ignored) { return CreatorRuntimeServiceArguments.succeeded("items", Arrays.asList("one", "two")); }
        };
        CreatorRuntimeServiceDispatcher dispatcher = new CreatorRuntimeServiceDispatcher().register(service);
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks",
                        Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "devices", "arguments", map("resultStateId", "paired", "resultKey", "items")))))));

        new CreatorRuntimeExecutor(dispatcher).dispatch(engine, "button", "click");

        assertThat((java.util.List<?>) engine.getCurrent().getState().get("paired")).containsExactly("one", "two").inOrder();
    }

    @Test public void evaluatesMediaPlayerGetterReportersThroughRuntimeNativeMediaService() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeService media = new CreatorRuntimeService() {
            @Override public String getId() { return "media"; }
            @Override public Result execute(Map<String, Object> arguments) {
                if ("duration".equals(arguments.get("action"))) return CreatorRuntimeServiceArguments.succeeded("value", 4200L);
                if ("is_playing".equals(arguments.get("action"))) return CreatorRuntimeServiceArguments.succeeded("value", true);
                return CreatorRuntimeServiceArguments.invalid("unsupported");
            }
        };
        Map<String, Object> duration = map("kind", "reporter", "opCode", "mediaplayergetduration", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "music")));
        Map<String, Object> playing = map("kind", "reporter", "opCode", "mediaplayerisplaying", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "music")));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "duration", "expression", duration)),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "playing", "expression", playing))))));

        new CreatorRuntimeExecutor(new CreatorRuntimeServiceDispatcher().register(media)).dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState()).containsEntry("duration", 4200L);
        assertThat(engine.getCurrent().getState()).containsEntry("playing", true);
    }

    @Test public void evaluatesFileUtilReporterQueriesThroughRuntimeNativeFileService() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(CreatorProjectDocument.empty("p", "Demo"), 20,
                new CreatorRuntimeEventLog(20));
        engine.apply(op("screen", 0, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(op("button", 1, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "button", "widgetType", "button", "parentId", "root")));
        CreatorRuntimeService file = new CreatorRuntimeService() {
            @Override public String getId() { return "file"; }
            @Override public Result execute(Map<String, Object> arguments) {
                String action = String.valueOf(arguments.get("action"));
                if ("read".equals(action)) return CreatorRuntimeServiceArguments.succeeded("content", "runtime");
                if ("exists".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", true);
                if ("length".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", 7L);
                return CreatorRuntimeServiceArguments.invalid("unsupported");
            }
        };
        Map<String, Object> content = map("kind", "reporter", "opCode", "fileutilread", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "/tmp/runtime.txt")));
        Map<String, Object> exists = map("kind", "reporter", "opCode", "fileutilisexist", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "/tmp/runtime.txt")));
        Map<String, Object> length = map("kind", "reporter", "opCode", "fileutillength", "arguments",
                Collections.singletonList(map("kind", "literal", "value", "/tmp/runtime.txt")));
        engine.apply(op("event", 2, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "button_click", "targetWidgetId", "button", "eventName", "click", "blocks", Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "content", "expression", content)),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "exists", "expression", exists)),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, map("stateId", "length", "expression", length))))));

        new CreatorRuntimeExecutor(new CreatorRuntimeServiceDispatcher().register(file)).dispatch(engine, "button", "click");

        assertThat(engine.getCurrent().getState()).containsEntry("content", "runtime");
        assertThat(engine.getCurrent().getState()).containsEntry("exists", true);
        assertThat(engine.getCurrent().getState()).containsEntry("length", 7L);
    }

    private static CreatorProjectOperation op(String id, long revision, CreatorProjectOperation.Type type, Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "p", revision, CreatorProjectOperation.ActorKind.USER, type, payload, 0);
    }
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }

    private static Map<String, Object> literal(String value) {
        return map("kind", "literal", "value", value);
    }

    private static Map<String, Object> reporter(String opCode, Map<String, Object>... arguments) {
        return map("kind", "reporter", "opCode", opCode, "arguments", Arrays.asList(arguments));
    }
}
