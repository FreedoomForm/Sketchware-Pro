package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable allow-list dispatcher for services shipped inside the Creator Runtime. */
public final class CreatorRuntimeServiceDispatcher {
    private final Map<String, CreatorRuntimeService> services = new LinkedHashMap<>();

    public CreatorRuntimeServiceDispatcher register(CreatorRuntimeService service) {
        if (service == null || service.getId() == null || service.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("service");
        }
        services.put(service.getId(), service);
        return this;
    }

    public CreatorRuntimeService.Result dispatch(String id, Map<String, Object> arguments) {
        CreatorRuntimeService service = services.get(id);
        if (service == null) {
            return new CreatorRuntimeService.Result(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT,
                    Collections.<String, Object>emptyMap(),
                    "No Creator Runtime service is registered for " + id + ".");
        }
        return service.execute(arguments == null ? Collections.<String, Object>emptyMap() : arguments);
    }

    public Map<String, CreatorRuntimeService> registered() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(services));
    }
}
