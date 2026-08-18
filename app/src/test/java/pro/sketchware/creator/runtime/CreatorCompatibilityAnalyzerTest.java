package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CreatorCompatibilityAnalyzerTest {

    @Test public void classifiesCorePluginsAndNativeFallbackExplicitly() {
        CreatorCompatibilityAnalyzer analyzer = new CreatorCompatibilityAnalyzer(CreatorRuntimePluginRegistry.defaults());

        assertThat(analyzer.classify("widget:button")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("block:if_else")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("plugin:camera")).isEqualTo(CreatorCompatibilityTier.R2_RUNTIME_PLUGIN);
        assertThat(analyzer.classify("plugin:unknown_service")).isEqualTo(CreatorCompatibilityTier.R3_NATIVE_FALLBACK);
        assertThat(analyzer.classify("java:MainActivity.java")).isEqualTo(CreatorCompatibilityTier.R3_NATIVE_FALLBACK);
        assertThat(analyzer.classify("dynamic_code:payload")).isEqualTo(CreatorCompatibilityTier.R0_UNSUPPORTED);
    }
}
