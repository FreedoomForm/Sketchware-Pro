package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreatorRuntimeEngineTest {

    @Test public void userAndAiOperationsShareTheSameVersionedPipeline() {
        CreatorRuntimeEngine engine = newEngine();

        CreatorApplyResult screen = engine.apply(operation("op-screen", 0,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root", "rootWidgetType", "column")));
        CreatorApplyResult widget = engine.apply(operation("op-widget", 1,
                CreatorProjectOperation.ActorKind.AI, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "title", "widgetType", "text", "parentId", "root",
                        "properties", map("text", "Welcome"))));
        CreatorApplyResult property = engine.apply(operation("op-property", 2,
                CreatorProjectOperation.ActorKind.AI, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY,
                map("widgetId", "title", "property", "text", "value", "Hello")));

        assertThat(screen.isApplied()).isTrue();
        assertThat(widget.isApplied()).isTrue();
        assertThat(property.isApplied()).isTrue();
        assertThat(engine.getCurrent().getRevision()).isEqualTo(3L);
        assertThat(engine.getCurrent().getWidgets().get("root").getChildren()).containsExactly("title");
        assertThat(engine.getCurrent().getWidgets().get("title").getProperties().get("text")).isEqualTo("Hello");
        assertThat(engine.getEventLog().snapshot()).hasSize(6);
    }

    @Test public void operationIdIsIdempotentAndDoesNotCreateASecondRevision() {
        CreatorRuntimeEngine engine = newEngine();
        CreatorProjectOperation operation = operation("op-screen", 0,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root"));

        CreatorApplyResult first = engine.apply(operation);
        CreatorApplyResult replay = engine.apply(operation);

        assertThat(first.isApplied()).isTrue();
        assertThat(replay.isApplied()).isTrue();
        assertThat(replay.isReplayed()).isTrue();
        assertThat(engine.getCurrent().getRevision()).isEqualTo(1L);
    }

    @Test public void staleOperationIsRejectedWithoutMutatingTheDocument() {
        CreatorRuntimeEngine engine = newEngine();
        engine.apply(operation("op-screen", 0, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));

        CreatorApplyResult stale = engine.apply(operation("op-stale", 0, CreatorProjectOperation.ActorKind.AI,
                CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE, map("label", "Open editor")));

        assertThat(stale.isApplied()).isFalse();
        assertThat(stale.getValidation().getCode()).isEqualTo(CreatorValidationResult.Code.STALE_REVISION);
        assertThat(engine.getCurrent().getRevision()).isEqualTo(1L);
        assertThat(engine.getCurrent().getEntryControl().getLabel()).isEqualTo("Continue");
    }

    @Test public void projectCannotDisableHostRecoveryThroughTheEntryControlOperation() {
        CreatorRuntimeEngine engine = newEngine();

        CreatorApplyResult result = engine.apply(operation("op-unsafe", 0,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE,
                map("shakeRecoveryEnabled", false, "visible", false)));

        assertThat(result.isApplied()).isFalse();
        assertThat(result.getValidation().getCode()).isEqualTo(CreatorValidationResult.Code.SAFETY_VIOLATION);
        assertThat(engine.getCurrent().getRevision()).isEqualTo(0L);
    }

    @Test public void unsupportedWidgetTypesAreRejectedForBothUserAndAiOperations() {
        CreatorRuntimeEngine engine = newEngine();
        engine.apply(operation("op-screen", 0, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        CreatorApplyResult rejected = engine.apply(operation("op-unknown", 1,
                CreatorProjectOperation.ActorKind.AI, CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "unknown", "widgetType", "unreviewed_native_widget", "parentId", "root")));

        assertThat(rejected.isApplied()).isFalse();
        assertThat(rejected.getValidation().getCode()).isEqualTo(CreatorValidationResult.Code.INVALID_PAYLOAD);
        assertThat(engine.getCurrent().getRevision()).isEqualTo(1L);
    }

    @Test public void restoreCreatesANewRevisionFromAnAvailableHistoricalSnapshot() {
        CreatorRuntimeEngine engine = newEngine();
        engine.apply(operation("op-screen", 0, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(operation("op-widget", 1, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.WIDGET_ADD,
                map("widgetId", "title", "widgetType", "text", "parentId", "root")));

        CreatorApplyResult restored = engine.apply(operation("op-restore", 2,
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.REVISION_RESTORE,
                map("targetRevision", 0)));

        assertThat(restored.isApplied()).isTrue();
        assertThat(engine.getCurrent().getRevision()).isEqualTo(3L);
        assertThat(engine.getCurrent().getScreens()).isEmpty();
        assertThat(engine.getCurrent().getWidgets()).isEmpty();
    }

    @Test public void historyListsRestorableRevisionsNewestFirst() {
        CreatorRuntimeEngine engine = newEngine();
        engine.apply(operation("op-screen", 0, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.SCREEN_CREATE,
                map("screenId", "home", "route", "/", "rootWidgetId", "root")));
        engine.apply(operation("op-entry", 1, CreatorProjectOperation.ActorKind.USER,
                CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE, map("label", "Open")));

        assertThat(engine.getRevisionStore().getAvailableRevisions()).containsExactly(2L, 1L, 0L).inOrder();
    }

    @Test public void diagnosticLogRedactsSensitiveContentBeforeItIsStored() {
        CreatorRuntimeEventLog log = new CreatorRuntimeEventLog(2);
        log.append(new CreatorRuntimeEvent(1, "project", 0, "ai", "ai.operation_requested",
                CreatorRuntimeEvent.Severity.INFO, "op", map("operationType", "widget.add",
                        "promptText", "private instruction", "apiToken", "secret-value")));

        List<CreatorRuntimeEvent> events = log.snapshot();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAttributes().get("operationType")).isEqualTo("widget.add");
        assertThat(events.get(0).getAttributes().get("promptText")).isEqualTo("[redacted]");
        assertThat(events.get(0).getAttributes().get("apiToken")).isEqualTo("[redacted]");
    }

    private static CreatorRuntimeEngine newEngine() {
        return new CreatorRuntimeEngine(CreatorProjectDocument.empty("project", "Demo"), 12,
                new CreatorRuntimeEventLog(50));
    }

    private static CreatorProjectOperation operation(String id, long baseRevision,
                                                      CreatorProjectOperation.ActorKind actor,
                                                      CreatorProjectOperation.Type type,
                                                      Map<String, Object> payload) {
        return new CreatorProjectOperation(id, "project", baseRevision, actor, type, payload, 1L);
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
