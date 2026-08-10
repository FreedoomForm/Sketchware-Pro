package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * ask_question - ask the user a question and wait for an answer.
 *
 * <p>The agent loop will pause and emit an ASK message to the UI.
 * The user's response is then injected as a user message.
 */
public final class AskQuestionTool implements SketchwareTool {

    @Override public String name() { return "ask_question"; }
    @Override public String category() { return "meta"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "Ask the user a question and wait for an answer. Use when the request is ambiguous.";
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
