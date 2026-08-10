package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Compaction pipeline. Mirrors Cline's
 * {@code sdk/packages/core/src/extensions/context/}.
 *
 * <p>Two strategies:
 * <ul>
 *   <li>{@link BasicCompactor} - deterministic, no LLM call. Used for overflow recovery.</li>
 *   <li>{@link AgenticCompactor} - LLM summarizer call (default).</li>
 * </ul>
 *
 * <p>Trigger: when estimated tokens > {@code 0.9 * maxInputTokens} (Cline's
 * {@code COMPACTION_TRIGGER_RATIO}).
 */
public interface Compactor {

    /**
     * Compact the conversation. Returns a new history where the older
     * messages are replaced by a summary, and the most recent
     * {@code preserveRecentMessages} messages are kept verbatim.
     */
    LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                     int maxInputTokens,
                                     int preserveRecentMessages);
}
