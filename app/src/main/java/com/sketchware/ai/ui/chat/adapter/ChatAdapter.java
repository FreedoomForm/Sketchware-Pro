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
 * <p>Layouts ported from FabioSilva11/Sketchware-IA:
 * <ul>
 *   <li>{@code ai_row_text.xml}   — assistant message (avatar, sender name, time,
 *       status chip, reasoning block, streaming dots, message text, action row
 *       with copy/refresh/edit/speak/more, token count).</li>
 *   <li>{@code ai_row_user.xml}   — user message (sender name, time, avatar,
 *       bubble with optional image preview, action row).</li>
 *   <li>{@code ai_row_tool.xml}   — collapsible tool-call card (header, args,
 *       result, optional approval buttons).</li>
 * </ul>
 *
 * <p>All optional views are looked up lazily and bound defensively (null-safe)
 * so that older layouts without the new fields keep working.
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
            @Override public boolean areItemsTheSame(@NonNull ChatMessage o, @NonNull ChatMessage n) {
                return o.ts == n.ts;
            }
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
        final TextView senderName;
        final TextView time;
        final LinearLayout messageActions;
        final View actionCopy;
        UserHolder(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.text);
            senderName = v.findViewById(R.id.sender_name);
            time = v.findViewById(R.id.time);
            messageActions = v.findViewById(R.id.message_actions);
            actionCopy = v.findViewById(R.id.action_copy);
        }
        void bind(ChatMessage m) {
            text.setText(m.text == null ? "" : m.text);
            // Optional fields — null-safe
            if (senderName != null) senderName.setText("You");
            if (time != null) time.setVisibility(View.GONE);
            // Show copy action on user messages too (handy for re-running prompts)
            if (messageActions != null) {
                messageActions.setVisibility(m.text == null || m.text.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (actionCopy != null) {
                actionCopy.setOnClickListener(v -> {
                    Context ctx = v.getContext();
                    try {
                        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("AI prompt", m.text));
                            Toast.makeText(ctx, R.string.ai_chat_action_copy_done, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Throwable ignored) { }
                });
            }
        }
    }

    static final class TextHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageView avatarIcon;
        final View avatarContainer;
        final TextView avatarText;
        final TextView senderName;
        final TextView time;
        final TextView statusChip;
        final LinearLayout messageActions;
        final View actionCopy;
        final TextView tokenCount;
        final com.sketchware.ai.ui.chat.TypingDotsView streamingDots;
        TextHolder(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.text);
            avatarIcon = v.findViewById(R.id.avatar_icon);
            avatarContainer = v.findViewById(R.id.avatar_container);
            avatarText = v.findViewById(R.id.avatar_text);
            senderName = v.findViewById(R.id.sender_name);
            time = v.findViewById(R.id.time);
            statusChip = v.findViewById(R.id.status_chip);
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
                if (senderName != null) senderName.setVisibility(View.GONE);
                if (time != null) time.setVisibility(View.GONE);
                if (statusChip != null) statusChip.setVisibility(View.GONE);
                return;
            } else if (ChatMessage.TYPE_API_REQ_START.equals(m.type)) {
                text.setText("Calling API...");
                if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
                if (messageActions != null) messageActions.setVisibility(View.GONE);
                if (streamingDots != null) streamingDots.setVisibility(View.GONE);
                if (senderName != null) senderName.setVisibility(View.GONE);
                if (time != null) time.setVisibility(View.GONE);
                if (statusChip != null) statusChip.setVisibility(View.GONE);
                return;
            } else if (ChatMessage.TYPE_COMPACTION.equals(m.type)) {
                text.setText("Conversation compacted to fit context window.");
                if (avatarContainer != null) avatarContainer.setVisibility(View.GONE);
                if (messageActions != null) messageActions.setVisibility(View.GONE);
                if (streamingDots != null) streamingDots.setVisibility(View.GONE);
                if (senderName != null) senderName.setVisibility(View.GONE);
                if (time != null) time.setVisibility(View.GONE);
                if (statusChip != null) statusChip.setVisibility(View.GONE);
                return;
            }
            // Real assistant text row — show avatar + actions.
            if (avatarContainer != null) avatarContainer.setVisibility(View.VISIBLE);
            // Resolve provider display name & icon
            String displayName = ProviderCatalog.safeDisplayName(providerId);
            if (senderName != null) {
                senderName.setVisibility(View.VISIBLE);
                senderName.setText(displayName);
            }
            if (time != null) {
                time.setVisibility(View.VISIBLE);
                time.setText(formatTime(m.ts));
            }
            if (avatarIcon != null) {
                int iconRes = ProviderIconResolver.resolveProvider(providerId, displayName);
                avatarIcon.setImageResource(iconRes);
                avatarIcon.setVisibility(View.VISIBLE);
                if (avatarText != null) avatarText.setVisibility(View.GONE);
            }
            // Status chip
            if (statusChip != null) {
                if (m.isStreaming) {
                    statusChip.setVisibility(View.VISIBLE);
                    statusChip.setText(R.string.ai_chat_status_chip_thinking);
                    statusChip.setTextColor(itemView.getContext().getResources().getColor(R.color.ai_status_chip_thinking_text));
                } else {
                    statusChip.setVisibility(View.GONE);
                }
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

        private static String formatTime(long ts) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                return sdf.format(new java.util.Date(ts));
            } catch (Throwable t) {
                return "";
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
        final View layoutToolDetails;
        final ImageView imgExpand;
        final TextView textToolStatus;
        final ImageView imgToolStatus;
        final View approvalRow;
        final View btnApprove;
        final View btnReject;
        final ImageView toolStatusIcon;
        private boolean expanded = false;

        ToolHolder(@NonNull View v) {
            super(v);
            toolName = v.findViewById(R.id.tool_name);
            toolArgs = v.findViewById(R.id.tool_args);
            toolResult = v.findViewById(R.id.tool_result);
            progress = v.findViewById(R.id.progress);
            // New optional fields — null-safe
            layoutToolDetails = v.findViewById(R.id.layout_tool_details);
            imgExpand = v.findViewById(R.id.img_expand);
            textToolStatus = v.findViewById(R.id.text_tool_status);
            imgToolStatus = v.findViewById(R.id.img_tool_status);
            approvalRow = v.findViewById(R.id.approval_row);
            btnApprove = v.findViewById(R.id.btn_approve);
            btnReject = v.findViewById(R.id.btn_reject);
            toolStatusIcon = v.findViewById(R.id.tool_status_icon);
        }
        void bind(ChatMessage m) {
            toolName.setText(m.toolName == null ? "" : m.toolName);
            toolArgs.setText(m.toolArgsJson == null ? "" : m.toolArgsJson);
            boolean hasResult = m.toolResult != null && !m.toolResult.isEmpty();
            if (hasResult) {
                toolResult.setVisibility(View.VISIBLE);
                toolResult.setText((m.isError ? "ERROR: " : "OK: ") + m.toolResult);
                progress.setVisibility(View.GONE);
                if (imgToolStatus != null) {
                    imgToolStatus.setVisibility(View.VISIBLE);
                    imgToolStatus.setImageResource(m.isError
                            ? R.drawable.ic_mtrl_close : R.drawable.ic_mtrl_check);
                }
                if (textToolStatus != null) {
                    textToolStatus.setVisibility(View.VISIBLE);
                    textToolStatus.setText(m.isError ? "Failed" : "Done");
                }
                if (imgExpand != null) imgExpand.setVisibility(View.VISIBLE);
                if (toolStatusIcon != null) toolStatusIcon.setVisibility(View.GONE);
            } else {
                toolResult.setVisibility(View.GONE);
                // Only show the spinner for tool_call rows (in-flight).
                boolean inFlight = ChatMessage.TYPE_TOOL_CALL.equals(m.type);
                progress.setVisibility(inFlight ? View.VISIBLE : View.GONE);
                if (imgToolStatus != null) imgToolStatus.setVisibility(View.GONE);
                if (textToolStatus != null) {
                    textToolStatus.setVisibility(inFlight ? View.VISIBLE : View.GONE);
                    if (inFlight) textToolStatus.setText("Running…");
                }
                if (imgExpand != null) imgExpand.setVisibility(View.GONE);
                if (toolStatusIcon != null) toolStatusIcon.setVisibility(View.VISIBLE);
            }
            // Default collapsed when result arrives; auto-expand while in-flight
            if (layoutToolDetails != null) {
                if (hasResult && !expanded) {
                    layoutToolDetails.setVisibility(View.GONE);
                } else if (!hasResult) {
                    // Show args during in-flight calls so user can see what's happening
                    layoutToolDetails.setVisibility(ChatMessage.TYPE_TOOL_CALL.equals(m.type) ? View.VISIBLE : View.GONE);
                } else {
                    layoutToolDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
                }
            }
            // Toggle expand on header click
            if (imgExpand != null && layoutToolDetails != null) {
                View headerTarget = itemView;
                headerTarget.setOnClickListener(v -> {
                    expanded = !expanded;
                    layoutToolDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
                    imgExpand.setImageResource(expanded ? R.drawable.ic_mtrl_arrow_down : R.drawable.ic_mtrl_arrow_down);
                    imgExpand.setRotation(expanded ? 180f : 0f);
                });
            }
            // Approval row (hidden by default — surfaced by ChatFragment when a tool needs approval)
            if (approvalRow != null) approvalRow.setVisibility(View.GONE);
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
