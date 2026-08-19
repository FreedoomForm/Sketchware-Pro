package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded in-memory diagnostics log that redacts common private-content fields. */
public final class CreatorRuntimeEventLog {
    private static final String REDACTED = "[redacted]";
    private final int capacity;
    private final List<CreatorRuntimeEvent> events = new ArrayList<>();

    public CreatorRuntimeEventLog(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public synchronized void append(CreatorRuntimeEvent event) {
        if (event == null) return;
        if (events.size() == capacity) events.remove(0);
        events.add(new CreatorRuntimeEvent(event.getTimestampEpochMs(), event.getProjectId(), event.getRevision(),
                event.getCategory(), event.getName(), event.getSeverity(), event.getCorrelationId(),
                redact(event.getAttributes())));
    }

    public synchronized List<CreatorRuntimeEvent> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    private static Map<String, Object> redact(Map<String, Object> attributes) {
        Map<String, Object> safe = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            if (key.contains("prompt") || key.contains("token") || key.contains("secret")
                    || key.contains("password") || key.contains("credential") || key.contains("content")
                    || key.contains("text") || key.contains("body")) {
                safe.put(entry.getKey(), REDACTED);
            } else {
                safe.put(entry.getKey(), entry.getValue());
            }
        }
        return safe;
    }
}
