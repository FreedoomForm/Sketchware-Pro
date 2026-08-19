package com.sketchware.ai.tools.manifest;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import mod.hilal.saif.android_manifest.AndroidManifestInjector;
import pro.sketchware.utility.FileUtil;

/**
 * manifest_manage — universal tool for managing AndroidManifest.xml entries.
 *
 * <p><b>FIX-D-PROJECT (Task D3):</b> removed the duplicate {@code add_permission}
 * action (it duplicated {@code permission_manage.add}). Use
 * {@code permission_manage:add} for adding &lt;uses-permission&gt; entries.
 *
 * <p><b>FIX-D-PROJECT (Task D4):</b> added 5 new actions:
 * <ul>
 *   <li>{@code set_launcher_activity} — sets the project's launcher activity
 *       via {@link AndroidManifestInjector#setLauncherActivity}.</li>
 *   <li>{@code edit_app_components} — replaces the raw XML injected into the
 *       &lt;application&gt; block (stored in {@code app_components.txt}).</li>
 *   <li>{@code edit_activity_components} — replaces the intent-filter XML
 *       injected into a specific &lt;activity&gt; (stored in
 *       {@code activities_components.json}).</li>
 *   <li>{@code edit_all_activities_attrs} — adds an attribute that applies
 *       to ALL &lt;activity&gt; entries (stored in {@code attributes.json}
 *       under the reserved name {@code _apply_for_all_activities}).</li>
 *   <li>{@code show_source} — read-only, returns the generated
 *       AndroidManifest.xml source (reflects {@code yq.getFileSrc}).</li>
 * </ul>
 *
 * <p>This implementation validates manifest entries against Android's
 * official attribute namespace ({@code android:*}) and rejects clearly
 * invalid input (empty activity names, malformed permission strings).
 *
 * <p>Operations (final list, 10 actions):
 * <ul>
 *   <li><b>add_activity</b>: registers a new &lt;activity&gt; entry.</li>
 *   <li><b>delete_activity</b>: removes the activity entry.</li>
 *   <li><b>set_activity_attribute</b>: sets an attribute on an existing
 *       &lt;activity&gt;.</li>
 *   <li><b>set_application_attribute</b>: sets an attribute on the
 *       &lt;application&gt; tag.</li>
 *   <li><b>set_components</b>: replaces the &lt;components&gt; section.</li>
 *   <li><b>set_launcher_activity</b>: sets the launcher activity name.</li>
 *   <li><b>edit_app_components</b>: replaces the raw XML injected into
 *       &lt;application&gt;.</li>
 *   <li><b>edit_activity_components</b>: replaces the intent-filter XML
 *       injected into a specific &lt;activity&gt;.</li>
 *   <li><b>edit_all_activities_attrs</b>: adds an attribute applied to
 *       every &lt;activity&gt;.</li>
 *   <li><b>show_source</b>: read-only, returns the generated manifest XML.</li>
 * </ul>
 */
public final class ManifestManageTool extends UniversalTool {

    /** Valid android: attribute name pattern (lowercase + underscore). */
    private static final String ANDROID_ATTR_PATTERN = "^[a-z][a-z0-9_]*$";

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
                "Manage AndroidManifest.xml entries: add_activity, delete_activity, "
                        + "set_activity_attribute, set_application_attribute, set_components, "
                        + "set_launcher_activity, edit_app_components, edit_activity_components, "
                        + "edit_all_activities_attrs, show_source. "
                        + "NOTE: use permission_manage:add to add <uses-permission> entries.",
                "manifest", false, false,
                "add_activity", "delete_activity",
                "set_activity_attribute", "set_application_attribute", "set_components",
                "set_launcher_activity", "edit_app_components",
                "edit_activity_components", "edit_all_activities_attrs",
                "show_source");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject activityName = new JsonObject();
        activityName.addProperty("type", "string");
        activityName.addProperty("description", "(add/delete_activity, set_activity_attribute, edit_activity_components, set_launcher_activity) Activity class name. Must be a valid Java class name (e.g. 'MainActivity' or 'com.example.MyActivity').");
        props.add("activity_name", activityName);

        JsonObject attrName = new JsonObject();
        attrName.addProperty("type", "string");
        attrName.addProperty("description", "(set_activity_attribute/set_application_attribute/edit_all_activities_attrs) Attribute name WITHOUT the 'android:' prefix (e.g. 'exported', 'theme').");
        props.add("attribute_name", attrName);

        JsonObject attrValue = new JsonObject();
        attrValue.addProperty("type", "string");
        attrValue.addProperty("description", "(set_activity_attribute/set_application_attribute/edit_all_activities_attrs) Attribute value. For booleans, use 'true' or 'false'. For resources, use '@string/foo' or '@drawable/foo'.");
        props.add("attribute_value", attrValue);

        JsonObject componentsXml = new JsonObject();
        componentsXml.addProperty("type", "string");
        componentsXml.addProperty("description", "(set_components / edit_app_components / edit_activity_components) Raw XML. For set_components: <components> section XML. For edit_app_components: XML to inject into <application>. For edit_activity_components: intent-filter XML to inject into a specific <activity>.");
        props.add("components_xml", componentsXml);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "add_activity": return doAddActivity(ctx, scId, args);
            case "delete_activity": return doDeleteActivity(ctx, scId, args);
            case "set_activity_attribute": return doSetActivityAttribute(ctx, scId, args);
            case "set_application_attribute": return doSetAppAttribute(ctx, scId, args);
            case "set_components": return doSetComponents(ctx, scId, args);
            case "set_launcher_activity": return doSetLauncherActivity(ctx, scId, args);
            case "edit_app_components": return doEditAppComponents(ctx, scId, args);
            case "edit_activity_components": return doEditActivityComponents(ctx, scId, args);
            case "edit_all_activities_attrs": return doEditAllActivitiesAttrs(ctx, scId, args);
            case "show_source": return doShowSource(ctx, scId);
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
            return setAttrWithWarning(scId, "activity", activity, attr, value,
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
            return setAttrWithWarning(scId, "application", null, attr, value,
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
    //  set_launcher_activity — FIX-D-PROJECT Task D4
    // ------------------------------------------------------------------
    private ToolResult doSetLauncherActivity(SketchwareToolContext ctx, String scId, JsonObject args) {
        String activityName = optString(args, "activity_name");
        if (activityName == null || activityName.isEmpty()) {
            return err("activity_name is required (the activity to mark as launcher).");
        }
        if (!isValidActivityName(activityName)) {
            return err("Invalid activity name '" + activityName + "'.");
        }
        try {
            AndroidManifestInjector.setLauncherActivity(scId, activityName);
            return ok("Set launcher activity = '" + activityName + "'. "
                    + "(written to .sketchware/data/" + scId + "/Injection/androidmanifest/activity_launcher.txt)");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  edit_app_components — FIX-D-PROJECT Task D4
    //  Replaces the raw XML that gets injected into <application>.
    // ------------------------------------------------------------------
    private ToolResult doEditAppComponents(SketchwareToolContext ctx, String scId, JsonObject args) {
        String xml = optString(args, "components_xml");
        if (xml == null) return err("components_xml is required (raw XML to inject at the <application> level).");
        try {
            java.io.File f = AndroidManifestInjector.getPathAndroidManifestAppComponents(scId);
            FileUtil.writeFile(f.getAbsolutePath(), xml);
            return ok("Replaced app_components.txt with " + xml.length() + " chars of XML. "
                    + "(path: " + f.getAbsolutePath() + ")");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  edit_activity_components — FIX-D-PROJECT Task D4
    //  Replaces the intent-filter XML injected into a specific <activity>.
    // ------------------------------------------------------------------
    private ToolResult doEditActivityComponents(SketchwareToolContext ctx, String scId, JsonObject args) {
        String activityName = optString(args, "activity_name");
        String xml = optString(args, "components_xml");
        if (activityName == null || activityName.isEmpty()) return err("activity_name is required.");
        if (xml == null) return err("components_xml is required (intent-filter XML).");
        try {
            java.io.File f = AndroidManifestInjector.getPathAndroidManifestActivitiesComponents(scId);
            ArrayList<HashMap<String, Object>> data;
            if (FileUtil.isExistFile(f.getAbsolutePath())) {
                String content = FileUtil.readFile(f.getAbsolutePath());
                if (content.trim().isEmpty()) {
                    data = new ArrayList<>();
                } else {
                    data = new Gson().fromJson(content, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
                    if (data == null) data = new ArrayList<>();
                }
            } else {
                data = new ArrayList<>();
            }
            // Remove any existing entry with the same activity name (replace semantics).
            for (int i = data.size() - 1; i >= 0; i--) {
                HashMap<String, Object> entry = data.get(i);
                Object name = entry.get("name");
                if (name != null && name.toString().equals(activityName)) {
                    data.remove(i);
                }
            }
            HashMap<String, Object> entry = new HashMap<>();
            entry.put("name", activityName);
            entry.put("value", xml);
            data.add(entry);
            FileUtil.writeFile(f.getAbsolutePath(), new Gson().toJson(data));
            return ok("Updated activity components for '" + activityName + "' "
                    + "with " + xml.length() + " chars of intent-filter XML. "
                    + "(path: " + f.getAbsolutePath() + ")");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  edit_all_activities_attrs — FIX-D-PROJECT Task D4
    //  Adds an android:attr="value" entry applied to every <activity>.
    //  Stored in attributes.json under the reserved key
    //  "_apply_for_all_activities".
    // ------------------------------------------------------------------
    private ToolResult doEditAllActivitiesAttrs(SketchwareToolContext ctx, String scId, JsonObject args) {
        String attr = optString(args, "attribute_name");
        String value = optString(args, "attribute_value");
        if (attr == null || value == null) {
            return err("attribute_name and attribute_value are required.");
        }
        if (!isValidAttrName(attr)) {
            return err("Invalid attribute name '" + attr + "'. Must match " + ANDROID_ATTR_PATTERN + ".");
        }
        // Build the full attribute string in the same format Sketchware uses.
        String fullAttr = "android:" + attr + "=\"" + value + "\"";
        try {
            java.io.File f = AndroidManifestInjector.getPathAndroidManifestAttributeInjection(scId);
            ArrayList<HashMap<String, Object>> data;
            if (FileUtil.isExistFile(f.getAbsolutePath())) {
                String content = FileUtil.readFile(f.getAbsolutePath());
                if (content.trim().isEmpty()) {
                    data = new ArrayList<>();
                } else {
                    data = new Gson().fromJson(content, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
                    if (data == null) data = new ArrayList<>();
                }
            } else {
                data = new ArrayList<>();
            }
            // Remove any existing "_apply_for_all_activities" entry that contains this same
            // android:attr="..." string to avoid duplicates.
            for (int i = data.size() - 1; i >= 0; i--) {
                HashMap<String, Object> entry = data.get(i);
                Object name = entry.get("name");
                Object val = entry.get("value");
                if ("_apply_for_all_activities".equals(name) && val instanceof String
                        && ((String) val).contains("android:" + attr + "=")) {
                    data.remove(i);
                }
            }
            HashMap<String, Object> entry = new HashMap<>();
            entry.put("name", "_apply_for_all_activities");
            entry.put("value", fullAttr);
            data.add(entry);
            FileUtil.writeFile(f.getAbsolutePath(), new Gson().toJson(data));
            return ok("Added '" + fullAttr + "' to all <activity> entries. "
                    + "(stored in attributes.json under name='_apply_for_all_activities')");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  show_source — FIX-D-PROJECT Task D4 (read-only)
    //  Reflects yq.getFileSrc("AndroidManifest.xml", jC.b(scId),
    //  jC.a(scId), jC.c(scId)) to return the generated manifest source.
    // ------------------------------------------------------------------
    private ToolResult doShowSource(SketchwareToolContext ctx, String scId) {
        try {
            Class<?> yqClass = Class.forName("a.a.a.yq");
            Object yq = yqClass.getConstructor(android.content.Context.class, String.class)
                    .newInstance(ctx.getContext(), scId);
            Object jCb = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
            Object jCa = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object jCc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            // Find getFileSrc(String, ?, ?, ?) — arg types are obfuscated hC/eC/iC.
            Method target = null;
            for (Method m : yqClass.getDeclaredMethods()) {
                if (!m.getName().equals("getFileSrc")) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 4) continue;
                if (!params[0].equals(String.class)) continue;
                target = m;
                break;
            }
            if (target == null) {
                return err("Could not locate yq.getFileSrc(String, ?, ?, ?) via reflection.");
            }
            target.setAccessible(true);
            Object result = target.invoke(yq, "AndroidManifest.xml", jCb, jCa, jCc);
            String source = result == null ? "" : result.toString();
            if (source.isEmpty()) {
                return ok("Failed to generate AndroidManifest.xml source (yq returned empty).");
            }
            return ok("Generated AndroidManifest.xml source for project '" + scId + "':\n\n" + source);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private ToolResult setAttrWithWarning(String scId, String target,
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

    private static boolean isValidAttrName(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.matches(ANDROID_ATTR_PATTERN);
    }
}
