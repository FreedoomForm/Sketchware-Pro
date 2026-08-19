package com.sketchware.ai.tools.manifest;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * xml_command_manage — universal tool for manifest operations.
 *
 * <p>Replaces 3 stubs: xml_command_manage:add, xml_command_manage:delete, xml_command_manage:edit
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class XmlCommandManageTool extends UniversalTool {

    public XmlCommandManageTool() {
        super("xml_command_manage",
                "Manage custom XML commands in the project: add, delete, edit.",
                "manifest", false, false,
"add",
                "delete",
                "edit");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_command_name = new JsonObject();
        p_command_name.addProperty("type", "string");
        p_command_name.addProperty("description", "XML command name.");
        props.add("command_name", p_command_name);
        JsonObject p_command_xml = new JsonObject();
        p_command_xml.addProperty("type", "string");
        p_command_xml.addProperty("description", "(add/edit) XML content of the command.");
        props.add("command_xml", p_command_xml);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add": {
                String name = optString(args, "command_name");
                                String xml = optString(args, "command_xml", "");
                                if (name == null) return err("command_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "p", name, xml);
                                    return ok("Added XML command '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String name = optString(args, "command_name");
                                if (name == null) return err("command_name is required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "q", name);
                                    return ok("Deleted XML command '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "edit": {
                String name = optString(args, "command_name");
                                String xml = optString(args, "command_xml");
                                if (name == null || xml == null) return err("command_name and command_xml required");
                                try {
                                    Object pm = SketchwareApi.invokeStatic("a.a.a.jC", "d", ctx.getScId());
                                    SketchwareApi.invoke(pm, "r", name, xml);
                                    return ok("Edited XML command '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
