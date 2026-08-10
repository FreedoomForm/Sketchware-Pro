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
    }

    public static final class ExtraHeader {
        public final String name;
        public final String value;
        public ExtraHeader(String name, String value) {
            this.name = name; this.value = value;
        }
    }
}
