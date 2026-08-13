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
 * Legacy LLM-summarizer compaction strategy. Mirrors Cline's
 * {@code sdk/packages/core/src/extensions/context/agentic-compaction.ts}
 * (runAgenticCompaction) and the shared helpers in
 * {@code compaction-shared.ts} (buildSummaryRequest, findCutIndex,
 * serializeConversation, extractFileOps, ensureFilesSection,
 * buildSummaryMessage).
 *
 * <p>Differences from {@link OhMyPiCompactor}: this strategy reuses the
 * Cline-shaped prompt layout (Goal / State / Highlights / Next / Files)
 * and the Cline summarizer system prompt verbatim, while OhMyPiCompactor
 * uses the richer oh-my-pi template (Constraints / Progress / Key
 * Decisions / Critical Context / Additional Notes). Both share the same
 * token-budget cut-point, safe-boundary adjustment, file-ops tracking,
 * and iterative-update logic.
 *
 * <p>Behavior:
 * <ul>
 *   <li><b>Token-budget cut point</b> — preserves the most recent
 *       {@link #KEEP_RECENT_TOKENS} worth of messages, walking backward
 *       from the end. Replaces the legacy message-count cut, which broke
 *       on conversations with a few very large messages (file dumps):
 *       a 30-message window with one 20K-token list_files result would
 *       blow past the model's context window even after compaction.</li>
 *   <li><b>Safe boundary</b> — never cuts at a tool-result-only user
 *       message; the cut walks backward to the nearest assistant or
 *       typed-user message so tool_use/tool_result pairs stay intact on
 *       the kept side. Mirrors Cline's {@code isSafeCutBoundary}.
 *       Cutting at a tool_result leaves an orphaned tool_use in the
 *       summarized region, and the provider rejects the next request
 *       because the kept half references a tool_use_id that no longer
 *       exists.</li>
 *   <li><b>Iterative update</b> — if the to-summarize region already
 *       contains a {@code <summary>...</summary>} block from a prior
 *       compaction, the prior summary is fed to the summarizer inside
 *       a {@code Previous summary:} block so the new summary is an
 *       update rather than a from-scratch rewrite. Mirrors Cline's
 *       {@code findLatestSummaryIndex} + {@code previousSummary}.</li>
 *   <li><b>File operations</b> — read/written/edited paths are extracted
 *       from assistant tool calls via {@link FileOperationsTracker} and
 *       appended to the final summary as a {@code <files>} tag. Carries
 *       forward across compactions. Mirrors Cline's
 *       {@code extractFileOps} + {@code ensureFilesSection}.</li>
 *   <li><b>Structured summary prompt</b> — uses Cline's
 *       {@code buildSummaryRequest} layout (Goal / State / Highlights /
 *       Next / Files) with a {@code Previous summary} block when one
 *       exists, instead of the ad-hoc "Summarize the conversation so
 *       far. Include: Goal/State/Highlights/Next/Files" prompt.</li>
 *   <li><b>Conversation serializer</b> — uses {@link ConversationSerializer}
 *       so role labels, tool-call argument capping, tool-result head/tail
 *       truncation, and useless-result elision match the rest of the
 *       codebase. The old ad-hoc {@code [role] text (thought: ...)
 *       [tool: name(args)] [result: name:output]} format produced
 *       inconsistent transcripts that confused the summarizer.</li>
 *   <li><b>Summary as user message</b> — the summary is inserted as a
 *       user message (not a second system message), so provider
 *       constraints about single-leading-system are respected. The old
 *       code emitted {@code [system, system(summary), ...]} which
 *       violates the OpenAI Chat Completions contract (system messages
 *       must be a single leading block) and the Anthropic Messages API
 *       (only one top-level system block allowed).</li>
 *   <li><b>Summarizer system prompt</b> — uses Cline's exact system
 *       prompt: "Summarize the provided coding session into a concise
 *       continuation note with detailed next steps." The old prompt
 *       ("You are a conversation summarizer. Be concise.") did not tell
 *       the model to focus on continuation, so summaries routinely
 *       dropped the "next steps" that the agent needed to resume.</li>
 *   <li><b>Output budget</b> — {@code SUMMARY_MAX_OUTPUT_TOKENS = 2048}
 *       gives reasoning models headroom for thinking output before the
 *       summary text. The old 1024 cap caused empty summaries on
 *       Claude/gpt-5 reasoning models (thinking consumed the entire
 *       budget, leaving no tokens for the summary itself).</li>
 * </ul>
 *
 * <p>Failure mode: if the summarizer call fails or returns no text, falls
 * back to {@link BasicCompactor}-style truncation (drop old, keep recent,
 * preserve system prompt) so the agent can still continue. Substituting
 * the error text as the new system message (the old behavior) would
 * permanently replace the agent's instructions with literal
 * "[Summary failed: ...]" text, breaking every subsequent turn.
 */
public class AgenticCompactor implements Compactor {

    /**
     * Tokens to preserve verbatim in the recent tail. Mirrors
     * {@link OhMyPiCompactor#DEFAULT_KEEP_RECENT_TOKENS}; Cline's
     * {@code DEFAULT_PRESERVE_RECENT_TOKENS} is 20_000 but we keep 16K
     * to stay consistent with OhMyPiCompactor and BasicCompactor.
     */
    static final int KEEP_RECENT_TOKENS = 16_000;

    /**
     * Hard cap on the summary output tokens. Cline's
     * {@code DEFAULT_SUMMARY_MAX_OUTPUT_TOKENS = 4_096}; we use 2_048
     * to match {@link OhMyPiCompactor#SUMMARY_MAX_OUTPUT_TOKENS} and
     * keep the summarizer call cheap. Reasoning models need this
     * headroom or thinking consumes the entire budget and the summary
     * text comes back empty (Cline's
     * {@code output_budget_consumed_by_reasoning} failure mode).
     */
    private static final int SUMMARY_MAX_OUTPUT_TOKENS = 2_048;

    /** Regex used to extract a prior {@code <summary>...</summary>} block. */
    private static final Pattern SUMMARY_BLOCK_RE =
        Pattern.compile("<summary>\\s*([\\s\\S]*?)\\s*</summary>");

    /**
     * Summarizer system prompt. Mirrors Cline's {@code generateSummary()}
     * system prompt verbatim: "Summarize the provided coding session into
     * a concise continuation note with detailed next steps." This wording
     * makes the model prioritize resumability over abstract summarization.
     */
    private static final String SUMMARIZER_SYSTEM =
        "Summarize the provided coding session into a concise continuation note with detailed next steps.";

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
        if (history == null) return new LinkedList<>();

        // Always keep the system prompt (index 0) if present. The system
        // prompt carries the agent's persona, tool list, and project
        // instructions; losing it permanently degrades every subsequent
        // turn. We do NOT pass the system prompt to the summarizer —
        // the summarizer has its own system prompt and the original one
        // would just consume its input budget.
        int start = 0;
        AgentMessage systemMsg = null;
        if (!history.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(history.get(0).role)) {
            systemMsg = history.get(0);
            start = 1;
        }

        int remaining = history.size() - start;
        if (remaining <= 1) return history;

        // Token-budget cut point: walk backward from the end until the
        // accumulated tokens reach KEEP_RECENT_TOKENS. The returned index
        // is the first index that should be KEPT.
        int cutIndex = findCutPoint(history, start, KEEP_RECENT_TOKENS);
        if (cutIndex <= start) return history;

        // Adjust the cut point so it never lands on a tool-result-only
        // user message; doing so would orphan the matching tool_use in
        // the summarized region. Mirrors Cline's isSafeCutBoundary.
        cutIndex = adjustCutPoint(history, start, cutIndex);
        if (cutIndex <= start) return history;

        // Partition: [start, cutIndex) = to summarize; [cutIndex, end) = to keep.
        List<AgentMessage> toSummarize = new ArrayList<>();
        for (int i = start; i < cutIndex; i++) toSummarize.add(history.get(i));
        List<AgentMessage> toKeep = new ArrayList<>();
        for (int i = cutIndex; i < history.size(); i++) toKeep.add(history.get(i));

        // Extract any prior <summary> block so we can do an iterative
        // update instead of a from-scratch rewrite.
        String priorSummary = extractPriorSummary(toSummarize);

        // Track file operations across the to-summarize region. Read /
        // written / edited paths are extracted from assistant tool calls
        // and appended to the final summary as a <files> tag, so the
        // agent knows which files it has already touched.
        FileOperationsTracker fileOps = new FileOperationsTracker();
        for (AgentMessage m : toSummarize) fileOps.extractFrom(m);

        // Serialize the conversation for the summarizer. ConversationSerializer
        // handles role labels ([User]/[Bot]/[Bot thinking]/[Tool Call]/
        // [Tool Result]), tool-result head/tail truncation, and useless-
        // result elision. The old ad-hoc serializer produced inconsistent
        // transcripts that confused the summarizer.
        String serialized = ConversationSerializer.serialize(toSummarize);
        if (serialized.isEmpty()) {
            // Nothing to summarize — just keep the recent tail with a note.
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        // Build the summarizer prompt (Cline's buildSummaryRequest shape).
        String userPrompt = buildSummarizerPrompt(serialized, priorSummary, fileOps);

        // Call the summarizer.
        String summary;
        try {
            summary = callSummarizer(userPrompt);
        } catch (Exception e) {
            // Fallback: drop old, keep recent. Do NOT substitute the error
            // text as the new system message — that would permanently
            // replace the agent's instructions with "[Summary failed: ...]".
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        if (summary == null || summary.trim().isEmpty()) {
            // Summarizer returned no text (likely reasoning consumed the
            // output budget). Fall back to basic truncation.
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        // Append/replace the <files> tag in the summary. Strips any prior
        // <files>/<read-files>/<modified-files> tags first so legacy
        // summaries self-heal.
        summary = upsertFileOperations(summary, fileOps);

        return assembleResult(systemMsg, summary, fileOps, toKeep);
    }

    /**
     * Walk backward from the end of {@code history} until the accumulated
     * tokens reach {@code keepTokens}. The returned index is the first
     * index that should be KEPT (i.e. {@code history[cutIndex, end)} is
     * the preserved tail). Mirrors Cline's {@code findCutIndex} budget
     * walk.
     *
     * <p>If the total history is smaller than the keep budget, returns
     * {@code start} — no compaction needed.
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
     * Adjust the cut point so it never lands on a tool-result-only user
     * message; doing so would orphan the matching tool_use in the
     * summarized region (the provider would then reject the next request
     * because the kept tool_result references a tool_use_id that no
     * longer exists). Mirrors Cline's {@code isSafeCutBoundary}.
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
     * prompt template. Mirrors Cline's {@code findLatestSummaryIndex} +
     * {@code getCompactionSummaryMetadata}.
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
     * Build the user prompt for the summarizer. Mirrors Cline's
     * {@code buildSummaryRequest} structure verbatim: a fixed instruction
     * line, then {@code ## Goal / ## State / ## Highlights / ## Next /
     * ## Files} sections, then an optional {@code Previous summary:}
     * block (when a prior summary exists), then the serialized
     * {@code Conversation:} block.
     *
     * <p>The fixed-section scaffolding makes the model produce a
     * consistently-shaped summary that the agent can parse on resume.
     * The old ad-hoc prompt left the format open, so summaries varied
     * wildly between runs and often dropped the "Next" section entirely.
     */
    private static String buildSummarizerPrompt(String serialized,
                                                 String priorSummary,
                                                 FileOperationsTracker fileOps) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarize this session for continuation. Be concise and factual.\n\n");
        sb.append("## Goal\n");
        sb.append("One sentence: what is being built or fixed.\n\n");
        sb.append("## State\n");
        sb.append("- Done: completed steps\n");
        sb.append("- In Progress: current work\n");
        sb.append("- Blocked: blockers or open questions\n\n");
        sb.append("## Highlights\n");
        sb.append("Key technical choices or notable findings (omit if none).\n\n");
        sb.append("## Next\n");
        sb.append("Immediate next steps.\n\n");

        // Inline the files section so the summarizer has it; the canonical
        // <files> tag is appended again to the final summary by
        // upsertFileOperations.
        FileOperationsTracker.ComputedFileLists lists = fileOps.computeLists();
        sb.append("## Files\n");
        sb.append("Read: ")
          .append(lists.readFiles.isEmpty() ? "none" : String.join(", ", lists.readFiles))
          .append('\n');
        sb.append("Edited: ")
          .append(lists.modifiedFiles.isEmpty() ? "none" : String.join(", ", lists.modifiedFiles))
          .append('\n');

        if (priorSummary != null && !priorSummary.isEmpty()) {
            sb.append("\nPrevious summary:\n").append(priorSummary).append('\n');
        }

        sb.append("\nConversation:\n").append(serialized.isEmpty() ? "(empty)" : serialized);
        return sb.toString();
    }

    /**
     * Call the summarizer model. Synchronous (no streaming collection).
     * Returns the joined text content of the response, trimmed.
     *
     * <p>Reasoning is explicitly disabled (ReasoningRequest(false, NONE,
     * null)) so reasoning models don't burn the output budget on thinking
     * before emitting summary text. Cline does the same in
     * {@code resolveSummarizerConfig} ({@code thinking: false}).
     */
    private String callSummarizer(String userPrompt) throws Exception {
        ModelInfo model = provider.getModel(modelId);
        List<AgentMessage> conv = new ArrayList<>();
        conv.add(AgentMessage.user(userPrompt));

        LlmRequest req = new LlmRequest(
                provider.getProviderId(),
                null, apiKey, model,
                SUMMARIZER_SYSTEM,
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
     * {@code <modified-files>} tags first so legacy summaries self-heal
     * across compactions. Mirrors {@code upsertFileOperations} and
     * Cline's {@code ensureFilesSection}.
     */
    private static String upsertFileOperations(String summary, FileOperationsTracker fileOps) {
        String filesTag = fileOps.format();
        if (filesTag.isEmpty()) return summary;
        String base = summary
            .replaceAll("<files>[\\s\\S]*?</files>\\s*", "")
            .replaceAll("<read-files>[\\s\\S]*?</read-files>\\s*", "")
            .replaceAll("<modified-files>[\\s\\S]*?</modified-files>\\s*", "")
            .trim();
        return base + "\n\n" + filesTag;
    }

    /**
     * Assemble the final compacted history: optional system prompt, the
     * wrapped summary as a user message, then the kept recent messages.
     *
     * <p>The summary is wrapped as a user message (not a second system
     * message) so we don't violate the single-leading-system constraint
     * of OpenAI-compatible providers and the Anthropic Messages API.
     * The wrapper template ({@link CompactionPrompts#COMPACTION_SUMMARY_CONTEXT})
     * tells the model that prior work exists and it MUST build on it
     * rather than duplicate it.
     *
     * <p>When no summary is available (summarizer failed or nothing to
     * summarize), a minimal note is inserted so the model knows history
     * was compacted and doesn't ask "what were we doing?" on the next
     * turn.
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
            result.add(AgentMessage.user(
                "[Note: earlier conversation history was compacted to save context. "
              + "Previous messages are not shown but the user's intent is preserved.]"));
        }
        result.addAll(toKeep);
        return result;
    }
}
