package com.sketchware.ai.tools.project;

import com.besome.sketch.projects.ThemeManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

import a.a.a.lC;
import a.a.a.wq;
import a.a.a.yB;
import mod.hey.studios.util.ProjectFile;
import pro.sketchware.utility.FileUtil;

/**
 * theme_manage - universal tool for managing the per-project color theme.
 *
 * <p>Wires the AI agent to Sketchware-Pro's theme subsystem, which is
 * normally driven by {@link com.besome.sketch.projects.MyProjectSettingActivity}
 * together with {@link ThemeManager} (the preset catalog).
 *
 * <p><b>Storage model</b> (verified by reading MyProjectSettingActivity):
 * <ul>
 *   <li>Project metadata file: {@code wq.c(sc_id)/project} (a HashMap
 *       serialized via {@code vB.a}/deserialized via {@code vB.a(...)}).
 *       The theme colors are stored as Integer values keyed by
 *       {@code color_accent}, {@code color_primary}, {@code color_primary_dark},
 *       {@code color_control_highlight}, {@code color_control_normal}
 *       (see {@link ProjectFile} constants).</li>
 *   <li>Read metadata: {@link lC#b(String)} returns the HashMap.</li>
 *   <li>Write metadata: {@link lC#b(String, HashMap)} merges the passed
 *       map's color_* keys back into the on-disk project file. BEWARE: the
 *       merge writes ALL of {my_sc_pkg_name, my_ws_name, my_app_name,
 *       sc_ver_code, sc_ver_name, sketchware_ver, custom_icon, isIconAdaptive,
 *       color_accent, color_primary, color_primary_dark, color_control_highlight,
 *       color_control_normal} from the passed map - any missing key is
 *       written as null. To avoid clobbering unrelated fields, always read
 *       the existing metadata first, mutate only the color fields, then
 *       write it back.</li>
 *   <li>Colors XML: {@code wq.b(sc_id) + "/files/resource/values/colors.xml"}.
 *       The 5 theme color entries are written as
 *       {@code <color name="colorAccent">#RRGGBB</color>} (and analogous for
 *       colorPrimary, colorPrimaryDark, colorControlHighlight, colorControlNormal).
 *       Regex-replace mirroring
 *       {@code MyProjectSettingActivity.updateProjectResourcesContents()}
 *       is used so that other user-defined colors in the same file are
 *       preserved.</li>
 * </ul>
 *
 * <p>Sketchware does NOT persist a kC.y() side-effect for colors.xml -
 * colors.xml is rewritten directly via FileUtil. This tool follows the
 * same pattern (it does NOT call jC.d(scId).y() after writing colors.xml).
 *
 * <p><b>Built-in presets</b> (12 total, from {@link ThemeManager#getThemePresets()}):
 * Material Purple, Material Blue, Material Green, Material Red, Material
 * Orange, Material Teal, Material Indigo, Material Pink, Dark Purple,
 * Dark Blue, Light Purple, Light Blue. There is no per-user saved-preset
 * store in Sketchware-Pro; {@code list_presets} returns only built-ins
 * (source="builtin").
 *
 * <p>Actions (5):
 * <ul>
 *   <li><b>apply_preset</b> - apply a built-in theme preset by name
 *       (params: {@code preset_name}).</li>
 *   <li><b>generate_random</b> - generate a random palette via
 *       {@link ThemeManager#generateRandomTheme()}. Optionally seed with
 *       {@code base_color} (hex like #FF5722). Updates metadata + colors.xml.</li>
 *   <li><b>reset</b> - reset to {@link ThemeManager#getDefault()} (Sketchware
 *       defaults from {@link ProjectFile#getDefaultColor(String)}).</li>
 *   <li><b>get_current</b> - read the 5 current theme colors from project
 *       metadata. Read-only.</li>
 *   <li><b>list_presets</b> - enumerate all built-in presets with their
 *       5-color previews. Read-only.</li>
 * </ul>
 *
 * <p><b>Quirk</b>: the per-action read-only flag is not supported by
 * {@link UniversalTool} (see FIX-C-RESOURCES design notes). Although
 * {@code get_current} and {@code list_presets} are read-only, the tool
 * itself is marked {@code readOnly=false, autoApproved=false} because the
 * other 3 actions mutate project state.
 */
public final class ThemeManageTool extends UniversalTool {

    /** Hex color string like #RRGGBB or #AARRGGBB. */
    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$");

    /** Theme color keys persisted in the project metadata HashMap. */
    private static final String[] META_KEYS = {
            ProjectFile.COLOR_ACCENT,
            ProjectFile.COLOR_PRIMARY,
            ProjectFile.COLOR_PRIMARY_DARK,
            ProjectFile.COLOR_CONTROL_HIGHLIGHT,
            ProjectFile.COLOR_CONTROL_NORMAL
    };

    /** Matching color names used inside colors.xml. */
    private static final String[] XML_NAMES = {
            "colorAccent",
            "colorPrimary",
            "colorPrimaryDark",
            "colorControlHighlight",
            "colorControlNormal"
    };

    public ThemeManageTool() {
        super("theme_manage",
                "Manage the per-project color theme: apply_preset, "
                        + "generate_random, reset, get_current, list_presets. "
                        + "Theme colors are persisted to the project metadata "
                        + "AND to colors.xml.",
                "project", false, false,
                "apply_preset", "generate_random", "reset",
                "get_current", "list_presets");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "preset_name",
                "(apply_preset) Name of a built-in preset, e.g. 'Material Blue'. "
                        + "Use list_presets to see available names.");
        addStringProp(props, "base_color",
                "(generate_random) Optional seed color in #RRGGBB or #AARRGGBB hex. "
                        + "If omitted, a fully random hue is used.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "apply_preset":   return doApplyPreset(ctx, scId, args);
            case "generate_random": return doGenerateRandom(ctx, scId, args);
            case "reset":          return doReset(ctx, scId);
            case "get_current":    return doGetCurrent(scId);
            case "list_presets":   return doListPresets();
            default:               return err("Unknown action: " + action);
        }
    }

    // ==================================================================
    //  apply_preset
    // ==================================================================

    /**
     * Apply a built-in theme preset by name (case-insensitive).
     * Required arg: preset_name. Updates both the project metadata and
     * colors.xml so the change is visible in the editor and survives a
     * project reload.
     */
    private ToolResult doApplyPreset(SketchwareToolContext ctx, String scId, JsonObject args) {
        String presetName = optString(args, "preset_name");
        if (presetName == null || presetName.isEmpty()) {
            return err("preset_name is required.");
        }
        ThemeManager.ThemePreset preset = findPreset(presetName);
        if (preset == null) {
            return err("No preset named '" + presetName + "'. Available: " + presetList());
        }
        return persistTheme(ctx, scId, preset, "Applied preset '" + preset.name + "'.");
    }

    // ==================================================================
    //  generate_random
    // ==================================================================

    /**
     * Generate a random theme palette. Optional arg: base_color
     * (#RRGGBB or #AARRGGBB). When provided, the random generator is
     * re-seeded so the primary color matches the supplied base. Otherwise
     * ThemeManager.generateRandomTheme() is used as-is.
     */
    private ToolResult doGenerateRandom(SketchwareToolContext ctx, String scId, JsonObject args) {
        String baseColor = optString(args, "base_color");
        ThemeManager.ThemePreset theme;
        if (baseColor == null || baseColor.isEmpty()) {
            theme = ThemeManager.generateRandomTheme();
        } else {
            if (!HEX_COLOR.matcher(baseColor).matches()) {
                return err("base_color must match #RRGGBB or #AARRGGBB. Got: " + baseColor);
            }
            theme = generateRandomFromBase(baseColor);
        }
        return persistTheme(ctx, scId, theme, "Generated random theme from base " + baseColor + ".");
    }

    // ==================================================================
    //  reset
    // ==================================================================

    /**
     * Reset the project theme to Sketchware defaults via
     * ThemeManager.getDefault(). The defaults come from
     * ProjectFile.getDefaultColor() (which itself branches on Android S+
     * dynamic colors).
     */
    private ToolResult doReset(SketchwareToolContext ctx, String scId) {
        ThemeManager.ThemePreset def = ThemeManager.getDefault();
        return persistTheme(ctx, scId, def, "Reset project theme to Sketchware defaults.");
    }

    // ==================================================================
    //  get_current
    // ==================================================================

    /**
     * Read the 5 current theme colors from the project metadata.
     * Returns a JSON-like text dump {color_name: #RRGGBB}.
     */
    private ToolResult doGetCurrent(String scId) {
        HashMap<String, Object> meta = readMeta(scId);
        if (meta == null) return err("Project metadata not found for sc_id=" + scId);
        StringBuilder sb = new StringBuilder();
        sb.append("Current theme colors for project '").append(scId).append("':\n");
        for (int i = 0; i < META_KEYS.length; i++) {
            int colorInt = yB.a(meta, META_KEYS[i], ProjectFile.getDefaultColor(META_KEYS[i]));
            sb.append("  ").append(XML_NAMES[i]).append(" = ")
              .append(String.format("#%08X", colorInt))
              .append("  (metadata key: ").append(META_KEYS[i]).append(")\n");
        }
        // Also include the colors.xml view (if the file exists, show what
        // is actually serialized on disk - useful for diagnosing mismatches
        // between metadata and colors.xml).
        String colorsXmlPath = getColorsXmlPath(scId);
        if (FileUtil.isExistFile(colorsXmlPath)) {
            String xml = FileUtil.readFile(colorsXmlPath);
            sb.append("\nColors.xml snapshot (filtered to theme entries):\n");
            for (String xmlName : XML_NAMES) {
                String match = extractColorEntry(xml, xmlName);
                if (match != null) {
                    sb.append("  ").append(match).append("\n");
                }
            }
        } else {
            sb.append("\n(colors.xml does not exist yet at ").append(colorsXmlPath).append(")\n");
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  list_presets
    // ==================================================================

    /**
     * Enumerate all built-in theme presets with their 5-color previews.
     * Returns a JSON array of {name, source:"builtin", preview_colors:[...]}.
     */
    private ToolResult doListPresets() {
        ThemeManager.ThemePreset[] presets = ThemeManager.getThemePresets();
        JsonArray arr = new JsonArray();
        StringBuilder sb = new StringBuilder();
        sb.append("Built-in theme presets (").append(presets.length).append("):\n");
        for (ThemeManager.ThemePreset p : presets) {
            JsonObject o = new JsonObject();
            o.addProperty("name", p.name);
            o.addProperty("source", "builtin");
            JsonArray colors = new JsonArray();
            colors.add(String.format("#%08X", p.colorPrimary));
            colors.add(String.format("#%08X", p.colorPrimaryDark));
            colors.add(String.format("#%08X", p.colorAccent));
            colors.add(String.format("#%08X", p.colorControlHighlight));
            colors.add(String.format("#%08X", p.colorControlNormal));
            o.add("preview_colors", colors);
            arr.add(o);

            sb.append("  - ").append(p.name).append("\n");
            sb.append("      primary           = ").append(String.format("#%08X", p.colorPrimary)).append("\n");
            sb.append("      primary_dark      = ").append(String.format("#%08X", p.colorPrimaryDark)).append("\n");
            sb.append("      accent            = ").append(String.format("#%08X", p.colorAccent)).append("\n");
            sb.append("      control_highlight = ").append(String.format("#%08X", p.colorControlHighlight)).append("\n");
            sb.append("      control_normal    = ").append(String.format("#%08X", p.colorControlNormal)).append("\n");
        }
        sb.append("\nJSON: ").append(arr.toString());
        return ok(sb.toString());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Find a preset by case-insensitive name match; null if not found. */
    private static ThemeManager.ThemePreset findPreset(String name) {
        for (ThemeManager.ThemePreset p : ThemeManager.getThemePresets()) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    /** Build a comma-separated list of preset names for error messages. */
    private static String presetList() {
        ThemeManager.ThemePreset[] presets = ThemeManager.getThemePresets();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < presets.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(presets[i].name);
        }
        return sb.toString();
    }

    /**
     * Generate a random theme derived from a user-supplied base color.
     * Mirrors {@link ThemeManager#generateRandomTheme()} but seeds the
     * primary hue from the supplied color.
     */
    private static ThemeManager.ThemePreset generateRandomFromBase(String hex) {
        int primaryColor = android.graphics.Color.parseColor(hex);
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(primaryColor, hsv);
        // Boost saturation/value to "Material-ish" range if the input was muted.
        if (hsv[1] < 0.5f) hsv[1] = 0.6f + (float) Math.random() * 0.3f;
        if (hsv[2] < 0.4f) hsv[2] = 0.5f + (float) Math.random() * 0.4f;
        primaryColor = android.graphics.Color.HSVToColor(hsv);

        // primary_dark = darker variant.
        float[] hsvDark = hsv.clone();
        hsvDark[2] *= 0.7f;
        int primaryDark = android.graphics.Color.HSVToColor(hsvDark);

        // accent = complementary hue (180-degree rotation) with high sat.
        float[] hsvAccent = hsv.clone();
        hsvAccent[0] = (hsvAccent[0] + 180f) % 360f;
        hsvAccent[1] = 0.7f + (float) Math.random() * 0.2f;
        hsvAccent[2] = 0.8f + (float) Math.random() * 0.2f;
        int accent = android.graphics.Color.HSVToColor(hsvAccent);

        // control_highlight = lightened primary.
        float[] hsvLight = hsv.clone();
        hsvLight[2] = Math.min(1.0f, hsvLight[2] + 0.9f);
        hsvLight[1] = Math.max(0.0f, hsvLight[1] - 0.45f);
        int controlHighlight = android.graphics.Color.HSVToColor(hsvLight);

        int controlNormal = android.graphics.Color.GRAY;
        return new ThemeManager.ThemePreset(
                "Random Theme", accent, primaryColor, primaryDark,
                controlHighlight, controlNormal);
    }

    /**
     * Persist a theme preset to (a) the project metadata file and (b) colors.xml.
     * Returns a success ToolResult prefixed with the supplied message.
     */
    private ToolResult persistTheme(SketchwareToolContext ctx, String scId,
                                    ThemeManager.ThemePreset theme, String message) {
        // 1. Read existing metadata, mutate only color fields, write back.
        HashMap<String, Object> meta = readMeta(scId);
        if (meta == null) {
            return err("Project metadata not found for sc_id=" + scId
                    + ". Open the project in MyProjectSettingActivity once first.");
        }
        int[] colors = {
                theme.colorAccent,
                theme.colorPrimary,
                theme.colorPrimaryDark,
                theme.colorControlHighlight,
                theme.colorControlNormal
        };
        for (int i = 0; i < META_KEYS.length; i++) {
            meta.put(META_KEYS[i], colors[i]);
        }
        try {
            lC.b(scId, meta);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        // 2. Update colors.xml via regex replace (mirror MyProjectSettingActivity.updateProjectResourcesContents).
        String colorsXmlPath = getColorsXmlPath(scId);
        if (FileUtil.isExistFile(colorsXmlPath)) {
            try {
                String xml = FileUtil.readFile(colorsXmlPath);
                for (int i = 0; i < XML_NAMES.length; i++) {
                    String newColor = String.format("#%06X", (0xFFFFFF & colors[i]));
                    xml = xml.replaceAll(
                            "(<color\\s+name=\"" + XML_NAMES[i] + "\">)(.*?)(</color>)",
                            "$1" + newColor + "$3");
                }
                FileUtil.writeFile(colorsXmlPath, xml);
            } catch (Throwable t) {
                return ToolResult.error(t);
            }
        } else {
            // colors.xml does not exist yet - synthesize a minimal one
            // containing the 5 theme color entries.
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
            for (int i = 0; i < XML_NAMES.length; i++) {
                String newColor = String.format("#%06X", (0xFFFFFF & colors[i]));
                sb.append("    <color name=\"").append(XML_NAMES[i])
                  .append("\">").append(newColor).append("</color>\n");
            }
            sb.append("</resources>\n");
            try {
                FileUtil.makeDir(new File(colorsXmlPath).getParent());
                FileUtil.writeFile(colorsXmlPath, sb.toString());
            } catch (Throwable t) {
                return ToolResult.error(t);
            }
        }

        ctx.refreshViewEditor();
        StringBuilder result = new StringBuilder(message);
        result.append("\n  primary           = ").append(String.format("#%08X", theme.colorPrimary));
        result.append("\n  primary_dark      = ").append(String.format("#%08X", theme.colorPrimaryDark));
        result.append("\n  accent            = ").append(String.format("#%08X", theme.colorAccent));
        result.append("\n  control_highlight = ").append(String.format("#%08X", theme.colorControlHighlight));
        result.append("\n  control_normal    = ").append(String.format("#%08X", theme.colorControlNormal));
        result.append("\n  colors.xml        = ").append(colorsXmlPath);
        return ok(result.toString());
    }

    /** Read the project metadata HashMap via lC.b(scId). Returns null on error. */
    private static HashMap<String, Object> readMeta(String scId) {
        try {
            return lC.b(scId);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Absolute path to the project's colors.xml file. */
    private static String getColorsXmlPath(String scId) {
        return wq.b(scId) + File.separator + "files" + File.separator
                + "resource" + File.separator + "values" + File.separator + "colors.xml";
    }

    /** Extract a single {@code <color name="...">...</color>} entry from the XML, or null. */
    private static String extractColorEntry(String xml, String name) {
        if (xml == null || xml.isEmpty()) return null;
        int idx = xml.indexOf("name=\"" + name + "\"");
        if (idx < 0) return null;
        // Find enclosing <color> ... </color>
        int start = xml.lastIndexOf("<color", idx);
        int end = xml.indexOf("</color>", idx);
        if (start < 0 || end < 0) return null;
        return xml.substring(start, end + "</color>".length()).trim();
    }
}
