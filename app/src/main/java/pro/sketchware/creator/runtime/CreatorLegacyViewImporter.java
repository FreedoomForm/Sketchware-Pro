package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ViewBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Initial, explicit bridge from legacy Sketchware {@link ViewBean} records to
 * Project IR. Unsupported elements are retained in the report instead of being
 * silently discarded or misrepresented in the live preview.
 */
public final class CreatorLegacyViewImporter {
    public static final class Result {
        private final CreatorProjectDocument document;
        private final CreatorCompatibilityReport report;

        Result(CreatorProjectDocument document, CreatorCompatibilityReport report) {
            this.document = document;
            this.report = report;
        }

        public CreatorProjectDocument getDocument() { return document; }
        public CreatorCompatibilityReport getReport() { return report; }
    }

    public Result importLayout(String projectId, String projectName, String screenId,
                               String route, List<ViewBean> legacyViews) {
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        String rootId = "root_" + screenId;
        widgets.put(rootId, new CreatorWidget(rootId, "column", null, null, null));
        List<ViewBean> ordered = new ArrayList<>(legacyViews == null ? Collections.<ViewBean>emptyList() : legacyViews);
        Collections.sort(ordered, Comparator.comparingInt(view -> view.index));

        for (ViewBean view : ordered) {
            if (view == null || view.id == null || view.id.trim().isEmpty()) {
                report.add("unknown", "ViewBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Legacy view has no stable ID and cannot be imported safely.");
                continue;
            }
            String runtimeType = toRuntimeType(view.type);
            if (runtimeType == null) {
                report.add(view.id, ViewBean.getViewTypeName(view.type), tierForUnsupportedLegacyType(view.type),
                        "This legacy widget remains visible in compatibility reporting and requires a plugin or native fallback.");
                continue;
            }
            String parentId = view.parent == null || view.parent.trim().isEmpty() || "root".equals(view.parent)
                    ? rootId : view.parent;
            if (!widgets.containsKey(parentId)) {
                report.add(view.id, ViewBean.getViewTypeName(view.type), CreatorCompatibilityTier.R3_NATIVE_FALLBACK,
                        "Parent is not runtime-native, so this widget cannot be placed safely in the live preview.");
                continue;
            }
            Map<String, Object> properties = propertiesFrom(view);
            CreatorWidget widget = new CreatorWidget(view.id, runtimeType, parentId, null, properties);
            widgets.put(widget.getId(), widget);
            widgets.put(parentId, widgets.get(parentId).withChild(widget.getId(), view.index));
            report.add(view.id, ViewBean.getViewTypeName(view.type), CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Imported as Creator Runtime " + runtimeType + ".");
        }
        screens.put(screenId, new CreatorScreen(screenId, route, rootId));
        CreatorProjectDocument document = new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION,
                projectId, 0, projectName, screenId, screens, widgets, CreatorEntryControl.defaultControl());
        return new Result(document, report);
    }

    private static String toRuntimeType(int legacyType) {
        switch (legacyType) {
            case ViewBean.VIEW_TYPE_LAYOUT_LINEAR: return "column";
            case ViewBean.VIEW_TYPE_LAYOUT_RELATIVE: return "stack";
            case ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW: return "scroll";
            case ViewBean.VIEW_TYPE_WIDGET_BUTTON: return "button";
            case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW: return "text";
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT: return "input";
            case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW: return "image";
            case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX: return "checkbox";
            case ViewBean.VIEW_TYPE_WIDGET_SWITCH: return "switch";
            default: return null;
        }
    }

    private static CreatorCompatibilityTier tierForUnsupportedLegacyType(int legacyType) {
        if (legacyType == ViewBean.VIEW_TYPE_WIDGET_WEBVIEW || legacyType == ViewBean.VIEW_TYPE_WIDGET_MAPVIEW) {
            return CreatorCompatibilityTier.R2_RUNTIME_PLUGIN;
        }
        return CreatorCompatibilityTier.R3_NATIVE_FALLBACK;
    }

    private static Map<String, Object> propertiesFrom(ViewBean view) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (view.text != null) {
            properties.put("text", view.text.text == null ? "" : view.text.text);
            properties.put("hint", view.text.hint == null ? "" : view.text.hint);
            properties.put("textSize", view.text.textSize);
            properties.put("singleLine", view.text.singleLine != 0);
            properties.put("checked", view.checked != 0);
        }
        LayoutBean layout = view.layout;
        if (layout != null) {
            properties.put("padding", Math.max(0, layout.paddingLeft));
            properties.put("legacyWidth", layout.width);
            properties.put("legacyHeight", layout.height);
            properties.put("legacyGravity", layout.gravity);
            if (view.type == ViewBean.VIEW_TYPE_LAYOUT_LINEAR
                    && layout.orientation == LayoutBean.ORIENTATION_HORIZONTAL) {
                properties.put("orientation", "horizontal");
            }
        }
        properties.put("legacyType", ViewBean.getViewTypeName(view.type));
        return properties;
    }
}
