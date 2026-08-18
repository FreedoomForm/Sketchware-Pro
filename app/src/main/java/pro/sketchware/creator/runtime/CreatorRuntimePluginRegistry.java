package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Allow-list of host-owned capability bridges. Projects request IDs from this
 * registry; they never load arbitrary Android code or unreviewed binaries.
 */
public final class CreatorRuntimePluginRegistry {
    public static final class Capability {
        private final String id;
        private final String displayName;
        private final boolean needsRuntimePermission;

        Capability(String id, String displayName, boolean needsRuntimePermission) {
            this.id = id;
            this.displayName = displayName;
            this.needsRuntimePermission = needsRuntimePermission;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public boolean needsRuntimePermission() { return needsRuntimePermission; }
    }

    private final Map<String, Capability> capabilities;

    private CreatorRuntimePluginRegistry(Map<String, Capability> capabilities) {
        this.capabilities = Collections.unmodifiableMap(new LinkedHashMap<>(capabilities));
    }

    public static CreatorRuntimePluginRegistry defaults() {
        Map<String, Capability> values = new LinkedHashMap<>();
        add(values, "http", "HTTP requests", false);
        add(values, "local_storage", "Local storage", false);
        add(values, "web_view", "Web view", false);
        add(values, "camera", "Camera", true);
        add(values, "location", "Location", true);
        add(values, "notifications", "Notifications", true);
        add(values, "media", "Media playback", false);
        add(values, "maps", "Maps", true);
        return new CreatorRuntimePluginRegistry(values);
    }

    public boolean supports(String capabilityId) { return capabilities.containsKey(capabilityId); }
    public Capability get(String capabilityId) { return capabilities.get(capabilityId); }
    public Map<String, Capability> all() { return capabilities; }

    private static void add(Map<String, Capability> values, String id, String displayName, boolean permission) {
        values.put(id, new Capability(id, displayName, permission));
    }
}
