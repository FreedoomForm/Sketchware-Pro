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

    public static ToolResult error(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) msg = t.getClass().getSimpleName();
        return new ToolResult(false, null, msg);
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
