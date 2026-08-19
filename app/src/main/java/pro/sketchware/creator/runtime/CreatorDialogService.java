package pro.sketchware.creator.runtime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native dialog and progress-dialog service. */
@SuppressWarnings("deprecation")
public final class CreatorDialogService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, ProgressDialog> progressDialogs = new LinkedHashMap<>();
    private final Map<String, ProgressConfiguration> progressConfigurations = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Map<String, AlertDialog> dialogs = new LinkedHashMap<>();
    private final Map<String, DialogButton> positiveButtons = new LinkedHashMap<>();
    private final Map<String, DialogButton> negativeButtons = new LinkedHashMap<>();
    private final Map<String, DialogButton> neutralButtons = new LinkedHashMap<>();

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
        if ("set_positive_button".equals(action) || "set_negative_button".equals(action)
                || "set_neutral_button".equals(action)) {
            String id = id(arguments);
            String label = CreatorRuntimeServiceArguments.string(arguments, "label");
            if (label == null) return CreatorRuntimeServiceArguments.invalid(action + " requires label.");
            DialogButton button = new DialogButton(label,
                    CreatorRuntimeServiceArguments.string(arguments, "callbackTargetId"));
            if ("set_positive_button".equals(action)) positiveButtons.put(id, button);
            else if ("set_negative_button".equals(action)) negativeButtons.put(id, button);
            else neutralButtons.put(id, button);
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
            DialogButton positive = positiveButtons.get(id);
            DialogButton negative = negativeButtons.get(id);
            DialogButton neutral = neutralButtons.get(id);
            environment.getActivity().runOnUiThread(() -> {
                AlertDialog existing = dialogs.remove(id);
                if (existing != null) existing.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(environment.getActivity())
                        .setTitle(finalTitle).setMessage(finalMessage);
                builder.setPositiveButton(positive == null ? label == null ? "OK" : label : positive.label,
                        positive == null ? null : (dialog, which) -> publishButton(id, "positive", positive));
                if (negative != null) builder.setNegativeButton(negative.label,
                        (dialog, which) -> publishButton(id, "negative", negative));
                if (neutral != null) builder.setNeutralButton(neutral.label,
                        (dialog, which) -> publishButton(id, "neutral", neutral));
                AlertDialog dialog = builder.create();
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
        if ("progress_set_title".equals(action) || "progress_set_message".equals(action)
                || "progress_set_max".equals(action) || "progress_set_value".equals(action)
                || "progress_set_cancelable".equals(action) || "progress_set_cancel_on_touch_outside".equals(action)
                || "progress_set_style".equals(action)) {
            String id = idOrDefault(arguments);
            ProgressConfiguration configuration = progressConfiguration(id);
            if ("progress_set_title".equals(action)) configuration.title = CreatorRuntimeServiceArguments.string(arguments, "value");
            else if ("progress_set_message".equals(action)) configuration.message = CreatorRuntimeServiceArguments.string(arguments, "value");
            else if ("progress_set_max".equals(action)) configuration.max = (int) CreatorRuntimeServiceArguments.longValue(arguments, "value", 0L);
            else if ("progress_set_value".equals(action)) configuration.progress = (int) CreatorRuntimeServiceArguments.longValue(arguments, "value", 0L);
            else if ("progress_set_cancelable".equals(action)) configuration.cancelable = booleanValue(arguments.get("value"), true);
            else if ("progress_set_cancel_on_touch_outside".equals(action)) configuration.cancelOnTouchOutside = booleanValue(arguments.get("value"), false);
            else configuration.style = CreatorRuntimeServiceArguments.string(arguments, "value");
            applyProgressConfiguration(id, configuration);
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
        }
        if ("show_progress".equals(action)) {
            String id = idOrDefault(arguments);
            ProgressConfiguration configuration = progressConfiguration(id);
            String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            String message = CreatorRuntimeServiceArguments.string(arguments, "message");
            if (title != null) configuration.title = title;
            if (message != null) configuration.message = message;
            environment.getActivity().runOnUiThread(() -> {
                ProgressDialog existing = progressDialogs.remove(id);
                if (existing != null) existing.dismiss();
                ProgressDialog dialog = new ProgressDialog(environment.getActivity());
                dialog.setTitle(configuration.title);
                dialog.setMessage(configuration.message);
                dialog.setProgressStyle(progressStyle(configuration.style));
                dialog.setMax(Math.max(0, configuration.max));
                dialog.setProgress(Math.max(0, configuration.progress));
                dialog.setCancelable(configuration.cancelable);
                dialog.setCanceledOnTouchOutside(configuration.cancelOnTouchOutside);
                progressDialogs.put(id, dialog);
                dialog.show();
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
        }
        if ("dismiss_progress".equals(action)) {
            String id = idOrDefault(arguments);
            environment.getActivity().runOnUiThread(() -> {
                ProgressDialog dialog = progressDialogs.remove(id);
                if (dialog != null) dialog.dismiss();
            });
            return CreatorRuntimeServiceArguments.succeeded("action", action, "dialogId", id);
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

    private ProgressConfiguration progressConfiguration(String id) {
        ProgressConfiguration configuration = progressConfigurations.get(id);
        if (configuration == null) {
            configuration = new ProgressConfiguration();
            progressConfigurations.put(id, configuration);
        }
        return configuration;
    }

    private void applyProgressConfiguration(String id, ProgressConfiguration configuration) {
        environment.getActivity().runOnUiThread(() -> {
            ProgressDialog dialog = progressDialogs.get(id);
            if (dialog == null) return;
            dialog.setTitle(configuration.title);
            dialog.setMessage(configuration.message);
            dialog.setProgressStyle(progressStyle(configuration.style));
            dialog.setMax(Math.max(0, configuration.max));
            dialog.setProgress(Math.max(0, configuration.progress));
            dialog.setCancelable(configuration.cancelable);
            dialog.setCanceledOnTouchOutside(configuration.cancelOnTouchOutside);
        });
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return fallback;
    }

    private static int progressStyle(String style) {
        return "STYLE_HORIZONTAL".equalsIgnoreCase(style) || "HORIZONTAL".equalsIgnoreCase(style)
                ? ProgressDialog.STYLE_HORIZONTAL : ProgressDialog.STYLE_SPINNER;
    }

    private void publishButton(String dialogId, String button, DialogButton configuration) {
        Map<String, Object> payload = CreatorRuntimeServiceArguments.output("dialogId", dialogId, "button", button);
        if (configuration.callbackTargetId != null && !configuration.callbackTargetId.trim().isEmpty()) {
            payload.put("callbackTargetId", configuration.callbackTargetId);
        }
        environment.publish(getId(), "button", payload);
    }

    private static final class DialogButton {
        final String label;
        final String callbackTargetId;
        DialogButton(String label, String callbackTargetId) {
            this.label = label;
            this.callbackTargetId = callbackTargetId;
        }
    }

    private static final class ProgressConfiguration {
        String title;
        String message;
        int max;
        int progress;
        boolean cancelable = true;
        boolean cancelOnTouchOutside;
        String style = "STYLE_SPINNER";
    }
}
