package pro.sketchware.creator.runtime;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.Map;

/** Runtime-native local notification service for the legacy Notification component. */
public final class CreatorNotificationService implements CreatorRuntimeService {
    private static final String DEFAULT_CHANNEL = "creator_runtime";
    private final CreatorRuntimeEnvironment environment;

    public CreatorNotificationService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "notification"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action == null || "show".equals(action)) return show(arguments);
        if ("cancel".equals(action)) {
            int id = (int) CreatorRuntimeServiceArguments.longValue(arguments, "id", 0L);
            NotificationManager manager = manager();
            if (manager == null) return CreatorRuntimeServiceArguments.failed("Notification service is unavailable.");
            manager.cancel(id);
            return CreatorRuntimeServiceArguments.succeeded("id", id, "cancelled", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported notification action: " + action);
    }

    private Result show(Map<String, Object> arguments) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(environment.getContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return new Result(Status.PERMISSION_REQUIRED, java.util.Collections.<String, Object>emptyMap(),
                    "Android notification permission is required.");
        }
        NotificationManager manager = manager();
        if (manager == null) return CreatorRuntimeServiceArguments.failed("Notification service is unavailable.");
        int id = (int) CreatorRuntimeServiceArguments.longValue(arguments, "id", System.currentTimeMillis() & 0x7fffffff);
        String channelId = CreatorRuntimeServiceArguments.string(arguments, "channelId");
        if (channelId == null) channelId = DEFAULT_CHANNEL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(channelId,
                    CreatorRuntimeServiceArguments.string(arguments, "channelName") == null
                            ? "Creator Runtime" : CreatorRuntimeServiceArguments.string(arguments, "channelName"),
                    NotificationManager.IMPORTANCE_DEFAULT));
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(environment.getContext(), channelId) : new Notification.Builder(environment.getContext());
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(CreatorRuntimeServiceArguments.string(arguments, "title"))
                .setContentText(CreatorRuntimeServiceArguments.string(arguments, "message"))
                .setAutoCancel(true);
        manager.notify(id, builder.build());
        return CreatorRuntimeServiceArguments.succeeded("id", id, "channelId", channelId, "shown", true);
    }

    private NotificationManager manager() {
        return (NotificationManager) environment.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
