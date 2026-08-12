package com.sketchware.ai.context.snapcompact;

import com.sketchware.ai.agent.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapcompact bitmap-frame context compaction. This is the Java/Android
 * port of oh-my-pi's {@code packages/snapcompact/src/snapcompact.ts}.
 *
 * <p><b>The idea:</b> instead of asking an LLM to summarize discarded
 * conversation history (which costs an extra API call, adds latency, and
 * can hallucinate), the discarded history is serialized to a compact text
 * transcript and rendered into dense PNG frames of pixel-font glyphs that
 * vision-capable LLMs read back directly as image content blocks. The whole
 * pass is local and deterministic — no LLM call, no API key, no latency
 * beyond PNG rendering.
 *
 * <p>The pipeline:
 * <ol>
 *   <li>{@link SnapCompactText#serializeConversation} converts discarded
 *       {@link AgentMessage}s to the {@code ¶user:}/{@code ¶think:}/
 *       {@code ¶ai:}/{@code ¶call:} transcript format with {@code <out>}
 *       wrappers around tool results.</li>
 *   <li>{@link SnapCompactText#normalize} strips ANSI, collapses whitespace,
 *       folds punctuation/emoji/box-drawing to ASCII, and replaces newline
 *       runs with the solid-block {@link SnapCompactText#NEWLINE_GLYPH}
 *       glyph so line structure survives at one-cell cost.</li>
 *   <li>{@link SnapCompactRenderer#paginate} slices the normalized text into
 *       frame-capacity chunks.</li>
 *   <li>{@link SnapCompactRenderer#renderToBase64Png} rasterizes each chunk
 *       to a base64 PNG using the BDF bitmap font for the chosen shape.</li>
 *   <li>A short {@code "Resume prior conversation"} lead-in is prepended to
 *       the frames explaining the format to the reader model.</li>
 * </ol>
 *
 * <p>The result is one {@link AgentMessage} of role {@code user} carrying
 * the lead-in text + the rendered PNG frames as base64 images, which the
 * LLM provider serializes as image content blocks in the next request.
 *
 * <p><b>Simplifications vs. the upstream TypeScript implementation:</b>
 * <ul>
 *   <li>Only the {@code bw} (black-on-white) ink variant — no per-sentence
 *       color cycling, no stopword dimming.</li>
 *   <li>Only single-column row-major grid layout — no two-column doc layout.</li>
 *   <li>Only four frame shapes: {@code 8on16-bw}, {@code 8on22-bw},
 *       {@code 11on16-bw}, {@code 5x8-bw}.</li>
 *   <li>Frame size fixed at 1568px square (no 1932px Claude high-res tier,
 *       no 2048px Gemini tier) — keeps memory and JSON payload bounded.</li>
 *   <li>No foveated HQ/LQ/HQ middle tier — every frame is rendered at the
 *       same shape. Oldest frames are dropped when over budget.</li>
 *   <li>No Silver TrueType fallback for CJK — non-Latin text outside the
 *       BDF font's coverage is substituted with {@code '?'}. This is a
 *       known limitation; the snapcompact-summary prompt warns the model.</li>
 *   <li>Max 20 frames per compaction (vs upstream's 80) — safer for mobile
 *       bandwidth and JSON payload size on low-RAM devices.</li>
 *   <li>No iterative re-rendering of prior archive text — each compaction
 *       is a fresh render of only the newly-discarded messages. The prior
 *       compaction's summary is included as a {@code [Summary of earlier
 *       history]} head when present.</li>
 * </ul>
 *
 * <p>Despite these simplifications the core idea — discard history into
 * PNG frames that vision LLMs read back directly, with no LLM call — is
 * fully preserved.
 */
public final class SnapCompact {

    /** Upper bound on archive frames per compaction. Mirrors
     *  {@code MAX_FRAMES_DEFAULT} but tightened for mobile bandwidth. */
    public static final int MAX_FRAMES = 20;

    /** Hard cap on total base64 bytes per compaction (3 MB, matches the
     *  upstream {@code FRAME_DATA_BYTES_BUDGET}). */
    public static final int MAX_FRAME_DATA_BYTES = 3_000_000;

    /** Conservative per-frame token estimate used for context budgeting. */
    public static final int FRAME_TOKEN_ESTIMATE = 3025;

    private SnapCompact() {}

    /** Result of one compaction pass. */
    public static final class CompactionResult {
        /** Lead-in summary text for the reader model. */
        public final String summary;
        /** Rendered PNG frames as base64 strings (no data: prefix). */
        public final List<String> frames;
        /** Estimated tokens consumed by the frames. */
        public final int estimatedTokens;

        public CompactionResult(String summary, List<String> frames, int estimatedTokens) {
            this.summary = summary;
            this.frames = frames;
            this.estimatedTokens = estimatedTokens;
        }
    }

    /**
     * Run one snapcompact compaction over the discarded messages.
     *
     * @param discarded     the messages being archived (oldest first)
     * @param modelId       the model id of the live conversation's model;
     *                      selects the frame shape
     * @param priorSummary  optional summary text from a previous compaction,
     *                      prepended to the archive as
     *                      {@code [Summary of earlier history]}
     * @return the compaction result (summary text + base64 PNG frames)
     */
    public static CompactionResult compact(List<AgentMessage> discarded,
                                           String modelId,
                                           String priorSummary) throws Exception {
        SnapCompactText.SerializeOptions opts = new SnapCompactText.SerializeOptions();
        String serialized = SnapCompactText.serializeConversation(discarded, opts);
        if (serialized.isEmpty()) {
            return new CompactionResult("No prior history.", new ArrayList<>(), 0);
        }

        SnapCompactRenderer.Shape shape = SnapCompactRenderer.resolveShape(modelId);
        String normalized = SnapCompactText.normalize(serialized);

        // Prepend a prior-summary head when present (mirrors the upstream
        // behavior when migrating from a text-based compaction).
        StringBuilder archiveText = new StringBuilder();
        if (priorSummary != null && !priorSummary.isEmpty()) {
            String normalizedPrior = SnapCompactText.normalize(
                    "[Summary of earlier history] " + priorSummary);
            if (!normalizedPrior.isEmpty()) {
                archiveText.append(normalizedPrior)
                        .append(SnapCompactText.NEWLINE_GLYPH);
            }
        }
        archiveText.append(normalized);
        String text = archiveText.toString();

        int frameCapacity = shape.capacity();
        String[] pages = SnapCompactRenderer.paginate(text, frameCapacity);

        // Cap by frame count and by total base64 byte budget. When over
        // budget, keep the newest frames (more relevant to the live turn)
        // and drop the oldest.
        List<String> frames = new ArrayList<>();
        int totalBytes = 0;
        int keptCount = 0;
        int droppedFrames = 0;
        int droppedChars = 0;
        // Walk newest-first to compute the kept set, then render oldest-first
        // so the frames list is in chronological order.
        int keepCount = Math.min(pages.length, MAX_FRAMES);
        // First pass: estimate total bytes for the kept set; if over budget,
        // shrink keepCount.
        // (Per-frame base64 size is roughly proportional to ink pixels; we
        // approximate with 60 KB per frame, the measured size of an 8x13
        // frame at 30% ink coverage.)
        int approxBytesPerFrame = 60_000;
        while (keepCount > 0 && keepCount * approxBytesPerFrame > MAX_FRAME_DATA_BYTES) {
            keepCount--;
        }
        droppedFrames = pages.length - keepCount;
        // Dropped frames are the OLDEST ones.
        int firstKept = droppedFrames;
        for (int p = 0; p < droppedFrames; p++) {
            droppedChars += pages[p].length();
        }
        // Render the kept pages (oldest first).
        for (int p = firstKept; p < pages.length; p++) {
            String base64 = SnapCompactRenderer.renderToBase64Png(pages[p], shape);
            if (totalBytes + base64.length() > MAX_FRAME_DATA_BYTES) {
                // Hard byte-budget ceiling — drop this and all remaining
                // (newest) frames too. This is rare; we already shrank
                // keepCount above with the approximation.
                droppedFrames++;
                droppedChars += pages[p].length();
                continue;
            }
            frames.add(base64);
            totalBytes += base64.length();
        }
        keptCount = frames.size();

        int estimatedTokens = keptCount * shape.frameTokenEstimate;

        String summary = buildSummary(shape, keptCount, droppedChars, priorSummary != null);
        return new CompactionResult(summary, frames, estimatedTokens);
    }

    /** Build the lead-in summary text shown to the reader model. Mirrors
     *  the {@code snapcompact-summary.md} template. */
    static String buildSummary(SnapCompactRenderer.Shape shape,
                                int frameCount,
                                int truncatedChars,
                                boolean includedPreviousSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resume prior conversation. Earlier turns archived under HISTORY below, oldest→newest. Read HISTORY fully; continue the live conversation following it.\n\n");
        sb.append("Archived transcript scopes:\n");
        sb.append("- ¶user:, ¶think:, ¶ai:, ¶call:: user, assistant reasoning, assistant reply, tool call.\n");
        sb.append("- Unprefixed following lines: current scope. Consecutive same-kind blocks omit repeated prefix.\n");
        sb.append("- Tool call: ¶call:name(args); <out>…</out>: tool output.\n\n");
        sb.append("Reading HISTORY:\n");
        sb.append("- Plain text: verbatim transcript; rely on it exactly.\n");
        if (frameCount > 0) {
            sb.append("- Some middle sections: images, not text. Each image: one page of that transcript, in reading order between marked delimiters. Solid black cell: newline; runs of spaces collapse to one.\n");
            sb.append("  - Frame: one grid ").append(shape.cols())
                    .append(" characters wide, up to ").append(shape.rows())
                    .append(" rows tall; read left→right, top→bottom. No word wrap; words may break across rows.\n");
        }
        if (includedPreviousSummary) {
            sb.append("- HISTORY opens with a condensed digest of still-older context predating archived turns.\n");
        }
        if (truncatedChars > 0) {
            sb.append("- About ").append(truncatedChars)
                    .append(" characters of older middle history dropped to fit archive budget.\n");
        }
        sb.append("- If an exact earlier detail matters and a section is unclear, re-derive from workspace (re-read files, re-run commands), rather than guess.\n");
        sb.append("- Non-Latin characters outside the bundled bitmap font's coverage render as '?' in the frames.\n\n");
        sb.append("HISTORY\n");
        sb.append("===================\n");
        sb.append("(see attached images)");
        return sb.toString();
    }
}
