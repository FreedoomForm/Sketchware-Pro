package pro.sketchware.creator.runtime;

import java.util.Locale;

/** Deterministic classification of legacy project features before migration. */
public final class CreatorCompatibilityAnalyzer {
    private final CreatorRuntimePluginRegistry plugins;

    public CreatorCompatibilityAnalyzer(CreatorRuntimePluginRegistry plugins) {
        this.plugins = plugins == null ? CreatorRuntimePluginRegistry.defaults() : plugins;
    }

    public CreatorCompatibilityTier classify(String featureId) {
        if (featureId == null || featureId.trim().isEmpty()) return CreatorCompatibilityTier.R0_UNSUPPORTED;
        String normalized = featureId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("widget:") || normalized.startsWith("block:")
                || normalized.startsWith("state:") || normalized.startsWith("navigation:")) {
            return CreatorCompatibilityTier.R1_RUNTIME_NATIVE;
        }
        if (normalized.startsWith("plugin:")) {
            return plugins.supports(normalized.substring("plugin:".length()))
                    ? CreatorCompatibilityTier.R2_RUNTIME_PLUGIN
                    : CreatorCompatibilityTier.R3_NATIVE_FALLBACK;
        }
        if (normalized.startsWith("java:") || normalized.startsWith("kotlin:")
                || normalized.startsWith("native:") || normalized.startsWith("library:")) {
            return CreatorCompatibilityTier.R3_NATIVE_FALLBACK;
        }
        if (normalized.startsWith("dex:") || normalized.startsWith("dynamic_code:")) {
            return CreatorCompatibilityTier.R0_UNSUPPORTED;
        }
        return CreatorCompatibilityTier.R0_UNSUPPORTED;
    }
}
