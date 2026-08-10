package com.sketchware.ai.tools.library;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * permission_manage — universal tool for library operations.
 *
 * <p>Replaces 6 stubs: permission_manage:add, permission_manage:add_custom, permission_manage:add_uses_feature, permission_manage:list, permission_manage:remove, permission_manage:reset_all
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class PermissionManageTool extends UniversalTool {

    public PermissionManageTool() {
        super("permission_manage",
                "Manage Android permissions: add (from list), add_custom, add_uses_feature, list, remove, reset_all.",
                "library", false, false,
"add",
                "add_custom",
                "add_uses_feature",
                "list",
                "remove",
                "reset_all");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_permission_name = new JsonObject();
        p_permission_name.addProperty("type", "string");
        p_permission_name.addProperty("description", "Permission name (e.g. android.permission.INTERNET).");
        props.add("permission_name", p_permission_name);
        JsonObject p_feature_name = new JsonObject();
        p_feature_name.addProperty("type", "string");
        p_feature_name.addProperty("description", "(add_uses_feature) uses-feature name (e.g. android.hardware.camera).");
        props.add("feature_name", p_feature_name);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add": {
                String perm = optString(args, "permission_name");
                                if (perm == null) return err("permission_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "b", perm);
                                    return ok("Added permission '" + perm + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_custom": {
                String perm = optString(args, "permission_name");
                                if (perm == null) return err("permission_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "c", perm);
                                    return ok("Added custom permission '" + perm + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_uses_feature": {
                String feat = optString(args, "feature_name");
                                if (feat == null) return err("feature_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "d", feat);
                                    return ok("Added uses-feature '" + feat + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "list": {
                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    Object perms = SketchwareApi.invoke(pm, "e");
                                    return ok("Permissions: " + perms);
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove": {
                String perm = optString(args, "permission_name");
                                if (perm == null) return err("permission_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "f", perm);
                                    return ok("Removed permission '" + perm + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "reset_all": {
                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "g");
                                    return ok("Reset all permissions.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
