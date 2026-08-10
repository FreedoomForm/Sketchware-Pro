package com.sketchware.ai.agent;

/**
 * Listener interface for receiving streaming events from {@link AgentRuntime}.
 * All callbacks are invoked on a background thread; UI listeners must post
 * to the main thread themselves.
 */
public interface AgentListener {
    /** Streamed text delta from the assistant. */
    void onTextDelta(String delta);

    /** Streamed reasoning (thinking) text delta. */
    void onReasoningDelta(String delta);

    /** A complete set of tool calls has been received and will be executed. */
    void onToolCalls(java.util.List<AgentMessage.ToolCall> calls);

    /** A single tool has started executing. */
    void onToolStart(String toolCallId, String toolName, String argsJson);

    /** A single tool finished executing. */
    void onToolResult(String toolCallId, AgentMessage.ToolResultContent result);

    /** Token usage stats from the provider. */
    void onUsage(int inputTokens, int outputTokens, int reasoningTokens, double cost);

    /** The run completed normally (assistant produced a final text with no tool calls). */
    void onComplete(String finalText);

    /** A recoverable error - the run keeps going (e.g. one tool failed). */
    void onWarning(String message);

    /** A non-recoverable error - the run has stopped. */
    void onError(Throwable error);

    /** The agent loop hit max iterations. */
    void onMaxIterationsReached(int max);
}
