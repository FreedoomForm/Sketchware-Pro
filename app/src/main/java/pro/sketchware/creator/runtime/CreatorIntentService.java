package pro.sketchware.creator.runtime;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executes legacy Intent behavior through the current Creator Runtime activity. */
public final class CreatorIntentService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, Intent> configuredIntents = new LinkedHashMap<>();
    private final Map<String, String> targetScreens = new LinkedHashMap<>();

    public CreatorIntentService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "intent"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        try {
            if ("configure_action".equals(action)) {
                intent(arguments).setAction(resolveIntentAction(CreatorRuntimeServiceArguments.string(arguments, "value")));
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
            if ("configure_data".equals(action)) {
                String value = CreatorRuntimeServiceArguments.string(arguments, "value");
                if (value == null) return CreatorRuntimeServiceArguments.invalid("configure_data requires value.");
                intent(arguments).setData(Uri.parse(value));
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
            if ("configure_screen".equals(action)) {
                String screenId = CreatorRuntimeServiceArguments.string(arguments, "screenId");
                if (screenId == null) return CreatorRuntimeServiceArguments.invalid("configure_screen requires screenId.");
                targetScreens.put(id(arguments), screenId);
                return CreatorRuntimeServiceArguments.succeeded("action", action, "screenId", screenId);
            }
            if ("put_extra".equals(action)) {
                String key = CreatorRuntimeServiceArguments.string(arguments, "key");
                if (key == null) return CreatorRuntimeServiceArguments.invalid("put_extra requires key.");
                intent(arguments).putExtra(key, CreatorRuntimeServiceArguments.string(arguments, "value"));
                return CreatorRuntimeServiceArguments.succeeded("action", action, "key", key);
            }
            if ("get_string".equals(action)) {
                String key = CreatorRuntimeServiceArguments.string(arguments, "key");
                if (key == null) return CreatorRuntimeServiceArguments.invalid("get_string requires key.");
                return CreatorRuntimeServiceArguments.succeeded("value", intent(arguments).getStringExtra(key));
            }
            if ("set_flags".equals(action)) {
                String flag = CreatorRuntimeServiceArguments.string(arguments, "flag");
                if (flag == null) return CreatorRuntimeServiceArguments.invalid("set_flags requires flag.");
                intent(arguments).setFlags(resolveIntentFlag(flag));
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
            if ("start".equals(action)) {
                String intentId = id(arguments);
                String screenId = targetScreens.get(intentId);
                if (screenId != null) {
                    environment.publish(getId(), "navigate", CreatorRuntimeServiceArguments.output("screenId", screenId));
                    return CreatorRuntimeServiceArguments.succeeded("action", action, "screenId", screenId);
                }
                environment.getActivity().startActivity(intent(arguments));
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
            if ("finish".equals(action)) {
                environment.getActivity().finish();
                return CreatorRuntimeServiceArguments.succeeded("action", action);
            }
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

    private Intent intent(Map<String, Object> arguments) {
        String id = id(arguments);
        Intent result = configuredIntents.get(id);
        if (result == null) {
            result = new Intent();
            configuredIntents.put(id, result);
        }
        return result;
    }

    private static String id(Map<String, Object> arguments) {
        String id = CreatorRuntimeServiceArguments.string(arguments, "intentId");
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("intentId is required.");
        return id;
    }

    private static String resolveIntentAction(String raw) {
        if (raw == null) throw new IllegalArgumentException("configure_action requires value.");
        String name = raw.replace("\"", "").trim();
        try {
            Field field = Intent.class.getField(name.startsWith("ACTION_") ? name : "ACTION_" + name);
            return String.valueOf(field.get(null));
        } catch (ReflectiveOperationException ignored) {
            return name;
        }
    }

    private static int resolveIntentFlag(String raw) {
        String name = raw.replace("\"", "").trim();
        try {
            Field field = Intent.class.getField(name.startsWith("FLAG_ACTIVITY_") ? name : "FLAG_ACTIVITY_" + name);
            return field.getInt(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Unsupported intent flag: " + raw);
        }
    }
}
