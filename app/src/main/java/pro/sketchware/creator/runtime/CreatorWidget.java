package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable node in a Creator Runtime widget tree. */
public final class CreatorWidget {
    private final String id;
    private final String type;
    private final String parentId;
    private final List<String> children;
    private final Map<String, Object> properties;

    public CreatorWidget(String id, String type, String parentId,
                         List<String> children, Map<String, Object> properties) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id");
        if (type == null || type.trim().isEmpty()) throw new IllegalArgumentException("type");
        this.id = id;
        this.type = type;
        this.parentId = parentId;
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null
                ? Collections.<String>emptyList() : children));
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties == null
                ? Collections.<String, Object>emptyMap() : properties));
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getParentId() { return parentId; }
    public List<String> getChildren() { return children; }
    public Map<String, Object> getProperties() { return properties; }

    public CreatorWidget withChild(String childId, int index) {
        List<String> nextChildren = new ArrayList<>(children);
        int safeIndex = index < 0 || index > nextChildren.size() ? nextChildren.size() : index;
        nextChildren.add(safeIndex, childId);
        return new CreatorWidget(id, type, parentId, nextChildren, properties);
    }

    public CreatorWidget withProperty(String key, Object value) {
        Map<String, Object> nextProperties = new LinkedHashMap<>(properties);
        nextProperties.put(key, value);
        return new CreatorWidget(id, type, parentId, children, nextProperties);
    }

    public CreatorWidget withoutChild(String childId) {
        List<String> nextChildren = new ArrayList<>(children);
        nextChildren.removeIf(childId::equals);
        return new CreatorWidget(id, type, parentId, nextChildren, properties);
    }
}
