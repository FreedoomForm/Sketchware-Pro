package com.sketchware.ai.context.snapcompact;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Renders normalized snapcompact text into black-on-white PNG frames using
 * a bundled BDF bitmap font. Mirrors the role of {@code renderSnapcompactPng}
 * in oh-my-pi's {@code crates/pi-natives/src/snapcompact.rs}, but in pure
 * Java using Android's {@link Bitmap} for PNG encoding.
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
 * <p>{@link SnapCompactText#NEWLINE_GLYPH} ("\u2588") renders as a fully
 * ink-filled cell — line structure survives whitespace collapsing at a
 * one-cell cost, exactly like the upstream native renderer.
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
     * Pre-defined shape variants matching snapcompact.ts SHAPE_VARIANTS
     * (only the {@code bw} (black-on-white) subset is supported; the
     * {@code sent} multi-color-ink and {@code doc} two-column variants
     * are skipped as mobile bandwidth savers).
     */
    public static final class Shapes {
        /** 5x8 X.org legacy font on its 5x8 cell. Used by the {@code legacy} shape. */
        public static final Shape FONT_5x8 = new Shape(
                BdfFontRegistry.FontName.FONT_5x8, 5, 8, 1568, 3025);
        /** 6x12 X.org misc font. Used by the {@code 6x12-dim} research variant. */
        public static final Shape FONT_6x12 = new Shape(
                BdfFontRegistry.FontName.FONT_6x12, 6, 12, 1568, 3025);
        /** 8x13 glyphs on 8x16 cell pitch (extra leading). */
        public static final Shape ON_8x16 = new Shape(
                BdfFontRegistry.FontName.FONT_8x13, 8, 16, 1568, 3025);
        /** 8x13 glyphs on 22px pitch (more leading — eval winner for
         *  Gemini 3.x and GPT-5.x). */
        public static final Shape ON_8x22 = new Shape(
                BdfFontRegistry.FontName.FONT_8x13, 8, 22, 1568, 3025);
        /** 8x13 glyphs on 11px advance (extra tracking — eval winner for
         *  Claude). */
        public static final Shape ON_11x16 = new Shape(
                BdfFontRegistry.FontName.FONT_8x13, 11, 16, 1568, 3025);
    }

    /**
     * Pick the eval-winning shape for a model id. Mirrors
     * {@code resolveShape()} from snapcompact.ts (only the {@code bw}
     * variants; we skip the high-res 1932px Claude tier and the 2048px
     * Gemini tier to keep the implementation simple).
     *
     * <p>Defaults to {@link Shapes#ON_8x22} (the safe unknown-provider
     * shape).
     */
    public static Shape resolveShape(String modelId) {
        if (modelId == null) return Shapes.ON_8x22;
        String lower = modelId.toLowerCase();
        if (lower.contains("claude")) return Shapes.ON_11x16;
        if (lower.contains("gemini")) return Shapes.ON_8x22;
        if (lower.contains("gpt") || lower.contains("codex")) return Shapes.ON_8x22;
        if (lower.contains("glm")) return Shapes.ON_8x16;
        if (lower.contains("kimi")) return Shapes.ON_8x22;
        return Shapes.ON_8x22;
    }

    /**
     * Render one page of normalized text to a base64 PNG string. The text
     * must already be normalized via {@link SnapCompactText#normalize} and
     * must fit within the shape's capacity — excess characters are clipped
     * at the frame's right/bottom edge.
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
            BdfFont.Glyph g = font.glyph(cp);
            if (g == null) continue;
            // Place the glyph at the cell origin with its BDF bounding-box
            // offset. BDF yOffset is positive-above-baseline; we draw
            // top-down so convert: pen origin is at (cellX - xOffset,
            // cellY + cellHeight - 1 + yOffset). For simplicity and to
            // match the snapcompact renderer's "natural-size on the cell
            // pitch" behaviour, we draw the glyph's BBX top-left at
            // (cellX + xOffset, cellY + cellHeight - 1 - yOffset - height + 1).
            // In practice the bundled fonts have BBX offsets that place
            // glyphs correctly with this formula.
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
     * and CJK-wide-cell paths for simplicity).
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
