package pro.sketchware.creator.runtime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import java.util.Map;

/** Runtime-native dialog and progress-dialog service. */
@SuppressWarnings("deprecation")
public final class CreatorDialogService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private ProgressDialog progressDialog;

    public CreatorDialogService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "dialog"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("show".equals(action)) {
            String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            String message = CreatorRuntimeServiceArguments.string(arguments, "message");
            String label = CreatorRuntimeServiceArguments.string(arguments, "positiveLabel");
            environment.getActivity().runOnUiThread(() -> new AlertDialog.Builder(environment.getActivity())
                    .setTitle(title).setMessage(message).setPositiveButton(label == null ? "OK" : label, null).show());
            return CreatorRuntimeServiceArguments.succeeded("action", action);
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
}
