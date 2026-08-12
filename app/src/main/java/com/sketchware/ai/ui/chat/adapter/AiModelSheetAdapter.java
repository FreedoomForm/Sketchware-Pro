package com.sketchware.ai.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;
import com.sketchware.ai.ui.settings.ProviderIconResolver;

/**
 * RecyclerView adapter for the model-picker bottom sheet
 * ({@link com.sketchware.ai.ui.chat.sheet.AiModelPickerSheet}).
 *
 * <p>Ported from {@code KelivoModelSheetAdapter} in
 * FabioSilva11/Sketchware-IA, but uses our {@link ProviderIconResolver}
 * for icon resolution so all 17 catalog providers (plus user-supplied
 * custom provider ids) resolve correctly.
 *
 * <p>Two view types:
 * <ul>
 *   <li>{@link #TYPE_HEADER} — section header: provider icon + display name.</li>
 *   <li>{@link #TYPE_MODEL} — single model row: avatar, name, capability
 *       tags (chat / text→image→text / tools), favorite heart toggle,
 *       and a soft accent background when selected.</li>
 * </ul>
 *
 * <p>The Favorites section uses the synthetic provider id {@code "favorites"}
 * so {@link #findProviderSectionPosition(String)} can scroll to it.
 */
public final class AiModelSheetAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Section header (provider name). */
    public static final int TYPE_HEADER = 0;
    /** Single model row. */
    public static final int TYPE_MODEL = 1;

    /** Synthetic provider id used for the favorites section. */
    public static final String PROVIDER_FAVORITES = "favorites";

    /**
     * Immutable row descriptor. Use the {@code Header} constructor for
     * section headers and the {@code Model} constructor for model rows.
     */
    public static final class Row {
        public final int type;
        public final String providerId;
        public final String providerLabel;
        public final String modelId;
        public final boolean selected;
        public final boolean pinned;

        public Row(String providerId, String providerLabel) {
            this.type = TYPE_HEADER;
            this.providerId = providerId == null ? "" : providerId;
            this.providerLabel = providerLabel == null ? "" : providerLabel;
            this.modelId = "";
            this.selected = false;
            this.pinned = false;
        }

        public Row(String providerId, String providerLabel, String modelId,
                   boolean selected, boolean pinned) {
            this.type = TYPE_MODEL;
            this.providerId = providerId == null ? "" : providerId;
            this.providerLabel = providerLabel == null ? "" : providerLabel;
            this.modelId = modelId == null ? "" : modelId;
            this.selected = selected;
            this.pinned = pinned;
        }
    }

    /** Host callback for row taps and favorite toggles. */
    public interface Listener {
        void onModelSelected(String providerId, String modelId);
        void onFavoriteToggle(String providerId, String modelId);
    }

    private final List<Row> rows = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Replace all rows; notifies adapter. */
    public void submit(List<Row> items) {
        rows.clear();
        if (items != null) rows.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * Find the position of a section header by provider id (e.g.
     * {@code "openai"}, {@code "anthropic"}, or {@link #PROVIDER_FAVORITES}).
     * Falls back to the first model row of that provider if no header exists.
     * Returns -1 if not found.
     */
    public int findProviderSectionPosition(String providerId) {
        if (providerId == null) return -1;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.type == TYPE_HEADER && providerId.equals(r.providerId)) {
                return i;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.type == TYPE_MODEL && providerId.equals(r.providerId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(
                    inflater.inflate(R.layout.ai_model_section_header, parent, false));
        }
        return new ModelHolder(
                inflater.inflate(R.layout.ai_model_sheet_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            bindHeader((HeaderHolder) holder, row);
        } else if (holder instanceof ModelHolder) {
            bindModel((ModelHolder) holder, row);
        }
    }

    private void bindHeader(HeaderHolder h, Row row) {
        h.title.setText(row.providerLabel);
        int iconRes = ProviderIconResolver.resolveProvider(
                row.providerId, row.providerLabel);
        if (iconRes != 0 && !PROVIDER_FAVORITES.equals(row.providerId)) {
            h.icon.setVisibility(View.VISIBLE);
            h.icon.setImageResource(iconRes);
        } else {
            h.icon.setVisibility(View.GONE);
        }
    }

    private void bindModel(ModelHolder h, Row row) {
        h.name.setText(row.modelId);

        // Model-specific icon (resolves "claude-*" → Anthropic, etc.).
        int iconRes = ProviderIconResolver.resolveModel(row.modelId);
        if (iconRes != 0) {
            h.icon.setVisibility(View.VISIBLE);
            h.icon.setImageResource(iconRes);
            h.avatar.setVisibility(View.GONE);
        } else {
            h.icon.setVisibility(View.GONE);
            h.avatar.setVisibility(View.VISIBLE);
        }

        // Image-input indicator (vision models).
        h.inputImageIcon.setVisibility(
                supportsImageInput(row.providerId, row.modelId)
                        ? View.VISIBLE : View.GONE);

        // Selected highlight on the whole row.
        h.itemView.setBackgroundResource(row.selected
                ? R.drawable.ai_model_selected_bg
                : android.R.color.transparent);

        // Favorite heart.
        h.favorite.setImageResource(row.pinned
                ? R.drawable.ic_heart_filled
                : R.drawable.ic_heart_outline);

        // Row tap → select model.
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModelSelected(row.providerId, row.modelId);
            }
        });

        // Heart tap → toggle favorite (don't trigger row tap).
        h.favorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteToggle(row.providerId, row.modelId);
            }
        });
    }

    /**
     * Heuristic: does this provider+model combination support image input?
     * Mirrors {@code KelivoModelSheetAdapter.supportsImageInput} from the
     * reference repo. Used purely for the small image icon in the IO tag.
     */
    private static boolean supportsImageInput(String providerId, String modelId) {
        String provider = providerId == null ? "" : providerId.toLowerCase(java.util.Locale.US);
        String key = ((providerId == null ? "" : providerId)
                + " "
                + (modelId == null ? "" : modelId)).toLowerCase(java.util.Locale.US);
        // DeepSeek currently doesn't expose vision models.
        if ("deepseek".equals(provider)) return false;
        return key.contains("claude")
                || key.contains("gemini")
                || key.contains("vision")
                || key.contains("gpt-4o")
                || key.contains("gpt-4.1")
                || key.contains("o3")
                || key.contains("o4")
                || key.contains("qwen-vl")
                || key.contains("qwen2-vl")
                || key.contains("qwen2.5-vl")
                || key.contains("qvq")
                || key.contains("glm-4v")
                || key.contains("pixtral")
                || key.contains("llava")
                || key.contains("minicpm-v")
                || key.contains("grok-vision")
                || key.contains("grok-2-vision");
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.provider_header_icon);
            title = itemView.findViewById(R.id.provider_header_title);
        }
    }

    static final class ModelHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final ImageView avatar;
        final TextView name;
        final ImageView inputImageIcon;
        final ImageView favorite;

        ModelHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.model_icon);
            avatar = itemView.findViewById(R.id.model_avatar);
            name = itemView.findViewById(R.id.model_name);
            inputImageIcon = itemView.findViewById(R.id.model_input_image_icon);
            favorite = itemView.findViewById(R.id.model_favorite);
        }
    }
}
