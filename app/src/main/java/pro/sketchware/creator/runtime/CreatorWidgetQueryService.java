package pro.sketchware.creator.runtime;

import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;
import java.util.Map;

/**
 * Read-only typed queries over the current Creator Runtime widget registry.
 * This is intentionally limited to canonical Sketchware reporter values and
 * never exposes arbitrary host views or Java execution.
 */
public final class CreatorWidgetQueryService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;

    public CreatorWidgetQueryService(CreatorRuntimeEnvironment environment) {
        if (environment == null) throw new IllegalArgumentException("environment");
        this.environment = environment;
    }

    @Override public String getId() { return "widget"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String widgetId = CreatorRuntimeServiceArguments.string(arguments, "widgetId");
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (widgetId == null || action == null) {
            return CreatorRuntimeServiceArguments.invalid("widget query requires widgetId and action.");
        }
        View widget = environment.findWidget(widgetId);
        if ("web_can_go_back".equals(action) && widget instanceof WebView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((WebView) widget).canGoBack());
        }
        if ("web_can_go_forward".equals(action) && widget instanceof WebView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((WebView) widget).canGoForward());
        }
        if ("list_checked_position".equals(action) && widget instanceof ListView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((ListView) widget).getCheckedItemPosition());
        }
        if ("list_checked_count".equals(action) && widget instanceof ListView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((ListView) widget).getCheckedItemCount());
        }
        return CreatorRuntimeServiceArguments.invalid("Widget does not support action: " + action);
    }
}
