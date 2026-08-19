package com.sketchware.ai.context.snapcompact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser and in-memory representation of an X.org BDF (Bitmap Distribution
 * Format) bitmap font. Mirrors the role of the bundled BDF fonts in
 * oh-my-pi's native snapcompact renderer
 * ({@code crates/pi-natives/src/fonts/8x13.bdf}, {@code 5x8.bdf},
 * {@code 6x12.bdf}).
 *
 * <p>BDF files are public-domain X.org bitmap fonts. Each glyph is stored
 * as a hex bitmap of {@code BBX height} rows × {@code BBX width} columns,
 * with a per-glyph bounding-box offset relative to the pen origin. This
 * parser loads every glyph into a {@code byte[]} of width × height pixels
 * (0 = background, 1 = ink), row-major, top row first.
 *
 * <p>Only the subset of BDF needed by snapcompact is supported:
 * <ul>
 *   <li>{@code STARTFONT 2.1}</li>
 *   <li>{@code FONTBOUNDINGBOX} (font-global default box)</li>
 *   <li>{@code STARTCHAR}/{@code ENCODING}/{@code BBX}/{@code BITMAP}</li>
 *   <li>Hex bitmap rows, MSB-first within each byte</li>
 * </ul>
 *
 * <p>Properties, comments, and the {@code DEFAULT_CHAR} directive are
 * ignored. Glyphs with {@code ENCODING -1} (the BDF "undefined" sentinel)
 * are skipped.
 */
public final class BdfFont {

    /** One glyph: width, height, x-offset, y-offset, pixel grid. */
    public static final class Glyph {
        public final int width;
        public final int height;
        public final int xOffset;
        public final int yOffset; // BDF convention: positive = above baseline
        public final byte[] pixels; // width * height, row-major, 0/1

        public Glyph(int width, int height, int xOffset, int yOffset, byte[] pixels) {
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.pixels = pixels;
        }
    }

    /** Internal parse result: encoding + glyph together. */
    private static final class ParsedGlyph {
        final int encoding;
        final Glyph glyph;
        ParsedGlyph(int encoding, Glyph glyph) {
            this.encoding = encoding;
            this.glyph = glyph;
        }
    }

    private final String name;
    private final int fontWidth;   // FONTBOUNDINGBOX width
    private final int fontHeight;  // FONTBOUNDINGBOX height
    private final int fontXOffset;
    private final int fontYOffset;
    private final Map<Integer, Glyph> glyphs;
    private final Glyph missing; // substitute for unknown code points

    private BdfFont(String name, int fw, int fh, int fx, int fy,
                    Map<Integer, Glyph> glyphs, Glyph missing) {
        this.name = name;
        this.fontWidth = fw;
        this.fontHeight = fh;
        this.fontXOffset = fx;
        this.fontYOffset = fy;
        this.glyphs = glyphs;
        this.missing = missing;
    }

    public String name() { return name; }
    public int fontWidth() { return fontWidth; }
    public int fontHeight() { return fontHeight; }
    public int glyphCount() { return glyphs.size(); }

    /** Get the glyph for a code point, or the font's substitute if missing. */
    public Glyph glyph(int codePoint) {
        Glyph g = glyphs.get(codePoint);
        return g != null ? g : missing;
    }

    /** True if a glyph for the code point exists in the font. */
    public boolean hasGlyph(int codePoint) {
        return glyphs.containsKey(codePoint);
    }

    /**
     * Load a BDF font from an Android asset / resource stream. Closes the
     * stream when done.
     */
    public static BdfFont load(String name, InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.US_ASCII))) {
            int fw = 0, fh = 0, fx = 0, fy = 0;
            Map<Integer, Glyph> glyphs = new HashMap<>();
            Glyph missing = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("FONTBOUNDINGBOX")) {
                    String[] parts = line.split("\\s+");
                    fw = Integer.parseInt(parts[1]);
                    fh = Integer.parseInt(parts[2]);
                    fx = Integer.parseInt(parts[3]);
                    fy = Integer.parseInt(parts[4]);
                } else if (line.startsWith("STARTCHAR")) {
                    ParsedGlyph parsed = parseGlyph(reader);
                    if (parsed != null && parsed.glyph != null
                            && parsed.glyph.width > 0 && parsed.glyph.height > 0) {
                        glyphs.put(parsed.encoding, parsed.glyph);
                        if (missing == null) missing = parsed.glyph;
                    }
                }
            }

            if (missing == null) {
                missing = new Glyph(1, 1, 0, 0, new byte[]{1});
            }
            return new BdfFont(name, fw, fh, fx, fy, glyphs, missing);
        }
    }

    private static ParsedGlyph parseGlyph(BufferedReader reader) throws IOException {
        int encoding = -1;
        int width = 0, height = 0, xOffset = 0, yOffset = 0;
        boolean inBitmap = false;
        List<String> hexRows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("ENCODING")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        encoding = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                        encoding = -1;
                    }
                }
            } else if (line.startsWith("BBX")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    width = Integer.parseInt(parts[1]);
                    height = Integer.parseInt(parts[2]);
                    xOffset = Integer.parseInt(parts[3]);
                    yOffset = Integer.parseInt(parts[4]);
                }
            } else if (line.startsWith("BITMAP")) {
                inBitmap = true;
            } else if (line.startsWith("ENDCHAR")) {
                break;
            } else if (inBitmap) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) hexRows.add(trimmed);
            }
        }
        if (encoding < 0 || width <= 0 || height <= 0) {
            return null;
        }
        byte[] pixels = new byte[width * height];
        for (int row = 0; row < height && row < hexRows.size(); row++) {
            String hex = hexRows.get(row);
            for (int col = 0; col < width; col++) {
                int bitIndex = col;
                int byteIndex = bitIndex / 8;
                int bitInByte = 7 - (bitIndex % 8);
                if (byteIndex * 2 + 2 > hex.length()) break;
                String byteStr = hex.substring(byteIndex * 2, byteIndex * 2 + 2);
                int byteVal;
                try {
                    byteVal = Integer.parseInt(byteStr, 16);
                } catch (NumberFormatException e) {
                    break;
                }
                boolean ink = ((byteVal >> bitInByte) & 1) == 1;
                pixels[row * width + col] = (byte) (ink ? 1 : 0);
            }
        }
        return new ParsedGlyph(encoding, new Glyph(width, height, xOffset, yOffset, pixels));
    }
}
