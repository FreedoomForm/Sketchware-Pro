package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes an attached event binding using only typed operations and visible effects. */
public final class CreatorRuntimeExecutor {
    public static final class Effect {
        private final String type;
        private final String value;
        Effect(String type, String value) { this.type = type; this.value = value; }
        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private final CreatorRuntimePluginDispatcher runtimeServices;

    public CreatorRuntimeExecutor() { this(null); }
    public CreatorRuntimeExecutor(CreatorRuntimePluginDispatcher runtimeServices) { this.runtimeServices = runtimeServices; }

    public List<Effect> dispatch(CreatorRuntimeEngine engine, String targetWidgetId, String eventName) {
        if (engine == null) return Collections.emptyList();
        CreatorEventBinding binding = findBinding(engine.getCurrent(), targetWidgetId, eventName);
        if (binding == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        for (CreatorRuntimeBlock block : binding.getBlocks()) {
            Map<String, Object> payload = block.getPayload();
            if (block.getType() == CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY) {
                apply(engine, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY, map(
                        "widgetId", payload.get("widgetId"), "property", payload.get("property"), "value", payload.get("value")));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SET_STATE) {
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", payload.get("stateId"), "value", payload.get("value")));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SHOW_MESSAGE) {
                effects.add(new Effect("message", String.valueOf(payload.get("message"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.NAVIGATE) {
                effects.add(new Effect("navigate", String.valueOf(payload.get("screenId"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL) {
                if (runtimeServices == null) effects.add(new Effect("runtime_service", "unavailable"));
                else {
                    String serviceId = String.valueOf(payload.get("serviceId"));
                    Object rawArguments = payload.get("arguments");
                    @SuppressWarnings("unchecked") Map<String, Object> arguments = rawArguments instanceof Map
                            ? (Map<String, Object>) rawArguments : Collections.<String, Object>emptyMap();
                    CreatorRuntimePlugin.Result result = runtimeServices.dispatch(serviceId, arguments);
                    effects.add(new Effect("runtime_service", serviceId + ":" + result.getStatus().name()));
                }
            }
        }
        return Collections.unmodifiableList(effects);
    }

    private CreatorEventBinding findBinding(CreatorProjectDocument document, String targetWidgetId, String eventName) {
        for (CreatorEventBinding binding : document.getEvents().values()) {
            if (binding.getTargetWidgetId().equals(targetWidgetId) && binding.getEventName().equals(eventName)) return binding;
        }
        return null;
    }

    private void apply(CreatorRuntimeEngine engine, CreatorProjectOperation.Type type, Map<String, Object> payload) {
        CreatorProjectDocument document = engine.getCurrent();
        engine.apply(new CreatorProjectOperation("runtime-" + UUID.randomUUID(), document.getProjectId(),
                document.getRevision(), CreatorProjectOperation.ActorKind.SYSTEM, type, payload, System.currentTimeMillis()));
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
