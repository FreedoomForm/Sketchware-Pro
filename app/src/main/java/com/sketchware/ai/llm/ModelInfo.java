package com.sketchware.ai.llm;

/**
 * Information about a specific model.
 */
public final class ModelInfo {
    public final String id;
    public final String name;
    public final int contextWindow;     // total tokens the model accepts as input
    public final int maxInputTokens;     // practical input limit (often < contextWindow)
    public final int maxOutputTokens;
    public final boolean supportsTools;
    public final boolean supportsImages;
    public final boolean supportsReasoning;
    public final double inputPricePer1M;  // USD
    public final double outputPricePer1M;
    public final double cacheReadPricePer1M;
    public final double cacheWritePricePer1M;

    public ModelInfo(String id,
                     String name,
                     int contextWindow,
                     int maxInputTokens,
                     int maxOutputTokens,
                     boolean supportsTools,
                     boolean supportsImages,
                     boolean supportsReasoning,
                     double inputPricePer1M,
                     double outputPricePer1M,
                     double cacheReadPricePer1M,
                     double cacheWritePricePer1M) {
        this.id = id;
        this.name = name;
        this.contextWindow = contextWindow;
        this.maxInputTokens = maxInputTokens;
        this.maxOutputTokens = maxOutputTokens;
        this.supportsTools = supportsTools;
        this.supportsImages = supportsImages;
        this.supportsReasoning = supportsReasoning;
        this.inputPricePer1M = inputPricePer1M;
        this.outputPricePer1M = outputPricePer1M;
        this.cacheReadPricePer1M = cacheReadPricePer1M;
        this.cacheWritePricePer1M = cacheWritePricePer1M;
    }

    public static ModelInfo defaultFor(String modelId) {
        // Reasonable defaults for a generic OpenAI-compatible model.
        return new ModelInfo(
                modelId, modelId,
                128_000, 128_000, 4_096,
                true, true, false,
                0.0, 0.0, 0.0, 0.0);
    }
}
