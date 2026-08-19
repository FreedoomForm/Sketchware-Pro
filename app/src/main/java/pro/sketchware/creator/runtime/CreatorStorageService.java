package pro.sketchware.creator.runtime;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native implementation of the legacy SharedPreferences component. */
public final class CreatorStorageService implements CreatorRuntimeService {
    private final SharedPreferences preferences;
    private final Context context;
    private final String projectId;
    private final Map<String, String> configuredStores = new LinkedHashMap<>();

    public CreatorStorageService(Context context, String projectId) {
        if (context == null || projectId == null || projectId.trim().isEmpty()) throw new IllegalArgumentException("context/projectId");
        this.context = context.getApplicationContext();
        this.projectId = projectId;
        preferences = this.context.getSharedPreferences("creator_runtime_" + projectId, Context.MODE_PRIVATE);
    }

    CreatorStorageService(SharedPreferences preferences) {
        if (preferences == null) throw new IllegalArgumentException("preferences");
        this.preferences = preferences;
        this.context = null;
        this.projectId = null;
    }

    @Override public String getId() { return "local_storage"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = String.valueOf(arguments.get("action"));
        if ("configure".equals(action)) {
            String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
            String storeName = CreatorRuntimeServiceArguments.string(arguments, "storeName");
            if (componentId == null || componentId.trim().isEmpty() || storeName == null || storeName.trim().isEmpty()) {
                return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Storage configuration requires componentId and storeName.");
            }
            configuredStores.put(componentId, storeName.trim());
            return new Result(Status.SUCCEEDED, CreatorRuntimeServiceArguments.output(
                    "componentId", componentId, "storeName", storeName.trim()), null);
        }
        String key = arguments.get("key") == null ? null : String.valueOf(arguments.get("key"));
        if (key == null || key.trim().isEmpty()) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "A storage key is required.");
        SharedPreferences selected = select(arguments);
        if ("get".equals(action)) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("value", selected.getString(key, null));
            return new Result(Status.SUCCEEDED, output, null);
        }
        if ("set".equals(action)) {
            Object value = arguments.get("value");
            selected.edit().putString(key, value == null ? null : String.valueOf(value)).apply();
            return new Result(Status.SUCCEEDED, Collections.singletonMap("key", key), null);
        }
        if ("remove".equals(action)) {
            selected.edit().remove(key).apply();
            return new Result(Status.SUCCEEDED, Collections.singletonMap("key", key), null);
        }
        return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Unsupported storage action: " + action);
    }

    private SharedPreferences select(Map<String, Object> arguments) {
        if (context == null) return preferences;
        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
        String configured = componentId == null ? null : configuredStores.get(componentId);
        String storeName = CreatorRuntimeServiceArguments.string(arguments, "storeName");
        if (storeName == null || storeName.trim().isEmpty()) storeName = configured;
        if (storeName == null || storeName.trim().isEmpty()) return preferences;
        return context.getSharedPreferences("creator_runtime_" + projectId + "_" + sanitize(storeName), Context.MODE_PRIVATE);
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
