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

    /**
     * Return a <b>snapshot copy</b> of the current message list.
     *
     * <p>Callers (notably {@code ChatAdapter.submitList(...)}) must pass a
     * different list reference each time, otherwise {@code ListAdapter}'s
     * {@code submitList} short-circuits with {@code list == currentList} and
     * skips the DiffUtil pass entirely — the UI never updates during
     * streaming. Returning a shallow copy guarantees a fresh wrapper while
     * keeping the {@link ChatMessage} instances themselves shared (so
     * in-place mutations are visible once DiffUtil flags the row as changed).
     */
    public List<ChatMessage> getMessages() {
        synchronized (this) {
            return new ArrayList<>(messages);
        }
    }

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

    /**
     * Whether the last message in the list is an assistant streaming-text row
     * ({@link ChatMessage#TYPE_TEXT}). Used by {@code ChatFragment} to decide
     * whether {@code onComplete(finalText)} / {@code onAborted(partialText)}
     * should append a separate completion row: if the streamed text is already
     * on screen, appending the same text again would duplicate it.
     */
    public synchronized boolean lastMessageIsStreamingText() {
        if (messages.isEmpty()) return false;
        return ChatMessage.TYPE_TEXT.equals(messages.get(messages.size() - 1).type);
    }
}
