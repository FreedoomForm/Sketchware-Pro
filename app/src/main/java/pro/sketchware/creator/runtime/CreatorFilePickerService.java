package pro.sketchware.creator.runtime;

import android.content.Intent;
import java.util.Map;

/** Runtime-native document picker using Android's persisted document provider contract. */
public final class CreatorFilePickerService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorFilePickerService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "file_picker"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (!"pick".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported file picker action: " + action);
        String mimeType = CreatorRuntimeServiceArguments.string(arguments, "mimeType");
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(mimeType == null ? "*/*" : mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        environment.launchForResult(getId(), "selected", pick);
        return CreatorRuntimeServiceArguments.succeeded("started", true, "mimeType", mimeType == null ? "*/*" : mimeType);
    }
}
