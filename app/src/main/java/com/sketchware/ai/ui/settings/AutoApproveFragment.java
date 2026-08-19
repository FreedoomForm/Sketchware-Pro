package com.sketchware.ai.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

/**
 * Auto-Approve settings fragment. Mirrors Kilo Code's Auto-Approve page.
 *
 * <p>The switches persist to a private {@link SharedPreferences} file
 * ({@code sketchware_ai_auto_approve}) using domain-prefixed keys so the
 * PermissionGate can later read them without colliding with the per-tool
 * auto-approve map in {@link com.sketchware.ai.llm.storage.ProviderConfigStore}.
 *
 * <p>"Max requests per run" maps to {@link com.sketchware.ai.agent.AgentRuntime#setMaxIterations(int)}.
 * ChatFragment reads this preference when constructing the agent.
 */
public final class AutoApproveFragment extends Fragment {

    public static final String PREFS_NAME = "sketchware_ai_auto_approve";
    public static final String KEY_YOLO = "yolo_mode";
    public static final String KEY_DOMAIN_PREFIX = "domain_";
    public static final String KEY_MAX_ITERATIONS = "max_iterations";
    public static final int DEFAULT_MAX_ITERATIONS = 50;

    /** Domain IDs matching the layout's switch IDs, in display order. */
    private static final int[] DOMAIN_SWITCH_IDS = {
            R.id.sw_aa_view,
            R.id.sw_aa_event,
            R.id.sw_aa_block,
            R.id.sw_aa_component,
            R.id.sw_aa_project,
            R.id.sw_aa_resource,
            R.id.sw_aa_java,
            R.id.sw_aa_library,
            R.id.sw_aa_manifest,
            R.id.sw_aa_build,
            R.id.sw_aa_export,
    };
    /** Domain key suffixes — must stay in the same order as DOMAIN_SWITCH_IDS. */
    private static final String[] DOMAIN_KEYS = {
            "view", "event", "block", "component", "project",
            "resource", "java", "library", "manifest", "build", "export",
    };

    private SharedPreferences prefs;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_auto_approve, container, false);

        // YOLO mode master switch.
        MaterialSwitch swYolo = root.findViewById(R.id.sw_auto_approve_main);
        swYolo.setChecked(prefs.getBoolean(KEY_YOLO, false));
        swYolo.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(KEY_YOLO, checked).apply();
            showSaved();
        });

        // Per-domain auto-approve switches.
        for (int i = 0; i < DOMAIN_SWITCH_IDS.length; i++) {
            MaterialSwitch sw = root.findViewById(DOMAIN_SWITCH_IDS[i]);
            // Default to false (require approval) for every domain.
            sw.setChecked(prefs.getBoolean(KEY_DOMAIN_PREFIX + DOMAIN_KEYS[i], false));
            final String key = KEY_DOMAIN_PREFIX + DOMAIN_KEYS[i];
            sw.setOnCheckedChangeListener((b, checked) -> {
                prefs.edit().putBoolean(key, checked).apply();
                showSaved();
            });
        }

        // Max requests per run.
        TextInputEditText etMax = root.findViewById(R.id.et_max_requests);
        int current = prefs.getInt(KEY_MAX_ITERATIONS, DEFAULT_MAX_ITERATIONS);
        etMax.setText(String.valueOf(current));
        etMax.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) return;
            String s = etMax.getText() == null ? "" : etMax.getText().toString().trim();
            int val;
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                Snackbar.make(root, "Max requests must be a number", Snackbar.LENGTH_SHORT).show();
                etMax.setText(String.valueOf(current));
                return;
            }
            if (val < 1) val = 1;
            if (val > 500) val = 500;
            prefs.edit().putInt(KEY_MAX_ITERATIONS, val).apply();
            etMax.setText(String.valueOf(val));
            showSaved();
        });

        return root;
    }

    private void showSaved() {
        View v = getView();
        if (v != null) {
            Snackbar.make(v, "Saved", Snackbar.LENGTH_SHORT).show();
        }
    }
}
