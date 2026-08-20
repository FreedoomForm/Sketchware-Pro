package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.button.MaterialButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorEntryControl;
import pro.sketchware.creator.runtime.CreatorEventBinding;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectDocumentCodec;
import pro.sketchware.creator.runtime.CreatorRuntimeBlock;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;
import pro.sketchware.creator.runtime.CreatorScreen;
import pro.sketchware.creator.runtime.CreatorWidget;

/**
 * Native behavior evidence for the typed Creator Runtime widget bridge.
 *
 * <p>The fixture is persisted through the production runtime store and launched
 * through the declared CreatorProjectActivity. It does not inject views or
 * invoke generated project Java.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreatorRuntimeNativeWidgetTest {

    private Context context;

    @Before public void clearRuntimeStore() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void typedWidgetEventsAndDrawerSurviveNativeRerender() {
        seedRuntimeDocument();

        try (ActivityScenario<CreatorProjectActivity> scenario =
                     ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup canvas = activity.findViewById(R.id.creator_preview_canvas);

                requireButton(canvas, "Increment").performClick();
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState().get("clicks"))
                        .isEqualTo(1L);

                ListView list = requireView(canvas, ListView.class);
                assertThat(list.getAdapter().getCount()).isEqualTo(3);
                list.performItemClick(list.getChildAt(1), 1, list.getAdapter().getItemId(1));
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("listSelection")).isEqualTo(1L);

                Spinner spinner = requireView(canvas, Spinner.class);
                assertThat(spinner.getAdapter().getCount()).isEqualTo(3);
                spinner.setSelection(2);
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("spinnerSelection")).isEqualTo(2L);

                CalendarView calendar = requireView(canvas, CalendarView.class);
                requireButton(canvas, "Set date").performClick();
                assertThat(CreatorRuntimeSession.get(activity).getDocument().getState()
                        .get("calendarDate")).isEqualTo(fixtureDate());
                assertThat(calendar.getDate()).isEqualTo(fixtureDate());

                requireButton(canvas, "Schedule timer").performClick();
                requireButton(canvas, "Open drawer").performClick();
                DrawerLayout openDrawer = requireView(canvas, DrawerLayout.class);
                assertThat(openDrawer.isDrawerOpen(GravityCompat.START)).isTrue();

                activity.onBackPressed();
                DrawerLayout closedDrawer = requireView(canvas, DrawerLayout.class);
                assertThat(closedDrawer.isDrawerOpen(GravityCompat.START)).isFalse();
            });
            long deadline = System.currentTimeMillis() + 3000L;
            while (System.currentTimeMillis() < deadline
                    && !Long.valueOf(1L).equals(CreatorRuntimeSession.get(context).getDocument()
                    .getState().get("timerTicks"))) {
                try { Thread.sleep(50L); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            assertThat(CreatorRuntimeSession.get(context).getDocument().getState().get("timerTicks"))
                    .isEqualTo(1L);
        }
    }

    private void seedRuntimeDocument() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null,
                Arrays.asList("button", "drawer_button", "calendar_button", "timer_button",
                        "list", "spinner", "calendar"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root",
                null, map("text", "Increment")));
        widgets.put("drawer_button", new CreatorWidget("drawer_button", "button", "root",
                null, map("text", "Open drawer")));
        widgets.put("calendar_button", new CreatorWidget("calendar_button", "button", "root",
                null, map("text", "Set date")));
        widgets.put("timer_button", new CreatorWidget("timer_button", "button", "root",
                null, map("text", "Schedule timer")));
        widgets.put("list", new CreatorWidget("list", "list", "root", null,
                map("customDataStateId", "items", "choiceMode", ListView.CHOICE_MODE_SINGLE)));
        widgets.put("spinner", new CreatorWidget("spinner", "spinner", "root", null,
                map("customDataStateId", "spinnerItems")));
        widgets.put("calendar", new CreatorWidget("calendar", "calendar_view", "root", null, null));
        widgets.put("drawer_root", new CreatorWidget("drawer_root", "column", null,
                Arrays.asList("drawer_text"), null));
        widgets.put("drawer_text", new CreatorWidget("drawer_text", "text", "drawer_root",
                null, map("text", "Runtime drawer")));

        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        screens.put("_drawer_home", new CreatorScreen("_drawer_home", "/drawer", "drawer_root"));

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("clicks", 0L);
        state.put("items", Arrays.asList("A", "B", "C"));
        state.put("spinnerItems", Arrays.asList("One", "Two", "Three"));
        state.put("calendarDate", 0L);
        state.put("timerTicks", 0L);
        state.put("legacy.projectFileIndex", map("home", map("hasDrawer", true)));

        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
        events.put("button-click", new CreatorEventBinding("button-click", "button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                        map("stateId", "clicks", "delta", 1L)))));
        events.put("drawer-click", new CreatorEventBinding("drawer-click", "drawer_button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "drawer", "arguments", map("action", "open"))))));
        events.put("calendar-click", new CreatorEventBinding("calendar-click", "calendar_button", "click",
                Arrays.asList(
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "calendar", "action", "calendar_set_date",
                                        "timestamp", fixtureDate()))),
                        new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                                map("serviceId", "widget", "arguments", map(
                                        "widgetId", "calendar", "action", "calendar_date",
                                        "resultStateId", "calendarDate"))))));
        events.put("timer-button", new CreatorEventBinding("timer-button", "timer_button", "click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "timer", "arguments", map(
                                "timerId", "timer1", "action", "after", "delayMs", 75L))))));
        events.put("timer-tick", new CreatorEventBinding("timer-tick", "timer1", "tick",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE,
                        map("stateId", "timerTicks", "delta", 1L)))));
        events.put("list-click", new CreatorEventBinding("list-click", "list", "item_click",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "widget", "arguments", map(
                                "widgetId", "list", "action", "list_checked_position",
                                "resultStateId", "listSelection"))))));
        events.put("spinner-select", new CreatorEventBinding("spinner-select", "spinner", "item_selected",
                Arrays.asList(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL,
                        map("serviceId", "widget", "arguments", map(
                                "widgetId", "spinner", "action", "spinner_selection",
                                "resultStateId", "spinnerSelection"))))));

        CreatorProjectDocument document = new CreatorProjectDocument(
                CreatorProjectDocument.SCHEMA_VERSION, "native-widget-fixture", 1L,
                "Native Widget Fixture", "home", screens, widgets,
                CreatorEntryControl.defaultControl(), state, events);
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit()
                .putString("active_document", CreatorProjectDocumentCodec.encode(document))
                .commit();
    }

    private static MaterialButton requireButton(View root, String text) {
        MaterialButton button = findButton(root, text);
        if (button == null) throw new AssertionError("Runtime button not found: " + text);
        return button;
    }

    private static MaterialButton findButton(View root, String text) {
        if (root instanceof MaterialButton && text.contentEquals(((MaterialButton) root).getText())) {
            return (MaterialButton) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends View> T requireView(View root, Class<T> type) {
        T view = findView(root, type);
        if (view == null) throw new AssertionError("Runtime view not found: " + type.getSimpleName());
        return view;
    }

    private static <T extends View> T findView(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findView(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static long fixtureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2022);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
