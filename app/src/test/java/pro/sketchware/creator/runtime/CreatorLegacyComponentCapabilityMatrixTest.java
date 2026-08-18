package pro.sketchware.creator.runtime;
import static com.google.common.truth.Truth.assertThat;
import com.besome.sketch.beans.ComponentBean;
import org.junit.Test;
public class CreatorLegacyComponentCapabilityMatrixTest {
    @Test public void everyDefinedLegacyComponentHasAnExplicitTier() {
        assertThat(CreatorLegacyComponentCapabilityMatrix.isComplete()).isTrue();
        assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(ComponentBean.COMPONENT_TYPE_CAMERA)).isNotNull();
        assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE)).isNotNull();
        assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(ComponentBean.COMPONENT_TYPE_NOTIFICATION)).isNotNull();
        assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(ComponentBean.COMPONENT_TYPE_CAMERA))
                .isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        for (CreatorCompatibilityTier tier : CreatorLegacyComponentCapabilityMatrix.all().values()) {
            assertThat(tier).isEqualTo(CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        }
    }
}
