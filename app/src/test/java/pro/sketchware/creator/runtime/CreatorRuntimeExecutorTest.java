package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
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

    private static CreatorProjectOperation op(String id, long revision, CreatorProjectOperation.Type type, Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "p", revision, CreatorProjectOperation.ActorKind.USER, type, payload, 0);
    }
    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
