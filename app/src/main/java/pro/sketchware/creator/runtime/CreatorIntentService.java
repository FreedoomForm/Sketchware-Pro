package pro.sketchware.creator.runtime;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.util.Map;

/** Executes the legacy Intent component through the current Creator Runtime activity. */
public final class CreatorIntentService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;

    public CreatorIntentService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "intent"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        try {
            if ("open_url".equals(action)) {
                String url = CreatorRuntimeServiceArguments.string(arguments, "url");
                if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
                    return CreatorRuntimeServiceArguments.invalid("open_url requires an http or https url.");
                }
                environment.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return CreatorRuntimeServiceArguments.succeeded("action", action, "url", url);
            }
            if ("share_text".equals(action)) {
                String text = CreatorRuntimeServiceArguments.string(arguments, "text");
                if (text == null) return CreatorRuntimeServiceArguments.invalid("share_text requires text.");
                Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, text);
                environment.getActivity().startActivity(Intent.createChooser(share,
                        CreatorRuntimeServiceArguments.string(arguments, "chooserTitle")));
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
            if ("dial".equals(action)) {
                String number = CreatorRuntimeServiceArguments.string(arguments, "number");
                if (number == null) return CreatorRuntimeServiceArguments.invalid("dial requires number.");
                environment.getActivity().startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
                return CreatorRuntimeServiceArguments.succeeded("action", action, "number", number);
            }
            return CreatorRuntimeServiceArguments.invalid("Unsupported intent action: " + action);
        } catch (ActivityNotFoundException error) {
            return CreatorRuntimeServiceArguments.failed("No Android activity can handle this intent.");
        } catch (RuntimeException error) {
            return CreatorRuntimeServiceArguments.failed(error.getMessage());
        }
    }
}
