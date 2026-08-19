package com.sketchware.ai.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import pro.sketchware.R;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

/** Experimental settings: background editing, AI image generation. */
public final class ExperimentalFragment extends Fragment {

    private ProviderConfigStore store;
    private ProviderConfigStore.Profile profile;

    private MaterialSwitch swBackgroundEditing;
    private MaterialSwitch swImageGeneration;
    private TextInputEditText etImageProvider;
    private TextInputEditText etImageApiKey;
    private TextInputEditText etImageModel;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_experimental, container, false);

        swBackgroundEditing = root.findViewById(R.id.sw_background_editing);
        swImageGeneration = root.findViewById(R.id.sw_image_generation);
        etImageProvider = root.findViewById(R.id.et_image_provider);
        etImageApiKey = root.findViewById(R.id.et_image_api_key);
        etImageModel = root.findViewById(R.id.et_image_model);

        // Load current values from the active profile.
        swBackgroundEditing.setChecked(profile.backgroundEditing);
        swImageGeneration.setChecked(profile.enableImageGeneration);
        etImageProvider.setText(profile.imageProviderId == null ? "" : profile.imageProviderId);
        etImageApiKey.setText(profile.imageApiKey == null ? "" : profile.imageApiKey);
        etImageApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etImageModel.setText(profile.imageModel == null ? "" : profile.imageModel);

        // Auto-save on every toggle / focus change. Previously this fragment
        // was a pure layout inflate — anything the user changed was silently
        // lost on navigation away.
        swBackgroundEditing.setOnCheckedChangeListener((b, checked) -> {
            profile.backgroundEditing = checked;
            saveProfile();
        });
        swImageGeneration.setOnCheckedChangeListener((b, checked) -> {
            profile.enableImageGeneration = checked;
            saveProfile();
        });
        View.OnFocusChangeListener saveOnFocus = (v, hasFocus) -> {
            if (!hasFocus) saveProfile();
        };
        etImageProvider.setOnFocusChangeListener(saveOnFocus);
        etImageApiKey.setOnFocusChangeListener(saveOnFocus);
        etImageModel.setOnFocusChangeListener(saveOnFocus);

        return root;
    }

    /** Persist the text fields + switch states to the active profile. */
    private void saveProfile() {
        profile.imageProviderId = etText(etImageProvider);
        profile.imageApiKey = etText(etImageApiKey);
        profile.imageModel = etText(etImageModel);
        store.upsertProfile(profile);
        store.setActiveProfile(profile.id);
        View sbHost = getView();
        if (sbHost != null) {
            Snackbar.make(sbHost, "Saved", Snackbar.LENGTH_SHORT).show();
        }
    }

    private static String etText(TextInputEditText et) {
        if (et.getText() == null) return "";
        return et.getText().toString().trim();
    }
}
