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

    @Test public void formatsComponentScopedCalendarState() {
        CreatorCalendarService service = new CreatorCalendarService();
        Map<String, Object> setTime = new LinkedHashMap<>();
        setTime.put("componentId", "calendar1");
        setTime.put("action", "set_time");
        setTime.put("timestamp", 12345L);
        service.execute(setTime);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("componentId", "calendar1");
        format.put("action", "format");
        format.put("pattern", "yyyy");
        CreatorRuntimeService.Result result = service.execute(format);

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(result.getOutput()).containsKey("formatted");
        assertThat(String.valueOf(result.getOutput().get("formatted"))).hasLength(4);
    }

    @Test public void computesDifferenceBetweenComponentScopedCalendars() {
        CreatorCalendarService service = new CreatorCalendarService();
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("componentId", "first"); first.put("action", "set_time"); first.put("timestamp", 1000L);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("componentId", "second"); second.put("action", "set_time"); second.put("timestamp", 4000L);
        service.execute(first); service.execute(second);

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("componentId", "second"); diff.put("action", "diff"); diff.put("otherComponentId", "first");
        CreatorRuntimeService.Result result = service.execute(diff);

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(result.getOutput().get("differenceMs")).isEqualTo(3000L);
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

    @Test public void getTimeQueryPreservesExistingComponentScopedState() {
        CreatorCalendarService service = new CreatorCalendarService();
        Map<String, Object> setTime = new LinkedHashMap<>();
        setTime.put("componentId", "calendar1");
        setTime.put("action", "set_time");
        setTime.put("timestamp", 12345L);
        service.execute(setTime);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("componentId", "calendar1");
        query.put("action", "get_time");
        CreatorRuntimeService.Result result = service.execute(query);

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(result.getOutput().get("timestamp")).isEqualTo(12345L);
    }
}
