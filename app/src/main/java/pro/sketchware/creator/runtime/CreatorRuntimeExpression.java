package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Evaluates a deliberately small, typed expression language used by imported blocks. */
public final class CreatorRuntimeExpression {
    private static final Random RANDOM = new Random();
    private CreatorRuntimeExpression() { }

    public static Object evaluate(String expression, Map<String, Object> state) {
        if (expression == null) return null;
        return evaluateInternal(expression.trim(), state == null ? Collections.<String, Object>emptyMap() : state);
    }

    private static Object evaluateInternal(String expression, Map<String, Object> state) {
        String value = stripParentheses(expression);
        if (value.isEmpty()) return "";
        int index = findOperator(value, "||");
        if (index >= 0) return truthy(evaluateInternal(value.substring(0, index), state))
                || truthy(evaluateInternal(value.substring(index + 2), state));
        index = findOperator(value, "&&");
        if (index >= 0) return truthy(evaluateInternal(value.substring(0, index), state))
                && truthy(evaluateInternal(value.substring(index + 2), state));
        for (String operator : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
            index = findOperator(value, operator);
            if (index >= 0) {
                int comparison = compare(evaluateInternal(value.substring(0, index), state),
                        evaluateInternal(value.substring(index + operator.length()), state));
                return "==".equals(operator) ? comparison == 0 : "!=".equals(operator) ? comparison != 0
                        : ">=".equals(operator) ? comparison >= 0 : "<=".equals(operator) ? comparison <= 0
                        : ">".equals(operator) ? comparison > 0 : comparison < 0;
            }
        }
        for (String operator : new String[]{"+", "-"}) {
            index = findOperator(value, operator);
            if (index > 0) {
                Object left = evaluateInternal(value.substring(0, index), state);
                Object right = evaluateInternal(value.substring(index + 1), state);
                if ("+".equals(operator) && (left instanceof String || right instanceof String)) {
                    return String.valueOf(left) + String.valueOf(right);
                }
                return number(left) + ("+".equals(operator) ? number(right) : -number(right));
            }
        }
        for (String operator : new String[]{"*", "/", "%"}) {
            index = findOperator(value, operator);
            if (index > 0) {
                double left = number(evaluateInternal(value.substring(0, index), state));
                double right = number(evaluateInternal(value.substring(index + 1), state));
                if ("*".equals(operator)) return left * right;
                if ("/".equals(operator)) return right == 0 ? 0d : left / right;
                return right == 0 ? 0d : left % right;
            }
        }
        if (value.startsWith("!")) return !truthy(evaluateInternal(value.substring(1), state));
        if (value.startsWith("\"") && value.endsWith("\"")) return value.substring(1, value.length() - 1);
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        try { return value.contains(".") ? Double.parseDouble(value) : Long.parseLong(value); }
        catch (NumberFormatException ignored) { }
        if (value.startsWith("state:")) return state.get(value.substring(6));
        if (value.startsWith("@")) return state.get(value.substring(1));
        Object stateValue = state.get(value);
        if (stateValue != null || state.containsKey(value)) return stateValue;
        return function(value, state);
    }

    private static Object function(String expression, Map<String, Object> state) {
        int open = expression.indexOf('(');
        if (open < 1 || !expression.endsWith(")")) return expression;
        String name = expression.substring(0, open).trim().toLowerCase(Locale.ROOT);
        String[] args = splitArguments(expression.substring(open + 1, expression.length() - 1));
        if ("currenttime".equals(name)) return System.currentTimeMillis();
        if ("mathpi".equals(name)) return Math.PI;
        if ("mathe".equals(name)) return Math.E;
        if ("random".equals(name) && args.length >= 2) {
            long min = (long) number(evaluateInternal(args[0], state));
            long max = (long) number(evaluateInternal(args[1], state));
            return max <= min ? min : min + RANDOM.nextInt((int) Math.min(Integer.MAX_VALUE, max - min));
        }
        Object first = args.length == 0 ? null : evaluateInternal(args[0], state);
        if ("stringlength".equals(name)) return String.valueOf(first).length();
        if ("trim".equals(name)) return String.valueOf(first).trim();
        if ("touppercase".equals(name)) return String.valueOf(first).toUpperCase(Locale.ROOT);
        if ("tolowercase".equals(name)) return String.valueOf(first).toLowerCase(Locale.ROOT);
        if ("tonumber".equals(name)) return number(first);
        if ("tostring".equals(name) || "tostringwithdecimal".equals(name)) return String.valueOf(first);
        if ("stringjoin".equals(name) && args.length >= 2) return String.valueOf(first) + String.valueOf(evaluateInternal(args[1], state));
        if ("stringcontains".equals(name) && args.length >= 2) return String.valueOf(first).contains(String.valueOf(evaluateInternal(args[1], state)));
        if ("stringequals".equals(name) && args.length >= 2) return String.valueOf(first).equals(String.valueOf(evaluateInternal(args[1], state)));
        if ("mathabs".equals(name)) return Math.abs(number(first));
        if ("mathsqrt".equals(name)) return Math.sqrt(number(first));
        if ("mathceil".equals(name)) return Math.ceil(number(first));
        if ("mathfloor".equals(name)) return Math.floor(number(first));
        if ("mathround".equals(name)) return Math.round(number(first));
        if ("mathsin".equals(name)) return Math.sin(number(first));
        if ("mathcos".equals(name)) return Math.cos(number(first));
        if ("mathtan".equals(name)) return Math.tan(number(first));
        if ("mathexp".equals(name)) return Math.exp(number(first));
        if ("mathlog".equals(name)) return Math.log(number(first));
        if ("mathlog10".equals(name)) return Math.log10(number(first));
        if ("mathtoradian".equals(name)) return Math.toRadians(number(first));
        if ("mathtodegree".equals(name)) return Math.toDegrees(number(first));
        if ("mathpow".equals(name) && args.length >= 2) return Math.pow(number(first), number(evaluateInternal(args[1], state)));
        if ("mathmin".equals(name) && args.length >= 2) return Math.min(number(first), number(evaluateInternal(args[1], state)));
        if ("mathmax".equals(name) && args.length >= 2) return Math.max(number(first), number(evaluateInternal(args[1], state)));
        return expression;
    }

    private static int findOperator(String value, String operator) {
        int depth = 0;
        boolean quoted = false;
        for (int i = value.length() - operator.length(); i >= 0; i--) {
            char c = value.charAt(i);
            if (c == '"') quoted = !quoted;
            if (quoted) continue;
            if (c == ')') depth++;
            else if (c == '(') depth--;
            if (depth == 0 && value.startsWith(operator, i)) return i;
        }
        return -1;
    }

    private static String stripParentheses(String value) {
        while (value.length() >= 2 && value.startsWith("(") && value.endsWith(")")) {
            int depth = 0; boolean wraps = true; boolean quoted = false;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '"') quoted = !quoted;
                if (quoted) continue;
                if (c == '(') depth++;
                else if (c == ')' && --depth == 0 && i != value.length() - 1) { wraps = false; break; }
            }
            if (!wraps) break;
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static String[] splitArguments(String input) {
        if (input.trim().isEmpty()) return new String[0];
        java.util.List<String> values = new java.util.ArrayList<>();
        int start = 0; int depth = 0; boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (!quoted && c == '(') depth++;
            else if (!quoted && c == ')') depth--;
            else if (!quoted && c == ',' && depth == 0) { values.add(input.substring(start, i).trim()); start = i + 1; }
        }
        values.add(input.substring(start).trim());
        return values.toArray(new String[0]);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        return value != null && !String.valueOf(value).isEmpty() && !"false".equalsIgnoreCase(String.valueOf(value));
    }

    private static double number(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0d; }
    }

    private static int compare(Object left, Object right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        if (left instanceof Number || right instanceof Number) return Double.compare(number(left), number(right));
        return String.valueOf(left).compareTo(String.valueOf(right));
    }
}
