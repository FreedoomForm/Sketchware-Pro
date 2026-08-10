package com.sketchware.ai.tools.java;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * java_modify_class — universal tool for java operations.
 *
 * <p>Replaces 11 stubs: java_modify_class:add_field, java_modify_class:add_import, java_modify_class:add_method, java_modify_class:create_file, java_modify_class:delete_file, java_modify_class:format, java_modify_class:import_files, java_modify_class:remove_from_manifest_as_activity, java_modify_class:remove_from_manifest_as_service, java_modify_class:rename_file, java_modify_class:search_replace
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class JavaModifyClassTool extends UniversalTool {

    public JavaModifyClassTool() {
        super("java_modify_class",
                "Modify a Java file: add_field, add_import, add_method, create_file, delete_file, format, import_files, remove_from_manifest_as_activity, remove_from_manifest_as_service, rename_file, search_replace.",
                "java", false, false,
"add_field",
                "add_import",
                "add_method",
                "create_file",
                "delete_file",
                "format",
                "import_files",
                "remove_from_manifest_as_activity",
                "remove_from_manifest_as_service",
                "rename_file",
                "search_replace");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_file_name = new JsonObject();
        p_file_name.addProperty("type", "string");
        p_file_name.addProperty("description", "Java file name (without .java).");
        props.add("file_name", p_file_name);
        JsonObject p_new_name = new JsonObject();
        p_new_name.addProperty("type", "string");
        p_new_name.addProperty("description", "(rename_file) New file name.");
        props.add("new_name", p_new_name);
        JsonObject p_field_name = new JsonObject();
        p_field_name.addProperty("type", "string");
        p_field_name.addProperty("description", "(add_field) Field name.");
        props.add("field_name", p_field_name);
        JsonObject p_field_type = new JsonObject();
        p_field_type.addProperty("type", "string");
        p_field_type.addProperty("description", "(add_field) Field type (e.g. 'String', 'int').");
        props.add("field_type", p_field_type);
        JsonObject p_import_path = new JsonObject();
        p_import_path.addProperty("type", "string");
        p_import_path.addProperty("description", "(add_import) Java import path (e.g. 'java.util.List').");
        props.add("import_path", p_import_path);
        JsonObject p_method_signature = new JsonObject();
        p_method_signature.addProperty("type", "string");
        p_method_signature.addProperty("description", "(add_method) Method signature (e.g. 'public void foo()').");
        props.add("method_signature", p_method_signature);
        JsonObject p_method_body = new JsonObject();
        p_method_body.addProperty("type", "string");
        p_method_body.addProperty("description", "(add_method) Method body code.");
        props.add("method_body", p_method_body);
        JsonObject p_search_text = new JsonObject();
        p_search_text.addProperty("type", "string");
        p_search_text.addProperty("description", "(search_replace) Text to search.");
        props.add("search_text", p_search_text);
        JsonObject p_replace_text = new JsonObject();
        p_replace_text.addProperty("type", "string");
        p_replace_text.addProperty("description", "(search_replace) Replacement text.");
        props.add("replace_text", p_replace_text);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(import_files) File system path to import from.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_field": {
                String file = optString(args, "file_name");
                                String fieldName = optString(args, "field_name");
                                String fieldType = optString(args, "field_type", "String");
                                if (file == null || fieldName == null) return err("file_name and field_name required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "a", file, "field:" + fieldType + " " + fieldName + ";");
                                    ctx.refreshLogicEditor();
                                    return ok("Added field '" + fieldType + " " + fieldName + "' to '" + file + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_import": {
                String file = optString(args, "file_name");
                                String imp = optString(args, "import_path");
                                if (file == null || imp == null) return err("file_name and import_path required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "a", file, "import:" + imp + ";");
                                    ctx.refreshLogicEditor();
                                    return ok("Added import '" + imp + "' to '" + file + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_method": {
                String file = optString(args, "file_name");
                                String sig = optString(args, "method_signature");
                                String body = optString(args, "method_body", "");
                                if (file == null || sig == null) return err("file_name and method_signature required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "a", file, "method:" + sig + " { " + body + " }");
                                    ctx.refreshLogicEditor();
                                    return ok("Added method '" + sig + "' to '" + file + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "create_file": {
                String file = optString(args, "file_name");
                                if (file == null) return err("file_name is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "b", file);
                                    ctx.refreshLogicEditor();
                                    return ok("Created Java file '" + file + ".java'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete_file": {
                String file = optString(args, "file_name");
                                if (file == null) return err("file_name is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "c", file);
                                    ctx.refreshLogicEditor();
                                    return ok("Deleted Java file '" + file + ".java'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "format": {
                String file = optString(args, "file_name");
                                if (file == null) return err("file_name is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "d", file);
                                    ctx.refreshLogicEditor();
                                    return ok("Formatted Java file '" + file + ".java'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import_files": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "e", path);
                                    ctx.refreshLogicEditor();
                                    return ok("Imported Java files from " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove_from_manifest_as_activity": {
                String file = optString(args, "file_name");
                                if (file == null) return err("file_name is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(jc, "a", "activity", file);
                                    return ok("Removed '" + file + "' from manifest as activity.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove_from_manifest_as_service": {
                String file = optString(args, "file_name");
                                if (file == null) return err("file_name is required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(jc, "a", "service", file);
                                    return ok("Removed '" + file + "' from manifest as service.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "rename_file": {
                String file = optString(args, "file_name");
                                String newName = optString(args, "new_name");
                                if (file == null || newName == null) return err("file_name and new_name required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "f", file, newName);
                                    ctx.refreshLogicEditor();
                                    return ok("Renamed Java file '" + file + "' → '" + newName + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "search_replace": {
                String file = optString(args, "file_name");
                                String search = optString(args, "search_text");
                                String replace = optString(args, "replace_text", "");
                                if (file == null || search == null) return err("file_name and search_text required");
                                try {
                                    Object jc = SketchwareApi.invokeStatic("a.a.a.jC", "c", ctx.getScId());
                                    SketchwareApi.invoke(jc, "g", file, search, replace);
                                    ctx.refreshLogicEditor();
                                    return ok("Replaced '" + search + "' → '" + replace + "' in '" + file + ".java'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
