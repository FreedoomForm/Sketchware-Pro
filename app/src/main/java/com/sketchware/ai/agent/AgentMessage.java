package com.sketchware.ai.agent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One message in the conversation history, mirroring the AI-SDK canonical
 * message shape used by Cline.
 *
 * <p>Roles:
 * <ul>
 *   <li><b>system</b> - the assembled system prompt (only the first message)</li>
 *   <li><b>user</b> - either plain text or a tool-result payload</li>
 *   <li><b>assistant</b> - either plain text or text+tool_calls+reasoning</li>
 * </ul>
 */
public final class AgentMessage {
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    public final String role;
    public final String text;
    public final String reasoning;
    public final List<ToolCall> toolCalls;
    public final List<ToolResultContent> toolResults;
    public final List<String> images;

    private AgentMessage(String role,
                         String text,
                         String reasoning,
                         List<ToolCall> toolCalls,
                         List<ToolResultContent> toolResults,
                         List<String> images) {
        this.role = role;
        this.text = text;
        this.reasoning = reasoning;
        this.toolCalls = toolCalls;
        this.toolResults = toolResults;
        this.images = images;
    }

    public static AgentMessage system(String content) {
        return new AgentMessage(ROLE_SYSTEM, content, null, null, null, null);
    }

    public static AgentMessage user(String content) {
        return new AgentMessage(ROLE_USER, content, null, null, null, null);
    }

    public static AgentMessage userWithImages(String content, List<String> base64Images) {
        return new AgentMessage(ROLE_USER, content, null, null, null, base64Images);
    }

    public static AgentMessage assistant(String text, String reasoning, List<ToolCall> toolCalls) {
        return new AgentMessage(ROLE_ASSISTANT, text, reasoning, toolCalls, null, null);
    }

    public static AgentMessage toolResult(List<ToolResultContent> results) {
        return new AgentMessage(ROLE_USER, null, null, null, results, null);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasToolResults() {
        return toolResults != null && !toolResults.isEmpty();
    }

    public static final class ToolCall {
        public final String id;
        public final String name;
        public final String argumentsJson; // raw JSON string

        public ToolCall(String id, String name, String argumentsJson) {
            this.id = id;
            this.name = name;
            this.argumentsJson = argumentsJson;
        }
    }

    public static final class ToolResultContent {
        public final String toolCallId;
        public final String toolName;
        public final String output;
        public final boolean isError;

        public ToolResultContent(String toolCallId, String toolName, String output, boolean isError) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.output = output;
            this.isError = isError;
        }
    }

    /**
     * Estimate the number of tokens this message consumes.
     * Uses a rough heuristic: 1 token ~= 4 chars.
     *
     * <p>For messages carrying images (e.g. snapcompact compaction frames),
     * a vision-token estimate is added on top of the text estimate. The
     * vision estimate uses OpenAI's tile-based formula: each 512×512 tile
     * costs ~170 tokens, with a base of 85 tokens per image. Without this
     * offset, snapcompact frames (typically ~80 frames × ~3000 tokens each
     * = ~240K tokens) would be invisible to {@code estimateTokens}, the
     * next overflow check would not fire, and the next {@code stream(req)}
     * call would hit a guaranteed context-length API error.
     */
    public int estimateTokens() {
        int chars = 0;
        if (text != null) chars += text.length();
        if (reasoning != null) chars += reasoning.length();
        if (toolCalls != null) for (ToolCall tc : toolCalls) {
            chars += (tc.name == null ? 0 : tc.name.length());
            chars += (tc.argumentsJson == null ? 0 : tc.argumentsJson.length());
            chars += (tc.id == null ? 0 : tc.id.length());
        }
        if (toolResults != null) for (ToolResultContent r : toolResults) {
            chars += (r.output == null ? 0 : r.output.length());
            chars += (r.toolName == null ? 0 : r.toolName.length());
            chars += (r.toolCallId == null ? 0 : r.toolCallId.length());
        }
        int textTokens = Math.max(1, chars / 4);
        // Vision token estimate for attached images. Each base64 data-URL
        // image is decoded to its pixel dimensions; cost is computed using
        // OpenAI's formula: 85 base tokens + 170 per 512×512 tile.
        int imageTokens = 0;
        if (images != null) {
            for (String img : images) {
                int[] dims = decodeImageDimensions(img);
                if (dims != null) {
                    int tilesW = (dims[0] + 511) / 512;
                    int tilesH = (dims[1] + 511) / 512;
                    imageTokens += 85 + 170 * (tilesW * tilesH);
                } else {
                    // Unknown dimensions — assume one tile.
                    imageTokens += 255;
                }
            }
        }
        return textTokens + imageTokens;
    }

    /**
     * Best-effort decode of an image's pixel dimensions from a base64
     * data-URL or raw base64 PNG/JPEG. Returns {@code null} if the format
     * is unrecognized or parsing fails — the caller falls back to a
     * one-tile estimate.
     */
    private static int[] decodeImageDimensions(String dataUrlOrBase64) {
        if (dataUrlOrBase64 == null || dataUrlOrBase64.isEmpty()) return null;
        try {
            String b64;
            // Strip data-URL prefix if present ("data:image/png;base64,AAAA...").
            int comma = dataUrlOrBase64.indexOf(',');
            if (dataUrlOrBase64.startsWith("data:") && comma > 0) {
                b64 = dataUrlOrBase64.substring(comma + 1);
            } else {
                b64 = dataUrlOrBase64;
            }
            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            // Use BitmapFactory just to read dimensions — no full decode.
            android.graphics.BitmapFactory.Options opts =
                    new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                return new int[]{opts.outWidth, opts.outHeight};
            }
        } catch (Throwable t) {
            // Fall through to null return.
        }
        return null;
    }
}
