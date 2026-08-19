package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serialize {@link AgentMessage} history into a transcript string suitable
 * for compaction summarizer input. Ported from oh-my-pi's
 * {@code packages/agent/src/compaction/utils.ts} (serializeConversation).
 *
 * <p>Features ported:
 * <ul>
 *   <li><b>Role labels</b> — {@code [User]}, {@code [Assistant]},
 *       {@code [Think]}, {@code [Tool Call]}, {@code [Tool Result]}.
 *       Mirrors the legacy default branch of the TypeScript serializer.</li>
 *   <li><b>Tool result truncation</b> — long tool outputs are truncated
 *       head+tail to {@link #TOOL_RESULT_MAX_CHARS} characters with a
 *       "[... N more characters truncated]" marker. Prevents a 50KB
 *       {@code list_files} dump from blowing the summarizer's input.</li>
 *   <li><b>Useless-result elision</b> — tool results flagged as
 *       contextually useless (zero-match searches, empty inbox drains)
 *       are dropped from the serialized text. The flag is read from
 *       {@link AgentMessage.ToolResultContent#isError}=false AND the
 *       output text matching {@link #USELESS_NOTICE}.</li>
 *   <li><b>Tool-call argument capping</b> — per-value and per-call caps
 *       so a single huge {@code write_file} payload doesn't dominate
 *       the summary input.</li>
 *   <li><b>Tool call/result pairing</b> — useless tool calls (paired
 *       with a useless result) are also dropped from the assistant turn.</li>
 * </ul>
 */
public final class ConversationSerializer {

    /** Maximum characters of a tool result retained in summary input. */
    static final int TOOL_RESULT_MAX_CHARS = 2000;

    /** Per-value cap for tool-call arguments (single JSON value). */
    private static final int TOOL_ARG_MAX_CHARS = 500;

    /** Per-call cap for tool-call arguments (sum across all args). */
    private static final int TOOL_CALL_MAX_CHARS = 2000;

    /** Head-to-tail ratio for truncation. 0.6 = 60% head, 40% tail. */
    private static final double TRUNCATE_HEAD_RATIO = 0.6;

    /** Placeholder for elided useless results. Mirrors USELESS_NOTICE. */
    public static final String USELESS_NOTICE = "[Uneventful result elided]";

    private ConversationSerializer() {}

    /**
     * Serialize the conversation to a transcript string.
     *
     * @param messages the conversation to serialize. Must already be in
     *                 chronological order. System messages are skipped
     *                 (the summarizer has its own system prompt).
     */
    public static String serialize(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";

        // First pass: identify useless tool-call IDs so the corresponding
        // tool calls can also be elided from assistant turns.
        Set<String> uselessCallIds = new HashSet<>();
        for (AgentMessage m : messages) {
            if (m.hasToolResults()) {
                for (AgentMessage.ToolResultContent r : m.toolResults) {
                    if (!r.isError && USELESS_NOTICE.equals(r.output)) {
                        if (r.toolCallId != null) uselessCallIds.add(r.toolCallId);
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (AgentMessage m : messages) {
            // Skip system messages — summarizer has its own system prompt.
            if (AgentMessage.ROLE_SYSTEM.equals(m.role)) continue;

            String block = serializeMessage(m, uselessCallIds);
            if (block == null || block.isEmpty()) continue;
            if (!first) sb.append("\n\n");
            sb.append(block);
            first = false;
        }
        return sb.toString();
    }

    private static String serializeMessage(AgentMessage m, Set<String> uselessCallIds) {
        StringBuilder sb = new StringBuilder();
        if (AgentMessage.ROLE_USER.equals(m.role)) {
            // User messages can be either plain text or tool results.
            if (m.hasToolResults()) {
                for (AgentMessage.ToolResultContent r : m.toolResults) {
                    if (uselessCallIds.contains(r.toolCallId)) continue;
                    String text = r.output == null ? "" : r.output;
                    if (text.isEmpty()) continue;
                    sb.append("[Tool Result]: ")
                      .append(truncateToolResult(text))
                      .append('\n');
                }
                if (sb.length() == 0) return null;
                // Trim trailing newline.
                return sb.toString().trim();
            }
            if (m.text != null && !m.text.isEmpty()) {
                return "[User]: " + m.text;
            }
            return null;
        }
        if (AgentMessage.ROLE_ASSISTANT.equals(m.role)) {
            // Reasoning first (matches oh-my-pi ordering).
            if (m.reasoning != null && !m.reasoning.isEmpty()) {
                sb.append("[Think]: ").append(m.reasoning).append("\n\n");
            }
            if (m.text != null && !m.text.isEmpty()) {
                sb.append("[Assistant]: ").append(m.text).append("\n\n");
            }
            if (m.toolCalls != null) {
                StringBuilder calls = new StringBuilder();
                for (AgentMessage.ToolCall tc : m.toolCalls) {
                    if (uselessCallIds.contains(tc.id)) continue;
                    if (calls.length() > 0) calls.append("; ");
                    calls.append(tc.name).append('(')
                         .append(renderToolCallArgs(tc.argumentsJson))
                         .append(')');
                }
                if (calls.length() > 0) {
                    sb.append("[Tool Call]: ").append(calls);
                }
            }
            String result = sb.toString().trim();
            return result.isEmpty() ? null : result;
        }
        // Unknown role — render defensively.
        if (m.text != null && !m.text.isEmpty()) {
            return "[" + m.role + "]: " + m.text;
        }
        return null;
    }

    /**
     * Truncate a tool result to {@link #TOOL_RESULT_MAX_CHARS} characters,
     * keeping the head and tail in a 60/40 ratio. Mirrors
     * {@code truncateToolResultForSummary}.
     */
    static String truncateToolResult(String text) {
        if (text == null) return "";
        if (text.length() <= TOOL_RESULT_MAX_CHARS) return text;
        int headLen = (int) (TOOL_RESULT_MAX_CHARS * TRUNCATE_HEAD_RATIO);
        int tailLen = TOOL_RESULT_MAX_CHARS - headLen;
        int truncatedChars = text.length() - TOOL_RESULT_MAX_CHARS;
        return text.substring(0, headLen)
            + "\n\n[... " + truncatedChars + " more characters truncated]\n\n"
            + text.substring(text.length() - tailLen);
    }

    /**
     * Render tool-call arguments as {@code key=value} pairs with per-value
     * and per-call caps. Mirrors {@code renderToolCalls} but with the
     * snapcompact-style value capping folded in.
     */
    private static String renderToolCallArgs(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isEmpty()) return "";
        // Parse JSON defensively; if parsing fails, fall back to the raw
        // string with a per-call cap.
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject obj = new JSONObject(argumentsJson);
            JSONArray keys = obj.names();
            if (keys == null) return "";
            for (int i = 0; i < keys.length(); i++) {
                if (sb.length() > 0) sb.append(", ");
                String key = keys.getString(i);
                sb.append(key).append('=');
                String valueStr = renderJsonValue(obj.get(key));
                sb.append(capValue(valueStr));
                if (sb.length() > TOOL_CALL_MAX_CHARS) {
                    sb.append(" [...truncated]");
                    break;
                }
            }
        } catch (JSONException e) {
            String raw = argumentsJson.length() > TOOL_CALL_MAX_CHARS
                ? argumentsJson.substring(0, TOOL_CALL_MAX_CHARS) + " [...truncated]"
                : argumentsJson;
            return raw;
        }
        return sb.toString();
    }

    private static String renderJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return (String) v;
        return v.toString();
    }

    private static String capValue(String v) {
        if (v == null) return "null";
        if (v.length() <= TOOL_ARG_MAX_CHARS) return v;
        return v.substring(0, TOOL_ARG_MAX_CHARS) + " [...truncated]";
    }
}
