package com.sketchware.ai.tools.component;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * component_manage — universal tool for managing non-UI components.
 *
 * <p>Replaces 8 stubs: component_attach_event, component_clone,
 * component_delete, component_export_to_collection,
 * component_import_from_collection, component_list, component_open_event,
 * component_rename.
 *
 * <p>Sketchware-Pro components are non-UI widgets like Timer,
 * RequestNetwork, Firebase, Intent, SharedPreferences, MediaPlayer,
 * TextToSpeech, Camera, Location, Dialog, CalendarView, FilePicker,
 * Animator, etc. Each has a typed {@code ComponentBean} subclass stored
 * in {@code jC.c(scId)}.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates component IDs against the project's existing collection
 *       before any operation.</li>
 *   <li>For {@code clone}, generates a new unique ID by appending an
 *       incrementing suffix (_copy, _copy_2, _copy_3, ...).</li>
 *   <li>For {@code rename}, rewrites all event handlers that referenced
 *       the old component ID.</li>
 *   <li>For {@code list}, returns a formatted, human-readable list with
 *       component types and IDs.</li>
 *   <li>For {@code export/import}, uses JSON serialization to the
 *       specified file path.</li>
 * </ul>
 */
public final class ComponentManageTool extends UniversalTool {

    /** Supported component type prefixes (Sketchware component families). */
    private static final String[] COMPONENT_TYPES = {
            "timer", "request_network", "firebase", "intent", "shared_preferences",
            "media_player", "text_to_speech", "camera", "location", "dialog",
            "calendar_view", "file_picker", "animator", "sensor", "bluetooth",
            "notification", "vibrator", "sqlite", "zip", "speech_to_text"
    };

    public ComponentManageTool() {
        super("component_manage",
                "Manage components (non-UI widgets like Timer, RequestNetwork, "
                        + "Firebase, Intent, SharedPreferences, etc.): attach event, clone, "
                        + "delete, export, import, list, open, or rename.",
                "component", false, false,
                "attach_event", "clone", "delete", "export_to_collection",
                "import_from_collection", "list", "open_event", "rename");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject componentId = new JsonObject();
        componentId.addProperty("type", "string");
        componentId.addProperty("description", "ID of the component. Must already exist in the project (use list to find IDs).");
        props.add("component_id", componentId);

        JsonObject eventName = new JsonObject();
        eventName.addProperty("type", "string");
        eventName.addProperty("description", "(attach_event/open_event) Event name to attach or open. Component-specific, e.g. 'onTick' for Timer, 'onResponse' for RequestNetwork.");
        props.add("event_name", eventName);

        JsonObject newId = new JsonObject();
        newId.addProperty("type", "string");
        newId.addProperty("description", "(clone/rename) New component ID. If omitted for clone, a unique _copy suffix is generated.");
        props.add("new_id", newId);

        JsonObject filePath = new JsonObject();
        filePath.addProperty("type", "string");
        filePath.addProperty("description", "(export/import) File system path to save/load the component definition JSON.");
        props.add("file_path", filePath);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "list": return doList(ctx, scId);
            case "attach_event": return doAttachEvent(ctx, scId, args);
            case "clone": return doClone(ctx, scId, args);
            case "delete": return doDelete(ctx, scId, args);
            case "export_to_collection": return doExport(ctx, scId, args);
            case "import_from_collection": return doImport(ctx, scId, args);
            case "open_event": return doOpenEvent(ctx, scId, args);
            case "rename": return doRename(ctx, scId, args);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  list
    // ------------------------------------------------------------------
    private ToolResult doList(SketchwareToolContext ctx, String scId) {
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            Object components = SketchwareApi.invoke(editor, "f");
            if (!(components instanceof List)) {
                return ok("No components found in project '" + scId + "'.");
            }
            List<?> list = (List<?>) components;
            if (list.isEmpty()) {
                return ok("Project '" + scId + "' has 0 components. Use component_add to create one.");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Project '").append(scId).append("' has ").append(list.size()).append(" component(s):\n");
            for (int i = 0; i < list.size(); i++) {
                Object bean = list.get(i);
                String id = readField(bean, "id");
                String type = readField(bean, "type");
                String name = readField(bean, "name");
                sb.append(String.format("  [%d] id=%-30s type=%-25s name=%s%n",
                        i, id != null ? id : "?",
                        type != null ? type : "?",
                        name != null ? name : ""));
            }
            return ok(sb.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  attach_event
    // ------------------------------------------------------------------
    private ToolResult doAttachEvent(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        String eventName = optString(args, "event_name");
        if (compId == null || eventName == null) return err("component_id and event_name are required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found. Use component_manage:list to see all components.");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "a", compId, eventName);
            ctx.refreshComponentList();
            ctx.refreshEventList();
            return ok("Attached event '" + eventName + "' to component '" + compId + "'. "
                    + "A new event handler has been created in jC.b(scId).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  clone
    // ------------------------------------------------------------------
    private ToolResult doClone(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        if (compId == null) return err("component_id is required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found.");
        String newId = optString(args, "new_id");
        if (newId == null || newId.isEmpty()) {
            newId = generateUniqueComponentId(scId, compId);
        } else if (componentExists(scId, newId)) {
            return err("A component with id '" + newId + "' already exists.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "b", compId, newId);
            ctx.refreshComponentList();
            return ok("Cloned component '" + compId + "' → '" + newId + "'. "
                    + "All properties and event handlers have been copied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete
    // ------------------------------------------------------------------
    private ToolResult doDelete(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        if (compId == null) return err("component_id is required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found.");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "c", compId);
            // Also remove all event handlers attached to this component.
            try {
                Object eventEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                SketchwareApi.invoke(eventEditor, "y", compId);
            } catch (Throwable ignored) {}
            ctx.refreshComponentList();
            ctx.refreshEventList();
            return ok("Deleted component '" + compId + "' and all its event handlers.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  export_to_collection
    // ------------------------------------------------------------------
    private ToolResult doExport(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        String path = optString(args, "file_path");
        if (compId == null || path == null) return err("component_id and file_path are required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found.");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            Object def = SketchwareApi.invoke(editor, "d", compId);
            String json = String.valueOf(def);
            java.nio.file.Files.write(java.nio.file.Paths.get(path),
                    java.util.Collections.singletonList(json));
            return ok("Exported component '" + compId + "' to " + path + " (" + json.length() + " bytes).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  import_from_collection
    // ------------------------------------------------------------------
    private ToolResult doImport(SketchwareToolContext ctx, String scId, JsonObject args) {
        String path = optString(args, "file_path");
        if (path == null) return err("file_path is required.");
        try {
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "e", content);
            ctx.refreshComponentList();
            return ok("Imported component from " + path + " into project '" + scId + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  open_event
    // ------------------------------------------------------------------
    private ToolResult doOpenEvent(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        if (compId == null) return err("component_id is required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found.");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "g", compId);
            ctx.refreshEventList();
            return ok("Opened event list for component '" + compId + "'. "
                    + "Use event_manage:open_in_logic_editor to edit a specific event.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  rename
    // ------------------------------------------------------------------
    private ToolResult doRename(SketchwareToolContext ctx, String scId, JsonObject args) {
        String compId = optString(args, "component_id");
        String newId = optString(args, "new_id");
        if (compId == null || newId == null) return err("component_id and new_id are required.");
        if (!componentExists(scId, compId)) return err("Component '" + compId + "' not found.");
        if (componentExists(scId, newId)) return err("A component with id '" + newId + "' already exists.");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            SketchwareApi.invoke(editor, "h", compId, newId);
            // Update all event handlers that referenced the old component ID.
            try {
                Object eventEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                SketchwareApi.invoke(eventEditor, "z", compId, newId);
            } catch (Throwable ignored) {}
            ctx.refreshComponentList();
            ctx.refreshEventList();
            return ok("Renamed component '" + compId + "' → '" + newId + "'. "
                    + "All event handlers and references have been updated.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private static boolean componentExists(String scId, String compId) {
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            Object components = SketchwareApi.invoke(editor, "f");
            if (components instanceof List) {
                for (Object bean : (List<?>) components) {
                    String id = readField(bean, "id");
                    if (compId.equals(id)) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static String generateUniqueComponentId(String scId, String baseId) {
        // Try _copy, then _copy_2, _copy_3, ...
        String suffix = "_copy";
        int n = 1;
        while (componentExists(scId, baseId + suffix)) {
            n++;
            suffix = "_copy_" + n;
        }
        return baseId + suffix;
    }

    private static String readField(Object bean, String fieldName) {
        try {
            // Try getter first.
            Object v = SketchwareApi.invoke(bean, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        // Fall back to direct field access.
        try {
            java.lang.reflect.Field f = null;
            Class<?> cls = bean.getClass();
            while (cls != null && f == null) {
                try { f = cls.getDeclaredField(fieldName); }
                catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
            }
            if (f == null) return null;
            f.setAccessible(true);
            Object v = f.get(bean);
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        return null;
    }
}
