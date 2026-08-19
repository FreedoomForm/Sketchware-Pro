package pro.sketchware.creator.runtime;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native calendar operations corresponding to the legacy Calendar component. */
public final class CreatorCalendarService implements CreatorRuntimeService {
    private final Map<String, Calendar> calendars = new LinkedHashMap<>();
    @Override public String getId() { return "calendar"; }

    @Override public synchronized Result execute(Map<String, Object> arguments) {
        String action = arguments.get("action") == null ? "now" : String.valueOf(arguments.get("action"));
        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
        if (componentId == null || componentId.trim().isEmpty()) componentId = "runtime";
        Calendar calendar = calendars.get(componentId);
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendars.put(componentId, calendar);
        }
        try {
            if ("get_time".equals(action)) {
                // Query only: preserve the component-scoped Calendar state.
            } else if ("now".equals(action) || "reset".equals(action)) {
                calendar.setTimeInMillis(System.currentTimeMillis());
            } else if ("add".equals(action) || "set".equals(action)) {
                String fieldName = CreatorRuntimeServiceArguments.string(arguments, "field");
                int field = field(fieldName);
                int value = (int) CreatorRuntimeServiceArguments.longValue(arguments, "value", 0L);
                if ("add".equals(action)) calendar.add(field, value); else calendar.set(field, value);
            } else if ("set_time".equals(action)) {
                calendar.setTimeInMillis(CreatorRuntimeServiceArguments.longValue(arguments, "timestamp", 0L));
            } else {
                return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Unsupported calendar action: " + action);
            }
        } catch (IllegalArgumentException error) {
            return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), error.getMessage());
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("componentId", componentId);
        output.put("timestamp", calendar.getTimeInMillis());
        output.put("year", calendar.get(Calendar.YEAR));
        output.put("month", calendar.get(Calendar.MONTH) + 1);
        output.put("day", calendar.get(Calendar.DAY_OF_MONTH));
        return new Result(Status.SUCCEEDED, output, null);
    }

    private static int field(String value) {
        if (value == null) throw new IllegalArgumentException("calendar add/set requires field.");
        String name = value.replace("Calendar.", "").trim().toUpperCase(java.util.Locale.ROOT);
        if ("YEAR".equals(name)) return Calendar.YEAR;
        if ("MONTH".equals(name)) return Calendar.MONTH;
        if ("DAY_OF_MONTH".equals(name) || "DATE".equals(name)) return Calendar.DAY_OF_MONTH;
        if ("HOUR".equals(name)) return Calendar.HOUR;
        if ("HOUR_OF_DAY".equals(name)) return Calendar.HOUR_OF_DAY;
        if ("MINUTE".equals(name)) return Calendar.MINUTE;
        if ("SECOND".equals(name)) return Calendar.SECOND;
        if ("MILLISECOND".equals(name)) return Calendar.MILLISECOND;
        if ("WEEK_OF_YEAR".equals(name)) return Calendar.WEEK_OF_YEAR;
        if ("DAY_OF_WEEK".equals(name)) return Calendar.DAY_OF_WEEK;
        throw new IllegalArgumentException("Unsupported calendar field: " + value);
    }
}
