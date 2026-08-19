package pro.sketchware.creator.runtime;

import androidx.viewpager.widget.ViewPager;
import java.util.Map;

/** Runtime-native page selection service for the typed live pager widget. */
public final class CreatorFragmentAdapterService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorFragmentAdapterService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "fragment_adapter"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String widgetId = CreatorRuntimeServiceArguments.string(arguments, "widgetId");
        if (widgetId == null) return CreatorRuntimeServiceArguments.invalid("fragment_adapter requires pager widgetId.");
        android.view.View view = environment.findWidget(widgetId);
        if (!(view instanceof ViewPager)) return CreatorRuntimeServiceArguments.invalid("widgetId does not reference a live pager.");
        ViewPager pager = (ViewPager) view;
        if ("select_page".equals(action)) {
            int page = (int) CreatorRuntimeServiceArguments.longValue(arguments, "page", -1L);
            if (page < 0 || pager.getAdapter() == null || page >= pager.getAdapter().getCount()) {
                return CreatorRuntimeServiceArguments.invalid("page is outside the live pager range.");
            }
            boolean smooth = !"false".equals(CreatorRuntimeServiceArguments.string(arguments, "smooth"));
            pager.setCurrentItem(page, smooth);
            return CreatorRuntimeServiceArguments.succeeded("widgetId", widgetId, "page", page);
        }
        if ("page_count".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("widgetId", widgetId,
                    "count", pager.getAdapter() == null ? 0 : pager.getAdapter().getCount());
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported fragment-adapter action: " + action);
    }
}
