package com.sketchware.ai.tools;

/**
 * Result of a tool invocation. Returned to the LLM as the {@code tool_result}
 * content block. The {@code output} text is truncated to 48 000 chars with a
 * middle-cut if longer (matching Cline's {@code MAX_READ_OUTPUT_CHARS}).
 */
public final class ToolResult {

    public static final int MAX_OUTPUT_CHARS = 48_000;

    private final boolean success;
    private final String output;
    private final String error;

    private ToolResult(boolean success, String output, String error) {
        this.success = success;
        this.output = output == null ? "" : truncate(output);
        this.error = error;
    }

    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, null, message);
    }

    /**
     * Build an error result from a Throwable, preserving the exception class
     * name, message, AND a trimmed stack trace so the LLM (and the developer
     * reading the chat log) has enough context to recover. Previously this
     * only kept {@code t.getMessage()}, which was often null for NullPointerException
     * and other runtime failures, leaving the LLM with no actionable signal.
     */
    public static ToolResult error(Throwable t) {
        if (t == null) return new ToolResult(false, null, "unknown error (null throwable)");
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName());
        if (t.getMessage() != null && !t.getMessage().isEmpty()) {
            sb.append(": ").append(t.getMessage());
        }
        // Append the top few stack frames so the LLM can see WHERE the
        // failure happened. Cap at 8 frames to avoid bloating the context.
        StackTraceElement[] trace = t.getStackTrace();
        if (trace != null && trace.length > 0) {
            sb.append("\n  at ");
            int max = Math.min(trace.length, 8);
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append("\n  at ");
                sb.append(trace[i]);
            }
            if (trace.length > max) {
                sb.append("\n  ... ").append(trace.length - max).append(" more");
            }
        }
        // If the throwable has a cause that's different from itself, include
        // it too (one level deep).
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            sb.append("\nCaused by: ").append(cause.getClass().getName());
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                sb.append(": ").append(cause.getMessage());
            }
        }
        return new ToolResult(false, null, sb.toString());
    }

    public boolean isSuccess() { return success; }
    public boolean isError() { return !success; }
    public String getOutput() { return output; }
    public String getError() { return error; }

    /** What gets serialized back to the LLM. */
    public String toLLMString() {
        if (success) return output;
        return "ERROR: " + (error == null ? "unknown" : error);
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_OUTPUT_CHARS) return s;
        int head = MAX_OUTPUT_CHARS * 2 / 3;
        int tail = MAX_OUTPUT_CHARS - head - 200;
        return s.substring(0, head)
                + "\n\n...[output truncated " + (s.length() - MAX_OUTPUT_CHARS) + " chars]...\n\n"
                + s.substring(s.length() - tail);
    }
}
