package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.ComponentBean;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit audit matrix for every defined legacy ComponentBean identifier. */
public final class CreatorLegacyComponentCapabilityMatrix {
    private static final Map<Integer, CreatorCompatibilityTier> TIERS;
    static {
        Map<Integer, CreatorCompatibilityTier> tiers = new LinkedHashMap<>();
        int[] types = {ComponentBean.COMPONENT_TYPE_INTENT, ComponentBean.COMPONENT_TYPE_SHAREDPREF,
                ComponentBean.COMPONENT_TYPE_CALENDAR, ComponentBean.COMPONENT_TYPE_VIBRATOR,
                ComponentBean.COMPONENT_TYPE_TIMERTASK, ComponentBean.COMPONENT_TYPE_FIREBASE,
                ComponentBean.COMPONENT_TYPE_DIALOG, ComponentBean.COMPONENT_TYPE_MEDIAPLAYER,
                ComponentBean.COMPONENT_TYPE_SOUNDPOOL, ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR,
                ComponentBean.COMPONENT_TYPE_GYROSCOPE, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH,
                ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD, ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE,
                ComponentBean.COMPONENT_TYPE_CAMERA, ComponentBean.COMPONENT_TYPE_FILE_PICKER,
                ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK, ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH,
                ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT, ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT,
                ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER, ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD,
                ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG, ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG,
                ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG, ComponentBean.COMPONENT_TYPE_NOTIFICATION,
                ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN};
        for (int type : types) tiers.put(type, CreatorCompatibilityTier.R1_RUNTIME_NATIVE);
        TIERS = Collections.unmodifiableMap(tiers);
    }
    private CreatorLegacyComponentCapabilityMatrix() { }
    public static CreatorCompatibilityTier tierFor(int type) { return TIERS.get(type); }
    public static Map<Integer, CreatorCompatibilityTier> all() { return TIERS; }
    public static boolean isComplete() { return TIERS.size() == 30; }
}
