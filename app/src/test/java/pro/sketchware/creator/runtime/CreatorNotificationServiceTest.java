package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.PackageManager;
import org.junit.Test;

public class CreatorNotificationServiceTest {
    @Test public void requiresRuntimePermissionOnlyForAndroid13AndLaterWhenNotGranted() {
        assertThat(CreatorNotificationService.requiresNotificationPermission(32, PackageManager.PERMISSION_DENIED)).isFalse();
        assertThat(CreatorNotificationService.requiresNotificationPermission(33, PackageManager.PERMISSION_DENIED)).isTrue();
        assertThat(CreatorNotificationService.requiresNotificationPermission(36, PackageManager.PERMISSION_GRANTED)).isFalse();
    }
}
