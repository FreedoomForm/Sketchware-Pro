package pro.sketchware.creator.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Queues native builds against an immutable Creator Runtime revision. It never
 * blocks the live preview and it reports every lifecycle state to diagnostics.
 */
public final class CreatorNativeBuildQueue {
    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED }

    public interface BuildRunner { void build(CreatorProjectDocument pinnedRevision) throws Exception; }
    public interface Listener { void onStatus(String buildId, Status status, String detail); }

    private final Executor executor;
    private final CreatorRuntimeEventLog eventLog;
    private final BuildRunner runner;

    public CreatorNativeBuildQueue(Executor executor, CreatorRuntimeEventLog eventLog, BuildRunner runner) {
        if (executor == null || runner == null) throw new IllegalArgumentException("executor and runner are required");
        this.executor = executor;
        this.eventLog = eventLog == null ? new CreatorRuntimeEventLog(100) : eventLog;
        this.runner = runner;
    }

    public String enqueue(CreatorProjectDocument document, Listener listener) {
        if (document == null) throw new IllegalArgumentException("document");
        final String buildId = "build-" + UUID.randomUUID();
        emit(document, buildId, "build.queued", CreatorRuntimeEvent.Severity.INFO, Status.QUEUED, null);
        if (listener != null) listener.onStatus(buildId, Status.QUEUED, null);
        executor.execute(() -> {
            emit(document, buildId, "build.started", CreatorRuntimeEvent.Severity.INFO, Status.RUNNING, null);
            if (listener != null) listener.onStatus(buildId, Status.RUNNING, null);
            try {
                runner.build(document);
                emit(document, buildId, "build.completed", CreatorRuntimeEvent.Severity.INFO, Status.SUCCEEDED, null);
                if (listener != null) listener.onStatus(buildId, Status.SUCCEEDED, null);
            } catch (Exception error) {
                String detail = error.getClass().getSimpleName();
                emit(document, buildId, "build.failed", CreatorRuntimeEvent.Severity.ERROR, Status.FAILED, detail);
                if (listener != null) listener.onStatus(buildId, Status.FAILED, detail);
            }
        });
        return buildId;
    }

    private void emit(CreatorProjectDocument document, String buildId, String name,
                      CreatorRuntimeEvent.Severity severity, Status status, String errorCode) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("buildId", buildId);
        attributes.put("status", status.name());
        attributes.put("sourceRevision", document.getRevision());
        if (errorCode != null) attributes.put("errorCode", errorCode);
        eventLog.append(new CreatorRuntimeEvent(System.currentTimeMillis(), document.getProjectId(), document.getRevision(),
                "build", name, severity, buildId, attributes));
    }
}
