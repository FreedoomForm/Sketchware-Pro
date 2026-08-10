package com.sketchware.ai.llm.routing;

import com.google.gson.JsonObject;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * All 21 provider-option rules ported verbatim from Cline's
 * {@code sdk/packages/llms/src/providers/routing/provider-option-rules.ts}.
 *
 * <p>Order matters and matches the original {@code PROVIDER_OPTION_RULES} array.
 */
public final class ProviderOptionRules {

    /** Singleton list of all 21 rules, in original order. */
    public static final List<ProviderOptionRule> ALL = Collections.unmodifiableList(Arrays.asList(
            rule01_directAnthropicProvider(),
            rule02_directGoogleProvider(),
            rule03_openAiAdapter(),
            rule04_openAiCodex(),
            rule05_genericProviderFanout(),
            rule06_clineGatewayReasoning(),
            rule07_openRouterReasoning(),
            rule08_clineMiniMaxM3GatewayReasoning(),
            rule09_vercelReasoning(),
            rule10_directMoonshotReasoning(),
            rule11_fireworksReasoning(),
            rule12_geminiThinking(),
            rule13_clineReasoningDisabledThinking(),
            rule14_kimiK26Thinking(),
            rule15_deepSeekThinking(),
            rule16_ollamaNativeOptions(),
            rule17_nonGlmProviderRoutingSuppression(),
            rule18_nativeZaiGlmThinking(),
            rule19_miniMaxThinking(),
            rule20_routedGlmReasoning(),
            rule21_togetherReasoningToggle()
    ));

    private ProviderOptionRules() {}

    // ---- Helpers ----

    private static JsonObject obj() { return new JsonObject(); }

    private static JsonObject thinkingPatch(String providerId, String providerOptionsKey, String thinkingType) {
        JsonObject bucket = obj();
        JsonObject thinking = obj();
        thinking.addProperty("type", thinkingType);
        bucket.add("thinking", thinking);
        JsonObject result = obj();
        result.add(providerOptionsKey != null ? providerOptionsKey : providerId, bucket);
        return result;
    }

    private static JsonObject reasoningPatch(String providerId, String providerOptionsKey, JsonObject reasoning) {
        JsonObject bucket = obj();
        bucket.add("reasoning", reasoning);
        JsonObject result = obj();
        result.add(providerOptionsKey != null ? providerOptionsKey : providerId, bucket);
        return result;
    }

    private static JsonObject providerAndAliasPatch(String providerId, String providerOptionsKey, JsonObject bucketOptions) {
        // Cline writes the same bucket under both providerId and camelCase alias.
        JsonObject result = obj();
        result.add(providerId, bucketOptions);
        if (providerOptionsKey != null && !providerOptionsKey.equals(providerId)) {
            result.add(providerOptionsKey, bucketOptions);
        }
        return result;
    }

    // ============================================================
    // Rule 1: provider.anthropic.direct
    // ============================================================
    private static ProviderOptionRule rule01_directAnthropicProvider() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.anthropic.direct"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Direct Anthropic owns the anthropic bucket built by the base patch.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "anthropic".equals(input.providerId);
            }
            @Override public Suppression suppresses() { return Suppression.genericFanout(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                return Optional.empty();
            }
        };
    }

    // ============================================================
    // Rule 2: provider.google.direct
    // ============================================================
    private static ProviderOptionRule rule02_directGoogleProvider() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.google.direct"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Direct Google owns the google bucket used for exact reasoning budgets.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "google".equals(input.providerId);
            }
            @Override public Suppression suppresses() { return Suppression.genericFanout(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                return Optional.empty();
            }
        };
    }

    // ============================================================
    // Rule 3: adapter.openai
    // ============================================================
    private static ProviderOptionRule rule03_openAiAdapter() {
        return new ProviderOptionRule() {
            @Override public String id() { return "adapter.openai"; }
            @Override public Phase phase() { return Phase.ADAPTER; }
            @Override public String description() {
                return "OpenAI adapter targets the AI SDK openai bucket, not provider-id buckets.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "openai".equals(input.target);
            }
            @Override public Suppression suppresses() { return Suppression.none(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject openai = obj();
                openai.addProperty("strictJsonSchema", false);
                // For openai / openai-native, would merge buildOpenAINativeProviderOptions()
                if ("openai".equals(input.match.providerId) || "openai-native".equals(input.match.providerId)) {
                    openai.addProperty("predictedImageUrlsCount", false); // simple stub of native options
                    openai.addProperty("parallelToolCalls", true);
                }
                JsonObject result = obj();
                result.add("openai", openai);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 4: provider.openai-codex
    // ============================================================
    private static ProviderOptionRule rule04_openAiCodex() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.openai-codex"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Codex CLI uses OpenAI Responses options plus provider-id aliases.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "openai-codex".equals(input.providerId);
            }
            @Override public Suppression suppresses() { return Suppression.genericFanout(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject codex = new JsonObject();
                codex.addProperty("store", false);
                codex.addProperty("strictJsonSchema", false);
                codex.addProperty("systemMessageMode", "remove");
                if (input.match.request != null && input.match.request.systemPrompt != null) {
                    codex.addProperty("instructions", input.match.request.systemPrompt);
                }
                JsonObject result = obj();
                result.add("openai", codex);
                // also alias bucket
                JsonObject alias = providerAndAliasPatch(input.match.providerId, input.providerOptionsKey, codex);
                deepMerge(result, alias);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 5: provider.generic-fanout
    // ============================================================
    private static ProviderOptionRule rule05_genericProviderFanout() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.generic-fanout"; }
            @Override public Phase phase() { return Phase.PROVIDER_FANOUT; }
            @Override public String description() {
                return "Default OpenAI-compatible providers receive provider-id and camelCase alias buckets.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return !"openai".equals(input.target);
            }
            @Override public Suppression suppresses() { return Suppression.none(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                if (input.suppressions != null && input.suppressions.genericFanout) {
                    return Optional.empty();
                }
                if (input.compatibleOptions == null) return Optional.empty();
                JsonObject patch = providerAndAliasPatch(input.match.providerId, input.providerOptionsKey, input.compatibleOptions);
                return Optional.of(patch);
            }
        };
    }

    // ============================================================
    // Rule 6: provider.cline.reasoning
    // ============================================================
    private static ProviderOptionRule rule06_clineGatewayReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.cline.reasoning"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Cline gateway accepts the shared gateway reasoning shape.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.isClineProvider();
            }
            @Override public Suppression suppresses() { return Suppression.none(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                // Cline gateway not used in our port - skip build.
                return Optional.empty();
            }
        };
    }

    // ============================================================
    // Rule 7: provider.openrouter.reasoning
    // ============================================================
    private static ProviderOptionRule rule07_openRouterReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.openrouter.reasoning"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "OpenRouter expects reasoning controls under its first-class reasoning object.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "openrouter".equals(input.providerId);
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                if (input.match.reasoning == null) return Optional.empty();
                JsonObject reasoning = new JsonObject();
                ReasoningRequest r = input.match.reasoning;
                if (r.effort != null && r.effort != ReasoningEffort.NONE) {
                    reasoning.addProperty("effort", r.effort.name().toLowerCase());
                }
                if (r.budgetTokens != null) {
                    // OpenRouter caps budget at 0.6 * maxTokens.
                    int max = input.match.request != null ? input.match.request.maxTokens : 0;
                    int cap = max > 0 ? (int) (0.6 * max) : r.budgetTokens;
                    reasoning.addProperty("max_tokens", Math.min(r.budgetTokens, cap));
                }
                if (r.enabled != null) {
                    reasoning.addProperty("enabled", r.enabled);
                }
                if (reasoning.size() == 0) return Optional.empty();
                JsonObject result = reasoningPatch(input.match.providerId, input.providerOptionsKey, reasoning);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 8: provider.cline.minimax-m3.gateway-reasoning
    // ============================================================
    private static ProviderOptionRule rule08_clineMiniMaxM3GatewayReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.cline.minimax-m3.gateway-reasoning"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Cline-routed MiniMax M3 keeps the gateway reasoning shape instead of leaking generic thinking.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.isClineProvider() && input.isMiniMaxM3Model();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                return Optional.empty();
            }
        };
    }

    // ============================================================
    // Rule 9: provider.vercel-ai-gateway.reasoning
    // ============================================================
    private static ProviderOptionRule rule09_vercelReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.vercel-ai-gateway.reasoning"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Vercel maps advertised toggle and budget controls to its gateway reasoning shape.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                if (!"vercel-ai-gateway".equals(input.providerId)) return false;
                if (input.reasoning == null) return false;
                if (input.modelMetadata != null && input.modelMetadata.reasoningToggleAdvertised
                        && input.reasoning.enabled != null) return true;
                if (input.modelMetadata != null && input.modelMetadata.reasoningBudgetAdvertised
                        && input.reasoning.budgetTokens != null) return true;
                return input.isMiniMaxM3Model();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                ReasoningRequest r = input.match.reasoning;
                if (r == null) return Optional.empty();
                JsonObject gatewayReasoning = null;
                if (r.budgetTokens != null) {
                    gatewayReasoning = new JsonObject();
                    gatewayReasoning.addProperty("max_tokens", r.budgetTokens);
                } else if (Boolean.FALSE.equals(r.enabled)) {
                    gatewayReasoning = new JsonObject();
                    gatewayReasoning.addProperty("exclude", true);
                } else if (Boolean.TRUE.equals(r.enabled)) {
                    gatewayReasoning = new JsonObject();
                    gatewayReasoning.addProperty("enabled", true);
                }
                if (gatewayReasoning == null) return Optional.empty();
                JsonObject result = reasoningPatch(input.match.providerId, input.providerOptionsKey, gatewayReasoning);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 10: provider.moonshot.toggle
    // ============================================================
    private static ProviderOptionRule rule10_directMoonshotReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.moonshot.toggle"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Direct Moonshot maps advertised toggle controls to thinking.type.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "moonshot".equals(input.providerId)
                        && input.modelMetadata != null
                        && input.modelMetadata.reasoningToggleAdvertised
                        && input.reasoning != null
                        && input.reasoning.enabled != null;
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                String thinkingType = Boolean.TRUE.equals(input.match.reasoning.enabled) ? "enabled" : "disabled";
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, thinkingType);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 11: provider.fireworks.reasoning-budget
    // ============================================================
    private static ProviderOptionRule rule11_fireworksReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.fireworks.reasoning-budget"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Fireworks uses its native thinking object for exact token budgets.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "fireworks".equals(input.providerId)
                        && input.reasoning != null
                        && input.reasoning.budgetTokens != null;
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject thinking = new JsonObject();
                thinking.addProperty("type", "enabled");
                thinking.addProperty("budget_tokens", input.match.reasoning.budgetTokens);
                JsonObject bucket = new JsonObject();
                bucket.add("thinking", thinking);
                JsonObject result = providerAndAliasPatch(input.match.providerId, input.providerOptionsKey, bucket);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 12: provider.google-gemini.thinking-config
    // ============================================================
    private static ProviderOptionRule rule12_geminiThinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.google-gemini.thinking-config"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Google/Gemini/Vertex uses thinkingConfig only for exact token budgets.";
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                String p = input.providerId;
                return ("google".equals(p) || "gemini".equals(p) || "vertex".equals(p))
                        && input.reasoning != null
                        && input.reasoning.budgetTokens != null;
            }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                Integer budget = input.match.reasoning.budgetTokens;
                if (budget == null) return Optional.empty();
                JsonObject thinkingConfig = new JsonObject();
                thinkingConfig.addProperty("thinkingBudget", budget);
                thinkingConfig.addProperty("includeThoughts", true);
                String providerOptionsName = "vertex".equals(input.match.providerId) ? "vertex" : "google";
                JsonObject bucket = new JsonObject();
                bucket.add("thinkingConfig", thinkingConfig);
                JsonObject result = new JsonObject();
                result.add(providerOptionsName, bucket);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 13: provider.cline.disable-thinking
    // ============================================================
    private static ProviderOptionRule rule13_clineReasoningDisabledThinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.cline.disable-thinking"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Cline-routed non-Kimi-K2.6 Moonshot Kimi models use thinking.type=disabled when reasoning is disabled.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.isClineProvider()
                        && input.isMoonshotKimiModel()
                        && input.reasoning != null
                        && Boolean.FALSE.equals(input.reasoning.enabled)
                        && !input.isKimiK26Family();
            }
            @Override public Suppression suppresses() { return Suppression.none(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, "disabled");
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 14: family.kimi-k2.6.thinking
    // ============================================================
    private static ProviderOptionRule rule14_kimiK26Thinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "family.kimi-k2.6.thinking"; }
            @Override public Phase phase() { return Phase.MODEL_FAMILY; }
            @Override public String description() {
                return "Kimi K2.6 uses thinking.type only for explicit disable.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.isKimiK26Family()
                        && !"openrouter".equals(input.providerId)
                        && input.reasoning != null
                        && Boolean.FALSE.equals(input.reasoning.enabled);
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, "disabled");
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 15: family.deepseek.thinking
    // ============================================================
    private static ProviderOptionRule rule15_deepSeekThinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "family.deepseek.thinking"; }
            @Override public Phase phase() { return Phase.MODEL_FAMILY; }
            @Override public String description() {
                return "DeepSeek models use thinking.type only for explicit reasoning enabled/disabled.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return !"openrouter".equals(input.providerId)
                        && input.isDeepSeekFamily()
                        && !"ollama".equals(input.target);
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                ReasoningRequest r = input.match.reasoning;
                String thinkingType = null;
                if (r != null) {
                    if (Boolean.TRUE.equals(r.enabled)) thinkingType = "enabled";
                    else if (Boolean.FALSE.equals(r.enabled)) thinkingType = "disabled";
                }
                if (thinkingType == null) return Optional.empty();
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, thinkingType);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 16: provider.ollama.native-options
    // ============================================================
    private static ProviderOptionRule rule16_ollamaNativeOptions() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.ollama.native-options"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Ollama receives only its context window through native provider options; reasoning is top-level.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "ollama".equals(input.target);
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                int contextWindow = 8192;
                if (input.match.model != null) {
                    if (input.match.model.contextWindow > 0) contextWindow = input.match.model.contextWindow;
                    else if (input.match.model.maxInputTokens > 0) contextWindow = input.match.model.maxInputTokens;
                }
                JsonObject options = new JsonObject();
                options.addProperty("num_ctx", contextWindow);
                JsonObject bucket = new JsonObject();
                bucket.add("options", options);
                JsonObject result = new JsonObject();
                result.add("ollama", bucket);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 17: provider.routing.glm-thinking.non-glm.suppress-generic-thinking
    // ============================================================
    private static ProviderOptionRule rule17_nonGlmProviderRoutingSuppression() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.routing.glm-thinking.non-glm.suppress-generic-thinking"; }
            @Override public Phase phase() { return Phase.PROVIDER; }
            @Override public String description() {
                return "Providers with GLM thinking routing should not apply generic adaptive thinking to non-GLM models.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.hasGlmThinkingProviderRouting()
                        && input.reasoning != null
                        && input.reasoning.enabled != null
                        && !input.usesGlmThinkingProviderRouting();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                return Optional.empty();
            }
        };
    }

    // ============================================================
    // Rule 18: provider.routing.glm-thinking
    // ============================================================
    private static ProviderOptionRule rule18_nativeZaiGlmThinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.routing.glm-thinking"; }
            @Override public Phase phase() { return Phase.MODEL_OVERLAY; }
            @Override public String description() {
                return "Providers routed to the GLM thinking format use thinking.type.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.usesGlmThinkingProviderRouting();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                ReasoningRequest r = input.match.reasoning;
                String thinkingType = null;
                if (r != null) {
                    if (Boolean.TRUE.equals(r.enabled)) thinkingType = "enabled";
                    else if (Boolean.FALSE.equals(r.enabled)) thinkingType = "disabled";
                }
                if (thinkingType == null) thinkingType = "enabled"; // adaptive default
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, thinkingType);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 19: provider.routing.minimax-thinking
    // ============================================================
    private static ProviderOptionRule rule19_miniMaxThinking() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.routing.minimax-thinking"; }
            @Override public Phase phase() { return Phase.MODEL_OVERLAY; }
            @Override public String description() {
                return "Direct MiniMax M3 uses thinking.type adaptive/disabled.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return input.usesMiniMaxThinkingProviderRouting();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                ReasoningRequest r = input.match.reasoning;
                String thinkingType = "adaptive";
                if (r != null && Boolean.FALSE.equals(r.enabled)) thinkingType = "disabled";
                JsonObject result = thinkingPatch(input.match.providerId, input.providerOptionsKey, thinkingType);
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 20: family.glm.routed-reasoning
    // ============================================================
    private static ProviderOptionRule rule20_routedGlmReasoning() {
        return new ProviderOptionRule() {
            @Override public String id() { return "family.glm.routed-reasoning"; }
            @Override public Phase phase() { return Phase.MODEL_OVERLAY; }
            @Override public String description() {
                return "Routed GLM models use the generic reasoning include/exclude shape, not thinking.type.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return !input.usesGlmThinkingProviderRouting() && input.isGlmModel();
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                ReasoningRequest r = input.match.reasoning;
                if (r == null || r.enabled == null) return Optional.empty();
                JsonObject reasoning = new JsonObject();
                reasoning.addProperty("enabled", r.enabled);
                if (r.budgetTokens != null) {
                    reasoning.addProperty("max_tokens", r.budgetTokens);
                }
                JsonObject result;
                if ("openrouter".equals(input.match.providerId)) {
                    result = new JsonObject();
                    JsonObject bucket = new JsonObject();
                    bucket.add("reasoning", reasoning);
                    result.add(input.match.providerId, bucket);
                } else {
                    result = reasoningPatch(input.match.providerId, input.providerOptionsKey, reasoning);
                }
                return Optional.of(result);
            }
        };
    }

    // ============================================================
    // Rule 21: provider.together.toggle
    // ============================================================
    private static ProviderOptionRule rule21_togetherReasoningToggle() {
        return new ProviderOptionRule() {
            @Override public String id() { return "provider.together.toggle"; }
            @Override public Phase phase() { return Phase.PROVIDER_REASONING; }
            @Override public String description() {
                return "Together maps advertised toggle controls to reasoning.enabled.";
            }
            @Override public boolean applies(ProviderOptionMatchInput input) {
                return "together".equals(input.providerId)
                        && input.modelMetadata != null
                        && input.modelMetadata.reasoningToggleAdvertised
                        && input.reasoning != null
                        && input.reasoning.enabled != null;
            }
            @Override public Suppression suppresses() { return Suppression.genericThinking(); }
            @Override public Optional<JsonObject> build(ProviderOptionBuildInput input) {
                JsonObject reasoning = new JsonObject();
                reasoning.addProperty("enabled", input.match.reasoning.enabled);
                JsonObject result = reasoningPatch(input.match.providerId, input.providerOptionsKey, reasoning);
                return Optional.of(result);
            }
        };
    }

    private static void deepMerge(JsonObject target, JsonObject source) {
        ProviderOptionRulesEngine.deepMerge(target, source);
    }
}
