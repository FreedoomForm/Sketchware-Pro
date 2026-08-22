package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CreatorRuntimeDefaultsTest {
    @Test public void seedsContinueButtonAndEditorBindingExactlyOnce() {
        CreatorProjectDocument empty = CreatorProjectDocument.empty("project", "Demo");
        CreatorProjectDocument seeded = CreatorRuntimeDefaults.ensureStarterContent(empty);

        assertThat(seeded.getScreens()).containsKey("main");
        assertThat(seeded.getScreens()).containsKey(CreatorRuntimeDefaults.EDITOR_SCREEN_ID);
        assertThat(seeded.getScreens().get(CreatorRuntimeDefaults.EDITOR_SCREEN_ID).isLocked()).isTrue();
        assertThat(seeded.getWidgets()).containsKey(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(seeded.getWidgets().get("root_main").getChildren())
                .containsExactly(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(seeded.getEvents()).containsKey(CreatorRuntimeDefaults.ENTRY_CLICK_BINDING_ID);
        assertThat(seeded.getState().get(CreatorRuntimeDefaults.STARTER_INITIALIZED_STATE)).isEqualTo(true);
        assertThat(CreatorRuntimeDefaults.ensureStarterContent(seeded)).isSameInstanceAs(seeded);
    }

    @Test public void markerPreventsReaddingButtonAfterUserRemovedIt() {
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        screens.put("main", new CreatorScreen("main", "/main", "root_main"));
        widgets.put("root_main", new CreatorWidget("root_main", "column", null,
                Collections.emptyList(), Collections.emptyMap()));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(CreatorRuntimeDefaults.STARTER_INITIALIZED_STATE, true);
        state.put(CreatorRuntimeDefaults.STARTER_VERSION_STATE, CreatorRuntimeDefaults.STARTER_VERSION);
        CreatorProjectDocument removed = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "project", 4, "Demo", "main",
                screens, widgets, CreatorEntryControl.defaultControl(), state,
                Collections.emptyMap());

        CreatorProjectDocument migrated = CreatorRuntimeDefaults.ensureStarterContent(removed);
        assertThat(migrated).isNotSameInstanceAs(removed);
        assertThat(migrated.getWidgets()).doesNotContainKey(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(migrated.getEvents()).isEmpty();
        assertThat(migrated.getWidgets().get("root_main").getProperties())
                .containsEntry("legacyGravity", android.view.Gravity.BOTTOM | android.view.Gravity.END);
    }

    @Test public void oldStarterMarkerIsMigratedOnce() {
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        screens.put("main", new CreatorScreen("main", "/main", "root_main"));
        widgets.put("root_main", new CreatorWidget("root_main", "column", null,
                Collections.emptyList(), Collections.emptyMap()));
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(CreatorRuntimeDefaults.STARTER_INITIALIZED_STATE, true);
        CreatorProjectDocument old = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "project", 4, "Demo", "main",
                screens, widgets, CreatorEntryControl.defaultControl(), state,
                Collections.emptyMap());

        CreatorProjectDocument migrated = CreatorRuntimeDefaults.ensureStarterContent(old);

        assertThat(migrated.getWidgets()).containsKey(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(migrated.getScreens()).containsKey(CreatorRuntimeDefaults.EDITOR_SCREEN_ID);
        assertThat(migrated.getState().get(CreatorRuntimeDefaults.STARTER_VERSION_STATE))
                .isEqualTo(CreatorRuntimeDefaults.STARTER_VERSION);
    }

    @Test public void initializedDocumentWithRootDefaultsIsNotRewritten() {
        CreatorProjectDocument seeded = CreatorRuntimeDefaults.ensureStarterContent(
                CreatorProjectDocument.empty("project", "Demo"));
        assertThat(CreatorRuntimeDefaults.ensureStarterContent(seeded)).isSameInstanceAs(seeded);
    }
}
