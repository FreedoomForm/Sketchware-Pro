package pro.sketchware.creator.runtime;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;

/** Runtime-native Firebase Realtime Database service. Firebase configuration remains app-owned. */
public final class CreatorFirebaseDatabaseService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FirebaseDatabase database;

    public CreatorFirebaseDatabaseService(CreatorRuntimeEnvironment environment) {
        this.environment = environment;
        this.database = FirebaseDatabase.getInstance();
    }
    @Override public String getId() { return "firebase"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String path = CreatorRuntimeServiceArguments.string(arguments, "path");
        if (action == null || path == null || path.startsWith("/")) {
            return CreatorRuntimeServiceArguments.invalid("firebase requires action and a relative path.");
        }
        DatabaseReference reference = database.getReference(path);
        if ("set".equals(action) || "update".equals(action)) {
            Object value = arguments.get("value");
            com.google.android.gms.tasks.Task<Void> task = "set".equals(action)
                    ? reference.setValue(value) : reference.updateChildren(CreatorRuntimeServiceArguments.map(arguments, "value"));
            task.addOnSuccessListener(ignored -> environment.publish(getId(), "success",
                    CreatorRuntimeServiceArguments.output("action", action, "path", path)))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "path", path);
        }
        if ("remove".equals(action)) {
            reference.removeValue().addOnSuccessListener(ignored -> environment.publish(getId(), "success",
                    CreatorRuntimeServiceArguments.output("action", action, "path", path)))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "path", path);
        }
        if ("get".equals(action)) {
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    environment.publish(getId(), "value", CreatorRuntimeServiceArguments.output(
                            "path", path, "exists", snapshot.exists(), "value", snapshot.getValue()));
                }
                @Override public void onCancelled(DatabaseError error) { publishError(action, path, error.toException()); }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "path", path);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported firebase action: " + action);
    }

    private void publishError(String action, String path, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "path", path, "message", error.getMessage()));
    }
}
