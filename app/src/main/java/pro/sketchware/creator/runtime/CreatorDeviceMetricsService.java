package pro.sketchware.creator.runtime;

import android.util.TypedValue;
import java.util.Map;

/** Runtime-native display-metric queries used by canonical Sketchware reporters. */
public final class CreatorDeviceMetricsService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;

    public CreatorDeviceMetricsService(CreatorRuntimeEnvironment environment) {
        if (environment == null) throw new IllegalArgumentException("environment");
        this.environment = environment;
    }

    @Override public String getId() { return "device_metrics"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("display_width".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("value",
                    environment.getContext().getResources().getDisplayMetrics().widthPixels);
        }
        if ("display_height".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("value",
                    environment.getContext().getResources().getDisplayMetrics().heightPixels);
        }
        if ("dip".equals(action)) {
            Object rawInput = arguments.get("input");
            int input = rawInput instanceof Number ? ((Number) rawInput).intValue() : parseInt(rawInput);
            float value = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input,
                    environment.getContext().getResources().getDisplayMetrics());
            return CreatorRuntimeServiceArguments.succeeded("value", value);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported device metrics action: " + action);
    }

    private static int parseInt(Object rawInput) {
        try { return rawInput == null ? 0 : (int) Double.parseDouble(String.valueOf(rawInput)); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
