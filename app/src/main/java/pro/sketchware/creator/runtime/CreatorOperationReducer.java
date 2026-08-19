package pro.sketchware.creator.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/** Applies an already-validated operation without touching Android UI or disk. */
public final class CreatorOperationReducer {
    private CreatorOperationReducer() { }

    @SuppressWarnings("unchecked")
    public static CreatorProjectDocument reduce(CreatorProjectDocument document,
                                                CreatorProjectOperation operation) {
        long nextRevision = document.getRevision() + 1;
        Map<String, CreatorScreen> screens = new LinkedHashMap<>(document.getScreens());
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>(document.getWidgets());
        Map<String, Object> state = new LinkedHashMap<>(document.getState());
        Map<String, CreatorEventBinding> events = new LinkedHashMap<>(document.getEvents());
        CreatorEntryControl entryControl = document.getEntryControl();
        String entryScreenId = document.getEntryScreenId();
        Map<String, Object> payload = operation.getPayload();

        switch (operation.getType()) {
            case SCREEN_CREATE: {
                String screenId = (String) payload.get("screenId");
                String rootWidgetId = (String) payload.get("rootWidgetId");
                String route = (String) payload.get("route");
                String rootWidgetType = payload.get("rootWidgetType") instanceof String
                        ? (String) payload.get("rootWidgetType") : "column";
                screens.put(screenId, new CreatorScreen(screenId, route, rootWidgetId));
                widgets.put(rootWidgetId, new CreatorWidget(rootWidgetId, rootWidgetType, null, null, null));
                if (entryScreenId == null) entryScreenId = screenId;
                break;
            }
            case WIDGET_ADD: {
                String widgetId = (String) payload.get("widgetId");
                String parentId = (String) payload.get("parentId");
                String type = (String) payload.get("widgetType");
                Map<String, Object> properties = payload.get("properties") instanceof Map
                        ? (Map<String, Object>) payload.get("properties") : null;
                int index = payload.get("index") instanceof Number
                        ? ((Number) payload.get("index")).intValue() : -1;
                widgets.put(widgetId, new CreatorWidget(widgetId, type, parentId, null, properties));
                widgets.put(parentId, widgets.get(parentId).withChild(widgetId, index));
                break;
            }
            case WIDGET_SET_PROPERTY: {
                String widgetId = (String) payload.get("widgetId");
                widgets.put(widgetId, widgets.get(widgetId).withProperty(
                        (String) payload.get("property"), payload.get("value")));
                break;
            }
            case ENTRY_CONTROL_UPDATE: {
                Boolean visible = payload.get("visible") instanceof Boolean ? (Boolean) payload.get("visible") : null;
                String label = payload.get("label") instanceof String ? (String) payload.get("label") : null;
                String placement = payload.get("placement") instanceof String ? (String) payload.get("placement") : null;
                entryControl = entryControl.withValues(visible, label, placement);
                break;
            }
            case STATE_SET:
                state.put((String) payload.get("stateId"), payload.get("value"));
                break;
            case EVENT_ATTACH:
                events.put((String) payload.get("bindingId"), new CreatorEventBinding(
                        (String) payload.get("bindingId"), (String) payload.get("targetWidgetId"),
                        (String) payload.get("eventName"),
                        (java.util.List<CreatorRuntimeBlock>) payload.get("blocks")));
                break;
            case REVISION_RESTORE:
                throw new IllegalArgumentException("revision restore is handled by CreatorRuntimeEngine");
            default:
                throw new IllegalArgumentException("unsupported operation type");
        }
        return new CreatorProjectDocument(document.getSchemaVersion(), document.getProjectId(), nextRevision,
                document.getName(), entryScreenId, screens, widgets, entryControl, state, events);
    }
}
