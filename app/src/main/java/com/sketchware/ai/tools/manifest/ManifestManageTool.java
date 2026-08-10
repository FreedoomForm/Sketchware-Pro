package com.sketchware.ai.tools.manifest;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * manifest_manage — universal tool for manifest operations.
 *
 * <p>Replaces 6 stubs: manifest_manage:add_activity, manifest_manage:add_permission, manifest_manage:delete_activity, manifest_manage:set_activity_attribute, manifest_manage:set_application_attribute, manifest_manage:set_components
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ManifestManageTool extends UniversalTool {

    public ManifestManageTool() {
        super("manifest_manage",
                "Manage AndroidManifest.xml entries: add_activity, add_permission, delete_activity, set_activity_attribute, set_application_attribute, set_components.",
                "manifest", false, false,
"add_activity",
                "add_permission",
                "delete_activity",
                "set_activity_attribute",
                "set_application_attribute",
                "set_components");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_activity_name = new JsonObject();
        p_activity_name.addProperty("type", "string");
        p_activity_name.addProperty("description", "(add/delete_activity) Activity class name.");
        props.add("activity_name", p_activity_name);
        JsonObject p_permission_name = new JsonObject();
        p_permission_name.addProperty("type", "string");
        p_permission_name.addProperty("description", "(add_permission) Permission name.");
        props.add("permission_name", p_permission_name);
        JsonObject p_attribute_name = new JsonObject();
        p_attribute_name.addProperty("type", "string");
        p_attribute_name.addProperty("description", "(set_activity_attribute/set_application_attribute) Attribute name.");
        props.add("attribute_name", p_attribute_name);
        JsonObject p_attribute_value = new JsonObject();
        p_attribute_value.addProperty("type", "string");
        p_attribute_value.addProperty("description", "(set_activity_attribute/set_application_attribute) Attribute value.");
        props.add("attribute_value", p_attribute_value);
        JsonObject p_components_xml = new JsonObject();
        p_components_xml.addProperty("type", "string");
        p_components_xml.addProperty("description", "(set_components) Raw XML for <components> section.");
        props.add("components_xml", p_components_xml);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_activity": {
                String name = optString(args, "activity_name");
                                if (name == null) return err("activity_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "h", name);
                                    return ok("Added activity '" + name + "' to manifest.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "add_permission": {
                String perm = optString(args, "permission_name");
                                if (perm == null) return err("permission_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "b", perm);
                                    return ok("Added permission '" + perm + "' to manifest.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete_activity": {
                String name = optString(args, "activity_name");
                                if (name == null) return err("activity_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "i", name);
                                    return ok("Deleted activity '" + name + "' from manifest.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_activity_attribute": {
                String activity = optString(args, "activity_name");
                                String attr = optString(args, "attribute_name");
                                String val = optString(args, "attribute_value");
                                if (activity == null || attr == null || val == null) return err("activity_name, attribute_name, attribute_value required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "j", activity, attr, val);
                                    return ok("Set activity '" + activity + "' attribute '" + attr + "' = '" + val + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_application_attribute": {
                String attr = optString(args, "attribute_name");
                                String val = optString(args, "attribute_value");
                                if (attr == null || val == null) return err("attribute_name and attribute_value required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "k", attr, val);
                                    return ok("Set application attribute '" + attr + "' = '" + val + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "set_components": {
                String xml = optString(args, "components_xml");
                                if (xml == null) return err("components_xml is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "l", xml);
                                    return ok("Set manifest <components> section.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
