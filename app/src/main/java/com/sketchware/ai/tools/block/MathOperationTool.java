package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * math_operation — universal tool for adding math operation blocks to event handlers.
 *
 * <p>Actions (18):
 * <ul>
 *   <li><b>abs</b>, <b>add</b>, <b>divide</b>, <b>max</b>, <b>min</b>, <b>modulo</b>,
 *       <b>multiply</b>, <b>random</b>, <b>round</b>, <b>subtract</b> — original 10</li>
 *   <li><b>sqrt</b> — square root (unary)</li>
 *   <li><b>pow</b> — power (binary)</li>
 *   <li><b>log</b> — natural logarithm (unary)</li>
 *   <li><b>sin</b>, <b>cos</b> — trigonometric (unary)</li>
 *   <li><b>ceil</b>, <b>floor</b> — rounding (unary)</li>
 *   <li><b>format_decimal</b> — format number with DecimalFormat pattern (binary)</li>
 * </ul>
 *
 * <p>All actions add a {@code math:opName} block to the event handler by invoking
 * the obfuscated project-file editor returned by {@code jC.b(sc_id)} via reflection
 * (method {@code y}=addMathBlock). The op name is passed as the block spec prefix
 * (e.g. {@code "math:sqrt"}). The Sketchware code generator (a.a.a.Fx) maps these
 * to the corresponding Java expressions (Math.sqrt, Math.pow, Math.sin, etc.).
 *
 * <p>For unary operations (sqrt, log, sin, cos, ceil, floor), the second operand
 * is ignored. For binary operations (pow, format_decimal), both operands are used.
 * For format_decimal, operand_a is the number and operand_b is the DecimalFormat
 * pattern string (e.g. "#.00").
 */
public final class MathOperationTool extends UniversalTool {

    public MathOperationTool() {
        super("math_operation",
                "Add a math operation block to an event handler: abs, add, divide, "
                        + "max, min, modulo, multiply, random, round, subtract, "
                        + "sqrt, pow, log, sin, cos, ceil, floor, format_decimal.",
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
                "subtract",
                "sqrt",
                "pow",
                "log",
                "sin",
                "cos",
                "ceil",
                "floor",
                "format_decimal");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID.");
        props.add("event_id", p_event_id);
        JsonObject p_operand_a = new JsonObject();
        p_operand_a.addProperty("type", "string");
        p_operand_a.addProperty("description", "First operand (variable name or literal). "
                + "For format_decimal, this is the number to format.");
        props.add("operand_a", p_operand_a);
        JsonObject p_operand_b = new JsonObject();
        p_operand_b.addProperty("type", "string");
        p_operand_b.addProperty("description", "Second operand (for binary ops). "
                + "For format_decimal, this is the DecimalFormat pattern (e.g. \"#.00\").");
        props.add("operand_b", p_operand_b);
        JsonObject p_result_var = new JsonObject();
        p_result_var.addProperty("type", "string");
        p_result_var.addProperty("description", "Variable name to store the result.");
        props.add("result_var", p_result_var);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "abs":
                return addMathBlock(ctx, args, "abs");
            case "add":
                return addMathBlock(ctx, args, "add");
            case "divide":
                return addMathBlock(ctx, args, "divide");
            case "max":
                return addMathBlock(ctx, args, "max");
            case "min":
                return addMathBlock(ctx, args, "min");
            case "modulo":
                return addMathBlock(ctx, args, "modulo");
            case "multiply":
                return addMathBlock(ctx, args, "multiply");
            case "random":
                return addMathBlock(ctx, args, "random");
            case "round":
                return addMathBlock(ctx, args, "round");
            case "subtract":
                return addMathBlock(ctx, args, "subtract");
            case "sqrt":
                return addMathBlock(ctx, args, "sqrt");
            case "pow":
                return addMathBlock(ctx, args, "pow");
            case "log":
                return addMathBlock(ctx, args, "log");
            case "sin":
                return addMathBlock(ctx, args, "sin");
            case "cos":
                return addMathBlock(ctx, args, "cos");
            case "ceil":
                return addMathBlock(ctx, args, "ceil");
            case "floor":
                return addMathBlock(ctx, args, "floor");
            case "format_decimal":
                // toStringFormat is the opcode that generates
                //   new DecimalFormat(pattern).format(number)
                return addMathBlock(ctx, args, "toStringFormat");
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
