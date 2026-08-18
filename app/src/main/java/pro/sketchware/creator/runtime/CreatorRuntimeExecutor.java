package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes an attached event binding using only typed operations and visible effects. */
public final class CreatorRuntimeExecutor {
    private static final int MAX_EXECUTION_STEPS = 10_000;
    private static final int MAX_REPEAT_COUNT = 1_000;

    public static final class Effect {
        private final String type;
        private final String value;
        Effect(String type, String value) { this.type = type; this.value = value; }
        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private static final class ExecutionContext {
        int steps;
        boolean consume() { return ++steps <= MAX_EXECUTION_STEPS; }
    }

    private final CreatorRuntimeServiceDispatcher runtimeServices;

    public CreatorRuntimeExecutor() { this(null); }
    public CreatorRuntimeExecutor(CreatorRuntimeServiceDispatcher runtimeServices) { this.runtimeServices = runtimeServices; }

    public List<Effect> dispatch(CreatorRuntimeEngine engine, String targetWidgetId, String eventName) {
        if (engine == null) return Collections.emptyList();
        CreatorEventBinding binding = findBinding(engine.getCurrent(), targetWidgetId, eventName);
        if (binding == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        ExecutionContext context = new ExecutionContext();
        executeBlocks(engine, binding.getBlocks(), effects, context, 0);
        if (context.steps > MAX_EXECUTION_STEPS) {
            effects.add(new Effect("runtime_error", "execution budget exceeded"));
        }
        return Collections.unmodifiableList(effects);
    }

    /** @return true when the current loop should stop because a break was encountered. */
    private boolean executeBlocks(CreatorRuntimeEngine engine, List<CreatorRuntimeBlock> blocks,
                                  List<Effect> effects, ExecutionContext context, int loopDepth) {
        for (CreatorRuntimeBlock block : blocks) {
            if (!context.consume()) return false;
            Map<String, Object> payload = block.getPayload();
            if (block.getType() == CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY) {
                apply(engine, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY, map(
                        "widgetId", payload.get("widgetId"), "property", payload.get("property"), "value", payload.get("value")));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SET_STATE) {
                Object value = payload.containsKey("expression")
                        ? CreatorRuntimeExpression.evaluate(String.valueOf(payload.get("expression")), engine.getCurrent().getState())
                        : payload.get("value");
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", payload.get("stateId"), "value", value));
            } else if (block.getType() == CreatorRuntimeBlock.Type.STATE_INCREMENT) {
                String stateId = String.valueOf(payload.get("stateId"));
                Object current = engine.getCurrent().getState().get(stateId);
                double next = number(current) + number(payload.get("delta"));
                Object normalized = next == Math.rint(next) ? (long) next : next;
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", normalized));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SHOW_MESSAGE) {
                effects.add(new Effect("message", String.valueOf(payload.get("message"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.NAVIGATE) {
                effects.add(new Effect("navigate", String.valueOf(payload.get("screenId"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL) {
                executeService(engine, payload, effects);
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_STATE_EQUALS) {
                Object actual = engine.getCurrent().getState().get(String.valueOf(payload.get("stateId")));
                Object expected = payload.get("equals");
                boolean matches = expected == null ? actual == null : expected.equals(actual);
                if (executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(),
                        effects, context, loopDepth)) return true;
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_CONDITION) {
                boolean matches = CreatorRuntimeCondition.evaluate(payload, engine.getCurrent().getState());
                if (executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(),
                        effects, context, loopDepth)) return true;
            } else if (block.getType() == CreatorRuntimeBlock.Type.REPEAT) {
                int count = repeatCount(payload.get("count"), engine.getCurrent().getState());
                if (count < 0) {
                    effects.add(new Effect("runtime_error", "repeat count must be a non-negative integer"));
                    continue;
                }
                for (int i = 0; i < count; i++) {
                    if (executeBlocks(engine, block.getThenBlocks(), effects, context, loopDepth + 1)) break;
                    if (context.steps > MAX_EXECUTION_STEPS) break;
                }
            } else if (block.getType() == CreatorRuntimeBlock.Type.FOREVER) {
                while (context.steps <= MAX_EXECUTION_STEPS) {
                    if (executeBlocks(engine, block.getThenBlocks(), effects, context, loopDepth + 1)) break;
                }
            } else if (block.getType() == CreatorRuntimeBlock.Type.BREAK) {
                if (loopDepth > 0) return true;
                effects.add(new Effect("runtime_error", "break used outside a loop"));
            }
            if (context.steps > MAX_EXECUTION_STEPS) return false;
        }
        return false;
    }

    private void executeService(CreatorRuntimeEngine engine, Map<String, Object> payload, List<Effect> effects) {
        if (runtimeServices == null) {
            effects.add(new Effect("runtime_service", "unavailable"));
            return;
        }
        String serviceId = String.valueOf(payload.get("serviceId"));
        Object rawArguments = payload.get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = rawArguments instanceof Map
                ? (Map<String, Object>) rawArguments : Collections.<String, Object>emptyMap();
        CreatorRuntimeService.Result result = runtimeServices.dispatch(serviceId, arguments);
        effects.add(new Effect("runtime_service", serviceId + ":" + result.getStatus().name()));
    }

    private static double number(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0d; }
    }

    private static int repeatCount(Object raw, Map<String, Object> state) {
        Object value = raw;
        if (raw instanceof String) {
            String text = (String) raw;
            if (text.startsWith("state:")) value = state.get(text.substring("state:".length()));
            else if (text.startsWith("@")) value = state.get(text.substring(1));
        }
        try {
            long count = Long.parseLong(String.valueOf(value));
            return count < 0 || count > MAX_REPEAT_COUNT ? -1 : (int) count;
        } catch (NumberFormatException error) {
            return -1;
        }
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
