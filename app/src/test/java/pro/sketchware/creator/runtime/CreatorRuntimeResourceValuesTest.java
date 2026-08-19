package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class CreatorRuntimeResourceValuesTest {
    @Test public void resolvesDefaultVariantStringAndColorReferencesFromTypedRuntimeState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("legacy.stringResources", families("welcome", "Creator Runtime"));
        state.put("legacy.colorResources", families("ink", "#102030"));
        CreatorProjectDocument document = CreatorProjectDocument.empty("p", "Demo").withRuntimeState(0L, state,
                new LinkedHashMap<String, CreatorEventBinding>());

        assertThat(CreatorRuntimeResourceValues.resolveString(document, "@string/welcome")).isEqualTo("Creator Runtime");
        assertThat(CreatorRuntimeResourceValues.resolveColor(document, "@color/ink")).isEqualTo("#102030");
        assertThat(CreatorRuntimeResourceValues.resolveString(document, "direct")).isEqualTo("direct");
        assertThat(CreatorRuntimeResourceValues.resolveColor(document, "@color/missing")).isEqualTo("@color/missing");
    }

    private static Map<String, Object> families(String key, String value) {
        Map<String, Object> entries = new LinkedHashMap<>();
        entries.put(key, value);
        Map<String, Object> variants = new LinkedHashMap<>();
        variants.put("", entries);
        return variants;
    }
}
