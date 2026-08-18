package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small validation helpers shared by runtime-native services. */
final class CreatorRuntimeServiceArguments {
    private CreatorRuntimeServiceArguments() { }

    static String string(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    static long longValue(Map<String, Object> arguments, String name, long fallback) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return fallback;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    static float floatValue(Map<String, Object> arguments, String name, float fallback) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return fallback;
        if (value instanceof Number) return ((Number) value).floatValue();
        return Float.parseFloat(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        if (!(value instanceof Map)) return Collections.emptyMap();
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    static CreatorRuntimeService.Result succeeded(Object... values) {
        return new CreatorRuntimeService.Result(CreatorRuntimeService.Status.SUCCEEDED, output(values), null);
    }

    static CreatorRuntimeService.Result failed(String detail) {
        return new CreatorRuntimeService.Result(CreatorRuntimeService.Status.FAILED,
                Collections.<String, Object>emptyMap(), detail);
    }

    static CreatorRuntimeService.Result invalid(String detail) {
        return new CreatorRuntimeService.Result(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT,
                Collections.<String, Object>emptyMap(), detail);
    }

    static Map<String, Object> output(Object... values) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) output.put(String.valueOf(values[i]), values[i + 1]);
        return output;
    }
}
