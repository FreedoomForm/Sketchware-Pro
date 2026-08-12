package com.sketchware.ai.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import pro.sketchware.R;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

import java.util.Locale;

/** Advanced settings: pricing fields, compaction strategy, reset to defaults. */
public final class AdvancedSettingsFragment extends Fragment {

    private ProviderConfigStore store;
    private ProviderConfigStore.Profile profile;

    private TextInputEditText etPriceInput;
    private TextInputEditText etPriceOutput;
    private TextInputEditText etPriceCacheRead;
    private TextInputEditText etPriceCacheWrite;
    private AutoCompleteTextView etCompactionStrategy;
    private TextView tvCompactionDescription;

    /** Display labels for the compaction strategy dropdown. */
    private String[] strategyLabels;
    /** Internal values for the compaction strategy dropdown. Index-aligned
     *  with {@link #strategyLabels}. */
    private String[] strategyValues;
    /** Long-form descriptions shown below the dropdown when each strategy
     *  is selected, so the user understands the trade-offs. */
    private String[] strategyDescriptions;

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
        etCompactionStrategy = root.findViewById(R.id.et_compaction_strategy);
        tvCompactionDescription = root.findViewById(R.id.tv_compaction_description);

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

        // Compaction strategy dropdown.
        setupCompactionDropdown();

        root.findViewById(R.id.btn_reset_defaults).setOnClickListener(v -> {
            profile.inputPrice = 0;
            profile.outputPrice = 0;
            profile.cacheReadPrice = 0;
            profile.cacheWritePrice = 0;
            profile.compactionStrategy = "auto";
            etPriceInput.setText("0");
            etPriceOutput.setText("0");
            etPriceCacheRead.setText("0");
            etPriceCacheWrite.setText("0");
            selectCompactionStrategy("auto");
            store.upsertProfile(profile);
            store.setActiveProfile(profile.id);
            View sbHost = getView();
            if (sbHost != null) {
                Snackbar.make(sbHost, "Reset to defaults", Snackbar.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /** Build the dropdown adapter, restore the saved selection, and wire
     *  the item-click listener to persist the new value. */
    private void setupCompactionDropdown() {
        strategyLabels = getResources().getStringArray(R.array.compaction_strategy_labels);
        strategyValues = getResources().getStringArray(R.array.compaction_strategy_values);
        strategyDescriptions = new String[] {
            "Pick the best strategy for the model: SnapCompact for vision models, Context-full for reasoning models, Shake for everything else.",
            "Render discarded history into dense PNG frames of pixel-font glyphs that vision LLMs read back directly. No LLM call during compaction; fully local. Requires a vision-capable model (e.g. GPT-4o, Claude, GLM-4V).",
            "Send the older portion to a summarizer LLM with a structured prompt (Goal / Progress / Key Decisions / Next Steps / ...) and replace it with the structured summary. Costs an extra API call per compaction.",
            "Mechanical strategy, no LLM call. Replaces heavy tool results older than the 16K-token protected window with placeholders and drops old reasoning blocks. Safe for overflow recovery.",
            "Legacy LLM-summarizer strategy with a simpler prompt. Retained for debugging — prefer Context-full for new sessions."
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                strategyLabels);
        etCompactionStrategy.setAdapter(adapter);

        // Restore the saved strategy (default to "auto" on null/unknown).
        String current = profile.compactionStrategy;
        if (current == null || current.isEmpty()) current = "auto";
        selectCompactionStrategy(current);

        etCompactionStrategy.setOnItemClickListener((parent, view, position, id) -> {
            String value = strategyValues[position];
            profile.compactionStrategy = value;
            tvCompactionDescription.setText(strategyDescriptions[position]);
            store.upsertProfile(profile);
            store.setActiveProfile(profile.id);
            View sbHost = getView();
            if (sbHost != null) {
                Snackbar.make(sbHost, "Compaction: " + strategyLabels[position], Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    /** Set the dropdown's displayed text to the label for the given internal
     *  value, and update the description below. No-op if the value is unknown. */
    private void selectCompactionStrategy(String value) {
        for (int i = 0; i < strategyValues.length; i++) {
            if (strategyValues[i].equals(value)) {
                etCompactionStrategy.setText(strategyLabels[i], false);
                tvCompactionDescription.setText(strategyDescriptions[i]);
                return;
            }
        }
        // Unknown value — fall back to "auto".
        etCompactionStrategy.setText(strategyLabels[0], false);
        tvCompactionDescription.setText(strategyDescriptions[0]);
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
