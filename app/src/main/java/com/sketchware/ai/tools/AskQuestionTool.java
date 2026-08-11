package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * ask_question - ask the user a question and wait for an answer.
 *
 * <p>When the LLM invokes this tool, the question text is returned to the
 * LLM as a tool_result (prefixed with "QUESTION: "). The LLM should then
 * stop and wait for the user's next message, which will be added to the
 * conversation as a regular user message.
 *
 * <p>The tool_result is also surfaced in the chat UI as a regular tool
 * result card, so the user sees the question and can type a reply in the
 * chat input.
 */
public final class AskQuestionTool implements SketchwareTool {

    @Override public String name() { return "ask_question"; }
    @Override public String category() { return "meta"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Ask the user a question and wait for an answer. Use when the request is ambiguous. "
                + "The question is returned as a tool_result; stop and wait for the user's next message.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject question = new JsonObject();
        question.addProperty("type", "string");
        question.addProperty("description", "Question to ask the user");
        props.add("question", question);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("question");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String question = args.has("question") ? args.get("question").getAsString() : "Please answer the question above.";
        return ToolResult.success("QUESTION: " + question);
    }
}
