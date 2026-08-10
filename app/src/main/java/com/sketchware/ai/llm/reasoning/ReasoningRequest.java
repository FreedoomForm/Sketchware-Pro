package com.sketchware.ai.llm.reasoning;

/**
 * Per-request reasoning configuration. Mirrors Cline's
 * {@code ProviderOptionBuildInput.request.reasoning}.
 */
public final class ReasoningRequest {

    /** Whether reasoning (thinking) is enabled. null = unset. */
    public final Boolean enabled;

    /** Effort level (used by OpenAI Responses, OpenRouter). */
    public final ReasoningEffort effort;

    /** Explicit token budget (used by Anthropic, Gemini, Fireworks). */
    public final Integer budgetTokens;

    public ReasoningRequest(Boolean enabled, ReasoningEffort effort, Integer budgetTokens) {
        this.enabled = enabled;
        this.effort = effort;
        this.budgetTokens = budgetTokens;
    }

    public static ReasoningRequest disabled() {
        return new ReasoningRequest(false, ReasoningEffort.NONE, null);
    }

    public static ReasoningRequest fromEffort(ReasoningEffort effort, int maxTokens) {
        if (effort == ReasoningEffort.NONE) return new ReasoningRequest(false, effort, null);
        boolean enabled = true;
        int budget = Math.max(1024, (int) (effort.ratio * maxTokens));
        return new ReasoningRequest(enabled, effort, budget);
    }

    public boolean isReasoningEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
