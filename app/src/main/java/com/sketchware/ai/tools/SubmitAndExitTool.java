package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * submit_and_exit - signal that the task is complete.
 *
 * <p>When the LLM invokes this tool, the agent loop terminates after the
 * tool_result is added to the conversation history. The summary text is
 * emitted to the UI as the final assistant message via
 * {@code AgentListener.onComplete(summary)}.
 *
 * <p>The LLM should call this tool ONCE at the end of a task, after all
 * other tool calls have completed. Do not call this tool mid-task.
 */
public final class SubmitAndExitTool implements SketchwareTool {

    @Override public String name() { return "submit_and_exit"; }
    @Override public String category() { return "meta"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Signal that the task is complete. Provide a brief summary of what was done. "
                + "The agent loop will stop after this tool is called.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject summary = new JsonObject();
        summary.addProperty("type", "string");
        summary.addProperty("description", "Brief summary of what was accomplished");
        props.add("summary", summary);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("summary");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String summary = args.has("summary") ? args.get("summary").getAsString() : "Task complete.";
        return ToolResult.success("COMPLETION: " + summary);
    }
}
