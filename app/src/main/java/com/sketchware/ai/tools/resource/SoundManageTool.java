package com.sketchware.ai.tools.resource;

import com.besome.sketch.beans.ProjectResourceBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * sound_manage — universal tool for managing project sound resources.
 *
 * <p>Wires the AI agent to Sketchware-Pro's sound management subsystem,
 * which is normally driven by
 * {@link com.besome.sketch.editor.manage.sound.ManageSoundActivity} +
 * {@link com.besome.sketch.editor.manage.sound.AddSoundActivity} +
 * {@link a.a.a.ow} (project sounds fragment).
 *
 * <p><b>Storage model</b>:
 * <ul>
 *   <li>Project sound list: {@code jC.d(scId).c} (field, an
 *       {@code ArrayList<ProjectResourceBean>}).</li>
 *   <li>Project sound directory: {@code jC.d(scId).o()} (a String path
 *       like {@code .sketchware/data/{scId}/files/sound/}).</li>
 *   <li>Persisting the list: {@code jC.d(scId).c(sounds)} +
 *       {@code jC.d(scId).y()} (see {@link a.a.a.ow#saveSounds()}).</li>
 *   <li>Global sound collection: {@code Qp.g()} singleton
 *       ({@code .f()} lists, {@code .a(scId, bean)} adds,
 *       {@code .a(name, false)} removes, {@code .d()} persists).</li>
 * </ul>
 *
 * <p>Actions (5):
 * <ul>
 *   <li><b>add</b> — add a sound to the project (params: {@code name},
 *       {@code file_path} OR {@code base64}, optional {@code add_to_collection}).</li>
 *   <li><b>edit</b> — rename an existing sound (params: {@code name},
 *       {@code new_name}).</li>
 *   <li><b>delete</b> — delete one or more sounds (params: {@code name}
 *       OR {@code names} array).</li>
 *   <li><b>list</b> — list all project sounds (read-only, auto-approved).</li>
 *   <li><b>import_from_collection</b> — import from global collection
 *       (params: {@code collection_names} array).</li>
 * </ul>
 *
 * <p>Sound files are typically {@code .mp3}, {@code .wav}, {@code .ogg}.
 * The tool preserves the source file extension when copying into the
 * project sound directory.
 */
public final class SoundManageTool extends UniversalTool {

    /** Sound name convention (matches Sketchware's WB/uq.b validator). */
    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** Standard base64 alphabet (with padding). */
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    public SoundManageTool() {
        super("sound_manage",
                "Manage project sound resources: add, edit, delete, list, "
                        + "import_from_collection. Sound names must match ^[a-z][a-z0-9_]*$.",
                "resource", false, false,
                "add", "edit", "delete", "list", "import_from_collection");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "name", "Sound resource name. Must match ^[a-z][a-z0-9_]*$.");
        addStringProp(props, "new_name", "(edit) New name to rename the sound to.");
        addStringProp(props, "base64", "Base64-encoded sound data (matching ^[A-Za-z0-9+/=]+$, length multiple of 4).");
        addStringProp(props, "file_path", "Absolute file path to an existing sound file (.mp3/.wav/.ogg).");
        addBoolProp(props, "add_to_collection", "(add) Also add to global sound collection (default false).");
        addArrayProp(props, "names", "(delete) Array of sound names to delete.");
        addArrayProp(props, "collection_names", "(import_from_collection) Array of collection sound names to import.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }
    private static void addBoolProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "boolean");
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
            case "add":                   return doAdd(ctx, scId, args);
            case "edit":                  return doEdit(ctx, scId, args);
            case "delete":                return doDelete(ctx, scId, args);
            case "list":                  return doList(ctx, scId);
            case "import_from_collection": return doImportFromCollection(ctx, scId, args);
            default:                      return err("Unknown action: " + action);
        }
    }

    // ==================================================================
    //  add
    // ==================================================================
    private ToolResult doAdd(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String base64 = optString(args, "base64");
        String filePath = optString(args, "file_path");
        String sourcePath;
        String ext = ".mp3";
        if (base64 != null && !base64.isEmpty()) {
            if (!looksLikeBase64(base64)) return err("base64 invalid (must match ^[A-Za-z0-9+/=]+$, length % 4).");
            byte[] raw;
            try { raw = Base64.getDecoder().decode(base64); }
            catch (IllegalArgumentException e) { return err("base64 decode failed: " + e.getMessage()); }
            sourcePath = writeTempBytes(scId, name, ext, raw);
            if (sourcePath == null) return err("Failed to write temp sound file.");
        } else if (filePath != null && !filePath.isEmpty()) {
            File f = new File(filePath);
            if (!f.exists() || !f.isFile()) return err("file_path '" + filePath + "' does not exist.");
            sourcePath = filePath;
            int dot = filePath.lastIndexOf('.');
            if (dot > 0) ext = filePath.substring(dot).toLowerCase();
        } else {
            return err("Either base64 or file_path is required.");
        }

        String soundDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            soundDir = (String) SketchwareApi.invoke(kc, "o");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        try { new File(soundDir).mkdirs(); } catch (Throwable ignored) {}

        ArrayList<ProjectResourceBean> sounds = loadProjectSounds(scId);
        if (findIndex(sounds, name) >= 0) {
            return err("Sound '" + name + "' already exists in project.");
        }
        String destPath = soundDir + File.separator + name + ext;
        try {
            pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        ProjectResourceBean bean = new ProjectResourceBean(
                ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, name + ext);
        bean.savedPos = 0;
        bean.isNew = false;
        sounds.add(bean);
        Throwable r = saveProjectSounds(scId, sounds);
        if (r != null) return ToolResult.error(r);

        boolean addToCollection = optBool(args, "add_to_collection", false);
        String note = "";
        if (addToCollection) {
            try {
                Object qp = SketchwareApi.invokeStatic("a.a.a.Qp", "g");
                SketchwareApi.invoke(qp, "a", scId, bean);
                SketchwareApi.invoke(qp, "d");
                note = " Also added to global sound collection.";
            } catch (Throwable t) {
                note = " (WARNING: failed to add to collection: " + t.getMessage() + ")";
            }
        }
        ctx.refreshViewEditor();
        long size = new File(destPath).length();
        return ok("Added sound '" + name + "' to project '" + scId + "' ("
                + size + " bytes, ext=" + ext + ")." + note);
    }

    // ==================================================================
    //  edit (rename only — sounds can't have their content "edited")
    // ==================================================================
    private ToolResult doEdit(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        String newName = optString(args, "new_name");
        if (newName == null || newName.isEmpty()) return err("new_name is required for sound edit.");
        if (!VALID_NAME.matcher(newName).matches()) {
            return err("Invalid new_name '" + newName + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        if (newName.equals(name)) return err("new_name is the same as name; nothing to do.");

        ArrayList<ProjectResourceBean> sounds = loadProjectSounds(scId);
        int idx = findIndex(sounds, name);
        if (idx < 0) return err("Sound '" + name + "' not found in project.");
        if (findIndex(sounds, newName) >= 0) {
            return err("Cannot rename: new name '" + newName + "' already exists.");
        }
        ProjectResourceBean bean = sounds.get(idx);

        String soundDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            soundDir = (String) SketchwareApi.invoke(kc, "o");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        String ext = ".mp3";
        if (bean.resFullName != null && bean.resFullName.contains(".")) {
            ext = bean.resFullName.substring(bean.resFullName.lastIndexOf('.')).toLowerCase();
        }
        String oldPath = soundDir + File.separator + name + ext;
        String newPath = soundDir + File.separator + newName + ext;
        try {
            pro.sketchware.utility.FileUtil.copyFile(oldPath, newPath);
            pro.sketchware.utility.FileUtil.deleteFile(oldPath);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        bean.resName = newName;
        bean.resFullName = newName + ext;
        bean.isEdited = true;
        sounds.set(idx, bean);

        Throwable r = saveProjectSounds(scId, sounds);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        return ok("Renamed sound '" + name + "' -> '" + newName + "'.");
    }

    // ==================================================================
    //  delete
    // ==================================================================
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

        ArrayList<ProjectResourceBean> sounds = loadProjectSounds(scId);
        String soundDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            soundDir = (String) SketchwareApi.invoke(kc, "o");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        List<String> deleted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String n : names) {
            int idx = findIndex(sounds, n);
            if (idx < 0) { missing.add(n); continue; }
            for (String ext : new String[]{".mp3", ".wav", ".ogg", ".m4a", ".aac", ".flac"}) {
                try { new File(soundDir + File.separator + n + ext).delete(); } catch (Throwable ignored) {}
            }
            sounds.remove(idx);
            deleted.add(n);
        }
        if (deleted.isEmpty()) {
            return err("No sounds deleted. Missing: " + missing);
        }
        Throwable r = saveProjectSounds(scId, sounds);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted ").append(deleted.size()).append(" sound(s): ").append(deleted);
        if (!missing.isEmpty()) sb.append(". Not found (skipped): ").append(missing);
        return ok(sb.toString());
    }

    // ==================================================================
    //  list
    // ==================================================================
    private ToolResult doList(SketchwareToolContext ctx, String scId) {
        ArrayList<ProjectResourceBean> sounds = loadProjectSounds(scId);
        if (sounds == null || sounds.isEmpty()) {
            return ok("No sounds in project '" + scId + "'.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Sounds in project '").append(scId).append("' (").append(sounds.size()).append("):\n");
        for (int i = 0; i < sounds.size(); i++) {
            ProjectResourceBean b = sounds.get(i);
            sb.append("  [").append(i).append("] name='").append(b.resName)
              .append("' file='").append(b.resFullName)
              .append("' duration=").append(b.totalSoundDuration).append("ms\n");
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  import_from_collection
    // ==================================================================
    private ToolResult doImportFromCollection(SketchwareToolContext ctx, String scId, JsonObject args) {
        if (!args.has("collection_names") || !args.get("collection_names").isJsonArray()) {
            return err("collection_names (array) is required.");
        }
        JsonArray arr = args.getAsJsonArray("collection_names");
        if (arr.size() == 0) return err("collection_names array is empty.");

        ArrayList<ProjectResourceBean> collection;
        try {
            Object qp = SketchwareApi.invokeStatic("a.a.a.Qp", "g");
            Object list = SketchwareApi.invoke(qp, "f");
            if (list instanceof ArrayList) {
                collection = (ArrayList<ProjectResourceBean>) list;
            } else {
                return err("Sound collection is empty or unreadable.");
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        ArrayList<ProjectResourceBean> toImport = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String name = arr.get(i).getAsString();
            ProjectResourceBean found = null;
            if (collection != null) {
                for (ProjectResourceBean b : collection) {
                    if (name.equals(b.resName)) { found = b; break; }
                }
            }
            if (found == null) missing.add(name);
            else toImport.add(found);
        }
        if (toImport.isEmpty()) {
            return err("None of the requested collection_names were found. Missing: " + missing);
        }

        ArrayList<ProjectResourceBean> sounds = loadProjectSounds(scId);
        String soundDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            soundDir = (String) SketchwareApi.invoke(kc, "o");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        try { new File(soundDir).mkdirs(); } catch (Throwable ignored) {}

        List<String> imported = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        for (ProjectResourceBean src : toImport) {
            if (findIndex(sounds, src.resName) >= 0) {
                duplicates.add(src.resName);
                continue;
            }
            String srcExt = ".mp3";
            if (src.resFullName != null && src.resFullName.contains(".")) {
                srcExt = src.resFullName.substring(src.resFullName.lastIndexOf('.')).toLowerCase();
            }
            String[] candidates = new String[]{
                    src.resFullName,
                    android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/.sketchware/collection/sound/" + src.resFullName,
                    android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/.sketchware/collection/sound/" + src.resName + srcExt,
            };
            String chosen = null;
            for (String c : candidates) {
                if (c != null && new File(c).exists()) { chosen = c; break; }
            }
            if (chosen == null) {
                missing.add(src.resName + " (collection file missing on disk)");
                continue;
            }
            String dest = soundDir + File.separator + src.resName + srcExt;
            try {
                pro.sketchware.utility.FileUtil.copyFile(chosen, dest);
            } catch (Throwable t) {
                missing.add(src.resName + " (copy failed: " + t.getMessage() + ")");
                continue;
            }
            ProjectResourceBean bean = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE, src.resName, src.resName + srcExt);
            bean.savedPos = 0;
            bean.isNew = false;
            sounds.add(bean);
            imported.add(src.resName);
        }
        if (imported.isEmpty()) {
            return err("Nothing imported. Duplicates: " + duplicates + ". Missing: " + missing);
        }
        Throwable r = saveProjectSounds(scId, sounds);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        StringBuilder sb = new StringBuilder();
        sb.append("Imported ").append(imported.size()).append(" sound(s) from collection: ").append(imported);
        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
        if (!missing.isEmpty()) sb.append(". Missing: ").append(missing);
        return ok(sb.toString());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    @SuppressWarnings("unchecked")
    private ArrayList<ProjectResourceBean> loadProjectSounds(String scId) {
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            java.lang.reflect.Field f = kc.getClass().getDeclaredField("c");
            f.setAccessible(true);
            Object v = f.get(kc);
            if (v instanceof ArrayList) {
                ArrayList<ProjectResourceBean> copy = new ArrayList<>();
                for (Object o : (ArrayList<?>) v) {
                    if (o instanceof ProjectResourceBean) copy.add((ProjectResourceBean) o);
                }
                return copy;
            }
        } catch (Throwable ignored) {}
        return new ArrayList<>();
    }

    /** Persist the project sound list: jC.d(scId).c(sounds) + jC.d(scId).y(). Returns null on success. */
    private Throwable saveProjectSounds(String scId, ArrayList<ProjectResourceBean> sounds) {
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(kc, "c", sounds);
            SketchwareApi.invoke(kc, "y");
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static int findIndex(ArrayList<ProjectResourceBean> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).resName)) return i;
        }
        return -1;
    }

    private static boolean looksLikeBase64(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!BASE64_PATTERN.matcher(s).matches()) return false;
        if (s.length() % 4 != 0) return false;
        return s.length() >= 8;
    }

    private static String writeTempBytes(String scId, String name, String ext, byte[] bytes) {
        try {
            String tmpDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/.sketchware/data/" + scId + "/import/";
            new File(tmpDir).mkdirs();
            String path = tmpDir + "sound_" + name + "_" + System.currentTimeMillis() + ext;
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) {
                fos.write(bytes);
            }
            return path;
        } catch (Throwable t) {
            return null;
        }
    }
}
