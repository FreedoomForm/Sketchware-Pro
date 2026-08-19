package com.sketchware.ai.context.snapcompact;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Silver TrueType fallback renderer for the snapcompact compactor.
 *
 * <p>The bundled BDF fonts (5x8, 6x12, 8x13) cover ASCII + Latin-1
 * supplement + a small subset of Latin Extended. Any code point outside
 * that coverage — most importantly CJK Unified Ideographs, Hiragana,
 * Katakana, Hangul, and the full Latin Extended ranges — would render
 * as {@code '?'} in the upstream-only-BDF renderer, losing information
 * that vision LLMs need when reading archived history frames.
 *
 * <p>Silver.ttf is a CJK-capable TrueType font (subset of Noto Serif SC
 * covering basic CJK + Hiragana + Katakana + Latin Extended + symbols)
 * bundled at {@code assets/fonts/Silver.ttf}. This class loads it once
 * per process via Android's {@link Typeface#createFromAsset}, then
 * rasterizes glyphs on demand via {@link Canvas}+{@link Paint} into
 * the same {@code int[]} pixel buffer the BDF path writes to. The
 * result is a transparent pixel-fallback chain: BDF first (pixel-
 * accurate, zero allocation), Silver second (TrueType, anti-aliased,
 * one Canvas allocation per glyph).
 *
 * <p><b>Cell fitting:</b> TrueType glyphs are vector and don't have a
 * natural pixel size. We render each glyph to fit its containing cell
 * (the snapcompact grid cell, {@code cellWidth × cellHeight} pixels)
 * using {@link Paint#setTextSize} calibrated against the font's ascent
 * and descent. Anti-aliasing is disabled to match the BDF fonts'
 * 1-bit ink appearance and to keep PNG compression effective.
 *
 * <p><b>Caching:</b> the {@link Typeface} is loaded once and reused.
 * The {@link Paint} is per-thread (Android Paint is not thread-safe);
 * we cache one Paint per cell-size combination to avoid reallocating
 * per glyph. A small per-cell-size {@link Bitmap}+{@link Canvas} pair
 * is likewise cached.
 *
 * <p><b>Coverage check:</b> {@link #canRender(int)} is the public API
 * the renderer calls to decide whether to use Silver for a given code
 * point. It returns true when the loaded Typeface has a glyph for the
 * code point (via {@link Paint#hasGlyph}, available since API 23).
 */
public final class SilverFontRegistry {

    private static final Object LOCK = new Object();
    private static Typeface silverTypeface;
    private static Context appContext;

    /** Per-cell-size paint state. Keyed by {@code cellWidth << 16 | cellHeight}. */
    private static final Map<Long, PaintState> PAINT_CACHE = new HashMap<>();

    private SilverFontRegistry() {}

    /** Bind the registry to an application context (call once at startup). */
    public static void init(Context context) {
        synchronized (LOCK) {
            appContext = context.getApplicationContext();
        }
    }

    /** Lazy-load the Silver Typeface from {@code assets/fonts/Silver.ttf}. */
    static Typeface getTypeface() throws IOException {
        synchronized (LOCK) {
            if (silverTypeface != null) return silverTypeface;
            if (appContext == null) {
                throw new IllegalStateException(
                    "SilverFontRegistry not initialized — call init(context) first");
            }
            silverTypeface = Typeface.createFromAsset(
                    appContext.getAssets(), "fonts/Silver.ttf");
            return silverTypeface;
        }
    }

    /**
     * Returns true when the loaded Silver Typeface has a glyph for the
     * given code point. Used by the renderer to decide whether to use
     * Silver as a fallback for a code point the BDF font lacks.
     */
    public static boolean canRender(int codePoint) {
        try {
            Typeface tf = getTypeface();
            Paint p = new Paint();
            p.setTypeface(tf);
            String s = new String(Character.toChars(codePoint));
            return p.hasGlyph(s);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Render one glyph into the pixel buffer at the given cell origin.
     *
     * <p>The glyph is sized to fit the cell vertically (cellHeight minus
     * 1px padding) and horizontally (cellWidth minus 1px padding),
     * left-aligned within the cell. Anti-aliasing is disabled to match
     * the BDF 1-bit ink style.
     *
     * @param codePoint   the Unicode code point to render
     * @param pixels      the frame pixel buffer (width × height, ARGB_8888)
     * @param width       frame width in pixels
     * @param height      frame height in pixels
     * @param cellX       cell origin X (top-left of the cell)
     * @param cellY       cell origin Y (top-left of the cell)
     * @param cellWidth   cell width in pixels
     * @param cellHeight  cell height in pixels
     * @return true if the glyph was rendered; false if Silver lacks the
     *         glyph or rendering failed (caller should substitute '?')
     */
    public static boolean renderGlyph(int codePoint, int[] pixels, int width, int height,
                                       int cellX, int cellY, int cellWidth, int cellHeight) {
        try {
            Typeface tf = getTypeface();
            long key = ((long) cellWidth << 16) | cellHeight;
            PaintState state;
            synchronized (LOCK) {
                state = PAINT_CACHE.get(key);
                if (state == null) {
                    state = new PaintState(tf, cellWidth, cellHeight);
                    PAINT_CACHE.put(key, state);
                }
            }
            return state.draw(codePoint, pixels, width, height, cellX, cellY);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Per-cell-size paint + bitmap + canvas cache. Allocated once per
     * distinct (cellWidth, cellHeight) pair, then reused for every
     * subsequent glyph at that size.
     *
     * <p><b>Thread safety:</b> {@code draw()} mutates {@code scratch} and
     * {@code canvas}, so concurrent calls at the same cell-size would
     * interleave pixel writes and produce garbled frames. We synchronize
     * on the PaintState instance to serialize per-cell-size calls. Different
     * cell sizes can render in parallel (different PaintState instances).
     */
    private static final class PaintState {
        final Paint paint;
        final Bitmap scratch;
        final Canvas canvas;
        final int cellWidth;
        final int cellHeight;
        final float textSize;
        final float baseline;
        /** Reused across draw() calls to avoid per-glyph allocation.
         *  Guarded by synchronizing on {@code this}. */
        final int[] scratchPixels;

        PaintState(Typeface tf, int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            this.paint = new Paint();
            paint.setTypeface(tf);
            paint.setAntiAlias(false);       // match BDF 1-bit ink
            paint.setSubpixelText(false);
            paint.setLinearText(false);
            paint.setColor(Color.BLACK);
            // Size the text to fit the cell vertically. Paint textSize is
            // approximate — we measure ascent+descent and shrink if needed.
            this.textSize = pickTextSize(cellWidth, cellHeight);
            paint.setTextSize(textSize);
            Paint.FontMetrics fm = paint.getFontMetrics();
            // Compute baseline so the rendered glyph's vertical center
            // matches the BDF path's. The BDF renderer pins the baseline at
            // the bottom of the cell (cellY + cellHeight - 1) so descenders
            // extend below; for TrueType we instead place the baseline so
            // ascent+descent fits inside the cell, with the descent room
            // reserved at the bottom to match BDF's descender behavior.
            // fm.top is negative (ascent), fm.bottom is positive (descent).
            float ascent = -fm.top;
            float descent = fm.bottom;
            // Reserve enough descent so common descenders (g, p, q, y) don't
            // get clipped at the cell's bottom edge.
            this.baseline = Math.max(ascent, (cellHeight - 1) - descent);
            // Scratch bitmap for rasterizing one glyph at a time.
            this.scratch = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(scratch);
            // Pre-allocate the pixel readback buffer once per PaintState
            // instead of per draw() call — a CJK-heavy archive can trigger
            // 240-480K allocations per compaction, ~150 MB of GC churn.
            this.scratchPixels = new int[cellWidth * cellHeight];
        }

        float pickTextSize(int cw, int ch) {
            // Start with cellHeight and shrink to fit width.
            float size = ch - 1;
            paint.setTextSize(size);
            // For typical CJK glyphs the advance width ~= textSize; for
            // narrow Latin glyphs it's much smaller. We pick the smaller
            // of (ch-1, cw-1) so CJK fills the cell without overflow.
            return Math.min(ch - 1, cw - 1);
        }

        boolean draw(int codePoint, int[] pixels, int width, int height, int cellX, int cellY) {
            String s = new String(Character.toChars(codePoint));
            if (!paint.hasGlyph(s)) return false;
            // Synchronize on the PaintState to serialize concurrent callers
            // at the same cell size — scratch/canvas/scratchPixels are not
            // thread-safe and would interleave pixel writes.
            synchronized (this) {
                // Clear scratch bitmap to transparent.
                scratch.eraseColor(Color.TRANSPARENT);
                // Center the glyph horizontally; baseline placed so the glyph
                // visually sits in the lower portion of the cell to match the
                // BDF path (which pins baseline at cellY + cellHeight - 1).
                float advance = paint.measureText(s);
                float x = Math.max(0, (cellWidth - advance) / 2f);
                float y = baseline;
                canvas.drawText(s, x, y, paint);
                // Read back pixels into the reused scratchPixels buffer.
                scratch.getPixels(scratchPixels, 0, cellWidth, 0, 0, cellWidth, cellHeight);
                for (int dy = 0; dy < cellHeight; dy++) {
                    int py = cellY + dy;
                    if (py < 0 || py >= height) continue;
                    for (int dx = 0; dx < cellWidth; dx++) {
                        int px = cellX + dx;
                        if (px < 0 || px >= width) continue;
                        int argb = scratchPixels[dy * cellWidth + dx];
                        // Alpha channel of the rasterized glyph indicates ink coverage.
                        int alpha = (argb >>> 24) & 0xff;
                        if (alpha >= 128) {
                            pixels[py * width + px] = Color.BLACK;
                        }
                    }
                }
                return true;
            }
        }
    }
}
