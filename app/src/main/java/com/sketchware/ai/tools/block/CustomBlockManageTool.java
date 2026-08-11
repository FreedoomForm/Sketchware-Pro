package com.sketchware.ai.tools.block;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

/**
 * custom_block_manage — universal tool for managing Sketchware-Pro's
 * <b>custom block palettes</b> (loaded from
 * {@code /sdcard/.sketchware/resources/block/My Block/block.json} and
 * {@code palette.json}).
 *
 * <p>This is a <b>separate subsystem</b> from {@link MoreblockManageTool}
 * (which manages project-level moreblocks via {@code jC.b(scId)}). Custom
 * blocks are <b>global</b> — they appear in every project's palette once
 * defined, and are managed by the static {@code BlocksManager} activity.
 *
 * <p>Actions (10):
 * <ul>
 *   <li><b>create</b> — create a new custom block in a palette</li>
 *   <li><b>edit</b> — edit an existing custom block's fields</li>
 *   <li><b>duplicate</b> — duplicate a block (auto-suffixes name with _copyNN)</li>
 *   <li><b>move</b> — move a block to a different palette</li>
 *   <li><b>delete</b> — delete a block (optionally permanent vs recycle bin)</li>
 *   <li><b>restore</b> — restore a block from the recycle bin</li>
 *   <li><b>export</b> — export one block (by name) or a whole palette to a JSON file</li>
 *   <li><b>import</b> — import blocks from a JSON file into a palette</li>
 *   <li><b>reorder_palettes</b> — swap two palette categories (by index)</li>
 *   <li><b>list</b> — list all custom blocks (optionally filtered by palette)</li>
 * </ul>
 *
 * <p>File layout (default paths from {@link ConfigActivity}):
 * <ul>
 *   <li>Palette file: {@code <ext>/.sketchware/resources/block/My Block/palette.json}
 *       — a JSON array of {@code {name, color}} objects.</li>
 *   <li>Block file: {@code <ext>/.sketchware/resources/block/My Block/block.json}
 *       — a JSON array of block definition objects.</li>
 * </ul>
 *
 * <p>Block object fields:
 * <ul>
 *   <li>{@code name} — unique block name (String)</li>
 *   <li>{@code type} — block type: " ", "regular", "c" (if), "e" (if-else),
 *       "s" (String), "b" (Boolean), "d" (Number), "v" (Variable),
 *       "a" (Map), "f" (stop), "l" (List), "p" (Component), "h" (Header)</li>
 *   <li>{@code typeName} — optional type name (String)</li>
 *   <li>{@code spec} — block spec with parameter placeholders, e.g.
 *       {@code "myBlock %s.inputOnly %b %d"} (see {@code BlocksManagerCreatorActivity})</li>
 *   <li>{@code color} — hex color String like {@code "#FFAABB"}</li>
 *   <li>{@code code} — Java code template (String)</li>
 *   <li>{@code palette} — palette index as String, where {@code "9"} = palette #0,
 *       {@code "10"} = palette #1, ... {@code "-1"} = recycle bin</li>
 *   <li>{@code spec2} — (optional) second spec line for type "e" (if-else)</li>
 *   <li>{@code imports} — (optional) Java imports string</li>
 * </ul>
 */
public final class CustomBlockManageTool extends UniversalTool {

    /** Recycle bin palette value (sentinel). */
    private static final String RECYCLE_BIN_PALETTE = "-1";
    /** Offset added to palette index to get the block's "palette" field value. */
    private static final int PALETTE_OFFSET = 9;
    /** Default export directory (relative to external storage). */
    private static final String DEFAULT_EXPORT_DIR =
            "/.sketchware/resources/block/export/";

    public CustomBlockManageTool() {
        super("custom_block_manage",
                "Manage global custom block palettes (loaded from "
                        + ".sketchware/resources/block/My Block/block.json): "
                        + "create, edit, duplicate, move, delete, restore, "
                        + "export, import, reorder_palettes, list.",
                "block", false, false,
                "create",
                "edit",
                "duplicate",
                "move",
                "delete",
                "restore",
                "export",
                "import",
                "reorder_palettes",
                "list");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        // Block identification / palette lookup
        addStringProp(props, "name", "Block name (unique identifier within the block file).");
        addStringProp(props, "palette_name", "Palette name (looked up in palette.json). Used as alternative to palette_index for create/move/import/export/list.");
        addIntProp(props, "palette_index", "0-based palette index. Used as alternative to palette_name.");

        // Block definition fields (for create / edit)
        addStringProp(props, "type", "(create/edit) Block type: regular|c|e|s|b|d|v|a|f|l|p|h. 'regular' or ' ' = generic block.");
        addStringProp(props, "type_name", "(create/edit) Optional type name.");
        addStringProp(props, "spec", "(create/edit) Block spec with parameter placeholders, e.g. 'myBlock %s.inputOnly %b %d'.");
        addStringProp(props, "spec2", "(create/edit) Second spec line for type 'e' (if-else).");
        addStringProp(props, "color", "(create/edit) Hex color like '#FFAABB'.");
        addStringProp(props, "code", "(create/edit) Java code template.");
        addStringProp(props, "imports", "(create/edit) Java imports string (optional).");

        // File paths (for export/import)
        addStringProp(props, "file_path", "(export/import) Absolute file path. For export: defaults to .sketchware/resources/block/export/<name>.json. For import: required.");

        // delete flag
        addBoolProp(props, "permanent", "(delete) If true (default), permanently remove. If false, move to recycle bin (palette='-1').");

        // reorder
        addIntProp(props, "from_index", "(reorder_palettes) Source 0-based palette index.");
        addIntProp(props, "to_index", "(reorder_palettes) Target 0-based palette index.");
    }

    private static void addStringProp(JsonObject props, String name, String desc) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", desc);
        props.add(name, p);
    }

    private static void addIntProp(JsonObject props, String name, String desc) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "integer");
        p.addProperty("description", desc);
        props.add(name, p);
    }

    private static void addBoolProp(JsonObject props, String name, String desc) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "boolean");
        p.addProperty("description", desc);
        props.add(name, p);
    }

    // ============================================================
    //  Dispatch
    // ============================================================

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        try {
            switch (action) {
                case "create":            return doCreate(args);
                case "edit":              return doEdit(args);
                case "duplicate":         return doDuplicate(args);
                case "move":              return doMove(args);
                case "delete":            return doDelete(args);
                case "restore":           return doRestore(args);
                case "export":            return doExport(args);
                case "import":            return doImport(args);
                case "reorder_palettes":  return doReorderPalettes(args);
                case "list":              return doList(args);
                default:
                    return err("Unknown action: " + action);
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        } finally {
            // Always refresh the in-memory BlockLoader so the UI picks up changes.
            try { BlockLoader.refresh(); } catch (Throwable ignored) {}
        }
    }

    // ============================================================
    //  Path resolution
    // ============================================================

    private static String paletteFilePath() {
        String rel = ConfigActivity.getStringSettingValueOrSetAndGet(
                ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                (String) ConfigActivity.getDefaultValue(
                        ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH));
        return FileUtil.getExternalStorageDir() + rel;
    }

    private static String blockFilePath() {
        String rel = ConfigActivity.getStringSettingValueOrSetAndGet(
                ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH,
                (String) ConfigActivity.getDefaultValue(
                        ConfigActivity.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH));
        return FileUtil.getExternalStorageDir() + rel;
    }

    // ============================================================
    //  JSON helpers
    // ============================================================

    private static ArrayList<HashMap<String, Object>> readJsonList(String path) {
        if (!FileUtil.isExistFile(path)) {
            return new ArrayList<>();
        }
        String content = FileUtil.readFile(path);
        if (content == null || content.isEmpty() || content.trim().equals("[]")) {
            return new ArrayList<>();
        }
        try {
            ArrayList<HashMap<String, Object>> list =
                    getGson().fromJson(content, Helper.TYPE_MAP_LIST);
            return list != null ? list : new ArrayList<>();
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    private static void writeJsonList(String path, ArrayList<HashMap<String, Object>> list) {
        FileUtil.writeFile(path, getGson().toJson(list));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String str(Object o, String def) {
        String s = str(o);
        return s == null ? def : s;
    }

    // ============================================================
    //  Palette index <-> name conversion
    // ============================================================

    /** Resolve the palette "value" (the String stored in block.palette) from
     *  either a palette_name or palette_index argument. Returns null if neither
     *  is provided or the lookup failed. */
    private String resolvePaletteValue(JsonObject args, ArrayList<HashMap<String, Object>> palettes) {
        String paletteName = optString(args, "palette_name");
        if (paletteName != null) {
            for (int i = 0; i < palettes.size(); i++) {
                if (paletteName.equals(str(palettes.get(i).get("name")))) {
                    return String.valueOf(i + PALETTE_OFFSET);
                }
            }
            return null; // not found
        }
        int idx = optInt(args, "palette_index", -1);
        if (idx >= 0 && idx < palettes.size()) {
            return String.valueOf(idx + PALETTE_OFFSET);
        }
        return null;
    }

    /** Look up a block by name in the list. Returns the index, or -1 if not found. */
    private static int findBlockIndex(ArrayList<HashMap<String, Object>> blocks, String name) {
        for (int i = 0; i < blocks.size(); i++) {
            if (name.equals(str(blocks.get(i).get("name")))) return i;
        }
        return -1;
    }

    // ============================================================
    //  Action implementations
    // ============================================================

    private ToolResult doCreate(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");
        String spec = optString(args, "spec", "");
        String type = optString(args, "type", "regular");
        String typeName = optString(args, "type_name", "");
        String color = optString(args, "color", "#FFFFFF");
        String code = optString(args, "code", "");
        String imports = optString(args, "imports");
        String spec2 = optString(args, "spec2");

        // Normalize "regular" -> " " (Sketchware's internal convention)
        if ("regular".equalsIgnoreCase(type) || type.isEmpty()) type = " ";

        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        String paletteValue = resolvePaletteValue(args, palettes);
        if (paletteValue == null) {
            return err("Could not resolve target palette. Provide palette_name (must exist) or palette_index (0-based).");
        }

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);

        // Disallow duplicate names
        if (findBlockIndex(blocks, name) >= 0) {
            return err("Block name '" + name + "' already exists. Use a different name or the 'edit' action.");
        }

        HashMap<String, Object> block = new HashMap<>();
        block.put("name", name);
        block.put("type", type);
        block.put("typeName", typeName);
        block.put("spec", spec);
        block.put("color", color);
        if ("e".equals(type) && spec2 != null) {
            block.put("spec2", spec2);
        }
        if (imports != null && !imports.isEmpty()) {
            block.put("imports", imports);
        }
        block.put("code", code);
        block.put("palette", paletteValue);

        blocks.add(block);
        writeJsonList(blockPath, blocks);
        return ok("Created custom block '" + name + "' in palette '"
                + paletteNameOrIndex(args, palettes) + "' (palette=" + paletteValue + ").");
    }

    private ToolResult doEdit(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        int idx = findBlockIndex(blocks, name);
        if (idx < 0) return err("Block '" + name + "' not found.");

        HashMap<String, Object> block = blocks.get(idx);
        boolean changed = false;

        String spec = optString(args, "spec");
        if (spec != null) { block.put("spec", spec); changed = true; }

        String type = optString(args, "type");
        if (type != null) {
            if ("regular".equalsIgnoreCase(type) || type.isEmpty()) type = " ";
            block.put("type", type);
            changed = true;
        }

        String typeName = optString(args, "type_name");
        if (typeName != null) { block.put("typeName", typeName); changed = true; }

        String color = optString(args, "color");
        if (color != null) { block.put("color", color); changed = true; }

        String code = optString(args, "code");
        if (code != null) { block.put("code", code); changed = true; }

        String imports = optString(args, "imports");
        if (imports != null) { block.put("imports", imports); changed = true; }

        String spec2 = optString(args, "spec2");
        if (spec2 != null) { block.put("spec2", spec2); changed = true; }

        if (!changed) return err("No edit fields provided. Specify at least one of: spec, type, type_name, color, code, imports, spec2.");

        writeJsonList(blockPath, blocks);
        return ok("Edited custom block '" + name + "'.");
    }

    private ToolResult doDuplicate(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        int idx = findBlockIndex(blocks, name);
        if (idx < 0) return err("Block '" + name + "' not found.");

        HashMap<String, Object> source = blocks.get(idx);
        HashMap<String, Object> copy = new HashMap<>(source);
        String newName = name;
        if (name.matches("(?s).*_copy[0-9][0-9]")) {
            newName = name.replaceAll("_copy[0-9][0-9]", "_copy" + SketchwareUtil.getRandom(11, 99));
        } else {
            newName = name + "_copy" + SketchwareUtil.getRandom(11, 99);
        }
        copy.put("name", newName);
        blocks.add(idx + 1, copy);
        writeJsonList(blockPath, blocks);
        return ok("Duplicated block '" + name + "' as '" + newName + "'.");
    }

    private ToolResult doMove(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");

        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        String targetPaletteValue = resolvePaletteValue(args, palettes);
        if (targetPaletteValue == null) {
            return err("Could not resolve target palette. Provide palette_name or palette_index.");
        }

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        int idx = findBlockIndex(blocks, name);
        if (idx < 0) return err("Block '" + name + "' not found.");

        HashMap<String, Object> block = blocks.get(idx);
        String oldPalette = str(block.get("palette"));
        block.put("palette", targetPaletteValue);
        // Sketchware's UI also swaps the block to the end of the list on move — mirror that.
        if (idx != blocks.size() - 1) {
            blocks.remove(idx);
            blocks.add(block);
        }
        writeJsonList(blockPath, blocks);
        return ok("Moved block '" + name + "' from palette=" + oldPalette
                + " to palette=" + targetPaletteValue + " ("
                + paletteNameOrIndex(args, palettes) + ").");
    }

    private ToolResult doDelete(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");
        boolean permanent = optBool(args, "permanent", true);

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        int idx = findBlockIndex(blocks, name);
        if (idx < 0) return err("Block '" + name + "' not found.");

        if (permanent) {
            blocks.remove(idx);
            writeJsonList(blockPath, blocks);
            return ok("Permanently deleted custom block '" + name + "'.");
        } else {
            blocks.get(idx).put("palette", RECYCLE_BIN_PALETTE);
            writeJsonList(blockPath, blocks);
            return ok("Moved custom block '" + name + "' to recycle bin (palette=-1).");
        }
    }

    private ToolResult doRestore(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required");

        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        String targetPaletteValue = resolvePaletteValue(args, palettes);
        if (targetPaletteValue == null) {
            return err("Could not resolve target palette to restore to. Provide palette_name or palette_index.");
        }

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        int idx = findBlockIndex(blocks, name);
        if (idx < 0) return err("Block '" + name + "' not found.");

        HashMap<String, Object> block = blocks.get(idx);
        String currentPalette = str(block.get("palette"));
        if (!RECYCLE_BIN_PALETTE.equals(currentPalette)) {
            return err("Block '" + name + "' is not in the recycle bin (current palette=" + currentPalette + "). Use 'move' instead.");
        }
        block.put("palette", targetPaletteValue);
        // Mirror Sketchware's UI: swap to end of list on restore.
        if (idx != blocks.size() - 1) {
            blocks.remove(idx);
            blocks.add(block);
        }
        writeJsonList(blockPath, blocks);
        return ok("Restored block '" + name + "' to palette="
                + targetPaletteValue + " (" + paletteNameOrIndex(args, palettes) + ").");
    }

    private ToolResult doExport(JsonObject args) {
        String name = optString(args, "name");
        String paletteName = optString(args, "palette_name");
        if ((name == null || name.isEmpty()) && (paletteName == null || paletteName.isEmpty())) {
            return err("Either name (single block) or palette_name (whole palette) is required.");
        }

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);

        ArrayList<HashMap<String, Object>> toExport = new ArrayList<>();
        String label;
        if (name != null && !name.isEmpty()) {
            int idx = findBlockIndex(blocks, name);
            if (idx < 0) return err("Block '" + name + "' not found.");
            toExport.add(blocks.get(idx));
            label = name;
        } else {
            int paletteIdx = -1;
            for (int i = 0; i < palettes.size(); i++) {
                if (paletteName.equals(str(palettes.get(i).get("name")))) {
                    paletteIdx = i; break;
                }
            }
            if (paletteIdx < 0) return err("Palette '" + paletteName + "' not found.");
            String paletteValue = String.valueOf(paletteIdx + PALETTE_OFFSET);
            for (HashMap<String, Object> b : blocks) {
                if (paletteValue.equals(str(b.get("palette")))) toExport.add(b);
            }
            label = paletteName;
        }

        if (toExport.isEmpty()) {
            return err("No blocks to export.");
        }

        String exportPath = optString(args, "file_path");
        if (exportPath == null || exportPath.isEmpty()) {
            exportPath = new File(FileUtil.getExternalStorageDir() + DEFAULT_EXPORT_DIR,
                    label + ".json").getAbsolutePath();
        }
        FileUtil.writeFile(exportPath, getGson().toJson(toExport));
        return ok("Exported " + toExport.size() + " block(s) to " + exportPath + ".");
    }

    private ToolResult doImport(JsonObject args) {
        String path = optString(args, "file_path");
        if (path == null || path.isEmpty()) return err("file_path is required");

        if (!FileUtil.isExistFile(path)) return err("File not found: " + path);
        String content = FileUtil.readFile(path);
        if (content == null || content.isEmpty() || content.trim().equals("[]")) {
            return err("File is empty: " + path);
        }

        ArrayList<HashMap<String, Object>> incoming;
        try {
            incoming = getGson().fromJson(content, Helper.TYPE_MAP_LIST);
        } catch (Throwable t) {
            return err("Invalid JSON in file: " + t.getMessage());
        }
        if (incoming == null || incoming.isEmpty()) {
            return err("No blocks found in file.");
        }

        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        String targetPaletteValue = resolvePaletteValue(args, palettes);
        if (targetPaletteValue == null) {
            return err("Could not resolve target palette. Provide palette_name or palette_index.");
        }

        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);

        int imported = 0;
        for (HashMap<String, Object> b : incoming) {
            // Set the palette to the target; do not preserve the imported palette value.
            b.put("palette", targetPaletteValue);
            blocks.add(b);
            imported++;
        }
        writeJsonList(blockPath, blocks);
        return ok("Imported " + imported + " block(s) from " + path
                + " into palette " + targetPaletteValue
                + " (" + paletteNameOrIndex(args, palettes) + ").");
    }

    private ToolResult doReorderPalettes(JsonObject args) {
        int from = optInt(args, "from_index", -1);
        int to = optInt(args, "to_index", -1);
        if (from < 0 || to < 0) return err("from_index and to_index are required (0-based).");

        String palettePath = paletteFilePath();
        ArrayList<HashMap<String, Object>> palettes = readJsonList(palettePath);
        if (from >= palettes.size() || to >= palettes.size()) {
            return err("Index out of range. Palette count: " + palettes.size());
        }
        if (from == to) return ok("from_index == to_index; nothing to do.");

        // Swap the two palettes.
        Collections.swap(palettes, from, to);
        writeJsonList(palettePath, palettes);

        // Update each block's "palette" field to reflect the new positions.
        String blockPath = blockFilePath();
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockPath);
        String fromValue = String.valueOf(from + PALETTE_OFFSET);
        String toValue = String.valueOf(to + PALETTE_OFFSET);
        final String TEMP = "TEMP_REORDER";
        for (Map<String, Object> b : blocks) {
            String p = str(b.get("palette"));
            if (fromValue.equals(p)) {
                b.put("palette", TEMP);
            } else if (toValue.equals(p)) {
                b.put("palette", fromValue);
            }
        }
        for (Map<String, Object> b : blocks) {
            if (TEMP.equals(b.get("palette"))) {
                b.put("palette", toValue);
            }
        }
        writeJsonList(blockPath, blocks);

        return ok("Swapped palette #" + from + " <-> #" + to + ".");
    }

    private ToolResult doList(JsonObject args) {
        String paletteName = optString(args, "palette_name");
        boolean includeRecycleBin = optBool(args, "include_recycle_bin", true);

        ArrayList<HashMap<String, Object>> palettes = readJsonList(paletteFilePath());
        ArrayList<HashMap<String, Object>> blocks = readJsonList(blockFilePath());

        StringBuilder sb = new StringBuilder();
        sb.append("Palettes (").append(palettes.size()).append("):\n");
        for (int i = 0; i < palettes.size(); i++) {
            HashMap<String, Object> p = palettes.get(i);
            sb.append("  [").append(i).append("] name=").append(str(p.get("name")))
              .append(" color=").append(str(p.get("color"), "#FFFFFF")).append("\n");
        }

        // Filter by palette if requested.
        String filterValue = null;
        if (paletteName != null && !paletteName.isEmpty()) {
            for (int i = 0; i < palettes.size(); i++) {
                if (paletteName.equals(str(palettes.get(i).get("name")))) {
                    filterValue = String.valueOf(i + PALETTE_OFFSET);
                    break;
                }
            }
            if (filterValue == null) {
                return err("Palette '" + paletteName + "' not found.");
            }
        }

        sb.append("\nBlocks (").append(blocks.size()).append(" total):\n");
        int shown = 0;
        for (int i = 0; i < blocks.size(); i++) {
            HashMap<String, Object> b = blocks.get(i);
            String pVal = str(b.get("palette"), "?");
            if (filterValue != null && !filterValue.equals(pVal)) continue;
            if (!includeRecycleBin && RECYCLE_BIN_PALETTE.equals(pVal)) continue;
            sb.append("  [").append(i).append("] name=").append(str(b.get("name")))
              .append(" type=").append(str(b.get("type"), " "))
              .append(" palette=").append(pVal);
            String paletteLabel = paletteLabelForValue(pVal, palettes);
            if (paletteLabel != null) sb.append(" (").append(paletteLabel).append(")");
            sb.append("\n");
            shown++;
        }
        sb.append("\nDisplayed: ").append(shown).append(" block(s).");
        return ok(sb.toString());
    }

    // ============================================================
    //  Small helpers
    // ============================================================

    /** Returns a human-readable label for the palette argument used in this call. */
    private String paletteNameOrIndex(JsonObject args, ArrayList<HashMap<String, Object>> palettes) {
        String paletteName = optString(args, "palette_name");
        if (paletteName != null) return paletteName;
        int idx = optInt(args, "palette_index", -1);
        if (idx >= 0 && idx < palettes.size()) {
            String name = str(palettes.get(idx).get("name"));
            return name != null ? name : ("palette#" + idx);
        }
        return "?";
    }

    /** Returns the palette name (or "Recycle Bin" for -1) for a given palette value. */
    private static String paletteLabelForValue(String paletteValue, ArrayList<HashMap<String, Object>> palettes) {
        if (RECYCLE_BIN_PALETTE.equals(paletteValue)) return "Recycle Bin";
        try {
            int v = Integer.parseInt(paletteValue);
            int idx = v - PALETTE_OFFSET;
            if (idx >= 0 && idx < palettes.size()) {
                String name = str(palettes.get(idx).get("name"));
                return name != null ? name : ("palette#" + idx);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
