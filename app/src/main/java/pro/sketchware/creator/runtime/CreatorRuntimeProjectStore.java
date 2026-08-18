package pro.sketchware.creator.runtime;

import android.content.Context;
import android.content.SharedPreferences;

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
                return CreatorProjectDocumentCodec.decode(serialized);
            } catch (RuntimeException ignored) {
                // A corrupt local draft must not block Creator Home. The next
                // revision will replace it; diagnostic UI will report this later.
            }
        }
        CreatorProjectDocument created = CreatorProjectDocument.empty("creator_default", "Untitled project");
        save(created);
        return created;
    }

    public boolean save(CreatorProjectDocument document) {
        return preferences.edit().putString(ACTIVE_DOCUMENT,
                CreatorProjectDocumentCodec.encode(document)).commit();
    }
}
