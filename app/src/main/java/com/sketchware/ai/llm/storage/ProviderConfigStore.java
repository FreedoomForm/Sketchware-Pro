package com.sketchware.ai.llm.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores AI provider configurations and profiles.
 *
 * <p>Mirrors Cline's {@code ProviderConfigStore} but backed by Android
 * {@link SharedPreferences}. Keys are obfuscated via Base64 so they're not
 * immediately visible in {@code adb shell run-as} dumps (not real encryption -
 * for true encryption use {@link androidx.security.crypto.EncryptedSharedPreferences}).
 *
 * <p>For production use of EncryptedSharedPreferences, add the
 * {@code androidx.security:security-crypto} dependency (already added in
 * build.gradle per the integration plan).
 */
public final class ProviderConfigStore {

    private static final String PREFS_NAME = "sketchware_ai_configs";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_ACTIVE_PROFILE = "active_profile";
    private static final String KEY_AUTO_APPROVE = "auto_approve";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public ProviderConfigStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** A configuration profile (one provider + API key + model + settings). */
    public static final class Profile {
        public String id;
        public String name;
        public String providerId;        // "anthropic", "openai", "openai-compat", "openrouter", "gemini", "ollama"
        public String baseUrl;
        public String apiKey;
        public String modelId;
        public boolean enableReasoning;
        public String reasoningEffort;   // "none"|"minimal"|"low"|"medium"|"high"|"xhigh"|"max"
        public int maxOutputTokens;
        public int contextWindowSize;
        public boolean imageSupport;
        public boolean promptCaching;
        public double inputPrice;
        public double outputPrice;
        public double cacheReadPrice;
        public double cacheWritePrice;
        public boolean enableStreaming;
        public boolean useLegacyOpenAiFormat;
        public boolean enableR1Params;
        public boolean useAzure;
        public String azureApiVersion;
        public List<ExtraHeader> customHeaders;
        /**
         * Force the OpenAI Responses API <b>flat</b> tool format
         * ({@code {type, name, description, parameters}}) instead of the
         * Chat Completions <b>wrapped</b> format
         * ({@code {type, function:{...}, strict:false}}).
         *
         * <p>Auto-detection in {@code OpenAiProvider.useFlatToolFormat()}
         * already enables flat format for Z.AI/GLM endpoints. This toggle
         * lets the user force-enable it for any other endpoint that rejects
         * the wrapped format with HTTP 422 {@code extra_forbidden} — for
         * example, custom OpenAI-compatible servers using Pydantic union
         * schemas similar to Z.AI's.
         *
         * <p>Values:
         * <ul>
         *   <li>{@code false} (default) — auto-detect via URL/model heuristics</li>
         *   <li>{@code true} — always use flat format (skip auto-detection)</li>
         * </ul>
         */
        public boolean forceFlatToolFormat;
        // Image generation (separate provider)
        public String imageProviderId;
        public String imageApiKey;
        public String imageModel;
        public boolean enableImageGeneration;
        public boolean backgroundEditing;
        /**
         * Context-window compaction strategy used when the conversation
         * approaches the model's max input context. Values:
         * <ul>
         *   <li><b>{@code auto}</b> (default) — pick the best strategy for
         *       the model: SnapCompactCompactor for vision-capable models,
         *       OhMyPiCompactor for reasoning-enabled non-vision models,
         *       BasicCompactor for everything else.</li>
         *   <li><b>{@code snapcompact}</b> — render discarded history into
         *       dense PNG frames of pixel-font glyphs that vision LLMs read
         *       back directly as image content blocks. No LLM call during
         *       compaction; fully local and deterministic. Requires a
         *       vision-capable model.</li>
         *   <li><b>{@code context-full}</b> — LLM-summarizer approach: send
         *       the older portion to a summarizer model with a structured
         *       prompt (Goal / Progress / Key Decisions / Next Steps / ...)
         *       and replace it with the structured summary. Costs an extra
         *       API call per compaction.</li>
         *   <li><b>{@code shake}</b> — mechanical strategy, no LLM call.
         *       Replaces heavy tool results older than the 16K-token
         *       protected window with {@code [Output truncated - N tokens]}
         *       placeholders and drops old reasoning blocks. Used for
         *       overflow recovery.</li>
         *   <li><b>{@code agentic-legacy}</b> — legacy LLM-summarizer
         *       strategy with a simpler prompt. Retained for debugging.</li>
         * </ul>
         *
         * <p>Set via the "Context Compaction" dropdown in Advanced settings.
         */
        public String compactionStrategy;

        public Profile() {
            id = java.util.UUID.randomUUID().toString();
            name = "Default";
            providerId = "openai-compat";
            baseUrl = "";
            apiKey = "";
            modelId = "";
            enableReasoning = false;
            reasoningEffort = "medium";
            maxOutputTokens = 4096;
            contextWindowSize = 0; // 0 = use model default
            imageSupport = false;
            promptCaching = false;
            inputPrice = 0;
            outputPrice = 0;
            cacheReadPrice = 0;
            cacheWritePrice = 0;
            enableStreaming = true;
            useLegacyOpenAiFormat = false;
            enableR1Params = false;
            useAzure = false;
            azureApiVersion = "";
            customHeaders = new ArrayList<>();
            forceFlatToolFormat = false;
            imageProviderId = "";
            imageApiKey = "";
            imageModel = "";
            enableImageGeneration = false;
            backgroundEditing = false;
            compactionStrategy = "auto";
        }
    }

    public static final class ExtraHeader {
        public String name;
        public String value;
        public ExtraHeader(String name, String value) { this.name = name; this.value = value; }
    }

    /** Get all stored profiles. */
    public List<Profile> getProfiles() {
        String json = prefs.getString(KEY_PROFILES, "[]");
        Type type = new TypeToken<List<Profile>>(){}.getType();
        List<Profile> profiles = gson.fromJson(json, type);
        if (profiles == null) profiles = new ArrayList<>();
        if (profiles.isEmpty()) {
            // Seed with one default profile.
            profiles.add(new Profile());
            saveProfiles(profiles);
        }
        return profiles;
    }

    public void saveProfiles(List<Profile> profiles) {
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply();
    }

    public Profile getActiveProfile() {
        String activeId = prefs.getString(KEY_ACTIVE_PROFILE, null);
        if (activeId == null) {
            List<Profile> all = getProfiles();
            return all.isEmpty() ? new Profile() : all.get(0);
        }
        for (Profile p : getProfiles()) {
            if (activeId.equals(p.id)) return p;
        }
        return getProfiles().get(0);
    }

    public void setActiveProfile(String profileId) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply();
    }

    public void upsertProfile(Profile profile) {
        List<Profile> all = getProfiles();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(profile.id)) {
                all.set(i, profile);
                found = true;
                break;
            }
        }
        if (!found) all.add(profile);
        saveProfiles(all);
    }

    public void deleteProfile(String profileId) {
        List<Profile> all = getProfiles();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).id.equals(profileId)) {
                all.remove(i);
                break;
            }
        }
        if (all.isEmpty()) all.add(new Profile());
        saveProfiles(all);
        // Reset active if it was the deleted one.
        if (profileId.equals(prefs.getString(KEY_ACTIVE_PROFILE, null))) {
            setActiveProfile(all.get(0).id);
        }
    }

    /** Auto-approve settings per tool name. */
    public Map<String, Boolean> getAutoApproveSettings() {
        String json = prefs.getString(KEY_AUTO_APPROVE, "{}");
        Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
        Map<String, Boolean> map = gson.fromJson(json, type);
        return map == null ? new HashMap<>() : map;
    }

    public void setAutoApprove(String toolName, boolean autoApprove) {
        Map<String, Boolean> map = getAutoApproveSettings();
        map.put(toolName, autoApprove);
        prefs.edit().putString(KEY_AUTO_APPROVE, gson.toJson(map)).apply();
    }
}
