package com.sketchware.ai.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sketchware.ai.ui.chat.ChatMessage;
import pro.sketchware.R;

/**
 * RecyclerView adapter for the chat. Renders different row types based on
 * {@link ChatMessage#type}. Mirrors Cline's {@code ChatRow.tsx} dispatch.
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

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override public boolean areItemsTheSame(@NonNull ChatMessage o, @NonNull ChatMessage n) {
                return o == n;
            }
            @Override public boolean areContentsTheSame(@NonNull ChatMessage o, @NonNull ChatMessage n) {
                return o == n;
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
            ((TextHolder) h).bind(m);
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
        TextHolder(@NonNull View v) { super(v); text = v.findViewById(R.id.text); }
        void bind(ChatMessage m) {
            String t = m.text == null ? "" : m.text;
            if (m.isStreaming) t = t + "▋"; // typewriter cursor
            text.setText(t);
            // API req info row
            if (ChatMessage.TYPE_API_REQ_DONE.equals(m.type)) {
                text.setText("API: in=" + m.inputTokens + " out=" + m.outputTokens + " cost=$" + String.format("%.4f", m.cost));
            } else if (ChatMessage.TYPE_API_REQ_START.equals(m.type)) {
                text.setText("Calling API...");
            } else if (ChatMessage.TYPE_COMPACTION.equals(m.type)) {
                text.setText("Conversation compacted to fit context window.");
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
            if (m.toolResult != null) {
                toolResult.setVisibility(View.VISIBLE);
                toolResult.setText((m.isError ? "ERROR: " : "OK: ") + m.toolResult);
                progress.setVisibility(View.GONE);
            } else {
                toolResult.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
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
