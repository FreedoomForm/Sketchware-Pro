package com.sketchware.ai.llm.storage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Instrumentation tests for {@link ProviderConfigStore}.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>A default profile is seeded on first run.</li>
 *   <li>Profiles can be saved, loaded, and deleted.</li>
 *   <li>The active profile is persisted across instances.</li>
 *   <li>Auto-approve settings are persisted.</li>
 * </ul>
 *
 * <p>Must run on a device/emulator because {@link android.content.SharedPreferences}
 * requires an Android Context.
 */
@RunWith(AndroidJUnit4.class)
public class ProviderConfigStoreTest {

    private ProviderConfigStore store;
    private Context context;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Clear all prefs before each test.
        context.getSharedPreferences("sketchware_ai_configs", Context.MODE_PRIVATE)
                .edit().clear().commit();
        store = new ProviderConfigStore(context);
    }

    @After public void tearDown() {
        context.getSharedPreferences("sketchware_ai_configs", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void getProfilesSeedsDefaultOnFirstRun() {
        List<ProviderConfigStore.Profile> profiles = store.getProfiles();
        assertThat(profiles).isNotEmpty();
        assertThat(profiles.get(0).name).isEqualTo("Default");
        assertThat(profiles.get(0).providerId).isEqualTo("openai-compat");
    }

    @Test public void upsertProfileAddsNewProfile() {
        List<ProviderConfigStore.Profile> initial = store.getProfiles();
        int initialCount = initial.size();

        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.name = "Test";
        p.providerId = "anthropic";
        p.apiKey = "sk-test";
        p.modelId = "claude-3-5-sonnet";
        store.upsertProfile(p);

        List<ProviderConfigStore.Profile> after = store.getProfiles();
        assertThat(after).hasSize(initialCount + 1);
        assertThat(after.get(after.size() - 1).name).isEqualTo("Test");
    }

    @Test public void upsertProfileUpdatesExistingProfile() {
        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.name = "Original";
        p.apiKey = "key1";
        store.upsertProfile(p);
        String id = p.id;

        p.name = "Updated";
        p.apiKey = "key2";
        store.upsertProfile(p);

        List<ProviderConfigStore.Profile> after = store.getProfiles();
        ProviderConfigStore.Profile updated = null;
        for (ProviderConfigStore.Profile profile : after) {
            if (profile.id.equals(id)) {
                updated = profile;
                break;
            }
        }
        assertThat(updated).isNotNull();
        assertThat(updated.name).isEqualTo("Updated");
        assertThat(updated.apiKey).isEqualTo("key2");
    }

    @Test public void deleteProfileRemovesIt() {
        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.name = "ToDelete";
        store.upsertProfile(p);
        int initialCount = store.getProfiles().size();

        store.deleteProfile(p.id);

        assertThat(store.getProfiles()).hasSize(initialCount - 1);
    }

    @Test public void deletingLastProfileSeedsNewDefault() {
        List<ProviderConfigStore.Profile> all = store.getProfiles();
        for (ProviderConfigStore.Profile p : all) {
            store.deleteProfile(p.id);
        }
        // After deleting all, the next call should seed a new default.
        assertThat(store.getProfiles()).isNotEmpty();
    }

    @Test public void activeProfilePersistsAcrossInstances() {
        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.name = "Active";
        p.providerId = "openai";
        store.upsertProfile(p);
        store.setActiveProfile(p.id);

        // Recreate the store.
        ProviderConfigStore newStore = new ProviderConfigStore(context);
        ProviderConfigStore.Profile active = newStore.getActiveProfile();
        assertThat(active.id).isEqualTo(p.id);
        assertThat(active.name).isEqualTo("Active");
    }

    @Test public void getActiveProfileReturnsFirstIfNoneSet() {
        ProviderConfigStore.Profile active = store.getActiveProfile();
        List<ProviderConfigStore.Profile> all = store.getProfiles();
        assertThat(active.id).isEqualTo(all.get(0).id);
    }

    @Test public void autoApproveSettingsPersist() {
        store.setAutoApprove("view_add_widget", true);
        store.setAutoApprove("view_delete_widget", false);

        // Recreate store.
        ProviderConfigStore newStore = new ProviderConfigStore(context);
        assertThat(newStore.getAutoApproveSettings().get("view_add_widget")).isTrue();
        assertThat(newStore.getAutoApproveSettings().get("view_delete_widget")).isFalse();
    }

    @Test public void autoApproveDefaultsToEmptyMap() {
        assertThat(store.getAutoApproveSettings()).isEmpty();
    }

    @Test public void profilePreservesAllFields() {
        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.name = "Full";
        p.providerId = "openrouter";
        p.baseUrl = "https://openrouter.ai/api";
        p.apiKey = "or-key";
        p.modelId = "anthropic/claude-3.5-sonnet";
        p.enableReasoning = true;
        p.reasoningEffort = "high";
        p.maxOutputTokens = 8192;
        p.contextWindowSize = 200000;
        p.imageSupport = true;
        p.promptCaching = true;
        p.enableStreaming = true;
        p.useLegacyOpenAiFormat = false;
        p.enableR1Params = false;
        p.useAzure = false;
        p.inputPrice = 3.0;
        p.outputPrice = 15.0;
        p.cacheReadPrice = 0.30;
        p.cacheWritePrice = 3.75;
        p.imageProviderId = "openai";
        p.imageApiKey = "img-key";
        p.imageModel = "dall-e-3";
        p.enableImageGeneration = true;
        p.backgroundEditing = true;
        store.upsertProfile(p);

        ProviderConfigStore loaded = new ProviderConfigStore(context).getActiveProfile();
        store.setActiveProfile(p.id);
        loaded = new ProviderConfigStore(context).getActiveProfile();

        assertThat(loaded.name).isEqualTo("Full");
        assertThat(loaded.providerId).isEqualTo("openrouter");
        assertThat(loaded.baseUrl).isEqualTo("https://openrouter.ai/api");
        assertThat(loaded.apiKey).isEqualTo("or-key");
        assertThat(loaded.modelId).isEqualTo("anthropic/claude-3.5-sonnet");
        assertThat(loaded.enableReasoning).isTrue();
        assertThat(loaded.reasoningEffort).isEqualTo("high");
        assertThat(loaded.maxOutputTokens).isEqualTo(8192);
        assertThat(loaded.contextWindowSize).isEqualTo(200000);
        assertThat(loaded.imageSupport).isTrue();
        assertThat(loaded.promptCaching).isTrue();
        assertThat(loaded.enableStreaming).isTrue();
        assertThat(loaded.inputPrice).isEqualTo(3.0);
        assertThat(loaded.outputPrice).isEqualTo(15.0);
        assertThat(loaded.cacheReadPrice).isEqualTo(0.30);
        assertThat(loaded.cacheWritePrice).isEqualTo(3.75);
        assertThat(loaded.imageProviderId).isEqualTo("openai");
        assertThat(loaded.imageApiKey).isEqualTo("img-key");
        assertThat(loaded.imageModel).isEqualTo("dall-e-3");
        assertThat(loaded.enableImageGeneration).isTrue();
        assertThat(loaded.backgroundEditing).isTrue();
    }
}
