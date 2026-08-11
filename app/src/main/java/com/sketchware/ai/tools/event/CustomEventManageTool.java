package com.sketchware.ai.tools.event;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mod.hilal.saif.events.EventsHandler;
import mod.hey.studios.util.Helper;
import pro.sketchware.utility.FileUtil;

/**
 * custom_event_manage - universal tool for managing Sketchware-Pro's
 * <b>custom user-defined events</b>.
 *
 * <p>Sketchware supports user-defined "custom events" via
 * {@link EventsHandler}. These are events that the user can register with a
 * name, target widget type, and event handler signature. They appear in the
 * event picker alongside built-in events like {@code onClick},
 * {@code onLongClick}, {@code onSwipeRefreshLayout}.
 *
 * <p><b>Storage model</b>: the custom events JSON file lives at
 * {@link EventsHandler#CUSTOM_EVENTS_FILE_PATH} which resolves to
 * {@code <ext>/.sketchware/data/system/events.json}. The format is an
 * {@code ArrayList<HashMap<String, Object>>} (Gson type
 * {@link Helper#TYPE_MAP_LIST}). Each entry has the following standard keys
 * (set by the Sketchware UI in {@code EventsManagerCreatorFragment.save()}):
 * <ul>
 *   <li>{@code name} (String) - event name (e.g. "onShake")</li>
 *   <li>{@code var} (String) - target widget type / activity marker
 *       (e.g. "Button", "TextView"; empty string means activity-level event)</li>
 *   <li>{@code listener} (String) - listener identifier used to group events
 *       under the same listener class (e.g. "onShakeListener")</li>
 *   <li>{@code icon} (String) - old Sketchware resource ID as a string
 *       (e.g. "2131165298" - the default used by EventsManagerCreatorFragment
 *       for activity events)</li>
 *   <li>{@code description} (String) - human-readable description shown in the
 *       event picker</li>
 *   <li>{@code parameters} (String) - block-builder spec string (e.g.
 *       "%d.position %s.value"); empty string means no extra params</li>
 *   <li>{@code code} (String) - Java code template containing {@code ###}
 *       (replaced with targetId) and {@code %s} (replaced with the user's
 *       block-generated code via {@code String.format})</li>
 *   <li>{@code headerSpec} (String) - block header spec (e.g.
 *       "when ### onShake %s.view"); {@code ###} is replaced with the widget
 *       name when the block is rendered</li>
 * </ul>
 *
 * <p>This tool additionally stores an <b>extension key</b>
 * {@code handlerSignature} (e.g. "(View view)") so the AI agent can round-trip
 * the Java method signature it was registered with. {@link EventsHandler}
 * ignores unknown keys, so this is safe.
 *
 * <p>After every mutating action, the tool calls
 * {@link EventsHandler#refreshCachedCustomEvents()} so the Sketchware UI picks
 * up the changes immediately (the cached list is reloaded from disk).
 *
 * <p>Actions (5):
 * <ul>
 *   <li><b>register</b> - register a new custom event (params:
 *       {@code name} required, {@code target_widget_type} required,
 *       {@code description} optional, {@code handler_signature} optional,
 *       plus optional overrides {@code listener}, {@code icon},
 *       {@code parameters}, {@code code}, {@code header_spec}).</li>
 *   <li><b>unregister</b> - remove a custom event by {@code name}.</li>
 *   <li><b>list</b> - list all registered custom events.</li>
 *   <li><b>get</b> - get full details of one custom event by {@code name}.</li>
 *   <li><b>update</b> - update an existing event (params: {@code name} to
 *       identify; optional {@code new_name}, {@code description},
 *       {@code handler_signature}, {@code listener}, {@code parameters},
 *       {@code code}, {@code header_spec}). Cannot change
 *       {@code target_widget_type} (would invalidate handlers).</li>
 * </ul>
 *
 * <p><b>API quirk discovered during research</b>: {@link EventsHandler} has
 * no public register/save method. The Sketchware UI itself (see
 * {@code EventsManagerCreatorFragment.save()}) follows the
 * read-modify-write pattern: read the JSON list from
 * {@link EventsHandler#CUSTOM_EVENTS_FILE_PATH}, mutate, then
 * {@link FileUtil#writeFile(String, String)} the result back. After writing,
 * {@link EventsHandler#refreshCachedCustomEvents()} must be called to update
 * the in-memory cache that {@code getActivityEvents()}, {@code addEvents()},
 * etc. read from. This tool follows the same pattern.
 *
 * <p><b>Validation</b>: {@code name} must match {@code ^[A-Za-z_][A-Za-z0-9_]*$}
 * (valid Java identifier). {@code target_widget_type} must be one of the
 * supported widget types listed in {@link #SUPPORTED_WIDGET_TYPES}.
 */
public final class CustomEventManageTool extends UniversalTool {

    /** Java identifier safety: letters, digits, underscore; must not start with a digit. */
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    /** Default handler signature when none is provided. */
    private static final String DEFAULT_HANDLER_SIGNATURE = "(View view)";

    /** Default icon ID used by the Sketchware UI for activity events. */
    private static final String DEFAULT_ICON_ID = "2131165298";

    /**
     * Supported target widget types for the {@code target_widget_type} argument.
     * These correspond to the {@code var} field stored in events.json and are
     * matched against {@code Gx.a(String)} at event-picking time. "Custom"
     * allows the user to register events for arbitrary user-defined component
     * types.
     */
    private static final Set<String> SUPPORTED_WIDGET_TYPES = new HashSet<>(Arrays.asList(
            "Button", "TextView", "ImageView", "LinearLayout", "RelativeLayout",
            "Spinner", "ListView", "RecyclerView", "WebView", "EditText",
            "CheckBox", "RadioButton", "SeekBar", "ProgressBar", "Custom"
    ));

    public CustomEventManageTool() {
        super("custom_event_manage",
                "Manage user-defined custom events: register, unregister, list, "
                        + "get details, or update. Custom events appear in the event "
                        + "picker alongside built-in events. Event names must match "
                        + "^[A-Za-z_][A-Za-z0-9_]*$. target_widget_type must be one of: "
                        + "Button, TextView, ImageView, LinearLayout, RelativeLayout, "
                        + "Spinner, ListView, RecyclerView, WebView, EditText, CheckBox, "
                        + "RadioButton, SeekBar, ProgressBar, Custom.",
                "event", /* readOnly */ false, /* autoApproved */ false,
                "register", "unregister", "list", "get", "update");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        addStringProp(props, "name", "Event name. Must match ^[A-Za-z_][A-Za-z0-9_]*$. Used as the unique identifier.");
        addStringProp(props, "target_widget_type",
                "(register) Widget type this event applies to. Must be one of: "
                        + "Button, TextView, ImageView, LinearLayout, RelativeLayout, "
                        + "Spinner, ListView, RecyclerView, WebView, EditText, CheckBox, "
                        + "RadioButton, SeekBar, ProgressBar, Custom.");
        addStringProp(props, "description", "(register/update) Human-readable description shown in the event picker.");
        addStringProp(props, "handler_signature",
                "(register/update) Java method signature like '(View view)' or '(View view, int position)'. "
                        + "Default: '(View view)'.");
        addStringProp(props, "new_name", "(update) New name for the event. Must match ^[A-Za-z_][A-Za-z0-9_]*$.");
        addStringProp(props, "listener",
                "(register/update) Listener class name used to group events under the same listener "
                        + "(e.g. 'onShakeListener'). Auto-derived from name if absent.");
        addStringProp(props, "icon",
                "(register/update) Old Sketchware resource ID as a string (e.g. '2131165298'). "
                        + "Default '2131165298'.");
        addStringProp(props, "parameters",
                "(register/update) Block-builder spec string (e.g. '%d.position %s.value'). "
                        + "Empty by default (no extra params).");
        addStringProp(props, "code",
                "(register/update) Java code template containing '###' (replaced with targetId) "
                        + "and a single '%s' (replaced with user's block code via String.format). "
                        + "Auto-derived from name + handler_signature if absent.");
        addStringProp(props, "header_spec",
                "(register/update) Block header spec (e.g. 'when ### onShake %s.view'). "
                        + "'###' is replaced with the widget name when rendered. "
                        + "Auto-derived from name if absent.");
    }

    private static void addStringProp(JsonObject p, String k, String d) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.addProperty("description", d);
        p.add(k, o);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        try {
            switch (action) {
                case "register":   return doRegister(args);
                case "unregister": return doUnregister(args);
                case "list":       return doList(args);
                case "get":        return doGet(args);
                case "update":     return doUpdate(args);
                default:           return err("Unknown action: " + action);
            }
        } finally {
            // Always refresh the cached custom events so the Sketchware UI picks up changes.
            try { EventsHandler.refreshCachedCustomEvents(); } catch (Throwable ignored) {}
        }
    }

    // ==================================================================
    //  register
    // ==================================================================

    /**
     * Register a new custom event. Reads the existing events.json list,
     * appends a new HashMap with all 8 standard keys (plus the
     * {@code handlerSignature} extension key), writes back, and refreshes
     * the in-memory cache via {@link EventsHandler#refreshCachedCustomEvents()}.
     */
    private ToolResult doRegister(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!VALID_NAME.matcher(name).matches()) {
            return err("Invalid name '" + name + "'. Must match ^[A-Za-z_][A-Za-z0-9_]*$.");
        }
        String widgetType = optString(args, "target_widget_type");
        if (widgetType == null || widgetType.isEmpty()) {
            return err("target_widget_type is required. Supported: " + SUPPORTED_WIDGET_TYPES + ".");
        }
        if (!SUPPORTED_WIDGET_TYPES.contains(widgetType)) {
            return err("Invalid target_widget_type '" + widgetType + "'. Supported: " + SUPPORTED_WIDGET_TYPES + ".");
        }
        String description = optString(args, "description", "No_Description");
        String handlerSignature = optString(args, "handler_signature", DEFAULT_HANDLER_SIGNATURE);

        // Auto-derive sensible defaults for the standard keys the Sketchware UI requires.
        String var = "Custom".equals(widgetType) ? name : widgetType;
        String listener = optString(args, "listener", deriveListenerName(name));
        String icon = optString(args, "icon", DEFAULT_ICON_ID);
        String parameters = optString(args, "parameters", "");
        String code = optString(args, "code", deriveCodeTemplate(name, handlerSignature));
        String headerSpec = optString(args, "header_spec", deriveHeaderSpec(name, handlerSignature));

        ArrayList<HashMap<String, Object>> list = readEvents();
        if (findIndex(list, name) >= 0) {
            return err("Custom event '" + name + "' already exists. Use 'update' to modify.");
        }

        HashMap<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("var", var);
        event.put("listener", listener);
        event.put("icon", icon);
        event.put("description", description);
        event.put("parameters", parameters);
        event.put("code", code);
        event.put("headerSpec", headerSpec);
        // Extension key for the Java signature (not used by EventsHandler, but
        // preserved for round-tripping by the AI agent).
        event.put("handlerSignature", handlerSignature);

        list.add(event);
        writeEvents(list);
        return ok("Registered custom event '" + name + "' (target=" + widgetType
                + ", listener=" + listener + ", signature=" + handlerSignature + ").");
    }

    // ==================================================================
    //  unregister
    // ==================================================================

    /**
     * Remove a custom event by name. Returns success/failure. If the event
     * doesn't exist, returns an error so the caller can correct the name.
     */
    private ToolResult doUnregister(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readEvents();
        int idx = findIndex(list, name);
        if (idx < 0) {
            return err("Custom event '" + name + "' not found. Use 'list' to see registered events.");
        }
        list.remove(idx);
        writeEvents(list);
        return ok("Unregistered custom event '" + name + "'.");
    }

    // ==================================================================
    //  list
    // ==================================================================

    /**
     * List all registered custom events as a human-readable table of
     * name / target_widget_type / description / handler_signature.
     */
    private ToolResult doList(JsonObject args) {
        ArrayList<HashMap<String, Object>> list = readEvents();
        if (list.isEmpty()) {
            return ok("No custom events registered. Use 'register' to create one.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Custom events (").append(list.size()).append("):\n");
        for (int i = 0; i < list.size(); i++) {
            HashMap<String, Object> e = list.get(i);
            sb.append("  [").append(i).append("] name='").append(str(e.get("name")))
              .append("' target='").append(str(e.get("var")))
              .append("' listener='").append(str(e.get("listener")))
              .append("' signature='").append(str(e.get("handlerSignature")))
              .append("'\n");
            String desc = str(e.get("description"));
            if (desc != null && !desc.isEmpty() && !desc.equals("No_Description")) {
                sb.append("        description: ").append(desc).append("\n");
            }
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  get
    // ==================================================================

    /**
     * Get full details of one custom event by name, including all 8 standard
     * keys plus the {@code handlerSignature} extension key.
     */
    private ToolResult doGet(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readEvents();
        int idx = findIndex(list, name);
        if (idx < 0) {
            return err("Custom event '" + name + "' not found. Use 'list' to see registered events.");
        }
        HashMap<String, Object> e = list.get(idx);
        StringBuilder sb = new StringBuilder();
        sb.append("Custom event '").append(name).append("':\n");
        sb.append("  name            : ").append(str(e.get("name"))).append("\n");
        sb.append("  var (target)    : ").append(str(e.get("var"))).append("\n");
        sb.append("  listener        : ").append(str(e.get("listener"))).append("\n");
        sb.append("  icon            : ").append(str(e.get("icon"))).append("\n");
        sb.append("  description     : ").append(str(e.get("description"))).append("\n");
        sb.append("  parameters      : ").append(str(e.get("parameters"))).append("\n");
        sb.append("  headerSpec      : ").append(str(e.get("headerSpec"))).append("\n");
        sb.append("  handlerSignature: ").append(str(e.get("handlerSignature"))).append("\n");
        sb.append("  code template   :\n");
        String code = str(e.get("code"));
        if (code != null && !code.isEmpty()) {
            // Indent each line for readability.
            for (String line : code.replace("\r", "").split("\n")) {
                sb.append("    ").append(line).append("\n");
            }
        }
        return ok(sb.toString());
    }

    // ==================================================================
    //  update
    // ==================================================================

    /**
     * Update an existing custom event. Cannot change
     * {@code target_widget_type} (would invalidate attached handlers).
     * Supports renaming via {@code new_name}.
     */
    private ToolResult doUpdate(JsonObject args) {
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        ArrayList<HashMap<String, Object>> list = readEvents();
        int idx = findIndex(list, name);
        if (idx < 0) {
            return err("Custom event '" + name + "' not found. Use 'list' to see registered events.");
        }
        HashMap<String, Object> event = list.get(idx);

        boolean changed = false;

        String newName = optString(args, "new_name");
        if (newName != null) {
            if (!VALID_NAME.matcher(newName).matches()) {
                return err("Invalid new_name '" + newName + "'. Must match ^[A-Za-z_][A-Za-z0-9_]*$.");
            }
            if (!newName.equals(name) && findIndex(list, newName) >= 0) {
                return err("An event named '" + newName + "' already exists.");
            }
            event.put("name", newName);
            changed = true;
        }

        String description = optString(args, "description");
        if (description != null) {
            event.put("description", description);
            changed = true;
        }

        String handlerSignature = optString(args, "handler_signature");
        if (handlerSignature != null) {
            event.put("handlerSignature", handlerSignature);
            changed = true;
        }

        String listener = optString(args, "listener");
        if (listener != null) {
            event.put("listener", listener);
            changed = true;
        }

        String icon = optString(args, "icon");
        if (icon != null) {
            event.put("icon", icon);
            changed = true;
        }

        String parameters = optString(args, "parameters");
        if (parameters != null) {
            event.put("parameters", parameters);
            changed = true;
        }

        String code = optString(args, "code");
        if (code != null) {
            event.put("code", code);
            changed = true;
        }

        String headerSpec = optString(args, "header_spec");
        if (headerSpec != null) {
            event.put("headerSpec", headerSpec);
            changed = true;
        }

        if (!changed) {
            return err("No update fields provided. Supported: new_name, description, "
                    + "handler_signature, listener, icon, parameters, code, header_spec. "
                    + "target_widget_type CANNOT be changed (would invalidate handlers).");
        }

        writeEvents(list);
        return ok("Updated custom event '" + name + "'. Changed fields: "
                + describeChanges(args) + ".");
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int findIndex(ArrayList<HashMap<String, Object>> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(str(list.get(i).get("name")))) return i;
        }
        return -1;
    }

    /**
     * Read the events.json file from {@link EventsHandler#CUSTOM_EVENTS_FILE_PATH}
     * using the read-modify-write pattern that the Sketchware UI itself uses.
     */
    @SuppressWarnings("unchecked")
    private static ArrayList<HashMap<String, Object>> readEvents() {
        String path = EventsHandler.CUSTOM_EVENTS_FILE_PATH;
        if (!FileUtil.isExistFile(path)) return new ArrayList<>();
        String content = FileUtil.readFile(path);
        if (content == null || content.isEmpty() || content.trim().equals("[]")) {
            return new ArrayList<>();
        }
        try {
            ArrayList<HashMap<String, Object>> list =
                    getGson().fromJson(content, Helper.TYPE_MAP_LIST);
            return list != null ? list : new ArrayList<>();
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    /**
     * Write the events list back to {@link EventsHandler#CUSTOM_EVENTS_FILE_PATH},
     * creating the parent directory if needed. The caller is responsible for
     * invoking {@link EventsHandler#refreshCachedCustomEvents()} afterwards
     * (handled centrally in {@link #dispatch}).
     */
    private static void writeEvents(ArrayList<HashMap<String, Object>> list) {
        String path = EventsHandler.CUSTOM_EVENTS_FILE_PATH;
        // Ensure parent dir exists (matches the pattern in CustomComponentManageTool).
        String parent = path.substring(0, Math.max(path.lastIndexOf(File.separator), 0));
        if (!FileUtil.isExistFile(parent)) {
            try { new File(parent).mkdirs(); } catch (Throwable ignored) {}
        }
        FileUtil.writeFile(path, getGson().toJson(list));
    }

    /**
     * Derive a listener class name from the event name. By Sketchware
     * convention, an event "onShake" lives under listener "onShakeListener".
     */
    private static String deriveListenerName(String eventName) {
        if (eventName == null || eventName.isEmpty()) return "customListener";
        if (eventName.endsWith("Listener")) return eventName;
        return eventName + "Listener";
    }

    /**
     * Derive a default Java code template for the event. The template uses
     * {@code ###} (replaced with targetId by EventsHandler) and a single
     * {@code %s} (replaced with the user's block code via String.format).
     *
     * <p>The default wraps the user's code in a method whose signature matches
     * the provided {@code handlerSignature}. The parameter names are derived
     * from the signature types (View _view, int _position, etc.) for safety,
     * since the {@code %s} format string only accepts positional args.
     */
    private static String deriveCodeTemplate(String eventName, String handlerSignature) {
        String paramList = deriveParamList(handlerSignature);
        return "public void " + eventName + "(" + paramList + ") {\r\n"
                + "%s\r\n"
                + "}";
    }

    /**
     * Derive a default block header spec. The {@code ###} is the target widget
     * name placeholder; the rest is the event label.
     */
    private static String deriveHeaderSpec(String eventName, String handlerSignature) {
        // Best-effort: produce a readable spec like "when ### onShake".
        return "when ### " + eventName;
    }

    /**
     * Convert a Java handler signature like "(View view, int position)" into
     * a parameter declaration list like "View _view, int _position".
     * Parameter names are underscore-prefixed to avoid clashing with user
     * block-generated variable names.
     */
    private static String deriveParamList(String signature) {
        if (signature == null) return "";
        // Strip surrounding parens.
        String inner = signature.trim();
        if (inner.startsWith("(")) inner = inner.substring(1);
        if (inner.endsWith(")")) inner = inner.substring(0, inner.length() - 1);
        inner = inner.trim();
        if (inner.isEmpty()) return "";
        String[] parts = inner.split(",");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (!first) sb.append(", ");
            first = false;
            // Split "Type name" into type + name; default to underscore-prefixed name.
            int sp = p.lastIndexOf(' ');
            if (sp > 0 && sp < p.length() - 1) {
                String type = p.substring(0, sp).trim();
                String name = p.substring(sp + 1).trim();
                sb.append(type).append(" _").append(name);
            } else {
                // Just a type, e.g. "View" -> "View _view".
                String type = p;
                String defaultName = type.startsWith("View") ? "view"
                        : type.startsWith("int") ? "position"
                        : type.startsWith("String") ? "value"
                        : "arg";
                sb.append(type).append(" _").append(defaultName);
            }
        }
        return sb.toString();
    }

    /**
     * Human-readable list of which update fields were provided in the args.
     * Used for the success message of the {@code update} action.
     */
    private static String describeChanges(JsonObject args) {
        List<String> fields = new ArrayList<>();
        String[] keys = {"new_name", "description", "handler_signature", "listener",
                "icon", "parameters", "code", "header_spec"};
        for (String k : keys) {
            if (args.has(k) && !args.get(k).isJsonNull()) fields.add(k);
        }
        return fields.isEmpty() ? "(none)" : String.join(", ", fields);
    }
}
