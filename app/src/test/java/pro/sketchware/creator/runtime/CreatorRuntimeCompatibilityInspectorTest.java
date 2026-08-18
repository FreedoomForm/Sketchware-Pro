package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreatorRuntimeCompatibilityInspectorTest {
    @Test public void reportSeparatesLiveRuntimeWidgetsFromNativeFallbackWidgets() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null,
                java.util.Arrays.asList("title", "legacy"), null));
        widgets.put("title", new CreatorWidget("title", "text", "root", null, null));
        widgets.put("legacy", new CreatorWidget("legacy", "listview", "root", null, null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        CreatorProjectDocument document = new CreatorProjectDocument(1, "project", 2, "Demo", "home",
                screens, widgets, CreatorEntryControl.defaultControl());

        CreatorCompatibilityReport report = CreatorRuntimeCompatibilityInspector.inspect(document);

        assertThat(report.count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(4);
        assertThat(report.count(CreatorCompatibilityTier.R3_NATIVE_FALLBACK)).isEqualTo(1);
        assertThat(report.canPreviewImmediately()).isFalse();
    }
}
