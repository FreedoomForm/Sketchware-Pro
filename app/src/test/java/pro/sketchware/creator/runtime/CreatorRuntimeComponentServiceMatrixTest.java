package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.ComponentBean;
import org.junit.Test;

public class CreatorRuntimeComponentServiceMatrixTest {
    @Test public void everyLegacyComponentHasOneExplicitRuntimeNativeService() {
        assertThat(CreatorRuntimeComponentServiceMatrix.isComplete()).isTrue();
        assertThat(CreatorRuntimeComponentServiceMatrix.all()).hasSize(30);
        for (Integer componentType : CreatorRuntimeComponentServiceMatrix.all().keySet()) {
            assertThat(CreatorRuntimeComponentServiceMatrix.serviceFor(componentType)).isNotEmpty();
            assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(componentType))
                    .isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        }
    }

    @Test public void sharedServicesCoverThePairsThatShareAndroidRuntimePrimitives() {
        assertThat(CreatorRuntimeComponentServiceMatrix.serviceFor(ComponentBean.COMPONENT_TYPE_DIALOG))
                .isEqualTo(CreatorRuntimeComponentServiceMatrix.serviceFor(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG));
        assertThat(CreatorRuntimeComponentServiceMatrix.serviceFor(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER))
                .isEqualTo(CreatorRuntimeComponentServiceMatrix.serviceFor(ComponentBean.COMPONENT_TYPE_SOUNDPOOL));
    }
}
