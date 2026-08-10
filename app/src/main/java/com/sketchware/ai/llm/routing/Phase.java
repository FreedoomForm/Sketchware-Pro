package com.sketchware.ai.llm.routing;

/**
 * Phase at which a {@link ProviderOptionRule} applies. Mirrors the order in
 * Cline's composer (provider, adapter, provider-fanout, provider-reasoning,
 * model-family, model-overlay).
 */
public enum Phase {
    PROVIDER,
    ADAPTER,
    PROVIDER_FANOUT,
    PROVIDER_REASONING,
    MODEL_FAMILY,
    MODEL_OVERLAY
}
