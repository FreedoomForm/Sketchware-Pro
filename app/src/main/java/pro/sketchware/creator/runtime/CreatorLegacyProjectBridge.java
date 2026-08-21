package pro.sketchware.creator.runtime;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a.a.a.GB;
import a.a.a.eC;
import a.a.a.hC;
import a.a.a.iC;
import a.a.a.jC;
import a.a.a.lC;
import a.a.a.nB;
import a.a.a.oB;
import a.a.a.wq;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.TextBean;
import com.besome.sketch.beans.ViewBean;
import mod.hey.studios.project.ProjectSettings;

/**
 * Compatibility boundary between the Creator Runtime document identity and
 * the original Sketchware editor's legacy project store.
 *
 * <p>The original editor remains the UI and continues to use its existing
 * ProjectBean/ProjectFileBean/ViewBean/BlockBean code. This bridge only
 * provisions a valid legacy project once and keeps its sc_id stable for the
 * corresponding Creator Runtime project.</p>
 */
public final class CreatorLegacyProjectBridge {
    private static final String PREFS = "creator_runtime_legacy_bridge";
    private static final String SC_ID_PREFIX = "legacy_sc_id_";

    private CreatorLegacyProjectBridge() {
    }

    public static synchronized String ensureLegacyProject(Context context,
                                                            CreatorProjectDocument document) {
        if (context == null) throw new IllegalArgumentException("context");
        if (document == null) throw new IllegalArgumentException("document");

        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = SC_ID_PREFIX + document.getProjectId();
        String existingScId = preferences.getString(key, null);
        if (existingScId != null && lC.b(existingScId) != null) {
            return existingScId;
        }

        String scId = lC.b();
        provisionLegacyProject(context, scId, document.getName());
        preferences.edit()
                .putString(key, scId)
                .putString("runtime_project_id_" + scId, document.getProjectId())
                .apply();
        return scId;
    }

    /** Returns the runtime project ID associated with a legacy editor sc_id, if any. */
    public static String runtimeProjectIdFor(Context context, String scId) {
        if (context == null || scId == null || scId.trim().isEmpty()) return null;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("runtime_project_id_" + scId, null);
    }

    /**
     * Mirrors the authoritative runtime metadata into the legacy project store
     * so the original DesignActivity toolbar, project lists and managers keep
     * displaying the same project identity.
     */
    public static synchronized void syncRuntimeMetadata(Context context,
                                                         CreatorProjectDocument document,
                                                         String scId) {
        if (context == null || document == null || scId == null || lC.b(scId) == null) return;
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("my_ws_name", document.getName());
        metadata.put("my_app_name", document.getName());
        metadata.put("sketchware_ver", GB.d(context.getApplicationContext()));
        lC.b(scId, metadata);
    }

    /**
     * Projects the runtime widget tree into the original ViewBean store. The
     * legacy editor and generator continue to own their existing UI and
     * persistence lifecycle; this method only updates the compatible records
     * and then invokes eC's normal view serialization.
     */
    public static synchronized void projectRuntimeViews(Context context,
                                                         CreatorProjectDocument document,
                                                         String scId) {
        if (context == null || document == null || scId == null || lC.b(scId) == null) return;
        eC viewStore = jC.a(scId);
        if (document.getScreens().isEmpty()) {
            projectScreen(viewStore, document, null, "main.xml");
        } else {
            for (CreatorScreen screen : document.getScreens().values()) {
                if (screen == null) continue;
                projectScreen(viewStore, document, screen, screen.getId() + ".xml");
            }
        }
        viewStore.n(wq.b(scId) + File.separator + "view");
    }

    private static void projectScreen(eC viewStore, CreatorProjectDocument document,
                                      CreatorScreen screen, String xmlName) {
        ArrayList<ViewBean> projected = new ArrayList<>();
        String rootId = screen == null ? null : screen.getRootWidgetId();
        for (CreatorWidget widget : document.getWidgets().values()) {
            if (widget == null || widget.getId() == null || widget.getId().startsWith("root_")) continue;
            if (rootId != null && !belongsToRoot(document, widget, rootId)) continue;
            int legacyType = toLegacyType(widget.getType());
            if (legacyType < 0) continue;
            ViewBean view = new ViewBean(widget.getId(), legacyType);
            view.id = widget.getId();
            view.name = widget.getId();
            view.type = legacyType;
            view.parent = widget.getParentId() == null || widget.getParentId().startsWith("root_")
                    ? null : widget.getParentId();
            view.parentType = -1;
            List<String> children = widget.getParentId() == null
                    ? java.util.Collections.<String>emptyList()
                    : document.getWidgets().containsKey(widget.getParentId())
                    ? document.getWidgets().get(widget.getParentId()).getChildren()
                    : java.util.Collections.<String>emptyList();
            view.index = Math.max(0, children.indexOf(widget.getId()));
            applyWidgetProperties(view, widget.getProperties());
            projected.add(view);
        }
        viewStore.c.put(xmlName, projected);
    }

    private static boolean belongsToRoot(CreatorProjectDocument document, CreatorWidget widget, String rootId) {
        String current = widget.getParentId();
        int guard = document.getWidgets().size() + 1;
        while (current != null && guard-- > 0) {
            if (rootId.equals(current)) return true;
            CreatorWidget parent = document.getWidgets().get(current);
            current = parent == null ? null : parent.getParentId();
        }
        return false;
    }

    /** Imports all legacy editor state back into the runtime document after an original editor surface returns. */
    public static synchronized CreatorProjectDocument importLegacyProject(Context context,
                                                                           CreatorProjectDocument current,
                                                                           String scId) {
        if (context == null || current == null || scId == null || lC.b(scId) == null) return current;
        eC viewStore = jC.a(scId);
        hC fileStore = jC.b(scId);
        ArrayList<ProjectFileBean> files = fileStore == null ? new ArrayList<>() : fileStore.b();
        if (files == null || files.isEmpty()) files = new ArrayList<>();
        if (files.isEmpty()) files.add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));

        Map<String, CreatorScreen> screens = new java.util.LinkedHashMap<>();
        Map<String, CreatorWidget> widgets = new java.util.LinkedHashMap<>();
        ArrayList<ComponentBean> components = new ArrayList<>();
        ArrayList<EventBean> events = new ArrayList<>();
        Map<String, List<BlockBean>> blocksByEvent = new java.util.LinkedHashMap<>();
        for (ProjectFileBean file : files) {
            if (file == null || file.fileName == null) continue;
            String screenId = file.fileName;
            String rootId = "root_" + screenId;
            CreatorLegacyViewImporter.Result imported = new CreatorLegacyViewImporter().importLayout(
                    current.getProjectId(), current.getName(), screenId, "/" + screenId,
                    viewStore.d(file.getXmlName()));
            screens.putAll(imported.getDocument().getScreens());
            widgets.putAll(imported.getDocument().getWidgets());
            ArrayList<ComponentBean> fileComponents = viewStore.e(file.getXmlName());
            if (fileComponents != null) components.addAll(fileComponents);
            ArrayList<EventBean> fileEvents = viewStore.g(file.getXmlName());
            if (fileEvents != null) {
                events.addAll(fileEvents);
                for (EventBean event : fileEvents) {
                    if (event != null) blocksByEvent.put(event.getEventKey(), viewStore.a(file.getXmlName(), event.getEventKey()));
                }
            }
        }
        CreatorProjectDocument importedDocument = new CreatorProjectDocument(
                current.getSchemaVersion(), current.getProjectId(), current.getRevision(), current.getName(),
                screens.containsKey(current.getEntryScreenId()) ? current.getEntryScreenId()
                        : (screens.isEmpty() ? current.getEntryScreenId() : screens.keySet().iterator().next()),
                screens, widgets, current.getEntryControl(), current.getState(), current.getEvents());
        CreatorLegacyArtifactImporter.Result artifacts = new CreatorLegacyArtifactImporter().importArtifacts(
                importedDocument, components, events, blocksByEvent);
        CreatorLegacyArtifactImporter.Result metadata = new CreatorLegacyArtifactImporter().importProjectMetadata(
                artifacts.getDocument(), files, collectLibraries(scId));
        return metadata.getDocument();
    }

    private static ArrayList<ProjectLibraryBean> collectLibraries(String scId) {
        ArrayList<ProjectLibraryBean> result = new ArrayList<>();
        iC libraries = jC.c(scId);
        if (libraries == null) return result;
        if (libraries.b() != null) result.add(libraries.b());
        if (libraries.c() != null) result.add(libraries.c());
        if (libraries.d() != null) result.add(libraries.d());
        if (libraries.e() != null) result.add(libraries.e());
        return result;
    }

    private static int toLegacyType(String runtimeType) {
        if (runtimeType == null) return -1;
        switch (runtimeType) {
            case "column": return ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            case "stack": return ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
            case "scroll": return ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW;
            case "hscroll": return ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW;
            case "button": return ViewBean.VIEW_TYPE_WIDGET_BUTTON;
            case "text": return ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW;
            case "input": return ViewBean.VIEW_TYPE_WIDGET_EDITTEXT;
            case "image": return ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW;
            case "web": return ViewBean.VIEW_TYPE_WIDGET_WEBVIEW;
            case "progress": return ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR;
            case "list": return ViewBean.VIEW_TYPE_WIDGET_LISTVIEW;
            case "spinner": return ViewBean.VIEW_TYPE_WIDGET_SPINNER;
            case "checkbox": return ViewBean.VIEW_TYPE_WIDGET_CHECKBOX;
            case "switch": return ViewBean.VIEW_TYPE_WIDGET_SWITCH;
            case "slider": return ViewBean.VIEW_TYPE_WIDGET_SEEKBAR;
            case "calendar_view": return ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW;
            case "fab": return ViewBean.VIEW_TYPE_WIDGET_FAB;
            case "ad_banner": return ViewBean.VIEW_TYPE_WIDGET_ADVIEW;
            case "map": return ViewBean.VIEW_TYPE_WIDGET_MAPVIEW;
            default: return -1;
        }
    }

    private static void applyWidgetProperties(ViewBean view, Map<String, Object> properties) {
        if (properties == null) return;
        TextBean text = view.text == null ? new TextBean() : view.text;
        if (properties.get("text") != null) text.text = String.valueOf(properties.get("text"));
        if (properties.get("hint") != null) text.hint = String.valueOf(properties.get("hint"));
        if (properties.get("textSize") instanceof Number) text.textSize = ((Number) properties.get("textSize")).intValue();
        if (properties.get("singleLine") instanceof Boolean) text.singleLine = ((Boolean) properties.get("singleLine")) ? 1 : 0;
        view.text = text;
        if (properties.get("checked") instanceof Boolean) view.checked = ((Boolean) properties.get("checked")) ? 1 : 0;
        if (properties.get("progress") instanceof Number) view.progress = ((Number) properties.get("progress")).intValue();
        if (properties.get("max") instanceof Number) view.max = ((Number) properties.get("max")).intValue();
        LayoutBean layout = view.layout == null ? new LayoutBean() : view.layout;
        setInt(properties, "paddingLeft", value -> layout.paddingLeft = value);
        setInt(properties, "paddingTop", value -> layout.paddingTop = value);
        setInt(properties, "paddingRight", value -> layout.paddingRight = value);
        setInt(properties, "paddingBottom", value -> layout.paddingBottom = value);
        setInt(properties, "marginLeft", value -> layout.marginLeft = value);
        setInt(properties, "marginTop", value -> layout.marginTop = value);
        setInt(properties, "marginRight", value -> layout.marginRight = value);
        setInt(properties, "marginBottom", value -> layout.marginBottom = value);
        if ("horizontal".equals(properties.get("orientation"))) layout.orientation = LayoutBean.ORIENTATION_HORIZONTAL;
        view.layout = layout;
    }

    private interface IntConsumer { void accept(int value); }

    private static void setInt(Map<String, Object> properties, String key, IntConsumer consumer) {
        Object value = properties.get(key);
        if (value instanceof Number) consumer.accept(((Number) value).intValue());
    }

    private static void provisionLegacyProject(Context context, String scId, String projectName) {
        String safeName = safeName(projectName);
        String packageName = "com.my." + safeName.toLowerCase();

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("sc_id", scId);
        metadata.put("my_ws_name", safeName);
        metadata.put("my_app_name", projectName == null || projectName.trim().isEmpty()
                ? safeName : projectName.trim());
        metadata.put("my_sc_pkg_name", packageName);
        metadata.put("my_sc_reg_dt", new nB().a("yyyyMMddHHmmss"));
        metadata.put("sc_ver_code", "1");
        metadata.put("sc_ver_name", "1.0");
        metadata.put("sketchware_ver", GB.d(context.getApplicationContext()));
        metadata.put("custom_icon", false);
        metadata.put("isIconAdaptive", false);
        metadata.put("proj_type", 1);
        metadata.put("color_accent", 0xff3f51b5);
        metadata.put("color_primary", 0xff3f51b5);
        metadata.put("color_primary_dark", 0xff303f9f);
        metadata.put("color_control_highlight", 0x333f51b5);
        metadata.put("color_control_normal", 0xff757575);

        lC.a(scId, metadata);
        createLegacyDirectories(scId);
        wq.a(context.getApplicationContext(), scId);
        new oB().b(wq.b(scId));

        ProjectSettings settings = new ProjectSettings(scId);
        settings.setValue(ProjectSettings.SETTING_NEW_XML_COMMAND,
                ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
        settings.setValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING,
                ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
    }

    private static void createLegacyDirectories(String scId) {
        oB fileUtility = new oB();
        fileUtility.f(wq.e() + File.separator + scId);
        fileUtility.f(wq.g() + File.separator + scId);
        fileUtility.f(wq.t() + File.separator + scId);
        fileUtility.f(wq.d() + File.separator + scId);
    }

    private static String safeName(String value) {
        String source = value == null ? "UntitledProject" : value.trim();
        if (source.isEmpty()) source = "UntitledProject";
        String safe = source.replaceAll("[^A-Za-z0-9_]", "");
        if (safe.isEmpty()) safe = "UntitledProject";
        if (Character.isDigit(safe.charAt(0))) safe = "Project" + safe;
        return safe;
    }
}
