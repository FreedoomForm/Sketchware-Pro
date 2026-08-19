package pro.sketchware.creator.runtime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native dialog and progress-dialog service. */
@SuppressWarnings("deprecation")
public final class CreatorDialogService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private ProgressDialog progressDialog;
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Map<String, AlertDialog> dialogs = new LinkedHashMap<>();

    public CreatorDialogService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "dialog"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("set_title".equals(action) || "set_message".equals(action)) {
            String id = id(arguments);
            String value = CreatorRuntimeServiceArguments.string(arguments, "value");
            if (value == null) return CreatorRuntimeServiceArguments.invalid(action + " requires value.");
            if ("set_title".equals(action)) titles.put(id, value); else messages.put(id, value);
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
        }
        if ("show".equals(action)) {
            String id = idOrDefault(arguments);
            String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            String message = CreatorRuntimeServiceArguments.string(arguments, "message");
            if (title == null) title = titles.get(id);
            if (message == null) message = messages.get(id);
            String label = CreatorRuntimeServiceArguments.string(arguments, "positiveLabel");
            String finalTitle = title;
            String finalMessage = message;
            environment.getActivity().runOnUiThread(() -> {
                AlertDialog existing = dialogs.remove(id);
                if (existing != null) existing.dismiss();
                AlertDialog dialog = new AlertDialog.Builder(environment.getActivity())
                        .setTitle(finalTitle).setMessage(finalMessage)
                        .setPositiveButton(label == null ? "OK" : label, null).create();
                dialogs.put(id, dialog);
                dialog.show();
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
        }
        if ("dismiss".equals(action)) {
            String id = idOrDefault(arguments);
            environment.getActivity().runOnUiThread(() -> {
                AlertDialog dialog = dialogs.remove(id);
                if (dialog != null) dialog.dismiss();
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
        }
        if ("show_progress".equals(action)) {
            String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            String message = CreatorRuntimeServiceArguments.string(arguments, "message");
            environment.getActivity().runOnUiThread(() -> {
                if (progressDialog != null) progressDialog.dismiss();
                progressDialog = ProgressDialog.show(environment.getActivity(), title, message, true, false);
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action);
        }
        if ("dismiss_progress".equals(action)) {
            environment.getActivity().runOnUiThread(() -> {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported dialog action: " + action);
    }

    private static String id(Map<String, Object> arguments) {
        String id = CreatorRuntimeServiceArguments.string(arguments, "dialogId");
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("dialogId is required.");
        return id;
    }

    private static String idOrDefault(Map<String, Object> arguments) {
        String id = CreatorRuntimeServiceArguments.string(arguments, "dialogId");
        return id == null || id.trim().isEmpty() ? "runtime" : id;
    }
}
