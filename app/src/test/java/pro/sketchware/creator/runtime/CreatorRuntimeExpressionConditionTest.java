package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CreatorRuntimeExpressionConditionTest {
    @Test public void evaluatesTypedArithmeticBooleanAndStringExpressions() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("count", 4L);
        state.put("name", "Sketchware");

        assertThat(CreatorRuntimeExpression.evaluate("count * 2 + 1", state)).isEqualTo(9d);
        assertThat(CreatorRuntimeExpression.evaluate("count >= 4 && name == \"Sketchware\"", state))
                .isEqualTo(true);
        assertThat(CreatorRuntimeExpression.evaluate("touppercase(name)", state)).isEqualTo("SKETCHWARE");
        assertThat(CreatorRuntimeExpression.evaluate("stringjoin(name, \" Runtime\")", state))
                .isEqualTo("Sketchware Runtime");
    }

    @Test public void handlesMalformedAndZeroDivisionExpressionsSafely() {
        Map<String, Object> state = new LinkedHashMap<>();
        assertThat(CreatorRuntimeExpression.evaluate("10 / 0", state)).isEqualTo(0d);
        assertThat(CreatorRuntimeExpression.evaluate("unknown_function(1)", state))
                .isEqualTo("unknown_function(1)");
        assertThat(CreatorRuntimeExpression.evaluate(null, state)).isNull();
    }

    @Test public void evaluatesNestedStateConditionsAndUnknownOperatorsAsFalse() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("enabled", true);
        state.put("count", 3L);

        Map<String, Object> equals = map("operator", "equals", "left", "state:count", "right", 3L);
        Map<String, Object> condition = map("operator", "and", "left", equals,
                "right", map("operator", "equals", "left", "enabled", "right", true));

        assertThat(CreatorRuntimeCondition.evaluate(condition, state)).isTrue();
        assertThat(CreatorRuntimeCondition.evaluate(map("operator", "not", "operand", condition), state))
                .isFalse();
        assertThat(CreatorRuntimeCondition.evaluate(map("operator", "future_operator"), state)).isFalse();
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
