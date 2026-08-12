package com.sketchware.ai.context.snapcompact;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Renders normalized snapcompact text into black-on-white PNG frames using
 * a bundled BDF bitmap font, with a Silver TrueType fallback for non-ASCII
 * code points outside the BDF font's coverage (notably CJK).
 *
 * <p>One {@link Shape} defines the font, the cell pitch (cell-width and
 * cell-height in pixels), the frame edge size, and the per-frame token
 * estimate used by the compactor for budget math. Each frame is a square
 * PNG of {@code frameSize × frameSize} pixels; the grid is
 * {@code floor(frameSize / cellWidth)} columns wide and
 * {@code floor(frameSize / cellHeight)} rows tall.
 *
 * <p>The renderer draws one glyph per cell at the glyph's natural pixel size
 * (BDF bitmaps are not scaled — they are pixel-accurate). When the glyph's
 * pixel size is smaller than the cell pitch, the glyph is left-aligned and
 * top-aligned within the cell (extra pitch becomes letter-spacing and
 * line-leading, which is exactly what the {@code 8on16-bw} / {@code 8on22-bw}
 * / {@code 11on16-bw} shape variants rely on for legibility).
 *
 * <p><b>CJK fallback (Silver TrueType)</b> — since 2026-08-12. When the
 * BDF font lacks a glyph for a code point, the renderer falls back to
 * {@link SilverFontRegistry#renderGlyph}, which rasterizes the code point
 * via Android {@link android.graphics.Typeface}+{@link android.graphics.Canvas}
 * using the bundled {@code assets/fonts/Silver.ttf}. Silver covers ASCII,
 * Latin Extended, CJK Unified Ideographs (basic), Hiragana, Katakana,
 * Bopomofo, CJK Symbols and Punctuation, and CJK Compatibility forms.
 * If Silver also lacks the glyph, the cell is left blank (no {@code '?'}
 * substitution — the BDF font's missing-glyph rendering is already a
 * visible mark).
 *
 * <p>{@link SnapCompactText#NEWLINE_GLYPH} ("\u2588") renders as a fully
 * ink-filled cell — line structure survives whitespace collapsing at a
 * one-cell cost, exactly like the upstream native renderer.
 *
 * <h2>Foveated HQ/LQ/HQ rendering (since 2026-08-12)</h2>
 *
 * <p>The renderer now supports three frame-size tiers:
 * <ul>
 *   <li><b>1568px</b> — mobile default (LQ tier)</li>
 *   <li><b>1932px</b> — Claude HQ tier (Claude's vision model performs
 *       better at this resolution)</li>
 *   <li><b>2048px</b> — Gemini HQ tier (Gemini's vision model performs
 *       better at this resolution)</li>
 * </ul>
 *
 * <p>The compactor ({@link SnapCompact}) partitions the archived pages
 * into three groups — oldest third, middle third, newest third — and
 * renders the oldest and newest groups at the HQ tier, the middle at
 * the LQ tier. This preserves high visual quality at the boundaries
 * (most relevant to the live turn) while saving billed tokens in the
 * middle (less relevant historical context).
 */
public final class SnapCompactRenderer {

    /** One renderable frame shape. Mirrors {@code Shape} in snapcompact.ts. */
    public static final class Shape {
        public final BdfFontRegistry.FontName font;
        public final int cellWidth;   // grid column advance, pixels
        public final int cellHeight;  // grid row pitch, pixels
        public final int frameSize;   // square frame edge, pixels
        public final int frameTokenEstimate;  // billed-token estimate

        public Shape(BdfFontRegistry.FontName font, int cellWidth, int cellHeight,
                     int frameSize, int frameTokenEstimate) {
            this.font = font;
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            this.frameSize = frameSize;
            this.frameTokenEstimate = frameTokenEstimate;
        }

        /** Grid columns in one frame. */
        public int cols() { return frameSize / cellWidth; }
        /** Grid rows in one frame. */
        public int rows() { return frameSize / cellHeight; }
        /** Character capacity of one frame. */
        public int capacity() { return cols() * rows(); }
    }

    /**
     * Pre-defined shape variants matching snapcompact.ts SHAPE_VARIANTS.
     * Each variant comes in three frame-size tiers (1568 / 1932 / 2048)
     * so the compactor can pick the foveated HQ/LQ/HQ distribution.
     */
    public static final class Shapes {
        /** 5x8 X.org legacy font on its 5x8 cell. Used by the {@code legacy} shape. */
        public static final Shape FONT_5x8       = new Shape(BdfFontRegistry.FontName.FONT_5x8,  5,  8, 1568, 3025);
        public static final Shape FONT_5x8_1932  = new Shape(BdfFontRegistry.FontName.FONT_5x8,  5,  8, 1932, 4550);
        public static final Shape FONT_5x8_2048  = new Shape(BdfFontRegistry.FontName.FONT_5x8,  5,  8, 2048, 5100);
        /** 6x12 X.org misc font. Used by the {@code 6x12-dim} research variant. */
        public static final Shape FONT_6x12      = new Shape(BdfFontRegistry.FontName.FONT_6x12, 6, 12, 1568, 3025);
        public static final Shape FONT_6x12_1932 = new Shape(BdfFontRegistry.FontName.FONT_6x12, 6, 12, 1932, 4550);
        public static final Shape FONT_6x12_2048 = new Shape(BdfFontRegistry.FontName.FONT_6x12, 6, 12, 2048, 5100);
        /** 8x13 glyphs on 8x16 cell pitch (extra leading). */
        public static final Shape ON_8x16        = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 16, 1568, 3025);
        public static final Shape ON_8x16_1932   = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 16, 1932, 4550);
        public static final Shape ON_8x16_2048   = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 16, 2048, 5100);
        /** 8x13 glyphs on 22px pitch (more leading — eval winner for
         *  Gemini 3.x and GPT-5.x). */
        public static final Shape ON_8x22        = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 22, 1568, 3025);
        public static final Shape ON_8x22_1932   = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 22, 1932, 4550);
        public static final Shape ON_8x22_2048   = new Shape(BdfFontRegistry.FontName.FONT_8x13, 8, 22, 2048, 5100);
        /** 8x13 glyphs on 11px advance (extra tracking — eval winner for
         *  Claude). */
        public static final Shape ON_11x16       = new Shape(BdfFontRegistry.FontName.FONT_8x13, 11, 16, 1568, 3025);
        public static final Shape ON_11x16_1932  = new Shape(BdfFontRegistry.FontName.FONT_8x13, 11, 16, 1932, 4550);
        public static final Shape ON_11x16_2048  = new Shape(BdfFontRegistry.FontName.FONT_8x13, 11, 16, 2048, 5100);
    }

    /**
     * Foveated shape pair: one HQ shape + one LQ shape for the same
     * font/cell combination. The compactor partitions archived pages
     * into HQ/LQ/HQ thirds and renders each third at the matching shape.
     */
    public static final class FoveatedShapes {
        public final Shape hq;
        public final Shape lq;
        public FoveatedShapes(Shape hq, Shape lq) {
            this.hq = hq;
            this.lq = lq;
        }
    }

    /**
     * Pick the eval-winning foveated shape pair for a model id. Mirrors
     * {@code resolveShape()} from snapcompact.ts, extended with the
     * 1932px Claude HQ tier and the 2048px Gemini HQ tier.
     *
     * <p>Defaults to the 1568/1932 {@link Shapes#ON_8x22} pair (the
     * safe unknown-provider combination).
     */
    public static FoveatedShapes resolveFoveatedShapes(String modelId) {
        if (modelId == null) return new FoveatedShapes(Shapes.ON_8x22_1932, Shapes.ON_8x22);
        String lower = modelId.toLowerCase();
        if (lower.contains("claude")) return new FoveatedShapes(Shapes.ON_11x16_1932, Shapes.ON_11x16);
        if (lower.contains("gemini")) return new FoveatedShapes(Shapes.ON_8x22_2048, Shapes.ON_8x22);
        if (lower.contains("gpt") || lower.contains("codex")) return new FoveatedShapes(Shapes.ON_8x22_1932, Shapes.ON_8x22);
        if (lower.contains("glm")) return new FoveatedShapes(Shapes.ON_8x16_1932, Shapes.ON_8x16);
        if (lower.contains("kimi")) return new FoveatedShapes(Shapes.ON_8x22_1932, Shapes.ON_8x22);
        return new FoveatedShapes(Shapes.ON_8x22_1932, Shapes.ON_8x22);
    }

    /**
     * Pick the eval-winning shape for a model id (single-tier API,
     * preserved for backward compatibility). Returns the LQ shape from
     * the foveated pair.
     */
    public static Shape resolveShape(String modelId) {
        return resolveFoveatedShapes(modelId).lq;
    }

    /**
     * Render one page of normalized text to a base64 PNG string. The text
     * must already be normalized via {@link SnapCompactText#normalize} and
     * must fit within the shape's capacity — excess characters are clipped
     * at the frame's right/bottom edge.
     *
     * <p>Code points outside the BDF font's coverage fall back to the
     * Silver TrueType renderer ({@link SilverFontRegistry#renderGlyph}).
     *
     * @param text  normalized text to render
     * @param shape shape controlling font + grid + frame size
     * @return base64-encoded PNG (no data: prefix)
     */
    public static String renderToBase64Png(String text, Shape shape) throws Exception {
        BdfFont font = BdfFontRegistry.get(shape.font);
        int cols = shape.cols();
        int rows = shape.rows();
        int width = shape.frameSize;
        int height = shape.frameSize;
        int[] pixels = new int[width * height];
        // Fill with white background.
        for (int i = 0; i < pixels.length; i++) pixels[i] = Color.WHITE;

        int cellIndex = 0;
        int maxCells = cols * rows;
        for (int i = 0; i < text.length() && cellIndex < maxCells; ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            String ch = new String(Character.toChars(cp));
            int col = cellIndex % cols;
            int row = cellIndex / cols;
            cellIndex++;
            int cellX = col * shape.cellWidth;
            int cellY = row * shape.cellHeight;
            if (ch.equals(SnapCompactText.NEWLINE_GLYPH)) {
                // Solid black cell.
                for (int dy = 0; dy < shape.cellHeight && cellY + dy < height; dy++) {
                    for (int dx = 0; dx < shape.cellWidth && cellX + dx < width; dx++) {
                        pixels[(cellY + dy) * width + (cellX + dx)] = Color.BLACK;
                    }
                }
                continue;
            }
            // Try BDF first.
            BdfFont.Glyph g = font.glyph(cp);
            if (g != null && font.hasGlyph(cp)) {
                int gx = cellX + g.xOffset;
                int gy = cellY + (shape.cellHeight - 1) - (g.yOffset + g.height - 1);
                for (int dy = 0; dy < g.height; dy++) {
                    for (int dx = 0; dx < g.width; dx++) {
                        if (g.pixels[dy * g.width + dx] == 0) continue;
                        int px = gx + dx;
                        int py = gy + dy;
                        if (px < 0 || px >= width || py < 0 || py >= height) continue;
                        pixels[py * width + px] = Color.BLACK;
                    }
                }
                continue;
            }
            // BDF lacks the glyph → try Silver TrueType fallback.
            // Silver covers CJK + Latin Extended; if Silver also lacks the
            // glyph, the cell is left blank (no '?' substitution — the
            // BDF font's missing-glyph rendering is already a visible mark
            // and we don't want to double-render).
            SilverFontRegistry.renderGlyph(cp, pixels, width, height,
                    cellX, cellY, shape.cellWidth, shape.cellHeight);
        }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
        if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)) {
            throw new RuntimeException("PNG compress failed");
        }
        bmp.recycle();
        byte[] pngBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(pngBytes);
    }

    /**
     * Paginate normalized text into chunks of {@code capacity} code points,
     * preserving code-point boundaries. Mirrors the row-major grid path of
     * {@code paginateCells} in snapcompact.ts (we skip the doc-2-column
     * path for simplicity; the foveated HQ/LQ/HQ split happens at the
     * compactor level, not the paginator).
     */
    public static String[] paginate(String text, int capacity) {
        if (text == null || text.isEmpty()) return new String[0];
        java.util.List<String> pages = new java.util.ArrayList<>();
        int[] codePoints = text.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i += capacity) {
            int end = Math.min(i + capacity, codePoints.length);
            StringBuilder sb = new StringBuilder(end - i);
            for (int j = i; j < end; j++) sb.appendCodePoint(codePoints[j]);
            pages.add(sb.toString());
        }
        return pages.toArray(new String[0]);
    }
}
