package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Smart context truncation strategy. Mirrors Cline's
 * {@code ContextManager.getNextTruncationRange()} with the
 * {@code keep: "none"|"lastTwo"|"half"|"quarter"} strategy.
 *
 * <p>When the conversation history exceeds the model's context window, the
 * {@link com.sketchware.ai.context.Compactor} is invoked to summarize older
 * messages. But how MANY messages to summarize? Truncating too few means
 * we'll be back here next iteration; truncating too many loses useful
 * recent context.
 *
 * <p>The strategy picks a truncation range based on the overflow severity:
 * <ul>
 *   <li><b>Mild overflow</b> (90-95% full): keep the last 2 messages, summarize the rest.</li>
 *   <li><b>Moderate overflow</b> (95-100% full): keep the last 4 messages or 25% of history, whichever is larger.</li>
 *   <li><b>Severe overflow</b> (&gt;100%): keep only the last message, summarize everything else.</li>
 * </ul>
 *
 * <p>The caller (typically {@link com.sketchware.ai.agent.AgentRuntime}) is
 * responsible for actually invoking the compactor on the range returned here.
 */
public final class ContextTruncator {

    private ContextTruncator() {}

    /** Strategy for how much recent history to preserve when truncating. */
    public enum KeepStrategy {
        /** Keep no messages — summarize everything. */
        NONE,
        /** Keep only the last 2 messages. */
        LAST_TWO,
        /** Keep the most recent 50% of messages. */
        HALF,
        /** Keep the most recent 25% of messages. */
        QUARTER,
    }

    /** Result of a truncation decision. */
    public static final class TruncationRange {
        /** Index (inclusive) where truncation starts. Messages [0, startIndex) will be summarized. */
        public final int startIndex;
        /** Index (exclusive) where truncation ends. Messages [startIndex, endIndex) will be summarized. */
        public final int endIndex;
        /** The strategy that was chosen. */
        public final KeepStrategy strategy;
        /** Estimated token count of the messages being summarized. */
        public final int tokensToSummarize;
        /** Estimated token count of the messages being kept. */
        public final int tokensToKeep;
        /** Whether truncation is needed at all. */
        public final boolean needsTruncation;

        public TruncationRange(int startIndex, int endIndex, KeepStrategy strategy,
                               int tokensToSummarize, int tokensToKeep, boolean needsTruncation) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.strategy = strategy;
            this.tokensToSummarize = tokensToSummarize;
            this.tokensToKeep = tokensToKeep;
            this.needsTruncation = needsTruncation;
        }
    }

    /**
     * Decide how much to truncate based on the current token usage.
     *
     * @param historySize       number of messages in the conversation.
     * @param estimatedTokens   total estimated tokens across all messages.
     * @param maxInputTokens    the model's max input context window.
     * @return a {@link TruncationRange} describing what to summarize.
     */
    public static TruncationRange decide(int historySize, int estimatedTokens, int maxInputTokens) {
        if (historySize <= 2 || estimatedTokens <= 0 || maxInputTokens <= 0) {
            return new TruncationRange(0, 0, KeepStrategy.NONE, 0, estimatedTokens, false);
        }

        double ratio = (double) estimatedTokens / maxInputTokens;
        if (ratio < 0.90) {
            // No truncation needed yet.
            return new TruncationRange(0, 0, KeepStrategy.NONE, 0, estimatedTokens, false);
        }

        KeepStrategy strategy;
        int keepCount;
        if (ratio >= 1.0) {
            strategy = KeepStrategy.NONE;
            keepCount = Math.min(1, historySize);
        } else if (ratio >= 0.95) {
            strategy = KeepStrategy.QUARTER;
            keepCount = Math.max(4, historySize / 4);
        } else if (ratio >= 0.90) {
            strategy = KeepStrategy.HALF;
            keepCount = Math.max(4, historySize / 2);
        } else {
            // Mild overflow (just barely over 90%): keep most, trim a little.
            strategy = KeepStrategy.LAST_TWO;
            keepCount = Math.max(2, historySize - 2);
        }

        // The truncation range is [0, historySize - keepCount).
        int summarizeEnd = historySize - keepCount;
        if (summarizeEnd <= 0) {
            return new TruncationRange(0, 0, KeepStrategy.NONE, 0, estimatedTokens, false);
        }

        // Estimate tokens for the summarized portion.
        // We don't have per-message token counts here, so assume uniform distribution.
        double avgPerMessage = (double) estimatedTokens / historySize;
        int tokensToSummarize = (int) (avgPerMessage * summarizeEnd);
        int tokensToKeep = estimatedTokens - tokensToSummarize;

        return new TruncationRange(0, summarizeEnd, strategy, tokensToSummarize, tokensToKeep, true);
    }

    /**
     * Apply a truncation range to the conversation history. Returns a new
     * list containing only the messages that should be KEPT (not summarized).
     * The caller is responsible for summarizing the removed messages and
     * prepending the summary.
     *
     * <p>The first message (system prompt) is always preserved.
     */
    public static LinkedList<AgentMessage> apply(LinkedList<AgentMessage> history, TruncationRange range) {
        if (history == null || !range.needsTruncation) return history;
        LinkedList<AgentMessage> kept = new LinkedList<>();
        // Always keep the system prompt (index 0) if present.
        int start = 0;
        if (!history.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(history.get(0).role)) {
            kept.add(history.get(0));
            start = 1;
        }
        // Keep messages from range.endIndex onwards.
        for (int i = Math.max(start, range.endIndex); i < history.size(); i++) {
            kept.add(history.get(i));
        }
        return kept;
    }

    /**
     * Build a notice to inject into the conversation when truncation happens.
     * This tells the LLM that older context has been summarized.
     */
    public static String truncationNotice(TruncationRange range) {
        if (!range.needsTruncation) return null;
        return "[CONTEXT TRUNCATED] " + range.tokensToSummarize + " tokens of older conversation history "
                + "have been summarized to fit within the model's context window. The summary is provided "
                + "above. Strategy: " + range.strategy + ". " + range.tokensToKeep + " tokens of recent "
                + "history preserved. If you need details from the summarized portion, ask the user.";
    }
}
