package pro.sketchware.creator.runtime;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Collections;
import java.util.Map;

/** Runtime implementation of the legacy Vibrator component. */
public final class CreatorVibratorPlugin implements CreatorRuntimePlugin {
    private final Vibrator vibrator;

    public CreatorVibratorPlugin(Context context) {
        if (context == null) throw new IllegalArgumentException("context");
        vibrator = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override public String getId() { return "vibrator"; }

    @Override public Result execute(Map<String, Object> arguments) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return new Result(Status.FAILED, Collections.emptyMap(), "Vibration hardware is unavailable.");
        }
        Object duration = arguments.get("durationMs");
        long durationMs;
        try { durationMs = duration == null ? 40L : Long.parseLong(String.valueOf(duration)); }
        catch (NumberFormatException error) { return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "durationMs must be numeric."); }
        if (durationMs <= 0L || durationMs > 10_000L) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "durationMs must be between 1 and 10000.");
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        return new Result(Status.SUCCEEDED, Collections.singletonMap("durationMs", durationMs), null);
    }
}
