package com.sketchware.ai.llm;

import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.List;

/**
 * One LLM request - the input to {@link LlmProvider#stream(LlmRequest)}.
 * Mirrors Cline's per-call request shape.
 */
public final class LlmRequest {
    public final String providerId;
    public final String baseUrl;        // e.g. "https://api.anthropic.com"
    public final String apiKey;
    public final ModelInfo model;
    public final String systemPrompt;
    public final List<AgentMessage> messages;
    public final String toolsJson;       // JSON array of tool definitions
    public final ReasoningRequest reasoning;
    public final int maxTokens;          // max output tokens
    public final boolean enableStreaming;
    public final List<ExtraHeader> extraHeaders;
    /**
     * User-controlled override that forces the OpenAI Responses API flat
     * tool format ({@code {type, name, description, parameters}}) regardless
     * of the auto-detection heuristics in
     * {@code OpenAiProvider.useFlatToolFormat()}.
     *
     * <p>Set to {@code true} by the user via the "Force flat tool format"
     * toggle in API settings when the auto-detection fails to identify a
     * Z.AI/GLM-like endpoint (e.g. when the user points the generic
     * "openai-compat" provider at a self-hosted GLM proxy whose URL does
     * not contain {@code z.ai} or {@code bigmodel.cn}).
     *
     * <p>Default: {@code false} (use auto-detection).
     */
    public final boolean forceFlatToolFormat;

    public LlmRequest(String providerId,
                      String baseUrl,
                      String apiKey,
                      ModelInfo model,
                      String systemPrompt,
                      List<AgentMessage> messages,
                      String toolsJson,
                      ReasoningRequest reasoning,
                      int maxTokens,
                      boolean enableStreaming,
                      List<ExtraHeader> extraHeaders) {
        this(providerId, baseUrl, apiKey, model, systemPrompt, messages, toolsJson,
                reasoning, maxTokens, enableStreaming, extraHeaders, false);
    }

    public LlmRequest(String providerId,
                      String baseUrl,
                      String apiKey,
                      ModelInfo model,
                      String systemPrompt,
                      List<AgentMessage> messages,
                      String toolsJson,
                      ReasoningRequest reasoning,
                      int maxTokens,
                      boolean enableStreaming,
                      List<ExtraHeader> extraHeaders,
                      boolean forceFlatToolFormat) {
        this.providerId = providerId;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.messages = messages;
        this.toolsJson = toolsJson;
        this.reasoning = reasoning;
        this.maxTokens = maxTokens;
        this.enableStreaming = enableStreaming;
        this.extraHeaders = extraHeaders;
        this.forceFlatToolFormat = forceFlatToolFormat;
    }

    public static final class ExtraHeader {
        public final String name;
        public final String value;
        public ExtraHeader(String name, String value) {
            this.name = name; this.value = value;
        }
    }
}
