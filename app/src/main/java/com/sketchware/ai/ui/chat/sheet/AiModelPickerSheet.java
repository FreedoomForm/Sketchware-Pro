package com.sketchware.ai.ui.chat.sheet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pro.sketchware.R;
import com.sketchware.ai.llm.ProviderCatalog;
import com.sketchware.ai.llm.storage.ProviderConfigStore;
import com.sketchware.ai.ui.chat.adapter.AiModelSheetAdapter;
import com.sketchware.ai.ui.settings.ProviderIconResolver;

/**
 * Full-featured model-picker bottom sheet, ported from
 * {@code KelivoModelBottomSheet} in FabioSilva11/Sketchware-IA.
 *
 * <p>Features:
 * <ul>
 *   <li><b>Cross-provider</b> — lists models from EVERY configured provider
 *       (built-in catalog + any user-saved custom models), not just the
 *       active one. Lets the user switch provider + model in one tap.</li>
 *   <li><b>Favorites</b> — pin any (provider, model) pair with the heart
 *       icon. Pinned models appear at the top under a "Favorites" section.
 *       The bookmark icon in the search bar jumps to that section.</li>
 *   <li><b>Search</b> — substring filter across provider id, provider
 *       display name, and model id. Favorites that don't match the query
 *       are hidden; the "Favorites" header is hidden when empty.</li>
 *   <li><b>Provider chips</b> — quick-jump strip at the bottom. Tapping
 *       a chip scrolls the list to that provider's section header.</li>
 *   <li><b>Capability tags</b> — every model row shows a "Chat" pill, an
 *       "IO" pill (Text → [Image?] → Text), and a tools/agent pill. The
 *       Image indicator is shown only for vision-capable models.</li>
 *   <li><b>Selected highlight</b> — the currently-active (provider, model)
 *       pair gets a soft accent background.</li>
 * </ul>
 *
 * <p>Favorites are persisted in the {@code chat_settings} prefs file
 * under {@link #PREF_PINNED} (and the legacy {@link #PREF_PINNED_LEGACY}
 * key from the reference repo is merged in for compatibility — users
 * migrating from FabioSilva11/Sketchware-IA keep their pins).
 */
public final class AiModelPickerSheet {

    /** Host callback when the user picks a (provider, model) pair. */
    public interface Callback {
        void onModelSelected(String providerId, String modelId);
    }

    /** prefs key for the pinned set (current schema). */
    private static final String PREF_PINNED = "pinned_models_v1";
    /** prefs key for the pinned set (legacy reference-repo schema). */
    private static final String PREF_PINNED_LEGACY = "kelivo_pinned_models";
    /** prefs file for chat-related settings (same as reference repo). */
    private static final String PREFS_CHAT = "chat_settings";
    /** prefs file for user-added custom models per provider. */
    private static final String PREFS_CUSTOM_MODELS = "ai_custom_models";

    private AiModelPickerSheet() { /* no instances */ }

    /**
     * Build and show the model-picker bottom sheet.
     *
     * @param activity the hosting activity (must be a FragmentActivity for
     *                 BottomSheetDialog; in practice the chat host)
     * @param callback invoked when the user taps a model row. The sheet
     *                 persists the new (provider, model) selection to the
     *                 active profile BEFORE calling back, so the host
     *                 just needs to refresh its UI / rebuild its agent.
     */
    public static void show(@NonNull Activity activity, @NonNull Callback callback) {
        Context ctx = activity.getApplicationContext();
        ProviderConfigStore store = new ProviderConfigStore(ctx);

        // Build the list of configured provider groups.
        List<ProviderGroup> groups = buildConfiguredGroups(ctx, store);
        if (groups.isEmpty()) {
            Toast.makeText(activity, R.string.ai_model_sheet_no_models,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Currently active (provider, model).
        ProviderConfigStore.Profile active = store.getActiveProfile();
        String currentProvider = active != null && active.providerId != null
                ? active.providerId : "";
        String currentModel = active != null && active.modelId != null
                ? active.modelId : "";

        // Inflate the sheet.
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.ai_model_sheet, null);
        dialog.setContentView(content);

        EditText search = content.findViewById(R.id.model_search);
        ImageView favoritesJump = content.findViewById(R.id.model_favorites_jump);
        RecyclerView list = content.findViewById(R.id.model_list);
        LinearLayout chips = content.findViewById(R.id.provider_chips);

        AiModelSheetAdapter adapter = new AiModelSheetAdapter();
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        // Refresh callback: rebuild rows from current query + source groups.
        final List<ProviderGroup> sourceGroups = groups;
        Runnable refresh = () -> {
            String q = search.getText() == null
                    ? "" : search.getText().toString().trim().toLowerCase(Locale.getDefault());
            adapter.submit(buildRows(activity, sourceGroups, q,
                    currentProvider, currentModel));
            if (favoritesJump != null) {
                favoritesJump.setVisibility(getPinned(ctx).isEmpty()
                        ? View.GONE : View.VISIBLE);
            }
        };

        adapter.setListener(new AiModelSheetAdapter.Listener() {
            @Override
            public void onModelSelected(String providerId, String modelId) {
                // Persist the new selection to the active profile for this
                // provider (creating one if necessary) before notifying host.
                applySelection(ctx, store, providerId, modelId);
                callback.onModelSelected(providerId, modelId);
                dialog.dismiss();
            }

            @Override
            public void onFavoriteToggle(String providerId, String modelId) {
                togglePinned(ctx, providerId, modelId);
                refresh.run();
            }
        });

        // Live search filter.
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                refresh.run();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Favorites quick-jump button.
        if (favoritesJump != null) {
            favoritesJump.setOnClickListener(v -> {
                if (search.getText() != null && search.getText().length() > 0) {
                    search.setText("");
                }
                list.post(() -> {
                    int pos = adapter.findProviderSectionPosition(
                            AiModelSheetAdapter.PROVIDER_FAVORITES);
                    if (pos >= 0) {
                        list.smoothScrollToPosition(pos);
                    }
                });
            });
        }

        // Provider chip strip — quick-jump to a section.
        buildProviderChips(activity, chips, sourceGroups, currentProvider,
                providerId -> {
                    int pos = adapter.findProviderSectionPosition(providerId);
                    if (pos >= 0) {
                        list.scrollToPosition(pos);
                    }
                });

        // Initial render.
        refresh.run();

        // Expand to 82% of screen height.
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                int targetHeight = (int) (metrics.heightPixels * 0.82f);
                sheet.getLayoutParams().height = targetHeight;
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setPeekHeight(targetHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        dialog.show();
    }

    // ------------------------------------------------------------------
    // Source-data construction
    // ------------------------------------------------------------------

    /** Tuple of (provider id, display name, list of model ids). */
    private static final class ProviderGroup {
        final String providerId;
        final String label;
        final List<String> models;

        ProviderGroup(String providerId, String label, List<String> models) {
            this.providerId = providerId;
            this.label = label;
            this.models = models;
        }
    }

    /**
     * Iterate every provider in the catalog. For each one, decide if it's
     * "configured" (has an API key when required, OR is a no-key provider
     * like Ollama / vLLM / LM Studio). For configured providers, gather
     * the built-in catalog models + any user-saved custom models from
     * the {@code ai_custom_models} prefs file.
     */
    private static List<ProviderGroup> buildConfiguredGroups(
            Context ctx, ProviderConfigStore store) {
        SharedPreferences customPrefs = ctx.getSharedPreferences(
                PREFS_CUSTOM_MODELS, Context.MODE_PRIVATE);
        List<ProviderConfigStore.Profile> profiles = safeProfiles(store);

        List<ProviderGroup> out = new ArrayList<>();
        for (ProviderCatalog.Entry entry : ProviderCatalog.all()) {
            // Skip the generic catch-all — it has no models of its own.
            if ("openai-compat".equals(entry.id)) continue;

            ProviderConfigStore.Profile p = findProfile(profiles, entry.id);
            boolean configured = isConfigured(entry, p);
            if (!configured) continue;

            // Build the model list: built-ins first, then customs.
            List<String> models = new ArrayList<>(entry.builtinModels);
            String rawCustom = customPrefs.getString("models_" + entry.id, "");
            if (rawCustom != null && !rawCustom.isEmpty()) {
                for (String s : rawCustom.split("\n")) {
                    String t = s.trim();
                    if (!t.isEmpty() && !models.contains(t)) models.add(t);
                }
            }
            // Also include the profile's currently-saved model even if it
            // isn't in either list (e.g. the user typed a custom model
            // directly into ProviderDetailActivity).
            if (p != null && p.modelId != null && !p.modelId.isEmpty()
                    && !models.contains(p.modelId)) {
                models.add(0, p.modelId);
            }
            if (models.isEmpty()) continue;

            out.add(new ProviderGroup(entry.id, entry.displayName, models));
        }

        // Also include user-defined custom providers (profiles whose
        // providerId isn't in the catalog) as a single "Custom" group.
        List<String> customIds = new ArrayList<>();
        for (ProviderConfigStore.Profile prof : profiles) {
            if (prof == null || prof.providerId == null) continue;
            if (ProviderCatalog.get(prof.providerId) != null) continue;
            if ("openai-compat".equals(prof.providerId)) continue;
            if (!customIds.contains(prof.providerId)) customIds.add(prof.providerId);
        }
        for (String pid : customIds) {
            ProviderConfigStore.Profile prof = findProfile(profiles, pid);
            if (prof == null) continue;
            boolean configured = prof.apiKey != null && !prof.apiKey.isEmpty();
            if (!configured) continue;
            List<String> models = new ArrayList<>();
            if (prof.modelId != null && !prof.modelId.isEmpty()) {
                models.add(prof.modelId);
            }
            String rawCustom = customPrefs.getString("models_" + pid, "");
            if (rawCustom != null && !rawCustom.isEmpty()) {
                for (String s : rawCustom.split("\n")) {
                    String t = s.trim();
                    if (!t.isEmpty() && !models.contains(t)) models.add(t);
                }
            }
            if (models.isEmpty()) continue;
            String label = ProviderCatalog.safeDisplayName(pid);
            out.add(new ProviderGroup(pid, label, models));
        }

        return out;
    }

    private static List<ProviderConfigStore.Profile> safeProfiles(ProviderConfigStore store) {
        try {
            List<ProviderConfigStore.Profile> p = store.getProfiles();
            return p != null ? p : new ArrayList<>();
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    private static ProviderConfigStore.Profile findProfile(
            List<ProviderConfigStore.Profile> profiles, String providerId) {
        if (profiles == null) return null;
        for (ProviderConfigStore.Profile p : profiles) {
            if (p != null && providerId.equals(p.providerId)) return p;
        }
        return null;
    }

    private static boolean isConfigured(ProviderCatalog.Entry entry,
                                        ProviderConfigStore.Profile p) {
        if (!entry.requiresApiKey) return true;
        return p != null && p.apiKey != null && !p.apiKey.isEmpty();
    }

    // ------------------------------------------------------------------
    // Row builder
    // ------------------------------------------------------------------

    /**
     * Build the flat list of rows shown in the RecyclerView:
     * <ol>
     *   <li>(optional) Favorites header + every pinned model that matches
     *       the current query.</li>
     *   <li>For every provider group that has at least one model matching
     *       the query: a section header + one row per matching model.</li>
     * </ol>
     */
    private static List<AiModelSheetAdapter.Row> buildRows(
            Context context,
            List<ProviderGroup> groups,
            String query,
            String currentProvider,
            String currentModel) {
        List<AiModelSheetAdapter.Row> rows = new ArrayList<>();
        Set<String> pinned = getPinned(context);

        // --- Favorites section ---
        List<AiModelSheetAdapter.Row> favRows = new ArrayList<>();
        for (ProviderGroup g : groups) {
            for (String model : g.models) {
                if (!matchesQuery(g, model, query)) continue;
                if (!pinned.contains(pinnedKey(g.providerId, model))) continue;
                boolean sel = g.providerId.equals(currentProvider)
                        && model.equals(currentModel);
                favRows.add(new AiModelSheetAdapter.Row(
                        g.providerId, g.label, model, sel, true));
            }
        }
        if (!favRows.isEmpty()) {
            rows.add(new AiModelSheetAdapter.Row(
                    AiModelSheetAdapter.PROVIDER_FAVORITES,
                    context.getString(R.string.ai_model_sheet_provider_section_favorites)));
            rows.addAll(favRows);
        }

        // --- Per-provider sections ---
        for (ProviderGroup g : groups) {
            List<String> matching = new ArrayList<>();
            for (String model : g.models) {
                if (matchesQuery(g, model, query)) matching.add(model);
            }
            if (matching.isEmpty()) continue;
            rows.add(new AiModelSheetAdapter.Row(g.providerId, g.label));
            for (String model : matching) {
                boolean sel = g.providerId.equals(currentProvider)
                        && model.equals(currentModel);
                boolean isPinned = pinned.contains(pinnedKey(g.providerId, model));
                rows.add(new AiModelSheetAdapter.Row(
                        g.providerId, g.label, model, sel, isPinned));
            }
        }
        return rows;
    }

    private static boolean matchesQuery(ProviderGroup g, String model, String query) {
        if (query == null || query.isEmpty()) return true;
        if (model != null && model.toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        if (g.label != null && g.label.toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        return g.providerId != null
                && g.providerId.toLowerCase(Locale.getDefault()).contains(query);
    }

    // ------------------------------------------------------------------
    // Provider chips
    // ------------------------------------------------------------------

    private static void buildProviderChips(
            Context context,
            LinearLayout container,
            List<ProviderGroup> groups,
            String selectedProviderId,
            ChipClickListener listener) {
        container.removeAllViews();
        int marginEnd = dp(context, 6);
        for (ProviderGroup g : groups) {
            LinearLayout chip = new LinearLayout(context);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setPadding(dp(context, 12), dp(context, 8),
                    dp(context, 14), dp(context, 8));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(marginEnd);
            chip.setLayoutParams(params);

            int iconRes = ProviderIconResolver.resolveProvider(g.providerId, g.label);
            if (iconRes != 0) {
                ImageView icon = new ImageView(context);
                icon.setImageResource(iconRes);
                icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                        dp(context, 18), dp(context, 18));
                iconParams.setMarginEnd(dp(context, 7));
                chip.addView(icon, iconParams);
            }

            TextView label = new TextView(context);
            label.setText(g.label);
            label.setTextSize(13f);
            boolean selected = g.providerId.equals(selectedProviderId);
            chip.setBackgroundResource(selected
                    ? R.drawable.ai_provider_chip_selected_bg
                    : R.drawable.ai_provider_chip_bg);
            label.setTextColor(context.getColor(selected
                    ? R.color.ai_chat_accent
                    : R.color.ai_chat_text_primary));
            chip.addView(label);
            chip.setOnClickListener(v -> listener.onChipClick(g.providerId));
            container.addView(chip);
        }
    }

    private interface ChipClickListener {
        void onChipClick(String providerId);
    }

    // ------------------------------------------------------------------
    // Selection persistence
    // ------------------------------------------------------------------

    /**
     * Apply a (provider, model) selection: find the user's stored profile
     * for that provider (creating a fresh one if necessary), update its
     * modelId, persist it, and mark it active.
     */
    private static void applySelection(Context ctx, ProviderConfigStore store,
                                       String providerId, String modelId) {
        try {
            List<ProviderConfigStore.Profile> profiles = store.getProfiles();
            ProviderConfigStore.Profile target = findProfile(profiles, providerId);
            if (target == null) {
                // No profile for this provider yet — create one from the
                // catalog defaults so the agent can build a valid client.
                ProviderCatalog.Entry entry = ProviderCatalog.getOrDefault(providerId);
                target = new ProviderConfigStore.Profile();
                target.providerId = providerId;
                target.name = entry.displayName;
                target.baseUrl = entry.defaultBaseUrl;
                target.modelId = modelId;
                // Carry over the API key from the previously-active profile
                // so the user doesn't have to re-enter it when switching
                // providers that share a key (e.g. an OpenRouter key).
                ProviderConfigStore.Profile prev = store.getActiveProfile();
                if (prev != null && prev.apiKey != null && !prev.apiKey.isEmpty()) {
                    target.apiKey = prev.apiKey;
                }
            } else {
                target.modelId = modelId;
            }
            store.upsertProfile(target);
            store.setActiveProfile(target.id);
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------
    // Favorites persistence
    // ------------------------------------------------------------------

    private static Set<String> getPinned(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHAT, Context.MODE_PRIVATE);
        Set<String> pinned = new HashSet<>(prefs.getStringSet(PREF_PINNED, new HashSet<>()));
        pinned.addAll(prefs.getStringSet(PREF_PINNED_LEGACY, new HashSet<>()));
        return pinned;
    }

    private static void togglePinned(Context context, String providerId, String modelId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHAT, Context.MODE_PRIVATE);
        Set<String> pinned = new HashSet<>(prefs.getStringSet(PREF_PINNED, new HashSet<>()));
        Set<String> legacy = new HashSet<>(prefs.getStringSet(PREF_PINNED_LEGACY, new HashSet<>()));
        pinned.addAll(legacy);
        String key = pinnedKey(providerId, modelId);
        if (pinned.contains(key)) {
            pinned.remove(key);
            legacy.remove(key);
        } else {
            pinned.add(key);
        }
        prefs.edit()
                .putStringSet(PREF_PINNED, pinned)
                .putStringSet(PREF_PINNED_LEGACY, legacy)
                .apply();
    }

    private static String pinnedKey(String providerId, String modelId) {
        return (providerId == null ? "" : providerId)
                + "::" + (modelId == null ? "" : modelId);
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
