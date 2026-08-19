package pro.sketchware.creator.runtime;

import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
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
        if ("list_set_data".equals(action) && widget instanceof ListView) {
            List<String> items = stringItems(arguments.get("items"));
            ListView list = (ListView) widget;
            list.setAdapter(new ArrayAdapter<>(environment.getContext(), listRowLayout(list), items));
            return CreatorRuntimeServiceArguments.succeeded("updated", true, "count", items.size());
        }
        if ("list_refresh".equals(action) && widget instanceof ListView) {
            if (((ListView) widget).getAdapter() instanceof BaseAdapter) {
                ((BaseAdapter) ((ListView) widget).getAdapter()).notifyDataSetChanged();
            }
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("list_set_item_checked".equals(action) && widget instanceof ListView) {
            int position = intValue(arguments.get("position"), -1);
            if (position < 0) return CreatorRuntimeServiceArguments.invalid("list checked position must be non-negative.");
            ((ListView) widget).setItemChecked(position, booleanValue(arguments.get("checked")));
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("list_smooth_scroll_to".equals(action) && widget instanceof ListView) {
            int position = intValue(arguments.get("position"), -1);
            if (position < 0) return CreatorRuntimeServiceArguments.invalid("list scroll position must be non-negative.");
            ((ListView) widget).smoothScrollToPosition(position);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("spinner_set_data".equals(action) && widget instanceof Spinner) {
            List<String> items = stringItems(arguments.get("items"));
            ((Spinner) widget).setAdapter(new ArrayAdapter<>(environment.getContext(),
                    android.R.layout.simple_spinner_dropdown_item, items));
            return CreatorRuntimeServiceArguments.succeeded("updated", true, "count", items.size());
        }
        if ("spinner_refresh".equals(action) && widget instanceof Spinner) {
            if (((Spinner) widget).getAdapter() instanceof BaseAdapter) {
                ((BaseAdapter) ((Spinner) widget).getAdapter()).notifyDataSetChanged();
            }
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("request_focus".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("focused", widget.requestFocus());
        }
        if ("web_go_back".equals(action) && widget instanceof WebView) {
            WebView web = (WebView) widget;
            if (web.canGoBack()) web.goBack();
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("web_go_forward".equals(action) && widget instanceof WebView) {
            WebView web = (WebView) widget;
            if (web.canGoForward()) web.goForward();
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Widget does not support action: " + action);
    }

    private static int listRowLayout(ListView list) {
        return list.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE
                ? android.R.layout.simple_list_item_multiple_choice
                : list.getChoiceMode() == ListView.CHOICE_MODE_SINGLE
                ? android.R.layout.simple_list_item_single_choice : android.R.layout.simple_list_item_1;
    }

    private static List<String> stringItems(Object rawItems) {
        List<String> items = new ArrayList<>();
        if (rawItems instanceof List) for (Object item : (List<?>) rawItems) items.add(String.valueOf(item));
        return items;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }
}
