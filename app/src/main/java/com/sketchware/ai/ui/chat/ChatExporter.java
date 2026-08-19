package com.sketchware.ai.ui.chat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exports the current chat conversation to a text file and offers it to the
 * user via Android's share-sheet ({@link Intent#ACTION_SEND}) so they can
 * send the full transcript (including tool calls, results, errors, and
 * usage stats) to a developer for debugging.
 *
 * <p>The export format is plain UTF-8 text with one section per
 * {@link ChatMessage}. Each section starts with a header line of the form:
 * <pre>
 *   === [01] 2026-08-11 21:18:49.336  USER ===
 * </pre>
 * followed by the message body indented by 2 spaces. Errors are prefixed
 * with {@code [ERROR]}. Tool calls show the tool name + JSON args. Tool
 * results show the tool name + truncated output (4 KB max). Usage rows
 * show in/out tokens and cost.
 *
 * <p>The file is written to the app's external files directory (no
 * permission needed on modern Android) under {@code chats/}, then shared
 * via {@link FileProvider} so the user can email/message/upload it.
 *
 * <p>If a {@code ACTION_CREATE_DOCUMENT} SAF flow is preferred over the
 * share-sheet, call {@link #createSaveIntent(Context, String)} and launch
 * it with the activity-result registry.
 */
public final class ChatExporter {

    /** Max characters of a tool result to include in the export. */
    private static final int MAX_TOOL_RESULT_CHARS = 4096;

    /** Max characters of a tool args JSON to include in the export. */
    private static final int MAX_TOOL_ARGS_CHARS = 2048;

    private ChatExporter() {}

    /**
     * Render the conversation as plain text.
     *
     * @param messages the chat rows (typically from {@link MessageReducer#getMessages()})
     * @return a non-null, human-readable transcript
     */
    public static String renderTranscript(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "(empty conversation)\n";
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        StringBuilder sb = new StringBuilder(8192);
        sb.append("=== Sketchware AI Chat Export ===\n");
        sb.append("Generated: ").append(fmt.format(new Date())).append("\n");
        sb.append("Messages: ").append(messages.size()).append("\n\n");

        int idx = 1;
        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        double totalCost = 0.0;

        for (ChatMessage m : messages) {
            String ts = fmt.format(new Date(m.ts));
            String label = labelFor(m.type, idx);
            sb.append("=== [").append(pad(idx, messages.size())).append("] ")
              .append(ts).append("  ").append(label).append(" ===\n");

            switch (m.type) {
                case ChatMessage.TYPE_USER:
                    sb.append("  ").append(safe(m.text)).append("\n\n");
                    break;
                case ChatMessage.TYPE_TEXT:
                    sb.append(indent(safe(m.text))).append("\n\n");
                    break;
                case ChatMessage.TYPE_REASONING:
                    sb.append("  (reasoning)\n");
                    sb.append(indent(safe(m.text))).append("\n\n");
                    break;
                case ChatMessage.TYPE_TOOL_CALL:
                    sb.append("  Tool: ").append(safe(m.toolName)).append("\n");
                    sb.append("  Args: ").append(truncate(safe(m.toolArgsJson), MAX_TOOL_ARGS_CHARS)).append("\n\n");
                    break;
                case ChatMessage.TYPE_TOOL_RESULT:
                    sb.append("  Tool: ").append(safe(m.toolName)).append("\n");
                    sb.append("  Result (").append(m.isError ? "ERROR" : "ok").append("):\n");
                    sb.append(indent(truncate(safe(m.toolResult), MAX_TOOL_RESULT_CHARS))).append("\n\n");
                    break;
                case ChatMessage.TYPE_ERROR:
                    sb.append("  [ERROR] ").append(safe(m.text)).append("\n\n");
                    break;
                case ChatMessage.TYPE_API_REQ_DONE:
                    sb.append("  Tokens: in=").append(m.inputTokens)
                      .append(", out=").append(m.outputTokens)
                      .append(", cost=$").append(String.format(Locale.US, "%.4f", m.cost))
                      .append("\n\n");
                    totalInputTokens += m.inputTokens;
                    totalOutputTokens += m.outputTokens;
                    totalCost += m.cost;
                    break;
                case ChatMessage.TYPE_API_REQ_START:
                    sb.append("  (API request started)\n\n");
                    break;
                case ChatMessage.TYPE_COMPLETION:
                    sb.append("  Completion: ").append(safe(m.text)).append("\n\n");
                    break;
                case ChatMessage.TYPE_COMPACTION:
                    sb.append("  (conversation compacted)\n\n");
                    break;
                default:
                    sb.append("  (").append(m.type).append(")\n\n");
                    break;
            }
            idx++;
        }

        sb.append("=== Summary ===\n");
        sb.append("Total messages: ").append(messages.size()).append("\n");
        sb.append("Total input tokens: ").append(totalInputTokens).append("\n");
        sb.append("Total output tokens: ").append(totalOutputTokens).append("\n");
        sb.append("Total cost: $").append(String.format(Locale.US, "%.4f", totalCost)).append("\n");
        return sb.toString();
    }

    /**
     * Write the transcript to the app's external files directory under
     * {@code chats/chat_<timestamp>.txt} and return the file.
     *
     * @param context any context (will use {@link Context#getExternalFilesDir})
     * @param messages the chat rows
     * @return the written file (never null)
     * @throws java.io.IOException if the file cannot be written
     */
    public static File writeToCacheFile(Context context, List<ChatMessage> messages) throws java.io.IOException {
        String transcript = renderTranscript(messages);
        File dir = new File(context.getExternalFilesDir(null), "chats");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new java.io.IOException("Could not create chats directory: " + dir);
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String name = "chat_" + fmt.format(new Date()) + ".txt";
        File file = new File(dir, name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(transcript.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return file;
    }

    /**
     * Build an {@link Intent#ACTION_SEND} intent that shares the cached
     * transcript file via Android's share-sheet. The caller must use
     * {@link androidx.core.content.FileProvider#getUriForFile} to grant
     * the receiving app read access.
     *
     * <p>The caller must also register a {@code <provider>} in
     * {@code AndroidManifest.xml} with {@code androidx.core.content.FileProvider}
     * and a {@code file_paths.xml} that includes
     * {@code <external-files-path name="chats" path="chats/" />}.
     *
     * @param context any context
     * @param file the file returned by {@link #writeToCacheFile}
     * @return a configured ACTION_SEND intent (chooseable)
     */
    public static Intent createShareIntent(Context context, File file) {
        // The app's FileProvider authority is "${applicationId}.provider"
        // (declared in AndroidManifest.xml with @xml/provider_paths that
        // exposes the entire external-path root).
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, "Sketchware AI Chat Export — " + file.getName());
        share.putExtra(Intent.EXTRA_TEXT,
                "Sketchware AI chat transcript attached.\n\nGenerated: " + new Date());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(share, "Share chat export");
    }

    /**
     * Save the transcript to a user-chosen location via SAF
     * (Storage Access Framework). The caller should launch this intent with
     * the activity-result registry and then write the content to the
     * returned URI.
     *
     * @param suggestedName the suggested file name (e.g. "chat_20260811.txt")
     * @return an {@link Intent#ACTION_CREATE_DOCUMENT} intent
     */
    public static Intent createSaveIntent(String suggestedName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        return intent;
    }

    /**
     * Write the transcript to a SAF-provided URI (from
     * {@link #createSaveIntent(String)}).
     *
     * @param resolver the content resolver
     * @param uri the URI returned by the SAF picker
     * @param messages the chat rows
     * @return true on success, false on failure
     */
    public static boolean writeToSafUri(ContentResolver resolver, Uri uri, List<ChatMessage> messages) {
        if (uri == null) return false;
        String transcript = renderTranscript(messages);
        try (OutputStream os = resolver.openOutputStream(uri)) {
            if (os == null) return false;
            os.write(transcript.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ----------------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------------

    private static String labelFor(String type, int idx) {
        switch (type) {
            case ChatMessage.TYPE_USER:          return "USER";
            case ChatMessage.TYPE_TEXT:          return "ASSISTANT";
            case ChatMessage.TYPE_REASONING:     return "REASONING";
            case ChatMessage.TYPE_API_REQ_START: return "API_REQ_START";
            case ChatMessage.TYPE_API_REQ_DONE:  return "API_REQ_DONE";
            case ChatMessage.TYPE_TOOL_CALL:     return "TOOL_CALL";
            case ChatMessage.TYPE_TOOL_RESULT:   return "TOOL_RESULT";
            case ChatMessage.TYPE_ERROR:         return "ERROR";
            case ChatMessage.TYPE_COMPLETION:    return "COMPLETION";
            case ChatMessage.TYPE_COMPACTION:    return "COMPACTION";
            default:                              return type.toUpperCase(Locale.US);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        int half = max / 2;
        return s.substring(0, half)
                + "\n  ... [truncated " + (s.length() - max) + " chars] ...\n  "
                + s.substring(s.length() - half);
    }

    private static String indent(String s) {
        if (s == null || s.isEmpty()) return "";
        // Indent every line by 2 spaces for readability.
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (String line : s.split("\n", -1)) {
            sb.append("  ").append(line).append('\n');
        }
        // Remove trailing newline (caller adds one).
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String pad(int value, int total) {
        int width = String.valueOf(total).length();
        return String.format(Locale.US, "%" + width + "d", value);
    }
}
