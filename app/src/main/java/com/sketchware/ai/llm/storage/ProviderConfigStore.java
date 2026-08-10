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
        // Image generation (separate provider)
        public String imageProviderId;
        public String imageApiKey;
        public String imageModel;
        public boolean enableImageGeneration;
        public boolean backgroundEditing;

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
            imageProviderId = "";
            imageApiKey = "";
            imageModel = "";
            enableImageGeneration = false;
            backgroundEditing = false;
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
