package com.sketchware.ai.llm.routing;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Engine that applies the 21 provider-option rules from Cline's
 * {@code provider-option-rules.ts}.
 *
 * <p>The algorithm:
 * <ol>
 *   <li>Match: find all rules whose {@code applies(input)} returns true.</li>
 *   <li>Resolve suppressions: OR together all matched rules' suppresses fields.</li>
 *   <li>Build: invoke each matched rule's {@code build(input)} with the resolved
 *       suppressions, and deep-merge the resulting patches into one JsonObject.</li>
 * </ol>
 */
public final class ProviderOptionRulesEngine {

    private final List<ProviderOptionRule> rules;

    public ProviderOptionRulesEngine(List<ProviderOptionRule> rules) {
        this.rules = rules;
    }

    /** Default engine with all 21 rules from {@link ProviderOptionRules#ALL}. */
    public static ProviderOptionRulesEngine defaultEngine() {
        return new ProviderOptionRulesEngine(ProviderOptionRules.ALL);
    }

    public JsonObject apply(ProviderOptionMatchInput input) {
        List<ProviderOptionRule> matched = new ArrayList<>();
        for (ProviderOptionRule rule : rules) {
            if (rule.applies(input)) matched.add(rule);
        }
        Suppression suppressions = resolveSuppressions(matched);

        JsonObject compatibleOptions = new JsonObject();
        // Pre-build compatible options if not suppressed by genericFanout.
        // For simplicity, we leave it empty here - each rule builds its own patches.

        ProviderOptionBuildInput buildInput = new ProviderOptionBuildInput(
                input, suppressions,
                /* providerOptionsKey */ providerOptionsKeyFor(input),
                compatibleOptions);

        JsonObject result = new JsonObject();
        for (ProviderOptionRule rule : matched) {
            Optional<JsonObject> patch = rule.build(buildInput);
            if (patch.isPresent()) {
                deepMerge(result, patch.get());
            }
        }
        return result;
    }

    private Suppression resolveSuppressions(List<ProviderOptionRule> matched) {
        boolean genericThinking = false;
        boolean genericFanout = false;
        for (ProviderOptionRule r : matched) {
            Suppression s = r.suppresses();
            if (s != null) {
                if (s.genericThinking) genericThinking = true;
                if (s.genericFanout) genericFanout = true;
            }
        }
        return new Suppression(genericThinking, genericFanout);
    }

    private String providerOptionsKeyFor(ProviderOptionMatchInput input) {
        // Maps providerId to the AI SDK provider options key.
        switch (input.providerId) {
            case "anthropic": return "anthropic";
            case "google":
            case "gemini": return "google";
            case "vertex": return "vertex";
            case "ollama": return "ollama";
            case "openai":
            case "openai-native":
            case "openai-codex": return "openai";
            case "openrouter": return "openrouter";
            default: return input.providerId;
        }
    }

    public static void deepMerge(JsonObject target, JsonObject source) {
        for (java.util.Map.Entry<String, JsonElement> e : source.entrySet()) {
            String k = e.getKey();
            JsonElement v = e.getValue();
            if (target.has(k) && target.get(k).isJsonObject() && v.isJsonObject()) {
                deepMerge(target.getAsJsonObject(k), v.getAsJsonObject());
            } else {
                target.add(k, v);
            }
        }
    }
}
