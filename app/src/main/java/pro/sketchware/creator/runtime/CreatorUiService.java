package pro.sketchware.creator.runtime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import java.util.Map;

/** Runtime-native activity title and clipboard operations used by legacy UI blocks. */
public final class CreatorUiService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;

    public CreatorUiService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "ui"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("set_title".equals(action)) {
            String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            if (title == null) return CreatorRuntimeServiceArguments.invalid("set_title requires title.");
            environment.getActivity().setTitle(title);
            return CreatorRuntimeServiceArguments.succeeded("action", action);
        }
        if ("copy_text".equals(action)) {
            String text = CreatorRuntimeServiceArguments.string(arguments, "text");
            if (text == null) return CreatorRuntimeServiceArguments.invalid("copy_text requires text.");
            ClipboardManager clipboard = (ClipboardManager) environment.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return CreatorRuntimeServiceArguments.failed("Clipboard service is unavailable.");
            clipboard.setPrimaryClip(ClipData.newPlainText("creator_runtime", text));
            return CreatorRuntimeServiceArguments.succeeded("action", action, "copied", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported UI action: " + action);
    }
}
