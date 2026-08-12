package com.sketchware.ai.context.snapcompact;

import com.sketchware.ai.agent.AgentMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text preparation pipeline for snapcompact. Two responsibilities, both
 * ported from {@code packages/snapcompact/src/snapcompact.ts}:
 *
 * <ol>
 *   <li><b>normalize</b> — strip ANSI escapes, collapse whitespace, fold
 *       box-drawing/emoji/punctuation to ASCII, replace newline runs with
 *       the solid-block {@link #NEWLINE_GLYPH} glyph so line structure
 *       survives whitespace collapsing at one-cell cost.</li>
 *   <li><b>serializeConversation</b> — convert a list of {@link AgentMessage}
 *       to the compact transcript format with scope markers
 *       ({@code ¶user:}, {@code ¶think:}, {@code ¶ai:}, {@code ¶call:})
 *       and {@code <out>...</out>} wrappers around tool results. Consecutive
 *       same-kind blocks omit repeated prefixes. Long tool results and
 *       tool-call argument lists are head+tail truncated.</li>
 * </ol>
 *
 * <p>Useless tool results (where the upstream library flagged the call as
 * uninformative) are elided entirely along with their paired tool call.
 * Error tool results are kept, since errors often carry the diagnostic
 * information a future turn needs.
 */
public final class SnapCompactText {

    private SnapCompactText() {}

    /** Solid block glyph: printed in place of newline runs so the renderer
     *  sees one ink-filled cell where whitespace collapsing erased the
     *  line break. Code point U+2588. */
    public static final String NEWLINE_GLYPH = "\u2588";

    /** Zero-width ink toggles — currently unused (we only emit bw frames)
     *  but kept for parity with the upstream renderer API. */
    public static final String DIM_ON = "\u000e";
    public static final String DIM_OFF = "\u000f";

    // ---- Defaults (mirrors snapcompact.ts constants) ----

    public static final int TOOL_RESULT_MAX_CHARS = 2000;
    public static final int TOOL_ARG_MAX_CHARS = 500;
    public static final int TOOL_CALL_MAX_CHARS = 2000;
    public static final double TRUNCATE_HEAD_RATIO = 0.6;

    // ---- normalize ----

    /** ANSI escape sequences (CSI, OSC, etc.) — stripped before any per-char
     *  processing because they confuse the whitespace collapser. */
    private static final Pattern ANSI = Pattern.compile("\u001b\\[[0-9;?]*[ -/]*[@-~]");

    /** Whitespace + zero-width format characters that collapse to a single
     *  space (or a NEWLINE_GLYPH if the run contained a line break). */
    private static final Pattern COLLAPSIBLE = Pattern.compile("[\\s\\p{Cf}]+");

    /** Line-break characters within a collapsible run that promote the run
     *  to a NEWLINE_GLYPH. */
    private static final Pattern LINE_BREAK = Pattern.compile("[\\n\\r\\u2028\\u2029]");

    /** Leading/trailing spaces or newline glyphs add no information to a frame. */
    private static final Pattern EDGE_RUNS = Pattern.compile("^[ \u2588]+|[ \u2588]+$");

    /** Glyph-less code points skipped instead of printing '?'. */
    private static final Pattern UNRENDERABLE = Pattern.compile("[\\p{Cc}\\p{Mn}\\p{Me}\\p{Cs}]");

    /** Combining marks left over after NFKD decomposition. */
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /** Punctuation/symbol fold map (subset of snapcompact.ts CHAR_FOLD). */
    private static final Map<String, String> CHAR_FOLD = new HashMap<>();
    static {
        // Quotation marks
        CHAR_FOLD.put("\u2018", "'");  CHAR_FOLD.put("\u2019", "'");
        CHAR_FOLD.put("\u201a", "'");  CHAR_FOLD.put("\u201b", "'");
        CHAR_FOLD.put("\u201c", "\""); CHAR_FOLD.put("\u201d", "\"");
        CHAR_FOLD.put("\u201e", "\"");
        CHAR_FOLD.put("\u2032", "'");  CHAR_FOLD.put("\u2033", "\"");
        CHAR_FOLD.put("\u2035", "'");  CHAR_FOLD.put("\u2036", "\"");
        CHAR_FOLD.put("\u2039", "<");  CHAR_FOLD.put("\u203a", ">");
        // Dashes
        CHAR_FOLD.put("\u2010", "-");  CHAR_FOLD.put("\u2011", "-");
        CHAR_FOLD.put("\u2012", "-");  CHAR_FOLD.put("\u2013", "-");
        CHAR_FOLD.put("\u2014", "-");  CHAR_FOLD.put("\u2015", "-");
        CHAR_FOLD.put("\u2212", "-");  CHAR_FOLD.put("\u2044", "/");
        // Dot leaders / ellipses
        CHAR_FOLD.put("\u2024", ".");  CHAR_FOLD.put("\u2025", "..");
        CHAR_FOLD.put("\u2026", "..."); CHAR_FOLD.put("\u22ef", "...");
        // Bullets
        CHAR_FOLD.put("\u2022", "*");  CHAR_FOLD.put("\u2023", "*");
        CHAR_FOLD.put("\u2043", "-");  CHAR_FOLD.put("\u2219", "*");
        CHAR_FOLD.put("\u25cf", "*");  CHAR_FOLD.put("\u25a0", "*");
        CHAR_FOLD.put("\u25aa", "*");
        // Arrows
        CHAR_FOLD.put("\u2190", "<-"); CHAR_FOLD.put("\u2191", "^");
        CHAR_FOLD.put("\u2192", "->"); CHAR_FOLD.put("\u2193", "v");
        CHAR_FOLD.put("\u2194", "<->"); CHAR_FOLD.put("\u21d0", "<=");
        CHAR_FOLD.put("\u21d2", "=>"); CHAR_FOLD.put("\u21d4", "<=>");
        // Check marks
        CHAR_FOLD.put("\u2713", "v");  CHAR_FOLD.put("\u2714", "v");
        CHAR_FOLD.put("\u2717", "x");  CHAR_FOLD.put("\u2718", "x");
    }

    /** Status-like pictographs that carry meaning in tool output. */
    private static final Map<String, String> EMOJI_FOLD = new HashMap<>();
    static {
        EMOJI_FOLD.put("\u2705", "[OK]");  EMOJI_FOLD.put("\u2611", "[OK]");
        EMOJI_FOLD.put("\u2714", "[OK]");  EMOJI_FOLD.put("\u274c", "[FAIL]");
        EMOJI_FOLD.put("\u274e", "[FAIL]"); EMOJI_FOLD.put("\u2716", "[FAIL]");
        EMOJI_FOLD.put("\u26a0", "[WARN]"); EMOJI_FOLD.put("\ud83d\udea8", "[ALERT]");
        EMOJI_FOLD.put("\u2139", "[INFO]"); EMOJI_FOLD.put("\ud83d\udc1b", "[BUG]");
        EMOJI_FOLD.put("\ud83d\udca5", "[CRASH]"); EMOJI_FOLD.put("\ud83d\udd25", "[HOT]");
        EMOJI_FOLD.put("\ud83d\udd12", "[LOCK]"); EMOJI_FOLD.put("\ud83d\udd13", "[UNLOCK]");
        EMOJI_FOLD.put("\ud83d\udcc1", "[DIR]");  EMOJI_FOLD.put("\ud83d\udcc2", "[DIR]");
        EMOJI_FOLD.put("\ud83d\udcc4", "[FILE]"); EMOJI_FOLD.put("\ud83d\udcdd", "[NOTE]");
        EMOJI_FOLD.put("\ud83e\uddea", "[TEST]"); EMOJI_FOLD.put("\u231b", "[WAIT]");
        EMOJI_FOLD.put("\u23f3", "[WAIT]");      EMOJI_FOLD.put("\ud83d\ude80", "[RUN]");
    }

    /** Box-drawing chars fold to ASCII. */
    private static boolean isBoxDrawing(int cp) {
        return cp >= 0x2500 && cp <= 0x257f;
    }

    /** Extended pictographic emoji ranges (rough — Java \p{Extended_Pictographic}
     *  is not available without UnicodeProperty; we approximate with the
     *  most common emoji blocks). */
    private static boolean isPictograph(int cp) {
        return (cp >= 0x1f300 && cp <= 0x1faff)
            || (cp >= 0x2600 && cp <= 0x27bf)
            || (cp >= 0x2b00 && cp <= 0x2bff);
    }

    /** ASCII or Latin-1 supplement — always renderable. */
    private static boolean isAsciiOrLatin1(int cp) {
        return (cp >= 0x20 && cp < 0x7f) || (cp >= 0xa0 && cp <= 0xff);
    }

    /**
     * Aggressive ASCII fold via Unicode NFKD: decompose compatibility form
     * (fullwidth, super/subscripts, ligatures, circled, math-styled),
     * strip combining marks, route leftover punctuation through CHAR_FOLD.
     * Returns null when no decomposition leaves an ASCII/Latin-1 skeleton.
     */
    private static String foldToAscii(String ch) {
        String decomposed;
        try {
            decomposed = java.text.Normalizer.normalize(ch, java.text.Normalizer.Form.NFKD)
                    .replaceAll("\\p{M}+", "");
        } catch (Throwable t) {
            return null;
        }
        if (decomposed.equals(ch)) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < decomposed.length(); ) {
            int cp = decomposed.codePointAt(i);
            String part = new String(Character.toChars(cp));
            if (isAsciiOrLatin1(cp)) {
                out.append(part);
            } else {
                String fold = CHAR_FOLD.get(part);
                if (fold == null) return null;
                out.append(fold);
            }
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    /**
     * Prepare text for printing: strip ANSI escape sequences, collapse
     * horizontal whitespace runs, fold unsupported symbols (including box
     * drawing to ASCII), and drop decorative emoji instead of printing '?'.
     * Newline runs fold into a single {@link #NEWLINE_GLYPH} so the line
     * structure survives at a one-cell cost.
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) return "";
        // 1. Strip ANSI.
        String stripped = ANSI.matcher(text).replaceAll("");
        // 2. Collapse whitespace runs.
        StringBuilder collapsed = new StringBuilder();
        Matcher m = COLLAPSIBLE.matcher(stripped);
        int lastEnd = 0;
        while (m.find()) {
            collapsed.append(stripped, lastEnd, m.start());
            String run = m.group();
            if (LINE_BREAK.matcher(run).find()) {
                collapsed.append(NEWLINE_GLYPH);
            } else if (Pattern.compile("[^\\p{Cf}]").matcher(run).find()) {
                collapsed.append(' ');
            }
            lastEnd = m.end();
        }
        collapsed.append(stripped, lastEnd, stripped.length());
        String s = collapsed.toString();
        // 3. Edge trim.
        s = EDGE_RUNS.matcher(s).replaceAll("");
        if (s.isEmpty()) return "";

        // 4. Per-codepoint fold: ASCII pass-through, NEWLINE_GLYPH/DIM markers
        // pass-through, emoji fold, box-drawing fold, NFKD fold, else drop if
        // pictograph else '?' if renderable else drop.
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            i += Character.charCount(cp);
            if (isAsciiOrLatin1(cp)) {
                out.append(ch);
                continue;
            }
            if (ch.equals(NEWLINE_GLYPH) || ch.equals(DIM_ON) || ch.equals(DIM_OFF)) {
                out.append(ch);
                continue;
            }
            String emoji = EMOJI_FOLD.get(ch);
            if (emoji != null) {
                out.append(emoji);
                continue;
            }
            String fold = CHAR_FOLD.get(ch);
            if (fold != null) {
                out.append(fold);
                continue;
            }
            if (isBoxDrawing(cp)) {
                out.append(cp == 0x2502 || cp == 0x2503 ? "|"
                        : cp == 0x2500 || cp == 0x2501 ? "-" : "+");
                continue;
            }
            if (UNRENDERABLE.matcher(ch).find()) {
                continue; // skip controls/combining/lone surrogates
            }
            if (isPictograph(cp)) {
                continue; // decorative emoji — drop
            }
            String folded = foldToAscii(ch);
            if (folded != null) {
                out.append(folded);
            } else {
                // Non-ASCII non-foldable. The bundled BDF fonts cover
                // ASCII + Latin-1; outside that we pass the code point
                // through unchanged so the renderer can fall back to the
                // Silver TrueType font (which covers CJK, Hiragana,
                // Katakana, and Latin Extended). If Silver also lacks
                // the glyph, the renderer leaves the cell blank.
                out.append(ch);
            }
        }
        // 5. Collapse double spaces created by multi-char folds (e.g. "<-" + " " = "<- ").
        String result = out.toString().replaceAll(" +", " ");
        return EDGE_RUNS.matcher(result).replaceAll("");
    }

    // ---- serializeConversation ----

    /** Options for {@link #serializeConversation}. */
    public static final class SerializeOptions {
        public final int toolResultMaxChars;
        public final int toolArgMaxChars;
        public final int toolCallMaxChars;
        public final double truncateHeadRatio;
        public final boolean dimToolResults;
        public final boolean includeThinking;

        public SerializeOptions() {
            this(TOOL_RESULT_MAX_CHARS, TOOL_ARG_MAX_CHARS, TOOL_CALL_MAX_CHARS,
                    TRUNCATE_HEAD_RATIO, true, true);
        }

        public SerializeOptions(int toolResultMaxChars, int toolArgMaxChars, int toolCallMaxChars,
                                double truncateHeadRatio, boolean dimToolResults, boolean includeThinking) {
            this.toolResultMaxChars = toolResultMaxChars;
            this.toolArgMaxChars = toolArgMaxChars;
            this.toolCallMaxChars = toolCallMaxChars;
            this.truncateHeadRatio = truncateHeadRatio;
            this.dimToolResults = dimToolResults;
            this.includeThinking = includeThinking;
        }
    }

    /** Head+tail truncation of text longer than maxChars. */
    static String truncateForSummary(String text, int maxChars, double headRatio) {
        if (text.length() <= maxChars) return text;
        double r = Math.min(Math.max(headRatio, 0), 1);
        int headChars = (int) Math.round(maxChars * r);
        int tailChars = maxChars - headChars;
        int elided = text.length() - maxChars;
        String tail = tailChars > 0 ? text.substring(text.length() - tailChars) : "";
        return text.substring(0, headChars) + " […" + elided + "ch elided…] " + tail;
    }

    /** Strip zero-width dim markers (we never emit them but defensive). */
    private static String stripDimMarkers(String text) {
        return text.replaceAll("[\u000e\u000f]", "");
    }

    /**
     * Serialize a list of agent messages to the snapcompact transcript
     * format. See class javadoc.
     */
    public static String serializeConversation(List<AgentMessage> messages, SerializeOptions opts) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        String[] lastPrefix = {null};

        // First pass: collect useless call ids + result text by call id.
        Set<String> uselessCallIds = new HashSet<>();
        java.util.Map<String, String> resultTextByCallId = new HashMap<>();
        for (AgentMessage msg : messages) {
            if (msg.hasToolResults()) {
                for (AgentMessage.ToolResultContent r : msg.toolResults) {
                    boolean useless = isUselessResult(r);
                    if (useless && !r.isError) {
                        uselessCallIds.add(r.toolCallId);
                        continue;
                    }
                    String t = r.output != null ? r.output : "";
                    if (!t.isEmpty()) resultTextByCallId.put(r.toolCallId, t);
                }
            }
        }

        // Helper: push a (prefix, content) pair, merging with the previous
        // pair when the prefix matches.
        java.util.function.BiConsumer<String, String> pushPart = (prefix, content) -> {
            if (content == null || content.isEmpty()) return;
            int last = parts.size() - 1;
            if (last >= 0 && lastPrefix[0] != null && lastPrefix[0].equals(prefix)) {
                String prev = parts.get(last);
                String sep = prev.endsWith("\n") || content.startsWith("\n") ? "" : "\n";
                parts.set(last, prev + sep + content);
            } else {
                parts.add(prefix + content);
                lastPrefix[0] = prefix;
            }
        };

        Set<String> mergedCallIds = new HashSet<>();

        for (AgentMessage msg : messages) {
            if (AgentMessage.ROLE_USER.equals(msg.role)) {
                if (msg.hasToolResults()) {
                    // Standalone tool results — usually already merged into
                    // their originating ¶call: scope below; only orphans
                    // render here.
                    for (AgentMessage.ToolResultContent r : msg.toolResults) {
                        if (uselessCallIds.contains(r.toolCallId)
                                || mergedCallIds.contains(r.toolCallId)) continue;
                        String text = resultTextByCallId.get(r.toolCallId);
                        if (text == null) text = r.output != null ? r.output : "";
                        pushPart.accept("¶call:", "\n" + renderResultBlock(text, opts));
                    }
                } else {
                    String content = msg.text != null ? stripDimMarkers(msg.text) : "";
                    if (!content.isEmpty()) pushPart.accept("¶user:", content);
                }
            } else if (AgentMessage.ROLE_ASSISTANT.equals(msg.role)) {
                // Buffer thinking and text, flush at each tool call boundary.
                java.util.List<String> pendingThinking = new java.util.ArrayList<>();
                java.util.List<String> pendingText = new java.util.ArrayList<>();
                Runnable flush = () -> {
                    if (!pendingThinking.isEmpty()) {
                        pushPart.accept("¶think:", String.join("\n", pendingThinking));
                        pendingThinking.clear();
                    }
                    if (!pendingText.isEmpty()) {
                        pushPart.accept("¶ai:", String.join("\n", pendingText));
                        pendingText.clear();
                    }
                };
                if (opts.includeThinking && msg.reasoning != null && !msg.reasoning.trim().isEmpty()) {
                    pendingThinking.add(stripDimMarkers(msg.reasoning));
                }
                if (msg.text != null && !msg.text.trim().isEmpty()) {
                    pendingText.add(stripDimMarkers(msg.text));
                }
                if (msg.hasToolCalls()) {
                    for (AgentMessage.ToolCall call : msg.toolCalls) {
                        if (uselessCallIds.contains(call.id)) continue;
                        flush.run();
                        String argsStr = truncateForSummary(
                                formatArgs(call.argumentsJson, opts),
                                opts.toolCallMaxChars,
                                opts.truncateHeadRatio);
                        String firstLine = call.name + "(" + argsStr + ")";
                        java.util.List<String> lines = new java.util.ArrayList<>();
                        lines.add(firstLine);
                        String resultText = resultTextByCallId.get(call.id);
                        if (resultText != null) {
                            mergedCallIds.add(call.id);
                            lines.add(renderResultBlock(resultText, opts));
                        }
                        pushPart.accept("¶call:", String.join("\n", lines));
                    }
                }
                flush.run();
            }
            // system role is skipped — system prompt is never archived
        }
        return String.join("\n\n", parts);
    }

    /** Render a tool-result body inside an {@code <out>} block. */
    private static String renderResultBlock(String rawText, SerializeOptions opts) {
        String body = truncateForSummary(stripDimMarkers(rawText),
                opts.toolResultMaxChars, opts.truncateHeadRatio);
        return "<out>\n" + body + "\n</out>";
    }

    /** Format a JSON arguments string as {@code key=value, key=value}. */
    private static String formatArgs(String argumentsJson, SerializeOptions opts) {
        if (argumentsJson == null || argumentsJson.isEmpty()) return "";
        // Parse as JSON object and re-emit as key=value pairs. If parsing
        // fails, fall back to the raw string (truncated).
        try {
            com.google.gson.JsonObject obj =
                    com.google.gson.JsonParser.parseString(argumentsJson).getAsJsonObject();
            java.util.List<String> pairs = new java.util.ArrayList<>();
            for (var entry : obj.entrySet()) {
                String key = entry.getKey();
                String valJson = entry.getValue() != null
                        ? entry.getValue().toString() : "null";
                String val = truncateForSummary(valJson, opts.toolArgMaxChars, opts.truncateHeadRatio);
                pairs.add(key + "=" + val);
            }
            return String.join(", ", pairs);
        } catch (Throwable t) {
            return truncateForSummary(argumentsJson, opts.toolCallMaxChars, opts.truncateHeadRatio);
        }
    }

    /** Heuristic: a tool result is "useless" if it's a success-but-empty
     *  confirmation (e.g. {@code {"ok":true}} with no payload). */
    private static boolean isUselessResult(AgentMessage.ToolResultContent r) {
        if (r.output == null) return false;
        String s = r.output.trim();
        if (s.isEmpty()) return true;
        if (s.length() > 50) return false;
        String lower = s.toLowerCase();
        return lower.equals("ok") || lower.equals("true")
                || lower.equals("done") || lower.equals("success")
                || lower.equals("{\"ok\":true}") || lower.equals("{\"success\":true}");
    }
}
