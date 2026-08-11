package com.sketchware.ai.tools.build;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.util.ArrayList;

import mod.hey.studios.project.proguard.ProguardHandler;
import pro.sketchware.utility.FileUtil;

/**
 * proguard_manage — universal tool for code shrinking (ProGuard / R8)
 * configuration.
 *
 * <p><b>FIX-D-PROJECT (Task D6):</b> new tool. Closes coverage-report
 * gap §2.4 — previously only the master enable/disable toggle was
 * exposed (via {@code build_action.set_setting} key=proguard, which
 * itself was broken because the key doesn't exist in
 * {@code mod.hey.studios.build.BuildSettings}). This tool talks
 * directly to {@link ProguardHandler} and exposes all 6 operations
 * available in {@code ManageProguardActivity}.
 *
 * <p>Storage (per {@link ProguardHandler}):
 * <ul>
 *   <li>Enabled / debug / R8 flags → {@code .sketchware/data/{scId}/proguard}
 *       (JSON HashMap with keys "enabled", "debug", "r8").</li>
 *   <li>FM (full-mode) lib list → {@code .sketchware/data/{scId}/proguard_fm}
 *       (JSON ArrayList of library names).</li>
 *   <li>Custom rules → {@code .sketchware/data/{scId}/proguard-rules.pro}
 *       (raw ProGuard rules text).</li>
 * </ul>
 */
public final class ProguardManageTool extends UniversalTool {

    public ProguardManageTool() {
        super("proguard_manage",
                "Manage ProGuard / R8 code shrinking: toggle_enabled, "
                        + "toggle_r8, toggle_debug, edit_rules, select_fm_libs, "
                        + "get_rules. Note: enabling R8 only takes effect if "
                        + "shrinking is also enabled (toggle_enabled=true).",
                "build", false, false,
                "toggle_enabled",
                "toggle_r8",
                "toggle_debug",
                "edit_rules",
                "select_fm_libs",
                "get_rules");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        JsonObject p;

        p = new JsonObject();
        p.addProperty("type", "boolean");
        p.addProperty("description", "(toggle_enabled / toggle_r8 / toggle_debug) true to enable, false to disable.");
        props.add("enabled", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(edit_rules) Full content of the proguard-rules.pro file. Replaces existing content.");
        props.add("rules_content", p);

        p = new JsonObject();
        p.addProperty("type", "array");
        p.addProperty("description", "(select_fm_libs) Local library names to apply ProGuard full-mode to. Replaces the existing list.");
        JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        p.add("items", items);
        props.add("lib_names", p);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        try {
            ProguardHandler pg = new ProguardHandler(scId);
            switch (action) {
                case "toggle_enabled": return doToggleEnabled(pg, scId, args);
                case "toggle_r8":      return doToggleR8(pg, scId, args);
                case "toggle_debug":   return doToggleDebug(pg, scId, args);
                case "edit_rules":     return doEditRules(pg, scId, args);
                case "select_fm_libs": return doSelectFmLibs(pg, scId, args);
                case "get_rules":      return doGetRules(pg, scId);
                default: return err("Unknown action: " + action);
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  toggle_enabled
    // ------------------------------------------------------------------
    private ToolResult doToggleEnabled(ProguardHandler pg, String scId, JsonObject args) {
        boolean enabled = optBool(args, "enabled", false);
        pg.setProguardEnabled(enabled);
        return ok("ProGuard shrinking " + (enabled ? "enabled" : "disabled")
                + " for project '" + scId + "'. "
                + "(stored in .sketchware/data/" + scId + "/proguard as {\"enabled\":\"" + enabled + "\"})");
    }

    // ------------------------------------------------------------------
    //  toggle_r8
    // ------------------------------------------------------------------
    private ToolResult doToggleR8(ProguardHandler pg, String scId, JsonObject args) {
        boolean enabled = optBool(args, "enabled", false);
        if (enabled && !pg.isShrinkingEnabled()) {
            return err("Cannot enable R8 while ProGuard shrinking is disabled. "
                    + "Call proguard_manage:toggle_enabled {enabled:true} first.");
        }
        pg.setR8Enabled(enabled);
        return ok("R8 " + (enabled ? "enabled" : "disabled")
                + " for project '" + scId + "'. "
                + "(when shrinking is enabled, R8 will be used instead of ProGuard)");
    }

    // ------------------------------------------------------------------
    //  toggle_debug
    // ------------------------------------------------------------------
    private ToolResult doToggleDebug(ProguardHandler pg, String scId, JsonObject args) {
        boolean enabled = optBool(args, "enabled", false);
        pg.setDebugEnabled(enabled);
        return ok("ProGuard debug files " + (enabled ? "enabled" : "disabled")
                + " for project '" + scId + "'. "
                + "(when enabled, mapping.txt / seeds.txt / usage.txt are emitted to .sketchware/mysc/" + scId + "/bin/)");
    }

    // ------------------------------------------------------------------
    //  edit_rules
    // ------------------------------------------------------------------
    private ToolResult doEditRules(ProguardHandler pg, String scId, JsonObject args) {
        String rules = optString(args, "rules_content");
        if (rules == null) return err("rules_content is required (the full proguard-rules.pro content).");
        // getCustomProguardRules() returns the PATH, not the content (per ProguardHandler source).
        String path = pg.getCustomProguardRules();
        if (path == null || path.isEmpty()) {
            return err("Could not resolve proguard-rules.pro path for project '" + scId + "'.");
        }
        try {
            FileUtil.writeFile(path, rules);
            return ok("Wrote " + rules.length() + " chars to " + path + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  select_fm_libs
    // ------------------------------------------------------------------
    private ToolResult doSelectFmLibs(ProguardHandler pg, String scId, JsonObject args) {
        ArrayList<String> libs = new ArrayList<>();
        if (args.has("lib_names") && !args.get("lib_names").isJsonNull()
                && args.get("lib_names").isJsonArray()) {
            for (var e : args.get("lib_names").getAsJsonArray()) {
                if (!e.isJsonNull()) {
                    String s = e.getAsString();
                    if (s != null && !s.isEmpty()) libs.add(s);
                }
            }
        }
        pg.setProguardFMLibs(libs);
        return ok("Set ProGuard full-mode library list to " + libs.size()
                + " lib(s): " + libs + ". "
                + "(stored in .sketchware/data/" + scId + "/proguard_fm)");
    }

    // ------------------------------------------------------------------
    //  get_rules — read-only
    // ------------------------------------------------------------------
    private ToolResult doGetRules(ProguardHandler pg, String scId) {
        String path = pg.getCustomProguardRules();
        if (path == null || path.isEmpty() || !FileUtil.isExistFile(path)) {
            return ok("No proguard-rules.pro file exists yet for project '" + scId + "'. "
                    + "It will be auto-created with default rules when ProguardManageTool is first used.");
        }
        try {
            String content = FileUtil.readFile(path);
            return ok("proguard-rules.pro for project '" + scId + "' (path: " + path + "):\n\n" + content);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
