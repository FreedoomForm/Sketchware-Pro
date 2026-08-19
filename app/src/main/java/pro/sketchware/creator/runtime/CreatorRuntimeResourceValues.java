package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.Map;

/** Resolves typed legacy value-resource references for the live Creator Runtime renderer. */
public final class CreatorRuntimeResourceValues {
    private CreatorRuntimeResourceValues() { }

    public static String resolveString(CreatorProjectDocument document, String value) {
        return resolve(document, "legacy.stringResources", "@string/", value);
    }

    public static String resolveColor(CreatorProjectDocument document, String value) {
        return resolve(document, "legacy.colorResources", "@color/", value);
    }

    @SuppressWarnings("unchecked")
    private static String resolve(CreatorProjectDocument document, String stateKey, String prefix, String value) {
        if (value == null || !value.startsWith(prefix) || document == null) return value;
        String name = value.substring(prefix.length());
        if (name.isEmpty()) return value;
        Object rawFamilies = document.getState().get(stateKey);
        if (!(rawFamilies instanceof Map)) return value;
        Object rawDefault = ((Map<?, ?>) rawFamilies).get("");
        Map<String, Object> defaultFamily = rawDefault instanceof Map
                ? (Map<String, Object>) rawDefault : Collections.<String, Object>emptyMap();
        Object resolved = defaultFamily.get(name);
        return resolved == null ? value : String.valueOf(resolved);
    }
}
