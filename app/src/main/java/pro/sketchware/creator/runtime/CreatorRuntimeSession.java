package pro.sketchware.creator.runtime;

import android.content.Context;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application-scoped bridge between Android screens and the pure runtime core.
 * Every accepted operation is persisted before the caller receives success.
 */
public final class CreatorRuntimeSession {
    public interface Listener { void onDocumentChanged(CreatorProjectDocument document); }

    private static volatile CreatorRuntimeSession instance;
    private final CreatorRuntimeProjectStore store;
    private CreatorRuntimeEngine engine;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private CreatorRuntimeSession(Context context) {
        store = new CreatorRuntimeProjectStore(context);
        engine = new CreatorRuntimeEngine(store.loadOrCreate(), 100, new CreatorRuntimeEventLog(300));
    }

    /** Test-only lifecycle hook; production callers should use the application-scoped singleton. */
    public static synchronized void resetForTests() {
        instance = null;
    }

    public static CreatorRuntimeSession get(Context context) {
        if (instance == null) {
            synchronized (CreatorRuntimeSession.class) {
                if (instance == null) instance = new CreatorRuntimeSession(context);
            }
        }
        return instance;
    }

    /**
     * Imports a complete snapshot produced by an original Sketchware editor
     * surface. The import is persisted as a new runtime revision and is
     * broadcast through the same listener boundary as AI/user operations.
     */
    public synchronized CreatorProjectDocument importLegacySnapshot(CreatorProjectDocument imported) {
        if (imported == null) return getDocument();
        CreatorProjectDocument current = getDocument();
        CreatorProjectDocument next = new CreatorProjectDocument(
                current.getSchemaVersion(), current.getProjectId(), current.getRevision() + 1,
                current.getName(), imported.getEntryScreenId(), imported.getScreens(), imported.getWidgets(),
                imported.getEntryControl(), imported.getState(), imported.getEvents());
        String currentComparable = CreatorProjectDocumentCodec.encode(current.withRevision(current.getRevision()));
        String nextComparable = CreatorProjectDocumentCodec.encode(next.withRevision(current.getRevision()));
        if (currentComparable.equals(nextComparable)) return current;
        engine = new CreatorRuntimeEngine(next, 100, new CreatorRuntimeEventLog(300));
        store.save(next);
        for (Listener listener : listeners) listener.onDocumentChanged(next);
        return next;
    }

    public synchronized CreatorApplyResult apply(CreatorProjectOperation operation) {
        CreatorApplyResult result = engine.apply(operation);
        if (result.isApplied() && !result.isReplayed()) {
            store.save(result.getDocument());
            for (Listener listener : listeners) listener.onDocumentChanged(result.getDocument());
        }
        return result;
    }

    public CreatorRuntimeEngine getEngine() { return engine; }
    public CreatorProjectDocument getDocument() { return engine.getCurrent(); }

    public void addListener(Listener listener) { if (listener != null) listeners.addIfAbsent(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }
}
