package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CreatorCompatibilityAnalyzerTest {

    @Test public void classifiesRuntimeServicesAndBlocksExecutableFallbacksExplicitly() {
        CreatorCompatibilityAnalyzer analyzer = new CreatorCompatibilityAnalyzer(CreatorRuntimeServiceCatalog.defaults());

        assertThat(analyzer.classify("widget:button")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("block:if_else")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("service:camera")).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        assertThat(analyzer.classify("service:unknown_service")).isEqualTo(CreatorCompatibilityTier.R0_UNSUPPORTED);
        assertThat(analyzer.classify("java:MainActivity.java")).isEqualTo(CreatorCompatibilityTier.R0_UNSUPPORTED);
        assertThat(analyzer.classify("dynamic_code:payload")).isEqualTo(CreatorCompatibilityTier.R0_UNSUPPORTED);
    }
}
