package com.sketchware.ai.llm;

import com.sketchware.ai.agent.AgentMessage;

import java.util.List;

/**
 * Per-model token estimation. Direct port of Cline's
 * {@code sdk/packages/core/src/token/estimate-tokens.ts} heuristics, with
 * extensions for the model families Sketchware Pro actually proxies.
 *
 * <p>The legacy single-ratio estimator ({@link AgentMessage#estimateTokens()})
 * uses {@code chars / 4} universally. That ratio is calibrated for
 * English Latin text on GPT-3.5-era tokenizers. It is wrong by 30-200%
 * for:
 * <ul>
 *   <li><b>CJK text</b> — Chinese / Japanese / Korean characters tokenize
 *       at roughly 1 token per character on every modern tokenizer
 *       (cl100k_base, o200k_base, Claude, Gemini). The legacy ratio
 *       under-counts CJK by 4x, so a 4 000-character Chinese conversation
 *       is estimated at 1 000 tokens but actually consumes ~4 000. The
 *       compaction trigger at 0.9 * maxInputTokens never fires, the
 *       provider rejects the next request with a context-length error,
 *       and the agent loop deadlocks.</li>
 *   <li><b>Claude models</b> — Claude's tokenizer averages ~3.5 chars/token
 *       on mixed code+prose, not 4. The 12% underestimate compounds across
 *       a long conversation.</li>
 *   <li><b>Code-heavy content</b> — code has many short tokens (parens,
 *       operators, identifiers) and the effective ratio drops to ~3
 *       chars/token. The legacy ratio over-counts code by ~33%, triggering
 *       compaction too early and wasting context.</li>
 *   <li><b>GPT-4o (o200k_base)</b> — tighter than cl100k_base on Latin
 *       text (~4.2 chars/token) but much tighter on CJK (~1.5 chars/char
 *       for common hanzi). The legacy 4-chars/token ratio under-counts
 *       CJK by ~6x on GPT-4o.</li>
 * </ul>
 *
 * <p>This estimator classifies each character into one of four buckets
 * (CJK / Latin-ASCII / whitespace / other-Unicode) and applies a per-bucket
 * ratio chosen per model family. The buckets are computed by Unicode block
 * ranges, not regexes, so the hot path is a single {@code switch} on
 * {@code Character.UnicodeBlock}.
 *
 * <p>The estimator is intentionally a heuristic — it does NOT replicate
 * any tokenizer exactly. The goal is to be within ±10% of the real token
 * count for the dominant content shapes (English prose, CJK prose, code,
 * mixed), so the compaction trigger fires at the right time and the
 * overflow-recovery path is not entered unnecessarily.
 *
 * <p>For messages carrying images, the vision-token estimate from
 * {@link AgentMessage#estimateTokens()} is reused — it already implements
 * OpenAI's tile-based formula (85 base + 170 per 512×512 tile) and is
 * accurate enough across providers.
 */
public final class TokenEstimator {

    private TokenEstimator() {}

    /** Per-family char/token ratios. Indexed by {@link Family#ordinal()}. */
    private static final double[] CHARS_PER_TOKEN_LATIN = {
        4.0,   // OPENAI      (gpt-4o / gpt-4.1 — o200k_base)
        3.5,   // ANTHROPIC   (claude-sonnet-4 / claude-opus-4)
        4.0,   // GEMINI      (gemini-2.5-pro / gemini-2.0-flash)
        4.0,   // DEEPSEEK    (deepseek-chat — cl100k_base derivative)
        3.5,   // CODEX       (codestral / deepseek-coder — code-tuned)
        4.0,   // QWEN        (qwen2.5 — baichuan tokenizer)
        4.0,   // LLAMA       (llama3 — llama tokenizer)
        3.5,   // GROK        (xai — sentencepiece derivative)
        4.0,   // MISTRAL    (mistral — sentencepiece)
        4.0,   // GENERIC
    };

    /** CJK chars-per-token. CJK is ~1 char/token on every modern tokenizer. */
    private static final double CJK_CHARS_PER_TOKEN = 1.0;

    /** Other-Unicode (emoji, accented Latin, etc.) chars-per-token. */
    private static final double OTHER_CHARS_PER_TOKEN = 1.5;

    /** Per-message overhead (role tag, separators). Mirrors Cline's overhead. */
    private static final int MESSAGE_OVERHEAD_TOKENS = 4;

    /** Model family classifier. Mirrors the families in {@link ProviderCatalog}. */
    public enum Family {
        OPENAI,      // gpt-4o, gpt-4.1, o3, o4-mini
        ANTHROPIC,   // claude-*
        GEMINI,      // gemini-*
        DEEPSEEK,    // deepseek-chat, deepseek-reasoner
        CODEX,       // codestral, deepseek-coder, code-tuned variants
        QWEN,        // qwen-*, Qwen/*
        LLAMA,       // llama-*, Llama-/*
        GROK,        // grok-*
        MISTRAL,     // mistral-*, ministral-*
        GENERIC      // fallback
    }

    /** Classify a model id into a token-estimation family. */
    public static Family familyOf(String modelId) {
        if (modelId == null || modelId.isEmpty()) return Family.GENERIC;
        String m = modelId.toLowerCase(java.util.Locale.ROOT);
        // Codex / code-tuned first (more specific than the brand match).
        if (m.contains("codestral") || m.contains("coder")
                || m.contains("codellama") || m.contains("codeqwen")) return Family.CODEX;
        if (m.startsWith("gpt-") || m.startsWith("o3") || m.startsWith("o4")
                || m.contains("gpt-4") || m.contains("gpt-5")
                || m.startsWith("openai/")) return Family.OPENAI;
        if (m.contains("claude") || m.startsWith("anthropic/")) return Family.ANTHROPIC;
        if (m.contains("gemini") || m.startsWith("google/")) return Family.GEMINI;
        if (m.contains("deepseek")) return Family.DEEPSEEK;
        if (m.contains("qwen")) return Family.QWEN;
        if (m.contains("llama") || m.contains("Llama".toLowerCase())) return Family.LLAMA;
        if (m.contains("grok")) return Family.GROK;
        if (m.contains("mistral") || m.contains("ministral")) return Family.MISTRAL;
        return Family.GENERIC;
    }

    /**
     * Estimate the token count of a string for a given model.
     *
     * @param text    the text to estimate. Null-safe.
     * @param modelId the model id (e.g. {@code "gpt-4o"}, {@code "claude-sonnet-4-20250514"}).
     *                Null or empty falls back to the generic family.
     * @return the estimated token count, never negative.
     */
    public static int estimateTokens(String text, String modelId) {
        if (text == null || text.isEmpty()) return 0;
        Family f = familyOf(modelId);
        double latinRatio = CHARS_PER_TOKEN_LATIN[f.ordinal()];

        int cjk = 0;
        int latin = 0;
        int whitespace = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjk++;
            } else if (c <= 0x7F) {
                // ASCII (Latin letters, digits, punctuation, whitespace).
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    whitespace++;
                } else {
                    latin++;
                }
            } else if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.LATIN_EXTENDED_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.LATIN_EXTENDED_B
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) {
                // Accented Latin — treat as Latin (most are 1 token, some 2).
                latin++;
            } else {
                other++;
            }
        }

        // Whitespace runs are merged by every modern tokenizer; estimate
        // ~1 token per 4 whitespace chars.
        double tokens = (latin / latinRatio)
                      + (cjk / CJK_CHARS_PER_TOKEN)
                      + (other / OTHER_CHARS_PER_TOKEN)
                      + (whitespace / 4.0);
        return Math.max(1, (int) Math.ceil(tokens));
    }

    /**
     * Estimate the token count of a single message for a given model.
     * Image tokens are computed via the same tile-based formula as
     * {@link AgentMessage#estimateTokens()} (OpenAI's 85 + 170 per 512×512
     * tile), which is accurate enough across providers.
     */
    public static int estimateTokens(AgentMessage m, String modelId) {
        if (m == null) return 0;
        int sum = 0;
        if (m.text != null) sum += estimateTokens(m.text, modelId);
        if (m.reasoning != null) sum += estimateTokens(m.reasoning, modelId);
        if (m.toolCalls != null) {
            for (AgentMessage.ToolCall tc : m.toolCalls) {
                if (tc.name != null) sum += estimateTokens(tc.name, modelId);
                if (tc.argumentsJson != null) sum += estimateTokens(tc.argumentsJson, modelId);
                if (tc.id != null) sum += estimateTokens(tc.id, modelId);
            }
        }
        if (m.toolResults != null) {
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                if (r.output != null) sum += estimateTokens(r.output, modelId);
                if (r.toolName != null) sum += estimateTokens(r.toolName, modelId);
                if (r.toolCallId != null) sum += estimateTokens(r.toolCallId, modelId);
            }
        }
        // Image tokens — reuse the tile-based formula directly. No
        // double-counting with the text estimate above (images are a
        // separate component).
        if (m.images != null && !m.images.isEmpty()) {
            sum += imageTokensLegacy(m);
        }
        return sum + MESSAGE_OVERHEAD_TOKENS;
    }

    /**
     * Estimate the total token count of a conversation history for a
     * given model. Iterates {@link #estimateTokens(AgentMessage, String)}
     * across all messages.
     */
    public static int estimateTokens(List<AgentMessage> history, String modelId) {
        if (history == null || history.isEmpty()) return 0;
        int sum = 0;
        for (AgentMessage m : history) sum += estimateTokens(m, modelId);
        return sum;
    }

    /** Cheap CJK detection via Unicode block ranges. */
    private static boolean isCjk(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E
            || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F
            || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
            || b == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
            || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || b == Character.UnicodeBlock.HIRAGANA
            || b == Character.UnicodeBlock.KATAKANA
            || b == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
            || b == Character.UnicodeBlock.HANGUL_SYLLABLES
            || b == Character.UnicodeBlock.HANGUL_JAMO
            || b == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            || b == Character.UnicodeBlock.BOPOMOFO
            || b == Character.UnicodeBlock.BOPOMOFO_EXTENDED
            || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * Compute the legacy image-token estimate for a message. Used to
     * subtract the legacy text-only estimate so the per-field estimate
     * above doesn't double-count text tokens.
     */
    private static int imageTokensLegacy(AgentMessage m) {
        if (m.images == null || m.images.isEmpty()) return 0;
        int sum = 0;
        for (String img : m.images) {
            int[] dims = decodeImageDimensions(img);
            if (dims != null) {
                int tilesW = (dims[0] + 511) / 512;
                int tilesH = (dims[1] + 511) / 512;
                sum += 85 + 170 * (tilesW * tilesH);
            } else {
                sum += 255;
            }
        }
        return sum;
    }

    /** Best-effort image dimension decode. Mirrors {@link AgentMessage}. */
    private static int[] decodeImageDimensions(String dataUrlOrBase64) {
        if (dataUrlOrBase64 == null || dataUrlOrBase64.isEmpty()) return null;
        try {
            String b64;
            int comma = dataUrlOrBase64.indexOf(',');
            if (dataUrlOrBase64.startsWith("data:") && comma > 0) {
                b64 = dataUrlOrBase64.substring(comma + 1);
            } else {
                b64 = dataUrlOrBase64;
            }
            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            android.graphics.BitmapFactory.Options opts =
                    new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return new int[]{opts.outWidth, opts.outHeight};
            }
        } catch (Throwable t) {
            // Fall through.
        }
        return null;
    }
}
