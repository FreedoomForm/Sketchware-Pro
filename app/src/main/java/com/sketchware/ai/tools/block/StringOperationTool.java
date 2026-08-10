package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * string_operation — universal tool for block operations.
 *
 * <p>Replaces 8 stubs: string_operation:concat, string_operation:contains, string_operation:length, string_operation:replace, string_operation:split, string_operation:substring, string_operation:to_lower, string_operation:to_upper
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class StringOperationTool extends UniversalTool {

    public StringOperationTool() {
        super("string_operation",
                "Add a string operation block to an event handler: concat, contains, length, replace, split, substring, to_lower, to_upper.",
                "block", false, false,
"concat",
                "contains",
                "length",
                "replace",
                "split",
                "substring",
                "to_lower",
                "to_upper");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID.");
        props.add("event_id", p_event_id);
        JsonObject p_operand_a = new JsonObject();
        p_operand_a.addProperty("type", "string");
        p_operand_a.addProperty("description", "First string operand.");
        props.add("operand_a", p_operand_a);
        JsonObject p_operand_b = new JsonObject();
        p_operand_b.addProperty("type", "string");
        p_operand_b.addProperty("description", "Second string operand (for concat/contains/replace/split).");
        props.add("operand_b", p_operand_b);
        JsonObject p_start = new JsonObject();
        p_start.addProperty("type", "integer");
        p_start.addProperty("description", "(substring) Start index.");
        props.add("start", p_start);
        JsonObject p_end = new JsonObject();
        p_end.addProperty("type", "integer");
        p_end.addProperty("description", "(substring) End index.");
        props.add("end", p_end);
        JsonObject p_result_var = new JsonObject();
        p_result_var.addProperty("type", "string");
        p_result_var.addProperty("description", "Variable name to store the result.");
        props.add("result_var", p_result_var);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "concat": {
                return addStringBlock(ctx, args, "concat");
            }
            case "contains": {
                return addStringBlock(ctx, args, "contains");
            }
            case "length": {
                return addStringBlock(ctx, args, "length");
            }
            case "replace": {
                return addStringBlock(ctx, args, "replace");
            }
            case "split": {
                return addStringBlock(ctx, args, "split");
            }
            case "substring": {
                return addStringBlock(ctx, args, "substring");
            }
            case "to_lower": {
                return addStringBlock(ctx, args, "toLower");
            }
            case "to_upper": {
                return addStringBlock(ctx, args, "toUpper");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult addStringBlock(SketchwareToolContext ctx, JsonObject args, String op) {
        String eventId = optString(args, "event_id");
        if (eventId == null) return err("event_id is required");
        String a = optString(args, "operand_a", "");
        String b = optString(args, "operand_b", "");
        int start = optInt(args, "start", 0);
        int end = optInt(args, "end", -1);
        String resultVar = optString(args, "result_var", "");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
            Object newBlockId = SketchwareApi.invoke(editor, "z", eventId, "string:" + op,
                    a, b, start, end, resultVar);
            ctx.refreshLogicEditor();
            return ok("Added string:" + op + " block to event '" + eventId + "' (id=" + newBlockId + ").");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
