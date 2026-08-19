package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * control_flow — universal tool for block operations.
 *
 * <p>Replaces 11 stubs: control_flow:if, control_flow:if_else, control_flow:for, control_flow:for_each, control_flow:while, control_flow:switch, control_flow:break, control_flow:continue, control_flow:return, control_flow:throw, control_flow:try_catch
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ControlFlowTool extends UniversalTool {

    public ControlFlowTool() {
        super("control_flow",
                "Add a control-flow block to an event handler: if, if_else, for, for_each, while, switch, break, continue, return, throw, try_catch.",
                "block", false, false,
"if",
                "if_else",
                "for",
                "for_each",
                "while",
                "switch",
                "break",
                "continue",
                "return",
                "throw",
                "try_catch");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_event_id = new JsonObject();
        p_event_id.addProperty("type", "string");
        p_event_id.addProperty("description", "Event handler ID.");
        props.add("event_id", p_event_id);
        JsonObject p_condition = new JsonObject();
        p_condition.addProperty("type", "string");
        p_condition.addProperty("description", "(if/while/for) Condition expression.");
        props.add("condition", p_condition);
        JsonObject p_variable = new JsonObject();
        p_variable.addProperty("type", "string");
        p_variable.addProperty("description", "(for/for_each/switch) Loop/switch variable.");
        props.add("variable", p_variable);
        JsonObject p_iterable = new JsonObject();
        p_iterable.addProperty("type", "string");
        p_iterable.addProperty("description", "(for_each) Iterable expression.");
        props.add("iterable", p_iterable);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(return/throw) Value or exception to return/throw.");
        props.add("value", p_value);
        JsonObject p_parent_block_id = new JsonObject();
        p_parent_block_id.addProperty("type", "string");
        p_parent_block_id.addProperty("description", "Parent block ID (for nesting). Empty = top-level.");
        props.add("parent_block_id", p_parent_block_id);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "if": {
                return addControlBlock(ctx, args, "if");
            }
            case "if_else": {
                return addControlBlock(ctx, args, "ifElse");
            }
            case "for": {
                return addControlBlock(ctx, args, "for");
            }
            case "for_each": {
                return addControlBlock(ctx, args, "forEach");
            }
            case "while": {
                return addControlBlock(ctx, args, "while");
            }
            case "switch": {
                return addControlBlock(ctx, args, "switch");
            }
            case "break": {
                return addControlBlock(ctx, args, "break");
            }
            case "continue": {
                return addControlBlock(ctx, args, "continue");
            }
            case "return": {
                return addControlBlock(ctx, args, "return");
            }
            case "throw": {
                return addControlBlock(ctx, args, "throw");
            }
            case "try_catch": {
                return addControlBlock(ctx, args, "tryCatch");
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    private ToolResult addControlBlock(SketchwareToolContext ctx, JsonObject args, String blockType) {
        String eventId = optString(args, "event_id");
        if (eventId == null) return err("event_id is required");
        String parentBlockId = optString(args, "parent_block_id", "");
        String condition = optString(args, "condition", "");
        String variable = optString(args, "variable", "");
        String iterable = optString(args, "iterable", "");
        String value = optString(args, "value", "");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
            Object newBlockId = SketchwareApi.invoke(editor, "x", eventId, blockType, parentBlockId,
                    condition, variable, iterable, value);
            ctx.refreshLogicEditor();
            return ok("Added " + blockType + " block to event '" + eventId + "' (id=" + newBlockId + ").");
        } catch (Throwable t) { return ToolResult.error(t); }
    }
}
