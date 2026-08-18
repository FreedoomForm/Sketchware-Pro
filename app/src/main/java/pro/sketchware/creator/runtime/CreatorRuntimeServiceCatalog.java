package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable catalog of reviewed service IDs compiled into Creator Runtime. */
public final class CreatorRuntimeServiceCatalog {
    private final Set<String> serviceIds;

    private CreatorRuntimeServiceCatalog(Set<String> serviceIds) {
        this.serviceIds = Collections.unmodifiableSet(new LinkedHashSet<>(serviceIds));
    }

    public static CreatorRuntimeServiceCatalog defaults() {
        Set<String> values = new LinkedHashSet<>(CreatorRuntimeComponentServiceMatrix.all().values());
        return new CreatorRuntimeServiceCatalog(values);
    }

    public boolean supports(String serviceId) {
        return serviceId != null && serviceIds.contains(serviceId);
    }

    public Set<String> all() { return serviceIds; }
}
