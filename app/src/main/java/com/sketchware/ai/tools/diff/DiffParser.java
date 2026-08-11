package com.sketchware.ai.tools.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses and applies Cline-style SEARCH/REPLACE diff blocks to a source file.
 *
 * <p>Mirrors Cline's {@code core/assistant-message/diff.ts} (v1 path) —
 * a single SEARCH/REPLACE block looks like:
 * <pre>
 * &lt;&lt;&lt;&lt;&lt;&lt;&lt; SEARCH
 *  lines to find
 * =======
 *  replacement lines
 * &gt;&gt;&gt;&gt;&gt;&gt;&gt; REPLACE
 * </pre>
 *
 * <p>The parser uses a 3-tier matching strategy (port of Cline's algorithm):
 * <ol>
 *   <li><b>Exact match</b> — search block text appears verbatim in the source.</li>
 *   <li><b>Line-trimmed match</b> — strip leading/trailing whitespace from each
 *       line on both sides and try again. Handles indentation drift.</li>
 *   <li><b>Block-anchor match</b> — find the first &amp; last non-blank lines of
 *       the search block as anchors, locate them in the source, then verify
 *       the interior lines match (line-trimmed). Handles reflowed whitespace
 *       inside the block.</li>
 * </ol>
 *
 * <p>If none of the three tiers match, {@link #apply(String, List)} throws
 * {@link DiffApplyException} with a context-rich message so the LLM can recover.
 *
 * <p>Designed to be Android-safe: no JDK-11+ APIs, no streaming, no I/O. Just
 * string manipulation.
 */
public final class DiffParser {

    public static final String SEARCH_MARKER = "<<<<<<< SEARCH";
    public static final String SEPARATOR     = "=======";
    public static final String REPLACE_MARKER = ">>>>>>> REPLACE";

    private DiffParser() {}

    /** A single SEARCH/REPLACE block parsed from the diff input. */
    public static final class Block {
        public final String search;
        public final String replace;
        public Block(String search, String replace) {
            this.search = search;
            this.replace = replace;
        }
    }

    /** Thrown when a SEARCH block can't be located in the source. */
    public static final class DiffApplyException extends Exception {
        public final int blockIndex;
        public final String searchSnippet;
        public DiffApplyException(int blockIndex, String searchSnippet, String message) {
            super(message);
            this.blockIndex = blockIndex;
            this.searchSnippet = searchSnippet;
        }
    }

    /**
     * Parse a diff string into a list of SEARCH/REPLACE blocks.
     * Blank lines outside of blocks are ignored. Returns an empty list
     * if the input contains no markers.
     *
     * @throws DiffApplyException if a block is malformed (missing REPLACE marker, etc.)
     */
    public static List<Block> parse(String diff) throws DiffApplyException {
        List<Block> blocks = new ArrayList<>();
        if (diff == null || diff.isEmpty()) return blocks;

        String[] lines = diff.split("\n", -1);
        int i = 0;
        int blockIdx = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.trim().equals(SEARCH_MARKER) || line.startsWith(SEARCH_MARKER)) {
                // Collect search section.
                StringBuilder search = new StringBuilder();
                i++;
                boolean foundSep = false;
                while (i < lines.length) {
                    String s = lines[i];
                    if (s.trim().equals(SEPARATOR)) { foundSep = true; i++; break; }
                    if (search.length() > 0) search.append('\n');
                    search.append(s);
                    i++;
                }
                if (!foundSep) {
                    throw new DiffApplyException(blockIdx, snippet(search.toString()),
                            "Block " + blockIdx + ": missing '=======' separator after SEARCH");
                }
                // Collect replace section.
                StringBuilder replace = new StringBuilder();
                boolean foundEnd = false;
                while (i < lines.length) {
                    String r = lines[i];
                    if (r.trim().equals(REPLACE_MARKER) || r.startsWith(REPLACE_MARKER)) {
                        foundEnd = true;
                        i++;
                        break;
                    }
                    if (replace.length() > 0) replace.append('\n');
                    replace.append(r);
                    i++;
                }
                if (!foundEnd) {
                    throw new DiffApplyException(blockIdx, snippet(search.toString()),
                            "Block " + blockIdx + ": missing '>>>>>>> REPLACE' end marker");
                }
                blocks.add(new Block(search.toString(), replace.toString()));
                blockIdx++;
            } else {
                i++;
            }
        }
        return blocks;
    }

    /**
     * Apply a list of SEARCH/REPLACE blocks to a source string. Each block
     * is applied in order; the result of one block is the input to the next.
     *
     * <p>3-tier matching:
     * <ol>
     *   <li>Exact substring match.</li>
     *   <li>Line-trimmed match (whitespace-normalized per line).</li>
     *   <li>Block-anchor match (first+last non-blank anchors).</li>
     * </ol>
     *
     * @throws DiffApplyException if any block fails all three matching tiers.
     */
    public static String apply(String source, List<Block> blocks) throws DiffApplyException {
        if (source == null) source = "";
        String current = source;
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String result = applyOne(current, b, i);
            current = result;
        }
        return current;
    }

    private static String applyOne(String source, Block b, int blockIdx) throws DiffApplyException {
        // Tier 1: exact match.
        if (!b.search.isEmpty() && source.contains(b.search)) {
            return source.replace(b.search, b.replace);
        }
        if (b.search.isEmpty()) {
            // Empty search means "insert at end of file".
            return source + (source.endsWith("\n") || source.isEmpty() ? "" : "\n") + b.replace;
        }

        // Tier 2: line-trimmed match.
        String trimmed = lineTrimmedMatch(source, b.search);
        if (trimmed != null) {
            return source.replace(trimmed, b.replace);
        }

        // Tier 3: block-anchor match.
        String anchored = blockAnchorMatch(source, b.search, b.replace);
        if (anchored != null) return anchored;

        throw new DiffApplyException(blockIdx, snippet(b.search),
                "Block " + blockIdx + ": SEARCH content not found in source (tried exact, "
                        + "line-trimmed, and block-anchor matching). Search snippet:\n"
                        + snippet(b.search));
    }

    /**
     * Tier 2: split both source and search into lines, trim each line of
     * leading/trailing whitespace, then look for the search sequence in
     * the source. Returns the (possibly different-whitespace) source
     * substring that matches, so the caller can do a plain replace.
     */
    private static String lineTrimmedMatch(String source, String search) {
        String[] srcLines = source.split("\n", -1);
        String[] schLines = search.split("\n", -1);
        if (schLines.length == 0 || schLines.length > srcLines.length) return null;

        // Precompute trimmed search lines.
        String[] schTrimmed = new String[schLines.length];
        for (int i = 0; i < schLines.length; i++) schTrimmed[i] = schLines[i].trim();

        for (int start = 0; start <= srcLines.length - schTrimmed.length; start++) {
            boolean ok = true;
            for (int j = 0; j < schTrimmed.length; j++) {
                if (!srcLines[start + j].trim().equals(schTrimmed[j])) { ok = false; break; }
            }
            if (ok) {
                // Found a line-trimmed match — rebuild the original source substring.
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < schTrimmed.length; j++) {
                    if (j > 0) sb.append('\n');
                    sb.append(srcLines[start + j]);
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Tier 3: take the first &amp; last non-blank lines of the search block as
     * anchors, find them in the source, then verify the interior lines match
     * (line-trimmed). If so, replace the whole source range with the replace block.
     */
    private static String blockAnchorMatch(String source, String search, String replace) {
        String[] srcLines = source.split("\n", -1);
        String[] schLines = search.split("\n", -1);

        int firstNonBlank = -1, lastNonBlank = -1;
        for (int i = 0; i < schLines.length; i++) {
            if (!schLines[i].trim().isEmpty()) {
                if (firstNonBlank < 0) firstNonBlank = i;
                lastNonBlank = i;
            }
        }
        if (firstNonBlank < 0) return null; // search is all blank
        String anchorFirst = schLines[firstNonBlank].trim();
        String anchorLast = schLines[lastNonBlank].trim();

        // Find anchorFirst in source (line-trimmed).
        for (int a = 0; a < srcLines.length; a++) {
            if (!srcLines[a].trim().equals(anchorFirst)) continue;
            // Find anchorLast at position >= a + (lastNonBlank - firstNonBlank).
            int minDist = lastNonBlank - firstNonBlank;
            for (int z = a + minDist; z < srcLines.length; z++) {
                if (!srcLines[z].trim().equals(anchorLast)) continue;
                // Verify interior: line count between a and z matches schLines interior.
                int srcSpan = z - a + 1;
                int schSpan = lastNonBlank - firstNonBlank + 1;
                if (srcSpan != schSpan) continue;
                // Verify each interior line matches (line-trimmed).
                boolean ok = true;
                for (int k = 0; k < schSpan; k++) {
                    if (!srcLines[a + k].trim().equals(schLines[firstNonBlank + k].trim())) {
                        ok = false; break;
                    }
                }
                if (!ok) continue;
                // Match found — rebuild source with replace substituted in.
                StringBuilder out = new StringBuilder();
                for (int i = 0; i < a; i++) {
                    if (i > 0) out.append('\n');
                    out.append(srcLines[i]);
                }
                if (a > 0) out.append('\n');
                out.append(replace);
                for (int i = z + 1; i < srcLines.length; i++) {
                    out.append('\n').append(srcLines[i]);
                }
                return out.toString();
            }
        }
        return null;
    }

    /** Produce a short snippet for error messages. */
    private static String snippet(String s) {
        if (s == null) return "<null>";
        int max = 200;
        return s.length() <= max ? s : s.substring(0, max) + "...(" + s.length() + " chars total)";
    }

    /**
     * Validate that a diff string is well-formed (all blocks have matching
     * SEARCH/REPLACE markers). Returns null on success, an error message otherwise.
     */
    public static String validate(String diff) {
        try {
            parse(diff);
            return null;
        } catch (DiffApplyException e) {
            return e.getMessage();
        }
    }

    /** Convenience: parse + apply in one call. */
    public static String applyDiff(String source, String diff) throws DiffApplyException {
        return apply(source, parse(diff));
    }

    /**
     * Helper for tests and consumers that need to know whether a SEARCH block
     * would match against the source without actually applying it.
     */
    public static boolean wouldMatch(String source, String search) {
        if (search == null || search.isEmpty()) return true;
        if (source == null) source = "";
        if (source.contains(search)) return true;
        if (lineTrimmedMatch(source, search) != null) return true;
        // Block-anchor check without replacement.
        String[] srcLines = source.split("\n", -1);
        String[] schLines = search.split("\n", -1);
        int firstNonBlank = -1, lastNonBlank = -1;
        for (int i = 0; i < schLines.length; i++) {
            if (!schLines[i].trim().isEmpty()) {
                if (firstNonBlank < 0) firstNonBlank = i;
                lastNonBlank = i;
            }
        }
        if (firstNonBlank < 0) return true;
        String anchorFirst = schLines[firstNonBlank].trim();
        String anchorLast = schLines[lastNonBlank].trim();
        for (int a = 0; a < srcLines.length; a++) {
            if (!srcLines[a].trim().equals(anchorFirst)) continue;
            int minDist = lastNonBlank - firstNonBlank;
            for (int z = a + minDist; z < srcLines.length; z++) {
                if (!srcLines[z].trim().equals(anchorLast)) continue;
                int srcSpan = z - a + 1;
                int schSpan = lastNonBlank - firstNonBlank + 1;
                if (srcSpan != schSpan) continue;
                boolean ok = true;
                for (int k = 0; k < schSpan; k++) {
                    if (!srcLines[a + k].trim().equals(schLines[firstNonBlank + k].trim())) {
                        ok = false; break;
                    }
                }
                if (ok) return true;
            }
        }
        return false;
    }
}
