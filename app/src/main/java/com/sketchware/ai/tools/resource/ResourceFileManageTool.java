package com.sketchware.ai.tools.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;

/**
 * resource_file_manage — universal tool for managing files in the project's
 * {@code resource/} directory (drawable XML, raw, anim, menu, layouts not
 * covered by view_manage_layout, etc.).
 *
 * <p>Wires the AI agent to Sketchware-Pro's resource-file manager
 * ({@link mod.agus.jcoderz.editor.manage.resource.ManageResourceActivity}).
 *
 * <p><b>Storage model</b>: the project resource directory is
 * {@code <ext>/.sketchware/data/{scId}/files/resource/} (see
 * {@link FilePathUtil#getPathResource(String)}). Subfolders include
 * {@code anim}, {@code drawable}, {@code drawable-xhdpi}, {@code layout},
 * {@code menu}, {@code values}.
 *
 * <p>Actions (7):
 * <ul>
 *   <li><b>create_folder</b> — create a new folder (params: {@code path}).</li>
 *   <li><b>create_xml</b> — create a new XML file (params: {@code path},
 *       {@code content}).</li>
 *   <li><b>import</b> — import one or more external files (params:
 *       {@code source_paths} array, {@code dest_dir}).</li>
 *   <li><b>edit</b> — overwrite an existing XML/text file (params:
 *       {@code path}, {@code content}).</li>
 *   <li><b>rename</b> — rename a file or folder (params: {@code old_path},
 *       {@code new_path}).</li>
 *   <li><b>delete</b> — delete a file or folder (params: {@code path}).</li>
 *   <li><b>list</b> — list entries in a directory (read-only, auto-approved;
 *       params: {@code path} — defaults to resource root).</li>
 * </ul>
 *
 * <p>All paths are RELATIVE to the project resource directory. The tool
 * joins them with {@link File#separator} and rejects any path that
 * escapes the resource root (e.g. via {@code ..}).
 */
public final class ResourceFileManageTool extends UniversalTool {

    /** Allow alphanumerics, underscore, hyphen, dot, and slash separators. */
    private static final Pattern SAFE_REL_PATH = Pattern.compile("^[A-Za-z0-9_][A-Za-z0-9_/\\-.]*$");

    public ResourceFileManageTool() {
        super("resource_file_manage",
                "Manage files in the project resource/ directory (drawable XML, "
                        + "raw, anim, menu, etc.): create_folder, create_xml, "
                        + "import, edit, rename, delete, list. Paths are relative "
                        + "to the project resource root.",
                "resource", false, false,
                "create_folder", "create_xml", "import", "edit",
                "rename", "delete", "list");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "path", "Relative path (to project resource dir) of the target file or folder.");
        addStringProp(props, "old_path", "(rename) Relative path of the existing file/folder.");
        addStringProp(props, "new_path", "(rename) Relative path of the new name.");
        addStringProp(props, "dest_dir", "(import) Destination directory (relative to resource root) to copy into.");
        addStringProp(props, "content", "(create_xml/edit) File content (text/XML).");
        addArrayProp(props, "source_paths", "(import) Array of absolute source file paths to import.");
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
        String root = new FilePathUtil().getPathResource(scId);

        switch (action) {
            case "create_folder": return doCreateFolder(args, root);
            case "create_xml":    return doCreateXml(args, root);
            case "import":        return doImport(args, root);
            case "edit":          return doEdit(args, root);
            case "rename":        return doRename(args, root);
            case "delete":        return doDelete(args, root);
            case "list":          return doList(args, root);
            default:              return err("Unknown action: " + action);
        }
    }

    // ==================================================================
    //  create_folder
    // ==================================================================
    private ToolResult doCreateFolder(JsonObject args, String root) {
        String rel = optString(args, "path");
        if (rel == null || rel.isEmpty()) return err("path is required.");
        if (!isSafeRelPath(rel)) return err("Invalid path '" + rel + "'. Must match " + SAFE_REL_PATH + ".");
        String abs = resolveUnderRoot(root, rel);
        if (abs == null) return err("Path escapes resource root: " + rel);
        if (FileUtil.isExistFile(abs)) return err("Path already exists: " + rel);
        FileUtil.makeDir(abs);
        return ok("Created folder '" + rel + "' (absolute: " + abs + ").");
    }

    // ==================================================================
    //  create_xml
    // ==================================================================
    private ToolResult doCreateXml(JsonObject args, String root) {
        String rel = optString(args, "path");
        if (rel == null || rel.isEmpty()) return err("path is required.");
        if (!isSafeRelPath(rel)) return err("Invalid path '" + rel + "'. Must match " + SAFE_REL_PATH + ".");
        String content = optString(args, "content", "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        String abs = resolveUnderRoot(root, rel);
        if (abs == null) return err("Path escapes resource root: " + rel);
        if (FileUtil.isExistFile(abs)) return err("File already exists: " + rel + ". Use 'edit' to overwrite.");
        // Ensure parent dir exists.
        String parent = abs.substring(0, Math.max(abs.lastIndexOf(File.separator), 0));
        if (!FileUtil.isExistFile(parent)) FileUtil.makeDir(parent);
        FileUtil.writeFile(abs, content);
        return ok("Created XML file '" + rel + "' (" + content.length() + " chars).");
    }

    // ==================================================================
    //  import
    // ==================================================================
    private ToolResult doImport(JsonObject args, String root) {
        if (!args.has("source_paths") || !args.get("source_paths").isJsonArray()) {
            return err("source_paths (array) is required.");
        }
        JsonArray arr = args.getAsJsonArray("source_paths");
        if (arr.size() == 0) return err("source_paths array is empty.");
        String destRel = optString(args, "dest_dir", "");
        String destAbs;
        if (destRel.isEmpty()) {
            destAbs = root;
        } else {
            if (!isSafeRelPath(destRel)) return err("Invalid dest_dir '" + destRel + "'.");
            destAbs = resolveUnderRoot(root, destRel);
            if (destAbs == null) return err("dest_dir escapes resource root: " + destRel);
        }
        if (!FileUtil.isExistFile(destAbs)) FileUtil.makeDir(destAbs);

        List<String> imported = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String src = arr.get(i).getAsString();
            File srcFile = new File(src);
            if (!srcFile.exists()) { missing.add(src); continue; }
            String destPath = destAbs + File.separator + srcFile.getName();
            try {
                FileUtil.copyDirectory(srcFile, new File(destPath));
                imported.add(srcFile.getName());
            } catch (Throwable t) {
                missing.add(src + " (copy failed: " + t.getMessage() + ")");
            }
        }
        if (imported.isEmpty()) {
            return err("No files imported. Missing/failed: " + missing);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Imported ").append(imported.size()).append(" file(s) into '")
          .append(destRel.isEmpty() ? "<resource root>" : destRel)
          .append("': ").append(imported);
        if (!missing.isEmpty()) sb.append(". Missing/failed: ").append(missing);
        return ok(sb.toString());
    }

    // ==================================================================
    //  edit
    // ==================================================================
    private ToolResult doEdit(JsonObject args, String root) {
        String rel = optString(args, "path");
        if (rel == null || rel.isEmpty()) return err("path is required.");
        if (!isSafeRelPath(rel)) return err("Invalid path '" + rel + "'.");
        String content = optString(args, "content");
        if (content == null) return err("content is required.");
        String abs = resolveUnderRoot(root, rel);
        if (abs == null) return err("Path escapes resource root: " + rel);
        if (!FileUtil.isExistFile(abs)) return err("File does not exist: " + rel + ". Use 'create_xml' to create.");
        FileUtil.writeFile(abs, content);
        return ok("Edited file '" + rel + "' (" + content.length() + " chars).");
    }

    // ==================================================================
    //  rename
    // ==================================================================
    private ToolResult doRename(JsonObject args, String root) {
        String oldRel = optString(args, "old_path");
        String newRel = optString(args, "new_path");
        if (oldRel == null || newRel == null) return err("old_path and new_path are required.");
        if (!isSafeRelPath(oldRel) || !isSafeRelPath(newRel)) {
            return err("Invalid path. Both must match " + SAFE_REL_PATH + ".");
        }
        String oldAbs = resolveUnderRoot(root, oldRel);
        String newAbs = resolveUnderRoot(root, newRel);
        if (oldAbs == null || newAbs == null) return err("Path escapes resource root.");
        if (!FileUtil.isExistFile(oldAbs)) return err("Source path does not exist: " + oldRel);
        if (FileUtil.isExistFile(newAbs)) return err("Target path already exists: " + newRel);
        boolean ok = FileUtil.renameFile(oldAbs, newAbs);
        if (!ok) return err("Rename failed (FileUtil.renameFile returned false).");
        return ok("Renamed '" + oldRel + "' -> '" + newRel + "'.");
    }

    // ==================================================================
    //  delete
    // ==================================================================
    private ToolResult doDelete(JsonObject args, String root) {
        String rel = optString(args, "path");
        if (rel == null || rel.isEmpty()) return err("path is required.");
        if (!isSafeRelPath(rel)) return err("Invalid path '" + rel + "'.");
        String abs = resolveUnderRoot(root, rel);
        if (abs == null) return err("Path escapes resource root: " + rel);
        if (!FileUtil.isExistFile(abs)) return err("Path does not exist: " + rel);
        FileUtil.deleteFile(abs);
        return ok("Deleted '" + rel + "' (" + (FileUtil.isDirectory(abs) ? "folder" : "file") + ").");
    }

    // ==================================================================
    //  list
    // ==================================================================
    private ToolResult doList(JsonObject args, String root) {
        String rel = optString(args, "path", "");
        String abs;
        if (rel.isEmpty()) {
            abs = root;
        } else {
            if (!isSafeRelPath(rel)) return err("Invalid path '" + rel + "'.");
            abs = resolveUnderRoot(root, rel);
            if (abs == null) return err("Path escapes resource root: " + rel);
        }
        if (!FileUtil.isExistFile(abs)) return err("Path does not exist: " + (rel.isEmpty() ? "<resource root>" : rel));
        if (!FileUtil.isDirectory(abs)) return err("Path is not a directory: " + rel);
        ArrayList<String> entries = new ArrayList<>();
        FileUtil.listDir(abs, entries);
        Collections.sort(entries, String.CASE_INSENSITIVE_ORDER);
        if (entries.isEmpty()) {
            return ok("Directory '" + (rel.isEmpty() ? "<resource root>" : rel) + "' is empty.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Contents of '").append(rel.isEmpty() ? "<resource root>" : rel)
          .append("' (").append(entries.size()).append(" entries):\n");
        for (int i = 0; i < entries.size(); i++) {
            String e = entries.get(i);
            String name = e.substring(e.lastIndexOf(File.separator) + 1);
            String type = FileUtil.isDirectory(e) ? "[DIR] " : "      ";
            sb.append("  ").append(type).append(name).append("\n");
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static boolean isSafeRelPath(String rel) {
        if (rel == null || rel.isEmpty()) return false;
        if (!SAFE_REL_PATH.matcher(rel).matches()) return false;
        // Disallow ".." segments.
        String[] parts = rel.split("/");
        for (String p : parts) {
            if ("..".equals(p)) return false;
        }
        return true;
    }

    /** Resolve a relative path under the root; return null if it escapes the root. */
    private static String resolveUnderRoot(String root, String rel) {
        try {
            File rootFile = new File(root).getCanonicalFile();
            File target = new File(rootFile, rel).getCanonicalFile();
            String targetPath = target.getAbsolutePath();
            String rootPath = rootFile.getAbsolutePath();
            if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
                return null;
            }
            return target.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }
}
