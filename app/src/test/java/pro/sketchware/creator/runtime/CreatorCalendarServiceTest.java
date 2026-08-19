package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collections;
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
}
