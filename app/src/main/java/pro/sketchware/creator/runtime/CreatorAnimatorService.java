package pro.sketchware.creator.runtime;

import android.animation.ObjectAnimator;
import android.view.View;
import java.util.Map;

/** Runtime-native ObjectAnimator implementation bound to rendered Creator widgets. */
public final class CreatorAnimatorService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorAnimatorService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "animator"; }

    @Override public Result execute(Map<String, Object> arguments) {
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
}
