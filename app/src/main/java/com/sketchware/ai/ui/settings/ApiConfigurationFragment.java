package com.sketchware.ai.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import pro.sketchware.R;
import com.sketchware.ai.llm.ProviderCatalog;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * API Configuration fragment - replicates the Kilo Code "Providers" page:
 * profile dropdown, provider id, base URL, API key (masked), model picker,
 * reasoning effort, max output tokens, context window size, image support,
 * prompt caching, pricing fields, custom headers.
 */
public final class ApiConfigurationFragment extends Fragment {

    private ProviderConfigStore store;
    private ProviderConfigStore.Profile profile;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_provider_settings, container, false);
        // Wire up fields to profile.
        TextInputEditText baseUrl = root.findViewById(R.id.et_base_url);
        TextInputEditText apiKey = root.findViewById(R.id.et_api_key);
        TextInputEditText modelId = root.findViewById(R.id.et_model_id);
        TextInputEditText maxTokens = root.findViewById(R.id.et_max_tokens);
        TextInputEditText contextWindow = root.findViewById(R.id.et_context_window);
        com.google.android.material.materialswitch.MaterialSwitch enableReasoning = root.findViewById(R.id.sw_reasoning);
        com.google.android.material.materialswitch.MaterialSwitch enableStreaming = root.findViewById(R.id.sw_streaming);
        com.google.android.material.materialswitch.MaterialSwitch imageSupport = root.findViewById(R.id.sw_image_support);
        com.google.android.material.materialswitch.MaterialSwitch promptCaching = root.findViewById(R.id.sw_prompt_caching);
        com.google.android.material.materialswitch.MaterialSwitch forceFlatToolFormat = root.findViewById(R.id.sw_force_flat_tool_format);

        // Defensive defaults — older stored profiles may have null/empty
        // values for fields added after the profile was first created.
        if (profile.reasoningEffort == null || profile.reasoningEffort.isEmpty()) {
            profile.reasoningEffort = "medium";
        }

        baseUrl.setText(profile.baseUrl);
        apiKey.setText(profile.apiKey);
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        modelId.setText(profile.modelId);
        maxTokens.setText(String.valueOf(profile.maxOutputTokens));
        contextWindow.setText(String.valueOf(profile.contextWindowSize));
        enableReasoning.setChecked(profile.enableReasoning);
        enableStreaming.setChecked(profile.enableStreaming);
        imageSupport.setChecked(profile.imageSupport);
        promptCaching.setChecked(profile.promptCaching);
        forceFlatToolFormat.setChecked(profile.forceFlatToolFormat);

        // Provider spinner — uses ProviderCatalog as the single source of truth.
        // Previously this was a hard-coded 11-item array with a parallel switch
        // for base URLs and a third switch for default models; the three lists
        // kept diverging (OpenRouter base URL was /api here vs /api/v1 in
        // ChatFragment, etc.). Going through the catalog keeps them in sync
        // and surfaces every supported provider, not just the 11 originally
        // listed.
        com.google.android.material.textfield.MaterialAutoCompleteTextView providerSpinner =
                root.findViewById(R.id.spinner_provider);
        String[] providerIds = ProviderCatalog.ids().toArray(new String[0]);
        String[] providerLabels = ProviderCatalog.displayNames().toArray(new String[0]);
        providerSpinner.setText(ProviderCatalog.safeDisplayName(profile.providerId), false);
        providerSpinner.setSimpleItems(providerLabels);
        providerSpinner.setOnItemClickListener((p, v, pos, id) -> {
            profile.providerId = providerIds[pos];
            ProviderCatalog.Entry entry = ProviderCatalog.getOrDefault(profile.providerId);
            // Pre-fill well-known base URL.
            if (entry.defaultBaseUrl != null && !entry.defaultBaseUrl.isEmpty()) {
                profile.baseUrl = entry.defaultBaseUrl;
                baseUrl.setText(profile.baseUrl);
            }
            // Clear any stale error when the provider changes.
            findTextInputLayout(baseUrl).setError(null);
            // Pre-fill a default model id if the user hasn't picked one yet.
            if ((profile.modelId == null || profile.modelId.isEmpty())
                    && entry.defaultModel != null && !entry.defaultModel.isEmpty()) {
                profile.modelId = entry.defaultModel;
                modelId.setText(profile.modelId);
            }
        });

        // Reasoning effort spinner
        com.google.android.material.textfield.MaterialAutoCompleteTextView effortSpinner =
                root.findViewById(R.id.spinner_reasoning_effort);
        String[] efforts = {"none", "minimal", "low", "medium", "high", "xhigh", "max"};
        effortSpinner.setText(profile.reasoningEffort, false);
        effortSpinner.setSimpleItems(efforts);
        effortSpinner.setOnItemClickListener((p, v, pos, id) -> profile.reasoningEffort = efforts[pos]);

        // Save button
        root.findViewById(R.id.btn_save).setOnClickListener(v -> {
            // Read raw field values.
            String baseUrlRaw = baseUrl.getText() == null ? "" : baseUrl.getText().toString().trim();
            String apiKeyRaw = apiKey.getText() == null ? "" : apiKey.getText().toString().trim();
            String modelIdRaw = modelId.getText() == null ? "" : modelId.getText().toString().trim();
            String maxTokensRaw = maxTokens.getText() == null ? "" : maxTokens.getText().toString().trim();
            String contextWindowRaw = contextWindow.getText() == null ? "" : contextWindow.getText().toString().trim();

            // --- Validation (previously: any garbage was silently saved
            // and the agent would fail at runtime with a cryptic error) ---
            // baseUrl: must be empty (some providers like Ollama can run
            // with defaults) or a syntactically valid http(s) URL.
            TextInputLayout baseUrlLayout = findTextInputLayout(baseUrl);
            if (!baseUrlRaw.isEmpty() && !isValidHttpUrl(baseUrlRaw)) {
                baseUrlLayout.setError("Enter a valid http:// or https:// URL");
                return;
            } else {
                baseUrlLayout.setError(null);
            }

            // Ollama can run without an API key, but every other provider
            // requires one.
            TextInputLayout apiKeyLayout = findTextInputLayout(apiKey);
            boolean needsKey = !"ollama".equals(profile.providerId);
            if (needsKey && apiKeyRaw.isEmpty()) {
                apiKeyLayout.setError("API key is required for " + profile.providerId);
                return;
            } else {
                apiKeyLayout.setError(null);
            }

            TextInputLayout modelIdLayout = findTextInputLayout(modelId);
            if (modelIdRaw.isEmpty()) {
                modelIdLayout.setError("Model ID is required");
                return;
            } else {
                modelIdLayout.setError(null);
            }

            TextInputLayout maxTokensLayout = findTextInputLayout(maxTokens);
            int maxTokensVal;
            try {
                maxTokensVal = Integer.parseInt(maxTokensRaw);
            } catch (NumberFormatException e) {
                maxTokensLayout.setError("Must be a number");
                return;
            }
            if (maxTokensVal <= 0) {
                maxTokensLayout.setError("Must be greater than 0");
                return;
            }
            maxTokensLayout.setError(null);

            TextInputLayout contextWindowLayout = findTextInputLayout(contextWindow);
            int contextWindowVal;
            try {
                contextWindowVal = Integer.parseInt(contextWindowRaw);
            } catch (NumberFormatException e) {
                contextWindowLayout.setError("Must be a number (0 = model default)");
                return;
            }
            if (contextWindowVal < 0) {
                contextWindowLayout.setError("Cannot be negative");
                return;
            }
            contextWindowLayout.setError(null);

            // --- Commit to profile ---
            profile.baseUrl = baseUrlRaw;
            profile.apiKey = apiKeyRaw;
            profile.modelId = modelIdRaw;
            profile.maxOutputTokens = maxTokensVal;
            profile.contextWindowSize = contextWindowVal;
            profile.enableReasoning = enableReasoning.isChecked();
            profile.enableStreaming = enableStreaming.isChecked();
            profile.imageSupport = imageSupport.isChecked();
            profile.promptCaching = promptCaching.isChecked();
            profile.forceFlatToolFormat = forceFlatToolFormat.isChecked();
            store.upsertProfile(profile);
            store.setActiveProfile(profile.id);
            View sbHost = getView();
            if (sbHost != null) {
                Snackbar.make(sbHost, "Saved", Snackbar.LENGTH_SHORT).show();
            }
            requireActivity().finish();
        });
        return root;
    }

    /**
     * Validate that {@code url} is a syntactically valid http(s) URL. Used to
     * reject garbage like {@code htp://invalid} before saving the profile —
     * the HTTP client would otherwise fail at runtime with a less helpful
     * error.
     */
    private static boolean isValidHttpUrl(String url) {
        try {
            URL u = new URL(url);
            String proto = u.getProtocol();
            return "http".equalsIgnoreCase(proto) || "https".equalsIgnoreCase(proto);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Walk up the parent chain of {@code v} until a {@link TextInputLayout}
     * is found. The direct parent of a {@link TextInputEditText} is normally
     * the TextInputLayout itself, but Material Components may introduce
     * intermediate helper views in some versions; walking the chain avoids a
     * ClassCastException in those cases.
     */
    private static TextInputLayout findTextInputLayout(View v) {
        android.view.ViewParent p = v.getParent();
        while (p != null && !(p instanceof TextInputLayout)) {
            p = p.getParent();
        }
        return (TextInputLayout) p;
    }
}
