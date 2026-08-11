package com.sketchware.ai.tools.component;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import a.a.a.wq;
import mod.hilal.saif.components.ComponentsHandler;
import mod.hey.studios.util.Helper;
import pro.sketchware.utility.FileUtil;

/**
 * custom_component_manage — universal tool for managing Sketchware-Pro's
 * <b>custom components</b>.
 *
 * <p>This is a <b>separate subsystem</b> from
 * {@link com.sketchware.ai.tools.view.ViewManageCustomWidgetTool}
 * (which manages "custom palette widgets" via {@code WidgetsCreatorManager}).
 * Custom components are user-defined Java UI components (like Lottie, RangeSlider,
 * etc.) that get injected into the palette and have their code generated
 * when used in a layout. They are managed by
 * {@link pro.sketchware.activities.editor.component.ManageCustomComponentActivity}
 * + {@link pro.sketchware.activities.editor.component.AddCustomComponentActivity}.
 *
 * <p><b>Storage model</b>: the custom components JSON file is at
 * {@code wq.getCustomComponent()} (a static path string, typically
 * {@code <ext>/.sketchware/resources/component/components.json}).
 * Export directory is {@code wq.getExtraDataExport() + "/components/"}.
 *
 * <p>The JSON file is an {@code ArrayList<HashMap<String, Object>>}
 * with the following keys per component:
 * <ul>
 *   <li>{@code name} (String) — display name</li>
 *   <li>{@code id} (String) — unique identifier (typically auto-derived from name)</li>
 *   <li>{@code icon} (String) — old resource ID as a string (e.g. "3d_rotation")</li>
 *   <li>{@code varName} (String) — Java variable name (lowercase first letter)</li>
 *   <li>{@code typeName} (String) — type name shown in palette (e.g. "Lottie")</li>
 *   <li>{@code buildClass} (String) — full class name for build (e.g. "com.airbnb.lottie.LottieAnimationView")</li>
 *   <li>{@code class} (String) — full Java class name for import</li>
 *   <li>{@code description} (String) — short description</li>
 *   <li>{@code url} (String) — documentation URL</li>
 *   <li>{@code additionalVar} (String) — extra variable declarations</li>
 *   <li>{@code defineAdditionalVar} (String) — extra variable definitions</li>
 *   <li>{@code imports} (String) — additional Java imports (semicolon-separated)</li>
 * </ul>
 *
 * <p>Actions (6):
 * <ul>
 *   <li><b>create</b> — create a new custom component (params: {@code name},
 *       {@code class_name}, {@code layout_xml}, optional {@code icon_path},
 *       optional {@code java_code}, optional {@code description},
 *       optional {@code doc_url}, optional {@code imports}).</li>
 *   <li><b>edit</b> — edit an existing component (params: {@code name} to identify,
 *       plus any fields to update).</li>
 *   <li><b>delete</b> — delete a component (params: {@code name}).</li>
 *   <li><b>export</b> — export a component to a JSON file (params: {@code name},
 *       optional {@code dest_path}).</li>
 *   <li><b>import</b> — import component(s) from a JSON file (params:
 *       {@code source_path}).</li>
 *   <li><b>list</b> — list all custom components (read-only, auto-approved).</li>
 * </ul>
 *
 * <p>After every mutating action, the tool calls
 * {@link ComponentsHandler#refreshCachedCustomComponents()} so the Sketchware
 * UI picks up the changes immediately.
 */
public final class CustomComponentManageTool extends UniversalTool {

    /** Component name convention: letters, digits, underscore. */
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    /** Java class name convention: dotted path of identifiers, last segment capitalized. */
    private static final Pattern VALID_CLASS_NAME =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$");

    public CustomComponentManageTool() {
        super("custom_component_manage",
                "Manage custom components (user-defined Java UI components): "
                        + "create, edit, delete, export, import, list. "
                        + "Component names must match ^[A-Za-z][A-Za-z0-9_]*$. "
                        + "Class names must be valid Java fully-qualified class names.",
                "component", false, false,
                "create", "edit", "delete", "export", "import", "list");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "name", "Component name (used as identifier). Must match ^[A-Za-z][A-Za-z0-9_]*$.");
        addStringProp(props, "class_name", "(create) Fully-qualified Java class name, e.g. 'com.airbnb.lottie.LottieAnimationView'.");
        addStringProp(props, "type_name", "(create/edit) Type name shown in palette (e.g. 'Lottie'). Defaults to component name.");
        addStringProp(props, "var_name", "(create/edit) Java variable name (lowercase first letter). Auto-derived from name if absent.");
        addStringProp(props, "icon", "(create/edit) Old Sketchware resource ID as a string (e.g. '3d_rotation').");
        addStringProp(props, "build_class", "(create/edit) Build class name (defaults to class_name).");
        addStringProp(props, "layout_xml", "(create) Layout XML for the component's palette preview (optional).");
        addStringProp(props, "java_code", "(create/edit) Additional Java code (optional).");
        addStringProp(props, "description", "(create/edit) Short description of the component.");
        addStringProp(props, "doc_url", "(create/edit) Documentation URL.");
        addStringProp(props, "imports", "(create/edit) Additional Java imports (semicolon-separated).");
        addStringProp(props, "additional_var", "(create/edit) Extra variable declarations.");
        addStringProp(props, "define_additional_var", "(create/edit) Extra variable definitions.");
        addStringProp(props, "dest_path", "(export) Absolute destination file path. Defaults to <export_dir>/<name>.json.");
        addStringProp(props, "source_path", "(import) Absolute source JSON file path.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        try {
            switch (action) {
                case "create": return doCreate(args);
                case "edit":   return doEdit(args);
                case "delete": return doDelete(args);
                case "export": return doExport(args);
                case "import": return doImport(args);
                case "list":   return doList(args);
                default:       return err("Unknown action: " + action);
            }
        } finally {
            // Always refresh the cached custom components so the UI picks up changes.
            try { ComponentsHandler.refreshCachedCustomComponents(); } catch (Throwable ignored) {}
        }
    }

    // ==================================================================
    //  create
    // ==================================================================
    private ToolResult doCreate(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[A-Za-z][A-Za-z0-9_]*$.");
        }
        String className = optString(args, "class_name");
        if (className == null || className.isEmpty()) return err("class_name is required.");
        if (!VALID_CLASS_NAME.matcher(className).matches()) {
            return err("Invalid class_name '" + className + "'. Must be a valid fully-qualified Java class name.");
        }
        String typeName = optString(args, "type_name", name);
        String varName = optString(args, "var_name", deriveVarName(name));
        String buildClass = optString(args, "build_class", className);
        String icon = optString(args, "icon", "3d_rotation");
        String description = optString(args, "description", "");
        String docUrl = optString(args, "doc_url", "");
        String imports = optString(args, "imports", "");
        String additionalVar = optString(args, "additional_var", "");
        String defineAdditionalVar = optString(args, "define_additional_var", "");
        String layoutXml = optString(args, "layout_xml", "");
        String javaCode = optString(args, "java_code", "");

        ArrayList<HashMap<String, Object>> list = readComponents();
        if (findIndex(list, name) >= 0) {
            return err("Component '" + name + "' already exists. Use 'edit' to modify.");
        }

        HashMap<String, Object> comp = new HashMap<>();
        comp.put("name", name);
        comp.put("id", name);
        comp.put("icon", icon);
        comp.put("varName", varName);
        comp.put("typeName", typeName);
        comp.put("buildClass", buildClass);
        comp.put("class", className);
        comp.put("description", description);
        comp.put("url", docUrl);
        comp.put("additionalVar", additionalVar);
        comp.put("defineAdditionalVar", defineAdditionalVar);
        comp.put("imports", imports);
        // Layout XML and Java code aren't standard component fields — Sketchware
        // doesn't natively persist them per-component, but we record them in
        // extension fields so the AI agent can round-trip them. ComponentsHandler
        // ignores unknown keys.
        if (!layoutXml.isEmpty()) comp.put("layoutXml", layoutXml);
        if (!javaCode.isEmpty()) comp.put("javaCode", javaCode);

        if (!ComponentsHandler.isValidComponent(comp)) {
            return err("Internal error: built component failed validation (missing required key).");
        }

        list.add(comp);
        writeComponents(list);
        return ok("Created custom component '" + name + "' (class=" + className
                + ", var=" + varName + ", type=" + typeName + ").");
    }

    // ==================================================================
    //  edit
    // ==================================================================
    private ToolResult doEdit(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readComponents();
        int idx = findIndex(list, name);
        if (idx < 0) return err("Component '" + name + "' not found.");
        HashMap<String, Object> comp = list.get(idx);

        boolean changed = false;
        String className = optString(args, "class_name");
        if (className != null) {
            if (!VALID_CLASS_NAME.matcher(className).matches()) {
                return err("Invalid class_name '" + className + "'.");
            }
            comp.put("class", className);
            changed = true;
        }
        String typeName = optString(args, "type_name");
        if (typeName != null) { comp.put("typeName", typeName); changed = true; }
        String varName = optString(args, "var_name");
        if (varName != null) { comp.put("varName", varName); changed = true; }
        String buildClass = optString(args, "build_class");
        if (buildClass != null) { comp.put("buildClass", buildClass); changed = true; }
        String icon = optString(args, "icon");
        if (icon != null) { comp.put("icon", icon); changed = true; }
        String description = optString(args, "description");
        if (description != null) { comp.put("description", description); changed = true; }
        String docUrl = optString(args, "doc_url");
        if (docUrl != null) { comp.put("url", docUrl); changed = true; }
        String imports = optString(args, "imports");
        if (imports != null) { comp.put("imports", imports); changed = true; }
        String additionalVar = optString(args, "additional_var");
        if (additionalVar != null) { comp.put("additionalVar", additionalVar); changed = true; }
        String defineAdditionalVar = optString(args, "define_additional_var");
        if (defineAdditionalVar != null) { comp.put("defineAdditionalVar", defineAdditionalVar); changed = true; }
        String layoutXml = optString(args, "layout_xml");
        if (layoutXml != null) { comp.put("layoutXml", layoutXml); changed = true; }
        String javaCode = optString(args, "java_code");
        if (javaCode != null) { comp.put("javaCode", javaCode); changed = true; }

        if (!changed) {
            return err("No edit fields provided. Specify at least one of: class_name, type_name, var_name, build_class, icon, description, doc_url, imports, additional_var, define_additional_var, layout_xml, java_code.");
        }
        if (!ComponentsHandler.isValidComponent(comp)) {
            return err("Internal error: edited component failed validation (missing required key).");
        }
        writeComponents(list);
        return ok("Edited custom component '" + name + "'.");
    }

    // ==================================================================
    //  delete
    // ==================================================================
    private ToolResult doDelete(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readComponents();
        int idx = findIndex(list, name);
        if (idx < 0) return err("Component '" + name + "' not found.");
        list.remove(idx);
        writeComponents(list);
        return ok("Deleted custom component '" + name + "'. " + list.size() + " component(s) remain.");
    }

    // ==================================================================
    //  export
    // ==================================================================
    private ToolResult doExport(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readComponents();
        int idx = findIndex(list, name);
        if (idx < 0) return err("Component '" + name + "' not found.");
        HashMap<String, Object> comp = list.get(idx);

        String destPath = optString(args, "dest_path");
        if (destPath == null || destPath.isEmpty()) {
            String exportDir = wq.getExtraDataExport() + "/components/";
            try { new File(exportDir).mkdirs(); } catch (Throwable ignored) {}
            destPath = exportDir + name + ".json";
        }
        // Ensure parent dir exists.
        String parent = destPath.substring(0, Math.max(destPath.lastIndexOf(File.separator), 0));
        if (!FileUtil.isExistFile(parent)) {
            try { new File(parent).mkdirs(); } catch (Throwable ignored) {}
        }
        ArrayList<HashMap<String, Object>> wrapper = new ArrayList<>();
        wrapper.add(comp);
        FileUtil.writeFile(destPath, getGson().toJson(wrapper));
        return ok("Exported component '" + name + "' to '" + destPath + "'.");
    }

    // ==================================================================
    //  import
    // ==================================================================
    private ToolResult doImport(JsonObject args) {
        String sourcePath = optString(args, "source_path");
        if (sourcePath == null || sourcePath.isEmpty()) return err("source_path is required.");
        if (!FileUtil.isExistFile(sourcePath)) return err("source_path does not exist: " + sourcePath);

        var readResult = ComponentsHandler.readComponents(sourcePath);
        if (readResult.first.isPresent()) {
            return err("Failed to read components: " + readResult.first.get());
        }
        List<HashMap<String, Object>> imported = readResult.second;
        if (imported == null || imported.isEmpty()) {
            return err("No valid components found in source file.");
        }

        ArrayList<HashMap<String, Object>> list = readComponents();
        List<String> added = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        for (HashMap<String, Object> comp : imported) {
            if (!ComponentsHandler.isValidComponent(comp)) continue;
            String name = str(comp.get("name"));
            if (name == null || name.isEmpty()) continue;
            if (findIndex(list, name) >= 0) {
                duplicates.add(name);
                continue;
            }
            list.add(comp);
            added.add(name);
        }
        if (added.isEmpty()) {
            return err("Nothing imported. Duplicates: " + duplicates + ". (Total in source: " + imported.size() + ")");
        }
        writeComponents(list);
        StringBuilder sb = new StringBuilder();
        sb.append("Imported ").append(added.size()).append(" component(s): ").append(added);
        if (!duplicates.isEmpty()) sb.append(". Skipped duplicates: ").append(duplicates);
        return ok(sb.toString());
    }

    // ==================================================================
    //  list
    // ==================================================================
    private ToolResult doList(JsonObject args) {
        ArrayList<HashMap<String, Object>> list = readComponents();
        if (list.isEmpty()) {
            return ok("No custom components defined. Use 'create' or 'import' to add some.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Custom components (").append(list.size()).append("):\n");
        for (int i = 0; i < list.size(); i++) {
            HashMap<String, Object> c = list.get(i);
            sb.append("  [").append(i).append("] name='").append(str(c.get("name")))
              .append("' class='").append(str(c.get("class")))
              .append("' type='").append(str(c.get("typeName")))
              .append("' icon='").append(str(c.get("icon"))).append("'\n");
            String desc = str(c.get("description"));
            if (desc != null && !desc.isEmpty()) {
                sb.append("        description: ").append(desc).append("\n");
            }
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int findIndex(ArrayList<HashMap<String, Object>> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(str(list.get(i).get("name")))) return i;
        }
        return -1;
    }

    /** Derive a Java variable name from a component name (lowercase first letter). */
    private static String deriveVarName(String name) {
        if (name == null || name.isEmpty()) return "component";
        if (Character.isUpperCase(name.charAt(0))) {
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private static ArrayList<HashMap<String, Object>> readComponents() {
        String path = wq.getCustomComponent();
        if (!FileUtil.isExistFile(path)) return new ArrayList<>();
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

    private static void writeComponents(ArrayList<HashMap<String, Object>> list) {
        String path = wq.getCustomComponent();
        // Ensure parent dir exists.
        String parent = path.substring(0, Math.max(path.lastIndexOf(File.separator), 0));
        if (!FileUtil.isExistFile(parent)) {
            try { new File(parent).mkdirs(); } catch (Throwable ignored) {}
        }
        FileUtil.writeFile(path, getGson().toJson(list));
    }
}
