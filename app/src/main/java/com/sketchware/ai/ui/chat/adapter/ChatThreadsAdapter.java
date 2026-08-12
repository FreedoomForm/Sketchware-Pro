package com.sketchware.ai.ui.chat.adapter;

import android.annotation.SuppressLint;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pro.sketchware.R;
import com.sketchware.ai.context.TaskHistoryStore;
import com.sketchware.ai.ui.settings.ProviderIconResolver;

/**
 * Adapter for the past-conversations list shown in the chat side drawer.
 * Backed by {@link TaskHistoryStore.TaskMetadata} items.
 *
 * <p>Each row shows:
 * <ul>
 *   <li>{@code thread_title} — the first user message of the task (truncated).</li>
 *   <li>{@code thread_subtitle} — relative timestamp + message count.</li>
 *   <li>{@code btn_thread_more} — overflow icon (rename / delete).</li>
 * </ul>
 *
 * <p>Click → {@link Callback#onOpen(TaskHistoryStore.TaskMetadata)}.
 * <br>Long-press or overflow icon → {@link Callback#onMore(TaskHistoryStore.TaskMetadata, View)}.
 */
public final class ChatThreadsAdapter extends
        ListAdapter<TaskHistoryStore.TaskMetadata, ChatThreadsAdapter.ThreadVH> {

    /** Host callback. */
    public interface Callback {
        void onOpen(TaskHistoryStore.TaskMetadata thread);
        void onMore(TaskHistoryStore.TaskMetadata thread, View anchor);
    }

    private final Callback callback;
    @Nullable
    private List<TaskHistoryStore.TaskMetadata> allItems = null;
    @Nullable
    private Set<String> pinnedIds = Collections.emptySet();

    public ChatThreadsAdapter(@NonNull Callback callback) {
        super(DIFF);
        this.callback = callback;
    }

    /**
     * Update the set of pinned (starred) thread ids. The adapter will show a
     * star icon on matching rows. Pass an empty set to clear all stars.
     */
    public void setPinnedIds(@Nullable Set<String> ids) {
        this.pinnedIds = ids == null ? Collections.emptySet() : new HashSet<>(ids);
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull @Override
    public ThreadVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ai_chat_thread_row, parent, false);
        return new ThreadVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadVH h, int position) {
        TaskHistoryStore.TaskMetadata item = getItem(position);
        String title = item.firstUserMessage;
        if (title == null || title.isEmpty()) title = "(no title)";
        h.title.setText(title);

        String subtitle = formatRelative(h.itemView.getContext(), item.updatedAt)
                + " · " + item.messageCount + " msgs";
        if (item.projectName != null && !item.projectName.isEmpty()) {
            subtitle = item.projectName + " · " + subtitle;
        }
        h.subtitle.setText(subtitle);

        // Show the provider/model emblem for this chat instead of a generic
        // bot icon. Resolve by provider id first, then by model id keyword
        // (e.g. "claude-3.5-sonnet" still maps to the Anthropic icon), and
        // fall back to the default bot icon for old chats that have no
        // provider/model recorded.
        if (h.icon != null) {
            int iconRes = 0;
            if (item.lastProviderId != null && !item.lastProviderId.isEmpty()) {
                iconRes = ProviderIconResolver.resolveProvider(item.lastProviderId, null);
            }
            if (iconRes == 0 || iconRes == R.drawable.ic_ai) {
                // Try the model id as a fallback (substring match).
                if (item.lastModelId != null && !item.lastModelId.isEmpty()) {
                    iconRes = ProviderIconResolver.resolveModel(item.lastModelId);
                }
            }
            if (iconRes != 0) {
                h.icon.setImageResource(iconRes);
                // Provider emblems are full-color PNGs; clear the tint so
                // they render in their brand colors instead of monochrome.
                h.icon.setImageTintList(null);
            } else {
                h.icon.setImageResource(R.drawable.kelivo_lucide_bot_message_square);
                h.icon.setImageTintList(androidx.core.content.ContextCompat
                        .getColorStateList(h.itemView.getContext(), R.color.ai_avatar_text));
            }
        }

        // Star indicator for pinned threads.
        if (h.pinnedStar != null) {
            boolean pinned = pinnedIds != null && item.id != null && pinnedIds.contains(item.id);
            h.pinnedStar.setVisibility(pinned ? View.VISIBLE : View.GONE);
        }

        h.itemView.setOnClickListener(v -> callback.onOpen(item));
        h.itemView.setOnLongClickListener(v -> {
            callback.onMore(item, v);
            return true;
        });
        h.more.setOnClickListener(v -> callback.onMore(item, v));
    }

    /**
     * Cache the full list so search-filtering can re-derive a filtered view
     * without re-querying the storage. Call this every time the drawer is
     * refreshed from {@link TaskHistoryStore}.
     */
    public void submitAll(@NonNull List<TaskHistoryStore.TaskMetadata> all) {
        this.allItems = new ArrayList<>(all);
        submitList(all);
    }

    /**
     * Filter the visible rows by a query string (case-insensitive substring
     * match on the first user message and project name).
     */
    @SuppressLint("NotifyDataSetChanged")
    public void filter(@Nullable String query) {
        if (allItems == null) return;
        if (query == null || query.trim().isEmpty()) {
            submitList(new ArrayList<>(allItems));
            return;
        }
        String q = query.trim().toLowerCase();
        List<TaskHistoryStore.TaskMetadata> out = new ArrayList<>();
        for (TaskHistoryStore.TaskMetadata m : allItems) {
            String t = m.firstUserMessage == null ? "" : m.firstUserMessage.toLowerCase();
            String p = m.projectName == null ? "" : m.projectName.toLowerCase();
            if (t.contains(q) || p.contains(q)) out.add(m);
        }
        submitList(out);
    }

    // ------------------------------------------------------------------

    static final class ThreadVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final ImageView more;
        final ImageView icon;
        final ImageView pinnedStar;
        ThreadVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.thread_title);
            subtitle = v.findViewById(R.id.thread_subtitle);
            more = v.findViewById(R.id.btn_thread_more);
            icon = v.findViewById(R.id.thread_icon);
            pinnedStar = v.findViewById(R.id.thread_pinned_star);
        }
    }

    private static String formatRelative(@NonNull android.content.Context ctx, long ts) {
        long now = System.currentTimeMillis();
        long diff = now - ts;
        if (diff < 60_000L) return "just now";
        if (diff < 3_600_000L) return (diff / 60_000L) + "m ago";
        if (diff < 86_400_000L) return (diff / 3_600_000L) + "h ago";
        if (diff < 7L * 86_400_000L) return (diff / 86_400_000L) + "d ago";
        return DateUtils.formatDateTime(ctx, ts,
                DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE);
    }

    private static final DiffUtil.ItemCallback<TaskHistoryStore.TaskMetadata> DIFF =
            new DiffUtil.ItemCallback<TaskHistoryStore.TaskMetadata>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskHistoryStore.TaskMetadata a,
                                               @NonNull TaskHistoryStore.TaskMetadata b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskHistoryStore.TaskMetadata a,
                                                  @NonNull TaskHistoryStore.TaskMetadata b) {
                    return a.id != null && a.id.equals(b.id)
                            && a.updatedAt == b.updatedAt
                            && a.messageCount == b.messageCount
                            && eq(a.firstUserMessage, b.firstUserMessage)
                            && eq(a.projectName, b.projectName);
                }

                private static boolean eq(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };
}
