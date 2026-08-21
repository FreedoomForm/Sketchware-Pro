package pro.sketchware.creator.runtime;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Device-local storage for the active Creator Runtime document. */
public final class CreatorRuntimeProjectStore {
    private static final String PREFERENCES = "creator_runtime";
    private static final String ACTIVE_DOCUMENT = "active_document";
    private final SharedPreferences preferences;

    public CreatorRuntimeProjectStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public CreatorProjectDocument loadOrCreate() {
        String serialized = preferences.getString(ACTIVE_DOCUMENT, null);
        if (serialized != null) {
            try {
                CreatorProjectDocument loaded = CreatorProjectDocumentCodec.decode(serialized);
                CreatorProjectDocument migrated = ensureMainScreen(loaded);
                if (migrated != loaded) save(migrated);
                return migrated;
            } catch (RuntimeException ignored) {
                // A corrupt local draft must not block Creator Home. The next
                // revision will replace it; diagnostic UI will report this later.
            }
        }
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        String rootId = "root_main";
        screens.put("main", new CreatorScreen("main", "/main", rootId));
        widgets.put(rootId, new CreatorWidget(rootId, "column", null,
                Collections.<String>emptyList(), Collections.<String, Object>emptyMap()));
        CreatorProjectDocument created = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "creator_default", 0,
                "Untitled project", "main", screens, widgets,
                CreatorEntryControl.defaultControl());
        save(created);
        return created;
    }

    private static CreatorProjectDocument ensureMainScreen(CreatorProjectDocument document) {
        if (document == null || !document.getScreens().isEmpty()) return document;
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>(document.getWidgets());
        String rootId = "root_main";
        screens.put("main", new CreatorScreen("main", "/main", rootId));
        if (!widgets.containsKey(rootId)) {
            widgets.put(rootId, new CreatorWidget(rootId, "column", null,
                    Collections.<String>emptyList(), Collections.<String, Object>emptyMap()));
        }
        return document.withState(document.getRevision(), "main", screens, widgets, document.getEntryControl());
    }

    public boolean save(CreatorProjectDocument document) {
        return preferences.edit().putString(ACTIVE_DOCUMENT,
                CreatorProjectDocumentCodec.encode(document)).commit();
    }
}
