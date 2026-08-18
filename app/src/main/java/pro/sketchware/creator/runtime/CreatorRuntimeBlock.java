package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One visible, serializable behavior block in a Creator Runtime event binding. */
public final class CreatorRuntimeBlock {
    public enum Type { SET_WIDGET_PROPERTY, SET_STATE, SHOW_MESSAGE, NAVIGATE }
    private final Type type;
    private final Map<String, Object> payload;

    public CreatorRuntimeBlock(Type type, Map<String, Object> payload) {
        if (type == null) throw new IllegalArgumentException("type");
        this.type = type;
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload == null
                ? Collections.<String, Object>emptyMap() : payload));
    }
    public Type getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
}
