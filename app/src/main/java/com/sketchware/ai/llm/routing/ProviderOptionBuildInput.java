package com.sketchware.ai.llm.routing;

import com.google.gson.JsonObject;

/**
 * Input passed to {@link ProviderOptionRule#build(ProviderOptionBuildInput)}.
 *
 * <p>Mirrors Cline's {@code ProviderOptionBuildInput}. It's the match input
 * plus the suppressions resolved from all matched rules plus a few derived
 * "compatible options" buckets.
 */
public final class ProviderOptionBuildInput {

    public final ProviderOptionMatchInput match;
    public final Suppression suppressions;
    public final String providerOptionsKey;
    public final JsonObject compatibleOptions; // pre-built generic options

    public ProviderOptionBuildInput(ProviderOptionMatchInput match,
                                    Suppression suppressions,
                                    String providerOptionsKey,
                                    JsonObject compatibleOptions) {
        this.match = match;
        this.suppressions = suppressions;
        this.providerOptionsKey = providerOptionsKey;
        this.compatibleOptions = compatibleOptions;
    }
}
