package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import java.util.LinkedList;

/**
 * Compaction pipeline. Mirrors oh-my-pi's
 * {@code packages/agent/src/compaction/compaction.ts} plus Cline's
 * {@code sdk/packages/core/src/extensions/context/} for the trigger policy.
 *
 * <p>Three strategies:
 * <ul>
 *   <li>{@link BasicCompactor} — mechanical "shake" strategy, no LLM call.
 *       Replaces heavy tool results with placeholders, drops old reasoning.
 *       Used for overflow recovery and reasoning-disabled profiles.</li>
 *   <li>{@link OhMyPiCompactor} — LLM-summarizer "context-full" strategy.
 *       Sends the older portion to a summarizer model with a structured
 *       prompt (Goal / Progress / Key Decisions / Next Steps / ...) and
 *       replaces it with the structured summary. Default for
 *       reasoning-enabled profiles.</li>
 *   <li>{@link AgenticCompactor} — legacy LLM-summarizer strategy with a
 *       simpler prompt. Retained for fallback when {@link OhMyPiCompactor}
 *       is not desired.</li>
 * </ul>
 *
 * <p>Trigger policy: when estimated tokens > {@code 0.9 * maxInputTokens}
 * (Cline's {@code COMPACTION_TRIGGER_RATIO}). Mid-turn overflow recovery
 * may also trigger compaction after a stream error.
 */
public interface Compactor {

    /**
     * Compact the conversation. Returns a new history where the older
     * messages are replaced by a summary (or by truncation placeholders
     * for the shake strategy), and the most recent messages are kept
     * verbatim.
     *
     * @param history                the full conversation history,
     *                               including the system prompt at index 0
     *                               if one is present.
     * @param maxInputTokens         the model's max input context window.
     *                               Used by some strategies to size the
     *                               keep-recent budget.
     * @param preserveRecentMessages hint for the number of recent messages
     *                               to preserve. Newer strategies
     *                               ({@link OhMyPiCompactor},
     *                               {@link BasicCompactor}) ignore this in
     *                               favor of a token-budget-based keep
     *                               window, but the parameter is kept for
     *                               backward compatibility with
     *                               {@link AgenticCompactor}.
     */
    LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                     int maxInputTokens,
                                     int preserveRecentMessages);

    /**
     * Human-readable name of the strategy. Used for logging and UI
     * display (e.g. "Compacted (shake)" vs "Compacted (context-full)").
     */
    default String strategyName() {
        return getClass().getSimpleName();
    }
}
