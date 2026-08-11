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

    /**
     * The run was aborted by the user (or by an external {@code abort()} call).
     * No further callbacks will be issued for this run. The listener should
     * finalise any in-flight UI state (e.g. hide the stop button, mark the
     * streaming message as finished).
     *
     * <p>Default implementation delegates to {@link #onComplete(String)} with
     * whatever partial text was accumulated, so simple listeners that only
     * override {@code onComplete} still get a clean shutdown signal.
     */
    default void onAborted(String partialText) {
        onComplete(partialText == null ? "" : partialText);
    }

    /**
     * Request user approval for a tool call that the permission gate has
     * marked as {@link com.sketchware.ai.tools.ToolPermissionGate.Decision#REQUIRE_APPROVAL}.
     *
     * <p>This is invoked synchronously on the agent's background thread. The
     * listener MUST NOT block the UI thread from inside this callback (post
     * a dialog and block on a CountDownLatch, or just return a cached
     * decision).
     *
     * <p>Default implementation returns {@code true} (auto-approve) to preserve
     * the previous MVP behaviour where mutating tools in ACT mode were
     * silently auto-approved. Override this to show a real approval dialog.
     *
     * @return {@code true} to execute the tool, {@code false} to deny (the
     *     LLM will receive a "permission denied" tool_result).
     */
    default boolean requestApproval(AgentMessage.ToolCall call) {
        return true;
    }

    /** A recoverable error - the run keeps going (e.g. one tool failed). */
    void onWarning(String message);

    /** A non-recoverable error - the run has stopped. */
    void onError(Throwable error);

    /** The agent loop hit max iterations. */
    void onMaxIterationsReached(int max);
}
