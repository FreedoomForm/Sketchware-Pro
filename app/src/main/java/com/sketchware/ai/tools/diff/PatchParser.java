package com.sketchware.ai.tools.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Cline's {@code apply_patch} multi-file unified diff format and
 * applies it to a set of file contents.
 *
 * <p>Mirrors Cline's {@code shared/Patch.ts} markers and the
 * {@code core/task/tools/utils/PatchParser.ts} algorithm.
 *
 * <h2>Format</h2>
 * <pre>
 * *** Begin Patch
 * *** Add File: path/to/new.txt
 * +new file content
 * +line 2
 * *** End File
 * *** Update File: path/to/existing.txt
 * @@ context line
 * -removed line
 * +added line
 *  unchanged context
 * *** End File
 * *** Delete File: path/to/old.txt
 * *** End Patch
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Lines beginning with {@code *** } are file-action markers (Add/Update/Delete).</li>
 *   <li>Within an Update File hunk:
 *     <ul>
 *       <li>{@code @@} prefix marks a context anchor (preserved verbatim).</li>
 *       <li>{@code -} prefix marks a line to remove (must match exactly).</li>
 *       <li>{@code +} prefix marks a line to add.</li>
 *       <li>{@code  } (single space) prefix marks an unchanged context line (preserved verbatim, used to locate the change).</li>
 *       <li>An empty line (no prefix) within a hunk is treated as an unchanged blank line.</li>
 *     </ul>
 *   </li>
 *   <li>{@code *** End File} terminates the current file's hunk.</li>
 *   <li>{@code *** End Patch} terminates the patch.</li>
 * </ul>
 *
 * <p>Update hunks are applied with line-trimmed matching as fallback when
 * exact matching fails, mirroring Cline's fuzz behaviour.
 */
public final class PatchParser {

    public static final String BEGIN_PATCH  = "*** Begin Patch";
    public static final String END_PATCH    = "*** End Patch";
    public static final String END_FILE     = "*** End File";
    public static final String ADD_FILE     = "*** Add File: ";
    public static final String UPDATE_FILE  = "*** Update File: ";
    public static final String DELETE_FILE  = "*** Delete File: ";

    private PatchParser() {}

    /** One operation in a patch: add / update / delete a file. */
    public static final class PatchOp {
        public enum Type { ADD, UPDATE, DELETE }
        public final Type type;
        public final String path;
        /** For ADD: the full file content. For UPDATE: list of hunk lines. For DELETE: unused. */
        public final String addContent;
        public final List<HunkLine> updateHunk;
        public PatchOp(Type type, String path, String addContent, List<HunkLine> updateHunk) {
            this.type = type;
            this.path = path;
            this.addContent = addContent;
            this.updateHunk = updateHunk;
        }
    }

    /** A single line within an Update File hunk. */
    public static final class HunkLine {
        public enum Kind { CONTEXT, REMOVE, ADD }
        public final Kind kind;
        public final String text; // the line content WITHOUT the prefix
        public HunkLine(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    /** Thrown when patch parsing or application fails. */
    public static final class PatchException extends Exception {
        public final String path;
        public PatchException(String path, String message) {
            super(message);
            this.path = path;
        }
    }

    /**
     * Parse a patch string into a list of {@link PatchOp}s.
     *
     * @throws PatchException if the patch is malformed.
     */
    public static List<PatchOp> parse(String patch) throws PatchException {
        List<PatchOp> ops = new ArrayList<>();
        if (patch == null || patch.isEmpty()) return ops;
        String[] lines = patch.split("\n", -1);
        int i = 0;
        // Skip leading blank lines.
        while (i < lines.length && lines[i].trim().isEmpty()) i++;
        if (i >= lines.length) return ops;
        if (!lines[i].trim().equals(BEGIN_PATCH)) {
            throw new PatchException(null, "Patch must start with '" + BEGIN_PATCH + "', found: " + lines[i]);
        }
        i++;
        while (i < lines.length) {
            String line = lines[i];
            if (line.trim().equals(END_PATCH)) break;
            if (line.startsWith(ADD_FILE)) {
                String path = line.substring(ADD_FILE.length()).trim();
                StringBuilder content = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().equals(END_FILE) && !lines[i].trim().equals(END_PATCH)) {
                    String s = lines[i];
                    // Add-file content lines must start with '+'.
                    if (s.startsWith("+")) {
                        if (content.length() > 0) content.append('\n');
                        content.append(s.substring(1));
                    } else if (s.isEmpty()) {
                        // blank line in add-file = literal blank line.
                        if (content.length() > 0) content.append('\n');
                    } else {
                        throw new PatchException(path,
                                "Add File hunk line must start with '+' or be empty, found: " + s);
                    }
                    i++;
                }
                if (i >= lines.length || !lines[i].trim().equals(END_FILE)) {
                    throw new PatchException(path, "Add File hunk missing '" + END_FILE + "'");
                }
                i++; // consume End File
                ops.add(new PatchOp(PatchOp.Type.ADD, path, content.toString(), null));
            } else if (line.startsWith(UPDATE_FILE)) {
                String path = line.substring(UPDATE_FILE.length()).trim();
                List<HunkLine> hunk = new ArrayList<>();
                i++;
                while (i < lines.length && !lines[i].trim().equals(END_FILE) && !lines[i].trim().equals(END_PATCH)) {
                    String s = lines[i];
                    if (s.startsWith("@@")) {
                        hunk.add(new HunkLine(HunkLine.Kind.CONTEXT, s.substring(2)));
                    } else if (s.startsWith("-")) {
                        hunk.add(new HunkLine(HunkLine.Kind.REMOVE, s.substring(1)));
                    } else if (s.startsWith("+")) {
                        hunk.add(new HunkLine(HunkLine.Kind.ADD, s.substring(1)));
                    } else if (s.startsWith(" ")) {
                        hunk.add(new HunkLine(HunkLine.Kind.CONTEXT, s.substring(1)));
                    } else if (s.isEmpty()) {
                        // blank line = unchanged blank line.
                        hunk.add(new HunkLine(HunkLine.Kind.CONTEXT, ""));
                    } else {
                        throw new PatchException(path,
                                "Update File hunk line must start with '@@', '-', '+', ' ' or be empty, found: " + s);
                    }
                    i++;
                }
                if (i >= lines.length || !lines[i].trim().equals(END_FILE)) {
                    throw new PatchException(path, "Update File hunk missing '" + END_FILE + "'");
                }
                i++; // consume End File
                ops.add(new PatchOp(PatchOp.Type.UPDATE, path, null, hunk));
            } else if (line.startsWith(DELETE_FILE)) {
                String path = line.substring(DELETE_FILE.length()).trim();
                ops.add(new PatchOp(PatchOp.Type.DELETE, path, null, null));
                i++;
                // Delete File has no End File marker.
            } else if (line.trim().isEmpty()) {
                // Skip blank lines between ops.
                i++;
            } else {
                throw new PatchException(null, "Unknown patch line: " + line);
            }
        }
        return ops;
    }

    /**
     * Apply a single Update-File hunk to a file's current content.
     * Algorithm: walk the hunk line-by-line, finding each REMOVE/CONTEXT
     * block in the source, applying the REMOVE+ADD substitutions in place.
     *
     * <p>Matching strategy:
     * <ol>
     *   <li>Exact match on the context+remove line sequence.</li>
     *   <li>Line-trimmed fallback if exact fails.</li>
     * </ol>
     *
     * @throws PatchException if a REMOVE line can't be located in the source.
     */
    public static String applyUpdateHunk(String source, List<HunkLine> hunk, String path) throws PatchException {
        if (source == null) source = "";
        String[] srcLines = source.split("\n", -1);
        List<String> out = new ArrayList<>();
        int srcIdx = 0;

        // Walk hunk in segments: each segment is a contiguous run of CONTEXT+REMOVE lines,
        // followed by zero or more ADD lines.
        int h = 0;
        while (h < hunk.size()) {
            // Collect the context+remove segment.
            List<HunkLine> segment = new ArrayList<>();
            while (h < hunk.size() && hunk.get(h).kind != HunkLine.Kind.ADD) {
                segment.add(hunk.get(h));
                h++;
            }
            // Collect the ADD segment that follows.
            List<String> adds = new ArrayList<>();
            while (h < hunk.size() && hunk.get(h).kind == HunkLine.Kind.ADD) {
                adds.add(hunk.get(h).text);
                h++;
            }

            // The segment's REMOVE lines must be found (in order) at or after srcIdx.
            // CONTEXT lines must also match (in order) but are preserved verbatim in the output.
            int removeCount = segmentRemoveCount(segment);
            int[] matchResult = findSegmentEnd(srcLines, srcIdx, segment, path);
            int segEnd = matchResult[0];
            int segStart = matchResult[1];
            if (segEnd < 0) {
                // Try line-trimmed fallback.
                matchResult = findSegmentEndTrimmed(srcLines, srcIdx, segment, path);
                segEnd = matchResult[0];
                segStart = matchResult[1];
                if (segEnd < 0) {
                    throw new PatchException(path,
                            "Could not locate context/remove segment in source near source line " + srcIdx + ".");
                }
            }
            // Copy unchanged lines from srcIdx up to segStart.
            for (int k = srcIdx; k < segStart; k++) {
                out.add(srcLines[k]);
            }
            // Walk segment lines: keep CONTEXT, drop REMOVE.
            int cursor = segStart;
            for (HunkLine hl : segment) {
                if (hl.kind == HunkLine.Kind.CONTEXT) {
                    out.add(srcLines[cursor]);
                    cursor++;
                } else { // REMOVE
                    cursor++;
                }
            }
            // Append ADDs.
            out.addAll(adds);
            srcIdx = cursor;
        }
        // Copy any remaining source lines.
        while (srcIdx < srcLines.length) {
            out.add(srcLines[srcIdx++]);
        }
        return String.join("\n", out);
    }

    private static int segmentRemoveCount(List<HunkLine> segment) {
        int c = 0;
        for (HunkLine hl : segment) if (hl.kind == HunkLine.Kind.REMOVE) c++;
        return c;
    }

    /**
     * Find the end index (exclusive) in srcLines such that the segment's
     * CONTEXT and REMOVE lines match exactly.
     *
     * @return {@code [endExclusive, startInclusive]} or {@code [-1, -1]} if no match.
     */
    private static int[] findSegmentEnd(String[] srcLines, int startIdx, List<HunkLine> segment, String path) {
        int totalLen = segment.size();
        for (int s = startIdx; s + totalLen <= srcLines.length; s++) {
            boolean ok = true;
            int cursor = s;
            for (HunkLine hl : segment) {
                if (!srcLines[cursor].equals(hl.text)) { ok = false; break; }
                cursor++;
            }
            if (ok) return new int[]{s + totalLen, s};
        }
        return new int[]{-1, -1};
    }

    /** Line-trimmed fallback for {@link #findSegmentEnd}. */
    private static int[] findSegmentEndTrimmed(String[] srcLines, int startIdx, List<HunkLine> segment, String path) {
        int totalLen = segment.size();
        for (int s = startIdx; s + totalLen <= srcLines.length; s++) {
            boolean ok = true;
            int cursor = s;
            for (HunkLine hl : segment) {
                if (!srcLines[cursor].trim().equals(hl.text.trim())) { ok = false; break; }
                cursor++;
            }
            if (ok) return new int[]{s + totalLen, s};
        }
        return new int[]{-1, -1};
    }

    /**
     * Validate that a patch string is well-formed. Returns null on success,
     * an error message otherwise.
     */
    public static String validate(String patch) {
        try {
            parse(patch);
            return null;
        } catch (PatchException e) {
            return e.getMessage();
        }
    }
}
