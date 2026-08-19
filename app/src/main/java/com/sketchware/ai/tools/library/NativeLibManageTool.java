package com.sketchware.ai.tools.library;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pro.sketchware.utility.FileUtil;

/**
 * native_lib_manage — universal tool for managing native libraries
 * ({@code .so} files) per project.
 *
 * <p><b>FIX-D-PROJECT (Task D7):</b> new tool. Closes coverage-report
 * gap §1.5 — the {@code ManageNativelibsActivity} UI exposes 5
 * operations (create_folder, import_so, rename, delete, list) but
 * none of them were reachable from the AI layer.
 *
 * <p>Storage convention (per {@code FilePathUtil#getPathNativelibs}):
 * <pre>
 *   .sketchware/data/{sc_id}/files/native_libs/
 *     armeabi/
 *     armeabi-v7a/
 *     arm64-v8a/
 *     x86/
 *     x86_64/
 *       libfoo.so
 *       libbar.so
 * </pre>
 *
 * <p>Only standard Android ABI folder names are accepted as
 * {@code abi_name}. The {@code list} action is read-only.
 */
public final class NativeLibManageTool extends UniversalTool {

    /** Standard Android ABI names accepted as {@code abi_name}. */
    private static final Set<String> VALID_ABIS = new HashSet<>(Arrays.asList(
            "armeabi", "armeabi-v7a", "arm64-v8a", "x86", "x86_64", "mips", "mips64"
    ));

    /** ABI names auto-created by Sketchware's {@code ManageNativelibsActivity.checkDir()}. */
    private static final Set<String> DEFAULT_ABIS = new HashSet<>(Arrays.asList(
            "armeabi", "armeabi-v7a", "arm64-v8a", "x86"
    ));

    public NativeLibManageTool() {
        super("native_lib_manage",
                "Manage native libraries (.so) for the current project: "
                        + "create_folder (creates an ABI subfolder), import_so (copies "
                        + "one or more .so files into an ABI folder), rename, delete, "
                        + "list (read-only). Default ABI folders are armeabi, armeabi-v7a, "
                        + "arm64-v8a, x86; x86_64 and mips/mips64 are also accepted.",
                "library", false, false,
                "create_folder",
                "import_so",
                "rename",
                "delete",
                "list");
    }

    @Override
    protected void addExtraProperties(JsonObject props) {
        JsonObject p;

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(create_folder / import_so / list) ABI name: armeabi / armeabi-v7a / arm64-v8a / x86 / x86_64 / mips / mips64.");
        props.add("abi_name", p);

        p = new JsonObject();
        p.addProperty("type", "array");
        p.addProperty("description", "(import_so) Array of absolute paths to .so files to copy into the target ABI folder.");
        JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        p.add("items", items);
        props.add("file_paths", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(rename / delete) Absolute path of the file or folder to rename/delete. Use list first to discover paths.");
        props.add("path", p);

        p = new JsonObject();
        p.addProperty("type", "string");
        p.addProperty("description", "(rename) New name (basename only — the parent directory is preserved).");
        props.add("new_path", p);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "create_folder": return doCreateFolder(scId, args);
            case "import_so":     return doImportSo(scId, args);
            case "rename":        return doRename(scId, args);
            case "delete":        return doDelete(scId, args);
            case "list":          return doList(scId, args);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private static String nativeLibsRoot(String scId) {
        return FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/files/native_libs";
    }

    private static ToolResult validateAbi(String abi) {
        if (abi == null || abi.isEmpty()) {
            return ToolResult.error("[native_lib_manage] abi_name is required.");
        }
        if (!VALID_ABIS.contains(abi)) {
            return ToolResult.error("[native_lib_manage] Invalid abi_name '" + abi
                    + "'. Must be one of: " + VALID_ABIS);
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  create_folder
    // ------------------------------------------------------------------
    private ToolResult doCreateFolder(String scId, JsonObject args) {
        String abi = optString(args, "abi_name");
        ToolResult v = validateAbi(abi);
        if (v != null) return v;

        String path = nativeLibsRoot(scId) + "/" + abi;
        if (FileUtil.isExistFile(path)) {
            return ok("ABI folder already exists: " + path);
        }
        try {
            // Ensure the parent native_libs dir exists.
            if (!FileUtil.isExistFile(nativeLibsRoot(scId))) {
                FileUtil.makeDir(nativeLibsRoot(scId));
                // Auto-create the default Sketchware ABIs alongside the requested one
                // (matches ManageNativelibsActivity.checkDir() behaviour).
                for (String d : DEFAULT_ABIS) {
                    if (!d.equals(abi)) FileUtil.makeDir(nativeLibsRoot(scId) + "/" + d);
                }
            }
            FileUtil.makeDir(path);
            return ok("Created ABI folder: " + path);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  import_so
    // ------------------------------------------------------------------
    private ToolResult doImportSo(String scId, JsonObject args) {
        String abi = optString(args, "abi_name");
        ToolResult v = validateAbi(abi);
        if (v != null) return v;

        List<String> paths = new ArrayList<>();
        if (args.has("file_paths") && !args.get("file_paths").isJsonNull()
                && args.get("file_paths").isJsonArray()) {
            for (var e : args.get("file_paths").getAsJsonArray()) {
                if (!e.isJsonNull()) paths.add(e.getAsString());
            }
        }
        if (paths.isEmpty()) {
            return err("file_paths[] is required (one or more .so file paths).");
        }

        String targetDir = nativeLibsRoot(scId) + "/" + abi;
        if (!FileUtil.isExistFile(targetDir)) {
            FileUtil.makeDir(targetDir);
        }

        int imported = 0;
        StringBuilder errors = new StringBuilder();
        for (String src : paths) {
            if (src == null || src.isEmpty()) continue;
            if (!src.toLowerCase().endsWith(".so")) {
                errors.append("Skipped '").append(src).append("' — not a .so file.\n");
                continue;
            }
            if (!FileUtil.isExistFile(src)) {
                errors.append("Skipped '").append(src).append("' — file not found.\n");
                continue;
            }
            String name = new File(src).getName();
            String dest = targetDir + File.separator + name;
            try {
                FileUtil.copyDirectory(new File(src), new File(dest));
                imported++;
            } catch (IOException e) {
                errors.append("Failed to copy '").append(src).append("': ")
                        .append(e.getMessage()).append("\n");
            }
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Imported ").append(imported).append(" .so file(s) into ").append(targetDir).append(".");
        if (errors.length() > 0) {
            msg.append("\nWarnings:\n").append(errors);
        }
        return ok(msg.toString());
    }

    // ------------------------------------------------------------------
    //  rename
    // ------------------------------------------------------------------
    private ToolResult doRename(String scId, JsonObject args) {
        String path = optString(args, "path");
        String newName = optString(args, "new_path");
        if (path == null || path.isEmpty()) return err("path is required.");
        if (newName == null || newName.isEmpty()) return err("new_path is required (the new basename).");
        // Defensive: don't allow the LLM to escape the project's native_libs dir.
        String root = nativeLibsRoot(scId);
        if (!path.startsWith(root)) {
            return err("path must be inside the project's native_libs directory: " + root);
        }
        if (!FileUtil.isExistFile(path)) {
            return err("Path does not exist: " + path);
        }
        // Strip any path separators from newName (must be a basename).
        String cleanName = newName.replace("/", "_").replace("\\", "_");
        String parent = new File(path).getParent();
        String dest = parent + File.separator + cleanName;
        try {
            boolean ok = FileUtil.renameFile(path, dest);
            if (!ok) return err("Rename failed (FileUtil.renameFile returned false).");
            return ok("Renamed: " + path + " → " + dest);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete
    // ------------------------------------------------------------------
    private ToolResult doDelete(String scId, JsonObject args) {
        String path = optString(args, "path");
        if (path == null || path.isEmpty()) return err("path is required.");
        String root = nativeLibsRoot(scId);
        if (!path.startsWith(root)) {
            return err("path must be inside the project's native_libs directory: " + root);
        }
        // Don't allow deleting the native_libs root itself.
        if (path.equals(root)) {
            return err("Refusing to delete the native_libs root directory. "
                    + "Use list to inspect contents and delete specific ABI folders or .so files.");
        }
        if (!FileUtil.isExistFile(path)) {
            return ok("Path does not exist (already deleted?): " + path);
        }
        try {
            FileUtil.deleteFile(path);
            return ok("Deleted: " + path);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  list — read-only
    // ------------------------------------------------------------------
    private ToolResult doList(String scId, JsonObject args) {
        String abi = optString(args, "abi_name");
        String root = nativeLibsRoot(scId);
        if (!FileUtil.isExistFile(root)) {
            return ok("No native_libs directory exists yet for project '" + scId + "'. "
                    + "Expected at: " + root + ". "
                    + "Use create_folder to create one.");
        }

        if (abi != null && !abi.isEmpty()) {
            ToolResult v = validateAbi(abi);
            if (v != null) return v;
            String abiDir = root + "/" + abi;
            if (!FileUtil.isExistFile(abiDir)) {
                return ok("ABI folder '" + abi + "' does not exist. Path: " + abiDir);
            }
            return ok("Contents of " + abiDir + ":\n" + listDir(abiDir));
        }

        // List all ABIs.
        StringBuilder out = new StringBuilder();
        out.append("Native libraries tree for project '").append(scId).append("':\n");
        out.append("Root: ").append(root).append("\n\n");
        File rootDir = new File(root);
        File[] abis = rootDir.listFiles();
        if (abis == null || abis.length == 0) {
            out.append("(no ABI folders yet — use create_folder to add one)");
        } else {
            List<File> sorted = new ArrayList<>(Arrays.asList(abis));
            sorted.sort(java.util.Comparator.comparing(File::getName));
            for (File f : sorted) {
                if (!f.isDirectory()) continue;
                out.append("[").append(f.getName()).append("]\n");
                out.append(listDir(f.getAbsolutePath())).append("\n");
            }
        }
        return ok(out.toString());
    }

    private static String listDir(String dir) {
        File d = new File(dir);
        File[] files = d.listFiles();
        if (files == null || files.length == 0) {
            return "  (empty)\n";
        }
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        sorted.sort(java.util.Comparator.comparing(File::getName));
        StringBuilder sb = new StringBuilder();
        for (File f : sorted) {
            sb.append("  ").append(f.getName());
            if (f.isDirectory()) sb.append("/");
            else sb.append("  (").append(f.length()).append(" bytes)");
            sb.append("\n");
        }
        return sb.toString();
    }
}
