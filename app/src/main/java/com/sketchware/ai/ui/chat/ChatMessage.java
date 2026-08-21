package com.sketchware.ai.ui.chat;

import java.util.concurrent.atomic.AtomicLong;

/**
 * One row in the chat RecyclerView. Mirrors Cline's {@code ClineMessage}.
 *
 * <p>Each row has a {@code type} that determines which ViewHolder renders it.
 */
public final class ChatMessage {

    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong();

    public static final String TYPE_USER = "user";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_REASONING = "reasoning";
    public static final String TYPE_API_REQ_START = "api_req_started";
    public static final String TYPE_API_REQ_DONE = "api_req_done";
    public static final String TYPE_TOOL_CALL = "tool_call";
    public static final String TYPE_TOOL_RESULT = "tool_result";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_COMPLETION = "completion";
    public static final String TYPE_COMPACTION = "compaction";
    public static final String TYPE_FOLLOWUP = "followup";
    public static final String TYPE_USAGE = "usage";

    public String type;
    public final long ts;
    public String text;
    public String reasoning;
    public String toolName;
    public String toolArgsJson;
    public String toolResult;
    public boolean isError;
    public boolean isStreaming;
    public int inputTokens;
    public int outputTokens;
    public double cost;

    public ChatMessage(String type) {
        this.type = type;
        long now = System.currentTimeMillis();
        this.ts = LAST_TIMESTAMP.updateAndGet(previous -> Math.max(now, previous + 1L));
    }

    public static ChatMessage user(String text) {
        ChatMessage m = new ChatMessage(TYPE_USER);
        m.text = text;
        return m;
    }
    public static ChatMessage text(String text) {
        ChatMessage m = new ChatMessage(TYPE_TEXT);
        m.text = text;
        return m;
    }
    public static ChatMessage reasoning(String text) {
        ChatMessage m = new ChatMessage(TYPE_REASONING);
        m.text = text;
        return m;
    }
    public static ChatMessage apiReqStart() {
        return new ChatMessage(TYPE_API_REQ_START);
    }
    public static ChatMessage apiReqDone(int inT, int outT, double c) {
        ChatMessage m = new ChatMessage(TYPE_API_REQ_DONE);
        m.inputTokens = inT;
        m.outputTokens = outT;
        m.cost = c;
        return m;
    }
    public static ChatMessage toolCall(String name, String args) {
        ChatMessage m = new ChatMessage(TYPE_TOOL_CALL);
        m.toolName = name;
        m.toolArgsJson = args;
        return m;
    }
    public static ChatMessage toolResult(String name, String result, boolean error) {
        ChatMessage m = new ChatMessage(TYPE_TOOL_RESULT);
        m.toolName = name;
        m.toolResult = result;
        m.isError = error;
        return m;
    }
    public static ChatMessage error(String message) {
        ChatMessage m = new ChatMessage(TYPE_ERROR);
        m.text = message;
        m.isError = true;
        return m;
    }
    public static ChatMessage completion(String text) {
        ChatMessage m = new ChatMessage(TYPE_COMPLETION);
        m.text = text;
        return m;
    }
    public static ChatMessage compaction() {
        return new ChatMessage(TYPE_COMPACTION);
    }
}
