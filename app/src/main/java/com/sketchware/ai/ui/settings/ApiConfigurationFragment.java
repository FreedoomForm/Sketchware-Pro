package com.sketchware.ai.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import pro.sketchware.R;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

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

        // Provider spinner
        com.google.android.material.textfield.MaterialAutoCompleteTextView providerSpinner =
                root.findViewById(R.id.spinner_provider);
        String[] providers = {"openai-compat", "mistral", "anthropic", "openai", "openrouter", "deepseek", "zai", "together", "fireworks", "gemini", "ollama"};
        providerSpinner.setText(profile.providerId, false);
        providerSpinner.setSimpleItems(providers);
        providerSpinner.setOnItemClickListener((p, v, pos, id) -> {
            profile.providerId = providers[pos];
            // Pre-fill well-known base URLs for convenience.
            switch (profile.providerId) {
                case "mistral":    profile.baseUrl = "https://api.mistral.ai/v1"; break;
                case "anthropic":  profile.baseUrl = "https://api.anthropic.com"; break;
                case "openai":     profile.baseUrl = "https://api.openai.com/v1"; break;
                case "openrouter": profile.baseUrl = "https://openrouter.ai/api/v1"; break;
                case "deepseek":   profile.baseUrl = "https://api.deepseek.com"; break;
                case "zai":        profile.baseUrl = "https://api.z.ai/api/paas/v4"; break;
                case "together":   profile.baseUrl = "https://api.together.xyz/v1"; break;
                case "fireworks":  profile.baseUrl = "https://api.fireworks.ai/inference/v1"; break;
                case "gemini":     profile.baseUrl = "https://generativelanguage.googleapis.com"; break;
                case "ollama":     profile.baseUrl = "http://localhost:11434"; break;
            }
            baseUrl.setText(profile.baseUrl);
            // Pre-fill a default model id if empty.
            if (profile.modelId == null || profile.modelId.isEmpty()) {
                switch (profile.providerId) {
                    case "mistral":    profile.modelId = "mistral-large-latest"; break;
                    case "anthropic":  profile.modelId = "claude-sonnet-4-20250514"; break;
                    case "openai":     profile.modelId = "gpt-4o"; break;
                    case "openrouter": profile.modelId = "anthropic/claude-3.5-sonnet"; break;
                    case "deepseek":   profile.modelId = "deepseek-chat"; break;
                    case "zai":        profile.modelId = "glm-4.6"; break;
                    case "together":   profile.modelId = "meta-llama/Llama-3.3-70B-Instruct-Turbo"; break;
                    case "fireworks":  profile.modelId = "accounts/fireworks/models/llama-v3p1-70b-instruct"; break;
                    case "gemini":     profile.modelId = "gemini-2.0-flash"; break;
                    case "ollama":     profile.modelId = "llama3.2"; break;
                }
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
            profile.baseUrl = baseUrl.getText() == null ? "" : baseUrl.getText().toString().trim();
            profile.apiKey = apiKey.getText() == null ? "" : apiKey.getText().toString().trim();
            profile.modelId = modelId.getText() == null ? "" : modelId.getText().toString().trim();
            try { profile.maxOutputTokens = Integer.parseInt(maxTokens.getText().toString().trim()); } catch (Exception e) {}
            try { profile.contextWindowSize = Integer.parseInt(contextWindow.getText().toString().trim()); } catch (Exception e) {}
            profile.enableReasoning = enableReasoning.isChecked();
            profile.enableStreaming = enableStreaming.isChecked();
            profile.imageSupport = imageSupport.isChecked();
            profile.promptCaching = promptCaching.isChecked();
            store.upsertProfile(profile);
            store.setActiveProfile(profile.id);
            requireActivity().finish();
        });
        return root;
    }
}
