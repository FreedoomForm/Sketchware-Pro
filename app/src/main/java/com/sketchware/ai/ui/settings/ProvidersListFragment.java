package com.sketchware.ai.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;
import com.sketchware.ai.llm.ProviderCatalog;
import com.sketchware.ai.llm.storage.ProviderConfigStore;

/**
 * Providers list fragment — a RecyclerView of every supported AI provider,
 * with search, status badges, and per-row model preview.
 *
 * <p>Replaces the old {@link ApiConfigurationFragment} as the default
 * landing screen of {@link AISettingsActivity}. Tapping a row opens
 * {@link ProviderDetailActivity} for that provider, where the user edits
 * API key / base URL / model list / etc.
 *
 * <p>Each row reflects the current state of the user's stored profile for
 * that provider:
 * <ul>
 *   <li>{@code ON} badge — the profile is configured (has the API key set,
 *       or the provider doesn't require one).</li>
 *   <li>{@code OFF} badge — the profile exists but the API key is missing.</li>
 *   <li>The model hint below the provider name shows the currently selected
 *       model id for that provider (or "Tap to configure" if unset).</li>
 * </ul>
 *
 * <p>This fragment is purely a launcher for {@link ProviderDetailActivity};
 * it never edits storage directly. The active provider/model used by the
 * chat is whatever profile was last marked active in
 * {@link ProviderConfigStore#setActiveProfile(String)}.
 */
public final class ProvidersListFragment extends Fragment {

    private ProviderConfigStore store;
    private ProvidersAdapter adapter;
    private EditText searchInput;
    private ImageView searchClear;
    private TextView emptyState;
    private List<ProviderRow> allRows = new ArrayList<>();

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProviderConfigStore(requireContext());
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.ai_providers_list, container, false);

        RecyclerView recycler = root.findViewById(R.id.providers_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProvidersAdapter();
        recycler.setAdapter(adapter);

        searchInput = root.findViewById(R.id.search_input);
        searchClear = root.findViewById(R.id.search_clear);
        emptyState = root.findViewById(R.id.empty_state);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
                searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        searchClear.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
        });

        loadData();
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        // Reload in case the user edited a provider detail and came back.
        loadData();
    }

    private void loadData() {
        allRows.clear();
        ProviderConfigStore.Profile active = store.getActiveProfile();
        String activeId = active != null ? active.providerId : null;

        for (ProviderCatalog.Entry entry : ProviderCatalog.all()) {
            // Find the user's stored profile for this provider, if any.
            ProviderConfigStore.Profile p = findProfileForProvider(entry.id);
            boolean configured = p != null && isConfigured(entry, p);
            String modelHint;
            if (p != null && p.modelId != null && !p.modelId.isEmpty()) {
                modelHint = p.modelId;
            } else if (!entry.defaultModel.isEmpty()) {
                modelHint = entry.defaultModel;
            } else {
                modelHint = configured ? "Tap to configure" : "Tap to configure";
            }
            boolean isActive = entry.id.equals(activeId);
            allRows.add(new ProviderRow(entry, configured, isActive, modelHint));
        }
        applyFilter(searchInput.getText() == null ? "" : searchInput.getText().toString());
    }

    private ProviderConfigStore.Profile findProfileForProvider(String providerId) {
        if (store == null) return null;
        try {
            List<ProviderConfigStore.Profile> profiles = store.getProfiles();
            if (profiles == null) return null;
            // First pass: exact providerId match.
            for (ProviderConfigStore.Profile p : profiles) {
                if (p != null && providerId.equals(p.providerId)) return p;
            }
            // Second pass: if no exact match, but this is the "openai-compat"
            // generic entry, accept any profile whose providerId isn't in the
            // catalog (i.e. a user-defined custom provider).
            if ("openai-compat".equals(providerId)) {
                for (ProviderConfigStore.Profile p : profiles) {
                    if (p != null && ProviderCatalog.get(p.providerId) == null) return p;
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private boolean isConfigured(ProviderCatalog.Entry entry, ProviderConfigStore.Profile p) {
        if (!entry.requiresApiKey) return true;
        return p.apiKey != null && !p.apiKey.isEmpty();
    }

    private void applyFilter(String query) {
        List<ProviderRow> filtered = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ProviderRow r : allRows) {
            if (q.isEmpty()
                    || r.entry.id.toLowerCase(Locale.ROOT).contains(q)
                    || r.entry.displayName.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(r);
            }
        }
        adapter.submitList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------
    // Adapter
    // ------------------------------------------------------------------

    private final class ProvidersAdapter extends RecyclerView.Adapter<ProvidersAdapter.RowVH> {
        private final List<ProviderRow> items = new ArrayList<>();

        void submitList(List<ProviderRow> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public RowVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ai_provider_row, parent, false);
            return new RowVH(v);
        }

        @Override public void onBindViewHolder(@NonNull RowVH h, int position) {
            ProviderRow r = items.get(position);
            h.bind(r, position == items.size() - 1);
        }

        @Override public int getItemCount() {
            return items.size();
        }

        final class RowVH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView modelHint;
            final TextView badge;
            final View divider;

            RowVH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.provider_icon);
                name = v.findViewById(R.id.provider_name);
                modelHint = v.findViewById(R.id.provider_model_hint);
                badge = v.findViewById(R.id.provider_status_badge);
                divider = v.findViewById(R.id.row_divider);
            }

            void bind(ProviderRow r, boolean isLast) {
                int iconRes = ProviderIconResolver.resolveProvider(r.entry.id, r.entry.displayName);
                icon.setImageResource(iconRes);
                name.setText(r.entry.displayName);
                modelHint.setText(r.modelHint);

                boolean on = r.configured;
                badge.setSelected(on);
                badge.setText(on
                        ? getText(R.string.ai_providers_status_on)
                        : getText(R.string.ai_providers_status_off));
                badge.setTextColor(getResources().getColor(on
                        ? R.color.ai_provider_status_on_text
                        : R.color.ai_provider_status_off_text));

                divider.setVisibility(isLast ? View.GONE : View.VISIBLE);

                itemView.setOnClickListener(v -> {
                    ProviderDetailActivity.start(requireContext(), r.entry.id, r.entry.displayName);
                });
            }
        }
    }

    // ------------------------------------------------------------------
    // Row model
    // ------------------------------------------------------------------

    private static final class ProviderRow {
        final ProviderCatalog.Entry entry;
        final boolean configured;
        final boolean active;
        final String modelHint;

        ProviderRow(ProviderCatalog.Entry entry, boolean configured, boolean active, String modelHint) {
            this.entry = entry;
            this.configured = configured;
            this.active = active;
            this.modelHint = modelHint;
        }
    }
}
