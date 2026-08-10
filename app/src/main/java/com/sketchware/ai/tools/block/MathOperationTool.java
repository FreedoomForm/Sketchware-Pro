package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * math_operation — universal tool for block operations.
 *
 * <p>Replaces 10 stubs: math_operation:abs, math_operation:add, math_operation:divide, math_operation:max, math_operation:min, math_operation:modulo, math_operation:multiply, math_operation:random, math_operation:round, math_operation:subtract
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class MathOperationTool extends UniversalTool {

    public MathOperationTool() {
        super("math_operation",
                "Add a math operation block to an event handler: abs, add, divide, max, min, modulo, multiply, random, round, subtract.",
                "block", false, false,
"abs",
                "add",
                "divide",
                "max",
                "min",
                "modulo",
                "multiply",
                "random",
                "round",
                "subtract");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID.");
        props.add("event_id", p_event_id);
        JsonObject p_operand_a = new JsonObject();
        p_operand_a.addProperty("type", "string");
        p_operand_a.addProperty("description", "First operand (variable name or literal).");
        props.add("operand_a", p_operand_a);
        JsonObject p_operand_b = new JsonObject();
        p_operand_b.addProperty("type", "string");
        p_operand_b.addProperty("description", "Second operand (for binary ops).");
        props.add("operand_b", p_operand_b);
        JsonObject p_result_var = new JsonObject();
        p_result_var.addProperty("type", "string");
        p_result_var.addProperty("description", "Variable name to store the result.");
        props.add("result_var", p_result_var);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "abs": {
                return addMathBlock(ctx, args, "abs");
            }
            case "add": {
                return addMathBlock(ctx, args, "add");
            }
            case "divide": {
                return addMathBlock(ctx, args, "divide");
            }
            case "max": {
                return addMathBlock(ctx, args, "max");
            }
            case "min": {
                return addMathBlock(ctx, args, "min");
            }
            case "modulo": {
                return addMathBlock(ctx, args, "modulo");
            }
            case "multiply": {
                return addMathBlock(ctx, args, "multiply");
            }
            case "random": {
                return addMathBlock(ctx, args, "random");
            }
            case "round": {
                return addMathBlock(ctx, args, "round");
            }
            case "subtract": {
                return addMathBlock(ctx, args, "subtract");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult addMathBlock(SketchwareToolContext ctx, JsonObject args, String op) {
        String eventId = optString(args, "event_id");
        if (eventId == null) return err("event_id is required");
        String a = optString(args, "operand_a", "");
        String b = optString(args, "operand_b", "");
        String resultVar = optString(args, "result_var", "");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
            Object newBlockId = SketchwareApi.invoke(editor, "y", eventId, "math:" + op, a, b, resultVar);
            ctx.refreshLogicEditor();
            return ok("Added math:" + op + " block to event '" + eventId + "' (id=" + newBlockId + ").");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
