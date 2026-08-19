package com.sketchware.ai.tools.project;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * project_set_property — universal tool for project operations.
 *
 * <p>Replaces 8 stubs: project_set_property:set_app_class, project_set_property:set_app_icon, project_set_property:set_min_sdk, project_set_property:set_project_name, project_set_property:set_target_sdk, project_set_property:set_theme_color, project_set_property:set_version_code, project_set_property:set_version_name
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ProjectSetPropertyTool extends UniversalTool {

    public ProjectSetPropertyTool() {
        super("project_set_property",
                "Set a project-level property: app_class, app_icon, min_sdk, project_name, target_sdk, theme_color, version_code, version_name.",
                "project", false, false,
"set_app_class",
                "set_app_icon",
                "set_min_sdk",
                "set_project_name",
                "set_target_sdk",
                "set_theme_color",
                "set_version_code",
                "set_version_name");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "Property value to set.");
        props.add("value", p_value);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "set_app_class": {
                return setProjectProp(ctx, args, "applicationClassName");
            }
            case "set_app_icon": {
                return setProjectProp(ctx, args, "iconPath");
            }
            case "set_min_sdk": {
                return setProjectProp(ctx, args, "minSdkVersion");
            }
            case "set_project_name": {
                return setProjectProp(ctx, args, "projectName");
            }
            case "set_target_sdk": {
                return setProjectProp(ctx, args, "targetSdkVersion");
            }
            case "set_theme_color": {
                return setProjectProp(ctx, args, "themeColor");
            }
            case "set_version_code": {
                return setProjectProp(ctx, args, "versionCode");
            }
            case "set_version_name": {
                return setProjectProp(ctx, args, "versionName");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult setProjectProp(SketchwareToolContext ctx, JsonObject args, String propKey) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project.");
        String value = optString(args, "value");
        if (value == null) return err("value is required");
        try {
            Object projectFile = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
            java.lang.reflect.Field f = null;
            Class<?> cls = projectFile.getClass();
            while (cls != null && f == null) {
                try { f = cls.getDeclaredField(propKey); } catch (NoSuchFieldException ignored) { cls = cls.getSuperclass(); }
            }
            if (f == null) {
                // fallback: invoke a setter method
                SketchwareApi.invoke(projectFile, "set" + Character.toUpperCase(propKey.charAt(0)) + propKey.substring(1), value);
            } else {
                f.setAccessible(true);
                if (f.getType() == int.class) {
                    try { f.setInt(projectFile, Integer.parseInt(value)); }
                    catch (NumberFormatException e) { return err("Property '" + propKey + "' requires an integer value."); }
                } else {
                    f.set(projectFile, value);
                }
            }
            SketchwareApi.invoke(projectFile, "saveConfig");
            return ok("Set " + propKey + " = '" + value + "' on project '" + scId + "'.");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
