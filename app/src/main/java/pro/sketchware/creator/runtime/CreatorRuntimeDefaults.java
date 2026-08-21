package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates the one-time starter surface shared by Creator Home, the live
 * native surface, and the original Sketchware editor projection.
 */
public final class CreatorRuntimeDefaults {
    public static final String STARTER_INITIALIZED_STATE = "creator.runtime.starter_initialized";
    public static final String ENTRY_WIDGET_ID = "creator_continue_button";
    public static final String EDITOR_INTENT_ID = "creator_editor";
    public static final String ENTRY_CLICK_BINDING_ID = "creator_continue_button_click";

    private CreatorRuntimeDefaults() { }

    /**
     * Adds the initial white-screen Continue button exactly once. The marker is
     * persisted so a user who removes or replaces the button is not overridden
     * on the next process start.
     */
    public static CreatorProjectDocument ensureStarterContent(CreatorProjectDocument document) {
        if (document == null || Boolean.TRUE.equals(document.getState().get(STARTER_INITIALIZED_STATE))) {
            return document;
        }

        Map<String, CreatorScreen> screens = new LinkedHashMap<>(document.getScreens());
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>(document.getWidgets());
        Map<String, Object> state = new LinkedHashMap<>(document.getState());
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>(document.getEvents());
        String entryScreenId = document.getEntryScreenId();

        if (screens.isEmpty()) {
            entryScreenId = "main";
            screens.put(entryScreenId, new CreatorScreen(entryScreenId, "/main", "root_main"));
            widgets.put("root_main", new CreatorWidget("root_main", "column", null,
                    Collections.<String>emptyList(), Collections.<String, Object>emptyMap()));
        }
        if (entryScreenId == null || !screens.containsKey(entryScreenId)) {
            entryScreenId = screens.keySet().iterator().next();
        }

        CreatorScreen entryScreen = screens.get(entryScreenId);
        String rootId = entryScreen.getRootWidgetId();
        CreatorWidget root = widgets.get(rootId);
        if (root == null) {
            root = new CreatorWidget(rootId, "column", null,
                    Collections.<String>emptyList(), Collections.<String, Object>emptyMap());
            widgets.put(rootId, root);
        }
        if (!widgets.containsKey(ENTRY_WIDGET_ID)) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("text", "Continue");
            properties.put("enabled", true);
            properties.put("clickable", true);
            properties.put("visible", true);
            properties.put("layout_width", "wrap_content");
            properties.put("layout_height", "wrap_content");
            widgets.put(ENTRY_WIDGET_ID, new CreatorWidget(ENTRY_WIDGET_ID, "button", rootId,
                    Collections.<String>emptyList(), properties));
            if (!root.getChildren().contains(ENTRY_WIDGET_ID)) {
                widgets.put(rootId, root.withChild(ENTRY_WIDGET_ID, -1));
            }
        }
        if (!hasClickBinding(events, ENTRY_WIDGET_ID)) {
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("intentId", EDITOR_INTENT_ID);
            arguments.put("action", "open_creator_editor");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("serviceId", "intent");
            payload.put("arguments", arguments);
            List<CreatorRuntimeBlock> blocks = new ArrayList<>();
            blocks.add(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL, payload));
            events.put(ENTRY_CLICK_BINDING_ID, new CreatorEventBinding(
                    ENTRY_CLICK_BINDING_ID, ENTRY_WIDGET_ID, "click", blocks));
        }
        state.put(STARTER_INITIALIZED_STATE, true);
        return new CreatorProjectDocument(document.getSchemaVersion(), document.getProjectId(),
                document.getRevision(), document.getName(), entryScreenId, screens, widgets,
                document.getEntryControl(), state, events);
    }

    private static boolean hasClickBinding(Map<String, CreatorEventBinding> events, String widgetId) {
        for (CreatorEventBinding binding : events.values()) {
            if (binding != null && widgetId.equals(binding.getTargetWidgetId())
                    && "click".equals(binding.getEventName())) return true;
        }
        return false;
    }
}

