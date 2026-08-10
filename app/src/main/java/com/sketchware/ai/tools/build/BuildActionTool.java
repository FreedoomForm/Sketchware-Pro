package com.sketchware.ai.tools.build;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.HashSet;
import java.util.Set;

/**
 * build_action — universal tool for build/run/install operations.
 *
 * <p>Replaces 7 stubs: build_cancel, build_install_last_apk, build_run,
 * build_set_setting, build_show_apk_signatures, build_show_last_error,
 * build_show_source_code.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates build setting keys against the supported set.</li>
 *   <li>Returns human-readable status messages including the project
 *       sc_id and elapsed time estimates.</li>
 *   <li>For {@code set_setting}, validates the value type (boolean for
 *       proguard/multidex, integer for SDK versions).</li>
 *   <li>For {@code show_last_error}, returns the full error stack trace
 *       truncated to 48KB (matching {@link ToolResult#MAX_OUTPUT_CHARS}).</li>
 * </ul>
 */
public final class BuildActionTool extends UniversalTool {

    /** Supported build setting keys. */
    private static final Set<String> BUILD_SETTINGS = new HashSet<>();
    static {
        BUILD_SETTINGS.add("min_sdk");         // int
        BUILD_SETTINGS.add("target_sdk");      // int
        BUILD_SETTINGS.add("version_code");    // int
        BUILD_SETTINGS.add("version_name");    // string
        BUILD_SETTINGS.add("proguard");        // boolean
        BUILD_SETTINGS.add("multidex");        // boolean
        BUILD_SETTINGS.add("obfuscate");       // boolean
        BUILD_SETTINGS.add("zip_align");       // boolean
        BUILD_SETTINGS.add("v1_signing");      // boolean
        BUILD_SETTINGS.add("v2_signing");      // boolean
        BUILD_SETTINGS.add("debuggable");      // boolean
    }

    public BuildActionTool() {
        super("build_action",
                "Run a build action: cancel, install_last_apk, run, set_setting, "
                        + "show_apk_signatures, show_last_error, show_source_code.",
                "build", false, false,
                "cancel", "install_last_apk", "run", "set_setting",
                "show_apk_signatures", "show_last_error", "show_source_code");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject settingKey = new JsonObject();
        settingKey.addProperty("type", "string");
        settingKey.addProperty("description", "(set_setting) Build setting key. Supported: min_sdk, target_sdk, version_code, version_name, proguard, multidex, obfuscate, zip_align, v1_signing, v2_signing, debuggable.");
        props.add("setting_key", settingKey);

        JsonObject settingValue = new JsonObject();
        settingValue.addProperty("type", "string");
        settingValue.addProperty("description", "(set_setting) Setting value. Integers for SDK versions, 'true'/'false' for booleans, dotted string for version_name.");
        props.add("setting_value", settingValue);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "cancel": return doCancel(ctx, scId);
            case "install_last_apk": return doInstallLastApk(ctx, scId);
            case "run": return doRun(ctx, scId);
            case "set_setting": return doSetSetting(ctx, scId, args);
            case "show_apk_signatures": return doShowApkSignatures(ctx, scId);
            case "show_last_error": return doShowLastError(ctx, scId);
            case "show_source_code": return doShowSourceCode(ctx, scId);
            default: return err("Unknown action: " + action);
        }
    }

    private ToolResult doCancel(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            SketchwareApi.invoke(bc, "cancel");
            return ok("Build cancelled for project '" + scId + "'. "
                    + "Any in-progress compilation tasks have been terminated.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doInstallLastApk(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            SketchwareApi.invoke(bc, "installLastApk", ctx.getContext());
            return ok("Installing last built APK for project '" + scId + "'. "
                    + "A 'package installer' dialog should appear on the device.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doRun(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            SketchwareApi.invoke(bc, "build", ctx.getContext());
            return ok("Build started for project '" + scId + "'. "
                    + "Approximate time: 30-90 seconds depending on project size and "
                    + "whether dependencies need downloading. "
                    + "Use build_action:show_last_error to retrieve any build errors.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doSetSetting(SketchwareToolContext ctx, String scId, JsonObject args) {
        String key = optString(args, "setting_key");
        String value = optString(args, "setting_value");
        if (key == null || value == null) return err("setting_key and setting_value are required.");
        if (!BUILD_SETTINGS.contains(key)) {
            return err("Unknown setting_key '" + key + "'. Supported: " + BUILD_SETTINGS);
        }
        // Validate value type.
        if (key.endsWith("_sdk") || key.equals("version_code")) {
            try { Integer.parseInt(value); }
            catch (NumberFormatException e) {
                return err("Setting '" + key + "' requires an integer value. Got: '" + value + "'.");
            }
        } else if (key.equals("proguard") || key.equals("multidex") || key.equals("obfuscate")
                || key.equals("zip_align") || key.equals("v1_signing") || key.equals("v2_signing")
                || key.equals("debuggable")) {
            if (!value.equals("true") && !value.equals("false")) {
                return err("Setting '" + key + "' requires 'true' or 'false'. Got: '" + value + "'.");
            }
        }
        try {
            Object bs = SketchwareApi.invokeStatic("mod.jbk.build.BuildSettings", "getInstance", scId);
            SketchwareApi.invoke(bs, "setSetting", key, value);
            return ok("Set build setting '" + key + "' = '" + value + "' for project '" + scId + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doShowApkSignatures(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            Object sigs = SketchwareApi.invoke(bc, "getApkSignatures");
            String sigStr = sigs == null ? "(none)" : sigs.toString();
            return ok("APK signatures for project '" + scId + "':\n" + sigStr);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doShowLastError(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            Object err = SketchwareApi.invoke(bc, "getLastError");
            if (err == null) {
                return ok("No build errors recorded for project '" + scId + "'. "
                        + "Either the last build succeeded or no build has been run yet.");
            }
            return ok("Last build error for project '" + scId + "':\n" + err);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult doShowSourceCode(SketchwareToolContext ctx, String scId) {
        try {
            Object bc = SketchwareApi.invokeStatic("mod.jbk.build.BuildManager", "getInstance", scId);
            Object code = SketchwareApi.invoke(bc, "getGeneratedSource");
            if (code == null) {
                return ok("No generated source available for project '" + scId + "'. "
                        + "Run a build first to generate the source code.");
            }
            return ok("Generated Java source for project '" + scId + "':\n" + code);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
