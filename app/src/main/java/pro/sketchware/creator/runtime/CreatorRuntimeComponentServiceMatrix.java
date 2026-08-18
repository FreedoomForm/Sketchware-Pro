package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.ComponentBean;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Complete, explicit binding from every legacy ComponentBean type to a Creator Runtime service ID. */
public final class CreatorRuntimeComponentServiceMatrix {
    private static final Map<Integer, String> SERVICES;
    static {
        Map<Integer, String> services = new LinkedHashMap<>();
        services.put(ComponentBean.COMPONENT_TYPE_INTENT, "intent");
        services.put(ComponentBean.COMPONENT_TYPE_SHAREDPREF, "local_storage");
        services.put(ComponentBean.COMPONENT_TYPE_CALENDAR, "calendar");
        services.put(ComponentBean.COMPONENT_TYPE_VIBRATOR, "vibrator");
        services.put(ComponentBean.COMPONENT_TYPE_TIMERTASK, "timer");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE, "firebase");
        services.put(ComponentBean.COMPONENT_TYPE_DIALOG, "dialog");
        services.put(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER, "media");
        services.put(ComponentBean.COMPONENT_TYPE_SOUNDPOOL, "media");
        services.put(ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR, "animator");
        services.put(ComponentBean.COMPONENT_TYPE_GYROSCOPE, "gyroscope");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH, "firebase_auth");
        services.put(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD, "ads_interstitial");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE, "firebase_storage");
        services.put(ComponentBean.COMPONENT_TYPE_CAMERA, "camera");
        services.put(ComponentBean.COMPONENT_TYPE_FILE_PICKER, "file_picker");
        services.put(ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK, "http");
        services.put(ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH, "text_to_speech");
        services.put(ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT, "speech_to_text");
        services.put(ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT, "bluetooth");
        services.put(ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER, "location");
        services.put(ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD, "ads_rewarded");
        services.put(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG, "dialog");
        services.put(ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG, "date_picker");
        services.put(ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG, "time_picker");
        services.put(ComponentBean.COMPONENT_TYPE_NOTIFICATION, "notification");
        services.put(ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER, "fragment_adapter");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, "firebase_auth_phone");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE, "firebase_cloud_message");
        services.put(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN, "firebase_auth_google");
        SERVICES = Collections.unmodifiableMap(services);
    }
    private CreatorRuntimeComponentServiceMatrix() { }
    public static String serviceFor(int componentType) { return SERVICES.get(componentType); }
    public static Map<Integer, String> all() { return SERVICES; }
    public static boolean isComplete() { return SERVICES.size() == 30 && !SERVICES.containsValue(null); }
}
