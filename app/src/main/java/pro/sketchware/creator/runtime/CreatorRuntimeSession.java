package pro.sketchware.creator.runtime;

import android.content.Context;

import java.util.UUID;
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

    /** Creates and persists a fresh active project for the editor sidebar. */
    public synchronized CreatorProjectDocument createNewProject(String name) {
        String safeName = name == null || name.trim().isEmpty() ? "Untitled project" : name.trim();
        CreatorProjectDocument created = CreatorProjectDocument.empty(
                "creator_" + UUID.randomUUID().toString(), safeName);
        engine = new CreatorRuntimeEngine(created, 100, new CreatorRuntimeEventLog(300));
        store.save(created);
        for (Listener listener : listeners) listener.onDocumentChanged(created);
        return created;
    }
    public void addListener(Listener listener) { if (listener != null) listeners.addIfAbsent(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }
}
