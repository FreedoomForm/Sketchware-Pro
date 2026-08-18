package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.ViewBean;
import org.junit.Test;

public class CreatorLegacyViewCapabilityMatrixTest {
    @Test public void everyLegacyViewTypeHasAnExplicitMigrationTier() {
        assertThat(CreatorLegacyViewCapabilityMatrix.isComplete()).isTrue();
        for (int type = 0; type < 49; type++) {
            assertThat(CreatorLegacyViewCapabilityMatrix.tierFor(type)).isNotNull();
        }
        assertThat(CreatorLegacyViewCapabilityMatrix.tierFor(ViewBean.VIEW_TYPE_WIDGET_BUTTON))
                .isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
    }

    @Test public void everyInventoriedLegacyViewUsesTheRuntimeNativeTier() {
        for (CreatorCompatibilityTier tier : CreatorLegacyViewCapabilityMatrix.all().values()) {
            assertThat(tier).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        }
    }
}
