package pro.sketchware.creator.runtime;

import com.google.firebase.messaging.FirebaseMessaging;
import java.util.Map;

/** Runtime-native Firebase Cloud Messaging token and topic-subscription service. */
public final class CreatorFirebaseCloudMessageService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorFirebaseCloudMessageService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "firebase_cloud_message"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("token".equals(action)) {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> environment.publish(getId(), "token",
                    CreatorRuntimeServiceArguments.output("token", token)))
                    .addOnFailureListener(error -> publishError(action, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true);
        }
        String topic = CreatorRuntimeServiceArguments.string(arguments, "topic");
        if ("subscribe".equals(action) || "unsubscribe".equals(action)) {
            if (topic == null) return CreatorRuntimeServiceArguments.invalid(action + " requires topic.");
            ("subscribe".equals(action) ? FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    : FirebaseMessaging.getInstance().unsubscribeFromTopic(topic))
                    .addOnSuccessListener(ignored -> environment.publish(getId(), "success",
                            CreatorRuntimeServiceArguments.output("action", action, "topic", topic)))
                    .addOnFailureListener(error -> publishError(action, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "topic", topic);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported cloud-message action: " + action);
    }
    private void publishError(String action, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "message", error.getMessage()));
    }
}
