package pro.sketchware.creator.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single authoritative mutation path for manual and AI Creator Runtime edits.
 * Android UI, agent adapters, and future importers all submit operations here.
 */
public final class CreatorRuntimeEngine {
    private final CreatorRevisionStore revisions;
    private final CreatorRuntimeEventLog eventLog;

    public CreatorRuntimeEngine(CreatorProjectDocument initial, int historyCapacity,
                                CreatorRuntimeEventLog eventLog) {
        this.revisions = new CreatorRevisionStore(initial, historyCapacity);
        this.eventLog = eventLog == null ? new CreatorRuntimeEventLog(200) : eventLog;
    }

    public synchronized CreatorApplyResult apply(CreatorProjectOperation operation) {
        CreatorProjectDocument current = revisions.getCurrent();
        CreatorApplyResult known = revisions.getOperationResult(operation.getOperationId());
        if (known != null) {
            log(current, "operation", "operation.replayed", CreatorRuntimeEvent.Severity.INFO, operation,
                    attributes("operationType", operation.getType().name()));
            return known.asReplayed();
        }
        log(current, "operation", "operation.requested", CreatorRuntimeEvent.Severity.DEBUG, operation,
                attributes("operationType", operation.getType().name(), "actor", operation.getActorKind().name()));
        CreatorValidationResult validation = CreatorOperationValidator.validate(current, operation);
        if (!validation.isOk()) {
            CreatorApplyResult rejected = CreatorApplyResult.rejected(validation, current);
            revisions.commit(operation, current, rejected);
            log(current, "operation", "operation.rejected", CreatorRuntimeEvent.Severity.WARNING, operation,
                    attributes("operationType", operation.getType().name(), "code", validation.getCode().name()));
            return rejected;
        }
        CreatorProjectDocument next;
        if (operation.getType() == CreatorProjectOperation.Type.REVISION_RESTORE) {
            long targetRevision = ((Number) operation.getPayload().get("targetRevision")).longValue();
            CreatorProjectDocument target = revisions.getRevision(targetRevision);
            if (target == null) {
                CreatorValidationResult rejectedValidation = CreatorValidationResult.error(
                        CreatorValidationResult.Code.MISSING_REFERENCE, "target revision is unavailable in history");
                CreatorApplyResult rejected = CreatorApplyResult.rejected(rejectedValidation, current);
                revisions.commit(operation, current, rejected);
                log(current, "revision", "revision.restore_rejected", CreatorRuntimeEvent.Severity.WARNING, operation,
                        attributes("targetRevision", targetRevision));
                return rejected;
            }
            next = target.withRevision(current.getRevision() + 1);
        } else {
            next = CreatorOperationReducer.reduce(current, operation);
        }
        CreatorApplyResult applied = CreatorApplyResult.applied(next);
        revisions.commit(operation, next, applied);
        log(next, "operation", "operation.applied", CreatorRuntimeEvent.Severity.INFO, operation,
                attributes("operationType", operation.getType().name(), "baseRevision", current.getRevision()));
        return applied;
    }

    public synchronized boolean checkpoint(String name) {
        boolean created = revisions.checkpoint(name);
        if (created) {
            CreatorProjectDocument current = revisions.getCurrent();
            eventLog.append(new CreatorRuntimeEvent(System.currentTimeMillis(), current.getProjectId(), current.getRevision(),
                    "revision", "revision.checkpoint_created", CreatorRuntimeEvent.Severity.INFO, null,
                    attributes("checkpoint", name)));
        }
        return created;
    }

    public CreatorProjectDocument getCurrent() { return revisions.getCurrent(); }
    public CreatorRevisionStore getRevisionStore() { return revisions; }
    public CreatorRuntimeEventLog getEventLog() { return eventLog; }

    private void log(CreatorProjectDocument document, String category, String name,
                     CreatorRuntimeEvent.Severity severity, CreatorProjectOperation operation,
                     Map<String, Object> attributes) {
        eventLog.append(new CreatorRuntimeEvent(System.currentTimeMillis(), document.getProjectId(), document.getRevision(),
                category, name, severity, operation.getOperationId(), attributes));
    }

    private static Map<String, Object> attributes(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
