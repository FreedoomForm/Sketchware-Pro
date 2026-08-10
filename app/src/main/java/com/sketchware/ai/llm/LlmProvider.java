package com.sketchware.ai.llm;

/**
 * Provider of LLM completions. Mirrors Cline's {@code ApiHandler} interface.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link com.sketchware.ai.llm.providers.AnthropicProvider}</li>
 *   <li>{@link com.sketchware.ai.llm.providers.OpenAiProvider}</li>
 *   <li>{@link com.sketchware.ai.llm.providers.OpenAiCompatProvider}</li>
 *   <li>{@link com.sketchware.ai.llm.providers.GeminiProvider}</li>
 *   <li>{@link com.sketchware.ai.llm.providers.OllamaProvider}</li>
 * </ul>
 */
public interface LlmProvider {

    /** Stable identifier - matches the {@code providerId} used in option rules. */
    String getProviderId();

    /** Look up model info for a given model id (falls back to defaults). */
    ModelInfo getModel(String modelId);

    /**
     * Open a streaming completion. Returns an iterable that yields
     * {@link ApiStreamChunk} instances until {@link ApiStreamChunk.Done}
     * is emitted (or the stream errors / is aborted).
     */
    Iterable<ApiStreamChunk> stream(LlmRequest request) throws Exception;

    /** Cancel any in-flight HTTP call. Safe to call repeatedly. */
    void abort();
}
