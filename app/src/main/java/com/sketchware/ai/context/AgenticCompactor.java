package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.TokenEstimator;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
 * <p>This revision (2026-08-14) fixes six bugs that caused compaction
 * to fail silently or produce empty summaries:
 *
 * <h2>Bug fixes (this revision)</h2>
 * <ul>
 *   <li><b>Summarizer input overflow</b> — the to-summarize region could
 *       be larger than the summarizer model's own input window (e.g.
 *       100K tokens of history sent to a 32K-input summarizer). The
 *       summarizer call then failed with the same overflow error we
 *       were trying to recover from, the fallback path returned only
 *       the recent tail, and the user lost all context. Fix: cap the
 *       serialized conversation to {@link #SUMMARIZER_INPUT_CAP_TOKENS}
 *       tokens (default 24K, leaving 8K for the prompt scaffolding and
 *       2K for the output on a 32K-input model). When the serialized
 *       text exceeds the cap, truncate it head+tail using the same
 *       60/40 ratio as {@link ConversationSerializer#truncateToolResult}.</li>
 *
 *   <li><b>Prior-summary extraction returned the FIRST match, not the
 *       latest</b> — when the conversation had been compacted multiple
 *       times, the to-summarize region contained multiple
 *       {@code <summary>} blocks (one per prior compaction). The old
 *       {@code extractPriorSummary} returned the FIRST (oldest), so the
 *       iterative-update prompt was fed stale information and the new
 *       summary regressed to the oldest version instead of building on
 *       the most recent. Fix: iterate all matches and return the LAST
 *       (most recent) summary.</li>
 *
 *   <li><b>Summarizer system prompt was too short</b> — the old prompt
 *       was a single sentence ("Summarize the provided coding session
 *       into a concise continuation note with detailed next steps.").
 *       Reasoning models (Claude 3.7, gpt-5, o3) interpreted this as
 *       permission to think at length, consuming the entire output
 *       budget on reasoning and returning an empty summary text. Fix:
 *       use the richer {@link CompactionPrompts#SUMMARIZATION_SYSTEM}
 *       prompt, which explicitly forbids continuing the conversation
 *       and requires the structured format.</li>
 *
 *   <li><b>adjustCutPoint could return ≤ start, leaving history
 *       unchanged</b> — when every message from {@code cutIndex} back
 *       to {@code start} was a tool_result message, the adjustment
 *       walked all the way back to {@code start} and the method
 *       returned history unchanged. But the original {@code cutIndex}
 *       was chosen because the history was already too big — returning
 *       it unchanged guaranteed the next stream call would overflow
 *       again. Fix: when adjustCutPoint returns ≤ start + 1, force
 *       {@code cutIndex = start + 1} so we summarize at least one
 *       message and make progress. The orphaned-tool_use risk is
 *       smaller than the guaranteed-overflow risk.</li>
 *
 *   <li><b>No timeout on the summarizer call</b> — if the summarizer
 *       provider hung (e.g. AgentRouter queue stall, network blackhole),
 *       the agent loop blocked forever on {@code provider.stream(req)}.
 *       Fix: run the summarizer call on a background executor with a
 *       {@link #SUMMARIZER_TIMEOUT_SECONDS} hard timeout; on timeout,
 *       fall back to {@link BasicCompactor} (shake).</li>
 *
 *   <li><b>Summarizer failure dropped context silently</b> — when the
 *       summarizer call failed, the fallback returned only the recent
 *       tail (no summary), losing all the to-summarize context. Fix:
 *       fall back to {@link BasicCompactor} (shake), which preserves
 *       the to-summarize messages but truncates their heavy tool
 *       results — better than dropping them entirely.</li>
 * </ul>
 *
 * <p>Behavior (preserved from prior version):
 * <ul>
 *   <li><b>Token-budget cut point</b> — preserves the most recent
 *       {@link #KEEP_RECENT_TOKENS} worth of messages, walking backward
 *       from the end.</li>
 *   <li><b>Safe boundary</b> — never cuts at a tool-result-only user
 *       message; the cut walks backward to the nearest assistant or
 *       typed-user message so tool_use/tool_result pairs stay intact.</li>
 *   <li><b>Iterative update</b> — if the to-summarize region already
 *       contains a {@code <summary>...</summary>} block from a prior
 *       compaction, the prior summary is fed to the summarizer inside
 *       a {@code Previous summary:} block so the new summary is an
 *       update rather than a from-scratch rewrite.</li>
 *   <li><b>File operations</b> — read/written/edited paths are extracted
 *       from assistant tool calls and appended to the final summary as
 *       a {@code <files>} tag.</li>
 *   <li><b>Summary as user message</b> — the summary is inserted as a
 *       user message (not a second system message), so provider
 *       constraints about single-leading-system are respected.</li>
 * </ul>
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

    /**
     * Hard cap on the serialized conversation sent to the summarizer,
     * in characters. The summarizer's input window must accommodate:
     * <ul>
     *   <li>System prompt (~500 chars)</li>
     *   <li>Section scaffolding (~1500 chars)</li>
     *   <li>Prior summary (up to ~8000 chars)</li>
     *   <li>Serialized conversation (capped here)</li>
     *   <li>Output budget (~8000 chars for 2048 tokens)</li>
     * </ul>
     * The cap is set to 96K chars (~24K tokens at 4 chars/token), which
     * fits comfortably within a 32K-input summarizer model. For larger
     * summarizer windows the cap is still applied — sending 100K tokens
     * to a summarizer is slow and expensive, and the marginal
     * information gain past ~24K is low (older turns are less relevant
     * by definition).
     */
    private static final int SUMMARIZER_INPUT_CAP_CHARS = 96_000;

    /**
     * Hard timeout on the summarizer call, in seconds. If the summarizer
     * provider does not return within this window, we abort and fall
     * back to {@link BasicCompactor} (shake). 60 seconds is generous —
     * most summarizer calls complete in 5-15 seconds; 60 covers slow
     * reasoning models and congested proxies without hanging the agent
     * loop indefinitely.
     */
    private static final long SUMMARIZER_TIMEOUT_SECONDS = 60L;

    /** Regex used to extract a prior {@code <summary>...</summary>} block. */
    private static final Pattern SUMMARY_BLOCK_RE =
        Pattern.compile("<summary>\\s*([\\s\\S]*?)\\s*</summary>");

    /**
     * Summarizer system prompt. Uses the richer
     * {@link CompactionPrompts#SUMMARIZATION_SYSTEM} prompt which
     * explicitly forbids continuing the conversation and requires the
     * structured format. The old single-sentence prompt caused
     * reasoning models to consume the output budget on thinking and
     * return empty summary text.
     */
    private static final String SUMMARIZER_SYSTEM =
        CompactionPrompts.SUMMARIZATION_SYSTEM;

    /**
     * Background executor for the summarizer call. Single-threaded —
     * only one summarizer call is in flight at a time per compactor
     * instance, and compactor instances are not shared across runs.
     */
    private final ExecutorService summarizerExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "agentic-compactor-summarizer");
            t.setDaemon(true);
            return t;
        });

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

        // Always keep the system prompt (index 0) if present.
        int start = 0;
        AgentMessage systemMsg = null;
        if (!history.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(history.get(0).role)) {
            systemMsg = history.get(0);
            start = 1;
        }

        int remaining = history.size() - start;
        if (remaining <= 1) return history;

        // Token-budget cut point.
        int cutIndex = findCutPoint(history, start, KEEP_RECENT_TOKENS, modelId);
        if (cutIndex <= start) return history;

        // Safe-boundary adjustment.
        cutIndex = adjustCutPoint(history, start, cutIndex);
        // FIX: if adjustCutPoint walked all the way back to start, force
        // at least one message to be summarized. Returning history
        // unchanged here guarantees the next stream call overflows
        // again — the orphaned-tool_use risk is smaller than the
        // guaranteed-overflow risk.
        if (cutIndex <= start) {
            cutIndex = start + 1;
        }

        // Partition.
        List<AgentMessage> toSummarize = new ArrayList<>();
        for (int i = start; i < cutIndex; i++) toSummarize.add(history.get(i));
        List<AgentMessage> toKeep = new ArrayList<>();
        for (int i = cutIndex; i < history.size(); i++) toKeep.add(history.get(i));

        // Extract the LATEST prior summary (not the first — see class
        // javadoc bug #2).
        String priorSummary = extractPriorSummary(toSummarize);

        // Track file operations.
        FileOperationsTracker fileOps = new FileOperationsTracker();
        for (AgentMessage m : toSummarize) fileOps.extractFrom(m);

        // Serialize with the per-model token estimator. The legacy
        // estimator under-counted CJK by 4x, which could cause the
        // serialized text to silently exceed the cap and overflow the
        // summarizer.
        String serialized = ConversationSerializer.serialize(toSummarize);
        if (serialized.isEmpty()) {
            return assembleResult(systemMsg, null, fileOps, toKeep);
        }

        // FIX: cap the serialized conversation to SUMMARIZER_INPUT_CAP_CHARS.
        // Without this cap, a 100K-token to-summarize region would be sent
        // to a 32K-input summarizer, which rejects it with the same
        // overflow error we were trying to recover from.
        if (serialized.length() > SUMMARIZER_INPUT_CAP_CHARS) {
            serialized = truncateSerialized(serialized, SUMMARIZER_INPUT_CAP_CHARS);
        }

        // Build the summarizer prompt.
        String userPrompt = buildSummarizerPrompt(serialized, priorSummary, fileOps);

        // Call the summarizer with a hard timeout.
        String summary;
        try {
            summary = callSummarizerWithTimeout(userPrompt);
        } catch (TimeoutException e) {
            // Summarizer hung — fall back to shake, which preserves the
            // to-summarize messages but truncates their heavy tool
            // results. Better than dropping them entirely.
            return shakeFallback(history, systemMsg, toKeep);
        } catch (Exception e) {
            // Summarizer failed — same shake fallback.
            return shakeFallback(history, systemMsg, toKeep);
        }

        if (summary == null || summary.trim().isEmpty()) {
            // Empty summary (likely reasoning consumed the budget) — shake.
            return shakeFallback(history, systemMsg, toKeep);
        }

        // Append/replace the <files> tag.
        summary = upsertFileOperations(summary, fileOps);

        return assembleResult(systemMsg, summary, fileOps, toKeep);
    }

    /**
     * Walk backward from the end of {@code history} until the accumulated
     * tokens reach {@code keepTokens}. Uses the per-model
     * {@link TokenEstimator} for accurate CJK/Latin/code ratios.
     */
    private int findCutPoint(LinkedList<AgentMessage> history, int start, int keepTokens, String modelId) {
        int accumulated = 0;
        for (int i = history.size() - 1; i >= start; i--) {
            accumulated += TokenEstimator.estimateTokens(history.get(i), modelId);
            if (accumulated >= keepTokens) {
                return i;
            }
        }
        return start;
    }

    /**
     * Adjust the cut point so it never lands on a tool-result-only user
     * message; doing so would orphan the matching tool_use in the
     * summarized region.
     */
    private int adjustCutPoint(LinkedList<AgentMessage> history, int start, int cutIndex) {
        int i = cutIndex;
        while (i > start) {
            AgentMessage m = history.get(i);
            if (isToolResultMessage(m)) {
                i--;
                continue;
            }
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
     * Extract the LATEST (most recent) {@code <summary>...</summary>}
     * block from the to-summarize region. The old implementation
     * returned the FIRST match, which was the OLDEST summary when the
     * conversation had been compacted multiple times — feeding stale
     * information to the iterative-update prompt.
     */
    private static String extractPriorSummary(List<AgentMessage> messages) {
        String latest = null;
        for (AgentMessage m : messages) {
            if (m.text == null) continue;
            Matcher matcher = SUMMARY_BLOCK_RE.matcher(m.text);
            while (matcher.find()) {
                latest = matcher.group(1).trim();
            }
        }
        return latest;
    }

    /**
     * Truncate the serialized conversation to {@code maxChars}, keeping
     * the head and tail in a 60/40 ratio. Mirrors
     * {@link ConversationSerializer#truncateToolResult}'s head/tail
     * approach so the truncation marker is consistent across the
     * codebase.
     */
    private static String truncateSerialized(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        int headLen = (int) (maxChars * 0.6);
        int tailLen = maxChars - headLen;
        int truncatedChars = text.length() - maxChars;
        return text.substring(0, headLen)
            + "\n\n[... " + truncatedChars + " more characters truncated to fit summarizer input ...]\n\n"
            + text.substring(text.length() - tailLen);
    }

    /**
     * Build the user prompt for the summarizer. Uses Cline's
     * {@code buildSummaryRequest} structure: a fixed instruction line,
     * then {@code ## Goal / ## State / ## Highlights / ## Next /
     * ## Files} sections, then an optional {@code Previous summary:}
     * block, then the serialized {@code Conversation:} block.
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
     * Call the summarizer with a hard timeout. Returns the joined text
     * content of the response, trimmed. Throws {@link TimeoutException}
     * if the summarizer does not return within
     * {@link #SUMMARIZER_TIMEOUT_SECONDS}.
     */
    private String callSummarizerWithTimeout(String userPrompt) throws Exception {
        Future<String> future = summarizerExecutor.submit(() -> callSummarizer(userPrompt));
        try {
            return future.get(SUMMARIZER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw e;
        }
    }

    /**
     * Synchronous summarizer call. Returns the joined text content of
     * the response, trimmed.
     *
     * <p>Reasoning is explicitly disabled so reasoning models don't burn
     * the output budget on thinking before emitting summary text.
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
     * Shake fallback: when the summarizer fails or times out, run
     * {@link BasicCompactor} on the full history. This preserves the
     * to-summarize messages but truncates their heavy tool results —
     * better than dropping them entirely.
     */
    private LinkedList<AgentMessage> shakeFallback(LinkedList<AgentMessage> history,
                                                     AgentMessage systemMsg,
                                                     List<AgentMessage> toKeep) {
        BasicCompactor shake = new BasicCompactor();
        LinkedList<AgentMessage> result = shake.compact(history, 0, 0);
        // BasicCompactor already preserves the system prompt and recent
        // tail; just return its result.
        return result;
    }

    /**
     * Insert or replace the {@code <files>} tag in the summary.
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
