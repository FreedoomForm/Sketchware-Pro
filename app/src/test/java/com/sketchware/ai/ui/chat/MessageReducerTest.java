package com.sketchware.ai.ui.chat;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link MessageReducer}.
 *
 * <p>The reducer is a pure function (port of Cline's {@code messageReducer.ts});
 * these tests verify that streaming chunks accumulate correctly into the
 * list of chat messages.
 */
public class MessageReducerTest {

    @Test public void startsEmpty() {
        MessageReducer r = new MessageReducer();
        assertThat(r.getMessages()).isEmpty();
    }

    @Test public void addUserMessageCreatesUserRow() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("hello");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_USER);
        assertThat(r.getMessages().get(0).text).isEqualTo("hello");
    }

    @Test public void appendTextCreatesNewTextRowIfLastIsNotText() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("hello");
        r.appendText("Hi");
        assertThat(r.getMessages()).hasSize(2);
        assertThat(r.getMessages().get(1).type).isEqualTo(ChatMessage.TYPE_TEXT);
        assertThat(r.getMessages().get(1).text).isEqualTo("Hi");
        assertThat(r.getMessages().get(1).isStreaming).isTrue();
    }

    @Test public void appendTextAccumulatesIntoLastTextRow() {
        MessageReducer r = new MessageReducer();
        r.appendText("Hello");
        r.appendText(" world");
        r.appendText("!");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).text).isEqualTo("Hello world!");
    }

    @Test public void appendReasoningCreatesNewReasoningRowIfLastIsNotReasoning() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("hello");
        r.appendReasoning("thinking...");
        assertThat(r.getMessages()).hasSize(2);
        assertThat(r.getMessages().get(1).type).isEqualTo(ChatMessage.TYPE_REASONING);
    }

    @Test public void appendReasoningAccumulatesIntoLastReasoningRow() {
        MessageReducer r = new MessageReducer();
        r.appendReasoning("First, ");
        r.appendReasoning("I think...");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).text).isEqualTo("First, I think...");
    }

    @Test public void textAndReasoningAlternate() {
        MessageReducer r = new MessageReducer();
        r.appendReasoning("Let me think.");
        r.appendText("Answer.");
        r.appendReasoning("More thinking.");
        r.appendText(" Updated answer.");
        assertThat(r.getMessages()).hasSize(4);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_REASONING);
        assertThat(r.getMessages().get(1).type).isEqualTo(ChatMessage.TYPE_TEXT);
        assertThat(r.getMessages().get(2).type).isEqualTo(ChatMessage.TYPE_REASONING);
        assertThat(r.getMessages().get(3).type).isEqualTo(ChatMessage.TYPE_TEXT);
        assertThat(r.getMessages().get(1).text).isEqualTo("Answer.");
        assertThat(r.getMessages().get(3).text).isEqualTo(" Updated answer.");
    }

    @Test public void finishStreamingClearsFlag() {
        MessageReducer r = new MessageReducer();
        r.appendText("hi");
        assertThat(r.getMessages().get(0).isStreaming).isTrue();
        r.finishStreaming();
        assertThat(r.getMessages().get(0).isStreaming).isFalse();
    }

    @Test public void addToolCallCreatesToolRow() {
        MessageReducer r = new MessageReducer();
        r.addToolCall("view_add_widget", "{\"widget_type\":\"Button\"}");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_TOOL_CALL);
        assertThat(r.getMessages().get(0).toolName).isEqualTo("view_add_widget");
        assertThat(r.getMessages().get(0).toolArgsJson).isEqualTo("{\"widget_type\":\"Button\"}");
    }

    @Test public void addToolResultCreatesResultRow() {
        MessageReducer r = new MessageReducer();
        r.addToolResult("view_add_widget", "Added Button with id=button1", false);
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_TOOL_RESULT);
        assertThat(r.getMessages().get(0).isError).isFalse();
        assertThat(r.getMessages().get(0).toolResult).contains("Added Button");
    }

    @Test public void addToolResultWithErrorFlag() {
        MessageReducer r = new MessageReducer();
        r.addToolResult("library_enable", "library_required", true);
        assertThat(r.getMessages().get(0).isError).isTrue();
        assertThat(r.getMessages().get(0).toolResult).isEqualTo("library_required");
    }

    @Test public void addErrorCreatesErrorRow() {
        MessageReducer r = new MessageReducer();
        r.addError("HTTP 500");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_ERROR);
        assertThat(r.getMessages().get(0).text).isEqualTo("HTTP 500");
        assertThat(r.getMessages().get(0).isError).isTrue();
    }

    @Test public void addCompletionCreatesCompletionRow() {
        MessageReducer r = new MessageReducer();
        r.addCompletion("All done!");
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_COMPLETION);
        assertThat(r.getMessages().get(0).text).isEqualTo("All done!");
    }

    @Test public void addApiReqStartCreatesApiReqRow() {
        MessageReducer r = new MessageReducer();
        r.addApiReqStart();
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_API_REQ_START);
    }

    @Test public void addUsageUpdatesLastApiReqStart() {
        MessageReducer r = new MessageReducer();
        r.addApiReqStart();
        r.addUsage(100, 50, 0.15);
        assertThat(r.getMessages()).hasSize(1);
        // The api_req_started row should be mutated to api_req_done.
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_API_REQ_DONE);
        assertThat(r.getMessages().get(0).inputTokens).isEqualTo(100);
        assertThat(r.getMessages().get(0).outputTokens).isEqualTo(50);
        assertThat(r.getMessages().get(0).cost).isEqualTo(0.15);
    }

    @Test public void addUsageWithoutPriorApiReqStartCreatesNewRow() {
        MessageReducer r = new MessageReducer();
        r.addUsage(100, 50, 0.15);
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_API_REQ_DONE);
    }

    @Test public void addCompactionCreatesCompactionRow() {
        MessageReducer r = new MessageReducer();
        r.addCompaction();
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.getMessages().get(0).type).isEqualTo(ChatMessage.TYPE_COMPACTION);
    }

    @Test public void resetClearsAllMessages() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("hello");
        r.appendText("hi");
        r.addToolCall("test", "{}");
        r.reset();
        assertThat(r.getMessages()).isEmpty();
    }

    @Test public void actionOperationsResolvePromptAndMutateRows() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("original prompt");
        ChatMessage user = r.getMessages().get(0);
        r.appendText("old answer");
        ChatMessage answer = r.getMessages().get(1);

        assertThat(r.userPromptFor(answer)).isEqualTo("original prompt");
        assertThat(r.editMessage(user.ts, "edited prompt")).isTrue();
        assertThat(r.getMessages().get(0).text).isEqualTo("edited prompt");
        assertThat(r.deleteMessage(answer.ts)).isTrue();
        assertThat(r.getMessages()).hasSize(1);
        assertThat(r.deleteMessage(answer.ts)).isFalse();
    }

    @Test public void removeTurnRemovesRowsUntilNextUserMessage() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("first");
        r.appendText("first answer");
        r.addToolCall("view_list_widgets", "{}");
        r.addUserMessage("second");
        r.appendText("second answer");

        ChatMessage firstAnswer = r.getMessages().get(1);
        assertThat(r.removeTurn(firstAnswer)).isTrue();
        assertThat(r.getMessages()).hasSize(2);
        assertThat(r.getMessages().get(0).text).isEqualTo("second");
        assertThat(r.getMessages().get(1).text).isEqualTo("second answer");
    }

    @Test public void fullConversationFlow() {
        MessageReducer r = new MessageReducer();
        r.addUserMessage("Add a button");
        r.addApiReqStart();
        r.appendReasoning("User wants a button. I'll add it to the layout.");
        r.appendText("Adding a button now.");
        r.addToolCall("view_add_widget", "{\"widget_type\":\"Button\"}");
        r.addToolResult("view_add_widget", "Added Button with id=button1", false);
        r.addUsage(150, 75, 0.02);
        r.finishStreaming();
        r.addCompletion("Added a button to the layout.");

        List<ChatMessage> messages = r.getMessages();
        // 7 rows: addUsage updates the existing api_req_started row in place
        // (changing its type to api_req_done), it does NOT add a new row.
        assertThat(messages).hasSize(7);
        assertThat(messages.get(0).type).isEqualTo(ChatMessage.TYPE_USER);
        assertThat(messages.get(1).type).isEqualTo(ChatMessage.TYPE_API_REQ_DONE);
        assertThat(messages.get(2).type).isEqualTo(ChatMessage.TYPE_REASONING);
        assertThat(messages.get(3).type).isEqualTo(ChatMessage.TYPE_TEXT);
        assertThat(messages.get(4).type).isEqualTo(ChatMessage.TYPE_TOOL_CALL);
        assertThat(messages.get(5).type).isEqualTo(ChatMessage.TYPE_TOOL_RESULT);
        assertThat(messages.get(6).type).isEqualTo(ChatMessage.TYPE_COMPLETION);
        // Verify usage values were merged into the api_req_done row.
        assertThat(messages.get(1).inputTokens).isEqualTo(150);
        assertThat(messages.get(1).outputTokens).isEqualTo(75);
        assertThat(messages.get(1).cost).isEqualTo(0.02);
        assertThat(messages.get(6).isStreaming).isFalse();
    }
}
