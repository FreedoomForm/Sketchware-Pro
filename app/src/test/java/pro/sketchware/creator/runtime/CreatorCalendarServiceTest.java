package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class CreatorCalendarServiceTest {
    @Test public void returnsTimestampAndCalendarFieldsForNowAction() {
        CreatorRuntimeService.Result result = new CreatorCalendarService().execute(Collections.singletonMap("action", "now"));

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(result.getOutput()).containsKey("timestamp");
        assertThat(result.getOutput()).containsKey("year");
        assertThat(result.getOutput()).containsKey("month");
        assertThat(result.getOutput()).containsKey("day");
        assertThat(((Number) result.getOutput().get("timestamp")).longValue()).isGreaterThan(0L);
        assertThat(((Number) result.getOutput().get("month")).intValue()).isAtLeast(1);
        assertThat(((Number) result.getOutput().get("month")).intValue()).isAtMost(12);
    }

    @Test public void preservesCalendarComponentStateAcrossMutationActions() {
        CreatorCalendarService service = new CreatorCalendarService();
        Map<String, Object> setTime = new LinkedHashMap<>();
        setTime.put("componentId", "calendar1");
        setTime.put("action", "set_time");
        setTime.put("timestamp", "1735689600000");
        CreatorRuntimeService.Result initial = service.execute(setTime);

        Map<String, Object> add = new LinkedHashMap<>();
        add.put("componentId", "calendar1");
        add.put("action", "add");
        add.put("field", "Calendar.DAY_OF_MONTH");
        add.put("value", "1");
        CreatorRuntimeService.Result next = service.execute(add);

        assertThat(initial.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(next.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(next.getOutput()).containsEntry("year", 2025);
        assertThat(next.getOutput()).containsEntry("month", 1);
        assertThat(next.getOutput()).containsEntry("day", 2);
    }

    @Test public void rejectsUnknownCalendarFields() {
        CreatorCalendarService service = new CreatorCalendarService();
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("componentId", "calendar1");
        arguments.put("action", "set");
        arguments.put("field", "NOT_A_FIELD");
        arguments.put("value", "1");
        assertThat(service.execute(arguments).getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }
}
