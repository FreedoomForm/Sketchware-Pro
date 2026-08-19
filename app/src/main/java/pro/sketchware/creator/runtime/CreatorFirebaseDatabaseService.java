package pro.sketchware.creator.runtime;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Runtime-native Firebase Realtime Database service. Firebase configuration remains app-owned. */
public final class CreatorFirebaseDatabaseService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FirebaseDatabase database;
    private final Map<String, ChildEventListener> childListeners = new LinkedHashMap<>();

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
        if ("push_key".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("key", reference.push().getKey(), "path", path);
        }
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
        if ("get_children".equals(action)) {
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Object raw = child.getValue();
                        if (raw instanceof Map) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                                if (entry.getKey() != null) row.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            rows.add(row);
                        }
                    }
                    environment.publish(getId(), "children", CreatorRuntimeServiceArguments.output(
                            "path", path, "rows", rows,
                            "resultStateId", CreatorRuntimeServiceArguments.string(arguments, "resultStateId"),
                            "callbackTargetId", CreatorRuntimeServiceArguments.string(arguments, "callbackTargetId")));
                }
                @Override public void onCancelled(DatabaseError error) { publishError(action, path, error.toException()); }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "path", path);
        }
        if ("listen".equals(action)) {
            ChildEventListener previous = childListeners.remove(path);
            if (previous != null) reference.removeEventListener(previous);
            ChildEventListener listener = new ChildEventListener() {
                @Override public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    publishChild("child_added", path, snapshot, previousChildName);
                }
                @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    publishChild("child_changed", path, snapshot, previousChildName);
                }
                @Override public void onChildRemoved(DataSnapshot snapshot) { publishChild("child_removed", path, snapshot, null); }
                @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                    publishChild("child_moved", path, snapshot, previousChildName);
                }
                @Override public void onCancelled(DatabaseError error) { publishError(action, path, error.toException()); }
            };
            childListeners.put(path, listener);
            reference.addChildEventListener(listener);
            return CreatorRuntimeServiceArguments.succeeded("listening", true, "path", path);
        }
        if ("stop_listen".equals(action)) {
            ChildEventListener listener = childListeners.remove(path);
            if (listener != null) reference.removeEventListener(listener);
            return CreatorRuntimeServiceArguments.succeeded("listening", false, "path", path);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported firebase action: " + action);
    }

    private void publishChild(String event, String path, DataSnapshot snapshot, String previousChildName) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
                "path", path, "key", snapshot.getKey(), "value", snapshot.getValue(), "previousChildName", previousChildName));
    }

    private void publishError(String action, String path, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "path", path, "message", error.getMessage()));
    }
}
