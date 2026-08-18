package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import java.util.EnumSet;

public class CreatorRuntimePermissionBridgeTest {
    @Test public void permissionRequiresHostAndExplicitUserDecision() {
        CreatorRuntimePermissionBridge bridge = new CreatorRuntimePermissionBridge(
                EnumSet.of(CreatorRuntimeCapability.CAMERA));
        assertThat(bridge.check(CreatorRuntimeCapability.CAMERA, false))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.NO_HOST);
        assertThat(bridge.check(CreatorRuntimeCapability.CAMERA, true))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.REQUEST_REQUIRED);
        assertThat(bridge.resolve(CreatorRuntimeCapability.CAMERA, false))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.DENIED);
        assertThat(bridge.resolve(CreatorRuntimeCapability.CAMERA, true))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.GRANTED);
        assertThat(bridge.check(CreatorRuntimeCapability.CAMERA, true))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.GRANTED);
        assertThat(bridge.check(CreatorRuntimeCapability.FINE_LOCATION, true))
                .isEqualTo(CreatorRuntimePermissionBridge.Outcome.UNSUPPORTED);
    }
}
