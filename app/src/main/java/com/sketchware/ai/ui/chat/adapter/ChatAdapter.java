package com.sketchware.ai.ui.chat.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sketchware.ai.llm.ProviderCatalog;
import com.sketchware.ai.ui.chat.ChatMessage;
import com.sketchware.ai.ui.settings.ProviderIconResolver;
import pro.sketchware.R;

/**
 * RecyclerView adapter for the chat. Renders different row types based on
 * {@link ChatMessage#type}. Mirrors Cline's {@code ChatRow.tsx} dispatch.
 *
 * <p>The TextHolder now binds a provider avatar (icon) and a "copy" action
 * button on each assistant message — inspired by
 * FabioSilva11/Sketchware-IA's item_message_bot.xml.
 */
public final class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_TEXT = 2;
    private static final int TYPE_REASONING = 3;
    private static final int TYPE_TOOL_CALL = 4;
    private static final int TYPE_TOOL_RESULT = 5;
    private static final int TYPE_ERROR = 6;
    private static final int TYPE_COMPLETION = 7;
    private static final int TYPE_API_REQ = 8;
    private static final int TYPE_COMPACTION = 9;

    /**
     * Provider id of the active profile, used to resolve the bot avatar
     * icon for assistant messages. Updated by {@link ChatFragment} on
     * resume / profile change.
     */
    private String providerId = "";

    public void setProviderId(String providerId) {
        this.providerId = providerId == null ? "" : providerId;
        notifyDataSetChanged();
    }

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            /**
             * Two messages represent the same logical row if they share the
             * same {@code ts} (creation timestamp, which is unique per
             * ChatMessage instance). This lets DiffUtil distinguish "row
             * changed" from "row added/removed" even when the surrounding
             * list is rebuilt.
             *
             * <p>Previously this used {@code o == n} (reference equality),
             * which combined with {@code MessageReducer.getMessages()}
             * returning the same live list reference caused ListAdapter's
             * {@code submitList} to short-circuit and never dispatch any
             * update — the chat UI never visually updated during streaming.
             */
            @Override public boolean areItemsTheSame(@NonNull ChatMessage o, @NonNull ChatMessage n) {
                return o.ts == n.ts;
            }
            /**
             * Always return {@code false} so that any in-place mutation of
             * a {@link ChatMessage} (e.g. streaming text appended by
             * {@code MessageReducer.appendText}) triggers a rebind of the
             * corresponding row. Because the reducer mutates the same
             * ChatMessage instance and then publishes a shallow copy of the
             * list, the old and new list slots point at the SAME instance —
             * value-equality comparison would therefore return {@code true}
             * and suppress the update. Forcing {@code false} is correct and
             * cheap: RecyclerView only rebinds visible holders (~5–10).
             */
            @Override public boolean areContentsTheSame(@NonNull ChatMessage o, @NonNull ChatMessage n) {
                return false;
            }
        });
    }

    @Override public int getItemViewType(int position) {
        ChatMessage m = getItem(position);
        switch (m.type) {
            case ChatMessage.TYPE_USER:        return TYPE_USER;
            case ChatMessage.TYPE_TEXT:         return TYPE_TEXT;
            case ChatMessage.TYPE_REASONING:    return TYPE_REASONING;
            case ChatMessage.TYPE_TOOL_CALL:    return TYPE_TOOL_CALL;
            case ChatMessage.TYPE_TOOL_RESULT:  return TYPE_TOOL_RESULT;
            case ChatMessage.TYPE_ERROR:        return TYPE_ERROR;
            case ChatMessage.TYPE_COMPLETION:   return TYPE_COMPLETION;
            case ChatMessage.TYPE_API_REQ_START:
            case ChatMessage.TYPE_API_REQ_DONE: return TYPE_API_REQ;
            case ChatMessage.TYPE_COMPACTION:   return TYPE_COMPACTION;
            default: return TYPE_TEXT;
        }
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER: return new UserHolder(inf.inflate(R.layout.ai_row_user, parent, false));
            case TYPE_TEXT: return new TextHolder(inf.inflate(R.layout.ai_row_text, parent, false));
            case TYPE_REASONING: return new ReasoningHolder(inf.inflate(R.layout.ai_row_reasoning, parent, false));
            case TYPE_TOOL_CALL:
            case TYPE_TOOL_RESULT:
                return new ToolHolder(inf.inflate(R.layout.ai_row_tool, parent, false));
            case TYPE_ERROR: return new ErrorHolder(inf.inflate(R.layout.ai_row_error, parent, false));
            case TYPE_COMPLETION: return new CompletionHolder(inf.inflate(R.layout.ai_row_completion, parent, false));
            case TYPE_API_REQ: return new TextHolder(inf.inflate(R.layout.ai_row_text, parent, false));
            case TYPE_COMPACTION: return new TextHolder(inf.inflate(R.layout.ai_row_text, parent, false));
            default: return new TextHolder(inf.inflate(R.layout.ai_row_text, parent, false));
        }
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        ChatMessage m = getItem(position);
        if (h instanceof UserHolder) {
            ((UserHolder) h).bind(m);
        } else if (h instanceof TextHolder) {
            ((TextHolder) h).bind(m, providerId);
        } else if (h instanceof ReasoningHolder) {
            ((ReasoningHolder) h).bind(m);
        } else if (h instanceof ToolHolder) {
            ((ToolHolder) h).bind(m);
        } else if (h instanceof ErrorHolder) {
            ((ErrorHolder) h).bind(m);
        } else if (h instanceof CompletionHolder) {
            ((CompletionHolder) h).bind(m);
        }
    }

    // --- ViewHolders ---

    static final class UserHolder extends RecyclerView.ViewHolder {
        final TextView text;
        UserHolder(@NonNull View v) { super(v); text = v.findViewById(R.id.text); }
        void bind(ChatMessage m) {
            text.setText(m.text == null ? "" : m.text);
        }
    }

    static final class TextHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageView avatarIcon;
        final View avatarContainer;
        final LinearLayout messageActions;
        final View actionCopy;
        final TextView tokenCount;
        final com.sketchware.ai.ui.chat.TypingDotsView streamingDots;
        TextHolder(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.text);
            avatarIcon = v.findViewById(R.id.avatar_icon);
            avatarContainer = v.findViewById(R.id.avatar_container);
            messageActions = v.findViewById(R.id.message_actions);
            actionCopy = v.findViewById(R.id.action_copy);
            tokenCount = v.findViewById(R.id.token_count);
            streamingDots = v.findViewById(R.id.streaming_dots);
        }
        void bind(ChatMessage m, String providerId) {
            // API req info row takes precedence over generic text rendering.
            if (ChatMessage.TYPE_API_REQ_DONE.equals(m.type)) {
                text.setText("API: in=" + m.inputTokens + " out=" + m.outputTokens + " cost=$" + String.format("%.4f", m.cost));
                if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
                if (messageActions != null) messageActions.setVisibility(View.GONE);
                if (streamingDots != null) streamingDots.setVisibility(View.GONE);
                return;
            } else if (ChatMessage.TYPE_API_REQ_START.equals(m.type)) {
                text.setText("Calling API...");
                if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
                if (messageActions != null) messageActions.setVisibility(View.GONE);
                if (streamingDots != null) streamingDots.setVisibility(View.GONE);
                return;
            } else if (ChatMessage.TYPE_COMPACTION.equals(m.type)) {
                text.setText("Conversation compacted to fit context window.");
                if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
                if (messageActions != null) messageActions.setVisibility(View.GONE);
                if (streamingDots != null) streamingDots.setVisibility(View.GONE);
                return;
            }
            // Real assistant text row — show avatar + actions.
            if (avatarContainer != null) avatarContainer.setVisibility(View.VISIBLE);
            if (avatarIcon != null) {
                int iconRes = ProviderIconResolver.resolveProvider(providerId,
                        ProviderCatalog.safeDisplayName(providerId));
                avatarIcon.setImageResource(iconRes);
            }
            String t = m.text == null ? "" : m.text;
            // Show typing dots while streaming AND no text yet; once text
            // starts arriving, hide the dots and show the typewriter cursor.
            if (streamingDots != null) {
                streamingDots.setVisibility(m.isStreaming && t.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (m.isStreaming) t = t + "▋"; // typewriter cursor
            text.setText(t);

            // Show the copy action on non-streaming assistant messages.
            if (messageActions != null) {
                boolean showActions = !m.isStreaming && t.length() > 0
                        && !ChatMessage.TYPE_API_REQ_DONE.equals(m.type)
                        && !ChatMessage.TYPE_API_REQ_START.equals(m.type)
                        && !ChatMessage.TYPE_COMPACTION.equals(m.type);
                messageActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
            }
            if (actionCopy != null) {
                actionCopy.setOnClickListener(v -> {
                    Context ctx = v.getContext();
                    try {
                        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("AI response", m.text));
                            Toast.makeText(ctx, R.string.ai_chat_action_copy_done, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Throwable ignored) { }
                });
            }
            if (tokenCount != null) {
                if (m.inputTokens > 0 || m.outputTokens > 0) {
                    tokenCount.setVisibility(View.VISIBLE);
                    tokenCount.setText((m.inputTokens + m.outputTokens) + " tokens");
                } else {
                    tokenCount.setVisibility(View.GONE);
                }
            }
        }
    }

    static final class ReasoningHolder extends RecyclerView.ViewHolder {
        final TextView text;
        ReasoningHolder(@NonNull View v) { super(v); text = v.findViewById(R.id.text); }
        void bind(ChatMessage m) {
            if (m.text == null || m.text.isEmpty()) {
                text.setVisibility(View.GONE);
            } else {
                text.setVisibility(View.VISIBLE);
                text.setText(m.text + (m.isStreaming ? "▋" : ""));
            }
        }
    }

    static final class ToolHolder extends RecyclerView.ViewHolder {
        final TextView toolName;
        final TextView toolArgs;
        final TextView toolResult;
        final ProgressBar progress;
        ToolHolder(@NonNull View v) {
            super(v);
            toolName = v.findViewById(R.id.tool_name);
            toolArgs = v.findViewById(R.id.tool_args);
            toolResult = v.findViewById(R.id.tool_result);
            progress = v.findViewById(R.id.progress);
        }
        void bind(ChatMessage m) {
            toolName.setText(m.toolName == null ? "" : m.toolName);
            toolArgs.setText(m.toolArgsJson == null ? "" : m.toolArgsJson);
            // Treat an empty-string result the same as null: the tool is still
            // in-flight (or returned nothing useful) and the progress spinner
            // should keep spinning until a non-empty result arrives.
            if (m.toolResult != null && !m.toolResult.isEmpty()) {
                toolResult.setVisibility(View.VISIBLE);
                toolResult.setText((m.isError ? "ERROR: " : "OK: ") + m.toolResult);
                progress.setVisibility(View.GONE);
            } else {
                toolResult.setVisibility(View.GONE);
                // Only show the spinner for tool_call rows (in-flight). Tool
                // _result rows with empty output should not spin forever.
                progress.setVisibility(ChatMessage.TYPE_TOOL_CALL.equals(m.type) ? View.VISIBLE : View.GONE);
            }
        }
    }

    static final class ErrorHolder extends RecyclerView.ViewHolder {
        final TextView text;
        ErrorHolder(@NonNull View v) { super(v); text = v.findViewById(R.id.text); }
        void bind(ChatMessage m) {
            text.setText(m.text == null ? "Unknown error" : m.text);
        }
    }

    static final class CompletionHolder extends RecyclerView.ViewHolder {
        final TextView text;
        CompletionHolder(@NonNull View v) { super(v); text = v.findViewById(R.id.text); }
        void bind(ChatMessage m) {
            text.setText(m.text == null ? "Done" : m.text);
        }
    }
}
