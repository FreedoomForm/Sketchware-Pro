package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;

import java.util.LinkedList;

/**
 * Deterministic compaction strategy. No LLM call.
 * Keeps only the most recent {@code preserveRecentMessages} messages and
 * drops the rest. Used for overflow recovery.
 */
public class BasicCompactor implements Compactor {

    @Override
    public LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                            int maxInputTokens,
                                            int preserveRecentMessages) {
        if (history == null) return new LinkedList<>();
        if (history.size() <= preserveRecentMessages) return history;

        LinkedList<AgentMessage> result = new LinkedList<>();
        int keep = Math.min(preserveRecentMessages, history.size());
        for (int i = history.size() - keep; i < history.size(); i++) {
            result.add(history.get(i));
        }
        // Prepend a synthetic system note explaining the compaction.
        AgentMessage note = AgentMessage.system(
                "[Note: earlier conversation history was compacted to save context. "
              + "Previous messages are not shown but the user's intent is preserved.]");
        result.addFirst(note);
        return result;
    }
}
