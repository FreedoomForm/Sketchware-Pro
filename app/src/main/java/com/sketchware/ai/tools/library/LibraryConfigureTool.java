package com.sketchware.ai.tools.library;

import com.besome.sketch.beans.AdUnitBean;
import com.besome.sketch.beans.AdTestDeviceBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.utility.FileUtil;

/**
 * library_configure — universal tool for library-specific configuration
 * of built-in libraries (Firebase, AdMob, GoogleMap, Material3).
 *
 * <p><b>FIX-D-PROJECT (Task D2):</b> new tool. Closes coverage-report
 * gap §1.11 — previously only the enable/disable toggle was exposed;
 * the per-library configuration wizard (API keys, ad units, themes,
 * import-from-other-project) was unreachable from the AI layer.
 *
 * <p>Underlying storage (per {@code ManageFirebaseActivity},
 * {@code AdmobActivity}, {@code ManageGoogleMapActivity},
 * {@code Material3LibraryActivity}):
 *
 * <ul>
 *   <li><b>Firebase</b> bean fields: {@code data} = realtime DB URL,
 *       {@code reserved1} = app_id, {@code reserved2} = api_key,
 *       {@code reserved3} = storage_bucket, {@code useYn} = enabled.</li>
 *   <li><b>AdMob</b> bean fields: {@code appId} = app_id,
 *       {@code adUnits} = {@code ArrayList<AdUnitBean>},
 *       {@code reserved1} = assigned banner unit ("name : id" or "id"),
 *       {@code reserved2} = assigned interstitial unit,
 *       {@code reserved3} = assigned reward unit,
 *       {@code testDevices} = {@code ArrayList<AdTestDeviceBean>}.</li>
 *   <li><b>GoogleMap</b> bean fields: {@code data} = API key,
 *       {@code useYn} = enabled.</li>
 *   <li><b>Material3</b> (lives inside compatLibraryBean.configurations):
 *       {@code material3}=Boolean, {@code theme}=String (DayNight/Light/Dark),
 *       {@code dynamic_colors}=Boolean.</li>
 * </ul>
 *
 * <p>Import-from-other-project uses a fresh {@code iC} instance bound
 * to the source sc_id (mirrors {@code LibrarySettingsImporter}).
 */
public final class LibraryConfigureTool extends UniversalTool {

    /** Valid ABIs / native-lib folder names (used for import paths). */
    private static final Pattern APP_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9:_.\\-]{8,}$");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{20,}$");
    private static final Pattern AD_UNIT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    public LibraryConfigureTool() {
        super("library_configure",
                "Configure built-in libraries: Firebase (import google-services.json, "
                        + "set storage bucket / app_id / api_key / firebase_url, import from project), "
                        + "AdMob (set_app_id, add_ad_unit, assign_ad_unit, add_test_device, import from project), "
                        + "GoogleMap (set_api_key, import from project), "
                        + "Material3 (set_theme DayNight/Light/Dark, toggle_dynamic_colors). "
                        + "Call library_enable:* first to enable the library.",
                "library", false, false,
                // Firebase
                "firebase_import_google_services_json",
                "firebase_import_from_project",
                "firebase_set_storage_bucket",
                "firebase_set_app_id",
                "firebase_set_api_key",
                "firebase_set_firebase_url",
                // AdMob
                "admob_set_app_id",
                "admob_add_ad_unit",
                "admob_assign_ad_unit",
                "admob_add_test_device",
                "admob_import_from_project",
                // GoogleMap
                "googlemap_set_api_key",
                "googlemap_import_from_project",
                // Material3
                "material3_set_theme",
                "material3_toggle_dynamic_colors");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        JsonObject p;

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(firebase_import_google_services_json) Raw contents of google-services.json. Mutually exclusive with file_path.");
        props.add("json_content", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(firebase_import_google_services_json) Path to a google-services.json file on disk. Used only if json_content is absent.");
        props.add("file_path", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(firebase_import_from_project / admob_import_from_project / googlemap_import_from_project) Source project sc_id to copy settings from.");
        props.add("source_sc_id", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(firebase_set_storage_bucket / firebase_set_app_id / firebase_set_api_key / firebase_set_firebase_url / admob_set_app_id / googlemap_set_api_key) The value to set.");
        props.add("value", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(admob_add_ad_unit / admob_assign_ad_unit) Human-friendly name of the ad unit (1-50 chars).");
        props.add("unit_name", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(admob_add_ad_unit) Ad unit ID (alphanumeric).");
        props.add("unit_id", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(admob_add_ad_unit) Ad unit type: banner / interstitial / reward.");
        props.add("type", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(admob_assign_ad_unit) Target slot to assign the ad unit to: banner / interstitial / reward.");
        props.add("target_widget_id", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(admob_add_test_device) Test device ID (hex hash, typically 32+ chars).");
        props.add("device_id", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(material3_set_theme) Theme variant: DayNight / Light / Dark.");
        props.add("theme", p);

        p = new JsonObject();
        p.addProperty("type", "boolean");
        p.addProperty("description", "(material3_toggle_dynamic_colors) true to enable dynamic colors, false to disable.");
        props.add("enabled", p);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        try {
            switch (action) {
                // ---------- Firebase ----------
                case "firebase_import_google_services_json": return firebaseImportJson(ctx, scId, args);
                case "firebase_import_from_project":         return importFromProject(ctx, scId, args, "d", "Firebase");
                case "firebase_set_storage_bucket":          return setFirebaseField(ctx, scId, args, "reserved3", "storage_bucket");
                case "firebase_set_app_id":                  return setFirebaseField(ctx, scId, args, "reserved1", "app_id");
                case "firebase_set_api_key":                 return setFirebaseField(ctx, scId, args, "reserved2", "api_key");
                case "firebase_set_firebase_url":            return setFirebaseField(ctx, scId, args, "data", "firebase_url");

                // ---------- AdMob ----------
                case "admob_set_app_id":                     return setAdmobAppId(ctx, scId, args);
                case "admob_add_ad_unit":                    return admobAddAdUnit(ctx, scId, args);
                case "admob_assign_ad_unit":                 return admobAssignAdUnit(ctx, scId, args);
                case "admob_add_test_device":                return admobAddTestDevice(ctx, scId, args);
                case "admob_import_from_project":            return importFromProject(ctx, scId, args, "b", "AdMob");

                // ---------- GoogleMap ----------
                case "googlemap_set_api_key":                return setGoogleMapApiKey(ctx, scId, args);
                case "googlemap_import_from_project":        return importFromProject(ctx, scId, args, "e", "GoogleMap");

                // ---------- Material3 ----------
                case "material3_set_theme":                  return material3SetTheme(ctx, scId, args);
                case "material3_toggle_dynamic_colors":      return material3ToggleDynamicColors(ctx, scId, args);

                default: return err("Unknown action: " + action);
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  Helpers — shared iC / bean access
    // ==================================================================

    private static Object getIC(String scId) throws Exception {
        return SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
    }

    private static ProjectLibraryBean getBean(Object iC, String getter) throws Exception {
        return (ProjectLibraryBean) SketchwareApi.invoke(iC, getter);
    }

    private static void saveBean(Object iC, String getter, ProjectLibraryBean bean) throws Exception {
        SketchwareApi.invoke(iC, getter, bean);
        SketchwareApi.invoke(iC, "k");
    }

    // ==================================================================
    //  Firebase
    // ==================================================================

    private ToolResult firebaseImportJson(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String json = optString(args, "json_content");
        String filePath = optString(args, "file_path");
        if (json == null && filePath == null) {
            return err("Either json_content or file_path is required.");
        }
        if (json == null) {
            if (!FileUtil.isExistFile(filePath)) {
                return err("File not found: " + filePath);
            }
            json = FileUtil.readFile(filePath);
        }

        String storageBucket = extractJsonField(json, "storage_bucket");
        String appId = extractJsonField(json, "mobilesdk_app_id");
        String apiKey = extractJsonField(json, "current_key");
        String firebaseUrl = extractJsonField(json, "firebase_url");
        // Realtime DB URL is stored without the https:// prefix.
        if (firebaseUrl != null) firebaseUrl = firebaseUrl.replace("https://", "");

        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "d");
        if (bean == null) return err("Could not load Firebase library bean.");
        bean.data = nullToEmpty(firebaseUrl);
        bean.reserved1 = nullToEmpty(appId);
        bean.reserved2 = nullToEmpty(apiKey);
        bean.reserved3 = nullToEmpty(storageBucket);
        // Enable Firebase implicitly if all 4 fields are populated.
        if (!firebaseUrl.isEmpty() && !appId.isEmpty() && !apiKey.isEmpty() && !storageBucket.isEmpty()) {
            bean.useYn = ProjectLibraryBean.LIB_USE_Y;
        }
        saveBean(iC, "d", bean);

        return ok("Imported google-services.json into Firebase config. "
                + "storage_bucket='" + storageBucket + "', "
                + "app_id='" + appId + "', "
                + "api_key='" + mask(apiKey) + "', "
                + "firebase_url='" + firebaseUrl + "'. "
                + (bean.isEnabled() ? "Firebase has been auto-enabled." : "Some fields were missing; Firebase remains disabled until you call library_enable:enable_firebase."));
    }

    private ToolResult setFirebaseField(SketchwareToolContext ctx, String scId, JsonObject args,
                                          String fieldName, String displayName) throws Exception {
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value is required.");
        // Soft validation for known formats.
        if (fieldName.equals("reserved1") && !APP_ID_PATTERN.matcher(value).matches()) {
            return err("app_id '" + value + "' does not look like a Firebase mobilesdk_app_id (expected 1: digits, colons, letters).");
        }
        if (fieldName.equals("reserved2") && !API_KEY_PATTERN.matcher(value).matches()) {
            return err("api_key '" + value + "' does not look like a Firebase current_key (expected 20+ alphanumeric chars).");
        }
        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "d");
        if (bean == null) return err("Could not load Firebase library bean.");
        switch (fieldName) {
            case "data": bean.data = value; break;
            case "reserved1": bean.reserved1 = value; break;
            case "reserved2": bean.reserved2 = value; break;
            case "reserved3": bean.reserved3 = value; break;
        }
        saveBean(iC, "d", bean);
        return ok("Set Firebase " + displayName + " = '" + mask(value) + "'.");
    }

    // ==================================================================
    //  AdMob
    // ==================================================================

    private ToolResult setAdmobAppId(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String appId = optString(args, "value");
        if (appId == null || appId.isEmpty()) return err("value (app_id) is required.");
        if (!APP_ID_PATTERN.matcher(appId).matches()) {
            return err("AdMob app_id '" + appId + "' does not look like a valid ca-app-pub-XXXX~YYYY identifier.");
        }
        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "b");
        if (bean == null) return err("Could not load AdMob library bean.");
        bean.appId = appId;
        // Auto-enable AdMob once app_id is set (matches UI step-1 behaviour).
        bean.useYn = ProjectLibraryBean.LIB_USE_Y;
        saveBean(iC, "b", bean);
        return ok("Set AdMob app_id = '" + appId + "' and enabled the AdMob library.");
    }

    private ToolResult admobAddAdUnit(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String name = optString(args, "unit_name");
        String id = optString(args, "unit_id");
        String type = optString(args, "type");
        if (name == null || name.isEmpty()) return err("unit_name is required (1-50 chars).");
        if (id == null || id.isEmpty()) return err("unit_id is required.");
        if (!AD_UNIT_ID_PATTERN.matcher(id).matches()) {
            return err("unit_id '" + id + "' must be alphanumeric (real AdMob unit IDs are pure alphanumeric).");
        }
        if (type == null) return err("type is required (banner / interstitial / reward).");
        String normType = type.toLowerCase();
        if (!normType.equals("banner") && !normType.equals("interstitial") && !normType.equals("reward")) {
            return err("type must be 'banner', 'interstitial', or 'reward'. Got: '" + type + "'.");
        }

        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "b");
        if (bean == null) return err("Could not load AdMob library bean.");
        if (bean.adUnits == null) bean.adUnits = new java.util.ArrayList<>();
        bean.adUnits.add(new AdUnitBean(id, name));
        saveBean(iC, "b", bean);
        return ok("Added AdMob ad unit '" + name + "' (id=" + id + ", type=" + normType
                + ") to the project's adUnits list. "
                + "Use library_configure:admob_assign_ad_unit to bind it to a banner/interstitial/reward slot.");
    }

    private ToolResult admobAssignAdUnit(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String name = optString(args, "unit_name");
        String target = optString(args, "target_widget_id");
        if (name == null || name.isEmpty()) return err("unit_name is required.");
        if (target == null) return err("target_widget_id is required (banner / interstitial / reward).");
        String normTarget = target.toLowerCase();
        if (!normTarget.equals("banner") && !normTarget.equals("interstitial") && !normTarget.equals("reward")) {
            return err("target_widget_id must be 'banner', 'interstitial', or 'reward'. Got: '" + target + "'.");
        }

        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "b");
        if (bean == null) return err("Could not load AdMob library bean.");
        if (bean.adUnits == null) bean.adUnits = new java.util.ArrayList<>();

        AdUnitBean matched = null;
        for (AdUnitBean u : bean.adUnits) {
            if (u != null && name.equals(u.name)) { matched = u; break; }
        }
        if (matched == null) {
            return err("No ad unit with name '" + name + "' found in this project's AdMob adUnits list. "
                    + "Call library_configure:admob_add_ad_unit first.");
        }
        String assigned = matched.name + " : " + matched.id;
        switch (normTarget) {
            case "banner":       bean.reserved1 = assigned; break;
            case "interstitial": bean.reserved2 = assigned; break;
            case "reward":       bean.reserved3 = assigned; break;
        }
        saveBean(iC, "b", bean);
        return ok("Assigned ad unit '" + name + "' (id=" + matched.id + ") to the " + normTarget + " slot.");
    }

    private ToolResult admobAddTestDevice(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String deviceId = optString(args, "device_id");
        if (deviceId == null || deviceId.isEmpty()) return err("device_id is required.");
        if (deviceId.length() < 16) {
            return err("device_id '" + deviceId + "' is too short — AdMob test device IDs are typically 32+ hex chars.");
        }

        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "b");
        if (bean == null) return err("Could not load AdMob library bean.");
        if (bean.testDevices == null) bean.testDevices = new java.util.ArrayList<>();
        // Avoid duplicates.
        for (AdTestDeviceBean d : bean.testDevices) {
            if (d != null && deviceId.equals(d.deviceId)) {
                return ok("Test device '" + deviceId + "' was already registered — no change.");
            }
        }
        bean.testDevices.add(new AdTestDeviceBean(deviceId));
        saveBean(iC, "b", bean);
        return ok("Added AdMob test device '" + deviceId + "'. Total: " + bean.testDevices.size() + ".");
    }

    // ==================================================================
    //  GoogleMap
    // ==================================================================

    private ToolResult setGoogleMapApiKey(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String apiKey = optString(args, "value");
        if (apiKey == null || apiKey.isEmpty()) return err("value (api_key) is required.");
        if (!API_KEY_PATTERN.matcher(apiKey).matches()) {
            return err("Google Maps API key '" + apiKey + "' does not look like a valid AIza... key (expected 20+ alphanumeric/underscore/dash chars).");
        }
        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "e");
        if (bean == null) return err("Could not load GoogleMap library bean.");
        bean.data = apiKey;
        // Auto-enable GoogleMap once API key is set (matches UI behaviour).
        bean.useYn = ProjectLibraryBean.LIB_USE_Y;
        saveBean(iC, "e", bean);
        return ok("Set Google Maps API key and enabled the GoogleMap library.");
    }

    // ==================================================================
    //  Material3
    // ==================================================================

    private ToolResult material3SetTheme(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        String theme = optString(args, "theme");
        if (theme == null) return err("theme is required (DayNight / Light / Dark).");
        String norm = theme.substring(0, 1).toUpperCase() + theme.substring(1).toLowerCase();
        if (!norm.equals("DayNight") && !norm.equals("Light") && !norm.equals("Dark")) {
            return err("theme must be 'DayNight', 'Light', or 'Dark'. Got: '" + theme + "'.");
        }
        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "c");
        if (bean == null) return err("Could not load AppCompat library bean.");
        if (!isMaterial3Enabled(bean)) {
            return err("Material3 is not enabled. Call library_enable:enable_material3 first.");
        }
        if (bean.configurations == null) bean.configurations = new java.util.HashMap<>();
        bean.configurations.put("theme", norm);
        saveBean(iC, "c", bean);
        return ok("Set Material3 theme = '" + norm + "'.");
    }

    private ToolResult material3ToggleDynamicColors(SketchwareToolContext ctx, String scId, JsonObject args) throws Exception {
        boolean enabled = optBool(args, "enabled", false);
        Object iC = getIC(scId);
        ProjectLibraryBean bean = getBean(iC, "c");
        if (bean == null) return err("Could not load AppCompat library bean.");
        if (!isMaterial3Enabled(bean)) {
            return err("Material3 is not enabled. Call library_enable:enable_material3 first.");
        }
        if (bean.configurations == null) bean.configurations = new java.util.HashMap<>();
        bean.configurations.put("dynamic_colors", enabled);
        saveBean(iC, "c", bean);
        return ok("Dynamic colors " + (enabled ? "enabled" : "disabled") + " for Material3.");
    }

    // ==================================================================
    //  Import from another project — mirrors LibrarySettingsImporter
    // ==================================================================

    private ToolResult importFromProject(SketchwareToolContext ctx, String scId, JsonObject args,
                                           String getter, String displayName) throws Exception {
        String sourceScId = optString(args, "source_sc_id");
        if (sourceScId == null || sourceScId.isEmpty()) return err("source_sc_id is required.");
        if (sourceScId.equals(scId)) return err("source_sc_id must be different from the current project's sc_id.");

        // Create a fresh iC bound to the source project (does NOT disturb the cached jC.c singleton).
        Object sourceIC;
        try {
            Class<?> iCClass = Class.forName("a.a.a.iC");
            java.lang.reflect.Constructor<?> ctor = iCClass.getConstructor(String.class);
            sourceIC = ctor.newInstance(sourceScId);
            // Load the source project's library config from disk.
            SketchwareApi.invoke(sourceIC, "i");
        } catch (Throwable t) {
            return err("Failed to load source project '" + sourceScId + "': " + t.getMessage());
        }

        ProjectLibraryBean sourceBean = (ProjectLibraryBean) SketchwareApi.invoke(sourceIC, getter);
        if (sourceBean == null || !sourceBean.isEnabled()) {
            return err("Source project '" + sourceScId + "' does not have " + displayName + " enabled.");
        }

        // Copy into the current project's bean.
        Object iC = getIC(scId);
        ProjectLibraryBean destBean = getBean(iC, getter);
        if (destBean == null) {
            // Create a fresh bean of the right type.
            destBean = new ProjectLibraryBean(sourceBean.libType);
        }
        destBean.copy(sourceBean);
        saveBean(iC, getter, destBean);
        return ok("Imported " + displayName + " configuration from project '" + sourceScId + "' "
                + "into the current project (" + scId + ").");
    }

    // ==================================================================
    //  Small utils
    // ==================================================================

    private static boolean isMaterial3Enabled(ProjectLibraryBean compatBean) {
        if (compatBean == null || !compatBean.isEnabled() || compatBean.configurations == null) return false;
        Object v = compatBean.configurations.get("material3");
        return v instanceof Boolean && (Boolean) v;
    }

    /** Extract a JSON string field by regex (tolerant of whitespace). */
    private static String extractJsonField(String json, String fieldName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Mask sensitive values for log output (keeps first 4 + last 4 chars). */
    private static String mask(String s) {
        if (s == null) return "(null)";
        if (s.length() <= 10) return s.substring(0, Math.min(4, s.length())) + "***";
        return s.substring(0, 4) + "..." + s.substring(s.length() - 4);
    }
}
