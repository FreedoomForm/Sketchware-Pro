package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Mechanical ("shake") compaction strategy. No LLM call. Ported from
 * oh-my-pi's {@code packages/agent/src/compaction/shake.ts}.
 *
 * <p>Instead of dropping old messages wholesale, {@code shake} walks the
 * history and rewrites heavy content in place:
 * <ul>
 *   <li><b>Tool results</b> older than the protected recent window are
 *       replaced with a short {@code [Output truncated - N tokens]}
 *       placeholder. The tool-call/result pair stays intact, so the
 *       conversation grammar remains valid for any provider that
 *       requires tool_use/tool_result pairing.</li>
 *   <li><b>Long reasoning blocks</b> in assistant messages are dropped
 *       entirely — reasoning is ephemeral and low-signal for old turns.</li>
 * </ul>
 *
 * <p>The protected recent window is sized in tokens (default 16K), not
 * messages, so a single huge tool result in the recent past stays intact
 * even if it dwarfs the rest of the window. Mirrors
 * {@link ShakeConfig#protectTokens}.
 *
 * <p>Use cases:
 * <ul>
 *   <li><b>Overflow recovery</b> — when a provider returns a context-length
 *       error mid-stream and we need to shrink the conversation before
 *       retrying. {@code shake} is safe here because it doesn't require
 *       another LLM call (which might also fail).</li>
 *   <li><b>Reasoning-disabled profiles</b> — when {@code enableReasoning}
 *       is false, we don't have a summarizer configured, so {@code shake}
 *       is the only viable strategy.</li>
 *   <li><b>Mid-turn threshold</b> — when the conversation crosses the
 *       compaction threshold mid-tool-loop, {@code shake} is cheaper than
 *       a full summarizer call and keeps the active turn intact.</li>
 * </ul>
 */
public class BasicCompactor implements Compactor {

    /** Default tokens protected from shaking (the recent tail). */
    static final int DEFAULT_PROTECT_TOKENS = 16_000;

    /** Minimum total savings required to actually perform the shake. */
    static final int DEFAULT_MIN_SAVINGS = 4_000;

    /** Minimum size of a tool result before it is eligible for shaking. */
    static final int MIN_SHAKEABLE_RESULT_CHARS = 200;

    /** Approximate token cost of the truncation placeholder. */
    static final int PLACEHOLDER_TOKEN_ESTIMATE = 16;

    private final int protectTokens;
    private final int minSavings;

    public BasicCompactor() {
        this(DEFAULT_PROTECT_TOKENS, DEFAULT_MIN_SAVINGS);
    }

    public BasicCompactor(int protectTokens, int minSavings) {
        this.protectTokens = protectTokens;
        this.minSavings = minSavings;
    }

    @Override
    public String strategyName() {
        return "shake";
    }

    @Override
    public LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                            int maxInputTokens,
                                            int preserveRecentMessages) {
        if (history == null || history.size() <= 1) {
            return history != null ? history : new LinkedList<>();
        }

        // Walk backward from the end to find the protected window boundary.
        // The boundary index is the first index NOT in the protected window.
        // Note: BasicCompactor does not know the model id, so we use the
        // legacy chars/4 estimator here. The other compactors
        // (AgenticCompactor, OhMyPiCompactor, SnapCompactCompactor) have
        // access to the model id and use the per-model TokenEstimator.
        // BasicCompactor is the overflow-recovery fallback and is called
        // when speed matters more than accuracy — the chars/4 estimator
        // is faster (no Unicode-block classification) and over-counts
        // CJK by 4x, which makes the shake trigger earlier on CJK-heavy
        // conversations. That's the safe direction: early shake never
        // causes an overflow, late shake does.
        int accumulated = 0;
        int boundary = history.size();
        for (int i = history.size() - 1; i >= 0; i--) {
            accumulated += history.get(i).estimateTokens();
            if (accumulated >= protectTokens) {
                boundary = i;
                break;
            }
            boundary = i;
        }

        if (boundary <= 0) {
            // Whole history is in the protected window — nothing to shake.
            return history;
        }

        // Walk the eligible region [0, boundary) and estimate savings.
        int estimatedSavings = 0;
        for (int i = 0; i < boundary; i++) {
            AgentMessage m = history.get(i);
            if (m.hasToolResults()) {
                for (AgentMessage.ToolResultContent r : m.toolResults) {
                    if (r.output != null && r.output.length() > MIN_SHAKEABLE_RESULT_CHARS) {
                        int originalTokens = r.output.length() / 4;
                        estimatedSavings += Math.max(0, originalTokens - PLACEHOLDER_TOKEN_ESTIMATE);
                    }
                }
            }
            // Reasoning blocks: drop entirely if non-empty.
            if (AgentMessage.ROLE_ASSISTANT.equals(m.role)
                    && m.reasoning != null && !m.reasoning.isEmpty()) {
                estimatedSavings += m.reasoning.length() / 4;
            }
        }

        if (estimatedSavings < minSavings) {
            // Not enough to bother — would just churn the prompt cache.
            return history;
        }

        // Apply the shake: build a new history with rewritten old messages
        // and verbatim recent messages.
        LinkedList<AgentMessage> result = new LinkedList<>();
        for (int i = 0; i < history.size(); i++) {
            AgentMessage m = history.get(i);
            if (i >= boundary) {
                result.add(m);
                continue;
            }
            result.add(shakeMessage(m));
        }

        // Prepend a system note so the model knows old tool outputs were
        // elided. This is the equivalent of oh-my-pi's shake event notice.
        AgentMessage note = AgentMessage.system(
            "[Note: older tool outputs in this conversation were truncated to save context. "
          + "Output content above is preserved; output content below the placeholder is omitted. "
          + "If you need details from a truncated output, ask the user.]");
        // Insert after the system prompt if one exists.
        int insertAt = 0;
        if (!result.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(result.get(0).role)) {
            insertAt = 1;
        }
        result.add(insertAt, note);
        return result;
    }

    /**
     * Shake a single message: replace large tool results with a placeholder,
     * and drop reasoning blocks. Text content and tool calls are preserved
     * so the conversation grammar stays valid.
     */
    private static AgentMessage shakeMessage(AgentMessage m) {
        if (AgentMessage.ROLE_ASSISTANT.equals(m.role)) {
            // Drop reasoning, keep text + tool calls.
            if (m.reasoning == null || m.reasoning.isEmpty()) return m;
            return AgentMessage.assistant(m.text, null, m.toolCalls);
        }
        if (m.hasToolResults()) {
            List<AgentMessage.ToolResultContent> shaken = new java.util.ArrayList<>();
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                if (r.output != null && r.output.length() > MIN_SHAKEABLE_RESULT_CHARS) {
                    int originalTokens = r.output.length() / 4;
                    shaken.add(new AgentMessage.ToolResultContent(
                        r.toolCallId, r.toolName,
                        "[Output truncated - " + originalTokens + " tokens]",
                        r.isError));
                } else {
                    shaken.add(r);
                }
            }
            return AgentMessage.toolResult(shaken);
        }
        return m;
    }

    /**
     * Configuration for the shake strategy. Mirrors
     * {@code ShakeConfig} in oh-my-pi's {@code shake.ts}. Exposed for
     * callers that want to tune the thresholds (e.g. rescue/aggressive
     * variants for overflow recovery).
     */
    public static final class ShakeConfig {
        public final int protectTokens;
        public final int minSavings;
        public ShakeConfig(int protectTokens, int minSavings) {
            this.protectTokens = protectTokens;
            this.minSavings = minSavings;
        }
    }
}
