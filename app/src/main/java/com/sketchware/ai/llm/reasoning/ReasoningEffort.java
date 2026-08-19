package com.sketchware.ai.llm.reasoning;

/**
 * Reasoning effort levels. Mirrors Cline's
 * {@code REASONING_EFFORT_RATIOS} table.
 *
 * <p>The ratio is the fraction of {@code maxTokens} to use as the reasoning
 * budget for providers that accept a token budget (Anthropic, Gemini, etc.).
 */
public enum ReasoningEffort {
    NONE   (0.0),
    MINIMAL(0.1),
    LOW    (0.2),
    MEDIUM (0.5),
    HIGH   (0.8),
    XHIGH  (0.95),
    MAX    (1.0);

    public final double ratio;

    ReasoningEffort(double ratio) {
        this.ratio = ratio;
    }

    public static ReasoningEffort parse(String s) {
        if (s == null) return NONE;
        switch (s.toLowerCase()) {
            case "none":    return NONE;
            case "minimal": return MINIMAL;
            case "low":     return LOW;
            case "medium":  return MEDIUM;
            case "high":    return HIGH;
            case "xhigh":   return XHIGH;
            case "max":     return MAX;
            default:        return NONE;
        }
    }
}
