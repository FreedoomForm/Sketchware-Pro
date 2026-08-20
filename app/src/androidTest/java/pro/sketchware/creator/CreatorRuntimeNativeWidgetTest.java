package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TimePicker;
import android.widget.TextClock;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.material.button.MaterialButton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorEntryControl;
import pro.sketchware.creator.runtime.CreatorEventBinding;
import pro.sketchware.creator.runtime.CreatorFirebaseAuthPhoneService;
import pro.sketchware.creator.runtime.CreatorFirebaseAuthService;
import pro.sketchware.creator.runtime.CreatorFirebaseStorageService;
import pro.sketchware.creator.runtime.CreatorFirebaseDatabaseService;
import pro.sketchware.creator.runtime.CreatorVibratorService;
import pro.sketchware.creator.runtime.CreatorMediaService;
import pro.sketchware.creator.runtime.CreatorCameraService;
import pro.sketchware.creator.runtime.CreatorFilePickerService;
import pro.sketchware.creator.runtime.CreatorDatePickerService;
import pro.sketchware.creator.runtime.CreatorTimePickerService;
import pro.sketchware.creator.runtime.CreatorDialogService;
import pro.sketchware.creator.runtime.CreatorAnimatorService;
import pro.sketchware.creator.runtime.CreatorDeviceMetricsService;
import pro.sketchware.creator.runtime.CreatorMapService;
import pro.sketchware.creator.runtime.CreatorBitmapService;
import pro.sketchware.creator.runtime.CreatorTextToSpeechService;
import pro.sketchware.creator.runtime.CreatorSpeechToTextService;
import pro.sketchware.creator.runtime.CreatorFileService;
import pro.sketchware.creator.runtime.CreatorCalendarService;
import pro.sketchware.creator.runtime.CreatorDrawerService;
import pro.sketchware.creator.runtime.CreatorUiService;
import pro.sketchware.creator.runtime.CreatorIntentService;
import pro.sketchware.creator.runtime.CreatorTimerService;
import pro.sketchware.creator.runtime.CreatorStorageService;
import pro.sketchware.creator.runtime.CreatorNetworkService;
import pro.sketchware.creator.runtime.CreatorBluetoothService;
import pro.sketchware.creator.runtime.CreatorWidgetQueryService;
import pro.sketchware.creator.runtime.CreatorFirebaseGoogleLoginService;
import pro.sketchware.creator.runtime.CreatorRewardedAdService;
import pro.sketchware.creator.runtime.CreatorFirebaseCloudMessageService;
import pro.sketchware.creator.runtime.CreatorFragmentAdapterService;
import pro.sketchware.creator.runtime.CreatorGyroscopeService;
import pro.sketchware.creator.runtime.CreatorInterstitialAdService;
import pro.sketchware.creator.runtime.CreatorLocationService;
import com.besome.sketch.beans.ViewBean;

import pro.sketchware.creator.runtime.CreatorRuntimeEnvironment;
import pro.sketchware.creator.runtime.CreatorLegacyViewCapabilityMatrix;
import pro.sketchware.creator.runtime.CreatorLegacyViewImporter;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectDocumentCodec;
import pro.sketchware.creator.runtime.CreatorNotificationService;
import pro.sketchware.creator.runtime.CreatorRuntimeCapability;
import pro.sketchware.creator.runtime.CreatorRuntimePermissionBridge;
import pro.sketchware.creator.runtime.CreatorRuntimeService;
import pro.sketchware.creator.runtime.CreatorRuntimeBlock;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;
import pro.sketchware.creator.runtime.CreatorScreen;
import pro.sketchware.creator.runtime.CreatorWidget;

/**
 * Native behavior evidence for the typed Creator Runtime widget bridge.
 *
 * <p>The fixture is persisted through the production runtime store and launched
 * through the declared CreatorProjectActivity. It does not inject views or
 * invoke generated project Java.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreatorRuntimeNativeWidgetTest {

    private Context context;

    @Rule public GrantPermissionRule locationPermissions = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);

    @Before public void clearRuntimeStore() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void firebaseCloudMessageRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseCloudMessageService service = new CreatorFirebaseCloudMessageService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "subscribe")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "unsubscribe")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void firebaseAuthPhoneRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseAuthPhoneService service = new CreatorFirebaseAuthPhoneService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "send_code")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "confirm_code", "code", "123456"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "confirm_code", "verificationId", "id"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void firebaseGoogleLoginRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseGoogleLoginService service = new CreatorFirebaseGoogleLoginService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "sign_in")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void rewardedAdRejectsInvalidInputsOnNativeRuntime() {
        CreatorRewardedAdService service = new CreatorRewardedAdService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "load")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "show")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.FAILED);
    }

    @Test public void fragmentAdapterRejectsInvalidInputsOnNativeRuntime() {
        CreatorFragmentAdapterService service = new CreatorFragmentAdapterService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "page_count")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "select_page", "page", 0L)).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void gyroscopeStartStopContractOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorRuntimeEnvironment environment = new CreatorRuntimeEnvironment(activity, null);
                CreatorGyroscopeService service = new CreatorGyroscopeService(environment);
                assertThat(service.execute(map("action", "invalid")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                CreatorRuntimeService.Result start = service.execute(map("action", "start"));
                assertThat(start.getStatus()).isAnyOf(CreatorRuntimeService.Status.SUCCEEDED,
                        CreatorRuntimeService.Status.FAILED);
                CreatorRuntimeService.Result stop = service.execute(map("action", "stop"));
                assertThat(stop.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
                assertThat(stop.getOutput().get("listening")).isEqualTo(false);
            });
        }
    }

    @Test public void locationRejectsInvalidProviderOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorRuntimeEnvironment environment = new CreatorRuntimeEnvironment(activity, null);
                CreatorLocationService service = new CreatorLocationService(environment);
                assertThat(service.execute(map("action", "invalid")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "start", "provider", "invalid-provider"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.FAILED);
                assertThat(service.execute(map("action", "stop")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
            });
        }
    }

    @Test public void interstitialAdRejectsInvalidInputsOnNativeRuntime() {
        CreatorInterstitialAdService service = new CreatorInterstitialAdService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "load", "componentId", "banner"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "show", "componentId", "banner"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.FAILED);
        assertThat(service.execute(map("action", "create", "componentId", "banner"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void firebaseAuthRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseAuthService service = new CreatorFirebaseAuthService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "sign_in", "email", "user@example.com"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "register", "password", "secret"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "reset_password")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "status")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "sign_out")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void firebaseStorageRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseStorageService service = new CreatorFirebaseStorageService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "delete_url")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "delete_url", "url", "not-a-storage-url"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "download_file", "url", "url"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "upload_uri", "path", "items/photo"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "upload_file", "path", "items/photo"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "download_url")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void firebaseDatabaseRejectsInvalidInputsOnNativeRuntime() {
        CreatorFirebaseDatabaseService service = new CreatorFirebaseDatabaseService(null);
        assertThat(service.execute(map("action", "invalid", "path", "items"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "get")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "get", "path", "/absolute"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "push_key", "path", "items"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "stop_listen", "path", "items"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void vibratorValidatesDurationBeforeHardwareOnNativeRuntime() {
        CreatorVibratorService service = new CreatorVibratorService(context);
        assertThat(service.execute(map("durationMs", "bad")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("durationMs", 0L)).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("durationMs", 10001L)).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("durationMs", 1L)).getStatus())
                .isAnyOf(CreatorRuntimeService.Status.SUCCEEDED, CreatorRuntimeService.Status.FAILED);
    }

    @Test public void mediaRejectsInvalidInputsOnNativeRuntime() {
        CreatorMediaService service = new CreatorMediaService(null);
        assertThat(service.execute(map("action", "invalid", "id", "player"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "play")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "load", "id", "player"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "play", "id", "player"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "sound_create", "id", "sound", "maxStreams", 0L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "sound_play", "id", "sound"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "sound_stop_stream", "id", "sound"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void cameraRejectsUnsupportedActionOnNativeRuntime() {
        CreatorCameraService service = new CreatorCameraService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "capture", "mode", "invalid"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.PERMISSION_REQUIRED);
    }

    @Test public void filePickerRejectsUnsupportedActionOnNativeRuntime() {
        CreatorFilePickerService service = new CreatorFilePickerService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void datePickerRejectsInvalidInputsOnNativeRuntime() {
        CreatorDatePickerService service = new CreatorDatePickerService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "show", "year", "not-a-year"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void timePickerRejectsInvalidInputsOnNativeRuntime() {
        CreatorTimePickerService service = new CreatorTimePickerService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "show", "hour", "not-an-hour"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void dialogRejectsInvalidInputsOnNativeRuntime() {
        CreatorDialogService service = new CreatorDialogService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_title", "value", "Title"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_message", "dialogId", "dialog"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_positive_button", "dialogId", "dialog"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void animatorRejectsInvalidInputsOnNativeRuntime() {
        CreatorAnimatorService service = new CreatorAnimatorService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_target", "componentId", "anim"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_duration", "componentId", "anim", "durationMs", 60001L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_repeat_count", "componentId", "anim", "repeatCount", 1001L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "is_running", "componentId", "anim"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "cancel", "componentId", "anim"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void deviceMetricsQueriesTypedValuesOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorDeviceMetricsService service = new CreatorDeviceMetricsService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map("action", "invalid")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "display_width")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
                assertThat(service.execute(map("action", "display_height")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
                assertThat(service.execute(map("action", "dip", "input", 8L)).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
            });
        }
    }

    @Test public void notificationRejectsUnsupportedActionOnNativeRuntime() {
        CreatorNotificationService service = new CreatorNotificationService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void mapRejectsUnavailableWidgetAndInvalidActionOnNativeRuntime() {
        CreatorMapService service = new CreatorMapService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("widgetId", "map", "action", "invalid"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("widgetId", "map", "action", "zoom_in"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void bitmapRejectsInvalidInputsOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorBitmapService service = new CreatorBitmapService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map()).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "resize_square", "path", ""))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "resize_square", "path", "missing.png", "destination", "out.png"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.FAILED);
            });
        }
    }

    @Test public void textToSpeechRejectsInvalidInputsOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorTextToSpeechService service = new CreatorTextToSpeechService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map("action", "invalid")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "speak")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "is_speaking")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
                assertThat(service.execute(map("action", "shutdown")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
            });
        }
    }

    @Test public void speechToTextRejectsInvalidActionAndSupportsLifecycleOnNativeRuntime() {
        CreatorSpeechToTextService service = new CreatorSpeechToTextService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "stop")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "shutdown")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void fileRejectsInvalidInputsOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorFileService service = new CreatorFileService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map()).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "read")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "get_public_dir", "directory", "INVALID"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "read", "path", "missing.txt"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.FAILED);
            });
        }
    }

    @Test public void calendarRejectsInvalidInputsAndReturnsTypedStateOnNativeRuntime() {
        CreatorCalendarService service = new CreatorCalendarService();
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "add", "field", "INVALID", "value", 1L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "add", "value", 1L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "format", "pattern", "["))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "diff", "otherComponentId", "missing"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_time", "timestamp", 0L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "get_time")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void drawerRejectsInvalidInputsOnNativeRuntime() {
        CreatorDrawerService service = new CreatorDrawerService();
        assertThat(service.execute(map()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "open")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void uiRejectsInvalidInputsOnNativeRuntime() {
        CreatorUiService service = new CreatorUiService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set_title")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "copy_text")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void intentRejectsInvalidInputsOnNativeRuntime() {
        CreatorIntentService service = new CreatorIntentService(null);
        assertThat(service.execute(map("action", "invalid")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "configure_data", "intentId", "intent"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "put_extra", "intentId", "intent"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "open_url", "url", "ftp://invalid"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "share_text"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "dial"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void timerRejectsInvalidInputsOnNativeRuntime() {
        CreatorTimerService service = new CreatorTimerService(null);
        assertThat(service.execute(map()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("timerId", "timer")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("timerId", "timer", "action", "after", "delayMs", "bad"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("timerId", "timer", "action", "after", "delayMs", 1L, "periodMs", 1L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("timerId", "timer", "action", "every", "delayMs", 1L))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("timerId", "timer", "action", "cancel"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void storageRejectsInvalidInputsAndSupportsTypedPathsOnNativeRuntime() {
        CreatorStorageService service = new CreatorStorageService(context, "validation");
        assertThat(service.execute(map()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "configure", "componentId", "storage"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "set")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "invalid", "key", "key"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(service.execute(map("action", "configure", "componentId", "storage", "storeName", "prefs"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "set", "componentId", "storage", "key", "key", "value", "value"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(map("action", "get", "componentId", "storage", "key", "key"))
                .getOutput().get("value")).isEqualTo("value");
        assertThat(service.execute(map("action", "remove", "componentId", "storage", "key", "key"))
                .getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
    }

    @Test public void networkRejectsInvalidInputsBeforeRequestOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorNetworkService service = new CreatorNetworkService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map()).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "set_params"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "start", "componentId", "request"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("url", "not-a-url"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
            });
        }
    }

    @Test public void bluetoothRejectsInvalidActionsOnNativeRuntime() {
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorBluetoothService service = new CreatorBluetoothService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map()).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "invalid")).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "ready_connection"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "start_connection", "tag", "socket"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "stop_connection"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("action", "send_data", "tag", "socket"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
            });
        }
    }

    @Test public void widgetQueryRejectsInvalidInputsOnNativeRuntime() {
        seedRuntimeDocument();
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                CreatorWidgetQueryService service = new CreatorWidgetQueryService(
                        new CreatorRuntimeEnvironment(activity, null));
                assertThat(service.execute(map()).getStatus())
                        .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("widgetId", "button"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("widgetId", "missing", "action", "get_text"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("widgetId", "button", "action", "unsupported_query"))
                        .getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
                assertThat(service.execute(map("widgetId", "button", "action", "get_text"))
                        .getOutput().get("value")).isEqualTo("Increment");
            });
        }
    }

    @Test public void notificationPermissionGateMatchesAndroidSdkOnNativeRuntime() {
        assertThat(CreatorNotificationService.requiresNotificationPermission(
                32, android.content.pm.PackageManager.PERMISSION_DENIED)).isFalse();
        assertThat(CreatorNotificationService.requiresNotificationPermission(
                33, android.content.pm.PackageManager.PERMISSION_DENIED)).isTrue();
        assertThat(CreatorNotificationService.requiresNotificationPermission(
                36, android.content.pm.PackageManager.PERMISSION_GRANTED)).isFalse();
    }

    @Test public void allLegacyViewTypesImportThroughProductionRuntimeOnNativeRuntime() {
        ArrayList<ViewBean> views = new ArrayList<>();
        for (int type = 0; type < 49; type++) {
            ViewBean view = new ViewBean("native_legacy_" + type, type);
            view.parent = "root";
            view.index = type;
            views.add(view);
        }
        CreatorLegacyViewImporter.Result imported = new CreatorLegacyViewImporter().importLayout(
                "native-all-types", "Native All Types", "main", "/", views);
        assertThat(CreatorLegacyViewCapabilityMatrix.isComplete()).isTrue();
        assertThat(imported.getReport().count(
                pro.sketchware.creator.runtime.CreatorCompatibilityTier.R1_RUNTIME_NATIVE))
                .isEqualTo(49);
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit()
                .putString("active_document", CreatorProjectDocumentCodec.encode(imported.getDocument()))
                .commit();
        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getWidgets()).hasSize(50);
                View canvas = activity.findViewById(R.id.creator_preview_canvas);
                assertThat((Object) canvas).isNotNull();
            });
        }
    }

    @Test public void permissionBridgeRequiresExplicitDecisionOnNativeRuntime() {
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

    @Test public void typedWidgetEventsAndDrawerSurviveNativeRerender() {
        seedRuntimeDocument();

        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup canvas = activity.findViewById(R.id.creator_preview_canvas);

                requireButton(canvas, "Increment").performClick();
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState().get("clicks"))
                        .isEqualTo(1L);

                ListView list = requireView(canvas, ListView.class);
                assertThat(list.getAdapter().getCount()).isEqualTo(3);
                list.performItemClick(list.getChildAt(1), 1, list.getAdapter().getItemId(1));
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("listSelection")).isEqualTo(1L);

                Spinner spinner = requireView(canvas, Spinner.class);
                assertThat(spinner.getAdapter().getCount()).isEqualTo(3);
                spinner.setSelection(2);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("spinnerSelection")).isEqualTo(2L);

                ProgressBar progress = requireView(canvas, ProgressBar.class);
                SeekBar seek = requireView(canvas, SeekBar.class);
                WebView web = requireView(canvas, WebView.class);
                RatingBar rating = requireView(canvas, RatingBar.class);
                requireButton(canvas, "Configure controls").performClick();
                assertThat(progress.isIndeterminate()).isTrue();
                assertThat(seek.getMax()).isEqualTo(120);
                assertThat(seek.getProgress()).isEqualTo(64);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("progressIndeterminate")).isEqualTo(true);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("seekMax")).isEqualTo(120L);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("seekProgress")).isEqualTo(64L);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("webUrl")).isEqualTo(web.getUrl());

                requireButton(canvas, "Configure rating").performClick();
                assertThat(rating.getNumStars()).isEqualTo(7);
                assertThat(rating.getStepSize()).isEqualTo(0.5f);
                assertThat(rating.getRating()).isEqualTo(3.5f);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("ratingValue")).isEqualTo(3.5f);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("ratingStars")).isEqualTo(7L);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("ratingStep")).isEqualTo(0.5f);

                AutoCompleteTextView autocomplete = requireView(canvas, AutoCompleteTextView.class);
                SearchView search = requireView(canvas, SearchView.class);
                TextClock clock = requireView(canvas, TextClock.class);
                VideoView video = requireView(canvas, VideoView.class);
                requireButton(canvas, "Configure next widgets").performClick();
                assertThat(autocomplete.getAdapter().getCount()).isEqualTo(3);
                assertThat(autocomplete.getThreshold()).isEqualTo(2);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("autocompleteThreshold")).isEqualTo(2L);
                assertThat(search.getQuery().toString()).isEqualTo("Ada");
                assertThat(search.getQueryHint().toString()).isEqualTo("Find suggestions");
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("searchQuery")).isEqualTo("Ada");
                assertThat(String.valueOf(clock.getFormat12Hour())).isEqualTo("h:mm a");
                assertThat(String.valueOf(clock.getFormat24Hour())).isEqualTo("HH:mm");
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("clockFormat12")).isEqualTo("h:mm a");
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("clockFormat24")).isEqualTo("HH:mm");
                assertThat(video.isPlaying()).isFalse();
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("videoPlaying")).isEqualTo(false);

                requireButton(canvas, "Generate Bluetooth UUID").performClick();
                String bluetoothUuid = String.valueOf(CreatorRuntimeSession.get(activity).getDocument()
                        .getState().get("bluetoothUuid"));
                assertThat(bluetoothUuid).isNotEmpty();

                CalendarView calendar = requireView(canvas, CalendarView.class);
                requireButton(canvas, "Set date").performClick();
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("calendarDate")).isEqualTo(fixtureDate());
                assertThat(calendar.getDate()).isEqualTo(fixtureDate());

                DatePicker datePicker = requireView(canvas, DatePicker.class);
                datePicker.updateDate(2024, Calendar.FEBRUARY, 14);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("datePickerYear")).isEqualTo(2024L);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("datePickerMonth")).isEqualTo(3L);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("datePickerDay")).isEqualTo(14L);

                TimePicker timePicker = requireView(canvas, TimePicker.class);
                timePicker.setHour(7);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("timePickerHour")).isEqualTo(7L);

                requireButton(canvas, "Schedule timer").performClick();
                requireButton(canvas, "Open drawer").performClick();
                DrawerLayout openDrawer = requireView(canvas, DrawerLayout.class);
                assertThat(openDrawer.isDrawerOpen(GravityCompat.START)).isTrue();

                activity.onBackPressed();
                DrawerLayout closedDrawer = requireView(canvas, DrawerLayout.class);
                assertThat(closedDrawer.isDrawerOpen(GravityCompat.START)).isFalse();
            });
            long deadline = System.currentTimeMillis() + 3000L;
            while (System.currentTimeMillis() < deadline
                    && !Long.valueOf(1L).equals(CreatorRuntimeSession.get(context).getDocument()
                    .getState().get("timerTicks"))) {
                try { Thread.sleep(50L); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            assertThat(CreatorRuntimeSession.get(context).getDocument().getState().get("timerTicks"))
                    .isEqualTo(1L);
        }
    }

    private void seedRuntimeDocument() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null,
                Arrays.asList("button", "drawer_button", "calendar_button", "timer_button", "control_button", "rating_button",
                        "next_button", "uuid_button", "list", "spinner", "progress", "seek", "web", "rating", "autocomplete", "search", "clock", "video",
                        "calendar", "date_picker", "time_picker"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root",
                null, map("text", "Increment")));
        widgets.put("drawer_button", new CreatorWidget("drawer_button", "button", "root",
                null, map("text", "Open drawer")));
        widgets.put("calendar_button", new CreatorWidget("calendar_button", "button", "root",
                null, map("text", "Set date")));
        widgets.put("timer_button", new CreatorWidget("timer_button", "button", "root",
                null, map("text", "Schedule timer")));
        widgets.put("control_button", new CreatorWidget("control_button", "button", "root",
                null, map("text", "Configure controls")));
        widgets.put("rating_button", new CreatorWidget("rating_button", "button", "root",
                null, map("text", "Configure rating")));
        widgets.put("next_button", new CreatorWidget("next_button", "button", "root",
                null, map("text", "Configure next widgets")));
        widgets.put("uuid_button", new CreatorWidget("uuid_button", "button", "root",
                null, map("text", "Generate Bluetooth UUID")));
        widgets.put("list", new CreatorWidget("list", "list", "root", null,
                map("customDataStateId", "items", "choiceMode", ListView.CHOICE_MODE_SINGLE)));
        widgets.put("spinner", new CreatorWidget("spinner", "spinner", "root", null,
                map("customDataStateId", "spinnerItems")));
        widgets.put("progress", new CreatorWidget("progress", "progress", "root", null, null));
        widgets.put("seek", new CreatorWidget("seek", "seekbar", "root", null,
                map("max", 100L, "progress", 10L)));
        widgets.put("web", new CreatorWidget("web", "web", "root", null,
                map("url", "about:blank")));
        widgets.put("rating", new CreatorWidget("rating", "rating", "root", null,
                map("max", 5L, "progress", 1L)));
        widgets.put("autocomplete", new CreatorWidget("autocomplete", "autocomplete", "root", null,
                map("customDataStateId", "suggestions", "threshold", 1L, "hint", "Type a name")));
        widgets.put("search", new CreatorWidget("search", "search", "root", null,
                map("hint", "Search names")));
        widgets.put("clock", new CreatorWidget("clock", "clock", "root", null,
                map("format12", "h:mm a", "format24", "HH:mm")));
        widgets.put("video", new CreatorWidget("video", "video", "root", null, null));
        widgets.put("calendar", new CreatorWidget("calendar", "calendar_view", "root", null, null));
        widgets.put("date_picker", new CreatorWidget("date_picker", "date_picker", "root", null, null));
        widgets.put("time_picker", new CreatorWidget("time_picker", "time_picker", "root", null, null));
        widgets.put("drawer_root", new CreatorWidget("drawer_root", "column", null,
                Arrays.asList("drawer_text"), null));
        widgets.put("drawer_text", new CreatorWidget("drawer_text", "text", "drawer_root",
                null, map("text", "Runtime drawer")));

        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        screens.put("_drawer_home", new CreatorScreen("_drawer_home", "/drawer", "drawer_root"));

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("clicks", 0L);
        state.put("items", Arrays.asList("A", "B", "C"));
        state.put("spinnerItems", Arrays.asList("One", "Two", "Three"));
        state.put("progressIndeterminate", false);
        state.put("seekMax", 0L);
        state.put("seekProgress", 0L);
        state.put("webUrl", "");
        state.put("ratingValue", 0f);
        state.put("ratingStars", 0L);
        state.put("ratingStep", 0f);
        state.put("autocompleteThreshold", 0L);
        state.put("searchQuery", "");
        state.put("clockFormat12", "");
        state.put("clockFormat24", "");
        state.put("videoPlaying", false);
        state.put("bluetoothUuid", "");
        state.put("suggestions", Arrays.asList("Ada", "Grace", "Linus"));
        state.put("calendarDate", 0L);
        state.put("datePickerYear", 0L);
        state.put("datePickerMonth", 0L);
        state.put("datePickerDay", 0L);
        state.put("timePickerHour", 0L);
        state.put("timerTicks", 0L);
        state.put("legacy.projectFileIndex", map("home", map("hasDrawer", true)));

        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("button-click", new CreatorEventBinding("button-click", "button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                        map("stateId", "clicks", "delta", 1L)))));
        events.put("drawer-click", new CreatorEventBinding("drawer-click", "drawer_button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "drawer", "arguments", map("action", "open"))))));
        events.put("control-click", new CreatorEventBinding("control-click", "control_button", "click",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "progress", "action", "progress_set_indeterminate",
                                        "indeterminate", true))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "progress", "action", "progress_indeterminate",
                                        "resultStateId", "progressIndeterminate"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "seek", "action", "seek_set_max", "max", 120L))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "seek", "action", "seek_set_progress", "progress", 64L))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "seek", "action", "seek_max",
                                        "resultStateId", "seekMax"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "seek", "action", "seek_progress",
                                        "resultStateId", "seekProgress"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "web", "action", "web_url",
                                        "resultStateId", "webUrl"))))));
        events.put("next-click", new CreatorEventBinding("next-click", "next_button", "click",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "autocomplete", "action", "autocomplete_set_data",
                                        "itemsStateId", "suggestions"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "autocomplete", "action", "autocomplete_threshold", "threshold", 2L))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "autocomplete", "action", "autocomplete_threshold",
                                        "resultStateId", "autocompleteThreshold"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "search", "action", "search_set_query", "query", "Ada"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "search", "action", "search_set_hint", "hint", "Find suggestions"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "search", "action", "search_query",
                                        "resultStateId", "searchQuery"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "clock", "action", "clock_format_12h", "format", "h:mm a"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "clock", "action", "clock_format_24h", "format", "HH:mm"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "clock", "action", "clock_get_format_12h",
                                        "resultStateId", "clockFormat12"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "clock", "action", "clock_get_format_24h",
                                        "resultStateId", "clockFormat24"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "video", "action", "video_set_url",
                                        "url", "android.resource://android/drawable/ic_media_play"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "video", "action", "video_start"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "video", "action", "video_pause"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "video", "action", "video_stop"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "video", "action", "video_is_playing",
                                        "resultStateId", "videoPlaying"))))));
        events.put("uuid-click", new CreatorEventBinding("uuid-click", "uuid_button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "bluetooth", "arguments", map(
                                "componentId", "bluetooth1", "action", "random_uuid",
                                "resultStateId", "bluetoothUuid", "resultKey", "uuid"))))));
        events.put("rating-click", new CreatorEventBinding("rating-click", "rating_button", "click",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_set_num_stars", "stars", 7L))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_set_step_size", "step", 0.5f))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_set_value", "rating", 3.5f))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_value",
                                        "resultStateId", "ratingValue"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_num_stars",
                                        "resultStateId", "ratingStars"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "rating", "action", "rating_step_size",
                                        "resultStateId", "ratingStep"))))));
        events.put("calendar-click", new CreatorEventBinding("calendar-click", "calendar_button", "click",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "calendar", "action", "calendar_set_date",
                                        "timestamp", fixtureDate()))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "calendar", "action", "calendar_date",
                                        "resultStateId", "calendarDate"))))));
        events.put("date-picker-selected", new CreatorEventBinding("date-picker-selected", "date_picker", "date_selected",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "date_picker", "action", "date_picker_year",
                                        "resultStateId", "datePickerYear"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "date_picker", "action", "date_picker_month",
                                        "resultStateId", "datePickerMonth"))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "date_picker", "action", "date_picker_day",
                                        "resultStateId", "datePickerDay"))))));
        events.put("time-picker-selected", new CreatorEventBinding("time-picker-selected", "time_picker", "time_selected",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "time_picker", "action", "time_picker_hour",
                                        "resultStateId", "timePickerHour"))))));
        events.put("timer-button", new CreatorEventBinding("timer-button", "timer_button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "timer", "arguments", map(
                                "timerId", "timer1", "action", "after", "delayMs", 75L))))));
        events.put("timer-tick", new CreatorEventBinding("timer-tick", "timer1", "tick",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                        map("stateId", "timerTicks", "delta", 1L)))));
        events.put("list-click", new CreatorEventBinding("list-click", "list", "item_click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "widget", "arguments", map(
                                "widgetId", "list", "action", "list_checked_position",
                                "resultStateId", "listSelection"))))));
        events.put("spinner-select", new CreatorEventBinding("spinner-select", "spinner", "item_selected",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "widget", "arguments", map(
                                "widgetId", "spinner", "action", "spinner_selection",
                                "resultStateId", "spinnerSelection"))))));

        CreatorProjectDocument document = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "native-widget-fixture", 1L,
                "Native Widget Fixture", "home", screens, widgets,
                CreatorEntryControl.defaultControl(), state, events);
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit()
                .putString("active_document", CreatorProjectDocumentCodec.encode(document))
                .commit();
    }

    private static MaterialButton requireButton(View root, String text) {
        MaterialButton button = findButton(root, text);
        if (button == null) throw new AssertionError("Runtime button not found: " + text);
        return button;
    }

    private static MaterialButton findButton(View root, String text) {
        if (root instanceof MaterialButton && text.contentEquals(((MaterialButton) root).getText())) {
            return (MaterialButton) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends View> T requireView(View root, Class<T> type) {
        T view = findView(root, type);
        if (view == null) throw new AssertionError("Runtime view not found: " + type.getSimpleName());
        return view;
    }

    private static <T extends View> T findView(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findView(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static long fixtureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2022);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
