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
 *       glyph so line structure survives at one-cell cost. Non-ASCII
 *       code points that the BDF font lacks are passed through unchanged
 *       so the renderer can fall back to the Silver TrueType font.</li>
 *   <li>{@link SnapCompactRenderer#paginate} slices the normalized text into
 *       frame-capacity chunks (LQ tier capacity).</li>
 *   <li>{@link SnapCompactRenderer#renderToBase64Png} rasterizes each chunk
 *       to a base64 PNG using the BDF bitmap font for the chosen shape,
 *       with the Silver TrueType fallback for code points the BDF font
 *       lacks (notably CJK).</li>
 *   <li>A short {@code "Resume prior conversation"} lead-in is prepended to
 *       the frames explaining the format to the reader model.</li>
 * </ol>
 *
 * <p>The result is one {@link AgentMessage} of role {@code user} carrying
 * the lead-in text + the rendered PNG frames as base64 images, which the
 * LLM provider serializes as image content blocks in the next request.
 *
 * <h2>Foveated HQ/LQ/HQ rendering (since 2026-08-12)</h2>
 *
 * <p>The compactor partitions the archived pages into three groups —
 * oldest third, middle third, newest third — and renders the oldest and
 * newest groups at the model-specific HQ tier (1932px Claude / 2048px
 * Gemini / 1932px default), the middle at the LQ tier (1568px). This
 * preserves high visual quality at the boundaries (most relevant to the
 * live turn) while saving billed tokens in the middle (less relevant
 * historical context).
 *
 * <p>When the total page count exceeds {@link #MAX_FRAMES}, the oldest
 * pages are dropped first (mirrors the upstream behavior).
 *
 * <h2>CJK fallback via Silver TrueType (since 2026-08-12)</h2>
 *
 * <p>Code points outside the bundled BDF fonts' coverage — most notably
 * CJK Unified Ideographs, Hiragana, Katakana, Bopomofo, and the full
 * Latin Extended ranges — are now rendered via the bundled
 * {@code assets/fonts/Silver.ttf} TrueType font using Android
 * {@link android.graphics.Typeface}+{@link android.graphics.Canvas}.
 * The BDF path is tried first (pixel-accurate, zero allocation); the
 * Silver path is the fallback (one Canvas allocation per glyph, cached
 * per cell size).
 *
 * <p>Silver covers: ASCII, Latin-1 supplement, Latin Extended-A/B, IPA,
 * general punctuation, arrows, math operators, box drawing, geometric
 * shapes, CJK Symbols and Punctuation, Hiragana, Katakana, Bopomofo,
 * Hangul Compatibility Jamo, Enclosed CJK Letters, CJK Unified
 * Ideographs (basic — modern Chinese), CJK Compatibility Ideographs,
 * CJK Compatibility Forms, Halfwidth and Fullwidth Forms.
 */
public final class SnapCompact {

    /** Upper bound on archive frames per compaction. Mirrors
     *  {@code MAX_FRAMES_DEFAULT} from the upstream snapcompact.ts. */
    public static final int MAX_FRAMES = 80;

    /** Hard cap on total base64 bytes per compaction (10 MB, raised from
     *  the upstream 3 MB to accommodate the higher HQ tier frame sizes).
     *  The per-frame base64 size of a 1932px HQ frame at 30% ink is
     *  ~85 KB; 80 frames × 85 KB ≈ 6.8 MB, well under this cap. */
    public static final int MAX_FRAME_DATA_BYTES = 10_000_000;

    /** Conservative per-frame token estimate used for context budgeting
     *  (LQ tier; HQ tier is ~1.5× this). */
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
     *                      selects the foveated HQ/LQ/HQ shape pair
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

        SnapCompactRenderer.FoveatedShapes foveated = SnapCompactRenderer.resolveFoveatedShapes(modelId);
        SnapCompactRenderer.Shape hqShape = foveated.hq;
        SnapCompactRenderer.Shape lqShape = foveated.lq;
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

        // Paginate at the LQ shape's capacity (smaller per-frame text).
        // The HQ tier has larger capacity but we want consistent page
        // boundaries across tiers; HQ frames will simply render the same
        // text at higher resolution.
        int frameCapacity = lqShape.capacity();
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
        // shrink keepCount. The estimate uses a per-tier byte cost:
        //   - HQ frames (first third + last third of the kept list) at ~85 KB
        //   - LQ frames (middle third) at ~60 KB
        int hqCount = (keepCount + 2) / 3;  // first third (rounded up)
        int lqCount = keepCount - 2 * hqCount;
        if (lqCount < 0) lqCount = 0;
        int approxBytesPerHqFrame = 85_000;
        int approxBytesPerLqFrame = 60_000;
        while (keepCount > 0) {
            hqCount = (keepCount + 2) / 3;
            lqCount = keepCount - 2 * hqCount;
            if (lqCount < 0) lqCount = 0;
            int approx = hqCount * approxBytesPerHqFrame + lqCount * approxBytesPerLqFrame;
            if (approx <= MAX_FRAME_DATA_BYTES) break;
            keepCount--;
        }
        droppedFrames = pages.length - keepCount;
        // Dropped frames are the OLDEST ones.
        int firstKept = droppedFrames;
        for (int p = 0; p < droppedFrames; p++) {
            droppedChars += pages[p].length();
        }

        // Render the kept pages with the foveated HQ/LQ/HQ split.
        // Page indices in [firstKept, firstKept + keepCount):
        //   [firstKept                  .. firstKept + hqCount)            → HQ (oldest)
        //   [firstKept + hqCount        .. firstKept + hqCount + lqCount)  → LQ (middle)
        //   [firstKept + hqCount + lqCount .. firstKept + keepCount)       → HQ (newest)
        int hqOldEnd = firstKept + hqCount;
        int lqEnd = firstKept + hqCount + lqCount;
        int totalHqTokens = 0;
        int totalLqTokens = 0;
        for (int p = firstKept; p < pages.length; p++) {
            SnapCompactRenderer.Shape shape;
            if (p < hqOldEnd || p >= lqEnd) {
                shape = hqShape;
            } else {
                shape = lqShape;
            }
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
            if (shape == hqShape) totalHqTokens += shape.frameTokenEstimate;
            else                  totalLqTokens += shape.frameTokenEstimate;
        }
        keptCount = frames.size();

        int estimatedTokens = totalHqTokens + totalLqTokens;

        String summary = buildSummary(foveated, keptCount, droppedChars, priorSummary != null);
        return new CompactionResult(summary, frames, estimatedTokens);
    }

    /** Build the lead-in summary text shown to the reader model. Mirrors
     *  the {@code snapcompact-summary.md} template. */
    static String buildSummary(SnapCompactRenderer.FoveatedShapes foveated,
                                int frameCount,
                                int truncatedChars,
                                boolean includedPreviousSummary) {
        SnapCompactRenderer.Shape hqShape = foveated.hq;
        SnapCompactRenderer.Shape lqShape = foveated.lq;
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
            sb.append("  - HQ frames (oldest third + newest third): one grid ")
                    .append(hqShape.cols())
                    .append(" characters wide, up to ")
                    .append(hqShape.rows())
                    .append(" rows tall at ")
                    .append(hqShape.frameSize)
                    .append("px resolution; read left→right, top→bottom. No word wrap; words may break across rows.\n");
            sb.append("  - LQ frames (middle third): one grid ")
                    .append(lqShape.cols())
                    .append(" characters wide, up to ")
                    .append(lqShape.rows())
                    .append(" rows tall at ")
                    .append(lqShape.frameSize)
                    .append("px resolution; same reading order.\n");
            sb.append("  - CJK characters render via the Silver TrueType fallback (vector font, anti-aliased off).\n");
        }
        if (includedPreviousSummary) {
            sb.append("- HISTORY opens with a condensed digest of still-older context predating archived turns.\n");
        }
        if (truncatedChars > 0) {
            sb.append("- About ").append(truncatedChars)
                    .append(" characters of older middle history dropped to fit archive budget.\n");
        }
        sb.append("- If an exact earlier detail matters and a section is unclear, re-derive from workspace (re-read files, re-run commands), rather than guess.\n\n");
        sb.append("HISTORY\n");
        sb.append("===================\n");
        sb.append("(see attached images)");
        return sb.toString();
    }
}
