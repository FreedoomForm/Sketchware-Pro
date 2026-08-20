package pro.sketchware.creator.runtime;

import android.Manifest;
import android.content.Intent;
import android.provider.MediaStore;
import java.util.Collections;
import java.util.Map;

/** Runtime-native Camera component using Android's capture intent. */
public final class CreatorCameraService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorCameraService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "camera"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (!"capture".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported camera action: " + action);
        if (environment == null || environment.getActivity() == null) {
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Camera host Activity is unavailable.");
        }
        if (!environment.hasPermission(Manifest.permission.CAMERA)) {
            environment.requestPermission(getId(), Manifest.permission.CAMERA);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Camera permission was requested.");
        }
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (capture.resolveActivity(environment.getActivity().getPackageManager()) == null) {
            return CreatorRuntimeServiceArguments.failed("No Android camera application is available.");
        }
        environment.launchForResult(getId(), "captured", capture);
        return CreatorRuntimeServiceArguments.succeeded("started", true);
    }
}
