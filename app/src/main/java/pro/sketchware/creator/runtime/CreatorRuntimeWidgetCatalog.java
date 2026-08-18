package pro.sketchware.creator.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Authoritative R1 widget vocabulary for the live Creator Runtime renderer. */
public final class CreatorRuntimeWidgetCatalog {
    private static final Set<String> RUNTIME_NATIVE = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "column", "row", "stack", "scroll", "text", "button", "input", "image", "checkbox", "switch")));

    private CreatorRuntimeWidgetCatalog() { }

    public static boolean isRuntimeNative(String type) {
        return type != null && RUNTIME_NATIVE.contains(type);
    }

    public static Set<String> runtimeNativeTypes() { return RUNTIME_NATIVE; }
}
