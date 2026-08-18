package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runtime-native scheduler for the legacy TimerTask component. */
public final class CreatorTimerService implements CreatorRuntimeService {
    public interface Listener { void onTick(String timerId); }
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    public CreatorTimerService(Listener listener) { this.listener = listener; }
    @Override public String getId() { return "timer"; }
    @Override public Result execute(Map<String, Object> arguments) {
        String id = arguments.get("timerId") == null ? null : String.valueOf(arguments.get("timerId"));
        if (id == null || id.trim().isEmpty()) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "timerId is required.");
        try {
            long delay = Long.parseLong(String.valueOf(arguments.get("delayMs")));
            long period = arguments.get("periodMs") == null ? 0L : Long.parseLong(String.valueOf(arguments.get("periodMs")));
            if (delay < 0L || period < 0L) throw new NumberFormatException();
            Runnable task = () -> { if (listener != null) listener.onTick(id); };
            if (period > 0L) scheduler.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
            else scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
            return new Result(Status.SUCCEEDED, Collections.singletonMap("timerId", id), null);
        } catch (NumberFormatException error) {
            return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "delayMs and periodMs must be non-negative numbers.");
        }
    }
}
