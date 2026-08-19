package com.sketchware.ai.ui.settings;

import pro.sketchware.R;

/**
 * Resolves a provider id (or model id) to a drawable resource id for the
 * provider / model icon.
 *
 * <p>Ported from {@code KelivoModelIconResolver} in
 * FabioSilva11/Sketchware-IA — but uses lightweight vector drawables
 * (24dp single-tone) instead of PNGs, and falls back to the generic
 * {@link R.drawable#ic_ai} sparkle when no specific icon exists.
 *
 * <p>The resolver is purely substring-based so it works for any
 * user-supplied provider id or model name (e.g. "claude-3.5-sonnet" still
 * resolves to the Anthropic icon even though it's a model, not a provider).
 */
public final class ProviderIconResolver {

    private ProviderIconResolver() { /* no instances */ }

    /**
     * Resolve a provider id (and optional display name) to a drawable.
     * Returns {@code R.drawable.ic_ai} if no specific match is found.
     *
     * @param providerId   stable provider id (e.g. "openai", "anthropic")
     * @param providerLabel optional display name (used as fallback if id doesn't match)
     */
    public static int resolveProvider(String providerId, String providerLabel) {
        int res = resolveById(providerId);
        if (res != 0) return res;
        res = resolveByKeyword(providerLabel);
        if (res != 0) return res;
        return R.drawable.ic_ai;
    }

    /**
     * Resolve a model id to a drawable. Uses the model name's substring
     * (e.g. "claude-3.5-sonnet" → Anthropic icon, "gpt-4o" → OpenAI icon).
     * Falls back to {@code R.drawable.ic_ai}.
     */
    public static int resolveModel(String modelId) {
        int res = resolveByKeyword(modelId);
        if (res != 0) return res;
        return R.drawable.ic_ai;
    }

    /**
     * Resolve a provider id to a drawable by exact id match. Returns 0 if
     * the id doesn't match a known provider (caller should fall back to
     * {@link #resolveByKeyword(String)} or {@link R.drawable#ic_ai}).
     */
    private static int resolveById(String providerId) {
        if (providerId == null) return 0;
        switch (providerId) {
            case "openai":          return R.drawable.ic_provider_openai;
            case "anthropic":       return R.drawable.ic_provider_anthropic;
            case "gemini":          return R.drawable.ic_provider_gemini;
            case "ollama":          return R.drawable.ic_provider_ollama;
            case "mistral":         return R.drawable.ic_provider_mistral;
            case "openrouter":      return R.drawable.ic_provider_openrouter;
            case "deepseek":        return R.drawable.ic_provider_deepseek;
            case "zai":             return R.drawable.ic_provider_zai;
            case "together":        return R.drawable.ic_provider_together;
            case "fireworks":       return R.drawable.ic_provider_fireworks;
            case "groq":            return R.drawable.ic_provider_groq;
            case "grok_xai":        return R.drawable.ic_provider_grok;
            case "huggingface":     return R.drawable.ic_provider_huggingface;
            case "minimax":         return R.drawable.ic_provider_minimax;
            case "agentrouter":      return R.drawable.ic_provider_agentrouter;
            case "litellm":         return R.drawable.ic_provider_litellm;
            case "vllm":            return R.drawable.ic_provider_vllm;
            case "lm_studio":       return R.drawable.ic_provider_lm_studio;
            case "openai-compat":   return R.drawable.ic_provider_compat;
            default:                return 0;
        }
    }

    /**
     * Resolve a free-text keyword (provider name, model name, or arbitrary
     * substring) to a drawable. The match is case-insensitive substring;
     * first match wins, evaluated top-down.
     *
     * <p>The order matters: more specific matches (e.g. "deepseek" before
     * "openai") must come first, and providers that share brand terms
     * (e.g. "anthropic" and "claude") must both be checked.
     */
    private static int resolveByKeyword(String keyword) {
        if (keyword == null) return 0;
        String s = keyword.toLowerCase();

        // --- Order matters: more specific first ---
        if (containsAny(s, "openrouter"))            return R.drawable.ic_provider_openrouter;
        if (containsAny(s, "openai", "gpt", "o3", "o4-mini")) return R.drawable.ic_provider_openai;
        if (containsAny(s, "anthropic", "claude"))   return R.drawable.ic_provider_anthropic;
        if (containsAny(s, "gemini", "google"))      return R.drawable.ic_provider_gemini;
        if (containsAny(s, "deepseek"))              return R.drawable.ic_provider_deepseek;
        if (containsAny(s, "qwen", "qwq", "qvq"))    return R.drawable.ic_provider_qwen;
        if (containsAny(s, "grok", "xai"))           return R.drawable.ic_provider_grok;
        if (containsAny(s, "mistral", "codestral", "devstral", "ministral")) return R.drawable.ic_provider_mistral;
        if (containsAny(s, "ollama"))                return R.drawable.ic_provider_ollama;
        if (containsAny(s, "groq"))                  return R.drawable.ic_provider_groq;
        if (containsAny(s, "together"))              return R.drawable.ic_provider_together;
        if (containsAny(s, "fireworks"))             return R.drawable.ic_provider_fireworks;
        if (containsAny(s, "huggingface", "hf.co"))  return R.drawable.ic_provider_huggingface;
        if (containsAny(s, "minimax"))               return R.drawable.ic_provider_minimax;
        if (containsAny(s, "agentrouter", "agent_router", "agent-router", "agent router")) return R.drawable.ic_provider_agentrouter;
        if (containsAny(s, "litellm"))               return R.drawable.ic_provider_litellm;
        if (containsAny(s, "vllm"))                  return R.drawable.ic_provider_vllm;
        if (containsAny(s, "lmstudio", "lm studio")) return R.drawable.ic_provider_lm_studio;
        if (containsAny(s, "zai", "z-ai", "zhipu", "glm")) return R.drawable.ic_provider_zai;
        if (containsAny(s, "llama", "meta-llama"))   return R.drawable.ic_provider_meta;
        if (containsAny(s, "gemma"))                 return R.drawable.ic_provider_gemini;
        if (containsAny(s, "phi"))                   return R.drawable.ic_provider_microsoft;
        if (containsAny(s, "compatible", "compat"))  return R.drawable.ic_provider_compat;
        return 0;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (s.contains(n)) return true;
        }
        return false;
    }
}
