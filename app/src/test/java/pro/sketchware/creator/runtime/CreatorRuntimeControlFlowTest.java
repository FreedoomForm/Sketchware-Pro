package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CreatorRuntimeControlFlowTest {
    @Test public void executesOnlyTheSelectedStateEqualityBranch() {
        CreatorRuntimeEngine engine = engineWithBranch("approved");

        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(engine, "button", "click");

        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getValue()).isEqualTo("Approved");
    }

    @Test public void serializesNestedTypedBranches() {
        CreatorProjectDocument source = engineWithBranch("pending").getCurrent();

        CreatorProjectDocument restored = CreatorProjectDocumentCodec.decode(CreatorProjectDocumentCodec.encode(source));
        CreatorRuntimeBlock branch = restored.getEvents().get("tap").getBlocks().get(0);

        assertThat(branch.getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_STATE_EQUALS);
        assertThat(branch.getThenBlocks()).hasSize(1);
        assertThat(branch.getElseBlocks()).hasSize(1);
        assertThat(new CreatorRuntimeExecutor().dispatch(new CreatorRuntimeEngine(restored, 10,
                new CreatorRuntimeEventLog(10)), "button", "click").get(0).getValue()).isEqualTo("Pending");
    }

    @Test public void emitsTypedNavigateEffectForAiVisibleBlock() {
        CreatorRuntimeEngine engine = engineWithBranch("approved");
        CreatorRuntimeBlock navigate = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.NAVIGATE,
                map("screenId", "settings"));
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("navigate", new CreatorEventBinding("navigate", "button", "click",
                Collections.singletonList(navigate)));
        CreatorProjectDocument source = engine.getCurrent();
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                source.getProjectId(), source.getRevision(), source.getName(), source.getEntryScreenId(),
                source.getScreens(), source.getWidgets(), source.getEntryControl(), source.getState(), events);
        List<CreatorRuntimeExecutor.Effect> effects = new CreatorRuntimeExecutor().dispatch(
                new CreatorRuntimeEngine(document, 10, new CreatorRuntimeEventLog(10)), "button", "click");
        assertThat(effects).hasSize(1);
        assertThat(effects.get(0).getType()).isEqualTo("navigate");
        assertThat(effects.get(0).getValue()).isEqualTo("settings");
    }

    private static CreatorRuntimeEngine engineWithBranch(String status) {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null, Arrays.asList("button"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root", null, null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        CreatorRuntimeBlock branch = new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_STATE_EQUALS,
                map("stateId", "status", "equals", "approved"),
                Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "Approved"))),
                Collections.singletonList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "Pending"))));
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("tap", new CreatorEventBinding("tap", "button", "click", Collections.singletonList(branch)));
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                "project", 0, "Demo", "home", screens, widgets, CreatorEntryControl.defaultControl(),
                map("status", status), events);
        return new CreatorRuntimeEngine(document, 10, new CreatorRuntimeEventLog(10));
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
