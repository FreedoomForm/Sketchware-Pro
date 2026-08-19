package pro.sketchware.creator.runtime;

import android.view.View;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CalendarView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
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
        if (widget == null) return CreatorRuntimeServiceArguments.invalid("Runtime widget is not available: " + widgetId);
        if ("location_x".equals(action) || "location_y".equals(action)) {
            int[] location = new int[2];
            widget.getLocationInWindow(location);
            return CreatorRuntimeServiceArguments.succeeded("value", "location_x".equals(action) ? location[0] : location[1]);
        }
        if ("get_enabled".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.isEnabled());
        if ("get_alpha".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getAlpha());
        if ("get_rotation".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getRotation());
        if ("get_translation_x".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getTranslationX());
        if ("get_translation_y".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getTranslationY());
        if ("get_scale_x".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getScaleX());
        if ("get_scale_y".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", widget.getScaleY());
        if ("get_text".equals(action) && widget instanceof TextView) {
            return CreatorRuntimeServiceArguments.succeeded("value", String.valueOf(((TextView) widget).getText()));
        }
        if ("get_checked".equals(action) && widget instanceof CompoundButton) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((CompoundButton) widget).isChecked());
        }
        if ("seek_max".equals(action) && widget instanceof SeekBar) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((SeekBar) widget).getMax());
        }
        if ("seek_progress".equals(action) && widget instanceof SeekBar) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((SeekBar) widget).getProgress());
        }
        if ("spinner_selection".equals(action) && widget instanceof Spinner) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((Spinner) widget).getSelectedItemPosition());
        }
        if ("web_url".equals(action) && widget instanceof WebView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((WebView) widget).getUrl());
        }
        if ("calendar_date".equals(action) && widget instanceof CalendarView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((CalendarView) widget).getDate());
        }
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
        if ("list_checked_positions".equals(action) && widget instanceof ListView) {
            android.util.SparseBooleanArray checked = ((ListView) widget).getCheckedItemPositions();
            List<Object> positions = new ArrayList<>();
            for (int index = 0; index < checked.size(); index++) {
                if (checked.valueAt(index)) positions.add(checked.keyAt(index));
            }
            return CreatorRuntimeServiceArguments.succeeded("positions", positions);
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
        if ("spinner_set_selection".equals(action) && widget instanceof Spinner) {
            int position = intValue(arguments.get("position"), -1);
            if (position < 0) return CreatorRuntimeServiceArguments.invalid("spinner selection must be non-negative.");
            ((Spinner) widget).setSelection(position);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("request_focus".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("focused", widget.requestFocus());
        }
        if ("progress_set_indeterminate".equals(action) && widget instanceof ProgressBar) {
            ((ProgressBar) widget).setIndeterminate(booleanValue(arguments.get("indeterminate")));
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("image_set_color_filter".equals(action) && widget instanceof ImageView) {
            Integer color = colorValue(arguments.get("color"));
            if (color == null) return CreatorRuntimeServiceArguments.invalid("Image color filter requires an Android color value.");
            ((ImageView) widget).setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
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
        if ("web_set_cache_mode".equals(action) && widget instanceof WebView) {
            Integer cacheMode = cacheMode(CreatorRuntimeServiceArguments.string(arguments, "cacheMode"));
            if (cacheMode == null) return CreatorRuntimeServiceArguments.invalid("Unsupported WebView cache mode.");
            ((WebView) widget).getSettings().setCacheMode(cacheMode);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("web_clear_cache".equals(action) && widget instanceof WebView) {
            ((WebView) widget).clearCache(true);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("web_clear_history".equals(action) && widget instanceof WebView) {
            ((WebView) widget).clearHistory();
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("web_stop_loading".equals(action) && widget instanceof WebView) {
            ((WebView) widget).stopLoading();
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("web_zoom_in".equals(action) && widget instanceof WebView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((WebView) widget).zoomIn());
        }
        if ("web_zoom_out".equals(action) && widget instanceof WebView) {
            return CreatorRuntimeServiceArguments.succeeded("value", ((WebView) widget).zoomOut());
        }
        if ("calendar_set_date".equals(action) && widget instanceof CalendarView) {
            ((CalendarView) widget).setDate(longValue(arguments.get("timestamp"), 0L), true, true);
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("calendar_set_min_date".equals(action) && widget instanceof CalendarView) {
            ((CalendarView) widget).setMinDate(longValue(arguments.get("timestamp"), 0L));
            return CreatorRuntimeServiceArguments.succeeded("updated", true);
        }
        if ("calendar_set_max_date".equals(action) && widget instanceof CalendarView) {
            ((CalendarView) widget).setMaxDate(longValue(arguments.get("timestamp"), 0L));
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

    private static long longValue(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return value == null ? fallback : Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static Integer cacheMode(String rawCacheMode) {
        if (rawCacheMode == null) return null;
        String mode = rawCacheMode.trim();
        if (mode.length() > 1 && mode.startsWith("\"") && mode.endsWith("\"")) mode = mode.substring(1, mode.length() - 1);
        if ("LOAD_CACHE_ELSE_NETWORK".equals(mode)) return android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;
        if ("LOAD_CACHE_ONLY".equals(mode)) return android.webkit.WebSettings.LOAD_CACHE_ONLY;
        if ("LOAD_DEFAULT".equals(mode)) return android.webkit.WebSettings.LOAD_DEFAULT;
        if ("LOAD_NO_CACHE".equals(mode)) return android.webkit.WebSettings.LOAD_NO_CACHE;
        return null;
    }

    private static Integer colorValue(Object rawColor) {
        if (rawColor instanceof Number) return ((Number) rawColor).intValue();
        if (rawColor == null) return null;
        String color = String.valueOf(rawColor).trim();
        try {
            if (color.startsWith("#")) return Color.parseColor(color);
            return Long.decode(color).intValue();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
