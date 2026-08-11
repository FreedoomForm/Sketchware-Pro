package com.sketchware.ai.tools.library;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * library_enable — universal tool for enabling / disabling Sketchware's
 * built-in libraries (AppCompat, Material3, Firebase, AdMob, GoogleMap).
 *
 * <p><b>FIX-D-PROJECT (Task D1):</b> refactored from a single-action
 * SketchwareTool that used a non-standard {@code library_type} enum
 * parameter into a UniversalTool with 10 explicit {@code action}
 * values. This matches the pattern used by every other tool in the
 * catalogue and removes the only schema inconsistency flagged in the
 * coverage report (§4.4).
 *
 * <p>Underlying API (per {@code ManageLibraryActivity.saveLibraryConfiguration}
 * and {@code Material3LibraryManager}):
 * <ul>
 *   <li>{@code jC.c(scId).c()} — returns the compatLibraryBean
 *       (ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT).</li>
 *   <li>{@code jC.c(scId).c(bean)} — saves the compatLibraryBean back.</li>
 *   <li>{@code jC.c(scId).d()} / {@code .d(bean)} — firebaseLibraryBean.</li>
 *   <li>{@code jC.c(scId).b()} / {@code .b(bean)} — admobLibraryBean.</li>
 *   <li>{@code jC.c(scId).e()} / {@code .e(bean)} — googleMapLibraryBean.</li>
 *   <li>{@code jC.c(scId).k()} — commit/persist all changes.</li>
 * </ul>
 *
 * <p>Material3 is stored inside {@code compatLibraryBean.configurations}
 * under keys {@code "material3"}, {@code "theme"}, {@code "dynamic_colors"}
 * — see {@code Material3LibraryActivity}. Material3 REQUIRES AppCompat to
 * be enabled first; this is enforced here.
 *
 * <p>Disabling AppCompat while Firebase or Material3 is enabled is blocked
 * (matches the UI's behaviour in {@code ManageCompatActivity.onClick}).
 */
public final class LibraryEnableTool extends UniversalTool {

    public LibraryEnableTool() {
        super("library_enable",
                "Enable or disable a built-in Sketchware library (AppCompat, "
                        + "Material3, Firebase, AdMob, GoogleMap). Material3 requires "
                        + "AppCompat to be enabled first. Disabling AppCompat while "
                        + "Firebase or Material3 are enabled is blocked.",
                "library", false, false,
                "enable_compat", "disable_compat",
                "enable_firebase", "disable_firebase",
                "enable_admob", "disable_admob",
                "enable_googlemap", "disable_googlemap",
                "enable_material3", "disable_material3");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        // No extra parameters — every action is implicit.
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        try {
            Object iC = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            switch (action) {
                case "enable_compat":   return enableSimple(iC, "c", "AppCompat");
                case "disable_compat":  return disableCompat(iC);
                case "enable_firebase": return enableSimple(iC, "d", "Firebase");
                case "disable_firebase": return disableSimple(iC, "d", "Firebase");
                case "enable_admob":    return enableSimple(iC, "b", "AdMob");
                case "disable_admob":   return disableSimple(iC, "b", "AdMob");
                case "enable_googlemap": return enableSimple(iC, "e", "GoogleMap");
                case "disable_googlemap": return disableSimple(iC, "e", "GoogleMap");
                case "enable_material3":  return enableMaterial3(iC);
                case "disable_material3": return disableMaterial3(iC);
                default: return err("Unknown action: " + action);
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Generic enable/disable for libraries whose state lives in
    //  ProjectLibraryBean.useYn ("Y"/"N").
    // ------------------------------------------------------------------
    private ToolResult enableSimple(Object iC, String getter, String displayName) {
        try {
            ProjectLibraryBean bean = (ProjectLibraryBean) SketchwareApi.invoke(iC, getter);
            if (bean == null) {
                return err("Could not load " + displayName + " library bean "
                        + "(jC.c(scId)." + getter + "() returned null).");
            }
            bean.useYn = ProjectLibraryBean.LIB_USE_Y;
            SketchwareApi.invoke(iC, getter, bean);
            SketchwareApi.invoke(iC, "k");
            return ok("Enabled " + displayName + " library.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult disableSimple(Object iC, String getter, String displayName) {
        try {
            ProjectLibraryBean bean = (ProjectLibraryBean) SketchwareApi.invoke(iC, getter);
            if (bean == null) {
                return err("Could not load " + displayName + " library bean.");
            }
            bean.useYn = ProjectLibraryBean.LIB_USE_N;
            SketchwareApi.invoke(iC, getter, bean);
            SketchwareApi.invoke(iC, "k");
            return ok("Disabled " + displayName + " library.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  AppCompat — blocking disable if Firebase or Material3 is enabled.
    // ------------------------------------------------------------------
    private ToolResult disableCompat(Object iC) {
        try {
            ProjectLibraryBean compatBean = (ProjectLibraryBean) SketchwareApi.invoke(iC, "c");
            if (compatBean == null) return err("Could not load AppCompat library bean.");

            ProjectLibraryBean firebaseBean = (ProjectLibraryBean) SketchwareApi.invoke(iC, "d");
            boolean firebaseOn = firebaseBean != null && firebaseBean.isEnabled();
            boolean material3On = isMaterial3Enabled(compatBean);

            if (firebaseOn || material3On) {
                StringBuilder why = new StringBuilder();
                if (firebaseOn) why.append("Firebase is enabled. ");
                if (material3On) why.append("Material3 is enabled. ");
                return err("Cannot disable AppCompat because: " + why.toString()
                        + "Disable those libraries first (library_enable:disable_firebase, "
                        + "library_enable:disable_material3).");
            }

            compatBean.useYn = ProjectLibraryBean.LIB_USE_N;
            SketchwareApi.invoke(iC, "c", compatBean);
            SketchwareApi.invoke(iC, "k");
            return ok("Disabled AppCompat library.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Material3 — settings live inside compatLibraryBean.configurations.
    // ------------------------------------------------------------------
    private ToolResult enableMaterial3(Object iC) {
        try {
            ProjectLibraryBean compatBean = (ProjectLibraryBean) SketchwareApi.invoke(iC, "c");
            if (compatBean == null) return err("Could not load AppCompat library bean.");
            if (!compatBean.isEnabled()) {
                return err("Material3 requires AppCompat to be enabled first. "
                        + "Call library_enable:enable_compat before this action.");
            }
            if (compatBean.configurations == null) compatBean.configurations = new java.util.HashMap<>();
            compatBean.configurations.put("material3", Boolean.TRUE);
            // Default theme to DayNight if not previously set (matches UI default).
            if (!(compatBean.configurations.get("theme") instanceof String)) {
                compatBean.configurations.put("theme", "DayNight");
            }
            SketchwareApi.invoke(iC, "c", compatBean);
            SketchwareApi.invoke(iC, "k");
            return ok("Enabled Material3 library (theme=DayNight by default; "
                    + "use library_configure:material3_set_theme to change).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult disableMaterial3(Object iC) {
        try {
            ProjectLibraryBean compatBean = (ProjectLibraryBean) SketchwareApi.invoke(iC, "c");
            if (compatBean == null) return err("Could not load AppCompat library bean.");
            if (compatBean.configurations != null) {
                compatBean.configurations.put("material3", Boolean.FALSE);
                compatBean.configurations.put("dynamic_colors", Boolean.FALSE);
            }
            SketchwareApi.invoke(iC, "c", compatBean);
            SketchwareApi.invoke(iC, "k");
            return ok("Disabled Material3 library (also turned off dynamic_colors).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Mirrors {@code Material3LibraryManager.isMaterial3Enabled()}. */
    private static boolean isMaterial3Enabled(ProjectLibraryBean compatBean) {
        if (compatBean == null || !compatBean.isEnabled() || compatBean.configurations == null) {
            return false;
        }
        Object v = compatBean.configurations.get("material3");
        return v instanceof Boolean && (Boolean) v;
    }
}
