package pro.sketchware.creator.runtime;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Map;

/** Runtime-native Firebase Storage upload, URL, and delete service. */
public final class CreatorFirebaseStorageService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    public CreatorFirebaseStorageService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "firebase_storage"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String path = CreatorRuntimeServiceArguments.string(arguments, "path");
        if (action == null || path == null) return CreatorRuntimeServiceArguments.invalid("firebase_storage requires action and path.");
        StorageReference reference = storage.getReference().child(path);
        if ("upload_uri".equals(action)) {
            String uri = CreatorRuntimeServiceArguments.string(arguments, "uri");
            if (uri == null) return CreatorRuntimeServiceArguments.invalid("upload_uri requires uri.");
            reference.putFile(Uri.parse(uri)).addOnSuccessListener(snapshot -> environment.publish(getId(), "uploaded",
                    CreatorRuntimeServiceArguments.output("path", path, "bytes", snapshot.getBytesTransferred())))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "path", path);
        }
        if ("download_url".equals(action)) {
            reference.getDownloadUrl().addOnSuccessListener(uri -> environment.publish(getId(), "download_url",
                    CreatorRuntimeServiceArguments.output("path", path, "url", uri.toString())))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "path", path);
        }
        if ("delete".equals(action)) {
            reference.delete().addOnSuccessListener(ignored -> environment.publish(getId(), "deleted",
                    CreatorRuntimeServiceArguments.output("path", path)))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "path", path);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported firebase_storage action: " + action);
    }

    private void publishError(String action, String path, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "path", path, "message", error.getMessage()));
    }
}
