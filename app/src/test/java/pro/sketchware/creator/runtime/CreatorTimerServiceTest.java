package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class CreatorTimerServiceTest {
    @Test public void schedulesOneShotTimerAndPublishesItsIdToTheRuntimeListener() throws Exception {
        CountDownLatch tick = new CountDownLatch(1);
        CreatorTimerService service = new CreatorTimerService(timerId -> {
            if ("once".equals(timerId)) tick.countDown();
        });

        CreatorRuntimeService.Result result = service.execute(arguments("once", "after", "10", null));

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(tick.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test public void replacesExistingTimerAndCancelsItByStableTimerId() {
        CreatorTimerService service = new CreatorTimerService(timerId -> { });
        assertThat(service.execute(arguments("repeat", "every", "1000", "1000")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);

        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("timerId", "repeat");
        cancel.put("action", "cancel");
        CreatorRuntimeService.Result result = service.execute(cancel);

        assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(result.getOutput()).containsEntry("cancelled", true);
    }

    @Test public void rejectsPeriodicTimerWithoutPositivePeriod() {
        CreatorTimerService service = new CreatorTimerService(timerId -> { });

        assertThat(service.execute(arguments("repeat", "every", "0", "0")).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    private static Map<String, Object> arguments(String timerId, String action, String delayMs, String periodMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timerId", timerId);
        result.put("action", action);
        result.put("delayMs", delayMs);
        if (periodMs != null) result.put("periodMs", periodMs);
        return result;
    }
}
