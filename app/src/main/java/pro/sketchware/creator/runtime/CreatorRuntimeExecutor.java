package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes an attached event binding using only typed operations and visible effects. */
public final class CreatorRuntimeExecutor {
    private static final int MAX_REPEAT_ITERATIONS = 10_000;
    public static final class Effect {
        private final String type;
        private final String value;
        Effect(String type, String value) { this.type = type; this.value = value; }
        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private final CreatorRuntimeServiceDispatcher runtimeServices;

    public CreatorRuntimeExecutor() { this(null); }
    public CreatorRuntimeExecutor(CreatorRuntimeServiceDispatcher runtimeServices) { this.runtimeServices = runtimeServices; }

    public List<Effect> dispatch(CreatorRuntimeEngine engine, String targetWidgetId, String eventName) {
        if (engine == null) return Collections.emptyList();
        CreatorEventBinding binding = findBinding(engine.getCurrent(), targetWidgetId, eventName);
        if (binding == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        executeBlocks(engine, binding.getBlocks(), effects);
        return Collections.unmodifiableList(effects);
    }

    private void executeBlocks(CreatorRuntimeEngine engine, List<CreatorRuntimeBlock> blocks, List<Effect> effects) {
        for (CreatorRuntimeBlock block : blocks) {
            Map<String, Object> payload = block.getPayload();
            if (block.getType() == CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY) {
                apply(engine, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY, map(
                        "widgetId", payload.get("widgetId"), "property", payload.get("property"), "value", payload.get("value")));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SET_STATE) {
                Object value = payload.containsKey("expression") ? evaluate(payload.get("expression"), engine) : payload.get("value");
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", payload.get("stateId"), "value", value));
            } else if (block.getType() == CreatorRuntimeBlock.Type.INCREMENT_STATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                Object rawCurrent = engine.getCurrent().getState().get(stateId);
                long current = number(rawCurrent);
                long delta = number(payload.get("delta"));
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", current + delta));
            } else if (block.getType() == CreatorRuntimeBlock.Type.LIST_MUTATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                java.util.List<Object> list = list(engine.getCurrent().getState().get(stateId));
                String action = String.valueOf(payload.get("action"));
                if ("add".equals(action)) list.add(payload.get("value"));
                else if ("insert".equals(action)) {
                    int index = (int) number(payload.get("index"));
                    if (index >= 0 && index <= list.size()) list.add(index, payload.get("value"));
                } else if ("remove_at".equals(action)) {
                    int index = (int) number(payload.get("index"));
                    if (index >= 0 && index < list.size()) list.remove(index);
                } else if ("clear".equals(action)) list.clear();
                else if ("add_all".equals(action)) list.addAll(list(engine.getCurrent().getState().get(
                        String.valueOf(payload.get("sourceStateId")))));
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", list));
            } else if (block.getType() == CreatorRuntimeBlock.Type.MAP_MUTATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                Map<String, Object> values = map(engine.getCurrent().getState().get(stateId));
                String action = String.valueOf(payload.get("action"));
                if ("put".equals(action)) values.put(String.valueOf(payload.get("key")), payload.get("value"));
                else if ("remove".equals(action)) values.remove(String.valueOf(payload.get("key")));
                else if ("clear".equals(action) || "create".equals(action)) values.clear();
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", values));
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
                    CreatorRuntimeService.Result result = runtimeServices.dispatch(serviceId, resolveServiceArguments(engine, arguments));
                    effects.add(new Effect("runtime_service", serviceId + ":" + result.getStatus().name()));
                }
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_STATE_EQUALS) {
                String stateId = String.valueOf(payload.get("stateId"));
                Object actual = engine.getCurrent().getState().get(stateId);
                Object expected = payload.get("equals");
                boolean matches = expected == null ? actual == null : expected.equals(actual);
                executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_BOOLEAN) {
                boolean matches;
                if (payload.containsKey("expression")) matches = booleanValue(evaluate(payload.get("expression"), engine));
                else if (payload.containsKey("constant")) matches = Boolean.TRUE.equals(payload.get("constant"));
                else matches = Boolean.TRUE.equals(engine.getCurrent().getState().get(String.valueOf(payload.get("stateId"))));
                executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
            } else if (block.getType() == CreatorRuntimeBlock.Type.REPEAT) {
                long requested = payload.containsKey("countExpression") ? number(evaluate(payload.get("countExpression"), engine))
                        : payload.containsKey("countStateId")
                        ? number(engine.getCurrent().getState().get(String.valueOf(payload.get("countStateId"))))
                        : number(payload.get("count"));
                int count = (int) Math.max(0L, Math.min(MAX_REPEAT_ITERATIONS, requested));
                if (requested > MAX_REPEAT_ITERATIONS) effects.add(new Effect("repeat", "capped:" + MAX_REPEAT_ITERATIONS));
                for (int iteration = 0; iteration < count; iteration++) executeBlocks(engine, block.getThenBlocks(), effects);
            }
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

    private static Map<String, Object> resolveServiceArguments(CreatorRuntimeEngine engine, Map<String, Object> arguments) {
        Map<String, Object> resolved = new LinkedHashMap<>(arguments);
        resolveStateMap(engine, resolved, "paramsStateId", "params");
        resolveStateMap(engine, resolved, "headersStateId", "headers");
        return resolved;
    }

    private static void resolveStateMap(CreatorRuntimeEngine engine, Map<String, Object> arguments, String referenceKey, String valueKey) {
        Object reference = arguments.get(referenceKey);
        if (reference == null) return;
        Object value = engine.getCurrent().getState().get(String.valueOf(reference));
        if (value instanceof Map) arguments.put(valueKey, value);
    }

    @SuppressWarnings("unchecked")
    private static Object evaluate(Object rawExpression, CreatorRuntimeEngine engine) {
        if (!(rawExpression instanceof Map)) return rawExpression;
        Map<String, Object> expression = (Map<String, Object>) rawExpression;
        if ("literal".equals(expression.get("kind"))) {
            String literal = String.valueOf(expression.get("value"));
            if (engine.getCurrent().getState().containsKey(literal)) return engine.getCurrent().getState().get(literal);
            if ("true".equalsIgnoreCase(literal) || "false".equalsIgnoreCase(literal)) return Boolean.valueOf(literal);
            try { return Double.valueOf(literal); } catch (NumberFormatException ignored) { return literal; }
        }
        if (!"reporter".equals(expression.get("kind"))) return null;
        String op = String.valueOf(expression.get("opCode"));
        List<Object> values = new ArrayList<>();
        Object rawArguments = expression.get("arguments");
        if (rawArguments instanceof List) for (Object argument : (List<?>) rawArguments) values.add(evaluate(argument, engine));
        Object first = values.isEmpty() ? null : values.get(0);
        Object second = values.size() < 2 ? null : values.get(1);
        if ("true".equals(op)) return true;
        if ("false".equals(op)) return false;
        if ("not".equals(op)) return !booleanValue(first);
        if ("&&".equals(op)) return booleanValue(first) && booleanValue(second);
        if ("||".equals(op)) return booleanValue(first) || booleanValue(second);
        if ("=".equals(op) || "stringequals".equals(op)) return first == null ? second == null : first.toString().equals(String.valueOf(second));
        if (">".equals(op)) return decimal(first) > decimal(second);
        if ("<".equals(op)) return decimal(first) < decimal(second);
        if ("+".equals(op)) return decimal(first) + decimal(second);
        if ("-".equals(op)) return decimal(first) - decimal(second);
        if ("*".equals(op)) return decimal(first) * decimal(second);
        if ("/".equals(op)) return decimal(second) == 0d ? 0d : decimal(first) / decimal(second);
        if ("%".equals(op)) return decimal(second) == 0d ? 0d : decimal(first) % decimal(second);
        if ("stringlength".equals(op)) return first == null ? 0d : (double) String.valueOf(first).length();
        if ("stringjoin".equals(op)) return String.valueOf(first) + String.valueOf(second);
        if ("stringcontains".equals(op)) return first != null && String.valueOf(first).contains(String.valueOf(second));
        if ("trim".equals(op)) return first == null ? "" : String.valueOf(first).trim();
        if ("touppercase".equals(op)) return first == null ? "" : String.valueOf(first).toUpperCase(java.util.Locale.ROOT);
        if ("tolowercase".equals(op)) return first == null ? "" : String.valueOf(first).toLowerCase(java.util.Locale.ROOT);
        if ("tonumber".equals(op)) return decimal(first);
        return null;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static double decimal(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ignored) { return 0d; }
    }

    private static long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value == null) return 0L;
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private static java.util.List<Object> list(Object value) {
        java.util.List<Object> result = new ArrayList<>();
        if (value instanceof java.util.List) result.addAll((java.util.List<?>) value);
        return result;
    }

    private static Map<String, Object> map(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }
}
