package pro.sketchware.creator.runtime;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
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
        if (action == null) return CreatorRuntimeServiceArguments.invalid("firebase_storage requires action.");
        if ("delete_url".equals(action)) {
            String url = CreatorRuntimeServiceArguments.string(arguments, "url");
            if (url == null) return CreatorRuntimeServiceArguments.invalid("delete_url requires url.");
            try {
                storage.getReferenceFromUrl(url).delete().addOnSuccessListener(ignored -> environment.publish(getId(), "deleted",
                        CreatorRuntimeServiceArguments.output("url", url)))
                        .addOnFailureListener(error -> publishError(action, url, error));
                return CreatorRuntimeServiceArguments.succeeded("started", true, "url", url);
            } catch (IllegalArgumentException error) {
                return CreatorRuntimeServiceArguments.invalid("delete_url requires a valid Firebase Storage URL.");
            }
        }
        if ("download_file".equals(action)) {
            String url = CreatorRuntimeServiceArguments.string(arguments, "url");
            String filePath = CreatorRuntimeServiceArguments.string(arguments, "filePath");
            if (url == null || filePath == null || filePath.trim().isEmpty()) {
                return CreatorRuntimeServiceArguments.invalid("download_file requires url and filePath.");
            }
            File destination = new File(filePath);
            File parent = destination.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return CreatorRuntimeServiceArguments.failed("Download destination directory cannot be created.");
            }
            try {
                storage.getReferenceFromUrl(url).getFile(destination)
                        .addOnProgressListener(snapshot -> environment.publish(getId(), "download_progress",
                                CreatorRuntimeServiceArguments.output("url", url, "filePath", filePath,
                                        "bytes", snapshot.getBytesTransferred(), "totalBytes", snapshot.getTotalByteCount())))
                        .addOnSuccessListener(snapshot -> environment.publish(getId(), "downloaded",
                                CreatorRuntimeServiceArguments.output("url", url, "filePath", filePath,
                                        "bytes", snapshot.getBytesTransferred())))
                        .addOnFailureListener(error -> publishError(action, url, error));
                return CreatorRuntimeServiceArguments.succeeded("started", true, "url", url, "filePath", filePath);
            } catch (IllegalArgumentException error) {
                return CreatorRuntimeServiceArguments.invalid("download_file requires a valid Firebase Storage URL.");
            }
        }
        if (path == null || path.trim().isEmpty()) return CreatorRuntimeServiceArguments.invalid("firebase_storage requires path.");
        StorageReference reference = storage.getReference().child(path);
        if ("upload_uri".equals(action)) {
            String uri = CreatorRuntimeServiceArguments.string(arguments, "uri");
            if (uri == null) return CreatorRuntimeServiceArguments.invalid("upload_uri requires uri.");
            reference.putFile(Uri.parse(uri)).addOnSuccessListener(snapshot -> environment.publish(getId(), "uploaded",
                    CreatorRuntimeServiceArguments.output("path", path, "bytes", snapshot.getBytesTransferred())))
                    .addOnFailureListener(error -> publishError(action, path, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "path", path);
        }
        if ("upload_file".equals(action)) {
            String filePath = CreatorRuntimeServiceArguments.string(arguments, "filePath");
            if (filePath == null || filePath.trim().isEmpty()) return CreatorRuntimeServiceArguments.invalid("upload_file requires filePath.");
            File localFile = new File(filePath);
            if (!localFile.isFile()) return CreatorRuntimeServiceArguments.invalid("upload_file requires an existing file.");
            reference.putFile(Uri.fromFile(localFile)).addOnProgressListener(snapshot -> environment.publish(getId(), "upload_progress",
                    CreatorRuntimeServiceArguments.output("path", path, "bytes", snapshot.getBytesTransferred(), "totalBytes", snapshot.getTotalByteCount())))
                    .addOnSuccessListener(snapshot -> reference.getDownloadUrl().addOnSuccessListener(uri -> environment.publish(getId(), "uploaded",
                            CreatorRuntimeServiceArguments.output("path", path, "bytes", snapshot.getBytesTransferred(), "url", uri.toString())))
                            .addOnFailureListener(error -> publishError("download_url", path, error)))
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
