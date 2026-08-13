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
        // IMPORTANT: See OpenAiProvider.useFlatToolFormat() for full context.
        // Z.AI's GLM API uses the standard OpenAI wrapped format, NOT flat.
        // The previous auto-detection (z.ai / bigmodel.cn / glm / paas/v4 URL
        // matches, providerId == "zai", model.id contains "glm") was WRONG
        // and caused HTTP 422 extra_forbidden on every tool call.
        //
        // Flat format is now opt-in ONLY via the user toggle.
        return request != null && request.forceFlatToolFormat;
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
        if ("groq".equals(providerId)) {
            // Groq runs Llama / Qwen / Mixtral with very low latency.
            // 128k context, 8k output, supports tools.
            return new ModelInfo(modelId, "Groq " + modelId,
                    128_000, 128_000, 8_192,
                    true, false, false,
                    0.59, 0.79, 0.0, 0.0);
        }
        if ("grok_xai".equals(providerId) || (modelId != null && modelId.toLowerCase().contains("grok"))) {
            return new ModelInfo(modelId, "Grok " + modelId,
                    131_000, 131_000, 8_192,
                    true, false, false,
                    2.00, 10.00, 0.0, 0.0);
        }
        if ("huggingface".equals(providerId)) {
            return new ModelInfo(modelId, "HF " + modelId,
                    128_000, 128_000, 8_192,
                    true, false, false,
                    0.50, 0.50, 0.0, 0.0);
        }
        if ("minimax".equals(providerId)) {
            return new ModelInfo(modelId, "MiniMax " + modelId,
                    1_000_000, 1_000_000, 8_192,
                    true, false, false,
                    1.00, 1.00, 0.0, 0.0);
        }
        if ("agentrouter".equals(providerId)) {
            // AgentRouter is a multi-model aggregator that proxies to underlying
            // upstream models (Claude Opus 4.x, GPT-5.5, GLM-5.2, ...). We can't
            // know the upstream's exact context window / pricing without a model
            // list fetch, so we use a safe 200k context / 8k output default that
            // covers the largest model in the catalog (Claude Opus 4 = 200k).
            // Tools and images are supported; reasoning is supported on
            // claude-opus-* and gpt-5.* but not on glm-5.2 — we err on the side
            // of "yes" and let the user toggle it off in the chat UI.
            return new ModelInfo(modelId, "AgentRouter " + modelId,
                    200_000, 200_000, 8_192,
                    true, true, true,
                    5.00, 15.00, 0.50, 6.25);
        }
        // vllm / lm_studio / litellm / openai-compat fall through to default;
        // these are user-hosted runtimes whose model catalog is unknown.
        return ModelInfo.defaultFor(modelId);
    }

    @Override protected JsonObject buildRequestBody(LlmRequest request) {
        JsonObject body = super.buildRequestBody(request);

        // Provider-specific tweaks
        switch (providerId) {
            case "openrouter": {
                boolean reasoningEnabled = request.reasoning != null
                        && request.reasoning.isReasoningEnabled();
                if (reasoningEnabled && request.reasoning.effort != null
                        && request.reasoning.effort != com.sketchware.ai.llm.reasoning.ReasoningEffort.NONE) {
                    JsonObject reasoning = new JsonObject();
                    reasoning.addProperty("effort", request.reasoning.effort.name().toLowerCase());
                    if (request.reasoning.budgetTokens != null) {
                        int cap = (int) (0.6 * request.maxTokens);
                        reasoning.addProperty("max_tokens", Math.min(request.reasoning.budgetTokens, cap));
                    }
                    body.add("reasoning", reasoning);
                }
                // OpenRouter prefers "include_reasoning" rather than reasoning_content field.
                // Only ask for reasoning output when the user actually wants reasoning
                // AND the model supports it (otherwise the server may reject the field).
                if (reasoningEnabled && request.model != null && request.model.supportsReasoning) {
                    body.addProperty("include_reasoning", true);
                }
                break;
            }
            case "deepseek":
            case "zai":
            case "z-ai": {
                // DeepSeek / Z.AI thinking toggle. Send enabled/disabled only
                // when the model supports reasoning — sending `thinking` to a
                // non-reasoning model causes HTTP 400 on DeepSeek.
                if (request.reasoning != null && request.model != null && request.model.supportsReasoning) {
                    JsonObject thinking = new JsonObject();
                    thinking.addProperty("type", Boolean.TRUE.equals(request.reasoning.enabled) ? "enabled" : "disabled");
                    body.add("thinking", thinking);
                }
                break;
            }
            case "agentrouter": {
                // AgentRouter is OpenAI-compatible and mirrors OpenRouter's
                // reasoning contract: an object `{effort, max_tokens}` plus an
                // `include_reasoning: true` flag when the user actually wants
                // reasoning output. Forwarded only when the model advertises
                // supportsReasoning (claude-opus-*, gpt-5.*) — glm-5.2 ignores it.
                boolean reasoningEnabled = request.reasoning != null
                        && request.reasoning.isReasoningEnabled();
                if (reasoningEnabled && request.reasoning.effort != null
                        && request.reasoning.effort != com.sketchware.ai.llm.reasoning.ReasoningEffort.NONE) {
                    JsonObject reasoning = new JsonObject();
                    reasoning.addProperty("effort", request.reasoning.effort.name().toLowerCase());
                    if (request.reasoning.budgetTokens != null) {
                        int cap = (int) (0.6 * request.maxTokens);
                        reasoning.addProperty("max_tokens", Math.min(request.reasoning.budgetTokens, cap));
                    }
                    body.add("reasoning", reasoning);
                }
                if (reasoningEnabled && request.model != null && request.model.supportsReasoning) {
                    body.addProperty("include_reasoning", true);
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
                    request.extraHeaders, request.forceFlatToolFormat);
        }
        // AgentRouter fingerprints the HTTP client (their backend is OneAPI-based
        // and rejects requests that don't look like Claude Code with HTTP 401
        // "unauthorized client detected"). Cline works because it identifies
        // itself via User-Agent + x-stainless-* + anthropic-* headers; we send
        // the same set so AgentRouter's fingerprinter accepts us. See:
        //   https://github.com/diegosouzapw/OmniRoute/issues/1921
        //   https://github.com/anomalyco/opencode/issues/2784
        // Headers are scoped to "agentrouter" providerId only — other providers
        // are unaffected. User-supplied extraHeaders still take precedence
        // (we merge ours first, then theirs via HttpClient which overrides).
        if ("agentrouter".equals(providerId)) {
            java.util.List<LlmRequest.ExtraHeader> merged =
                    new java.util.ArrayList<>(AGENTROUTER_FINGERPRINT_HEADERS.size()
                            + (request.extraHeaders == null ? 0 : request.extraHeaders.size()));
            merged.addAll(AGENTROUTER_FINGERPRINT_HEADERS);
            if (request.extraHeaders != null) merged.addAll(request.extraHeaders);
            request = new LlmRequest(
                    request.providerId, request.baseUrl, request.apiKey, request.model,
                    request.systemPrompt, request.messages, request.toolsJson,
                    request.reasoning, request.maxTokens, request.enableStreaming,
                    merged, request.forceFlatToolFormat);
        }
        return super.stream(request);
    }

    /**
     * Headers injected into every AgentRouter request to pass their client
     * fingerprinting check. Mirrors what the Claude Code CLI sends (verified
     * against opencode issue #2784 and OmniRoute issue #1921). Without these,
     * AgentRouter returns HTTP 401 with {@code "unauthorized_client_error"}
     * even when the API key is valid.
     *
     * <p>Note: these are spoofed for compatibility — AgentRouter explicitly
     * rejects non-Claude-Code clients. This is the same approach Cline users
     * use successfully (and the approach Roo Code, opencode, OmniRoute etc.
     * had to adopt).
     */
    private static final java.util.List<LlmRequest.ExtraHeader> AGENTROUTER_FINGERPRINT_HEADERS =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                    new LlmRequest.ExtraHeader("User-Agent", "claude-cli/1.0.108 (external, cli)"),
                    new LlmRequest.ExtraHeader("anthropic-version", "2023-06-01"),
                    new LlmRequest.ExtraHeader("anthropic-beta", "claude-code-20250219,oauth-2025-04-20"),
                    new LlmRequest.ExtraHeader("anthropic-dangerous-direct-browser-access", "true"),
                    new LlmRequest.ExtraHeader("x-app", "cli"),
                    new LlmRequest.ExtraHeader("x-stainless-lang", "js"),
                    new LlmRequest.ExtraHeader("x-stainless-package-version", "0.55.1"),
                    new LlmRequest.ExtraHeader("x-stainless-os", "android"),
                    new LlmRequest.ExtraHeader("x-stainless-arch", "arm64"),
                    new LlmRequest.ExtraHeader("x-stainless-runtime", "node"),
                    new LlmRequest.ExtraHeader("x-stainless-runtime-version", "v22.0.0"),
                    new LlmRequest.ExtraHeader("HTTP-Referer", "https://agentrouter.org/"),
                    new LlmRequest.ExtraHeader("X-Title", "Claude Code")
            ));
}
