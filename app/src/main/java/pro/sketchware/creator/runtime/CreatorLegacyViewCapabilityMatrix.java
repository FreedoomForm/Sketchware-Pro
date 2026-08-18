package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.ViewBean;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import mod.agus.jcoderz.beans.ViewBeans;

/** Explicit migration classification for every legacy ViewBean type 0 through 48. */
public final class CreatorLegacyViewCapabilityMatrix {
    private static final Map<Integer, CreatorCompatibilityTier> TIERS;
    static {
        Map<Integer, CreatorCompatibilityTier> tiers = new LinkedHashMap<>();
        r1(tiers, ViewBean.VIEW_TYPE_LAYOUT_LINEAR, ViewBean.VIEW_TYPE_LAYOUT_RELATIVE,
                ViewBean.VIEW_TYPE_WIDGET_BUTTON, ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW,
                ViewBean.VIEW_TYPE_WIDGET_EDITTEXT, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW,
                ViewBean.VIEW_TYPE_WIDGET_CHECKBOX, ViewBean.VIEW_TYPE_WIDGET_SWITCH,
                ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW, ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW,
                ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR, ViewBean.VIEW_TYPE_WIDGET_SPINNER,
                ViewBean.VIEW_TYPE_WIDGET_SEEKBAR, ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW,
                ViewBean.VIEW_TYPE_WIDGET_FAB, ViewBean.VIEW_TYPE_WIDGET_LISTVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW, ViewBeans.VIEW_TYPE_WIDGET_GRIDVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON, ViewBeans.VIEW_TYPE_WIDGET_RATINGBAR,
                ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW, ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW, ViewBeans.VIEW_TYPE_WIDGET_ANALOGCLOCK,
                ViewBeans.VIEW_TYPE_WIDGET_DIGITALCLOCK, ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER,
                ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER, ViewBean.VIEW_TYPE_WIDGET_WEBVIEW,
                ViewBean.VIEW_TYPE_WIDGET_MAPVIEW, ViewBean.VIEW_TYPE_WIDGET_ADVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW, ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW, ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT,
                ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER, ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_BADGEVIEW, ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_WAVESIDEBAR,
                ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW, ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
                ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT, ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT,
                ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP, ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON,
                ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON, ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW,
                ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW, ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW);
        TIERS = Collections.unmodifiableMap(tiers);
    }

    private CreatorLegacyViewCapabilityMatrix() { }
    public static Map<Integer, CreatorCompatibilityTier> all() { return TIERS; }
    public static CreatorCompatibilityTier tierFor(int type) { return TIERS.get(type); }
    public static boolean isComplete() { return TIERS.size() == ViewBean.VIEW_TYPE_COUNT + 30; }
    private static void r1(Map<Integer, CreatorCompatibilityTier> m, int... types) { put(m, CreatorCompatibilityTier.R1_RUNTIME_NATIVE, types); }
    private static void put(Map<Integer, CreatorCompatibilityTier> m, CreatorCompatibilityTier tier, int... types) {
        for (int type : types) m.put(type, tier);
    }
}
