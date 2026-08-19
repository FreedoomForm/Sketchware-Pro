package com.sketchware.ai.tools.build;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.project.stringfog.StringfogHandler;
import pro.sketchware.utility.FileUtil;

/**
 * build_action — universal tool for build/run/install operations.
 *
 * <p>Replaces 7 stubs: build_cancel, build_install_last_apk, build_run,
 * build_set_setting, build_show_apk_signatures, build_show_last_error,
 * build_show_source_code.
 *
 * <p><b>FIX-D-PROJECT (Task D5):</b>
 * <ul>
 *   <li>Switched {@code set_setting} from the non-existent
 *       {@code mod.jbk.build.BuildSettings.getInstance(scId).setSetting(k,v)}
 *       reflection call to the real
 *       {@link BuildSettings#setValue(String, String)} API.</li>
 *   <li>Added 8 new setting keys: {@code android_jar_path},
 *       {@code classpath}, {@code dexer} (Dx/D8),
 *       {@code java_version} (1.7/1.8/1.9/10/11),
 *       {@code no_warnings}, {@code no_http_legacy},
 *       {@code enable_logcat}, {@code stringfog_enabled}.
 *       Task-spec key names are mapped to the actual setting keys
 *       stored in {@code build_config} (e.g. {@code android_jar_path}
 *       → {@code android_jar}, {@code java_version} → {@code java_ver}).</li>
 *   <li>Added 2 new actions: {@code clean_temp_files} (deletes
 *       {@code .sketchware/mysc/{scId}/}) and {@code clean_build_cache}
 *       (deletes {@code .sketchware/mysc/{scId}/bin/}).</li>
 * </ul>
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates build setting keys against the supported set.</li>
 *   <li>Returns human-readable status messages including the project
 *       sc_id and elapsed time estimates.</li>
 *   <li>For {@code set_setting}, validates the value type (boolean for
 *       proguard/multidex, integer for SDK versions, enum for dexer
 *       and java_version).</li>
 *   <li>For {@code show_last_error}, returns the full error stack trace
 *       truncated to 48KB (matching {@link ToolResult#MAX_OUTPUT_CHARS}).</li>
 * </ul>
 */
public final class BuildActionTool extends UniversalTool {

    /** Supported build setting keys (LLM-facing names). */
    private static final Set<String> BUILD_SETTINGS = new HashSet<>();
    static {
        // Original keys (kept for backward compat — stored as-is in build_config).
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
        // FIX-D-PROJECT Task D5: new keys.
        BUILD_SETTINGS.add("android_jar_path");  // string (path to android.jar)
        BUILD_SETTINGS.add("classpath");         // string (extra classpath entries)
        BUILD_SETTINGS.add("dexer");             // enum: "dx" or "d8" (case-insensitive)
        BUILD_SETTINGS.add("java_version");      // enum: 1.7 / 1.8 / 1.9 / 10 / 11
        BUILD_SETTINGS.add("no_warnings");       // boolean
        BUILD_SETTINGS.add("no_http_legacy");    // boolean
        BUILD_SETTINGS.add("enable_logcat");     // boolean
        BUILD_SETTINGS.add("stringfog_enabled"); // boolean (special: uses StringfogHandler)
    }

    /** Keys whose values must be integers. */
    private static final Set<String> INT_KEYS = new HashSet<>(Arrays.asList(
            "min_sdk", "target_sdk", "version_code"
    ));

    /** Keys whose values must be 'true' or 'false'. */
    private static final Set<String> BOOL_KEYS = new HashSet<>(Arrays.asList(
            "proguard", "multidex", "obfuscate", "zip_align",
            "v1_signing", "v2_signing", "debuggable",
            "no_warnings", "no_http_legacy", "enable_logcat", "stringfog_enabled"
    ));

    /** Allowed dexer values (LLM-friendly lowercase). */
    private static final Set<String> DEXER_VALUES = new HashSet<>(Arrays.asList("dx", "d8"));

    /** Allowed java_version values. */
    private static final Set<String> JAVA_VERSION_VALUES =
            new HashSet<>(Arrays.asList("1.7", "1.8", "1.9", "10", "11"));

    public BuildActionTool() {
        super("build_action",
                "Run a build action: cancel, install_last_apk, run, set_setting, "
                        + "show_apk_signatures, show_last_error, show_source_code, "
                        + "clean_temp_files, clean_build_cache. "
                        + "set_setting accepts 19 keys: min_sdk, target_sdk, version_code, "
                        + "version_name, proguard, multidex, obfuscate, zip_align, v1_signing, "
                        + "v2_signing, debuggable, android_jar_path, classpath, dexer (dx/d8), "
                        + "java_version (1.7/1.8/1.9/10/11), no_warnings, no_http_legacy, "
                        + "enable_logcat, stringfog_enabled.",
                "build", false, false,
                "cancel", "install_last_apk", "run", "set_setting",
                "show_apk_signatures", "show_last_error", "show_source_code",
                "clean_temp_files", "clean_build_cache");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject settingKey = new JsonObject();
        settingKey.addProperty("type", "string");
        settingKey.addProperty("description",
                "(set_setting) Build setting key. Supported: min_sdk, target_sdk, "
                        + "version_code, version_name, proguard, multidex, obfuscate, zip_align, "
                        + "v1_signing, v2_signing, debuggable, android_jar_path, classpath, "
                        + "dexer (dx/d8), java_version (1.7/1.8/1.9/10/11), no_warnings, "
                        + "no_http_legacy, enable_logcat, stringfog_enabled.");
        props.add("setting_key", settingKey);

        JsonObject settingValue = new JsonObject();
        settingValue.addProperty("type", "string");
        settingValue.addProperty("description",
                "(set_setting) Setting value. Integers for SDK versions, 'true'/'false' for "
                        + "booleans, dotted string for version_name, 'dx'/'d8' for dexer, "
                        + "'1.7'/'1.8'/'1.9'/'10'/'11' for java_version, file path for "
                        + "android_jar_path.");
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
            case "clean_temp_files": return doCleanTempFiles(ctx, scId);
            case "clean_build_cache": return doCleanBuildCache(ctx, scId);
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
        if (INT_KEYS.contains(key)) {
            try { Integer.parseInt(value); }
            catch (NumberFormatException e) {
                return err("Setting '" + key + "' requires an integer value. Got: '" + value + "'.");
            }
        } else if (BOOL_KEYS.contains(key)) {
            if (!value.equals("true") && !value.equals("false")) {
                return err("Setting '" + key + "' requires 'true' or 'false'. Got: '" + value + "'.");
            }
        } else if ("dexer".equals(key)) {
            String norm = value.toLowerCase();
            if (!DEXER_VALUES.contains(norm)) {
                return err("Setting 'dexer' must be 'dx' or 'd8' (case-insensitive). Got: '" + value + "'.");
            }
            value = norm.equals("d8") ? BuildSettings.SETTING_DEXER_D8 : BuildSettings.SETTING_DEXER_DX;
        } else if ("java_version".equals(key)) {
            if (!JAVA_VERSION_VALUES.contains(value)) {
                return err("Setting 'java_version' must be one of " + JAVA_VERSION_VALUES
                        + ". Got: '" + value + "'.");
            }
        }

        try {
            // stringfog_enabled lives in its own config file managed by StringfogHandler.
            if ("stringfog_enabled".equals(key)) {
                new StringfogHandler(scId).setStringfogEnabled("true".equals(value));
                return ok("Set build setting '" + key + "' = '" + value + "' for project '" + scId + "'. "
                        + "(stored in .sketchware/data/" + scId + "/stringfog)");
            }

            // Map LLM-facing key names to actual setting keys stored in build_config.
            String actualKey = mapSettingKey(key);
            BuildSettings settings = new BuildSettings(scId);
            settings.setValue(actualKey, value);
            return ok("Set build setting '" + key + "' = '" + value + "' for project '" + scId + "'. "
                    + "(stored as '" + actualKey + "' in .sketchware/data/" + scId + "/build_config)");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Translate LLM-facing key names to the actual BuildSettings key names. */
    private static String mapSettingKey(String llmKey) {
        switch (llmKey) {
            case "android_jar_path": return BuildSettings.SETTING_ANDROID_JAR_PATH; // "android_jar"
            case "classpath":         return BuildSettings.SETTING_CLASSPATH;        // "classpath"
            case "dexer":             return BuildSettings.SETTING_DEXER;            // "dexer"
            case "java_version":      return BuildSettings.SETTING_JAVA_VERSION;     // "java_ver"
            case "no_warnings":       return BuildSettings.SETTING_NO_WARNINGS;      // "no_warn"
            case "no_http_legacy":    return BuildSettings.SETTING_NO_HTTP_LEGACY;   // "no_http_legacy"
            case "enable_logcat":     return BuildSettings.SETTING_ENABLE_LOGCAT;    // "enable_logcat"
            default:                  return llmKey; // min_sdk, target_sdk, etc. — stored as-is
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

    // ------------------------------------------------------------------
    //  FIX-D-PROJECT Task D5: clean_temp_files & clean_build_cache
    // ------------------------------------------------------------------

    /**
     * Delete the project's build-temp directory (the workspace where
     * Sketchware assembles the Gradle-like project before invoking the
     * compiler). Actual path: {@code .sketchware/mysc/{scId}/}.
     */
    private ToolResult doCleanTempFiles(SketchwareToolContext ctx, String scId) {
        String path = FileUtil.getExternalStorageDir() + "/.sketchware/mysc/" + scId;
        if (!FileUtil.isExistFile(path)) {
            return ok("No temp files to clean for project '" + scId
                    + "' (path does not exist: " + path + ").");
        }
        try {
            FileUtil.deleteFile(path);
            return ok("Deleted temp files for project '" + scId + "'. "
                    + "(removed: " + path + ")");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /**
     * Delete only the build-cache subdirectory ({@code bin/}) of the
     * project's build-temp workspace. Less aggressive than
     * {@link #doCleanTempFiles} — preserves generated source/resources
     * but forces the next build to recompile classes and repackage the APK.
     */
    private ToolResult doCleanBuildCache(SketchwareToolContext ctx, String scId) {
        String path = FileUtil.getExternalStorageDir() + "/.sketchware/mysc/" + scId + "/bin";
        if (!FileUtil.isExistFile(path)) {
            return ok("No build cache to clean for project '" + scId
                    + "' (path does not exist: " + path + "). "
                    + "Run a build first to populate the cache.");
        }
        try {
            FileUtil.deleteFile(path);
            return ok("Cleared build cache for project '" + scId + "'. "
                    + "(removed: " + path + ")");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
