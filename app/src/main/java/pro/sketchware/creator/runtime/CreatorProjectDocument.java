package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, versioned project document used by the live Creator Runtime.
 * The document is intentionally independent from legacy Sketchware storage.
 */
public final class CreatorProjectDocument {
    public static final int SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final String projectId;
    private final long revision;
    private final String name;
    private final String entryScreenId;
    private final Map<String, CreatorScreen> screens;
    private final Map<String, CreatorWidget> widgets;
    private final CreatorEntryControl entryControl;

    public CreatorProjectDocument(int schemaVersion, String projectId, long revision,
                                  String name, String entryScreenId,
                                  Map<String, CreatorScreen> screens,
                                  Map<String, CreatorWidget> widgets,
                                  CreatorEntryControl entryControl) {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported schema version");
        if (projectId == null || projectId.trim().isEmpty()) throw new IllegalArgumentException("projectId");
        if (revision < 0) throw new IllegalArgumentException("revision");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name");
        this.schemaVersion = schemaVersion;
        this.projectId = projectId;
        this.revision = revision;
        this.name = name;
        this.entryScreenId = entryScreenId;
        this.screens = Collections.unmodifiableMap(new LinkedHashMap<>(screens == null
                ? Collections.<String, CreatorScreen>emptyMap() : screens));
        this.widgets = Collections.unmodifiableMap(new LinkedHashMap<>(widgets == null
                ? Collections.<String, CreatorWidget>emptyMap() : widgets));
        this.entryControl = entryControl == null ? CreatorEntryControl.defaultControl() : entryControl;
    }

    public static CreatorProjectDocument empty(String projectId, String name) {
        return new CreatorProjectDocument(SCHEMA_VERSION, projectId, 0, name, null,
                Collections.<String, CreatorScreen>emptyMap(),
                Collections.<String, CreatorWidget>emptyMap(), CreatorEntryControl.defaultControl());
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getProjectId() { return projectId; }
    public long getRevision() { return revision; }
    public String getName() { return name; }
    public String getEntryScreenId() { return entryScreenId; }
    public Map<String, CreatorScreen> getScreens() { return screens; }
    public Map<String, CreatorWidget> getWidgets() { return widgets; }
    public CreatorEntryControl getEntryControl() { return entryControl; }

    public CreatorProjectDocument withState(long nextRevision, String nextEntryScreenId,
                                            Map<String, CreatorScreen> nextScreens,
                                            Map<String, CreatorWidget> nextWidgets,
                                            CreatorEntryControl nextEntryControl) {
        return new CreatorProjectDocument(schemaVersion, projectId, nextRevision, name,
                nextEntryScreenId, nextScreens, nextWidgets, nextEntryControl);
    }

    public CreatorProjectDocument withRevision(long nextRevision) {
        return withState(nextRevision, entryScreenId, screens, widgets, entryControl);
    }
}
