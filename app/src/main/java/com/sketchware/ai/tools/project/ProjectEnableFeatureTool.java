package com.sketchware.ai.tools.project;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * project_enable_feature — universal tool for project operations.
 *
 * <p>Replaces 3 stubs: project_enable_feature:enable_bridgeless_themes, project_enable_feature:enable_new_xml_command, project_enable_feature:enable_viewbinding
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ProjectEnableFeatureTool extends UniversalTool {

    public ProjectEnableFeatureTool() {
        super("project_enable_feature",
                "Enable an optional project feature: bridgeless_themes, new_xml_command, or viewbinding.",
                "project", false, false,
"enable_bridgeless_themes",
                "enable_new_xml_command",
                "enable_viewbinding");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_enabled = new JsonObject();
        p_enabled.addProperty("type", "boolean");
        p_enabled.addProperty("description", "Whether to enable (true) or disable (false) the feature. Default: true.");
        props.add("enabled", p_enabled);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "enable_bridgeless_themes": {
                return enableFeature(ctx, args, "bridgelessThemes");
            }
            case "enable_new_xml_command": {
                return enableFeature(ctx, args, "newXmlCommand");
            }
            case "enable_viewbinding": {
                return enableFeature(ctx, args, "viewBinding");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult enableFeature(SketchwareToolContext ctx, JsonObject args, String featureKey) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project.");
        boolean enabled = optBool(args, "enabled", true);
        try {
            Object projectFile = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
            java.lang.reflect.Field f = null;
            Class<?> cls = projectFile.getClass();
            while (cls != null && f == null) {
                try { f = cls.getDeclaredField(featureKey); } catch (NoSuchFieldException ignored) { cls = cls.getSuperclass(); }
            }
            if (f != null) {
                f.setAccessible(true);
                if (f.getType() == boolean.class) f.setBoolean(projectFile, enabled);
                else f.set(projectFile, enabled);
            } else {
                SketchwareApi.invoke(projectFile, "set" + Character.toUpperCase(featureKey.charAt(0)) + featureKey.substring(1), enabled);
            }
            SketchwareApi.invoke(projectFile, "saveConfig");
            return ok((enabled ? "Enabled" : "Disabled") + " feature '" + featureKey + "' on project '" + scId + "'.");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
