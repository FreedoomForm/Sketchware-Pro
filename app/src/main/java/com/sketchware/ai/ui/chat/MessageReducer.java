package com.sketchware.ai.ui.chat;

/**
 * Pure-function reducer that merges streaming chunks into the list of
 * chat messages. Mirrors Cline's {@code messageReducer.ts}.
 *
 * <p>Rules:
 * <ul>
 *   <li>Text delta appends to the last message if it's TYPE_TEXT.</li>
 *   <li>Reasoning delta appends to the last message if it's TYPE_REASONING.</li>
 *   <li>Tool call starts a new row.</li>
 *   <li>API request start inserts a TYPE_API_REQ_START row that is later
 *       updated with usage on completion.</li>
 * </ul>
 */
import java.util.ArrayList;
import java.util.List;

public final class MessageReducer {

    private final List<ChatMessage> messages = new ArrayList<>();

    public List<ChatMessage> getMessages() { return messages; }

    public synchronized void reset() {
        messages.clear();
    }

    public synchronized void addUserMessage(String text) {
        messages.add(ChatMessage.user(text));
    }

    public synchronized void appendText(String delta) {
        if (messages.isEmpty() || !ChatMessage.TYPE_TEXT.equals(messages.get(messages.size() - 1).type)) {
            ChatMessage m = ChatMessage.text(delta);
            m.isStreaming = true;
            messages.add(m);
        } else {
            ChatMessage m = messages.get(messages.size() - 1);
            m.text = (m.text == null ? "" : m.text) + delta;
            m.isStreaming = true;
        }
    }

    public synchronized void appendReasoning(String delta) {
        if (messages.isEmpty() || !ChatMessage.TYPE_REASONING.equals(messages.get(messages.size() - 1).type)) {
            ChatMessage m = ChatMessage.reasoning(delta);
            m.isStreaming = true;
            messages.add(m);
        } else {
            ChatMessage m = messages.get(messages.size() - 1);
            m.text = (m.text == null ? "" : m.text) + delta;
            m.isStreaming = true;
        }
    }

    public synchronized void finishStreaming() {
        if (!messages.isEmpty()) {
            messages.get(messages.size() - 1).isStreaming = false;
        }
    }

    public synchronized void addApiReqStart() {
        messages.add(ChatMessage.apiReqStart());
    }

    public synchronized void addUsage(int inputTokens, int outputTokens, double cost) {
        // Find the last api_req_started and update it, or add a new usage row.
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (ChatMessage.TYPE_API_REQ_START.equals(m.type)) {
                m.type = ChatMessage.TYPE_API_REQ_DONE;
                m.inputTokens = inputTokens;
                m.outputTokens = outputTokens;
                m.cost = cost;
                return;
            }
        }
        messages.add(ChatMessage.apiReqDone(inputTokens, outputTokens, cost));
    }

    public synchronized void addToolCall(String name, String args) {
        messages.add(ChatMessage.toolCall(name, args));
    }

    public synchronized void addToolResult(String name, String result, boolean error) {
        messages.add(ChatMessage.toolResult(name, result, error));
    }

    public synchronized void addError(String msg) {
        messages.add(ChatMessage.error(msg));
    }

    public synchronized void addCompletion(String text) {
        messages.add(ChatMessage.completion(text));
    }

    public synchronized void addCompaction() {
        messages.add(ChatMessage.compaction());
    }
}
