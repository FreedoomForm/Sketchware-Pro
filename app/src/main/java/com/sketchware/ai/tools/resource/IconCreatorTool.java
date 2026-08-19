package com.sketchware.ai.tools.resource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import a.a.a.wq;
import pro.sketchware.utility.FileUtil;

/**
 * icon_creator - universal tool for creating, editing, and deleting
 * launcher icons (adaptive and legacy) in a Sketchware project.
 *
 * <p>Wires the AI agent to the same on-disk layout used by Sketchware's
 * {@link pro.sketchware.activities.iconcreator.IconCreatorActivity} and
 * the project-save flow in
 * {@link com.besome.sketch.projects.MyProjectSettingActivity}.
 *
 * <p><b>Storage model</b> (verified from IconCreatorActivity +
 * MyProjectSettingActivity + DesignActivity):
 * <ul>
 *   <li><b>Per-project icons root</b>: {@code wq.e() + "/" + scId + "/mipmaps/"}.
 *       This is the final destination that Sketchware's project builder
 *       copies into the build's {@code res/} directory at APK build time
 *       (see DesignActivity line 1097: {@code q.aa(wq.e() + "/" + sc_id + "/mipmaps")}).</li>
 *   <li><b>Legacy icon PNGs</b>: {@code mipmaps/mipmap-{density}/{name}.png}
 *       for each of mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi (48/72/96/144/192 px).</li>
 *   <li><b>Adaptive icon XML</b>: {@code mipmaps/mipmap-anydpi-v26/{name}.xml}
 *       containing an {@code <adaptive-icon>} element that references
 *       {@code @mipmap/{name}_foreground} and either
 *       {@code @color/{name}_background} (when a background_color is
 *       supplied) or {@code @mipmap/{name}_background} (when a
 *       background_image is supplied).</li>
 *   <li><b>Adaptive foreground PNGs</b>: {@code mipmaps/mipmap-{density}/{name}_foreground.png}
 *       for each of the 5 standard densities.</li>
 *   <li><b>Adaptive background PNGs</b> (when background_image is supplied):
 *       {@code mipmaps/mipmap-{density}/{name}_background.png} for each
 *       density.</li>
 *   <li><b>Background color entry</b> (when background_color is supplied):
 *       a {@code <color name="{name}_background">#RRGGBB</color>} entry is
 *       written to {@code wq.b(scId)/files/resource/values/colors.xml}.
 *       If the file already contains an entry with the same name, it is
 *       regex-replaced; otherwise the entry is appended before
 *       {@code </resources>}.</li>
 * </ul>
 *
 * <p><b>Quirk</b>: Sketchware's own IconCreatorActivity does NOT generate
 * the {@code mipmap-anydpi-v26/{name}.xml} file at edit time. Instead, it
 * dumps the foreground/background/monochrome PNGs into the mipmaps
 * directory, and the build process later synthesizes an adaptive-icon XML
 * at build time (DesignActivity line 1099, via
 * {@code q.createLauncherIconXml(...)}). This tool takes the more
 * standards-compliant approach of writing the XML file directly so that
 * the icon is correctly registered as adaptive without relying on the
 * build-time synthesis step.
 *
 * <p><b>Image input</b>: every action that accepts an image supports BOTH
 * base64-encoded data (validated: {@code ^[A-Za-z0-9+/=]+$}, length % 4 == 0,
 * &gt;= 8 chars) AND an absolute file path. The same pattern as
 * {@link ImageManageTool} is reused for decoding &amp; temp-file writing.
 *
 * <p><b>Path safety</b>: the {@code name} field is validated against
 * {@code ^[a-z][a-z0-9_]*$} (Android resource name convention, matches
 * Sketchware's own validator) to prevent path traversal and to ensure the
 * icon can be referenced by {@code @mipmap/{name}}.
 *
 * <p>Actions (6):
 * <ul>
 *   <li><b>create_adaptive</b> - create a new adaptive icon (Android 8.0+).
 *       Args: name (required), foreground_image (base64 or file_path,
 *       required), background_color (hex like #FFFFFF) OR background_image
 *       (base64 or file_path). Defaults to white background.</li>
 *   <li><b>create_legacy</b> - create a legacy (pre-Android 8.0) launcher
 *       icon. Args: name (required), image (base64 or file_path, required).
 *       Writes the PNG to all 5 mipmap densities.</li>
 *   <li><b>set_foreground</b> - replace the foreground of an existing
 *       adaptive icon. Args: name, foreground_image.</li>
 *   <li><b>set_background</b> - replace the background of an existing
 *       adaptive icon. Args: name, background_color OR background_image.</li>
 *   <li><b>delete</b> - delete an icon (adaptive or legacy). Removes all
 *       mipmap entries + the colors.xml entry if applicable.</li>
 *   <li><b>list</b> - list all launcher icons in the project. Read-only.</li>
 * </ul>
 *
 * <p><b>Per-action read-only note</b>: {@code list} is read-only, but
 * because {@link UniversalTool} only supports tool-level read-only flags
 * (not per-action), the tool is marked {@code readOnly=false,
 * autoApproved=false}. The permission gate will require user approval
 * for every action including {@code list}; this matches the convention
 * used by ImageManageTool and the other resource-management tools.
 */
public final class IconCreatorTool extends UniversalTool {

    /** Android resource name convention (lowercase, digits, underscore). */
    private static final Pattern VALID_RES_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** Standard base64 alphabet (with padding). */
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    /** Hex color string like #RRGGBB or #AARRGGBB. */
    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$");

    /** Mipmap density buckets and their pixel sizes. */
    private static final String[] DENSITIES = {
            "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi",
            "mipmap-xxhdpi", "mipmap-xxxhdpi"
    };
    private static final int[] DENSITY_PX = {48, 72, 96, 144, 192};

    public IconCreatorTool() {
        super("icon_creator",
                "Create and manage launcher icons: create_adaptive "
                        + "(mipmap-anydpi-v26 XML + per-density PNGs), "
                        + "create_legacy (per-density PNGs), set_foreground, "
                        + "set_background, delete, list. Icon names must match "
                        + "^[a-z][a-z0-9_]*$. Images can be provided as base64 "
                        + "or absolute file paths.",
                "resource", false, false,
                "create_adaptive", "create_legacy",
                "set_foreground", "set_background",
                "delete", "list");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "name",
                "Icon resource name. Must match ^[a-z][a-z0-9_]*$. "
                        + "Used as @mipmap/{name} in the AndroidManifest.");
        addStringProp(props, "foreground_image",
                "(create_adaptive, set_foreground) Foreground image: base64 "
                        + "string OR absolute file path.");
        addStringProp(props, "background_color",
                "(create_adaptive, set_background) Background color in #RRGGBB "
                        + "or #AARRGGBB hex. Mutually exclusive with background_image.");
        addStringProp(props, "background_image",
                "(create_adaptive, set_background) Background image: base64 string "
                        + "OR absolute file path. Mutually exclusive with background_color.");
        addStringProp(props, "image",
                "(create_legacy) Icon image: base64 string OR absolute file path.");
        addArrayProp(props, "names", "(delete) Array of icon names to delete.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }
    private static void addArrayProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "array");
        o.addProperty("description", d);
        JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        o.add("items", items);
        p.add(k, o);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "create_adaptive": return doCreateAdaptive(ctx, scId, args);
            case "create_legacy":   return doCreateLegacy(ctx, scId, args);
            case "set_foreground":  return doSetForeground(ctx, scId, args);
            case "set_background":  return doSetBackground(ctx, scId, args);
            case "delete":          return doDelete(ctx, scId, args);
            case "list":            return doList(scId);
            default:                return err("Unknown action: " + action);
        }
    }

    // ==================================================================
    //  create_adaptive
    // ==================================================================

    /**
     * Create a new adaptive icon (Android 8.0+). Writes the adaptive-icon
     * XML to mipmap-anydpi-v26/{name}.xml, the foreground PNG to all 5
     * mipmap density buckets, and either a background color entry to
     * colors.xml OR a background PNG to all 5 density buckets.
     * Required: name, foreground_image. Optional: background_color OR
     * background_image (default: white #FFFFFF).
     */
    private ToolResult doCreateAdaptive(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_RES_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String mipmapsRoot = getMipmapsRoot(scId);

        // Reject duplicates (adaptive XML or legacy PNG already present).
        if (iconExists(mipmapsRoot, name)) {
            return err("Icon '" + name + "' already exists. Use 'set_foreground' "
                    + "or 'set_background' to modify, or 'delete' first.");
        }

        // Resolve foreground image bytes.
        byte[] fgBytes = resolveImageBytes(args, "foreground_image");
        if (fgBytes == null) {
            return err("foreground_image is required (base64 OR file_path).");
        }

        // Resolve background: either a color OR an image. Default white.
        String bgColor = optString(args, "background_color");
        String bgImagePath = optString(args, "background_image");
        boolean bgIsImage = false;
        byte[] bgBytes = null;
        if (bgColor != null && !bgColor.isEmpty()) {
            if (!HEX_COLOR.matcher(bgColor).matches()) {
                return err("background_color must match #RRGGBB or #AARRGGBB. Got: " + bgColor);
            }
        } else if (bgImagePath != null && !bgImagePath.isEmpty()) {
            bgBytes = resolveImageBytes(args, "background_image");
            if (bgBytes == null) {
                return err("background_image was provided but could not be decoded.");
            }
            bgIsImage = true;
        } else {
            bgColor = "#FFFFFF"; // default white
        }

        // Decode foreground once.
        Bitmap fgBmp = decodeBitmap(fgBytes);
        if (fgBmp == null) return err("Failed to decode foreground_image bitmap.");

        // Write foreground PNGs at each density.
        List<String> writtenFg = new ArrayList<>();
        for (int i = 0; i < DENSITIES.length; i++) {
            String path = mipmapsRoot + File.separator + DENSITIES[i]
                    + File.separator + name + "_foreground.png";
            if (writeScaledPng(fgBmp, path, DENSITY_PX[i], DENSITY_PX[i])) {
                writtenFg.add(DENSITIES[i] + ":" + DENSITY_PX[i] + "px");
            }
        }
        fgBmp.recycle();

        // Write background.
        String bgRefInXml;
        if (bgIsImage) {
            Bitmap bgBmp = decodeBitmap(bgBytes);
            if (bgBmp == null) return err("Failed to decode background_image bitmap.");
            for (int i = 0; i < DENSITIES.length; i++) {
                String path = mipmapsRoot + File.separator + DENSITIES[i]
                        + File.separator + name + "_background.png";
                writeScaledPng(bgBmp, path, DENSITY_PX[i], DENSITY_PX[i]);
            }
            bgBmp.recycle();
            bgRefInXml = "@mipmap/" + name + "_background";
        } else {
            // Write/replace color entry in colors.xml.
            String colorsXmlPath = getColorsXmlPath(scId);
            try {
                upsertColorEntry(colorsXmlPath, name + "_background", bgColor);
            } catch (Throwable t) {
                return ToolResult.error(t);
            }
            bgRefInXml = "@color/" + name + "_background";
        }

        // Write adaptive-icon XML.
        String xmlPath = mipmapsRoot + File.separator + "mipmap-anydpi-v26"
                + File.separator + name + ".xml";
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <background android:drawable=\"" + bgRefInXml + "\"/>\n"
                + "    <foreground android:drawable=\"@mipmap/" + name + "_foreground\"/>\n"
                + "</adaptive-icon>\n";
        try {
            FileUtil.makeDir(new File(xmlPath).getParent());
            FileUtil.writeFile(xmlPath, xml);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        ctx.refreshViewEditor();
        return ok("Created adaptive icon '" + name + "' for project '" + scId + "'.\n"
                + "  XML: " + xmlPath + "\n"
                + "  Background reference: " + bgRefInXml + "\n"
                + "  Foreground PNGs written: " + writtenFg);
    }

    // ==================================================================
    //  create_legacy
    // ==================================================================

    /**
     * Create a legacy (pre-Android 8.0) launcher icon. Writes the source
     * image as a PNG to all 5 mipmap density buckets at the standard
     * launcher icon sizes (48/72/96/144/192 px).
     * Required: name, image.
     */
    private ToolResult doCreateLegacy(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_RES_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String mipmapsRoot = getMipmapsRoot(scId);
        if (iconExists(mipmapsRoot, name)) {
            return err("Icon '" + name + "' already exists. Use 'delete' first.");
        }
        byte[] bytes = resolveImageBytes(args, "image");
        if (bytes == null) return err("image is required (base64 OR file_path).");
        Bitmap bmp = decodeBitmap(bytes);
        if (bmp == null) return err("Failed to decode image bitmap.");

        List<String> written = new ArrayList<>();
        for (int i = 0; i < DENSITIES.length; i++) {
            String path = mipmapsRoot + File.separator + DENSITIES[i]
                    + File.separator + name + ".png";
            if (writeScaledPng(bmp, path, DENSITY_PX[i], DENSITY_PX[i])) {
                written.add(DENSITIES[i] + ":" + DENSITY_PX[i] + "px");
            }
        }
        bmp.recycle();
        ctx.refreshViewEditor();
        return ok("Created legacy icon '" + name + "' for project '" + scId + "'.\n"
                + "  PNGs written: " + written);
    }

    // ==================================================================
    //  set_foreground
    // ==================================================================

    /**
     * Replace the foreground of an existing adaptive icon. The adaptive
     * XML must already exist (created via create_adaptive); this action
     * only rewrites the foreground PNGs at all 5 density buckets.
     * Required: name, foreground_image.
     */
    private ToolResult doSetForeground(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_RES_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String mipmapsRoot = getMipmapsRoot(scId);
        String xmlPath = mipmapsRoot + File.separator + "mipmap-anydpi-v26"
                + File.separator + name + ".xml";
        if (!FileUtil.isExistFile(xmlPath)) {
            return err("Adaptive icon '" + name + "' does not exist (no XML at "
                    + "mipmap-anydpi-v26/" + name + ".xml). Use 'create_adaptive' first.");
        }
        byte[] bytes = resolveImageBytes(args, "foreground_image");
        if (bytes == null) return err("foreground_image is required (base64 OR file_path).");
        Bitmap bmp = decodeBitmap(bytes);
        if (bmp == null) return err("Failed to decode foreground_image bitmap.");

        List<String> written = new ArrayList<>();
        for (int i = 0; i < DENSITIES.length; i++) {
            String path = mipmapsRoot + File.separator + DENSITIES[i]
                    + File.separator + name + "_foreground.png";
            if (writeScaledPng(bmp, path, DENSITY_PX[i], DENSITY_PX[i])) {
                written.add(DENSITIES[i]);
            }
        }
        bmp.recycle();
        ctx.refreshViewEditor();
        return ok("Replaced foreground of adaptive icon '" + name + "'. PNGs written: " + written);
    }

    // ==================================================================
    //  set_background
    // ==================================================================

    /**
     * Replace the background of an existing adaptive icon. Accepts either
     * background_color (writes a color entry to colors.xml and rewrites
     * the XML to reference @color/{name}_background) OR background_image
     * (writes a PNG to all density buckets and rewrites the XML to
     * reference @mipmap/{name}_background).
     * Required: name, background_color OR background_image.
     */
    private ToolResult doSetBackground(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_RES_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String mipmapsRoot = getMipmapsRoot(scId);
        String xmlPath = mipmapsRoot + File.separator + "mipmap-anydpi-v26"
                + File.separator + name + ".xml";
        if (!FileUtil.isExistFile(xmlPath)) {
            return err("Adaptive icon '" + name + "' does not exist. Use 'create_adaptive' first.");
        }
        String bgColor = optString(args, "background_color");
        String bgImagePath = optString(args, "background_image");
        String bgRefInXml;
        if (bgColor != null && !bgColor.isEmpty()) {
            if (!HEX_COLOR.matcher(bgColor).matches()) {
                return err("background_color must match #RRGGBB or #AARRGGBB. Got: " + bgColor);
            }
            String colorsXmlPath = getColorsXmlPath(scId);
            try {
                upsertColorEntry(colorsXmlPath, name + "_background", bgColor);
            } catch (Throwable t) {
                return ToolResult.error(t);
            }
            bgRefInXml = "@color/" + name + "_background";
            // Best-effort: delete any stale _background.png files from a
            // previous background_image setting so they don't shadow the color.
            for (String density : DENSITIES) {
                String png = mipmapsRoot + File.separator + density
                        + File.separator + name + "_background.png";
                if (FileUtil.isExistFile(png)) {
                    try { FileUtil.deleteFile(png); } catch (Throwable ignored) {}
                }
            }
        } else if (bgImagePath != null && !bgImagePath.isEmpty()) {
            byte[] bytes = resolveImageBytes(args, "background_image");
            if (bytes == null) return err("background_image could not be decoded.");
            Bitmap bmp = decodeBitmap(bytes);
            if (bmp == null) return err("Failed to decode background_image bitmap.");
            for (int i = 0; i < DENSITIES.length; i++) {
                String path = mipmapsRoot + File.separator + DENSITIES[i]
                        + File.separator + name + "_background.png";
                writeScaledPng(bmp, path, DENSITY_PX[i], DENSITY_PX[i]);
            }
            bmp.recycle();
            bgRefInXml = "@mipmap/" + name + "_background";
            // Best-effort: remove the color entry from colors.xml so it
            // doesn't shadow the PNG.
            String colorsXmlPath = getColorsXmlPath(scId);
            try {
                removeColorEntry(colorsXmlPath, name + "_background");
            } catch (Throwable ignored) {}
        } else {
            return err("Either background_color or background_image is required.");
        }

        // Rewrite the XML so the new background reference takes effect.
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <background android:drawable=\"" + bgRefInXml + "\"/>\n"
                + "    <foreground android:drawable=\"@mipmap/" + name + "_foreground\"/>\n"
                + "</adaptive-icon>\n";
        try {
            FileUtil.writeFile(xmlPath, xml);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        ctx.refreshViewEditor();
        return ok("Replaced background of adaptive icon '" + name + "'. New reference: "
                + bgRefInXml + ". XML rewritten: " + xmlPath);
    }

    // ==================================================================
    //  delete
    // ==================================================================

    /**
     * Delete one or more icons (adaptive or legacy). Removes all
     * mipmap-{density}/{name}*.png entries, the mipmap-anydpi-v26/{name}.xml
     * (if present), and the {name}_background color entry from colors.xml
     * (if present). Accepts either 'name' (single) or 'names' (array).
     */
    private ToolResult doDelete(SketchwareToolContext ctx, String scId, JsonObject args) {
        List<String> names = new ArrayList<>();
        if (args.has("names") && args.get("names").isJsonArray()) {
            JsonArray arr = args.getAsJsonArray("names");
            for (int i = 0; i < arr.size(); i++) {
                String s = arr.get(i).getAsString();
                if (s != null && !s.isEmpty()) names.add(s);
            }
        } else {
            String single = optString(args, "name");
            if (single != null && !single.isEmpty()) names.add(single);
        }
        if (names.isEmpty()) return err("Provide 'name' (string) or 'names' (array of strings).");

        String mipmapsRoot = getMipmapsRoot(scId);
        String colorsXmlPath = getColorsXmlPath(scId);

        List<String> deleted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            if (!VALID_RES_NAME.matcher(name).matches()) {
                missing.add(name + " (invalid name)");
                continue;
            }
            boolean any = false;
            // Adaptive XML.
            String xmlPath = mipmapsRoot + File.separator + "mipmap-anydpi-v26"
                    + File.separator + name + ".xml";
            if (FileUtil.isExistFile(xmlPath)) {
                try { FileUtil.deleteFile(xmlPath); any = true; } catch (Throwable ignored) {}
            }
            // All density buckets: {name}.png, {name}_foreground.png, {name}_background.png.
            for (String density : DENSITIES) {
                for (String suffix : new String[]{"", "_foreground", "_background"}) {
                    String png = mipmapsRoot + File.separator + density
                            + File.separator + name + suffix + ".png";
                    if (FileUtil.isExistFile(png)) {
                        try { FileUtil.deleteFile(png); any = true; } catch (Throwable ignored) {}
                    }
                }
            }
            // Color entry in colors.xml.
            if (FileUtil.isExistFile(colorsXmlPath)) {
                try {
                    if (removeColorEntry(colorsXmlPath, name + "_background")) {
                        any = true;
                    }
                } catch (Throwable ignored) {}
            }
            if (any) {
                deleted.add(name);
            } else {
                missing.add(name);
            }
        }
        ctx.refreshViewEditor();
        if (deleted.isEmpty()) {
            return err("No icons deleted. Missing: " + missing);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted ").append(deleted.size()).append(" icon(s): ").append(deleted);
        if (!missing.isEmpty()) sb.append(". Not found (skipped): ").append(missing);
        return ok(sb.toString());
    }

    // ==================================================================
    //  list
    // ==================================================================

    /**
     * List all launcher icons in the project. Returns a text dump and a
     * JSON array of {name, type:"adaptive"|"legacy", densities:[...]}.
     * Read-only.
     */
    private ToolResult doList(String scId) {
        String mipmapsRoot = getMipmapsRoot(scId);
        if (!FileUtil.isExistFile(mipmapsRoot)) {
            return ok("No icons directory found for project '" + scId + "' (looked at "
                    + mipmapsRoot + ").");
        }

        // Collect distinct icon names.
        java.util.Set<String> names = new java.util.TreeSet<>();
        // Adaptive icons (XML in mipmap-anydpi-v26).
        String anyDpiDir = mipmapsRoot + File.separator + "mipmap-anydpi-v26";
        if (FileUtil.isExistFile(anyDpiDir)) {
            ArrayList<String> files = new ArrayList<>();
            FileUtil.listDir(anyDpiDir, files);
            for (String f : files) {
                String n = new File(f).getName();
                if (n.endsWith(".xml")) {
                    names.add(n.substring(0, n.length() - ".xml".length()));
                }
            }
        }
        // Legacy icons (PNG {name}.png present in mipmap-mdpi).
        String mdpiDir = mipmapsRoot + File.separator + "mipmap-mdpi";
        if (FileUtil.isExistFile(mdpiDir)) {
            ArrayList<String> files = new ArrayList<>();
            FileUtil.listDir(mdpiDir, files);
            for (String f : files) {
                String n = new File(f).getName();
                if (n.endsWith(".png") && !n.contains("_")) {
                    names.add(n.substring(0, n.length() - ".png".length()));
                }
            }
        }
        // Also scan other density buckets for _foreground.png that may
        // indicate an adaptive icon whose XML is missing.
        for (String density : DENSITIES) {
            String dir = mipmapsRoot + File.separator + density;
            if (!FileUtil.isExistFile(dir)) continue;
            ArrayList<String> files = new ArrayList<>();
            FileUtil.listDir(dir, files);
            for (String f : files) {
                String n = new File(f).getName();
                if (n.endsWith("_foreground.png")) {
                    names.add(n.substring(0, n.length() - "_foreground.png".length()));
                }
            }
        }

        if (names.isEmpty()) {
            return ok("No launcher icons found in project '" + scId + "'.");
        }

        JsonArray arr = new JsonArray();
        StringBuilder sb = new StringBuilder();
        sb.append("Launcher icons in project '").append(scId).append("' (").append(names.size()).append("):\n");
        for (String name : names) {
            boolean isAdaptive = FileUtil.isExistFile(mipmapsRoot + File.separator
                    + "mipmap-anydpi-v26" + File.separator + name + ".xml");
            List<String> densitiesPresent = new ArrayList<>();
            for (String density : DENSITIES) {
                String legacy = mipmapsRoot + File.separator + density
                        + File.separator + name + ".png";
                String fg = mipmapsRoot + File.separator + density
                        + File.separator + name + "_foreground.png";
                if (FileUtil.isExistFile(legacy) || FileUtil.isExistFile(fg)) {
                    densitiesPresent.add(density);
                }
            }
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            o.addProperty("type", isAdaptive ? "adaptive" : "legacy");
            JsonArray darr = new JsonArray();
            for (String d : densitiesPresent) darr.add(d);
            o.add("densities", darr);
            arr.add(o);

            sb.append("  - name='").append(name).append("' type=")
              .append(isAdaptive ? "adaptive" : "legacy")
              .append(" densities=").append(densitiesPresent).append("\n");
        }
        sb.append("\nJSON: ").append(arr.toString());
        return ok(sb.toString());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Root directory for this project's icon PNGs/XML: wq.e()/{scId}/mipmaps/. */
    private static String getMipmapsRoot(String scId) {
        return wq.e() + File.separator + scId + File.separator + "mipmaps";
    }

    /** Absolute path to the project's colors.xml file. */
    private static String getColorsXmlPath(String scId) {
        return wq.b(scId) + File.separator + "files" + File.separator
                + "resource" + File.separator + "values" + File.separator + "colors.xml";
    }

    /** Returns true if an icon with the given name already exists (adaptive XML or legacy PNG). */
    private static boolean iconExists(String mipmapsRoot, String name) {
        if (FileUtil.isExistFile(mipmapsRoot + File.separator + "mipmap-anydpi-v26"
                + File.separator + name + ".xml")) return true;
        for (String density : DENSITIES) {
            if (FileUtil.isExistFile(mipmapsRoot + File.separator + density
                    + File.separator + name + ".png")) return true;
            if (FileUtil.isExistFile(mipmapsRoot + File.separator + density
                    + File.separator + name + "_foreground.png")) return true;
        }
        return false;
    }

    /**
     * Resolve an image argument to raw bytes. Accepts either:
     * <ul>
     *   <li>An absolute file path (looked up via the corresponding
     *       {@code *_file_path} companion key, OR if the raw value itself
     *       is a valid existing file path).</li>
     *   <li>A base64 string (validated against BASE64_PATTERN).</li>
     * </ul>
     * Returns null if neither is available.
     */
    private byte[] resolveImageBytes(JsonObject args, String imageKey) {
        String imageVal = optString(args, imageKey);
        if (imageVal == null || imageVal.isEmpty()) return null;
        // Try file path first: either an explicit {key}_file_path, OR the
        // value itself if it looks like a path that exists.
        String filePathKey = imageKey + "_file_path";
        String filePath = optString(args, filePathKey);
        if (filePath == null || filePath.isEmpty()) {
            // Heuristic: if the value starts with '/' and the file exists,
            // treat it as a path.
            if (imageVal.startsWith("/") && new File(imageVal).isFile()) {
                filePath = imageVal;
            }
        }
        if (filePath != null && !filePath.isEmpty()) {
            File f = new File(filePath);
            if (f.isFile()) {
                try {
                    return readFileBytes(f);
                } catch (Throwable t) {
                    return null;
                }
            }
        }
        // Fall back to base64.
        if (looksLikeBase64(imageVal)) {
            try {
                return android.util.Base64.decode(imageVal, android.util.Base64.DEFAULT);
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    /** Read all bytes from a file. */
    private static byte[] readFileBytes(File f) throws Exception {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            while (off < buf.length) {
                int n = fis.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            return buf;
        }
    }

    /** Decode raw bytes into a Bitmap (ARGB_8888). */
    private static Bitmap decodeBitmap(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Scale a source bitmap to the requested size and write it as a PNG.
     * Returns true on success, false on failure. The source bitmap is NOT
     * recycled by this method (caller manages its lifecycle).
     */
    private static boolean writeScaledPng(Bitmap src, String destPath, int w, int h) {
        if (src == null) return false;
        try {
            FileUtil.makeDir(new File(destPath).getParent());
            Bitmap scaled;
            if (src.getWidth() == w && src.getHeight() == h) {
                scaled = src;
            } else {
                scaled = Bitmap.createScaledBitmap(src, w, h, true);
            }
            try (FileOutputStream fos = new FileOutputStream(destPath)) {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            if (scaled != src) scaled.recycle();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Validate base64: matches BASE64_PATTERN, length is multiple of 4, >= 8 chars. */
    private static boolean looksLikeBase64(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!BASE64_PATTERN.matcher(s).matches()) return false;
        if (s.length() % 4 != 0) return false;
        return s.length() >= 8;
    }

    /**
     * Insert or replace a {@code <color name="...">#RRGGBB</color>} entry
     * in colors.xml. If the file does not exist, it is created with the
     * single entry. If the entry exists, it is regex-replaced. Otherwise
     * the entry is appended before {@code </resources>}.
     */
    private static void upsertColorEntry(String colorsXmlPath, String name, String hexValue) throws Exception {
        if (!FileUtil.isExistFile(colorsXmlPath)) {
            FileUtil.makeDir(new File(colorsXmlPath).getParent());
            String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
                    + "    <color name=\"" + name + "\">" + hexValue + "</color>\n"
                    + "</resources>\n";
            FileUtil.writeFile(colorsXmlPath, xml);
            return;
        }
        String xml = FileUtil.readFile(colorsXmlPath);
        String entryRegex = "(<color\\s+name=\"" + Pattern.quote(name) + "\">)(.*?)(</color>)";
        if (xml.matches("(?s).*" + entryRegex + ".*")) {
            xml = xml.replaceAll(entryRegex, "$1" + hexValue + "$3");
        } else if (xml.contains("</resources>")) {
            // Insert before </resources>.
            String newEntry = "    <color name=\"" + name + "\">" + hexValue + "</color>\n";
            xml = xml.replace("</resources>", newEntry + "</resources>");
        } else {
            // No </resources> closing tag - just append.
            xml = xml + "\n<color name=\"" + name + "\">" + hexValue + "</color>\n";
        }
        FileUtil.writeFile(colorsXmlPath, xml);
    }

    /**
     * Remove a {@code <color name="...">...</color>} entry from colors.xml.
     * Returns true if the file was modified (entry was present and removed),
     * false otherwise.
     */
    private static boolean removeColorEntry(String colorsXmlPath, String name) throws Exception {
        if (!FileUtil.isExistFile(colorsXmlPath)) return false;
        String xml = FileUtil.readFile(colorsXmlPath);
        // Match the entry plus its trailing newline (and any leading whitespace).
        String entryRegex = "\\s*<color\\s+name=\"" + Pattern.quote(name) + "\">.*?</color>\\s*";
        String newXml = xml.replaceAll(entryRegex, "\n");
        if (newXml.equals(xml)) return false;
        FileUtil.writeFile(colorsXmlPath, newXml);
        return true;
    }
}
