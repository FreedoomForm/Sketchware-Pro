package pro.sketchware.creator.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-owned bridge to the currently rendered Creator project surface.
 * It exposes only the active activity and project widget registry; it does not
 * expose arbitrary host application internals or allow executable-code loading.
 */
public final class CreatorRuntimeEnvironment {
    public interface EventListener {
        void onServiceEvent(String serviceId, String eventName, Map<String, Object> payload);
    }

    private final Activity activity;
    private final Map<String, View> widgets = new LinkedHashMap<>();
    private final Map<Integer, PendingAction> pendingActions = new LinkedHashMap<>();
    private final EventListener listener;
    private int nextRequestCode = 9100;

    private static final class PendingAction {
        final String serviceId;
        final String eventName;
        PendingAction(String serviceId, String eventName) {
            this.serviceId = serviceId;
            this.eventName = eventName;
        }
    }

    public CreatorRuntimeEnvironment(Activity activity, EventListener listener) {
        if (activity == null) throw new IllegalArgumentException("activity");
        this.activity = activity;
        this.listener = listener;
    }

    public Activity getActivity() { return activity; }
    public Context getContext() { return activity; }

    public void clearWidgets() { widgets.clear(); }
    public void registerWidget(String widgetId, View view) {
        if (widgetId != null && view != null) widgets.put(widgetId, view);
    }
    public View findWidget(String widgetId) { return widgets.get(widgetId); }

    public void publish(String serviceId, String eventName, Map<String, Object> payload) {
        if (listener == null) return;
        listener.onServiceEvent(serviceId, eventName, Collections.unmodifiableMap(
                new LinkedHashMap<>(payload == null ? Collections.<String, Object>emptyMap() : payload)));
    }

    public boolean hasPermission(String permission) {
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(String serviceId, String permission) {
        int requestCode = nextRequestCode++;
        pendingActions.put(requestCode, new PendingAction(serviceId, "permission_result"));
        activity.requestPermissions(new String[]{permission}, requestCode);
    }

    public void launchForResult(String serviceId, String eventName, Intent intent) {
        int requestCode = nextRequestCode++;
        pendingActions.put(requestCode, new PendingAction(serviceId, eventName));
        activity.startActivityForResult(intent, requestCode);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        PendingAction pending = pendingActions.remove(requestCode);
        if (pending == null) return false;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resultCode", resultCode);
        result.put("cancelled", resultCode == Activity.RESULT_CANCELED);
        if (data != null && data.getData() != null) result.put("uri", data.getData().toString());
        publish(pending.serviceId, pending.eventName, result);
        return true;
    }

    public boolean handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        PendingAction pending = pendingActions.remove(requestCode);
        if (pending == null) return false;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        publish(pending.serviceId, pending.eventName, CreatorRuntimeServiceArguments.output(
                "permission", permissions.length == 0 ? null : permissions[0], "granted", granted));
        return true;
    }
}
