package com.sketchware.ai.llm.providers;

import com.google.gson.JsonObject;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;

/**
 * Generic OpenAI-compatible provider (OpenRouter, DeepSeek, GLM, Together, etc.).
 *
 * <p>Wire format is identical to {@link OpenAiProvider}; only the endpoint
 * and request body tweaks differ. Notable variations handled here:
 * <ul>
 *   <li>DeepSeek: sends {@code thinking.type=enabled/disabled} instead of reasoning_effort.</li>
 *   <li>OpenRouter: sends {@code reasoning = {effort, max_tokens}} object.</li>
 *   <li>GLM (Z.AI): sends {@code thinking.type=enabled/disabled}.</li>
 * </ul>
 */
public class OpenAiCompatProvider extends OpenAiProvider {

    private final String providerId;
    private final String defaultBaseUrl;

    public OpenAiCompatProvider(String providerId, String defaultBaseUrl) {
        this.providerId = providerId;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @Override public String getProviderId() { return providerId; }

    /**
     * Z.AI's GLM API (open.bigmodel.cn) and a few other OpenAI-compatible
     * servers use a Pydantic union schema for the {@code tools} field that
     * requires the <b>flat</b> tool format ({@code {type, name, description,
     * parameters}}) and rejects the Chat Completions {@code function} wrapper
     * as an extra field (HTTP 422 extra_forbidden).
     *
     * <p>Detection rules (any match triggers flat format):
     * <ul>
     *   <li>{@code providerId} is {@code "zai"} or {@code "z-ai"}</li>
     *   <li>{@code baseUrl} host contains {@code z.ai}, {@code bigmodel.cn},
     *       or {@code glm}</li>
     *   <li>{@code model.id} contains {@code glm} (case-insensitive)</li>
     * </ul>
     */
    @Override protected boolean useFlatToolFormat(LlmRequest request) {
        if ("zai".equals(providerId) || "z-ai".equals(providerId)) return true;
        if (request != null) {
            String url = request.baseUrl;
            if (url != null) {
                String lower = url.toLowerCase();
                if (lower.contains("z.ai") || lower.contains("bigmodel.cn") || lower.contains("glm")) {
                    return true;
                }
            }
            if (request.model != null && request.model.id != null
                    && request.model.id.toLowerCase().contains("glm")) {
                return true;
            }
        }
        return false;
    }

    @Override public ModelInfo getModel(String modelId) {
        if ("openrouter".equals(providerId)) {
            // OpenRouter models are passed through; we use defaults.
            return ModelInfo.defaultFor(modelId);
        }
        if ("mistral".equals(providerId)
                || (modelId != null && modelId.toLowerCase().contains("mistral"))) {
            // Mistral models: mistral-large-latest, mistral-medium-latest, etc.
            // 128k context, 8k output, supports tools, supports images (for pixtral variants).
            // Pricing as of 2025 (per 1M tokens, USD).
            return new ModelInfo(modelId, "Mistral " + modelId,
                    128_000, 128_000, 8_192,
                    true, true, false,
                    2.00, 6.00, 0.50, 2.00);
        }
        if ("deepseek".equals(providerId) || (modelId != null && modelId.toLowerCase().contains("deepseek"))) {
            return new ModelInfo(modelId, "DeepSeek " + modelId,
                    64_000, 64_000, 8_192,
                    true, false, true,
                    0.14, 0.28, 0.014, 0.14);
        }
        if ("zai".equals(providerId) || "z-ai".equals(providerId)
                || (modelId != null && modelId.toLowerCase().contains("glm"))) {
            return new ModelInfo(modelId, "GLM " + modelId,
                    128_000, 128_000, 8_192,
                    true, true, true,
                    0.07, 0.28, 0.007, 0.07);
        }
        if ("together".equals(providerId)) {
            return new ModelInfo(modelId, "Together " + modelId,
                    128_000, 128_000, 4_096,
                    true, false, false,
                    0.50, 1.50, 0.0, 0.0);
        }
        if ("fireworks".equals(providerId)) {
            return new ModelInfo(modelId, "Fireworks " + modelId,
                    128_000, 128_000, 16_384,
                    true, false, true,
                    0.50, 1.50, 0.0, 0.0);
        }
        return ModelInfo.defaultFor(modelId);
    }

    @Override protected JsonObject buildRequestBody(LlmRequest request) {
        JsonObject body = super.buildRequestBody(request);

        // Provider-specific tweaks
        switch (providerId) {
            case "openrouter": {
                if (request.reasoning != null && request.reasoning.effort != null
                        && request.reasoning.effort != com.sketchware.ai.llm.reasoning.ReasoningEffort.NONE) {
                    JsonObject reasoning = new JsonObject();
                    reasoning.addProperty("effort", request.reasoning.effort.name().toLowerCase());
                    if (request.reasoning.budgetTokens != null) {
                        int cap = (int) (0.6 * request.maxTokens);
                        reasoning.addProperty("max_tokens", Math.min(request.reasoning.budgetTokens, cap));
                    }
                    body.add("reasoning", reasoning);
                }
                // OpenRouter prefers "include_reasoning" rather than reasoning_content field
                if (request.model != null && request.model.supportsReasoning) {
                    body.addProperty("include_reasoning", true);
                }
                break;
            }
            case "deepseek":
            case "zai":
            case "z-ai": {
                if (request.reasoning != null && request.model != null && request.model.supportsReasoning) {
                    JsonObject thinking = new JsonObject();
                    thinking.addProperty("type", Boolean.TRUE.equals(request.reasoning.enabled) ? "enabled" : "disabled");
                    body.add("thinking", thinking);
                }
                break;
            }
        }
        return body;
    }

    @Override public Iterable<com.sketchware.ai.llm.ApiStreamChunk> stream(LlmRequest request) throws Exception {
        if (request.baseUrl == null || request.baseUrl.isEmpty()) {
            request = new LlmRequest(
                    request.providerId, defaultBaseUrl, request.apiKey, request.model,
                    request.systemPrompt, request.messages, request.toolsJson,
                    request.reasoning, request.maxTokens, request.enableStreaming,
                    request.extraHeaders);
        }
        return super.stream(request);
    }
}
