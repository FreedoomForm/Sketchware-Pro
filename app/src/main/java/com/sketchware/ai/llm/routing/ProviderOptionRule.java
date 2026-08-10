package com.sketchware.ai.llm.routing;

import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * One rule in the provider option rules engine. Mirrors Cline's
 * {@code ProviderOptionRule} type from {@code provider-option-rules.ts}.
 *
 * <p>Each rule has:
 * <ul>
 *   <li>{@code id} - dotted name like {@code "provider.anthropic.direct"}</li>
 *   <li>{@code phase} - phase at which it applies</li>
 *   <li>{@code description}</li>
 *   <li>{@code applies(input)} - predicate that decides if the rule fires</li>
 *   <li>{@code suppresses} - what generic behaviours the rule disables</li>
 *   <li>{@code build(input)} - constructs the JSON patch to merge into provider options</li>
 * </ul>
 */
public interface ProviderOptionRule {

    String id();
    Phase phase();
    String description();

    /** Predicate - true if this rule applies to the given request. */
    boolean applies(ProviderOptionMatchInput input);

    /** What this rule suppresses when it fires (may be null). */
    default Suppression suppresses() {
        return Suppression.none();
    }

    /** Construct the JSON patch to merge into the provider options object. */
    Optional<JsonObject> build(ProviderOptionBuildInput input);
}
