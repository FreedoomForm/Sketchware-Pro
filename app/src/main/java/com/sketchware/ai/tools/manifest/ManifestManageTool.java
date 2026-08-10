package com.sketchware.ai.tools.manifest;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * manifest_manage — universal tool for managing AndroidManifest.xml entries.
 *
 * <p>Replaces 6 stubs: manifest_add_activity, manifest_add_permission,
 * manifest_delete_activity, manifest_set_activity_attribute,
 * manifest_set_application_attribute, manifest_set_components.
 *
 * <p>This implementation validates manifest entries against Android's
 * official attribute namespace ({@code android:*}) and rejects clearly
 * invalid input (empty activity names, malformed permission strings).
 *
 * <p>Operations:
 * <ul>
 *   <li><b>add_activity</b>: registers a new {@code <activity>} entry.
 *       Validates that the activity name is a valid Java class name
 *       (capitalized, no dots issue).</li>
 *   <li><b>add_permission</b>: adds a {@code <uses-permission>} entry.
 *       Validates against the Android permission name format
 *       ({@code android.permission.X} or a custom name).</li>
 *   <li><b>delete_activity</b>: removes the activity entry.</li>
 *   <li><b>set_activity_attribute</b>: sets an attribute on an existing
 *       {@code <activity>} (e.g. {@code android:exported},
 *       {@code android:launchMode}).</li>
 *   <li><b>set_application_attribute</b>: sets an attribute on the
 *       {@code <application>} tag (e.g. {@code android:icon},
 *       {@code android:theme}, {@code android:allowBackup}).</li>
 *   <li><b>set_components</b>: replaces the entire
 *       {@code <components>} section (Android 12+ package visibility).</li>
 * </ul>
 */
public final class ManifestManageTool extends UniversalTool {

    /** Valid android: attribute name pattern (lowercase + underscore). */
    private static final String ANDROID_ATTR_PATTERN = "^[a-z][a-z0-9_]*$";

    /** Common Android permissions (for validation hints, not strict enforcement). */
    private static final Set<String> COMMON_PERMISSIONS = new HashSet<>(Arrays.asList(
            "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE", "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.VIBRATE", "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED", "android.permission.FOREGROUND_SERVICE",
            "android.permission.POST_NOTIFICATIONS", "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO", "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN", "android.permission.NFC",
            "android.permission.READ_SMS", "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS", "android.permission.CALL_PHONE",
            "android.permission.READ_PHONE_STATE", "android.permission.USE_BIOMETRIC"
    ));

    /** Common application attributes that we accept. */
    private static final Set<String> APP_ATTRS = new HashSet<>(Arrays.asList(
            "allowBackup", "icon", "label", "roundIcon", "theme", "name",
            "debuggable", "hardwareAccelerated", "largeHeap", "supportsRtl",
            "usesCleartextTraffic", "networkSecurityConfig", "dataExtractionRules",
            "fullBackupContent", "enableOnBackInvokedCallback", "appComponentFactory",
            "requestLegacyExternalStorage"
    ));

    /** Common activity attributes that we accept. */
    private static final Set<String> ACTIVITY_ATTRS = new HashSet<>(Arrays.asList(
            "name", "label", "theme", "exported", "enabled", "excludeFromRecents",
            "launchMode", "noHistory", "permission", "process", "screenOrientation",
            "taskAffinity", "windowSoftInputMode", "configChanges", "parentActivityName",
            "intentFilter", "clearTaskOnLaunch", "stateNotNeeded", "finishOnTaskLaunch"
    ));

    public ManifestManageTool() {
        super("manifest_manage",
                "Manage AndroidManifest.xml entries: add_activity, add_permission, "
                        + "delete_activity, set_activity_attribute, set_application_attribute, "
                        + "set_components. Validates against Android attribute conventions.",
                "manifest", false, false,
                "add_activity", "add_permission", "delete_activity",
                "set_activity_attribute", "set_application_attribute", "set_components");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject activityName = new JsonObject();
        activityName.addProperty("type", "string");
        activityName.addProperty("description", "(add/delete_activity, set_activity_attribute) Activity class name. Must be a valid Java class name (e.g. 'MainActivity' or 'com.example.MyActivity').");
        props.add("activity_name", activityName);

        JsonObject permissionName = new JsonObject();
        permissionName.addProperty("type", "string");
        permissionName.addProperty("description", "(add_permission) Permission name (e.g. 'android.permission.INTERNET').");
        props.add("permission_name", permissionName);

        JsonObject attrName = new JsonObject();
        attrName.addProperty("type", "string");
        attrName.addProperty("description", "(set_activity_attribute/set_application_attribute) Attribute name WITHOUT the 'android:' prefix (e.g. 'exported', 'theme').");
        props.add("attribute_name", attrName);

        JsonObject attrValue = new JsonObject();
        attrValue.addProperty("type", "string");
        attrValue.addProperty("description", "(set_activity_attribute/set_application_attribute) Attribute value. For booleans, use 'true' or 'false'. For resources, use '@string/foo' or '@drawable/foo'.");
        props.add("attribute_value", attrValue);

        JsonObject componentsXml = new JsonObject();
        componentsXml.addProperty("type", "string");
        componentsXml.addProperty("description", "(set_components) Raw XML for the <components> section (Android 12+ package visibility). E.g. '<package android:name=\"com.example\"/><intent>...</intent>'.");
        props.add("components_xml", componentsXml);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "add_activity": return doAddActivity(ctx, scId, args);
            case "add_permission": return doAddPermission(ctx, scId, args);
            case "delete_activity": return doDeleteActivity(ctx, scId, args);
            case "set_activity_attribute": return doSetActivityAttribute(ctx, scId, args);
            case "set_application_attribute": return doSetAppAttribute(ctx, scId, args);
            case "set_components": return doSetComponents(ctx, scId, args);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  add_activity
    // ------------------------------------------------------------------
    private ToolResult doAddActivity(SketchwareToolContext ctx, String scId, JsonObject args) {
        String activityName = optString(args, "activity_name");
        if (activityName == null) return err("activity_name is required.");
        if (!isValidActivityName(activityName)) {
            return err("Invalid activity name '" + activityName + "'. Must be a valid Java class name "
                    + "(start with uppercase, alphanumeric + dots).");
        }
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "h", activityName);
            return ok("Added <activity android:name=\"" + activityName + "\" /> to AndroidManifest.xml. "
                    + "Note: set android:exported explicitly via set_activity_attribute for Android 12+.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  add_permission
    // ------------------------------------------------------------------
    private ToolResult doAddPermission(SketchwareToolContext ctx, String scId, JsonObject args) {
        String perm = optString(args, "permission_name");
        if (perm == null) return err("permission_name is required.");
        if (!isValidPermissionName(perm)) {
            return err("Invalid permission name '" + perm + "'. Must match "
                    + "^[a-z][a-z0-9_.]*$ (e.g. 'android.permission.INTERNET').");
        }
        boolean isCommon = COMMON_PERMISSIONS.contains(perm);
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "b", perm);
            String hint = isCommon ? "" : " (custom permission — make sure it's spelled correctly; "
                    + "see https://developer.android.com/reference/android/Manifest.permission)";
            return ok("Added <uses-permission android:name=\"" + perm + "\" /> to AndroidManifest.xml." + hint);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete_activity
    // ------------------------------------------------------------------
    private ToolResult doDeleteActivity(SketchwareToolContext ctx, String scId, JsonObject args) {
        String activityName = optString(args, "activity_name");
        if (activityName == null) return err("activity_name is required.");
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "i", activityName);
            return ok("Removed <activity android:name=\"" + activityName + "\" /> from AndroidManifest.xml.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_activity_attribute
    // ------------------------------------------------------------------
    private ToolResult doSetActivityAttribute(SketchwareToolContext ctx, String scId, JsonObject args) {
        String activity = optString(args, "activity_name");
        String attr = optString(args, "attribute_name");
        String value = optString(args, "attribute_value");
        if (activity == null || attr == null || value == null) {
            return err("activity_name, attribute_name, and attribute_value are required.");
        }
        if (!isValidAttrName(attr)) {
            return err("Invalid attribute name '" + attr + "'. Must match " + ANDROID_ATTR_PATTERN + ".");
        }
        if (!ACTIVITY_ATTRS.contains(attr)) {
            // Soft warning — don't reject, but inform the LLM.
            return setAttrWithWarning(ctx, scId, "activity", activity, attr, value,
                    "Attribute '" + attr + "' is not in the common-attributes list. "
                            + "Common activity attributes: " + ACTIVITY_ATTRS);
        }
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "j", activity, attr, value);
            return ok("Set android:" + attr + "=\"" + value + "\" on <activity android:name=\"" + activity + "\" />.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_application_attribute
    // ------------------------------------------------------------------
    private ToolResult doSetAppAttribute(SketchwareToolContext ctx, String scId, JsonObject args) {
        String attr = optString(args, "attribute_name");
        String value = optString(args, "attribute_value");
        if (attr == null || value == null) {
            return err("attribute_name and attribute_value are required.");
        }
        if (!isValidAttrName(attr)) {
            return err("Invalid attribute name '" + attr + "'. Must match " + ANDROID_ATTR_PATTERN + ".");
        }
        if (!APP_ATTRS.contains(attr)) {
            return setAttrWithWarning(ctx, scId, "application", null, attr, value,
                    "Attribute '" + attr + "' is not in the common-attributes list. "
                            + "Common application attributes: " + APP_ATTRS);
        }
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "k", attr, value);
            return ok("Set android:" + attr + "=\"" + value + "\" on <application>.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_components
    // ------------------------------------------------------------------
    private ToolResult doSetComponents(SketchwareToolContext ctx, String scId, JsonObject args) {
        String xml = optString(args, "components_xml");
        if (xml == null) return err("components_xml is required.");
        // Basic XML well-formedness check.
        if (!xml.trim().startsWith("<")) {
            return err("components_xml must start with '<' (valid XML). Got: " + xml.substring(0, Math.min(40, xml.length())) + "...");
        }
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(pm, "l", xml);
            return ok("Replaced <components> section in AndroidManifest.xml. "
                    + "This affects Android 12+ package visibility (queries).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private ToolResult setAttrWithWarning(SketchwareToolContext ctx, String scId, String target,
                                            String activity, String attr, String value, String warning) {
        try {
            Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            if (activity != null) {
                SketchwareApi.invoke(pm, "j", activity, attr, value);
            } else {
                SketchwareApi.invoke(pm, "k", attr, value);
            }
            return ok("Set android:" + attr + "=\"" + value + "\" on <" + target + ">. "
                    + "WARNING: " + warning);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private static boolean isValidActivityName(String name) {
        if (name == null || name.isEmpty()) return false;
        // Allow simple (MainActivity) or fully-qualified (com.example.MainActivity).
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isJavaIdentifierPart(c) || c == '.')) return false;
        }
        // Last segment must start uppercase (Java class convention).
        int lastDot = name.lastIndexOf('.');
        char firstChar = lastDot == -1 ? name.charAt(0) : name.charAt(lastDot + 1);
        return Character.isUpperCase(firstChar);
    }

    private static boolean isValidPermissionName(String name) {
        if (name == null || name.isEmpty()) return false;
        // android.permission.X or a custom name (lowercase + dots + underscores).
        if (!Character.isLowerCase(name.charAt(0))) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '.' || c == '_')) return false;
        }
        return name.contains(".");
    }

    private static boolean isValidAttrName(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.matches(ANDROID_ATTR_PATTERN);
    }
}
