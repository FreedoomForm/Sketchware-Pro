package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * moreblock_manage — universal tool for block operations.
 *
 * <p>Replaces 3 stubs: moreblock_manage:create, moreblock_manage:import_from_collection, moreblock_manage:save_to_collection
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class MoreblockManageTool extends UniversalTool {

    public MoreblockManageTool() {
        super("moreblock_manage",
                "Manage moreblocks (custom blocks): create, import_from_collection, save_to_collection.",
                "block", false, false,
"create",
                "import_from_collection",
                "save_to_collection");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Moreblock name.");
        props.add("name", p_name);
        JsonObject p_definition = new JsonObject();
        p_definition.addProperty("type", "string");
        p_definition.addProperty("description", "(create) JSON definition of the moreblock.");
        props.add("definition", p_definition);
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "(save/import) File system path.");
        props.add("file_path", p_file_path);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "name");
                                String def = optString(args, "definition", "{}");
                                if (name == null) return err("name is required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "q", "moreblock:" + name, def);
                                    return ok("Created moreblock '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "import_from_collection": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    SketchwareApi.invoke(pf, "r", "moreblock:import", content);
                                    return ok("Imported moreblock from " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "save_to_collection": {
                String name = optString(args, "name");
                                String path = optString(args, "file_path");
                                if (name == null || path == null) return err("name and file_path required");
                                try {
                                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                                    Object def = SketchwareApi.invoke(pf, "s", "moreblock:" + name);
                                    java.nio.file.Files.write(java.nio.file.Paths.get(path),
                                            java.util.Collections.singletonList(String.valueOf(def)));
                                    return ok("Saved moreblock '" + name + "' to " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
