package com.sketchware.ai.ui.settings;

import android.os.Bundle;
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
import com.sketchware.ai.llm.storage.ProviderConfigStore;

import java.util.Locale;

/** Advanced settings: pricing fields, reset to defaults. */
public final class AdvancedSettingsFragment extends Fragment {

    private ProviderConfigStore store;
    private ProviderConfigStore.Profile profile;

    private TextInputEditText etPriceInput;
    private TextInputEditText etPriceOutput;
    private TextInputEditText etPriceCacheRead;
    private TextInputEditText etPriceCacheWrite;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProviderConfigStore(requireContext());
        profile = store.getActiveProfile();
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_advanced, container, false);

        etPriceInput = root.findViewById(R.id.et_price_input);
        etPriceOutput = root.findViewById(R.id.et_price_output);
        etPriceCacheRead = root.findViewById(R.id.et_price_cache_read);
        etPriceCacheWrite = root.findViewById(R.id.et_price_cache_write);

        // Load current values. Format with up to 4 decimal places, stripping
        // trailing zeros for cleanliness.
        etPriceInput.setText(formatPrice(profile.inputPrice));
        etPriceOutput.setText(formatPrice(profile.outputPrice));
        etPriceCacheRead.setText(formatPrice(profile.cacheReadPrice));
        etPriceCacheWrite.setText(formatPrice(profile.cacheWritePrice));

        // Auto-save on focus loss. Previously this fragment was a pure layout
        // inflate with no wiring — any prices the user typed were silently
        // discarded on navigation away.
        View.OnFocusChangeListener saveOnFocus = (v, hasFocus) -> {
            if (!hasFocus) saveFromFields();
        };
        etPriceInput.setOnFocusChangeListener(saveOnFocus);
        etPriceOutput.setOnFocusChangeListener(saveOnFocus);
        etPriceCacheRead.setOnFocusChangeListener(saveOnFocus);
        etPriceCacheWrite.setOnFocusChangeListener(saveOnFocus);

        root.findViewById(R.id.btn_reset_defaults).setOnClickListener(v -> {
            profile.inputPrice = 0;
            profile.outputPrice = 0;
            profile.cacheReadPrice = 0;
            profile.cacheWritePrice = 0;
            etPriceInput.setText("0");
            etPriceOutput.setText("0");
            etPriceCacheRead.setText("0");
            etPriceCacheWrite.setText("0");
            store.upsertProfile(profile);
            store.setActiveProfile(profile.id);
            View sbHost = getView();
            if (sbHost != null) {
                Snackbar.make(sbHost, "Reset to defaults", Snackbar.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /** Parse all four price fields and persist them to the active profile. */
    private void saveFromFields() {
        profile.inputPrice = parsePrice(etPriceInput);
        profile.outputPrice = parsePrice(etPriceOutput);
        profile.cacheReadPrice = parsePrice(etPriceCacheRead);
        profile.cacheWritePrice = parsePrice(etPriceCacheWrite);
        store.upsertProfile(profile);
        store.setActiveProfile(profile.id);
    }

    private static double parsePrice(TextInputEditText et) {
        if (et.getText() == null) return 0;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            // Use US locale to parse "0.5" regardless of the device's locale
            // (some locales use comma as the decimal separator, which
            // Double.parseDouble rejects).
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatPrice(double v) {
        if (v == 0) return "0";
        // Up to 4 decimal places, no trailing zeros.
        return String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
