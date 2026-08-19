package com.sketchware.ai.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pro.sketchware.R;
import com.sketchware.ai.llm.ProviderCatalog;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

/**
 * Detail editor for a single AI provider.
 *
 * <p>Hosted in two tabs at the bottom: "Config" (API key, base URL, API
 * path, model id, reasoning effort, max tokens, switches) and "Models"
 * (list of built-in models for this provider + any custom models the user
 * has added + Fetch button + Add Model button).
 *
 * <p>This is the spiritual successor to {@link ApiConfigurationFragment} —
 * the old fragment crammed every provider into one form; this activity is
 * per-provider, mirrors the FabioSilva11/Sketchware-IA "ProviderDetail"
 * screen, and lets the user keep separate configs for every provider at
 * once instead of overwriting the active profile each time.
 *
 * <p>Storage strategy:
 * <ul>
 *   <li>Each provider id gets its own {@link ProviderConfigStore.Profile}
 *       entry (id = "provider:&lt;providerId&gt;" so it's stable).</li>
 *   <li>If the user activates the provider (taps Save), this profile
 *       becomes the active one — the chat will use it on the next turn.</li>
 *   <li>Custom models added via the Models tab are persisted in
 *       SharedPreferences under {@code custom_models_<providerId>} as a
 *       newline-delimited list; built-in models from
 *       {@link ProviderCatalog} are always shown above them.</li>
 * </ul>
 *
 * <p>The "Fetch" button hits the provider's {@code /models} endpoint (or
 * Gemini's {@code /v1beta/models} / Anthropic's {@code /v1/models} with
 * the right auth header) and adds every returned model id to the custom
 * list. This is a best-effort network call; failures are surfaced as a
 * toast.
 */
public final class ProviderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "provider_id";
    public static final String EXTRA_PROVIDER_TITLE = "provider_title";

    public static void start(Context ctx, String providerId, String providerTitle) {
        Intent i = new Intent(ctx, ProviderDetailActivity.class);
        i.putExtra(EXTRA_PROVIDER_ID, providerId);
        i.putExtra(EXTRA_PROVIDER_TITLE, providerTitle);
        ctx.startActivity(i);
    }

    private ProviderConfigStore store;
    private ProviderCatalog.Entry entry;
    private ProviderConfigStore.Profile profile;

    // Config tab views
    private ImageView providerIcon;
    private TextView providerName;
    private TextView providerFamily;
    private TextView statusBadge;
    private TextInputLayout tilApiKey;
    private TextInputEditText etApiKey;
    private TextInputEditText etBaseUrl;
    private TextInputEditText etApiPath;
    private TextInputEditText etModelId;
    private MaterialAutoCompleteTextView spinnerReasoningEffort;
    private TextInputEditText etMaxTokens;
    private TextInputEditText etContextWindow;
    private MaterialSwitch swReasoning;
    private MaterialSwitch swStreaming;
    private MaterialSwitch swImageSupport;
    private MaterialSwitch swPromptCaching;
    private MaterialSwitch swForceFlatToolFormat;
    private MaterialButton btnSave;

    // Models tab views
    private RecyclerView modelsRecycler;
    private TextView modelsEmptyState;
    private MaterialButton btnFetchModels;
    private MaterialButton btnAddModel;
    private ModelsAdapter modelsAdapter;

    // Tab bar
    private LinearLayout tabConfig;
    private LinearLayout tabModels;
    private ImageView iconTabConfig;
    private ImageView iconTabModels;
    private TextView labelTabConfig;
    private TextView labelTabModels;
    private View configTab;
    private View modelsTab;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        if (providerId == null) providerId = "openai-compat";
        entry = ProviderCatalog.getOrDefault(providerId);
        setContentView(R.layout.ai_provider_detail);

        store = new ProviderConfigStore(this);
        profile = findOrCreateProfileFor(providerId);

        // Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(entry.displayName);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindConfigTab();
        bindModelsTab();
        bindTabBar();

        showConfigTab();
    }

    private ProviderConfigStore.Profile findOrCreateProfileFor(String providerId) {
        // Find existing profile for this providerId; if none, create a fresh
        // one with the catalog defaults pre-filled so the user only has to
        // enter their API key.
        for (ProviderConfigStore.Profile p : store.getProfiles()) {
            if (p != null && providerId.equals(p.providerId)) return p;
        }
        ProviderConfigStore.Profile p = new ProviderConfigStore.Profile();
        p.id = "provider:" + providerId;
        p.name = entry.displayName;
        p.providerId = providerId;
        p.baseUrl = entry.defaultBaseUrl;
        p.modelId = entry.defaultModel;
        return p;
    }

    // ------------------------------------------------------------------
    // Config tab
    // ------------------------------------------------------------------

    private void bindConfigTab() {
        providerIcon = findViewById(R.id.provider_icon);
        providerName = findViewById(R.id.provider_name);
        providerFamily = findViewById(R.id.provider_family);
        statusBadge = findViewById(R.id.status_badge);
        tilApiKey = findViewById(R.id.til_api_key);
        etApiKey = findViewById(R.id.et_api_key);
        etBaseUrl = findViewById(R.id.et_base_url);
        etApiPath = findViewById(R.id.et_api_path);
        etModelId = findViewById(R.id.et_model_id);
        spinnerReasoningEffort = findViewById(R.id.spinner_reasoning_effort);
        etMaxTokens = findViewById(R.id.et_max_tokens);
        etContextWindow = findViewById(R.id.et_context_window);
        swReasoning = findViewById(R.id.sw_reasoning);
        swStreaming = findViewById(R.id.sw_streaming);
        swImageSupport = findViewById(R.id.sw_image_support);
        swPromptCaching = findViewById(R.id.sw_prompt_caching);
        swForceFlatToolFormat = findViewById(R.id.sw_force_flat_tool_format);
        btnSave = findViewById(R.id.btn_save);

        providerIcon.setImageResource(ProviderIconResolver.resolveProvider(entry.id, entry.displayName));
        providerName.setText(entry.displayName);
        providerFamily.setText(String.format(Locale.ROOT, "Family: %s", entry.family));

        // Hide API key field for local providers that don't need one.
        if (!entry.requiresApiKey) {
            tilApiKey.setVisibility(View.GONE);
        } else {
            tilApiKey.setVisibility(View.VISIBLE);
        }

        if (profile.apiKey != null) etApiKey.setText(profile.apiKey);
        etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        if (profile.baseUrl == null || profile.baseUrl.isEmpty()) {
            etBaseUrl.setText(entry.defaultBaseUrl);
        } else {
            etBaseUrl.setText(profile.baseUrl);
        }
        etApiPath.setText(entry.defaultApiPath);

        if (profile.modelId == null || profile.modelId.isEmpty()) {
            etModelId.setText(entry.defaultModel);
        } else {
            etModelId.setText(profile.modelId);
        }
        // Tapping the model id field opens the model picker.
        etModelId.setOnClickListener(v -> showModelPicker());

        // Reasoning effort dropdown
        String[] efforts = {"none", "minimal", "low", "medium", "high", "xhigh", "max"};
        String currentEffort = (profile.reasoningEffort == null || profile.reasoningEffort.isEmpty())
                ? "medium" : profile.reasoningEffort;
        spinnerReasoningEffort.setText(currentEffort, false);
        spinnerReasoningEffort.setSimpleItems(efforts);
        spinnerReasoningEffort.setOnItemClickListener((p, v, pos, id) -> profile.reasoningEffort = efforts[pos]);

        etMaxTokens.setText(String.valueOf(profile.maxOutputTokens));
        etContextWindow.setText(String.valueOf(profile.contextWindowSize));

        // Defensive: ensure effort is set if user never tapped the dropdown.
        if (profile.reasoningEffort == null || profile.reasoningEffort.isEmpty()) {
            profile.reasoningEffort = currentEffort;
        }
        swReasoning.setChecked(profile.enableReasoning);
        swStreaming.setChecked(profile.enableStreaming);
        swImageSupport.setChecked(profile.imageSupport);
        swPromptCaching.setChecked(profile.promptCaching);
        swForceFlatToolFormat.setChecked(profile.forceFlatToolFormat);

        updateStatusBadge();

        btnSave.setOnClickListener(v -> saveAndExit());
    }

    private void updateStatusBadge() {
        boolean configured;
        if (!entry.requiresApiKey) {
            configured = true;
        } else {
            String key = etApiKey.getText() == null ? "" : etApiKey.getText().toString().trim();
            configured = !key.isEmpty();
        }
        statusBadge.setSelected(configured);
        statusBadge.setText(configured
                ? getText(R.string.ai_providers_status_on)
                : getText(R.string.ai_providers_status_off));
        statusBadge.setTextColor(getResources().getColor(configured
                ? R.color.ai_provider_status_on_text
                : R.color.ai_provider_status_off_text));
    }

    private void saveAndExit() {
        String apiKey = etApiKey.getText() == null ? "" : etApiKey.getText().toString().trim();
        String baseUrl = etBaseUrl.getText() == null ? "" : etBaseUrl.getText().toString().trim();
        String modelId = etModelId.getText() == null ? "" : etModelId.getText().toString().trim();
        String maxTokensStr = etMaxTokens.getText() == null ? "" : etMaxTokens.getText().toString().trim();
        String contextWindowStr = etContextWindow.getText() == null ? "" : etContextWindow.getText().toString().trim();

        if (entry.requiresApiKey && apiKey.isEmpty()) {
            tilApiKey.setError("API key is required");
            return;
        }
        if (modelId.isEmpty()) {
            Snackbar.make(btnSave, "Model ID is required", Snackbar.LENGTH_SHORT).show();
            return;
        }
        int maxTokens;
        try { maxTokens = Integer.parseInt(maxTokensStr); }
        catch (NumberFormatException e) {
            Snackbar.make(btnSave, "Max tokens must be a number", Snackbar.LENGTH_SHORT).show();
            return;
        }
        int contextWindow;
        try { contextWindow = Integer.parseInt(contextWindowStr); }
        catch (NumberFormatException e) {
            Snackbar.make(btnSave, "Context window must be a number", Snackbar.LENGTH_SHORT).show();
            return;
        }

        profile.apiKey = apiKey;
        profile.baseUrl = baseUrl;
        profile.modelId = modelId;
        profile.maxOutputTokens = maxTokens;
        profile.contextWindowSize = contextWindow;
        profile.enableReasoning = swReasoning.isChecked();
        profile.enableStreaming = swStreaming.isChecked();
        profile.imageSupport = swImageSupport.isChecked();
        profile.promptCaching = swPromptCaching.isChecked();
        profile.forceFlatToolFormat = swForceFlatToolFormat.isChecked();
        store.upsertProfile(profile);
        store.setActiveProfile(profile.id);

        View sb = findViewById(android.R.id.content);
        Snackbar.make(sb, getText(R.string.ai_provider_detail_saved), Snackbar.LENGTH_SHORT).show();
        finish();
    }

    // ------------------------------------------------------------------
    // Models tab
    // ------------------------------------------------------------------

    private void bindModelsTab() {
        modelsRecycler = findViewById(R.id.models_recycler);
        modelsEmptyState = findViewById(R.id.models_empty_state);
        btnFetchModels = findViewById(R.id.btn_fetch_models);
        btnAddModel = findViewById(R.id.btn_add_model);

        modelsRecycler.setLayoutManager(new LinearLayoutManager(this));
        modelsAdapter = new ModelsAdapter();
        modelsRecycler.setAdapter(modelsAdapter);
        refreshModelsList();

        btnFetchModels.setOnClickListener(v -> fetchModels());
        btnAddModel.setOnClickListener(v -> showAddModelDialog());
    }

    private void refreshModelsList() {
        List<String> models = new ArrayList<>(entry.builtinModels);
        // Add user-saved custom models for this provider.
        Set<String> customs = getCustomModels(entry.id);
        for (String m : customs) {
            if (!models.contains(m)) models.add(m);
        }
        modelsAdapter.submitList(models, profile.modelId);
        modelsEmptyState.setVisibility(models.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private Set<String> getCustomModels(String providerId) {
        Set<String> out = new LinkedHashSet<>();
        try {
            String raw = getSharedPreferences("ai_custom_models", MODE_PRIVATE)
                    .getString("models_" + providerId, "");
            if (raw != null && !raw.isEmpty()) {
                for (String s : raw.split("\n")) {
                    String t = s.trim();
                    if (!t.isEmpty()) out.add(t);
                }
            }
        } catch (Throwable ignored) { }
        return out;
    }

    private void saveCustomModels(String providerId, Set<String> models) {
        StringBuilder sb = new StringBuilder();
        for (String m : models) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(m);
        }
        getSharedPreferences("ai_custom_models", MODE_PRIVATE)
                .edit()
                .putString("models_" + providerId, sb.toString())
                .apply();
    }

    private void showAddModelDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(R.string.ai_models_add_id_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_models_add_title)
                .setView(input)
                .setPositiveButton(R.string.ai_models_add_confirm, (d, w) -> {
                    String m = input.getText() == null ? "" : input.getText().toString().trim();
                    if (m.isEmpty()) {
                        Toast.makeText(this, R.string.ai_models_add_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Set<String> customs = getCustomModels(entry.id);
                    customs.add(m);
                    saveCustomModels(entry.id, customs);
                    refreshModelsList();
                })
                .setNegativeButton(R.string.ai_models_add_cancel, null)
                .show();
    }

    private void showModelPicker() {
        // Reuse the bottom sheet — but for simplicity, fall back to a
        // MaterialAlertDialog list of models for the current provider.
        List<String> models = new ArrayList<>(entry.builtinModels);
        models.addAll(getCustomModels(entry.id));
        if (models.isEmpty()) {
            Snackbar.make(btnSave, R.string.ai_model_sheet_no_models, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String[] arr = models.toArray(new String[0]);
        int currentIdx = -1;
        String current = etModelId.getText() == null ? "" : etModelId.getText().toString();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(current)) { currentIdx = i; break; }
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_model_sheet_title)
                .setSingleChoiceItems(arr, currentIdx, (d, w) -> {
                    etModelId.setText(arr[w]);
                    profile.modelId = arr[w];
                    d.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Background fetch of {@code GET <base>/models} (or provider-specific
     * equivalent). Surfaces the count via Snackbar on success, or the
     * error message on failure. Network call runs on a worker thread; UI
     * is updated on the main thread.
     */
    private void fetchModels() {
        String baseUrl = etBaseUrl.getText() == null ? "" : etBaseUrl.getText().toString().trim();
        if (baseUrl.isEmpty()) baseUrl = entry.defaultBaseUrl;
        if (baseUrl.isEmpty()) {
            Snackbar.make(btnFetchModels, "Base URL is empty", Snackbar.LENGTH_SHORT).show();
            return;
        }
        final String key = etApiKey.getText() == null ? "" : etApiKey.getText().toString().trim();
        final String urlBase = baseUrl;
        final String pid = entry.id;

        btnFetchModels.setEnabled(false);
        Snackbar.make(btnFetchModels,
                String.format(getText(R.string.ai_models_fetching).toString(), entry.displayName),
                Snackbar.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                List<String> fetched = doFetchModels(pid, urlBase, key);
                runOnUiThread(() -> {
                    btnFetchModels.setEnabled(true);
                    if (fetched.isEmpty()) {
                        Snackbar.make(btnFetchModels,
                                String.format(getText(R.string.ai_models_fetch_failed).toString(), "no models"),
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Set<String> customs = getCustomModels(pid);
                    customs.addAll(fetched);
                    saveCustomModels(pid, customs);
                    refreshModelsList();
                    Snackbar.make(btnFetchModels,
                            String.format(getText(R.string.ai_models_fetched).toString(), fetched.size()),
                            Snackbar.LENGTH_SHORT).show();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    btnFetchModels.setEnabled(true);
                    Snackbar.make(btnFetchModels,
                            String.format(getText(R.string.ai_models_fetch_failed).toString(),
                                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
                            Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Synchronous network call to fetch the model list from the provider.
     * Switches on the provider family to pick the right URL &amp; headers
     * (OpenAI-compat: {@code GET <base>/models} with Bearer; Anthropic:
     * {@code GET <base>/v1/models} with {@code x-api-key}; Gemini:
     * {@code GET <base>/v1beta/models} with {@code x-goog-api-key}).
     */
    private static List<String> doFetchModels(String providerId, String baseUrl, String apiKey) throws Exception {
        String family = ProviderCatalog.familyOf(providerId);
        URL url;
        HttpURLConnection conn;
        org.json.JSONArray arr;

        if ("anthropic".equals(family)) {
            url = new URL(stripTrailingSlash(baseUrl) + "/v1/models");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            arr = readJsonArray(conn, "data");
        } else if ("gemini".equals(family)) {
            url = new URL(stripTrailingSlash(baseUrl) + "/v1beta/models");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("x-goog-api-key", apiKey);
            arr = readJsonArray(conn, "models");
        } else if ("ollama".equals(family)) {
            url = new URL(stripTrailingSlash(baseUrl) + "/api/tags");
            conn = (HttpURLConnection) url.openConnection();
            arr = readJsonArray(conn, "models");
        } else {
            // OpenAI-compatible: GET <base>/models with Bearer auth.
            url = new URL(stripTrailingSlash(baseUrl) + "/models");
            conn = (HttpURLConnection) url.openConnection();
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            arr = readJsonArray(conn, "data");
        }

        List<String> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", o.optString("name", ""));
            if (id.isEmpty()) continue;
            // Gemini returns "models/gemini-1.5-flash"; strip the prefix.
            if (id.startsWith("models/")) id = id.substring(7);
            out.add(id);
        }
        Collections.sort(out);
        return out;
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static org.json.JSONArray readJsonArray(HttpURLConnection conn, String field) throws Exception {
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        int code = conn.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new RuntimeException("HTTP " + code);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        String body = sb.toString();
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            org.json.JSONArray arr = o.optJSONArray(field);
            return arr != null ? arr : new org.json.JSONArray();
        } catch (org.json.JSONException notObject) {
            // Maybe the response IS an array directly.
            try {
                return new org.json.JSONArray(body);
            } catch (org.json.JSONException e) {
                return new org.json.JSONArray();
            }
        }
    }

    // ------------------------------------------------------------------
    // Tab bar
    // ------------------------------------------------------------------

    private void bindTabBar() {
        tabConfig = findViewById(R.id.tab_config);
        tabModels = findViewById(R.id.tab_models);
        iconTabConfig = findViewById(R.id.icon_tab_config);
        iconTabModels = findViewById(R.id.icon_tab_models);
        labelTabConfig = findViewById(R.id.label_tab_config);
        labelTabModels = findViewById(R.id.label_tab_models);
        configTab = findViewById(R.id.config_tab);
        modelsTab = findViewById(R.id.models_tab);

        tabConfig.setOnClickListener(v -> showConfigTab());
        tabModels.setOnClickListener(v -> showModelsTab());
    }

    private void showConfigTab() {
        configTab.setVisibility(View.VISIBLE);
        modelsTab.setVisibility(View.GONE);
        iconTabConfig.setColorFilter(getResources().getColor(R.color.ai_chat_accent));
        iconTabModels.setColorFilter(getResources().getColor(R.color.ai_chat_text_secondary));
        labelTabConfig.setTextColor(getResources().getColor(R.color.ai_chat_accent));
        labelTabModels.setTextColor(getResources().getColor(R.color.ai_chat_text_secondary));
    }

    private void showModelsTab() {
        configTab.setVisibility(View.GONE);
        modelsTab.setVisibility(View.VISIBLE);
        iconTabConfig.setColorFilter(getResources().getColor(R.color.ai_chat_text_secondary));
        iconTabModels.setColorFilter(getResources().getColor(R.color.ai_chat_accent));
        labelTabConfig.setTextColor(getResources().getColor(R.color.ai_chat_text_secondary));
        labelTabModels.setTextColor(getResources().getColor(R.color.ai_chat_accent));
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_test_provider) {
            testProvider();
            return true;
        }
        if (id == R.id.action_share_provider) {
            shareProvider();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void testProvider() {
        String key = etApiKey.getText() == null ? "" : etApiKey.getText().toString().trim();
        String baseUrl = etBaseUrl.getText() == null ? "" : etBaseUrl.getText().toString().trim();
        if (baseUrl.isEmpty()) baseUrl = entry.defaultBaseUrl;
        final String finalBase = baseUrl;
        final String finalKey = key;
        View sb = findViewById(android.R.id.content);
        Snackbar.make(sb, "Testing…", Snackbar.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                List<String> models = doFetchModels(entry.id, finalBase, finalKey);
                runOnUiThread(() -> {
                    if (models.isEmpty()) {
                        Snackbar.make(sb,
                                String.format(getText(R.string.ai_provider_detail_test_failed).toString(), "no models returned"),
                                Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(sb,
                                getText(R.string.ai_provider_detail_test_ok) + " (" + models.size() + " models)",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            } catch (final Exception e) {
                runOnUiThread(() -> Snackbar.make(sb,
                        String.format(getText(R.string.ai_provider_detail_test_failed).toString(),
                                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
                        Snackbar.LENGTH_LONG).show());
            }
        }).start();
    }

    private void shareProvider() {
        String key = etApiKey.getText() == null ? "" : etApiKey.getText().toString().trim();
        String baseUrl = etBaseUrl.getText() == null ? "" : etBaseUrl.getText().toString().trim();
        String modelId = etModelId.getText() == null ? "" : etModelId.getText().toString().trim();
        // Never include the API key in the share text — that's a security risk.
        String text = "Provider: " + entry.displayName + "\n"
                + "Base URL: " + baseUrl + "\n"
                + "Model: " + modelId;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share provider"));
    }

    // ------------------------------------------------------------------
    // Models adapter
    // ------------------------------------------------------------------

    private final class ModelsAdapter extends RecyclerView.Adapter<ModelsAdapter.ModelVH> {
        private final List<String> items = new ArrayList<>();
        private String selectedModel = "";

        void submitList(List<String> list, String selected) {
            items.clear();
            items.addAll(list);
            selectedModel = selected == null ? "" : selected;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ModelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ai_provider_detail_model_row, parent, false);
            return new ModelVH(v);
        }

        @Override public void onBindViewHolder(@NonNull ModelVH h, int position) {
            String model = items.get(position);
            h.bind(model, model.equals(selectedModel));
        }

        @Override public int getItemCount() { return items.size(); }

        final class ModelVH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView providerLabel;
            final ImageView check;
            final ImageView deleteBtn;

            ModelVH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.model_icon);
                name = v.findViewById(R.id.model_name);
                providerLabel = v.findViewById(R.id.model_provider);
                check = v.findViewById(R.id.selected_check);
                // Reuse the check ImageView as a delete button by toggling
                // its drawable on long-press — simpler than adding a second
                // widget to the row.
                deleteBtn = check;
            }

            void bind(String model, boolean selected) {
                icon.setImageResource(ProviderIconResolver.resolveModel(model));
                name.setText(model);
                providerLabel.setText(entry.displayName);
                check.setVisibility(selected ? View.VISIBLE : View.GONE);
                check.setImageResource(R.drawable.ic_check);
                itemView.setOnClickListener(v -> {
                    etModelId.setText(model);
                    profile.modelId = model;
                    updateStatusBadge();
                    showConfigTab();
                });
                itemView.setOnLongClickListener(v -> {
                    // Delete custom model (built-in models can't be removed).
                    if (entry.builtinModels.contains(model)) {
                        Toast.makeText(itemView.getContext(),
                                "Built-in models cannot be removed",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Set<String> customs = getCustomModels(entry.id);
                    customs.remove(model);
                    saveCustomModels(entry.id, customs);
                    refreshModelsList();
                    return true;
                });
            }
        }
    }
}
