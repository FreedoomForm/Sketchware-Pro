package com.sketchware.ai.tools.library;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * library_manage — universal tool for library operations.
 *
 * <p>Replaces 6 stubs: library_manage:add_repository, library_manage:disable, library_manage:download_local_lib, library_manage:list, library_manage:remove_repository, library_manage:set_version
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class LibraryManageTool extends UniversalTool {

    public LibraryManageTool() {
        super("library_manage",
                "Manage libraries: add_repository, disable, download_local_lib, list, remove_repository, set_version.",
                "library", false, false,
"add_repository",
                "disable",
                "download_local_lib",
                "list",
                "remove_repository",
                "set_version");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_library_name = new JsonObject();
        p_library_name.addProperty("type", "string");
        p_library_name.addProperty("description", "Library name (disable/list/set_version).");
        props.add("library_name", p_library_name);
        JsonObject p_repository_url = new JsonObject();
        p_repository_url.addProperty("type", "string");
        p_repository_url.addProperty("description", "(add_repository) Maven repository URL.");
        props.add("repository_url", p_repository_url);
        JsonObject p_repository_name = new JsonObject();
        p_repository_name.addProperty("type", "string");
        p_repository_name.addProperty("description", "(add_repository/remove_repository) Repository name.");
        props.add("repository_name", p_repository_name);
        JsonObject p_version = new JsonObject();
        p_version.addProperty("type", "string");
        p_version.addProperty("description", "(set_version) Library version.");
        props.add("version", p_version);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(download_local_lib) Local .jar/.aar path.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_repository": {
                String name = optString(args, "repository_name");
                                String url = optString(args, "repository_url");
                                if (name == null || url == null) return err("repository_name and repository_url required");
                                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(dep, "addRepository", name, url);
                                    return ok("Added repository '" + name + "' at " + url + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "disable": {
                String name = optString(args, "library_name");
                                if (name == null) return err("library_name is required");
                                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(dep, "disable", name);
                                    return ok("Disabled library '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "download_local_lib": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(dep, "addLocalLib", path);
                                    return ok("Added local library from " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "list": {
                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    Object libs = SketchwareApi.invoke(dep, "list");
                                    return ok("Libraries: " + libs);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove_repository": {
                String name = optString(args, "repository_name");
                                if (name == null) return err("repository_name is required");
                                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(dep, "removeRepository", name);
                                    return ok("Removed repository '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_version": {
                String name = optString(args, "library_name");
                                String ver = optString(args, "version");
                                if (name == null || ver == null) return err("library_name and version required");
                                try {
                                    Object dep = SketchwareApi.invokeStatic("mod.pranav.dependency.DependencyManager", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(dep, "setVersion", name, ver);
                                    return ok("Set version of '" + name + "' to '" + ver + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
