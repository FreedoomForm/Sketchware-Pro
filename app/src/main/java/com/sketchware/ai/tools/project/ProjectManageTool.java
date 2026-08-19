package com.sketchware.ai.tools.project;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * project_manage — universal tool for project operations.
 *
 * <p>Replaces 7 stubs: project_manage:create, project_manage:open, project_manage:close, project_manage:save, project_manage:delete, project_manage:export, project_manage:import
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ProjectManageTool extends UniversalTool {

    public ProjectManageTool() {
        super("project_manage",
                "Manage whole projects: create, open, close, save, delete, export, or import.",
                "project", false, false,
"create",
                "open",
                "close",
                "save",
                "delete",
                "export",
                "import");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_project_name = new JsonObject();
        p_project_name.addProperty("type", "string");
        p_project_name.addProperty("description", "(create) Project name.");
        props.add("project_name", p_project_name);
        JsonObject p_project_id = new JsonObject();
        p_project_id.addProperty("type", "string");
        p_project_id.addProperty("description", "Project sc_id (open/close/save/delete).");
        props.add("project_id", p_project_id);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(export/import) File system path.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "project_name");
                                if (name == null) return err("project_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    Object newId = SketchwareApi.invoke(pm, "a", name);
                                    return ok("Created project '" + name + "' with sc_id=" + newId + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "open": {
                String scId = optString(args, "project_id");
                                if (scId == null) return err("project_id is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    SketchwareApi.invoke(pm, "b", scId);
                                    return ok("Opened project '" + scId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "close": {
                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    SketchwareApi.invoke(pm, "c");
                                    return ok("Closed active project.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "save": {
                String scId = optString(args, "project_id", ctx.getScId());
                                if (scId == null) return err("project_id or active context required");
                                try {
                                    SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                                    SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
                                    SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
                                    return ok("Saved project '" + scId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String scId = optString(args, "project_id");
                                if (scId == null) return err("project_id is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    SketchwareApi.invoke(pm, "d", scId);
                                    return ok("Deleted project '" + scId + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "export": {
                String scId = optString(args, "project_id", ctx.getScId());
                                String path = optString(args, "file_path");
                                if (scId == null || path == null) return err("project_id and file_path required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    SketchwareApi.invoke(pm, "e", scId, path);
                                    return ok("Exported project '" + scId + "' to " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "b");
                                    Object newId = SketchwareApi.invoke(pm, "f", path);
                                    return ok("Imported project from " + path + " (sc_id=" + newId + ").");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
