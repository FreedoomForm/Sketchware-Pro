package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runtime-native scheduler for the legacy TimerTask component. */
public final class CreatorTimerService implements CreatorRuntimeService {
    public interface Listener { void onTick(String timerId); }
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    private final Map<String, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();
    public CreatorTimerService(Listener listener) { this.listener = listener; }
    @Override public String getId() { return "timer"; }
    @Override public Result execute(Map<String, Object> arguments) {
        String id = arguments.get("timerId") == null ? null : String.valueOf(arguments.get("timerId"));
        if (id == null || id.trim().isEmpty()) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "timerId is required.");
        String action = arguments.get("action") == null ? "schedule" : String.valueOf(arguments.get("action"));
        if ("cancel".equals(action)) {
            ScheduledFuture<?> existing = scheduled.remove(id);
            boolean cancelled = existing != null && existing.cancel(false);
            return new Result(Status.SUCCEEDED, CreatorRuntimeServiceArguments.output("timerId", id,
                    "cancelled", cancelled), null);
        }
        if (!("schedule".equals(action) || "after".equals(action) || "every".equals(action))) {
            return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Unsupported timer action: " + action);
        }
        try {
            long delay = Long.parseLong(String.valueOf(arguments.get("delayMs")));
            long period = arguments.get("periodMs") == null ? 0L : Long.parseLong(String.valueOf(arguments.get("periodMs")));
            if (delay < 0L || period < 0L) throw new NumberFormatException();
            if ("after".equals(action) && period != 0L) {
                return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "after does not allow periodMs.");
            }
            if ("every".equals(action) && period <= 0L) {
                return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "every requires positive periodMs.");
            }
            ScheduledFuture<?> prior = scheduled.remove(id);
            if (prior != null) prior.cancel(false);
            Runnable task = () -> {
                if (listener != null) listener.onTick(id);
                if (period <= 0L) scheduled.remove(id);
            };
            ScheduledFuture<?> future = period > 0L
                    ? scheduler.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS)
                    : scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
            scheduled.put(id, future);
            return new Result(Status.SUCCEEDED, CreatorRuntimeServiceArguments.output("timerId", id,
                    "action", action, "periodic", period > 0L), null);
        } catch (NumberFormatException error) {
            return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "delayMs and periodMs must be non-negative numbers.");
        }
    }
}
