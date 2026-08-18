package pro.sketchware.creator.runtime;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native implementation of the legacy SharedPreferences component. */
public final class CreatorStorageService implements CreatorRuntimeService {
    private final SharedPreferences preferences;

    public CreatorStorageService(Context context, String projectId) {
        if (context == null || projectId == null || projectId.trim().isEmpty()) throw new IllegalArgumentException("context/projectId");
        preferences = context.getApplicationContext().getSharedPreferences("creator_runtime_" + projectId, Context.MODE_PRIVATE);
    }

    @Override public String getId() { return "local_storage"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = String.valueOf(arguments.get("action"));
        String key = arguments.get("key") == null ? null : String.valueOf(arguments.get("key"));
        if (key == null || key.trim().isEmpty()) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "A storage key is required.");
        if ("get".equals(action)) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("value", preferences.getString(key, null));
            return new Result(Status.SUCCEEDED, output, null);
        }
        if ("set".equals(action)) {
            Object value = arguments.get("value");
            preferences.edit().putString(key, value == null ? null : String.valueOf(value)).apply();
            return new Result(Status.SUCCEEDED, Collections.singletonMap("key", key), null);
        }
        if ("remove".equals(action)) {
            preferences.edit().remove(key).apply();
            return new Result(Status.SUCCEEDED, Collections.singletonMap("key", key), null);
        }
        return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Unsupported storage action: " + action);
    }
}
