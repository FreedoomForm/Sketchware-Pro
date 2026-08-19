package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class CreatorStorageServiceTest {
    @Test public void getsSetsAndRemovesValuesUsingTheRuntimeStorageContract() {
        CreatorStorageService service = new CreatorStorageService(new InMemoryPreferences());

        assertThat(service.execute(arguments("get", "greeting", null)).getOutput()).containsEntry("value", null);
        assertThat(service.execute(arguments("set", "greeting", "Hello")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(arguments("get", "greeting", null)).getOutput()).containsEntry("value", "Hello");
        assertThat(service.execute(arguments("remove", "greeting", null)).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(arguments("get", "greeting", null)).getOutput()).containsEntry("value", null);
    }

    @Test public void acceptsNamedLegacyFileComponentConfiguration() {
        CreatorStorageService service = new CreatorStorageService(new InMemoryPreferences());
        Map<String, Object> configure = new LinkedHashMap<>();
        configure.put("action", "configure");
        configure.put("componentId", "settings1");
        configure.put("storeName", "settings");
        assertThat(service.execute(configure).getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);

        Map<String, Object> set = arguments("set", "theme", "dark");
        set.put("componentId", "settings1");
        Map<String, Object> get = arguments("get", "theme", null);
        get.put("componentId", "settings1");
        assertThat(service.execute(set).getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(service.execute(get).getOutput()).containsEntry("value", "dark");
    }

    private static Map<String, Object> arguments(String action, String key, String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("key", key);
        if (value != null) result.put("value", value);
        return result;
    }

    private static final class InMemoryPreferences implements SharedPreferences {
        private final Map<String, Object> values = new LinkedHashMap<>();

        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Map<String, ?> getAll() { return Collections.unmodifiableMap(values); }
        @Override public String getString(String key, String fallback) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : fallback;
        }
        @Override public Set<String> getStringSet(String key, Set<String> fallback) { return fallback; }
        @Override public int getInt(String key, int fallback) { return fallback; }
        @Override public long getLong(String key, long fallback) { return fallback; }
        @Override public float getFloat(String key, float fallback) { return fallback; }
        @Override public boolean getBoolean(String key, boolean fallback) { return fallback; }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) { }
        @Override public Editor edit() { return new Editor() {
            private boolean clear;
            private final Map<String, Object> updates = new LinkedHashMap<>();
            private final java.util.Set<String> removals = new java.util.LinkedHashSet<>();
            @Override public Editor putString(String key, String value) { updates.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) { updates.put(key, value); return this; }
            @Override public Editor putInt(String key, int value) { updates.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { updates.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { updates.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { updates.put(key, value); return this; }
            @Override public Editor remove(String key) { removals.add(key); return this; }
            @Override public Editor clear() { clear = true; return this; }
            @Override public boolean commit() { apply(); return true; }
            @Override public void apply() {
                if (clear) values.clear();
                for (String key : removals) values.remove(key);
                values.putAll(updates);
            }
        }; }
    }
}
