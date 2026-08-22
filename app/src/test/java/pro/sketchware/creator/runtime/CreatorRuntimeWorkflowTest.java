package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/** Validates the first complete live-edit loop without depending on Android UI. */
public class CreatorRuntimeWorkflowTest {

    @Test public void userAndAiCanCreatePreviewableProjectAndPreserveRecoveryGuarantees() {
        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(
                CreatorProjectDocument.empty("project", "Demo"), 20, new CreatorRuntimeEventLog(50));

        CreatorApplyResult screen = engine.apply(operation("user-screen", 0,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.checkpoint("empty-home");

        JsonObject aiArgs = new JsonObject();
        aiArgs.addProperty("action", "add_widget");
        aiArgs.addProperty("widget_id", "continue_button");
        aiArgs.addProperty("widget_type", "button");
        aiArgs.addProperty("parent_id", "root");
        JsonObject properties = new JsonObject();
        properties.addProperty("text", "Continue");
        aiArgs.add("properties", properties);
        CreatorProjectOperation aiOperation = CreatorRuntimeOperationMapper.map(aiArgs, engine.getCurrent(),
                CreatorProjectOperation.ActorKind.AI);
        CreatorApplyResult aiWidget = engine.apply(aiOperation);

        CreatorRuntimeBlock openEditor = new CreatorRuntimeBlock(
                CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                map("serviceId", "intent", "arguments", map(
                        "intentId", CreatorRuntimeDefaults.EDITOR_INTENT_ID,
                        "action", "open_creator_editor")));
        CreatorApplyResult entryBehavior = engine.apply(operation("user-entry", 2,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.EVENT_ATTACH,
                map("bindingId", "continue-click", "targetWidgetId", "continue_button",
                        "eventName", "click", "blocks", java.util.Collections.singletonList(openEditor))));
        CreatorCompatibilityAnalyzer analyzer = new CreatorCompatibilityAnalyzer(CreatorRuntimeServiceCatalog.defaults());

        assertThat(screen.isApplied()).isTrue();
        assertThat(aiWidget.isApplied()).isTrue();
        assertThat(entryBehavior.isApplied()).isTrue();
        assertThat(engine.getCurrent().getEntryScreenId()).isEqualTo("home");
        assertThat(engine.getCurrent().getWidgets().get("root").getChildren()).containsExactly("continue_button");
        assertThat(engine.getCurrent().getEvents().get("continue-click").getTargetWidgetId())
                .isEqualTo("continue_button");
        assertThat(engine.getRevisionStore().getCheckpointRevision("empty-home")).isEqualTo(1L);
        assertThat(analyzer.classify("service:camera")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("java:CustomActivity")).isEqualTo(CreatorCompatibilityTier.R0_UNSUPPORTED);
        assertThat(engine.getCurrent().getEntryControl().getLabel()).isEqualTo("Continue");
        assertThat(engine.getEventLog().snapshot()).isNotEmpty();
    }

    @Test public void validatesActivityAndImportedComponentEventTargets() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null,
                java.util.Collections.<String>emptyList(), null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("main", new CreatorScreen("main", "/", "root"));
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("timer1", map("serviceId", "timer"));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("legacy.components", components);
        CreatorProjectDocument document = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "project", 0, "Demo", "main",
                screens, widgets, CreatorEntryControl.defaultControl(), state,
                java.util.Collections.<String, CreatorEventBinding>emptyMap());

        CreatorRuntimeBlock message = new CreatorRuntimeBlock(
                CreatorRuntimeBlock.Type.SHOW_MESSAGE, map("message", "ok"));
        Map<String, Object> activityPayload = map("bindingId", "activity-resume",
                "targetWidgetId", CreatorLegacyArtifactImporter.ACTIVITY_EVENT_TARGET,
                "eventName", "resume", "blocks", java.util.Collections.singletonList(message));
        Map<String, Object> componentPayload = map("bindingId", "timer-tick",
                "targetWidgetId", "timer1", "eventName", "tick",
                "blocks", java.util.Collections.singletonList(message));

        assertThat(CreatorOperationValidator.validate(document, operation("activity", 0,
                CreatorProjectOperation.ActorKind.AI, CreatorProjectOperation.Type.EVENT_ATTACH,
                activityPayload)).isOk()).isTrue();
        assertThat(CreatorOperationValidator.validate(document, operation("component", 0,
                CreatorProjectOperation.ActorKind.AI, CreatorProjectOperation.Type.EVENT_ATTACH,
                componentPayload)).isOk()).isTrue();
    }

    private static CreatorProjectOperation operation(String id, long revision,
                                                      CreatorProjectOperation.ActorKind actor,
                                                      CreatorProjectOperation.Type type,
                                                      Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "project", revision, actor, type, payload, 1L);
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
