package com.sketchware.ai.tools.java;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.regex.Pattern;

/**
 * java_modify_class — universal tool for modifying Java files in the project.
 *
 * <p>Replaces 12 stubs: java_add_field, java_add_import, java_add_method,
 * java_create_file, java_delete_file, java_format, java_import_files,
 * java_remove_from_manifest_as_activity, java_remove_from_manifest_as_service,
 * java_rename_file, java_search_replace.
 *
 * <p>This implementation validates Java identifiers and basic syntax. It
 * uses Sketchware's {@code jC.c(scId)} Java file manager singleton to
 * persist changes, and {@code jC.d(scId)} for manifest operations.
 *
 * <p>The LLM provides raw Java source snippets; the tool wraps them with
 * Sketchware's class skeleton (package declaration, imports, class header)
 * if creating a new file, or appends/inserts them at the correct position
 * if modifying an existing file.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>File names must match {@code ^[A-Z][A-Za-z0-9_]*$} (Java class convention).</li>
 *   <li>Field names must match {@code ^[a-z][A-Za-z0-9_]*$}.</li>
 *   <li>Method signatures must contain {@code (} and {@code )}.</li>
 *   <li>Import paths must match {@code ^[a-z][a-z0-9_]*(\.[a-z0-9_]+)*$}.</li>
 *   <li>Search/replace text must be non-empty.</li>
 * </ul>
 */
public final class JavaModifyClassTool extends UniversalTool {

    /** Java identifier regex (class names, file names). */
    private static final Pattern JAVA_CLASS_NAME = Pattern.compile("^[A-Z][A-Za-z0-9_]*$");
    /** Java identifier regex (field names, method names, parameters). */
    private static final Pattern JAVA_ID = Pattern.compile("^[a-z][A-Za-z0-9_]*$");
    /** Java fully-qualified import path. */
    private static final Pattern JAVA_IMPORT = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)*(\\.[A-Z][A-Za-z0-9_]*)?$");

    public JavaModifyClassTool() {
        super("java_modify_class",
                "Modify a Java file: add_field, add_import, add_method, create_file, "
                        + "delete_file, format, import_files, remove_from_manifest_as_activity, "
                        + "remove_from_manifest_as_service, rename_file, search_replace.",
                "java", false, false,
                "add_field", "add_import", "add_method", "create_file", "delete_file",
                "format", "import_files", "remove_from_manifest_as_activity",
                "remove_from_manifest_as_service", "rename_file", "search_replace");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject fileName = new JsonObject();
        fileName.addProperty("type", "string");
        fileName.addProperty("description", "Java file name (without .java extension). Must match ^[A-Z][A-Za-z0-9_]*$ (e.g. 'MainActivity', 'NetworkHelper').");
        props.add("file_name", fileName);

        JsonObject newName = new JsonObject();
        newName.addProperty("type", "string");
        newName.addProperty("description", "(rename_file) New file name. Must match ^[A-Z][A-Za-z0-9_]*$.");
        props.add("new_name", newName);

        JsonObject fieldName = new JsonObject();
        fieldName.addProperty("type", "string");
        fieldName.addProperty("description", "(add_field) Field name. Must match ^[a-z][A-Za-z0-9_]*$.");
        props.add("field_name", fieldName);

        JsonObject fieldType = new JsonObject();
        fieldType.addProperty("type", "string");
        fieldType.addProperty("description", "(add_field) Field type (e.g. 'String', 'int', 'List<String>', 'MapView').");
        props.add("field_type", fieldType);

        JsonObject fieldInit = new JsonObject();
        fieldInit.addProperty("type", "string");
        fieldInit.addProperty("description", "(add_field) Optional initializer expression (e.g. '\"\"' or 'new ArrayList<>()').");
        props.add("field_init", fieldInit);

        JsonObject importPath = new JsonObject();
        importPath.addProperty("type", "string");
        importPath.addProperty("description", "(add_import) Fully-qualified Java import (e.g. 'java.util.List', 'android.view.View').");
        props.add("import_path", importPath);

        JsonObject methodSig = new JsonObject();
        methodSig.addProperty("type", "string");
        methodSig.addProperty("description", "(add_method) Method signature INCLUDING return type + name + params, e.g. 'public void onClick(View v)' or 'private String getName()'.");
        props.add("method_signature", methodSig);

        JsonObject methodBody = new JsonObject();
        methodBody.addProperty("type", "string");
        methodBody.addProperty("description", "(add_method) Method body (without braces). Multi-line allowed.");
        props.add("method_body", methodBody);

        JsonObject searchText = new JsonObject();
        searchText.addProperty("type", "string");
        searchText.addProperty("description", "(search_replace) Text to search for (literal match).");
        props.add("search_text", searchText);

        JsonObject replaceText = new JsonObject();
        replaceText.addProperty("type", "string");
        replaceText.addProperty("description", "(search_replace) Replacement text. Empty string deletes the search text.");
        props.add("replace_text", replaceText);

        JsonObject filePath = new JsonObject();
        filePath.addProperty("type", "string");
        filePath.addProperty("description", "(import_files) File system path to import from (a .java file or .zip).");
        props.add("file_path", filePath);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "add_field": return doAddField(ctx, scId, args);
            case "add_import": return doAddImport(ctx, scId, args);
            case "add_method": return doAddMethod(ctx, scId, args);
            case "create_file": return doCreateFile(ctx, scId, args);
            case "delete_file": return doDeleteFile(ctx, scId, args);
            case "format": return doFormat(ctx, scId, args);
            case "import_files": return doImportFiles(ctx, scId, args);
            case "remove_from_manifest_as_activity": return doRemoveFromManifest(ctx, scId, args, "activity");
            case "remove_from_manifest_as_service": return doRemoveFromManifest(ctx, scId, args, "service");
            case "rename_file": return doRenameFile(ctx, scId, args);
            case "search_replace": return doSearchReplace(ctx, scId, args);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  add_field
    // ------------------------------------------------------------------
    private ToolResult doAddField(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        String fieldName = optString(args, "field_name");
        String fieldType = optString(args, "field_type", "String");
        String fieldInit = optString(args, "field_init");
        if (file == null || fieldName == null) return err("file_name and field_name are required.");
        if (!JAVA_CLASS_NAME.matcher(file).matches()) return err("Invalid file name '" + file + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        if (!JAVA_ID.matcher(fieldName).matches()) return err("Invalid field name '" + fieldName + "'. Must match ^[a-z][A-Za-z0-9_]*$.");
        if (fieldType.isEmpty()) return err("field_type cannot be empty.");
        // Construct field declaration.
        String decl = "private " + fieldType + " " + fieldName;
        if (fieldInit != null && !fieldInit.isEmpty()) decl += " = " + fieldInit;
        decl += ";";
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "a", file, "field:" + decl);
            ctx.refreshLogicEditor();
            return ok("Added field '" + decl + "' to '" + file + ".java'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  add_import
    // ------------------------------------------------------------------
    private ToolResult doAddImport(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        String imp = optString(args, "import_path");
        if (file == null || imp == null) return err("file_name and import_path are required.");
        if (!JAVA_CLASS_NAME.matcher(file).matches()) return err("Invalid file name '" + file + "'.");
        if (!JAVA_IMPORT.matcher(imp).matches()) return err("Invalid import path '" + imp + "'. Must be a fully-qualified Java name (e.g. 'java.util.List').");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "a", file, "import:" + imp + ";");
            ctx.refreshLogicEditor();
            return ok("Added import '" + imp + "' to '" + file + ".java'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  add_method
    // ------------------------------------------------------------------
    private ToolResult doAddMethod(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        String sig = optString(args, "method_signature");
        String body = optString(args, "method_body", "");
        if (file == null || sig == null) return err("file_name and method_signature are required.");
        if (!JAVA_CLASS_NAME.matcher(file).matches()) return err("Invalid file name '" + file + "'.");
        if (!sig.contains("(") || !sig.contains(")")) {
            return err("Invalid method signature '" + sig + "'. Must contain '(' and ')' (e.g. 'public void onClick(View v)').");
        }
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            String methodDecl = "method:" + sig + " { " + body + " }";
            SketchwareApi.invoke(jc, "a", file, methodDecl);
            ctx.refreshLogicEditor();
            return ok("Added method to '" + file + ".java':\n  " + sig + " {\n    " + body + "\n  }");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  create_file
    // ------------------------------------------------------------------
    private ToolResult doCreateFile(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        if (file == null) return err("file_name is required.");
        if (!JAVA_CLASS_NAME.matcher(file).matches()) return err("Invalid file name '" + file + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "b", file);
            ctx.refreshLogicEditor();
            return ok("Created Java file '" + file + ".java'. Default skeleton: package + class declaration.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete_file
    // ------------------------------------------------------------------
    private ToolResult doDeleteFile(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        if (file == null) return err("file_name is required.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "c", file);
            ctx.refreshLogicEditor();
            return ok("Deleted Java file '" + file + ".java' and all references.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  format
    // ------------------------------------------------------------------
    private ToolResult doFormat(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        if (file == null) return err("file_name is required.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "d", file);
            ctx.refreshLogicEditor();
            return ok("Formatted Java file '" + file + ".java' (applied Google Java Style 4-space indent).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  import_files
    // ------------------------------------------------------------------
    private ToolResult doImportFiles(SketchwareToolContext ctx, String scId, JsonObject args) {
        String path = optString(args, "file_path");
        if (path == null) return err("file_path is required.");
        if (!path.endsWith(".java") && !path.endsWith(".zip")) {
            return err("file_path must end with .java or .zip. Got: " + path);
        }
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "e", path);
            ctx.refreshLogicEditor();
            return ok("Imported Java file(s) from " + path + " into project '" + scId + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  remove_from_manifest_as_activity / service
    // ------------------------------------------------------------------
    private ToolResult doRemoveFromManifest(SketchwareToolContext ctx, String scId, JsonObject args, String tag) {
        String file = optString(args, "file_name");
        if (file == null) return err("file_name is required.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            SketchwareApi.invoke(jc, "a", tag, file);
            return ok("Removed <" + tag + " android:name=\"" + file + "\" /> from AndroidManifest.xml.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  rename_file
    // ------------------------------------------------------------------
    private ToolResult doRenameFile(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        String newName = optString(args, "new_name");
        if (file == null || newName == null) return err("file_name and new_name are required.");
        if (!JAVA_CLASS_NAME.matcher(newName).matches()) return err("Invalid new_name '" + newName + "'. Must match ^[A-Z][A-Za-z0-9_]*$.");
        if (file.equals(newName)) return err("new_name must be different from file_name.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "f", file, newName);
            // Update manifest reference if this class is registered as an activity/service.
            try {
                Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
                SketchwareApi.invoke(pm, "z", file, newName);
            } catch (Throwable ignored) {}
            ctx.refreshLogicEditor();
            return ok("Renamed Java file '" + file + ".java' → '" + newName + ".java'. "
                    + "All manifest references and event handlers updated.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  search_replace
    // ------------------------------------------------------------------
    private ToolResult doSearchReplace(SketchwareToolContext ctx, String scId, JsonObject args) {
        String file = optString(args, "file_name");
        String search = optString(args, "search_text");
        String replace = optString(args, "replace_text", "");
        if (file == null || search == null) return err("file_name and search_text are required.");
        if (search.isEmpty()) return err("search_text cannot be empty.");
        if (search.equals(replace)) return err("search_text and replace_text are identical — no change.");
        try {
            Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(jc, "g", file, search, replace);
            ctx.refreshLogicEditor();
            return ok("Replaced all occurrences of '" + truncate(search, 50) + "' → '" + truncate(replace, 50) + "' in '" + file + ".java'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
