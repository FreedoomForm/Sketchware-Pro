package pro.sketchware.creator.runtime;

import java.util.Locale;

/** Deterministic classification of legacy project features before migration. */
public final class CreatorCompatibilityAnalyzer {
    private final CreatorRuntimeServiceCatalog services;

    public CreatorCompatibilityAnalyzer(CreatorRuntimeServiceCatalog services) {
        this.services = services == null ? CreatorRuntimeServiceCatalog.defaults() : services;
    }

    public CreatorCompatibilityTier classify(String featureId) {
        if (featureId == null || featureId.trim().isEmpty()) return CreatorCompatibilityTier.R0_UNSUPPORTED;
        String normalized = featureId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("widget:") || normalized.startsWith("block:")
                || normalized.startsWith("state:") || normalized.startsWith("navigation:")) {
            return CreatorCompatibilityTier.R1_RUNTIME_NATIVE;
        }
        if (normalized.startsWith("service:") || normalized.startsWith("plugin:")) {
            String serviceId = normalized.substring(normalized.indexOf(':') + 1);
            return services.supports(serviceId)
                    ? CreatorCompatibilityTier.R1_RUNTIME_NATIVE : CreatorCompatibilityTier.R0_UNSUPPORTED;
        }
        if (normalized.startsWith("java:") || normalized.startsWith("kotlin:")
                || normalized.startsWith("native:") || normalized.startsWith("library:")) {
            return CreatorCompatibilityTier.R0_UNSUPPORTED;
        }
        if (normalized.startsWith("dex:") || normalized.startsWith("dynamic_code:")) {
            return CreatorCompatibilityTier.R0_UNSUPPORTED;
        }
        return CreatorCompatibilityTier.R0_UNSUPPORTED;
    }
}
