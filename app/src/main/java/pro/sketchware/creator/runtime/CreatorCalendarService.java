package pro.sketchware.creator.runtime;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native calendar operations corresponding to the legacy Calendar component. */
public final class CreatorCalendarService implements CreatorRuntimePlugin {
    @Override public String getId() { return "calendar"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = arguments.get("action") == null ? "now" : String.valueOf(arguments.get("action"));
        if (!"now".equals(action)) return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Unsupported calendar action: " + action);
        Calendar now = Calendar.getInstance();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("timestamp", now.getTimeInMillis());
        output.put("year", now.get(Calendar.YEAR));
        output.put("month", now.get(Calendar.MONTH) + 1);
        output.put("day", now.get(Calendar.DAY_OF_MONTH));
        return new Result(Status.SUCCEEDED, output, null);
    }
}
