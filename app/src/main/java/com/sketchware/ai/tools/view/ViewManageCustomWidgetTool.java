package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * view_manage_custom_widget — universal tool for managing custom widget
 * definitions (saved widget trees that can be reused across layouts and
 * projects). Custom widgets compile to Java classes, so their names follow
 * the Java class convention.
 *
 * <p>Replaces 5 stubs: view_manage_custom_widget:{create, edit, delete,
 * export, import}.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates custom widget names against {@code ^[A-Z][A-Za-z0-9_]*$}
 *       (Java class convention — they're compiled to classes).</li>
 *   <li>For {@code create}: rejects if a widget with the same name already
 *       exists in the project.</li>
 *   <li>For {@code edit}: verifies the widget exists first (returns a
 *       helpful error listing available widgets if not).</li>
 *   <li>For {@code delete}: warns but allows if other layouts reference
 *       the widget (best-effort scan via reflection on jC.a(scId)).</li>
 *   <li>For {@code export}: validates that {@code file_path} ends with
 *       {@code .json} and reports the byte size of the exported file.</li>
 *   <li>For {@code import}: validates that the file exists, starts with
 *       {@code \{} (JSON check), and reports the imported byte size.</li>
 * </ul>
 */
public final class ViewManageCustomWidgetTool extends UniversalTool {

    /** Custom widget names compile to Java classes, so use Java class convention. */
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Z][A-Za-z0-9_]*$");

    /** Prefix used internally by Sketchware to namespace custom widget entries. */
    private static final String PREFIX = "_custom_widget:";

    public ViewManageCustomWidgetTool() {
        super("view_manage_custom_widget",
                "Manage custom widget definitions: create, edit, delete, export, or import. "
                        + "Custom widget names must match ^[A-Z][A-Za-z0-9_]*$ (Java class convention).",
                "view", false, false,
                "create", "edit", "delete", "export", "import");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pName = new JsonObject();
        pName.addProperty("type", "string");
        pName.addProperty("description", "Custom widget name. Must match ^[A-Z][A-Za-z0-9_]*$ (Java class convention — custom widgets are compiled to classes).");
        props.add("name", pName);

        JsonObject pDef = new JsonObject();
        pDef.addProperty("type", "string");
        pDef.addProperty("description", "(create/edit) JSON definition of the widget structure. Must be a valid JSON object.");
        props.add("definition", pDef);

        JsonObject pPath = new JsonObject();
        pPath.addProperty("type", "string");
        pPath.addProperty("description", "(export/import) Absolute file system path. For export, must end with '.json'. For import, the file must exist and contain valid JSON.");
        props.add("file_path", pPath);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "create":  return doCreate(ctx, scId, args);
            case "edit":    return doEdit(ctx, scId, args);
            case "delete":  return doDelete(ctx, scId, args);
            case "export":  return doExport(ctx, scId, args);
            case "import":  return doImport(ctx, scId, args);
            default:        return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  create
    // ------------------------------------------------------------------
    private ToolResult doCreate(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid custom widget name '" + name + "'. Must match ^[A-Z][A-Za-z0-9_]*$ "
                    + "(start with uppercase letter, alphanumerics and underscores only).");
        }
        String definition = optString(args, "definition", "{}");
        if (!looksLikeJson(definition)) {
            return err("definition must be a valid JSON object (must start with '{'). Got: '"
                    + truncate(definition, 80) + "'.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        if (customWidgetExists(editor, name)) {
            return err("A custom widget named '" + name + "' already exists in project '" + scId
                    + "'. Use view_manage_custom_widget:edit to modify it, "
                    + "or delete it first.");
        }
        try {
            SketchwareApi.invoke(editor, "a", PREFIX + name, definition);
            ctx.refreshViewEditor();
            return ok("Created custom widget '" + name + "' in project '" + scId + "'. "
                    + "Definition size: " + definition.length() + " bytes.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  edit
    // ------------------------------------------------------------------
    private ToolResult doEdit(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid custom widget name '" + name + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        }
        String definition = optString(args, "definition", "{}");
        if (!looksLikeJson(definition)) {
            return err("definition must be a valid JSON object (must start with '{'). Got: '"
                    + truncate(definition, 80) + "'.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        if (!customWidgetExists(editor, name)) {
            return err("Custom widget '" + name + "' not found in project '" + scId + "'. "
                    + "Use view_manage_custom_widget:create to create a new one. "
                    + "Existing custom widgets: " + listCustomWidgets(editor));
        }
        try {
            SketchwareApi.invoke(editor, "b", PREFIX + name, definition);
            ctx.refreshViewEditor();
            return ok("Edited custom widget '" + name + "' in project '" + scId + "'. "
                    + "New definition size: " + definition.length() + " bytes.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete
    // ------------------------------------------------------------------
    private ToolResult doDelete(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid custom widget name '" + name + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        if (!customWidgetExists(editor, name)) {
            return err("Custom widget '" + name + "' not found in project '" + scId
                    + "'. Existing custom widgets: " + listCustomWidgets(editor));
        }
        // Best-effort: scan layouts to see if any reference this widget.
        List<String> referencingLayouts = findLayoutsReferencingCustomWidget(scId, name);
        try {
            SketchwareApi.invoke(editor, "c", PREFIX + name);
            ctx.refreshViewEditor();
            StringBuilder msg = new StringBuilder();
            msg.append("Deleted custom widget '").append(name).append("' from project '").append(scId).append("'.");
            if (!referencingLayouts.isEmpty()) {
                msg.append("\nWARNING: The following layouts reference this custom widget ")
                   .append("and will now have dangling references: ")
                   .append(referencingLayouts)
                   .append(". Replace or remove them before building.");
            }
            return ok(msg.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  export
    // ------------------------------------------------------------------
    private ToolResult doExport(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "name");
        String path = optString(args, "file_path");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (path == null || path.isEmpty()) return err("file_path is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid custom widget name '" + name + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        }
        if (!path.endsWith(".json")) {
            return err("file_path must end with '.json'. Got: '" + path + "'.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        if (!customWidgetExists(editor, name)) {
            return err("Custom widget '" + name + "' not found in project '" + scId
                    + "'. Existing custom widgets: " + listCustomWidgets(editor));
        }
        try {
            Object def = SketchwareApi.invoke(editor, "d", PREFIX + name);
            String json = def == null ? "{}" : String.valueOf(def);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return err("Failed to create parent directory for '" + path + "'.");
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
            return ok("Exported custom widget '" + name + "' to " + path + " "
                    + "(" + bytes.length + " bytes).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  import
    // ------------------------------------------------------------------
    private ToolResult doImport(SketchwareToolContext ctx, String scId, JsonObject args) {
        String path = optString(args, "file_path");
        String name = optString(args, "name");
        if (path == null || path.isEmpty()) return err("file_path is required.");
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return err("File not found: '" + path + "'. Provide a valid absolute path to a JSON file.");
        }
        if (file.length() == 0) {
            return err("File is empty: '" + path + "'.");
        }
        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(file.toPath());
        } catch (Throwable t) {
            // Fallback: stream read.
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
                bytes = bos.toByteArray();
            } catch (Throwable t2) {
                return ToolResult.error(t2);
            }
        }
        String content = new String(bytes, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.charAt(0) != '{') {
            return err("File '" + path + "' does not appear to be valid JSON "
                    + "(content must start with '{'). First 80 chars: '"
                    + truncate(content, 80) + "'.");
        }
        if (name == null || name.isEmpty()) {
            // Derive from filename (strip .json extension).
            String fname = file.getName();
            int dot = fname.lastIndexOf('.');
            name = (dot > 0 ? fname.substring(0, dot) : fname);
        }
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Derived/imported name '" + name + "' is invalid. Must match ^[A-Z][A-Za-z0-9_]*$. "
                    + "Provide an explicit 'name' argument that satisfies the convention.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            if (customWidgetExists(editor, name)) {
                return err("A custom widget named '" + name + "' already exists. "
                        + "Delete it first or provide a different 'name' argument.");
            }
            SketchwareApi.invoke(editor, "a", PREFIX + name, content);
            ctx.refreshViewEditor();
            return ok("Imported custom widget '" + name + "' from " + path + " "
                    + "(" + bytes.length + " bytes read).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private static boolean looksLikeJson(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        return !trimmed.isEmpty() && trimmed.charAt(0) == '{';
    }

    private static boolean customWidgetExists(Object editor, String name) {
        if (editor == null) return false;
        try {
            Object def = SketchwareApi.invoke(editor, "d", PREFIX + name);
            return def != null && !def.toString().isEmpty();
        } catch (Throwable ignored) {}
        return false;
    }

    private static List<String> listCustomWidgets(Object editor) {
        List<String> names = new java.util.ArrayList<>();
        if (editor == null) return names;
        try {
            Object all = SketchwareApi.invoke(editor, "e");
            if (all instanceof List) {
                for (Object key : (List<?>) all) {
                    if (key == null) continue;
                    String s = key.toString();
                    if (s.startsWith(PREFIX)) names.add(s.substring(PREFIX.length()));
                }
            }
        } catch (Throwable ignored) {}
        return names;
    }

    /**
     * Best-effort scan: iterate all layouts in the project, return the names
     * of those whose widget tree contains a custom-widget reference matching
     * the given name. Returns an empty list if the scan fails (don't block
     * the delete on a reflection failure).
     */
    private static List<String> findLayoutsReferencingCustomWidget(String scId, String widgetName) {
        List<String> hits = new java.util.ArrayList<>();
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object layouts = SketchwareApi.invoke(editor, "e");
            if (layouts instanceof List) {
                for (Object layoutKey : (List<?>) layouts) {
                    if (layoutKey == null) continue;
                    String key = layoutKey.toString();
                    if (key.startsWith(PREFIX)) continue; // skip custom widget entries
                    try {
                        Object widgets = SketchwareApi.invoke(editor, "d", key);
                        if (widgets instanceof List) {
                            for (Object b : (List<?>) widgets) {
                                String type = readField(b, "type");
                                if (type != null && type.contains(widgetName)) {
                                    hits.add(key);
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return hits;
    }

    private static String readField(Object bean, String fieldName) {
        if (bean == null) return null;
        try {
            Object v = SketchwareApi.invoke(bean,
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        try {
            Class<?> cls = bean.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(bean);
                    return v == null ? null : v.toString();
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
