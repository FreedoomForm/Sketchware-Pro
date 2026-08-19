package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One visible, serializable behavior block in a Creator Runtime event binding. */
public final class CreatorRuntimeBlock {
    public enum Type { SET_WIDGET_PROPERTY, SET_STATE, INCREMENT_STATE, LIST_MUTATE, MAP_MUTATE, SHOW_MESSAGE, NAVIGATE, RUNTIME_SERVICE_CALL, IF_STATE_EQUALS }
    private final Type type;
    private final Map<String, Object> payload;
    private final List<CreatorRuntimeBlock> thenBlocks;
    private final List<CreatorRuntimeBlock> elseBlocks;

    public CreatorRuntimeBlock(Type type, Map<String, Object> payload) {
        this(type, payload, Collections.<CreatorRuntimeBlock>emptyList(), Collections.<CreatorRuntimeBlock>emptyList());
    }
    public CreatorRuntimeBlock(Type type, Map<String, Object> payload,
                               List<CreatorRuntimeBlock> thenBlocks, List<CreatorRuntimeBlock> elseBlocks) {
        if (type == null) throw new IllegalArgumentException("type");
        this.type = type;
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload == null
                ? Collections.<String, Object>emptyMap() : payload));
        this.thenBlocks = Collections.unmodifiableList(new ArrayList<>(thenBlocks == null
                ? Collections.<CreatorRuntimeBlock>emptyList() : thenBlocks));
        this.elseBlocks = Collections.unmodifiableList(new ArrayList<>(elseBlocks == null
                ? Collections.<CreatorRuntimeBlock>emptyList() : elseBlocks));
    }
    public Type getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
    public List<CreatorRuntimeBlock> getThenBlocks() { return thenBlocks; }
    public List<CreatorRuntimeBlock> getElseBlocks() { return elseBlocks; }
}
