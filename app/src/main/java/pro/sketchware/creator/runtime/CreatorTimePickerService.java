package pro.sketchware.creator.runtime;

import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Map;

/** Runtime-native TimePickerDialog service. */
public final class CreatorTimePickerService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorTimePickerService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "time_picker"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action != null && !"show".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported time picker action: " + action);
        Calendar now = Calendar.getInstance();
        try {
            int hour = (int) CreatorRuntimeServiceArguments.longValue(arguments, "hour", now.get(Calendar.HOUR_OF_DAY));
            int minute = (int) CreatorRuntimeServiceArguments.longValue(arguments, "minute", now.get(Calendar.MINUTE));
            boolean is24Hour = !"false".equals(CreatorRuntimeServiceArguments.string(arguments, "is24Hour"));
            environment.getActivity().runOnUiThread(() -> new TimePickerDialog(environment.getActivity(), (view, h, m) ->
                    environment.publish(getId(), "selected", CreatorRuntimeServiceArguments.output(
                            "hour", h, "minute", m)), hour, minute, is24Hour).show());
            return CreatorRuntimeServiceArguments.succeeded("shown", true);
        } catch (RuntimeException error) {
            return CreatorRuntimeServiceArguments.invalid("hour and minute must be valid numbers.");
        }
    }
}
