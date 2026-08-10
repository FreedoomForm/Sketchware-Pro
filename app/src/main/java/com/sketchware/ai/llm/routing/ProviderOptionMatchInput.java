package com.sketchware.ai.llm.routing;

import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.List;

/**
 * Input to {@link ProviderOptionRule#applies(ProviderOptionMatchInput)}.
 *
 * <p>Mirrors Cline's {@code ProviderOptionMatchInput} type.
 */
public final class ProviderOptionMatchInput {

    /** The provider id (e.g. "anthropic", "openai", "openrouter", "google", etc.). */
    public final String providerId;

    /** The adapter target ("openai", "anthropic", "google", "ollama", "gemini", "openai-native", ...). */
    public final String target;

    /** The LLM request (model, system prompt, etc.). */
    public final LlmRequest request;

    /** The model info. */
    public final ModelInfo model;

    /** The reasoning request. */
    public final ReasoningRequest reasoning;

    /** Whether the reasoning route matches (e.g. "glm-thinking", "minimax-thinking"). */
    public final String reasoningRoute;

    /** The model metadata blob (e.g. routing format). */
    public final ModelMetadata modelMetadata;

    public ProviderOptionMatchInput(String providerId,
                                    String target,
                                    LlmRequest request,
                                    ModelInfo model,
                                    ReasoningRequest reasoning,
                                    String reasoningRoute,
                                    ModelMetadata modelMetadata) {
        this.providerId = providerId;
        this.target = target;
        this.request = request;
        this.model = model;
        this.reasoning = reasoning;
        this.reasoningRoute = reasoningRoute;
        this.modelMetadata = modelMetadata;
    }

    /** Convenience: provider id == request.providerId check (always true here). */
    public boolean isClineProvider() {
        // "cline" gateway not used in our port - we treat all providers as direct.
        // Override per-rule if needed.
        return false;
    }

    public boolean isDeepSeekFamily() {
        if (model == null || model.id == null) return false;
        String id = model.id.toLowerCase();
        return id.contains("deepseek") || "deepseek".equals(providerId);
    }

    public boolean isGlmModel() {
        if (model == null || model.id == null) return false;
        return model.id.toLowerCase().contains("glm") || "zai".equals(providerId) || "z-ai".equals(providerId);
    }

    public boolean isKimiK26Family() {
        if (model == null || model.id == null) return false;
        String id = model.id.toLowerCase();
        return id.contains("kimi-k2") || id.contains("k2.6");
    }

    public boolean isMiniMaxM3Model() {
        if (model == null || model.id == null) return false;
        return model.id.toLowerCase().contains("minimax-m3") || model.id.toLowerCase().contains("minimaxm3");
    }

    public boolean isMoonshotKimiModel() {
        if (model == null || model.id == null) return false;
        String id = model.id.toLowerCase();
        return "moonshot".equals(providerId) || id.contains("moonshot") || id.contains("kimi");
    }

    public boolean hasGlmThinkingProviderRouting() {
        return "glm-thinking".equals(reasoningRoute);
    }

    public boolean usesGlmThinkingProviderRouting() {
        return "glm-thinking".equals(reasoningRoute);
    }

    public boolean usesMiniMaxThinkingProviderRouting() {
        return "minimax-thinking".equals(reasoningRoute);
    }

    /** Minimal model metadata holder. */
    public static final class ModelMetadata {
        public final String routingReasoningFormat;
        public final boolean reasoningToggleAdvertised;
        public final boolean reasoningBudgetAdvertised;

        public ModelMetadata(String routingReasoningFormat,
                             boolean reasoningToggleAdvertised,
                             boolean reasoningBudgetAdvertised) {
            this.routingReasoningFormat = routingReasoningFormat;
            this.reasoningToggleAdvertised = reasoningToggleAdvertised;
            this.reasoningBudgetAdvertised = reasoningBudgetAdvertised;
        }

        public static ModelMetadata empty() {
            return new ModelMetadata(null, false, false);
        }
    }
}
