package pro.sketchware.creator.runtime;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native ObjectAnimator implementation bound to rendered Creator widgets. */
public final class CreatorAnimatorService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, Configuration> configurations = new LinkedHashMap<>();
    public CreatorAnimatorService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "animator"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action != null) return executeConfigured(action, arguments);
        return executeImmediate(arguments);
    }

    private Result executeImmediate(Map<String, Object> arguments) {
        String widgetId = CreatorRuntimeServiceArguments.string(arguments, "widgetId");
        String property = CreatorRuntimeServiceArguments.string(arguments, "property");
        if (widgetId == null || property == null) {
            return CreatorRuntimeServiceArguments.invalid("animator requires widgetId and property.");
        }
        View target = environment.findWidget(widgetId);
        if (target == null) return CreatorRuntimeServiceArguments.invalid("No rendered widget exists for " + widgetId + ".");
        try {
            float from = CreatorRuntimeServiceArguments.floatValue(arguments, "from", 0f);
            float to = CreatorRuntimeServiceArguments.floatValue(arguments, "to", 1f);
            long duration = CreatorRuntimeServiceArguments.longValue(arguments, "durationMs", 300L);
            if (duration < 0L || duration > 60_000L) return CreatorRuntimeServiceArguments.invalid("durationMs must be between 0 and 60000.");
            environment.getActivity().runOnUiThread(() -> {
                ObjectAnimator animator = ObjectAnimator.ofFloat(target, property, from, to);
                animator.setDuration(duration);
                animator.start();
            });
            return CreatorRuntimeServiceArguments.succeeded("widgetId", widgetId, "property", property,
                    "from", from, "to", to, "durationMs", duration);
        } catch (NumberFormatException error) {
            return CreatorRuntimeServiceArguments.invalid("from, to, and durationMs must be numeric.");
        }
    }

    private synchronized Result executeConfigured(String action, Map<String, Object> arguments) {
        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
        if (componentId == null || componentId.trim().isEmpty()) {
            return CreatorRuntimeServiceArguments.invalid("animator configuration requires componentId.");
        }
        Configuration configuration = configurations.get(componentId);
        if (configuration == null) {
            configuration = new Configuration();
            configurations.put(componentId, configuration);
        }
        try {
            if ("set_target".equals(action)) {
                String widgetId = CreatorRuntimeServiceArguments.string(arguments, "widgetId");
                if (widgetId == null) return CreatorRuntimeServiceArguments.invalid("set_target requires widgetId.");
                configuration.widgetId = widgetId;
            } else if ("set_property".equals(action)) {
                String property = CreatorRuntimeServiceArguments.string(arguments, "property");
                if (property == null) return CreatorRuntimeServiceArguments.invalid("set_property requires property.");
                configuration.property = property;
            } else if ("set_value".equals(action)) {
                configuration.to = CreatorRuntimeServiceArguments.floatValue(arguments, "value", 0f);
                configuration.hasFrom = false;
            } else if ("set_from_to".equals(action)) {
                configuration.from = CreatorRuntimeServiceArguments.floatValue(arguments, "from", 0f);
                configuration.to = CreatorRuntimeServiceArguments.floatValue(arguments, "to", 1f);
                configuration.hasFrom = true;
            } else if ("set_duration".equals(action)) {
                long duration = CreatorRuntimeServiceArguments.longValue(arguments, "durationMs", 300L);
                if (duration < 0L || duration > 60_000L) return CreatorRuntimeServiceArguments.invalid("durationMs must be between 0 and 60000.");
                configuration.durationMs = duration;
            } else if ("set_repeat_mode".equals(action)) {
                String mode = CreatorRuntimeServiceArguments.string(arguments, "repeatMode");
                configuration.repeatMode = "REVERSE".equalsIgnoreCase(mode) ? ValueAnimator.REVERSE : ValueAnimator.RESTART;
            } else if ("set_repeat_count".equals(action)) {
                long repeatCount = CreatorRuntimeServiceArguments.longValue(arguments, "repeatCount", 0L);
                if (repeatCount < ValueAnimator.INFINITE || repeatCount > 1_000L) {
                    return CreatorRuntimeServiceArguments.invalid("repeatCount must be between -1 and 1000.");
                }
                configuration.repeatCount = (int) repeatCount;
            } else if ("set_interpolator".equals(action)) {
                configuration.interpolator = interpolator(CreatorRuntimeServiceArguments.string(arguments, "interpolator"));
            } else if ("start".equals(action)) {
                return start(componentId, configuration);
            } else if ("cancel".equals(action)) {
                final ObjectAnimator active = configuration.active;
                if (active != null) environment.getActivity().runOnUiThread(active::cancel);
                return CreatorRuntimeServiceArguments.succeeded("cancelled", true, "componentId", componentId);
            } else if ("is_running".equals(action)) {
                return CreatorRuntimeServiceArguments.succeeded("value", configuration.active != null && configuration.active.isRunning());
            } else {
                return CreatorRuntimeServiceArguments.invalid("Unsupported animator action: " + action);
            }
            return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
        } catch (NumberFormatException error) {
            return CreatorRuntimeServiceArguments.invalid("Animator numeric values are invalid.");
        }
    }

    private Result start(String componentId, Configuration configuration) {
        if (configuration.widgetId == null || configuration.property == null) {
            return CreatorRuntimeServiceArguments.invalid("Animator start requires a configured target and property.");
        }
        View target = environment.findWidget(configuration.widgetId);
        if (target == null) return CreatorRuntimeServiceArguments.invalid("No rendered widget exists for " + configuration.widgetId + ".");
        environment.getActivity().runOnUiThread(() -> {
            ObjectAnimator animator = configuration.hasFrom
                    ? ObjectAnimator.ofFloat(target, configuration.property, configuration.from, configuration.to)
                    : ObjectAnimator.ofFloat(target, configuration.property, configuration.to);
            animator.setDuration(configuration.durationMs);
            animator.setRepeatMode(configuration.repeatMode);
            animator.setRepeatCount(configuration.repeatCount);
            animator.setInterpolator(configuration.interpolator);
            configuration.active = animator;
            animator.start();
        });
        return CreatorRuntimeServiceArguments.succeeded("started", true, "componentId", componentId);
    }

    private static TimeInterpolator interpolator(String value) {
        if ("Accelerate".equals(value)) return new AccelerateInterpolator();
        if ("Decelerate".equals(value)) return new DecelerateInterpolator();
        if ("AccelerateDeccelerate".equals(value) || "AccelerateDecelerate".equals(value)) return new AccelerateDecelerateInterpolator();
        if ("Bounce".equals(value)) return new BounceInterpolator();
        return new LinearInterpolator();
    }

    private static final class Configuration {
        String widgetId;
        String property;
        float from;
        float to = 1f;
        boolean hasFrom;
        long durationMs = 300L;
        int repeatMode = ValueAnimator.RESTART;
        int repeatCount;
        TimeInterpolator interpolator = new LinearInterpolator();
        ObjectAnimator active;
    }
}
