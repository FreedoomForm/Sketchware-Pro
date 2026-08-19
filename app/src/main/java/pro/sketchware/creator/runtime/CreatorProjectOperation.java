package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A requested, inspectable transition to a Creator Runtime document. */
public final class CreatorProjectOperation {
    public enum ActorKind { USER, AI, SYSTEM }
    public enum Type {
        SCREEN_CREATE,
        WIDGET_ADD,
        WIDGET_SET_PROPERTY,
        ENTRY_CONTROL_UPDATE,
        STATE_SET,
        EVENT_ATTACH,
        REVISION_RESTORE
    }

    private final String operationId;
    private final String projectId;
    private final long baseRevision;
    private final ActorKind actorKind;
    private final Type type;
    private final Map<String, Object> payload;
    private final long requestedAtEpochMs;

    public CreatorProjectOperation(String operationId, String projectId, long baseRevision,
                                   ActorKind actorKind, Type type,
                                   Map<String, Object> payload, long requestedAtEpochMs) {
        if (operationId == null || operationId.trim().isEmpty()) throw new IllegalArgumentException("operationId");
        if (projectId == null || projectId.trim().isEmpty()) throw new IllegalArgumentException("projectId");
        if (baseRevision < 0) throw new IllegalArgumentException("baseRevision");
        if (actorKind == null) throw new IllegalArgumentException("actorKind");
        if (type == null) throw new IllegalArgumentException("type");
        this.operationId = operationId;
        this.projectId = projectId;
        this.baseRevision = baseRevision;
        this.actorKind = actorKind;
        this.type = type;
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload == null
                ? Collections.<String, Object>emptyMap() : payload));
        this.requestedAtEpochMs = requestedAtEpochMs;
    }

    public String getOperationId() { return operationId; }
    public String getProjectId() { return projectId; }
    public long getBaseRevision() { return baseRevision; }
    public ActorKind getActorKind() { return actorKind; }
    public Type getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
    public long getRequestedAtEpochMs() { return requestedAtEpochMs; }
}
