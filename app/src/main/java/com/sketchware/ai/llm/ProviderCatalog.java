package com.sketchware.ai.llm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalog of every AI provider supported by Sketchware Pro.
 *
 * <p>This class is the single source of truth for:
 * <ul>
 *   <li>provider id (stable identifier persisted in {@link
 *       com.sketchware.ai.llm.storage.ProviderConfigStore.Profile#providerId})</li>
 *   <li>display name (for the settings list &amp; chat header subtitle)</li>
 *   <li>default base URL &amp; API path (so the user doesn't have to type them)</li>
 *   <li>built-in model list (for the model picker / fetch fallback)</li>
 *   <li>icon resource id (resolved via {@link com.sketchware.ai.ui.settings.ProviderIconResolver})</li>
 *   <li>whether an API key is required (Ollama / vLLM / LM Studio don't need one)</li>
 * </ul>
 *
 * <p>Previously the provider list was hard-coded as a switch in three places
 * ({@code ChatFragment.buildProvider}, {@code ApiConfigurationFragment}'s
 * spinner, and the {@code OpenAiCompatProvider} family detector in the
 * LLM layer). Those switches kept diverging — e.g. OpenRouter's base URL
 * was {@code /api} in one place and {@code /api/v1} in another. This
 * catalog consolidates them so a single edit propagates everywhere.
 *
 * <p>The catalog is intentionally a static, immutable list — it mirrors the
 * 15 built-in providers from the FabioSilva11/Sketchware-IA reference plus
 * the 2 Z.AI / Together / Fireworks providers we already shipped, for a
 * total of 17 entries. User-added custom providers ({@code openai-compat}
 * with a custom base URL) are still supported and are surfaced as an extra
 * pseudo-entry in the settings UI; they don't live in this catalog.
 */
public final class ProviderCatalog {

    /**
     * A single provider entry in the catalog.
     *
     * <p>Fields are final and public because the catalog is immutable; the
     * only mutable state lives in the user's stored profile (API key,
     * base URL override, model list override).
     */
    public static final class Entry {
        public final String id;
        public final String displayName;
        public final String defaultBaseUrl;
        public final String defaultApiPath;
        public final String defaultModel;
        public final boolean requiresApiKey;
        /** "openai" | "anthropic" | "gemini" | "ollama" | "openai-compat" */
        public final String family;
        public final List<String> builtinModels;

        public Entry(String id, String displayName, String defaultBaseUrl,
                     String defaultApiPath, String defaultModel,
                     boolean requiresApiKey, String family,
                     List<String> builtinModels) {
            this.id = id;
            this.displayName = displayName;
            this.defaultBaseUrl = defaultBaseUrl;
            this.defaultApiPath = defaultApiPath;
            this.defaultModel = defaultModel;
            this.requiresApiKey = requiresApiKey;
            this.family = family;
            this.builtinModels = Collections.unmodifiableList(
                    builtinModels == null ? Collections.emptyList() : new ArrayList<>(builtinModels));
        }
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
        // --- The big three ---
        new Entry("openai", "OpenAI",
                "https://api.openai.com/v1", "/chat/completions",
                "gpt-4o", true, "openai",
                Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini",
                        "gpt-4.1-nano", "o3", "o4-mini")),
        new Entry("anthropic", "Anthropic",
                "https://api.anthropic.com", "",
                "claude-sonnet-4-20250514", true, "anthropic",
                Arrays.asList("claude-opus-4-20250514", "claude-sonnet-4-20250514",
                        "claude-3-7-sonnet-latest", "claude-3-5-sonnet-latest",
                        "claude-3-5-haiku-latest", "claude-3-opus-latest")),
        new Entry("gemini", "Gemini",
                "https://generativelanguage.googleapis.com", "",
                "gemini-2.0-flash", true, "gemini",
                Arrays.asList("gemini-2.5-pro-exp-03-25", "gemini-2.5-flash-preview-04-17",
                        "gemini-2.0-flash", "gemini-2.0-flash-lite",
                        "gemini-2.5-pro-preview-05-06", "gemini-1.5-pro", "gemini-1.5-flash")),

        // --- Aggregators / OpenAI-compatible ---
        new Entry("openrouter", "OpenRouter",
                "https://openrouter.ai/api/v1", "/chat/completions",
                "anthropic/claude-3.5-sonnet", true, "openai-compat",
                Arrays.asList("anthropic/claude-opus-4", "anthropic/claude-sonnet-4",
                        "qwen/qwen3-235b-a22b", "anthropic/claude-3.7-sonnet",
                        "anthropic/claude-3.5-sonnet", "deepseek/deepseek-r1",
                        "google/gemini-2.0-flash-exp:free")),
        new Entry("mistral", "Mistral",
                "https://api.mistral.ai/v1", "/chat/completions",
                "mistral-large-latest", true, "openai-compat",
                Arrays.asList("codestral-latest", "devstral-small-latest",
                        "mistral-large-latest", "mistral-medium-latest",
                        "ministral-3b-latest", "ministral-8b-latest")),
        new Entry("deepseek", "DeepSeek",
                "https://api.deepseek.com", "/chat/completions",
                "deepseek-chat", true, "openai-compat",
                Arrays.asList("deepseek-chat", "deepseek-reasoner")),
        new Entry("zai", "Z.AI (GLM)",
                "https://api.z.ai/api/paas/v4", "/chat/completions",
                "glm-4.6", true, "openai-compat",
                Arrays.asList("glm-4.6", "glm-4.5", "glm-4-plus", "glm-4-air",
                        "glm-4-flash", "glm-4-flashx", "glm-z1-flash")),
        new Entry("together", "Together AI",
                "https://api.together.xyz/v1", "/chat/completions",
                "meta-llama/Llama-3.3-70B-Instruct-Turbo", true, "openai-compat",
                Arrays.asList("meta-llama/Llama-3.3-70B-Instruct-Turbo",
                        "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo",
                        "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
                        "Qwen/Qwen2.5-72B-Instruct-Turbo",
                        "deepseek-ai/DeepSeek-R1")),
        new Entry("fireworks", "Fireworks AI",
                "https://api.fireworks.ai/inference/v1", "/chat/completions",
                "accounts/fireworks/models/llama-v3p1-70b-instruct", true, "openai-compat",
                Arrays.asList("accounts/fireworks/models/llama-v3p1-70b-instruct",
                        "accounts/fireworks/models/llama-v3p1-405b-instruct",
                        "accounts/fireworks/models/llama4-scout-instruct-basic",
                        "accounts/fireworks/models/qwen2p5-72b-instruct",
                        "accounts/fireworks/models/deepseek-v3")),
        new Entry("groq", "Groq",
                "https://api.groq.com/openai/v1", "/chat/completions",
                "llama-3.1-8b-instant", true, "openai-compat",
                Arrays.asList("qwen-qwq-32b", "llama-3.3-70b-versatile",
                        "llama-3.1-8b-instant", "gemma2-9b-it", "mixtral-8x7b-32768")),
        new Entry("grok_xai", "Grok (xAI)",
                "https://api.x.ai/v1", "/chat/completions",
                "grok-3", true, "openai-compat",
                Arrays.asList("grok-2", "grok-3", "grok-3-mini",
                        "grok-3-fast", "grok-3-mini-fast")),
        new Entry("huggingface", "Hugging Face",
                "https://router.huggingface.co/v1", "/chat/completions",
                "openai/gpt-oss-120b:fastest", true, "openai-compat",
                Arrays.asList("openai/gpt-oss-120b:fastest", "deepseek-ai/DeepSeek-R1:fastest",
                        "meta-llama/Llama-3.3-70B-Instruct", "Qwen/Qwen2.5-72B-Instruct")),
        new Entry("minimax", "MiniMax",
                "https://api.minimax.io/v1", "/chat/completions",
                "MiniMax-M2", true, "openai-compat",
                Arrays.asList("MiniMax-M2.7", "MiniMax-M2.7-highspeed",
                        "MiniMax-M2.5", "MiniMax-M2.5-highspeed",
                        "MiniMax-M2.1", "MiniMax-M2.1-highspeed", "MiniMax-M2", "M2-her")),

        // --- AgentRouter (multi-model aggregator, OpenAI-compatible endpoint) ---
        // AgentRouter exposes two protocols per its docs:
        //   * Anthropic native (base URL https://agentrouter.org, no /v1) for
        //     Claude Opus models.
        //   * OpenAI-compatible (base URL https://agentrouter.org/v1) for
        //     GPT-5.5, GLM-5.2 and other non-Claude models.
        // Sketchware Pro's chat runtime talks one protocol per profile, so we
        // register AgentRouter here as an OpenAI-compatible entry (the generic
        // path that works for all models). Users who specifically want the
        // Anthropic-native Claude Opus path can pick the "Anthropic" provider
        // and override the base URL to https://agentrouter.org.
        new Entry("agentrouter", "AgentRouter",
                "https://agentrouter.org/v1", "/chat/completions",
                "claude-opus-4-1", true, "openai-compat",
                Arrays.asList("claude-opus-4-1", "claude-opus-4-6", "claude-opus-4-8",
                        "claude-opus-4-7", "gpt-5.5", "glm-5.2")),

        // --- Local runtimes ---
        new Entry("ollama", "Ollama",
                "http://127.0.0.1:11434", "",
                "llama3.2", false, "ollama",
                Arrays.asList("llama3.2", "llama3.1", "qwen2.5-coder:7b",
                        "qwen2.5-coder:3b", "qwq", "deepseek-r1", "mistral",
                        "codellama", "phi4", "gemma2")),
        new Entry("vllm", "vLLM",
                "http://localhost:8000", "/chat/completions",
                "", false, "openai-compat",
                Collections.emptyList()),
        new Entry("lm_studio", "LM Studio",
                "http://localhost:1234", "/chat/completions",
                "", false, "openai-compat",
                Collections.emptyList()),
        new Entry("litellm", "LiteLLM",
                "http://localhost:4000", "/chat/completions",
                "", true, "openai-compat",
                Collections.emptyList()),

        // --- Generic catch-all (kept last) ---
        new Entry("openai-compat", "OpenAI-Compatible",
                "", "/chat/completions",
                "", true, "openai-compat",
                Collections.emptyList())
    ));

    private static final Map<String, Entry> BY_ID;
    static {
        Map<String, Entry> m = new LinkedHashMap<>(ENTRIES.size());
        for (Entry e : ENTRIES) m.put(e.id, e);
        BY_ID = Collections.unmodifiableMap(m);
    }

    private ProviderCatalog() { /* no instances */ }

    /** All catalog entries in display order. */
    public static List<Entry> all() {
        return ENTRIES;
    }

    /** Look up an entry by provider id; returns null if not found. */
    public static Entry get(String providerId) {
        if (providerId == null) return null;
        return BY_ID.get(providerId);
    }

    /**
     * Look up an entry by provider id, falling back to the generic
     * {@code openai-compat} entry if not found. Never returns null.
     */
    public static Entry getOrDefault(String providerId) {
        Entry e = get(providerId);
        return e != null ? e : BY_ID.get("openai-compat");
    }

    /**
     * Display name for a provider id, falling back to the raw id if the
     * provider is unknown (e.g. a user-defined custom provider).
     */
    public static String displayNameFor(String providerId) {
        Entry e = get(providerId);
        if (e != null) return e.displayName;
        if (providerId == null) return "";
        if (providerId.isEmpty()) return "OpenAI-Compatible";
        // Beautify: "openai-compat" -> "OpenAI-Compat"; "custom_x" -> "Custom X"
        String s = providerId.replace('_', ' ').trim();
        if (s.isEmpty()) return "OpenAI-Compatible";
        StringBuilder sb = new StringBuilder(s.length());
        boolean cap = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { sb.append(c); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Default base URL for a provider id (may be empty for {@code openai-compat}).
     * Returns "" if the provider is unknown.
     */
    public static String defaultBaseUrlFor(String providerId) {
        Entry e = get(providerId);
        return e != null ? e.defaultBaseUrl : "";
    }

    /** Default API path for a provider id (e.g. "/chat/completions" or "" for Anthropic/Gemini). */
    public static String defaultApiPathFor(String providerId) {
        Entry e = get(providerId);
        return e != null ? e.defaultApiPath : "/chat/completions";
    }

    /** Default model id for a provider id (may be empty for {@code openai-compat} / {@code vllm} etc.). */
    public static String defaultModelFor(String providerId) {
        Entry e = get(providerId);
        return e != null ? e.defaultModel : "";
    }

    /** True if the provider requires an API key (false for Ollama / vLLM / LM Studio). */
    public static boolean requiresApiKey(String providerId) {
        Entry e = get(providerId);
        return e == null || e.requiresApiKey;
    }

    /** Family classifier used by the LLM layer to pick the right provider class. */
    public static String familyOf(String providerId) {
        Entry e = get(providerId);
        return e != null ? e.family : "openai-compat";
    }

    /** Built-in model list for a provider id; empty if the provider has no catalog. */
    public static List<String> builtinModelsFor(String providerId) {
        Entry e = get(providerId);
        return e != null ? e.builtinModels : Collections.<String>emptyList();
    }

    /** Stable list of provider ids in display order. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>(ENTRIES.size());
        for (Entry e : ENTRIES) out.add(e.id);
        return out;
    }

    /** Stable list of provider display names in display order. */
    public static List<String> displayNames() {
        List<String> out = new ArrayList<>(ENTRIES.size());
        for (Entry e : ENTRIES) out.add(e.displayName);
        return out;
    }

    /**
     * Quick id → display-name lookup, used by chat header subtitle
     * ("GPT-4o (OpenAI)"). Returns the raw id if unknown.
     */
    public static String safeDisplayName(String providerId) {
        String n = displayNameFor(providerId);
        return n == null || n.isEmpty() ? (providerId == null ? "" : providerId) : n;
    }

    /**
     * Normalize user input to a known provider id. Accepts case-insensitive
     * matches and a few common aliases ("z-ai" → "zai", "claude" →
     * "anthropic", "google" → "gemini", "openai-compatible" → "openai-compat").
     */
    public static String normalize(String raw) {
        if (raw == null) return "openai-compat";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return "openai-compat";
        if (BY_ID.containsKey(s)) return s;
        if ("z-ai".equals(s) || "z_ai".equals(s) || "zhipu".equals(s) || "glm".equals(s)) return "zai";
        if ("claude".equals(s)) return "anthropic";
        if ("google".equals(s)) return "gemini";
        if ("openai-compatible".equals(s) || "compat".equals(s)) return "openai-compat";
        if ("xai".equals(s) || "grok".equals(s)) return "grok_xai";
        if ("hf".equals(s)) return "huggingface";
        if ("agent_router".equals(s) || "agent-router".equals(s)) return "agentrouter";
        if ("lmstudio".equals(s) || "lm studio".equals(s)) return "lm_studio";
        return s;
    }
}
