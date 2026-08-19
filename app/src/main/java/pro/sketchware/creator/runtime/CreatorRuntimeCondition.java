package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.Map;

/** Evaluates only typed, data-backed conditions; it never executes source code. */
public final class CreatorRuntimeCondition {
    private CreatorRuntimeCondition() { }

    public static boolean evaluate(Map<String, Object> condition, Map<String, Object> state) {
        if (condition == null) return false;
        String operator = string(condition.get("operator"));
        if (operator == null) return false;
        Map<String, Object> safeState = state == null ? Collections.<String, Object>emptyMap() : state;
        switch (operator) {
            case "true": return true;
            case "false": return false;
            case "not": return !evaluate(asMap(condition.get("operand")), safeState);
            case "and": return evaluate(asMap(condition.get("left")), safeState)
                    && evaluate(asMap(condition.get("right")), safeState);
            case "or": return evaluate(asMap(condition.get("left")), safeState)
                    || evaluate(asMap(condition.get("right")), safeState);
            case "equals": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) == 0;
            case "not_equals": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) != 0;
            case "greater": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) > 0;
            case "greater_or_equal": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) >= 0;
            case "less": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) < 0;
            case "less_or_equal": return compare(resolve(condition.get("left"), safeState),
                    resolve(condition.get("right"), safeState)) <= 0;
            default: return false;
        }
    }

    private static Object resolve(Object raw, Map<String, Object> state) {
        if (raw instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) raw;
            Object stateId = map.get("stateId");
            if (stateId != null) return state.get(String.valueOf(stateId));
            if (map.containsKey("value")) return map.get("value");
        }
        if (raw instanceof String) {
            String value = (String) raw;
            if (value.startsWith("state:")) return state.get(value.substring("state:".length()));
            if (value.startsWith("@")) return state.get(value.substring(1));
            if (state.containsKey(value)) return state.get(value);
            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        }
        return raw;
    }

    private static int compare(Object left, Object right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT);
    }
}
