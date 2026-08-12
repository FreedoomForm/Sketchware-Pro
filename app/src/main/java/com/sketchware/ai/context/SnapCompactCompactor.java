package com.sketchware.ai.context;

import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.context.snapcompact.BdfFontRegistry;
import com.sketchware.ai.context.snapcompact.SnapCompact;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bitmap-frame context compaction strategy. Java/Android port of
 * oh-my-pi's {@code packages/snapcompact} — see
 * {@link com.sketchware.ai.context.snapcompact.SnapCompact} for the
 * architecture and the simplifications made for the mobile port.
 *
 * <p>This compactor renders discarded conversation history into dense PNG
 * frames of pixel-font glyphs that vision-capable LLMs read back directly
 * as image content blocks. No LLM call is made during compaction — the
 * pass is fully local and deterministic.
 *
 * <p>Use this compactor only when the live model is vision-capable
 * ({@code ModelInfo.supportsImages == true}). When the model cannot read
 * images, snapcompact frames would be invisible to it — fall back to
 * {@link OhMyPiCompactor} or {@link BasicCompactor} instead.
 *
 * <p>The discarded messages are replaced by a single {@code user} message
 * carrying the snapcompact lead-in summary text plus the rendered PNG
 * frames as base64 image attachments. The provider serializes these
 * images as image content blocks in the next LLM request (OpenAI:
 * {@code image_url} with data URL; Anthropic: {@code image} with
 * {@code base64} source; Gemini: {@code inline_data} with base64).
 *
 * <p>Frame shape is auto-selected by model id (Claude → 11on16-bw,
 * Gemini → 8on22-bw, GPT → 8on22-bw, GLM → 8on16-bw, unknown → 8on22-bw).
 *
 * <p>Cut-point logic mirrors {@link OhMyPiCompactor}:
 * <ul>
 *   <li>Token-budget-based keep window (default 16K tokens).</li>
 *   <li>Never cut at a tool-result message — walk backward to the
 *       preceding user/assistant message to keep tool-call/result pairs
 *       intact in the kept region.</li>
 *   <li>Prior {@code <summary>...</summary>} block in the discarded
 *       region is extracted and prepended to the archive as
 *       {@code [Summary of earlier history]}.</li>
 * </ul>
 *
 * <p>Failure mode: if rendering fails for any reason (font load error,
 * OOM, etc.), falls back to {@link BasicCompactor} behavior.
 */
public class SnapCompactCompactor implements Compactor {

    /** Default tokens to preserve verbatim in the recent tail. */
    public static final int DEFAULT_KEEP_RECENT_TOKENS = 16_000;

    /** Regex used to extract a prior {@code <summary>...</summary>} block. */
    private static final Pattern SUMMARY_BLOCK_RE =
            Pattern.compile("<summary>\\s*([\\s\\S]*?)\\s*</summary>");

    private final int keepRecentTokens;
    private final String modelId;
    private final Listener listener;
    private final Object appContext; // android.content.Context, kept as Object to avoid hard dependency

    /** Optional listener for logging compaction events. */
    public interface Listener {
        void onCompactionEvent(String event);
    }

    public SnapCompactCompactor(String modelId, Object appContext) {
        this(modelId, appContext, DEFAULT_KEEP_RECENT_TOKENS, null);
    }

    public SnapCompactCompactor(String modelId, Object appContext,
                                int keepRecentTokens, Listener listener) {
        this.modelId = modelId;
        this.appContext = appContext;
        this.keepRecentTokens = keepRecentTokens;
        this.listener = listener;
    }

    @Override
    public String strategyName() {
        return "snapcompact";
    }

    @Override
    public LinkedList<AgentMessage> compact(LinkedList<AgentMessage> history,
                                            int maxInputTokens,
                                            int preserveRecentMessages) {
        if (history == null) return new LinkedList<>();

        // Ensure the BDF font registry is bound to a context (idempotent).
        if (appContext instanceof android.content.Context) {
            BdfFontRegistry.init((android.content.Context) appContext);
        }

        int start = 0;
        AgentMessage systemMsg = null;
        if (!history.isEmpty() && AgentMessage.ROLE_SYSTEM.equals(history.get(0).role)) {
            systemMsg = history.get(0);
            start = 1;
        }

        int remaining = history.size() - start;
        if (remaining <= 1) return history;

        int cutIndex = findCutPoint(history, start, keepRecentTokens);
        if (cutIndex <= start) return history;

        cutIndex = adjustCutPoint(history, start, cutIndex);
        if (cutIndex <= start) return history;

        List<AgentMessage> toArchive = new ArrayList<>();
        for (int i = start; i < cutIndex; i++) toArchive.add(history.get(i));
        List<AgentMessage> toKeep = new ArrayList<>();
        for (int i = cutIndex; i < history.size(); i++) toKeep.add(history.get(i));

        String priorSummary = extractPriorSummary(toArchive);

        try {
            SnapCompact.CompactionResult result =
                    SnapCompact.compact(toArchive, modelId, priorSummary);
            if (result.frames.isEmpty()) {
                // Nothing rendered — keep just the lead-in summary text.
                AgentMessage summaryMsg = AgentMessage.user(result.summary);
                return assembleResult(systemMsg, summaryMsg, toKeep);
            }
            // Build a user message carrying the summary text + images.
            AgentMessage summaryMsg = AgentMessage.userWithImages(
                    result.summary, new ArrayList<>(result.frames));
            if (listener != null) {
                listener.onCompactionEvent("snapcompact: archived "
                        + toArchive.size() + " messages into "
                        + result.frames.size() + " PNG frames, ~"
                        + result.estimatedTokens + " tokens.");
            }
            return assembleResult(systemMsg, summaryMsg, toKeep);
        } catch (Throwable t) {
            if (listener != null) {
                listener.onCompactionEvent("snapcompact render failed: "
                        + t.getMessage() + " — falling back to basic compaction.");
            }
            // Fallback: drop old, keep recent, no summary.
            return assembleResult(systemMsg, null, toKeep);
        }
    }

    /** Assemble the final history: system (if any), summary (if any), kept. */
    private LinkedList<AgentMessage> assembleResult(AgentMessage systemMsg,
                                                    AgentMessage summaryMsg,
                                                    List<AgentMessage> toKeep) {
        LinkedList<AgentMessage> out = new LinkedList<>();
        if (systemMsg != null) out.add(systemMsg);
        if (summaryMsg != null) out.add(summaryMsg);
        out.addAll(toKeep);
        return out;
    }

    /** Find the cut point: walk backward from the end until accumulated
     *  tokens ≥ {@code keepTokens}. */
    private int findCutPoint(LinkedList<AgentMessage> history, int start, int keepTokens) {
        int accumulated = 0;
        for (int i = history.size() - 1; i >= start; i--) {
            accumulated += history.get(i).estimateTokens();
            if (accumulated >= keepTokens) {
                return i;
            }
        }
        return start;
    }

    /** Adjust cut point so it never lands on a tool-result message. */
    private int adjustCutPoint(LinkedList<AgentMessage> history, int start, int cutIndex) {
        while (cutIndex > start) {
            AgentMessage m = history.get(cutIndex);
            if (m.hasToolResults()) {
                cutIndex--;
                continue;
            }
            break;
        }
        return cutIndex;
    }

    /** Extract a prior {@code <summary>...</summary>} block from the
     *  discarded region. Returns null when none is found. */
    private String extractPriorSummary(List<AgentMessage> discarded) {
        StringBuilder sb = new StringBuilder();
        for (AgentMessage m : discarded) {
            if (m.text != null) sb.append(m.text).append('\n');
        }
        Matcher m = SUMMARY_BLOCK_RE.matcher(sb.toString());
        if (m.find()) return m.group(1).trim();
        return null;
    }
}
