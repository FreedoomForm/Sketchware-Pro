package pro.sketchware.creator.runtime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Runtime-native calendar operations corresponding to the legacy Calendar component. */
public final class CreatorCalendarService implements CreatorRuntimeService {
    private final Map<String, Calendar> calendars = new LinkedHashMap<>();

    @Override public String getId() { return "calendar"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String id = CreatorRuntimeServiceArguments.string(arguments, "id");
        if (action == null) action = "now";
        if (id == null) id = "default";
        try {
            if ("now".equals(action)) {
                Calendar calendar = Calendar.getInstance();
                calendars.put(id, calendar);
                return output(id, calendar);
            }
            Calendar calendar = calendars.get(id);
            if (calendar == null) return CreatorRuntimeServiceArguments.invalid("Unknown calendar id: " + id);
            if ("add".equals(action) || "set".equals(action)) {
                int field = field(CreatorRuntimeServiceArguments.string(arguments, "field"));
                int amount = (int) CreatorRuntimeServiceArguments.longValue(arguments, "value", 0L);
                if ("add".equals(action)) calendar.add(field, amount); else calendar.set(field, amount);
                return output(id, calendar);
            }
            if ("format".equals(action)) {
                String pattern = CreatorRuntimeServiceArguments.string(arguments, "pattern");
                if (pattern == null) pattern = "yyyy/MM/dd HH:mm:ss";
                return CreatorRuntimeServiceArguments.succeeded("id", id,
                        "formatted", new SimpleDateFormat(pattern, Locale.US).format(calendar.getTime()));
            }
            if ("get_time".equals(action)) {
                return CreatorRuntimeServiceArguments.succeeded("id", id, "timestamp", calendar.getTimeInMillis());
            }
            if ("set_time".equals(action)) {
                calendar.setTimeInMillis(CreatorRuntimeServiceArguments.longValue(arguments, "timestamp", 0L));
                return output(id, calendar);
            }
            if ("diff".equals(action)) {
                String otherId = CreatorRuntimeServiceArguments.string(arguments, "otherId");
                Calendar other = calendars.get(otherId);
                if (other == null) return CreatorRuntimeServiceArguments.invalid("Unknown calendar id: " + otherId);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "otherId", otherId,
                        "differenceMs", calendar.getTimeInMillis() - other.getTimeInMillis());
            }
            return CreatorRuntimeServiceArguments.invalid("Unsupported calendar action: " + action);
        } catch (IllegalArgumentException error) {
            return CreatorRuntimeServiceArguments.invalid(error.getMessage());
        }
    }

    private static Result output(String id, Calendar calendar) {
        return CreatorRuntimeServiceArguments.succeeded("id", id, "timestamp", calendar.getTimeInMillis(),
                "year", calendar.get(Calendar.YEAR), "month", calendar.get(Calendar.MONTH) + 1,
                "day", calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static int field(String name) {
        if (name == null) throw new IllegalArgumentException("field is required");
        switch (name.toUpperCase(Locale.ROOT)) {
            case "YEAR": return Calendar.YEAR;
            case "MONTH": return Calendar.MONTH;
            case "DAY_OF_MONTH": case "DAY": return Calendar.DAY_OF_MONTH;
            case "HOUR": case "HOUR_OF_DAY": return Calendar.HOUR_OF_DAY;
            case "MINUTE": return Calendar.MINUTE;
            case "SECOND": return Calendar.SECOND;
            case "MILLISECOND": return Calendar.MILLISECOND;
            default: throw new IllegalArgumentException("Unsupported calendar field: " + name);
        }
    }
}
