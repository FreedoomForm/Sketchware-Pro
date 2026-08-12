package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * LLM-summarizer compaction strategy. Sends a summarization request to the
 * provider and prepends the summary to the recent messages.
 *
 * <p>Mirrors Cline's {@code agentic} strategy with the summarizer prompt
 * from {@code buildSummaryRequest}.
 */
public class AgenticCompactor implements Compactor {

    private final LlmProvider provider;
    private final String apiKey;
    private final String modelId;

    public AgenticCompactor(LlmProvider provider, String apiKey, String modelId) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.modelId = modelId;
    }

    @Override
    public String strategyName() {
        return "agentic-legacy";
    }

    @Override
    public LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                            int maxInputTokens,
                                            int preserveRecentMessages) {
        if (history == null || history.size() <= preserveRecentMessages + 1) {
            return history != null ? history : new LinkedList<>();
        }

        // Capture the original system prompt (if any) before partitioning —
        // we must carry it through the compaction rather than replacing it
        // with the summary. Losing the system prompt drops the agent's
        // persona/instructions, and every subsequent turn silently degrades.
        // OhMyPiCompactor does the same; see OhMyPiCompactor.compact.
        AgentMessage systemMsg = null;
        if (!history.isEmpty() && "system".equals(history.get(0).role)) {
            systemMsg = history.get(0);
        }

        // Split: summary part + recent part.
        int summaryCount = history.size() - preserveRecentMessages;
        List<AgentMessage> toSummarize = new ArrayList<>(history.subList(0, summaryCount));
        List<AgentMessage> recent = new ArrayList<>(history.subList(summaryCount, history.size()));

        String summary = summarizeWithLLM(toSummarize);

        // On summarizer failure, fall back to BasicCompactor-style behavior
        // (keep recent, drop old) instead of substituting the error string
        // as the new system message. Substituting the error permanently
        // replaces the agent's instructions with literal "[Summary failed: ...]"
        // text, breaking every subsequent turn.
        if (summary == null) {
            LinkedList<AgentMessage> fallback = new LinkedList<>();
            if (systemMsg != null) fallback.add(systemMsg);
            fallback.add(AgentMessage.system("[Context compacted: summarizer failed, "
                    + "older messages were dropped to fit the context window.]"));
            fallback.addAll(recent);
            return fallback;
        }

        LinkedList<AgentMessage> result = new LinkedList<>();
        // Preserve the original system prompt FIRST, then add the summary as
        // a separate system message so the agent retains both its instructions
        // and the conversation context.
        if (systemMsg != null) result.add(systemMsg);
        result.add(AgentMessage.system("[Conversation summary]\n" + summary));
        result.addAll(recent);
        return result;
    }

    private String summarizeWithLLM(List<AgentMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarize the conversation so far. Include:\n");
        sb.append("- Goal: the user's main objective\n");
        sb.append("- State: what has been done so far\n");
        sb.append("- Highlights: key decisions, files changed, tools used\n");
        sb.append("- Next: what the next step should be\n");
        sb.append("- Files: list of files/beans/views affected\n\n");
        sb.append("Conversation:\n\n");
        for (AgentMessage m : messages) {
            sb.append("[").append(m.role).append("] ");
            if (m.text != null) sb.append(m.text);
            if (m.reasoning != null && !m.reasoning.isEmpty()) sb.append(" (thought: ").append(m.reasoning).append(")");
            if (m.toolCalls != null) for (AgentMessage.ToolCall tc : m.toolCalls) {
                sb.append(" [tool: ").append(tc.name).append("(").append(tc.argumentsJson).append(")]");
            }
            if (m.toolResults != null) for (AgentMessage.ToolResultContent r : m.toolResults) {
                sb.append(" [result: ").append(r.toolName).append(": ").append(r.output).append("]");
            }
            sb.append("\n");
        }
        sb.append("\nWrite the summary now. Keep it under 500 words.\n");

        // Synchronous call (no streaming) to keep the code simple.
        try {
            ModelInfo model = provider.getModel(modelId);
            List<AgentMessage> conv = new ArrayList<>();
            conv.add(AgentMessage.user(sb.toString()));
            LlmRequest req = new LlmRequest(
                    provider.getProviderId(),
                    null, apiKey, model,
                    "You are a conversation summarizer. Be concise.",
                    conv, null,
                    new com.sketchware.ai.llm.reasoning.ReasoningRequest(false, null, null),
                    1024, false, null);
            StringBuilder result = new StringBuilder();
            for (ApiStreamChunk chunk : provider.stream(req)) {
                if (chunk.isText()) result.append(chunk.asText().text);
                if (chunk.isDone()) break;
            }
            return result.toString();
        } catch (Exception e) {
            // Return null so the caller knows summarization failed and can
            // fall back to BasicCompactor-style truncation rather than
            // substituting the error text as the system message.
            return null;
        }
    }
}
