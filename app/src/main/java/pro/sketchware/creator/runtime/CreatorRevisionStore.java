package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, immutable revision history with named checkpoints and idempotent operation results. */
public final class CreatorRevisionStore {
    private final int capacity;
    private final LinkedHashMap<Long, CreatorProjectDocument> revisions = new LinkedHashMap<>();
    private final Map<String, CreatorApplyResult> operationResults = new LinkedHashMap<>();
    private final Map<String, Long> checkpoints = new LinkedHashMap<>();
    private CreatorProjectDocument current;

    public CreatorRevisionStore(CreatorProjectDocument initial, int capacity) {
        if (initial == null) throw new IllegalArgumentException("initial");
        if (capacity < 2) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        this.current = initial;
        revisions.put(initial.getRevision(), initial);
    }

    public synchronized CreatorProjectDocument getCurrent() { return current; }

    public synchronized CreatorApplyResult getOperationResult(String operationId) {
        return operationResults.get(operationId);
    }

    public synchronized CreatorProjectDocument getRevision(long revision) {
        return revisions.get(revision);
    }

    /** Returns currently restorable revisions, newest first, without exposing mutable documents. */
    public synchronized List<Long> getAvailableRevisions() {
        List<Long> ordered = new ArrayList<>(revisions.keySet());
        Collections.reverse(ordered);
        return Collections.unmodifiableList(ordered);
    }

    public synchronized void commit(CreatorProjectOperation operation, CreatorProjectDocument next,
                                    CreatorApplyResult result) {
        current = next;
        revisions.put(next.getRevision(), next);
        operationResults.put(operation.getOperationId(), result);
        while (revisions.size() > capacity) revisions.remove(revisions.keySet().iterator().next());
        while (operationResults.size() > capacity) {
            operationResults.remove(operationResults.keySet().iterator().next());
        }
    }

    public synchronized boolean checkpoint(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        checkpoints.put(name, current.getRevision());
        while (checkpoints.size() > capacity) {
            checkpoints.remove(checkpoints.keySet().iterator().next());
        }
        return true;
    }

    public synchronized Long getCheckpointRevision(String name) { return checkpoints.get(name); }
    public synchronized Map<String, Long> getCheckpoints() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(checkpoints));
    }
}
