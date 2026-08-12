package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-summarizer compaction strategy. Ported from oh-my-pi's
 * {@code packages/agent/src/compaction/compaction.ts} (compact function).
 *
 * <p>This is the {@code context-full} strategy: the older portion of the
 * conversation is sent to a summarizer model with a structured prompt, and
 * the returned structured summary (Goal / Constraints / Progress / Key
 * Decisions / Next Steps / Critical Context / Additional Notes) replaces
 * the older portion in the live history. The most recent messages are
 * preserved verbatim so the model can continue the active turn.
 *
 * <p>Features ported from oh-my-pi:
 * <ul>
 *   <li><b>Structured summary format</b> with fixed sections —
 *       {@link CompactionPrompts#COMPACTION_SUMMARY} for first pass,
 *       {@link CompactionPrompts#COMPACTION_UPDATE_SUMMARY} when a prior
 *       summary already exists in the conversation.</li>
 *   <li><b>Iterative update</b> — if the conversation already contains a
 *       {@code <summary>} block from a prior compaction, the new summary
 *       is built as an update rather than from scratch. Preserves prior
 *       progress and only adds new information.</li>
 *   <li><b>File operations tracking</b> — read/written/edited file paths
 *       are extracted from assistant tool calls and appended to the
 *       summary as a {@code <files>} tag. Carries forward across
 *       compactions.</li>
 *   <li><b>Cut-point logic</b> — never cuts at a tool-result message.
 *       If the cut point lands on a tool result, it is moved backward to
 *       the preceding user/assistant message to keep tool-call/result
 *       pairs intact in the kept region.</li>
 *   <li><b>Token-budget preserving</b> — instead of preserving a fixed
 *       number of messages, preserves the most recent
 *       {@code keepRecentTokens} worth of messages (default 16K). This
 *       handles conversations with very large individual messages (e.g.
 *       file dumps) more gracefully than message-count preservation.</li>
 *   <li><b>Tool-result truncation</b> — long tool outputs are truncated
 *       head+tail in the serialized input to the summarizer. Prevents a
 *       single 50KB list_files dump from blowing the summarizer's input.</li>
 *   <li><b>Previous-summary preservation</b> — when a prior summary
 *       exists, it is included in the new summarizer call inside
 *       {@code <previous-summary>} tags so the update can be incremental.</li>
 * </ul>
 *
 * <p>Failure mode: if the summarizer call fails for any reason, falls back
 * to {@link BasicCompactor} behavior (drop old messages, keep recent) so
 * the agent can still continue. The fallback is logged via the
 * {@link Compactor.Listener} if one is attached.
 */
public class OhMyPiCompactor implements Compactor {

    /** Default tokens to preserve verbatim in the recent tail. */
    public static final int DEFAULT_KEEP_RECENT_TOKENS = 16_000;

    /** Hard cap on the summary output to prevent runaway summarizer output. */
    private static final int SUMMARY_MAX_OUTPUT_TOKENS = 2048;

    /** Regex used to extract a prior {@code <summary>...</summary>} block. */
    private static final Pattern SUMMARY_BLOCK_RE =
        Pattern.compile("<summary>\\s*([\\s\\S]*?)\\s*</summary>");

    private final LlmProvider provider;
    private final String apiKey;
    private final String modelId;
    private final int keepRecentTokens;
    private final Listener listener;

    /** Optional listener for logging compaction events. */
    public interface Listener {
        void onCompactionEvent(String event);
    }

    public OhMyPiCompactor(LlmProvider provider, String apiKey, String modelId) {
        this(provider, apiKey, modelId, DEFAULT_KEEP_RECENT_TOKENS, null);
    }

    public OhMyPiCompactor(LlmProvider provider, String apiKey, String modelId,
                           int keepRecentTokens, Listener listener) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.keepRecentTokens = keepRecentTokens;
        this.listener = listener;
    }

    @Override
    public String strategyName() {
        return "context-full";
    }

    @Override
    public LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                            int maxInputTokens,
                                            int preserveRecentMessages) {
        if (history == null) return new LinkedList<>();
        // Always keep the system prompt (index 0) if present.
        int start = 0;
        AgentMessage systemMsg = null;
        if (!history.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(history.get(0).role)) {
            systemMsg = history.get(0);
            start = 1;
        }

        int remaining = history.size() - start;
        if (remaining <= 1) return history;

        // Find the cut point using token budget.
        int cutIndex = findCutPoint(history, start, keepRecentTokens);
        if (cutIndex <= start) return history;

        // Adjust cut point to never split a tool-call/result pair.
        // Walk backward from cutIndex to the nearest user/assistant message.
        cutIndex = adjustCutPoint(history, start, cutIndex);

        if (cutIndex <= start) return history;

        // Partition: [start, cutIndex) = to summarize; [cutIndex, end) = to keep.
        List<AgentMessage> toSummarize = new ArrayList<>();
        for (int i = start; i < cutIndex; i++) toSummarize.add(history.get(i));
        List<AgentMessage> toKeep = new ArrayList<>();
        for (int i = cutIndex; i < history.size(); i++) toKeep.add(history.get(i));

        // Extract any prior summary from the to-summarize region.
        String priorSummary = extractPriorSummary(toSummarize);

        // Track file operations across the to-summarize region.
        FileOperationsTracker fileOps = new FileOperationsTracker();
        for (AgentMessage m : toSummarize) fileOps.extractFrom(m);

        // Serialize the conversation for the summarizer.
        String serialized = ConversationSerializer.serialize(toSummarize);
        if (serialized.isEmpty()) {
            // Nothing to summarize — just keep the recent tail.
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        // Build the summarizer prompt.
        String userPrompt = buildSummarizerPrompt(serialized, priorSummary);

        // Call the summarizer.
        String summary;
        try {
            summary = callSummarizer(userPrompt);
        } catch (Exception e) {
            if (listener != null) {
                listener.onCompactionEvent("Summarizer call failed: " + e.getMessage()
                    + " — falling back to basic compaction.");
            }
            // Fallback: drop old, keep recent.
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        if (summary == null || summary.trim().isEmpty()) {
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        // Append the <files> tag to the summary.
        summary = upsertFileOperations(summary, fileOps);

        return assembleResult(systemMsg, summary, fileOps, toKeep);
    }

    /**
     * Find the cut point: the index in {@code history} such that the
     * tokens in {@code [cutIndex, history.size())} are roughly
     * {@code >= keepRecentTokens}. Walks backward from the end.
     */
    private int findCutPoint(LinkedList<AgentMessage> history, int start, int keepTokens) {
        int accumulated = 0;
        for (int i = history.size() - 1; i >= start; i--) {
            accumulated += history.get(i).estimateTokens();
            if (accumulated >= keepTokens) {
                return i;
            }
        }
        // Total history is smaller than the keep budget — no compaction needed.
        return start;
    }

    /**
     * Adjust the cut point so it never lands on a tool-result message.
     * Tool results in our model are {@code user} role messages with
     * non-empty {@code toolResults}. Cutting at such a message would
     * orphan the tool-call half of the pair in the summarized region.
     *
     * <p>Also pulls the cut point past any preceding assistant message
     * whose only content is tool calls — those would be orphaned too if
     * their results are kept.
     */
    private int adjustCutPoint(LinkedList<AgentMessage> history, int start, int cutIndex) {
        int i = cutIndex;
        while (i > start) {
            AgentMessage m = history.get(i);
            if (isToolResultMessage(m)) {
                i--;
                continue;
            }
            // If this is an assistant message with tool calls but no text,
            // and the next message is a tool result, move past it.
            if (AgentMessage.ROLE_ASSISTANT.equals(m.role)
                    && (m.text == null || m.text.isEmpty())
                    && m.hasToolCalls()
                    && i + 1 < history.size()
                    && isToolResultMessage(history.get(i + 1))) {
                i--;
                continue;
            }
            break;
        }
        return i;
    }

    private static boolean isToolResultMessage(AgentMessage m) {
        return m != null
            && AgentMessage.ROLE_USER.equals(m.role)
            && m.hasToolResults();
    }

    /**
     * Extract the contents of a prior {@code <summary>...</summary>} block
     * from the to-summarize region. Used to switch to the iterative update
     * prompt template.
     */
    private static String extractPriorSummary(List<AgentMessage> messages) {
        for (AgentMessage m : messages) {
            if (m.text == null) continue;
            Matcher matcher = SUMMARY_BLOCK_RE.matcher(m.text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    /**
     * Build the user prompt for the summarizer. Switches between the
     * first-pass template and the iterative-update template based on
     * whether a prior summary was found.
     */
    private static String buildSummarizerPrompt(String serialized, String priorSummary) {
        StringBuilder sb = new StringBuilder();
        if (priorSummary != null) {
            sb.append(CompactionPrompts.COMPACTION_UPDATE_SUMMARY)
              .append(priorSummary)
              .append("\n</previous-summary>\n\n")
              .append("New conversation to integrate:\n<conversation>\n")
              .append(serialized)
              .append("\n</conversation>\n");
        } else {
            sb.append(CompactionPrompts.COMPACTION_SUMMARY)
              .append(serialized)
              .append("\n</conversation>\n");
        }
        return sb.toString();
    }

    /**
     * Call the summarizer model. Synchronous (no streaming collection).
     * Returns the joined text content of the response.
     */
    private String callSummarizer(String userPrompt) throws Exception {
        ModelInfo model = provider.getModel(modelId);
        List<AgentMessage> conv = new ArrayList<>();
        conv.add(AgentMessage.user(userPrompt));

        LlmRequest req = new LlmRequest(
                provider.getProviderId(),
                null, apiKey, model,
                CompactionPrompts.SUMMARIZATION_SYSTEM,
                conv, null,
                new ReasoningRequest(false, ReasoningEffort.NONE, null),
                SUMMARY_MAX_OUTPUT_TOKENS, false, null);

        StringBuilder result = new StringBuilder();
        for (ApiStreamChunk chunk : provider.stream(req)) {
            if (chunk.isText()) result.append(chunk.asText().text);
            if (chunk.isDone()) break;
        }
        return result.toString().trim();
    }

    /**
     * Insert or replace the {@code <files>} tag in the summary. Strips
     * any prior {@code <files>}, {@code <read-files>}, or
     * {@code <modified-files>} tags first so legacy summaries self-heal.
     * Mirrors {@code upsertFileOperations}.
     */
    private static String upsertFileOperations(String summary, FileOperationsTracker fileOps) {
        String filesTag = fileOps.format();
        if (filesTag.isEmpty()) return summary;

        // Strip any existing tags.
        String base = summary
            .replaceAll("<files>[\\s\\S]*?</files>\\s*", "")
            .replaceAll("<read-files>[\\s\\S]*?</read-files>\\s*", "")
            .replaceAll("<modified-files>[\\s\\S]*?</modified-files>\\s*", "")
            .trim();

        return base + "\n\n" + filesTag;
    }

    /**
     * Assemble the final compacted history: optional system prompt,
     * the wrapped summary as a user message, then the kept recent messages.
     */
    private static LinkedList<AgentMessage> assembleResult(
            AgentMessage systemMsg,
            String summary,
            FileOperationsTracker fileOps,
            List<AgentMessage> toKeep) {
        LinkedList<AgentMessage> result = new LinkedList<>();
        if (systemMsg != null) result.add(systemMsg);
        if (summary != null && !summary.trim().isEmpty()) {
            String wrapped = String.format(CompactionPrompts.COMPACTION_SUMMARY_CONTEXT, summary);
            result.add(AgentMessage.user(wrapped));
        } else {
            // No summary (summarizer failed or nothing to summarize) —
            // insert a minimal note so the model knows history was compacted.
            result.add(AgentMessage.user(
                "[Note: earlier conversation history was compacted to save context. "
              + "Previous messages are not shown but the user's intent is preserved.]"));
        }
        result.addAll(toKeep);
        return result;
    }
}
