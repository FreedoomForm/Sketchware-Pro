package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A user-inspectable widget event and its ordered block list. */
public final class CreatorEventBinding {
    private final String id;
    private final String targetWidgetId;
    private final String eventName;
    private final List<CreatorRuntimeBlock> blocks;

    public CreatorEventBinding(String id, String targetWidgetId, String eventName, List<CreatorRuntimeBlock> blocks) {
        if (id == null || id.trim().isEmpty() || targetWidgetId == null || targetWidgetId.trim().isEmpty()
                || eventName == null || eventName.trim().isEmpty()) throw new IllegalArgumentException("binding fields");
        this.id = id;
        this.targetWidgetId = targetWidgetId;
        this.eventName = eventName;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks == null
                ? Collections.<CreatorRuntimeBlock>emptyList() : blocks));
    }
    public String getId() { return id; }
    public String getTargetWidgetId() { return targetWidgetId; }
    public String getEventName() { return eventName; }
    public List<CreatorRuntimeBlock> getBlocks() { return blocks; }
}
