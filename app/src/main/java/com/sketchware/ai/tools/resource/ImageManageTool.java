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
 * image_manage — universal tool for managing project image resources.
 *
 * <p>This implementation wires up the AI agent to Sketchware-Pro's image
 * management subsystem, which is normally driven by
 * {@link com.besome.sketch.editor.manage.image.ManageImageActivity} +
 * {@link com.besome.sketch.editor.manage.image.AddImageActivity}.
 *
 * <p><b>Storage model</b>:
 * <ul>
 *   <li>Project image list: {@code jC.d(scId).b} (field, an
 *       {@code ArrayList<ProjectResourceBean>}).</li>
 *   <li>Project image directory: {@code jC.d(scId).l()} (a String path
 *       like {@code .sketchware/data/{scId}/files/image/}).</li>
 *   <li>Persisting the list: {@code jC.d(scId).b(images)} +
 *       {@code jC.d(scId).y()}.</li>
 *   <li>Global image collection: {@code Op.g()} singleton
 *       ({@code .f()} lists, {@code .a(scId, bean)} adds,
 *       {@code .a(name, false)} removes, {@code .d()} persists).</li>
 * </ul>
 *
 * <p>Actions (9):
 * <ul>
 *   <li><b>add</b> — add an image to the project (params: {@code name},
 *       {@code base64} OR {@code file_path}, optional
 *       {@code rotate_degrees}/{@code flip_h}/{@code flip_v}/{@code add_to_collection}).</li>
 *   <li><b>edit</b> — rename and/or replace content of an existing image
 *       (params: {@code name}, optional {@code new_name}, optional
 *       {@code base64} OR {@code file_path}).</li>
 *   <li><b>delete</b> — delete one or more images (params: {@code name}
 *       OR {@code names} array).</li>
 *   <li><b>list</b> — list all project images (read-only, auto-approved).</li>
 *   <li><b>rotate</b> — rotate image 90° clockwise (params: {@code name}).</li>
 *   <li><b>flip_horizontal</b> — flip horizontally (params: {@code name}).</li>
 *   <li><b>flip_vertical</b> — flip vertically (params: {@code name}).</li>
 *   <li><b>import_from_collection</b> — import from global collection
 *       (params: {@code collection_names} array).</li>
 *   <li><b>add_to_collection</b> — add to global collection (params:
 *       {@code name}, {@code base64} OR {@code file_path}).</li>
 * </ul>
 *
 * <p>For image transformations, we use {@code iB.a(srcPath, destPath,
 * rotate, flipH, flipV)} — the same helper used by Sketchware's own
 * image save flow — to physically rewrite the bitmap file on disk
 * so the change is immediately visible in the editor.
 */
public final class ImageManageTool extends UniversalTool {

    /** Resource name convention (matches Sketchware's PB/uq.b validator). */
    private static final Pattern VALID_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** Standard base64 alphabet (with padding). */
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    public ImageManageTool() {
        super("image_manage",
                "Manage project image resources: add, edit, delete, list, rotate, "
                        + "flip_horizontal, flip_vertical, import_from_collection, "
                        + "add_to_collection. Image names must match ^[a-z][a-z0-9_]*$.",
                "resource", false, false,
                "add", "edit", "delete", "list",
                "rotate", "flip_horizontal", "flip_vertical",
                "import_from_collection", "add_to_collection");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "name", "Image resource name. Must match ^[a-z][a-z0-9_]*$.");
        addStringProp(props, "new_name", "(edit) New name to rename the image to.");
        addStringProp(props, "base64", "Base64-encoded image data (matching ^[A-Za-z0-9+/=]+$, length multiple of 4).");
        addStringProp(props, "file_path", "Absolute file path to an existing image file on disk.");
        addIntProp(props, "rotate_degrees", "(add) Initial rotation in degrees (multiple of 90).");
        addBoolProp(props, "flip_h", "(add) Flip horizontally on add (default false).");
        addBoolProp(props, "flip_v", "(add) Flip vertically on add (default false).");
        addBoolProp(props, "add_to_collection", "(add) Also add to global image collection (default false).");
        addArrayProp(props, "names", "(delete) Array of image names to delete.");
        addArrayProp(props, "collection_names", "(import_from_collection) Array of collection image names to import.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }
    private static void addIntProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "integer");
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
            case "rotate":                return doTransform(ctx, scId, args, Transform.ROTATE);
            case "flip_horizontal":       return doTransform(ctx, scId, args, Transform.FLIP_H);
            case "flip_vertical":         return doTransform(ctx, scId, args, Transform.FLIP_V);
            case "import_from_collection": return doImportFromCollection(ctx, scId, args);
            case "add_to_collection":     return doAddToCollection(ctx, scId, args);
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

        byte[] rawBytes;
        String sourcePath;
        String ext = ".png";

        String base64 = optString(args, "base64");
        String filePath = optString(args, "file_path");
        if (base64 != null && !base64.isEmpty()) {
            if (!looksLikeBase64(base64)) {
                return err("base64 must match ^[A-Za-z0-9+/=]+$ and length be a multiple of 4.");
            }
            try {
                rawBytes = Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException e) {
                return err("base64 failed to decode: " + e.getMessage());
            }
            sourcePath = writeTempBytes(scId, name, ".png", rawBytes);
            if (sourcePath == null) return err("Failed to write temp image file.");
        } else if (filePath != null && !filePath.isEmpty()) {
            File f = new File(filePath);
            if (!f.exists() || !f.isFile()) {
                return err("file_path '" + filePath + "' does not exist.");
            }
            sourcePath = filePath;
            int dot = filePath.lastIndexOf('.');
            if (dot > 0) ext = filePath.substring(dot).toLowerCase();
            rawBytes = null;
        } else {
            return err("Either base64 or file_path is required.");
        }

        // Resolve project image directory.
        String imgDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            imgDir = (String) SketchwareApi.invoke(kc, "l");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        if (imgDir == null) return err("Could not resolve project image directory.");
        // Make sure it exists.
        try { new File(imgDir).mkdirs(); } catch (Throwable ignored) {}

        int rotate = clampRotation(optInt(args, "rotate_degrees", 0));
        int flipH = optBool(args, "flip_h", false) ? -1 : 1;
        int flipV = optBool(args, "flip_v", false) ? -1 : 1;

        // Load the current image list.
        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);

        // Reject duplicates.
        if (findIndex(images, name) >= 0) {
            return err("Image '" + name + "' already exists in project. Use 'edit' or pick a different name.");
        }

        // Physically copy/transform source into the project image dir.
        String destPath = imgDir + File.separator + name + ext;
        try {
            SketchwareApi.invokeStatic("a.a.a.iB",
                    "a", sourcePath, destPath, rotate, flipH, flipV);
        } catch (Throwable t) {
            // Fallback: raw copy (no transformation).
            try {
                pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
            } catch (Throwable t2) {
                return ToolResult.error(t2);
            }
        }

        // Append a ProjectResourceBean pointing at the saved file.
        ProjectResourceBean bean = new ProjectResourceBean(
                ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, name + ext);
        bean.savedPos = 0;
        bean.isNew = false;
        bean.rotate = rotate;
        bean.flipHorizontal = flipH;
        bean.flipVertical = flipV;
        images.add(bean);

        // Persist.
        Throwable saveErr = saveProjectImages(scId, images);
        if (saveErr != null) return ToolResult.error(saveErr);

        // Optionally also add to global collection.
        boolean addToCollection = optBool(args, "add_to_collection", false);
        String collectionNote = "";
        if (addToCollection) {
            try {
                Object op = SketchwareApi.invokeStatic("a.a.a.Op", "g");
                SketchwareApi.invoke(op, "a", scId, bean);
                SketchwareApi.invoke(op, "d");
                collectionNote = " Also added to global image collection.";
            } catch (Throwable t) {
                collectionNote = " (WARNING: failed to add to collection: " + t.getMessage() + ")";
            }
        }

        ctx.refreshViewEditor();
        long size = new File(destPath).length();
        return ok("Added image '" + name + "' to project '" + scId + "' ("
                + size + " bytes, ext=" + ext + ", rotate=" + rotate
                + ", flipH=" + flipH + ", flipV=" + flipV + ")." + collectionNote);
    }

    // ==================================================================
    //  edit
    // ==================================================================
    private ToolResult doEdit(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        String newName = optString(args, "new_name");
        if (newName != null && !newName.isEmpty() && !VALID_NAME.matcher(newName).matches()) {
            return err("Invalid new_name '" + newName + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String base64 = optString(args, "base64");
        String filePath = optString(args, "file_path");

        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);
        int idx = findIndex(images, name);
        if (idx < 0) return err("Image '" + name + "' not found in project.");

        ProjectResourceBean bean = images.get(idx);
        String imgDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            imgDir = (String) SketchwareApi.invoke(kc, "l");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        // Determine current extension and on-disk file.
        String currentExt = ".png";
        if (bean.resFullName != null && bean.resFullName.contains(".")) {
            currentExt = bean.resFullName.substring(bean.resFullName.lastIndexOf('.')).toLowerCase();
        }
        String oldPath = imgDir + File.separator + name + currentExt;

        // Replace content if requested.
        String replacedNote = "";
        if (base64 != null && !base64.isEmpty() || filePath != null && !filePath.isEmpty()) {
            String sourcePath;
            String newExt = currentExt;
            if (base64 != null && !base64.isEmpty()) {
                if (!looksLikeBase64(base64)) return err("base64 invalid (must match ^[A-Za-z0-9+/=]+$, length % 4).");
                byte[] raw;
                try { raw = Base64.getDecoder().decode(base64); }
                catch (IllegalArgumentException e) { return err("base64 decode failed: " + e.getMessage()); }
                sourcePath = writeTempBytes(scId, name, currentExt, raw);
                if (sourcePath == null) return err("Failed to write temp file.");
            } else {
                File f = new File(filePath);
                if (!f.exists() || !f.isFile()) return err("file_path '" + filePath + "' does not exist.");
                sourcePath = filePath;
                int dot = filePath.lastIndexOf('.');
                if (dot > 0) newExt = filePath.substring(dot).toLowerCase();
            }
            String oldExtForReplace = currentExt;
            // Apply same rotate/flip as currently set so the on-disk file is the canonical version.
            String destPath = imgDir + File.separator + name + newExt;
            try {
                SketchwareApi.invokeStatic("a.a.a.iB",
                        "a", sourcePath, destPath, bean.rotate, bean.flipHorizontal, bean.flipVertical);
                currentExt = newExt;
                replacedNote = " Replaced image content (" + new File(destPath).length() + " bytes).";
                // If extension changed, attempt to delete the old file (best-effort).
                if (!newExt.equals(oldExtForReplace)) {
                    try { new File(oldPath).delete(); } catch (Throwable ignored) {}
                }
            } catch (Throwable t) {
                try {
                    pro.sketchware.utility.FileUtil.copyFile(sourcePath, destPath);
                    currentExt = newExt;
                    replacedNote = " Replaced image content (raw copy, " + new File(destPath).length() + " bytes).";
                    if (!newExt.equals(oldExtForReplace)) {
                        try { new File(oldPath).delete(); } catch (Throwable ignored) {}
                    }
                } catch (Throwable t2) {
                    return ToolResult.error(t2);
                }
            }
        }

        // Rename if requested.
        String renameNote = "";
        if (newName != null && !newName.isEmpty() && !newName.equals(name)) {
            if (findIndex(images, newName) >= 0) {
                return err("Cannot rename: new name '" + newName + "' already exists.");
            }
            // Move the file on disk.
            String oldPath2 = imgDir + File.separator + name + currentExt;
            String newPath2 = imgDir + File.separator + newName + currentExt;
            try {
                pro.sketchware.utility.FileUtil.copyFile(oldPath2, newPath2);
                pro.sketchware.utility.FileUtil.deleteFile(oldPath2);
            } catch (Throwable t) {
                return ToolResult.error(t);
            }
            bean.resName = newName;
            bean.resFullName = newName + currentExt;
            renameNote = " Renamed '" + name + "' -> '" + newName + "'.";
        } else {
            bean.resFullName = name + currentExt;
        }
        bean.isEdited = true;
        images.set(idx, bean);

        Throwable r = saveProjectImages(scId, images);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        return ok("Edited image '" + name + "'." + renameNote + replacedNote);
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

        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);
        String imgDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            imgDir = (String) SketchwareApi.invoke(kc, "l");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        List<String> deleted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String n : names) {
            int idx = findIndex(images, n);
            if (idx < 0) { missing.add(n); continue; }
            // Try to delete the on-disk file (best-effort, multiple extensions).
            for (String ext : new String[]{".png", ".9.png", ".xml", ".jpg", ".jpeg", ".webp", ".gif"}) {
                try { new File(imgDir + File.separator + n + ext).delete(); } catch (Throwable ignored) {}
            }
            images.remove(idx);
            deleted.add(n);
        }
        if (deleted.isEmpty()) {
            return err("No images deleted. Missing: " + missing);
        }
        Throwable r = saveProjectImages(scId, images);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        StringBuilder sb = new StringBuilder();
        sb.append("Deleted ").append(deleted.size()).append(" image(s): ").append(deleted);
        if (!missing.isEmpty()) {
            sb.append(". Not found (skipped): ").append(missing);
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  list
    // ==================================================================
    private ToolResult doList(SketchwareToolContext ctx, String scId) {
        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);
        if (images == null || images.isEmpty()) {
            return ok("No images in project '" + scId + "'.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Images in project '").append(scId).append("' (").append(images.size()).append("):\n");
        for (int i = 0; i < images.size(); i++) {
            ProjectResourceBean b = images.get(i);
            sb.append("  [").append(i).append("] name='").append(b.resName)
              .append("' file='").append(b.resFullName)
              .append("' rotate=").append(b.rotate)
              .append(" flipH=").append(b.flipHorizontal)
              .append(" flipV=").append(b.flipVertical).append("\n");
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  rotate / flip_horizontal / flip_vertical
    // ==================================================================
    private enum Transform { ROTATE, FLIP_H, FLIP_V }

    private ToolResult doTransform(SketchwareToolContext ctx, String scId, JsonObject args, Transform t) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);
        int idx = findIndex(images, name);
        if (idx < 0) return err("Image '" + name + "' not found in project.");
        ProjectResourceBean bean = images.get(idx);

        // Current extension and file path.
        String ext = ".png";
        if (bean.resFullName != null && bean.resFullName.contains(".")) {
            ext = bean.resFullName.substring(bean.resFullName.lastIndexOf('.')).toLowerCase();
        }
        String imgDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            imgDir = (String) SketchwareApi.invoke(kc, "l");
        } catch (Throwable ex) {
            return ToolResult.error(ex);
        }
        String srcPath = imgDir + File.separator + name + ext;
        if (!new File(srcPath).exists()) {
            return err("Image file not found on disk: " + srcPath);
        }

        // Compute new transformation. Mirror AddImageActivity's logic:
        //   - flipHorizontal/flipVertical are sign-multiplied.
        //   - rotate is accumulated modulo 360.
        // For rotate 90/270, swap which axis a flip affects.
        switch (t) {
            case ROTATE:
                bean.rotate = (bean.rotate + 90) % 360;
                break;
            case FLIP_H:
                if (bean.rotate == 90 || bean.rotate == 270) {
                    bean.flipVertical *= -1;
                } else {
                    bean.flipHorizontal *= -1;
                }
                break;
            case FLIP_V:
                if (bean.rotate == 90 || bean.rotate == 270) {
                    bean.flipHorizontal *= -1;
                } else {
                    bean.flipVertical *= -1;
                }
                break;
        }

        // Apply transformation in place: read src, write to a temp path, then replace src.
        String tmpPath = srcPath + ".tmp";
        try {
            SketchwareApi.invokeStatic("a.a.a.iB",
                    "a", srcPath, tmpPath, bean.rotate, bean.flipHorizontal, bean.flipVertical);
        } catch (Throwable ex) {
            return ToolResult.error(ex);
        }
        try {
            pro.sketchware.utility.FileUtil.deleteFile(srcPath);
            pro.sketchware.utility.FileUtil.copyFile(tmpPath, srcPath);
            pro.sketchware.utility.FileUtil.deleteFile(tmpPath);
        } catch (Throwable t2) {
            // FIX-D-PROJECT: renamed catch var from `t` to `t2` to avoid shadowing
            // the method-parameter `Transform t` (was a compile-blocking error).
            return ToolResult.error(t2);
        }

        bean.isEdited = true;
        images.set(idx, bean);
        Throwable r = saveProjectImages(scId, images);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        String opName = t == Transform.ROTATE ? "rotated 90°"
                : t == Transform.FLIP_H ? "flipped horizontally"
                : "flipped vertically";
        return ok("Image '" + name + "' " + opName + ". New state: rotate="
                + bean.rotate + " flipH=" + bean.flipHorizontal
                + " flipV=" + bean.flipVertical + ".");
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

        // Read collection list.
        ArrayList<ProjectResourceBean> collection;
        try {
            Object op = SketchwareApi.invokeStatic("a.a.a.Op", "g");
            Object list = SketchwareApi.invoke(op, "f");
            if (list instanceof ArrayList) {
                collection = (ArrayList<ProjectResourceBean>) list;
            } else {
                return err("Collection list is empty or unreadable.");
            }
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        // For each requested name, look it up in the collection.
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
            if (found == null) {
                missing.add(name);
            } else {
                toImport.add(found);
            }
        }
        if (toImport.isEmpty()) {
            return err("None of the requested collection_names were found. Missing: " + missing);
        }

        // Add to project list — copy file into image dir for each.
        ArrayList<ProjectResourceBean> images = loadProjectImages(scId);
        String imgDir;
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            imgDir = (String) SketchwareApi.invoke(kc, "l");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        try { new File(imgDir).mkdirs(); } catch (Throwable ignored) {}

        List<String> imported = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        for (ProjectResourceBean src : toImport) {
            if (findIndex(images, src.resName) >= 0) {
                duplicates.add(src.resName);
                continue;
            }
            // Determine source file path: collection bean's resFullName is
            // typically a relative name in the global collection dir; the
            // collection dir is rooted at .sketchware/collection/image/.
            String srcExt = ".png";
            if (src.resFullName != null && src.resFullName.contains(".")) {
                srcExt = src.resFullName.substring(src.resFullName.lastIndexOf('.')).toLowerCase();
            }
            // Try a few candidate source paths.
            String[] candidates = new String[]{
                    src.resFullName,
                    android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/.sketchware/collection/image/" + src.resFullName,
                    android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/.sketchware/collection/image/" + src.resName + srcExt,
            };
            String chosen = null;
            for (String c : candidates) {
                if (c != null && new File(c).exists()) { chosen = c; break; }
            }
            if (chosen == null) {
                missing.add(src.resName + " (collection file missing on disk)");
                continue;
            }
            String dest = imgDir + File.separator + src.resName + srcExt;
            try {
                SketchwareApi.invokeStatic("a.a.a.iB",
                        "a", chosen, dest, src.rotate, src.flipHorizontal, src.flipVertical);
            } catch (Throwable t) {
                try {
                    pro.sketchware.utility.FileUtil.copyFile(chosen, dest);
                } catch (Throwable t2) {
                    missing.add(src.resName + " (copy failed: " + t2.getMessage() + ")");
                    continue;
                }
            }
            ProjectResourceBean bean = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE, src.resName, src.resName + srcExt);
            bean.savedPos = 0;
            bean.isNew = false;
            bean.rotate = src.rotate;
            bean.flipHorizontal = src.flipHorizontal;
            bean.flipVertical = src.flipVertical;
            images.add(bean);
            imported.add(src.resName);
        }
        if (imported.isEmpty()) {
            return err("Nothing imported. Duplicates: " + duplicates + ". Missing: " + missing);
        }
        Throwable r = saveProjectImages(scId, images);
        if (r != null) return ToolResult.error(r);
        ctx.refreshViewEditor();
        StringBuilder sb = new StringBuilder();
        sb.append("Imported ").append(imported.size()).append(" image(s) from collection: ").append(imported);
        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
        if (!missing.isEmpty()) sb.append(". Missing: ").append(missing);
        return ok(sb.toString());
    }

    // ==================================================================
    //  add_to_collection
    // ==================================================================
    private ToolResult doAddToCollection(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        String base64 = optString(args, "base64");
        String filePath = optString(args, "file_path");
        String sourcePath;
        String ext = ".png";
        byte[] rawBytes;
        if (base64 != null && !base64.isEmpty()) {
            if (!looksLikeBase64(base64)) return err("base64 invalid (must match ^[A-Za-z0-9+/=]+$, length % 4).");
            try { rawBytes = Base64.getDecoder().decode(base64); }
            catch (IllegalArgumentException e) { return err("base64 decode failed: " + e.getMessage()); }
            sourcePath = writeTempBytes(scId, name, ".png", rawBytes);
            if (sourcePath == null) return err("Failed to write temp image file.");
        } else if (filePath != null && !filePath.isEmpty()) {
            File f = new File(filePath);
            if (!f.exists() || !f.isFile()) return err("file_path '" + filePath + "' does not exist.");
            sourcePath = filePath;
            int dot = filePath.lastIndexOf('.');
            if (dot > 0) ext = filePath.substring(dot).toLowerCase();
            rawBytes = null;
        } else {
            return err("Either base64 or file_path is required.");
        }

        try {
            Object op = SketchwareApi.invokeStatic("a.a.a.Op", "g");
            ProjectResourceBean bean = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, sourcePath);
            bean.savedPos = 1;
            bean.isNew = true;
            SketchwareApi.invoke(op, "a", scId, bean);
            SketchwareApi.invoke(op, "d");
            long size = rawBytes != null ? rawBytes.length : new File(sourcePath).length();
            return ok("Added image '" + name + "' to global image collection ("
                    + size + " bytes, ext=" + ext + ").");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Read the project image list from jC.d(scId).b (field). */
    @SuppressWarnings("unchecked")
    private ArrayList<ProjectResourceBean> loadProjectImages(String scId) {
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            java.lang.reflect.Field f = kc.getClass().getDeclaredField("b");
            f.setAccessible(true);
            Object v = f.get(kc);
            if (v instanceof ArrayList) {
                // Defensive copy so we can mutate freely.
                ArrayList<ProjectResourceBean> copy = new ArrayList<>();
                for (Object o : (ArrayList<?>) v) {
                    if (o instanceof ProjectResourceBean) copy.add((ProjectResourceBean) o);
                }
                return copy;
            }
        } catch (Throwable ignored) {}
        return new ArrayList<>();
    }

    /** Persist the project image list: jC.d(scId).b(images) + jC.d(scId).y(). Returns null on success. */
    private Throwable saveProjectImages(String scId, ArrayList<ProjectResourceBean> images) {
        try {
            Object kc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(kc, "b", images);
            SketchwareApi.invoke(kc, "y");
            return null; // success
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

    private static int clampRotation(int deg) {
        int r = deg % 360;
        if (r < 0) r += 360;
        return r;
    }

    private static boolean looksLikeBase64(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!BASE64_PATTERN.matcher(s).matches()) return false;
        if (s.length() % 4 != 0) return false;
        return s.length() >= 8;
    }

    /** Write raw bytes to a temp file under the project's import dir, return the path. */
    private static String writeTempBytes(String scId, String name, String ext, byte[] bytes) {
        try {
            String tmpDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/.sketchware/data/" + scId + "/import/";
            new File(tmpDir).mkdirs();
            String path = tmpDir + "img_" + name + "_" + System.currentTimeMillis() + ext;
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) {
                fos.write(bytes);
            }
            return path;
        } catch (Throwable t) {
            return null;
        }
    }
}
