package com.sketchware.ai.agent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One message in the conversation history, mirroring the AI-SDK canonical
 * message shape used by Cline.
 *
 * <p>Roles:
 * <ul>
 *   <li><b>system</b> - the assembled system prompt (only the first message)</li>
 *   <li><b>user</b> - either plain text or a tool-result payload</li>
 *   <li><b>assistant</b> - either plain text or text+tool_calls+reasoning</li>
 * </ul>
 */
public final class AgentMessage {
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    public final String role;
    public final String text;
    public final String reasoning;
    public final List<ToolCall> toolCalls;
    public final List<ToolResultContent> toolResults;
    public final List<String> images;

    private AgentMessage(String role,
                         String text,
                         String reasoning,
                         List<ToolCall> toolCalls,
                         List<ToolResultContent> toolResults,
                         List<String> images) {
        this.role = role;
        this.text = text;
        this.reasoning = reasoning;
        this.toolCalls = toolCalls;
        this.toolResults = toolResults;
        this.images = images;
    }

    public static AgentMessage system(String content) {
        return new AgentMessage(ROLE_SYSTEM, content, null, null, null, null);
    }

    public static AgentMessage user(String content) {
        return new AgentMessage(ROLE_USER, content, null, null, null, null);
    }

    public static AgentMessage userWithImages(String content, List<String> base64Images) {
        return new AgentMessage(ROLE_USER, content, null, null, null, base64Images);
    }

    public static AgentMessage assistant(String text, String reasoning, List<ToolCall> toolCalls) {
        return new AgentMessage(ROLE_ASSISTANT, text, reasoning, toolCalls, null, null);
    }

    public static AgentMessage toolResult(List<ToolResultContent> results) {
        return new AgentMessage(ROLE_USER, null, null, null, results, null);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasToolResults() {
        return toolResults != null && !toolResults.isEmpty();
    }

    public static final class ToolCall {
        public final String id;
        public final String name;
        public final String argumentsJson; // raw JSON string

        public ToolCall(String id, String name, String argumentsJson) {
            this.id = id;
            this.name = name;
            this.argumentsJson = argumentsJson;
        }
    }

    public static final class ToolResultContent {
        public final String toolCallId;
        public final String toolName;
        public final String output;
        public final boolean isError;

        public ToolResultContent(String toolCallId, String toolName, String output, boolean isError) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.output = output;
            this.isError = isError;
        }
    }

    /**
     * Estimate the number of tokens this message consumes.
     * Uses a rough heuristic: 1 token ~= 4 chars.
     */
    public int estimateTokens() {
        int chars = 0;
        if (text != null) chars += text.length();
        if (reasoning != null) chars += reasoning.length();
        if (toolCalls != null) for (ToolCall tc : toolCalls) {
            chars += (tc.name == null ? 0 : tc.name.length());
            chars += (tc.argumentsJson == null ? 0 : tc.argumentsJson.length());
            chars += (tc.id == null ? 0 : tc.id.length());
        }
        if (toolResults != null) for (ToolResultContent r : toolResults) {
            chars += (r.output == null ? 0 : r.output.length());
            chars += (r.toolName == null ? 0 : r.toolName.length());
            chars += (r.toolCallId == null ? 0 : r.toolCallId.length());
        }
        return Math.max(1, chars / 4);
    }
}
