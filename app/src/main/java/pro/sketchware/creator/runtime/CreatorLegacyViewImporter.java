package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ViewBean;
import mod.agus.jcoderz.beans.ViewBeans;

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
                report.add(view.id, ViewBean.getViewTypeName(view.type), CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "No Creator Runtime widget mapping exists; import is blocked rather than using fallback execution.");
                continue;
            }
            String parentId = view.parent == null || view.parent.trim().isEmpty() || "root".equals(view.parent)
                    ? rootId : view.parent;
            if (!widgets.containsKey(parentId)) {
                report.add(view.id, ViewBean.getViewTypeName(view.type), CreatorCompatibilityTier.R0_UNSUPPORTED,
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
            case ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW: return "hscroll";
            case ViewBean.VIEW_TYPE_WIDGET_BUTTON: return "button";
            case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW: return "text";
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT: return "input";
            case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW: return "image";
            case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX: return "checkbox";
            case ViewBean.VIEW_TYPE_WIDGET_SWITCH: return "switch";
            case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR: return "progress";
            case ViewBean.VIEW_TYPE_WIDGET_SPINNER: return "spinner";
            case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR: return "slider";
            case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW: return "calendar_view";
            case ViewBean.VIEW_TYPE_WIDGET_FAB: return "fab";
            case ViewBean.VIEW_TYPE_WIDGET_WEBVIEW:
            case ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW: return "web";
            case ViewBean.VIEW_TYPE_WIDGET_MAPVIEW: return "map";
            case ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW: return "video";
            case ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW: return "lottie";
            case ViewBean.VIEW_TYPE_WIDGET_ADVIEW: return "ad_banner";
            case ViewBean.VIEW_TYPE_WIDGET_LISTVIEW: return "list";
            case ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW: return "list";
            case ViewBeans.VIEW_TYPE_WIDGET_GRIDVIEW: return "grid";
            case ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON: return "radio";
            case ViewBeans.VIEW_TYPE_WIDGET_RATINGBAR: return "rating";
            case ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW: return "search";
            case ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW:
            case ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW: return "autocomplete";
            case ViewBeans.VIEW_TYPE_WIDGET_ANALOGCLOCK:
            case ViewBeans.VIEW_TYPE_WIDGET_DIGITALCLOCK: return "clock";
            case ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER: return "date_picker";
            case ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER: return "time_picker";
            case ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT: return "tabs";
            case ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER: return "pager";
            case ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW: return "bottom_navigation";
            case ViewBeans.VIEW_TYPE_WIDGET_BADGEVIEW: return "badge";
            case ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW: return "pattern";
            case ViewBeans.VIEW_TYPE_WIDGET_WAVESIDEBAR: return "sidebar";
            case ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW: return "card";
            case ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT: return "collapsing";
            case ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT: return "text_input";
            case ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT: return "swipe_refresh";
            case ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP: return "radio_group";
            case ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON: return "button";
            case ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON: return "sign_in";
            case ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW: return "circle_image";
            case ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW: return "otp";
            case ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW: return "code";
            default: return null;
        }
    }

    private static Map<String, Object> propertiesFrom(ViewBean view) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (view.text != null) {
            properties.put("text", view.text.text == null ? "" : view.text.text);
            properties.put("hint", view.text.hint == null ? "" : view.text.hint);
            properties.put("textSize", view.text.textSize);
            properties.put("textFont", view.text.textFont == null ? "default_font" : view.text.textFont);
            properties.put("textType", view.text.textType);
            properties.put("textColor", colorReference(view.text.resTextColor, view.text.textColor));
            properties.put("hintTextColor", colorReference(view.text.resHintColor, view.text.hintColor));
            properties.put("singleLine", view.text.singleLine != 0);
            properties.put("checked", view.checked != 0);
            properties.put("progress", view.progress);
            properties.put("max", view.max);
        }
        if (view.image != null) {
            properties.put("resourceName", view.image.resName == null ? "" : view.image.resName);
            properties.put("rotation", view.image.rotate);
            properties.put("scaleType", view.image.scaleType == null ? "CENTER" : view.image.scaleType);
        }
        LayoutBean layout = view.layout;
        if (layout != null) {
            properties.put("paddingLeft", Math.max(0, layout.paddingLeft));
            properties.put("paddingTop", Math.max(0, layout.paddingTop));
            properties.put("paddingRight", Math.max(0, layout.paddingRight));
            properties.put("paddingBottom", Math.max(0, layout.paddingBottom));
            properties.put("marginLeft", Math.max(0, layout.marginLeft));
            properties.put("marginTop", Math.max(0, layout.marginTop));
            properties.put("marginRight", Math.max(0, layout.marginRight));
            properties.put("marginBottom", Math.max(0, layout.marginBottom));
            properties.put("legacyWidth", layout.width);
            properties.put("legacyHeight", layout.height);
            properties.put("legacyGravity", layout.gravity);
            properties.put("legacyLayoutGravity", layout.layoutGravity);
            properties.put("legacyWeight", layout.weight);
            properties.put("legacyWeightSum", layout.weightSum);
            properties.put("backgroundColor", colorReference(layout.backgroundResColor, layout.backgroundColor));
            properties.put("backgroundResource", layout.backgroundResource == null ? "" : layout.backgroundResource);
            if (view.type == ViewBean.VIEW_TYPE_LAYOUT_LINEAR
                    && layout.orientation == LayoutBean.ORIENTATION_HORIZONTAL) {
                properties.put("orientation", "horizontal");
            }
        }
        properties.put("legacyType", ViewBean.getViewTypeName(view.type));
        properties.put("adUnitId", view.adUnitId == null ? "" : view.adUnitId);
        return properties;
    }

    private static String colorReference(String resourceName, int color) {
        if (resourceName != null && !resourceName.trim().isEmpty()) {
            return resourceName.startsWith("@color/") ? resourceName : "@color/" + resourceName;
        }
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }
}
