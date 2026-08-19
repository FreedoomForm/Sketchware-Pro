package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * string_operation — universal tool for adding string operation blocks to event handlers.
 *
 * <p>Actions (13):
 * <ul>
 *   <li><b>concat</b>, <b>contains</b>, <b>length</b>, <b>replace</b>, <b>split</b>,
 *       <b>substring</b>, <b>to_lower</b>, <b>to_upper</b> — original 8</li>
 *   <li><b>equals</b> — exact string equality (returns boolean)</li>
 *   <li><b>starts_with</b> — prefix check (returns boolean)</li>
 *   <li><b>ends_with</b> — suffix check (returns boolean)</li>
 *   <li><b>trim</b> — remove leading/trailing whitespace</li>
 *   <li><b>index_of</b> — find substring position (returns int)</li>
 * </ul>
 *
 * <p>All actions add a {@code string:opName} block to the event handler by invoking
 * the obfuscated project-file editor returned by {@code jC.b(sc_id)} via reflection
 * (method {@code z}=addStringBlock). The op name is passed as the block spec prefix
 * (e.g. {@code "string:equals"}). The Sketchware code generator (a.a.a.Fx) maps
 * these to the corresponding Java expressions:
 * <ul>
 *   <li>{@code string:equals} → {@code a.equals(b)}</li>
 *   <li>{@code string:startsWith} → {@code a.startsWith(b)}</li>
 *   <li>{@code string:endsWith} → {@code a.endsWith(b)}</li>
 *   <li>{@code string:trim} → {@code a.trim()}</li>
 *   <li>{@code string:index} → {@code a.indexOf(b)}</li>
 * </ul>
 *
 * <p>For unary operations (trim), the second operand is ignored. For binary
 * operations (equals/starts_with/ends_with/index_of), operand_a is the string to
 * search within, and operand_b is the substring to find/check.
 */
public final class StringOperationTool extends UniversalTool {

    public StringOperationTool() {
        super("string_operation",
                "Add a string operation block to an event handler: concat, contains, "
                        + "length, replace, split, substring, to_lower, to_upper, "
                        + "equals, starts_with, ends_with, trim, index_of.",
                "block", false, false,
                "concat",
                "contains",
                "length",
                "replace",
                "split",
                "substring",
                "to_lower",
                "to_upper",
                "equals",
                "starts_with",
                "ends_with",
                "trim",
                "index_of");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID.");
        props.add("event_id", p_event_id);
        JsonObject p_operand_a = new JsonObject();
        p_operand_a.addProperty("type", "string");
        p_operand_a.addProperty("description", "First string operand (the string to operate on).");
        props.add("operand_a", p_operand_a);
        JsonObject p_operand_b = new JsonObject();
        p_operand_b.addProperty("type", "string");
        p_operand_b.addProperty("description", "Second string operand (for concat/contains/replace/split/equals/starts_with/ends_with/index_of).");
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
            case "concat":
                return addStringBlock(ctx, args, "concat");
            case "contains":
                return addStringBlock(ctx, args, "contains");
            case "length":
                return addStringBlock(ctx, args, "length");
            case "replace":
                return addStringBlock(ctx, args, "replace");
            case "split":
                return addStringBlock(ctx, args, "split");
            case "substring":
                return addStringBlock(ctx, args, "substring");
            case "to_lower":
                return addStringBlock(ctx, args, "toLower");
            case "to_upper":
                return addStringBlock(ctx, args, "toUpper");
            case "equals":
                return addStringBlock(ctx, args, "equals");
            case "starts_with":
                return addStringBlock(ctx, args, "startsWith");
            case "ends_with":
                return addStringBlock(ctx, args, "endsWith");
            case "trim":
                return addStringBlock(ctx, args, "trim");
            case "index_of":
                return addStringBlock(ctx, args, "index");
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
