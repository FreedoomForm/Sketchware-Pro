package pro.sketchware.creator.runtime;

import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Map;

/** Runtime-native DatePickerDialog service. */
public final class CreatorDatePickerService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorDatePickerService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "date_picker"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action != null && !"show".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported date picker action: " + action);
        Calendar now = Calendar.getInstance();
        try {
            int year = (int) CreatorRuntimeServiceArguments.longValue(arguments, "year", now.get(Calendar.YEAR));
            int month = (int) CreatorRuntimeServiceArguments.longValue(arguments, "month", now.get(Calendar.MONTH) + 1) - 1;
            int day = (int) CreatorRuntimeServiceArguments.longValue(arguments, "day", now.get(Calendar.DAY_OF_MONTH));
            environment.getActivity().runOnUiThread(() -> new DatePickerDialog(environment.getActivity(), (view, y, m, d) ->
                    environment.publish(getId(), "selected", CreatorRuntimeServiceArguments.output(
                            "year", y, "month", m + 1, "day", d)), year, month, day).show());
            return CreatorRuntimeServiceArguments.succeeded("shown", true);
        } catch (RuntimeException error) {
            return CreatorRuntimeServiceArguments.invalid("year, month, and day must be valid numbers.");
        }
    }
}
