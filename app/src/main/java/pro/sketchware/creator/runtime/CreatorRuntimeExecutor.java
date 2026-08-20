package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Executes an attached event binding using only typed operations and visible effects. */
public final class CreatorRuntimeExecutor {
    private static final int MAX_REPEAT_ITERATIONS = 10_000;
    private static final int MAX_CUSTOM_FUNCTION_DEPTH = 64;
    public static final class Effect {
        private final String type;
        private final String value;
        Effect(String type, String value) { this.type = type; this.value = value; }
        public String getType() { return type; }
        public String getValue() { return value; }
    }

    private final CreatorRuntimeServiceDispatcher runtimeServices;
    private static final String RETURN_VALUE_KEY = "__creator_return_value__";
    private final Deque<Map<String, Object>> customFunctionFrames = new ArrayDeque<>();
    private int customFunctionDepth;

    public CreatorRuntimeExecutor() { this(null); }
    public CreatorRuntimeExecutor(CreatorRuntimeServiceDispatcher runtimeServices) { this.runtimeServices = runtimeServices; }

    public List<Effect> dispatch(CreatorRuntimeEngine engine, String targetWidgetId, String eventName) {
        if (engine == null) return Collections.emptyList();
        CreatorEventBinding binding = findBinding(engine.getCurrent(), targetWidgetId, eventName);
        if (binding == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        Flow flow = executeBlocks(engine, binding.getBlocks(), effects);
        if (flow == Flow.BREAK) {
            effects.add(new Effect("break", "ignored_outside_loop"));
        } else if (flow == Flow.RETURN) {
            effects.add(new Effect("return", "ignored_outside_more_block"));
        }
        return Collections.unmodifiableList(effects);
    }

    private enum Flow { CONTINUE, BREAK, RETURN }

    private Flow executeBlocks(CreatorRuntimeEngine engine, List<CreatorRuntimeBlock> blocks, List<Effect> effects) {
        for (CreatorRuntimeBlock block : blocks) {
            Map<String, Object> payload = block.getPayload();
            if (block.getType() == CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY) {
                Object value = payload.containsKey("expression") ? evaluate(payload.get("expression"), engine) : payload.get("value");
                apply(engine, CreatorProjectOperation.Type.WIDGET_SET_PROPERTY, map(
                        "widgetId", payload.get("widgetId"), "property", payload.get("property"), "value", value));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SET_STATE) {
                Object value = payload.containsKey("expression") ? evaluate(payload.get("expression"), engine) : payload.get("value");
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", payload.get("stateId"), "value", value));
            } else if (block.getType() == CreatorRuntimeBlock.Type.INCREMENT_STATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                Object rawCurrent = engine.getCurrent().getState().get(stateId);
                long current = number(rawCurrent);
                long delta = number(payload.get("delta"));
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", current + delta));
            } else if (block.getType() == CreatorRuntimeBlock.Type.LIST_MUTATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                java.util.List<Object> list = list(engine.getCurrent().getState().get(stateId));
                String action = String.valueOf(payload.get("action"));
                Object value = payload.containsKey("valueExpression") ? evaluate(payload.get("valueExpression"), engine) : payload.get("value");
                Object json = payload.containsKey("jsonExpression") ? evaluate(payload.get("jsonExpression"), engine) : payload.get("json");
                int index = (int) number(payload.containsKey("indexExpression") ? evaluate(payload.get("indexExpression"), engine) : payload.get("index"));
                Object key = payload.containsKey("keyExpression") ? evaluate(payload.get("keyExpression"), engine) : payload.get("key");
                if ("add".equals(action)) list.add(value);
                else if ("insert".equals(action)) {
                    if (index >= 0 && index <= list.size()) list.add(index, value);
                } else if ("remove_at".equals(action)) {
                    if (index >= 0 && index < list.size()) list.remove(index);
                } else if ("clear".equals(action)) list.clear();
                else if ("set_at".equals(action)) {
                    if (index >= 0 && index < list.size()) list.set(index, value);
                }
                else if ("add_all".equals(action)) list.addAll(list(engine.getCurrent().getState().get(
                        String.valueOf(payload.get("sourceStateId")))));
                else if ("replace_map_keys".equals(action)) {
                    list.clear();
                    list.addAll(map(engine.getCurrent().getState().get(
                            String.valueOf(payload.get("sourceMapStateId")))).keySet());
                }
                else if ("replace_json_maps".equals(action)) {
                    try {
                        Object parsed = new com.google.gson.Gson().fromJson(String.valueOf(json), java.util.List.class);
                        list.clear();
                        list.addAll(list(parsed));
                    } catch (com.google.gson.JsonSyntaxException ignored) { }
                }
                else if ("map_put_at".equals(action) && index >= 0 && index < list.size()) {
                    Map<String, Object> item = map(list.get(index));
                    item.put(String.valueOf(key), value);
                    list.set(index, item);
                }
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", list));
            } else if (block.getType() == CreatorRuntimeBlock.Type.MAP_MUTATE) {
                String stateId = String.valueOf(payload.get("stateId"));
                Map<String, Object> values = map(engine.getCurrent().getState().get(stateId));
                String action = String.valueOf(payload.get("action"));
                Object key = payload.containsKey("keyExpression") ? evaluate(payload.get("keyExpression"), engine) : payload.get("key");
                Object value = payload.containsKey("valueExpression") ? evaluate(payload.get("valueExpression"), engine) : payload.get("value");
                Object json = payload.containsKey("jsonExpression") ? evaluate(payload.get("jsonExpression"), engine) : payload.get("json");
                if ("put".equals(action)) values.put(String.valueOf(key), value);
                else if ("remove".equals(action)) values.remove(String.valueOf(key));
                else if ("clear".equals(action) || "create".equals(action)) values.clear();
                else if ("replace_json".equals(action)) {
                    try {
                        Object parsed = new com.google.gson.Gson().fromJson(String.valueOf(json), java.util.Map.class);
                        values.clear();
                        values.putAll(map(parsed));
                    } catch (com.google.gson.JsonSyntaxException ignored) { }
                }
                apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", stateId, "value", values));
            } else if (block.getType() == CreatorRuntimeBlock.Type.ATTACH_EVENT) {
                apply(engine, CreatorProjectOperation.Type.EVENT_ATTACH, map(
                        "bindingId", payload.get("bindingId"),
                        "targetWidgetId", payload.get("targetWidgetId"),
                        "eventName", payload.get("eventName"),
                        "blocks", block.getThenBlocks()));
            } else if (block.getType() == CreatorRuntimeBlock.Type.SHOW_MESSAGE) {
                effects.add(new Effect("message", String.valueOf(payload.get("message"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.NAVIGATE) {
                effects.add(new Effect("navigate", String.valueOf(payload.get("screenId"))));
            } else if (block.getType() == CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL) {
                if (runtimeServices == null) effects.add(new Effect("runtime_service", "unavailable"));
                else {
                    String serviceId = String.valueOf(payload.get("serviceId"));
                    Object rawArguments = payload.get("arguments");
                    @SuppressWarnings("unchecked") Map<String, Object> arguments = rawArguments instanceof Map
                            ? (Map<String, Object>) rawArguments : Collections.<String, Object>emptyMap();
                    CreatorRuntimeService.Result result = runtimeServices.dispatch(serviceId, resolveServiceArguments(engine, arguments));
                    String resultStateId = CreatorRuntimeServiceArguments.string(arguments, "resultStateId");
                    String resultKey = CreatorRuntimeServiceArguments.string(arguments, "resultKey");
                    if (result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED && resultStateId != null) {
                        Object value = resultKey == null ? result.getOutput() : result.getOutput().get(resultKey);
                        apply(engine, CreatorProjectOperation.Type.STATE_SET, map("stateId", resultStateId, "value", value));
                    }
                    effects.add(new Effect("runtime_service", serviceId + ":" + result.getStatus().name()));
                }
            } else if (block.getType() == CreatorRuntimeBlock.Type.CUSTOM_FUNCTION_CALL) {
                invokeMoreBlock(engine, String.valueOf(payload.get("functionId")),
                        expressionValues(payload.get("arguments"), engine), effects);
            } else if (block.getType() == CreatorRuntimeBlock.Type.RETURN) {
                if (!customFunctionFrames.isEmpty()) {
                    Object value = payload.containsKey("expression")
                            ? evaluate(payload.get("expression"), engine) : payload.get("value");
                    customFunctionFrames.peek().put(RETURN_VALUE_KEY, value);
                }
                return Flow.RETURN;
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_STATE_EQUALS) {
                String stateId = String.valueOf(payload.get("stateId"));
                Object actual = engine.getCurrent().getState().get(stateId);
                Object expected = payload.get("equals");
                boolean matches = expected == null ? actual == null : expected.equals(actual);
                Flow childFlow = executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
                if (childFlow == Flow.BREAK || childFlow == Flow.RETURN) return childFlow;
            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_BOOLEAN) {
                boolean matches;
                if (payload.containsKey("expression")) matches = booleanValue(evaluate(payload.get("expression"), engine));
                else if (payload.containsKey("constant")) matches = Boolean.TRUE.equals(payload.get("constant"));
                else matches = Boolean.TRUE.equals(engine.getCurrent().getState().get(String.valueOf(payload.get("stateId"))));
                Flow childFlow = executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
                if (childFlow == Flow.BREAK || childFlow == Flow.RETURN) return childFlow;
            } else if (block.getType() == CreatorRuntimeBlock.Type.REPEAT) {
                long requested = payload.containsKey("countExpression") ? number(evaluate(payload.get("countExpression"), engine))
                        : payload.containsKey("countStateId")
                        ? number(engine.getCurrent().getState().get(String.valueOf(payload.get("countStateId"))))
                        : number(payload.get("count"));
                int count = (int) Math.max(0L, Math.min(MAX_REPEAT_ITERATIONS, requested));
                if (requested > MAX_REPEAT_ITERATIONS) effects.add(new Effect("repeat", "capped:" + MAX_REPEAT_ITERATIONS));
                for (int iteration = 0; iteration < count; iteration++) {
                    Flow childFlow = executeBlocks(engine, block.getThenBlocks(), effects);
                    if (childFlow == Flow.RETURN) return Flow.RETURN;
                    if (childFlow == Flow.BREAK) break;
                }
            } else if (block.getType() == CreatorRuntimeBlock.Type.FOREVER) {
                boolean broken = false;
                for (int iteration = 0; iteration < MAX_REPEAT_ITERATIONS; iteration++) {
                    Flow childFlow = executeBlocks(engine, block.getThenBlocks(), effects);
                    if (childFlow == Flow.RETURN) return Flow.RETURN;
                    if (childFlow == Flow.BREAK) {
                        broken = true;
                        break;
                    }
                }
                if (!broken) effects.add(new Effect("forever", "capped:" + MAX_REPEAT_ITERATIONS));
            } else if (block.getType() == CreatorRuntimeBlock.Type.BREAK) {
                return Flow.BREAK;
            }
        }
        return Flow.CONTINUE;
    }

    private CreatorEventBinding findBinding(CreatorProjectDocument document, String targetWidgetId, String eventName) {
        for (CreatorEventBinding binding : document.getEvents().values()) {
            if (binding.getTargetWidgetId().equals(targetWidgetId) && binding.getEventName().equals(eventName)) return binding;
        }
        return null;
    }

    private void apply(CreatorRuntimeEngine engine, CreatorProjectOperation.Type type, Map<String, Object> payload) {
        CreatorProjectDocument document = engine.getCurrent();
        engine.apply(new CreatorProjectOperation("runtime-" + UUID.randomUUID(), document.getProjectId(),
                document.getRevision(), CreatorProjectOperation.ActorKind.SYSTEM, type, payload, System.currentTimeMillis()));
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }

    private static Map<String, Object> resolveServiceArguments(CreatorRuntimeEngine engine, Map<String, Object> arguments) {
        Map<String, Object> resolved = new LinkedHashMap<>(arguments);
        resolveStateMap(engine, resolved, "paramsStateId", "params");
        resolveStateMap(engine, resolved, "headersStateId", "headers");
        resolveStateMap(engine, resolved, "valueStateId", "value");
        resolveStateList(engine, resolved, "itemsStateId", "items");
        return resolved;
    }

    private static void resolveStateMap(CreatorRuntimeEngine engine, Map<String, Object> arguments, String referenceKey, String valueKey) {
        Object reference = arguments.get(referenceKey);
        if (reference == null) return;
        Object value = engine.getCurrent().getState().get(String.valueOf(reference));
        if (value instanceof Map) arguments.put(valueKey, value);
    }

    private static void resolveStateList(CreatorRuntimeEngine engine, Map<String, Object> arguments, String referenceKey, String valueKey) {
        Object reference = arguments.get(referenceKey);
        if (reference == null) return;
        Object value = engine.getCurrent().getState().get(String.valueOf(reference));
        if (value instanceof List) arguments.put(valueKey, value);
    }

    @SuppressWarnings("unchecked")
    private Object evaluate(Object rawExpression, CreatorRuntimeEngine engine) {
        if (!(rawExpression instanceof Map)) return rawExpression;
        Map<String, Object> expression = (Map<String, Object>) rawExpression;
        if ("literal".equals(expression.get("kind"))) {
            String literal = String.valueOf(expression.get("value"));
            if (engine.getCurrent().getState().containsKey(literal)) return engine.getCurrent().getState().get(literal);
            if ("true".equalsIgnoreCase(literal) || "false".equalsIgnoreCase(literal)) return Boolean.valueOf(literal);
            try { return Double.valueOf(literal); } catch (NumberFormatException ignored) { return literal; }
        }
        if (!"reporter".equals(expression.get("kind"))) return null;
        String op = String.valueOf(expression.get("opCode"));
        List<Object> values = new ArrayList<>();
        Object rawArguments = expression.get("arguments");
        if (rawArguments instanceof List) for (Object argument : (List<?>) rawArguments) values.add(evaluate(argument, engine));
        Object first = values.isEmpty() ? null : values.get(0);
        Object second = values.size() < 2 ? null : values.get(1);
        if ("true".equals(op)) return true;
        if ("false".equals(op)) return false;
        if ("getarg".equals(op)) return currentCustomArgument(String.valueOf(expression.get("spec")));
        if ("definedfunc".equals(op)) return invokeMoreBlock(engine, String.valueOf(expression.get("spec")), values,
                new ArrayList<Effect>());
        if ("random".equals(op)) return randomInclusive(first, second);
        if ("not".equals(op)) return !booleanValue(first);
        if ("&&".equals(op)) return booleanValue(first) && booleanValue(second);
        if ("||".equals(op)) return booleanValue(first) || booleanValue(second);
        if ("=".equals(op) || "stringequals".equals(op)) return first == null ? second == null : first.toString().equals(String.valueOf(second));
        if (">".equals(op)) return decimal(first) > decimal(second);
        if ("<".equals(op)) return decimal(first) < decimal(second);
        if ("+".equals(op)) return decimal(first) + decimal(second);
        if ("-".equals(op)) return decimal(first) - decimal(second);
        if ("*".equals(op)) return decimal(first) * decimal(second);
        if ("/".equals(op)) return decimal(second) == 0d ? 0d : decimal(first) / decimal(second);
        if ("%".equals(op)) return decimal(second) == 0d ? 0d : decimal(first) % decimal(second);
        if ("stringlength".equals(op)) return first == null ? 0d : (double) String.valueOf(first).length();
        if ("stringjoin".equals(op)) return String.valueOf(first) + String.valueOf(second);
        if ("stringindex".equals(op)) return second == null ? -1d : (double) String.valueOf(second).indexOf(String.valueOf(first));
        if ("stringlastindex".equals(op)) return second == null ? -1d : (double) String.valueOf(second).lastIndexOf(String.valueOf(first));
        if ("stringsub".equals(op)) {
            String text = first == null ? "" : String.valueOf(first);
            int start = boundedIndex(decimal(second), text.length());
            int end = boundedIndex(decimal(values.size() < 3 ? null : values.get(2)), text.length());
            return end < start ? "" : text.substring(start, end);
        }
        if ("stringcontains".equals(op)) return first != null && String.valueOf(first).contains(String.valueOf(second));
        if ("fileutilstartswith".equals(op)) return first != null && String.valueOf(first).startsWith(String.valueOf(second));
        if ("fileutilendswith".equals(op)) return first != null && String.valueOf(first).endsWith(String.valueOf(second));
        if ("fileutilgetlastsegmentpath".equals(op)) {
            String path = first == null ? "" : String.valueOf(first);
            int slash = path.lastIndexOf('/');
            return slash < 0 ? path : slash == path.length() - 1 ? "" : path.substring(slash + 1);
        }
        if ("stringreplace".equals(op)) return String.valueOf(first).replace(String.valueOf(second), String.valueOf(values.size() < 3 ? "" : values.get(2)));
        if ("stringreplacefirst".equals(op)) return String.valueOf(first).replaceFirst(String.valueOf(second), String.valueOf(values.size() < 3 ? "" : values.get(2)));
        if ("stringreplaceall".equals(op)) return String.valueOf(first).replaceAll(String.valueOf(second), String.valueOf(values.size() < 3 ? "" : values.get(2)));
        if ("trim".equals(op)) return first == null ? "" : String.valueOf(first).trim();
        if ("touppercase".equals(op)) return first == null ? "" : String.valueOf(first).toUpperCase(java.util.Locale.ROOT);
        if ("tolowercase".equals(op)) return first == null ? "" : String.valueOf(first).toLowerCase(java.util.Locale.ROOT);
        if ("tonumber".equals(op)) return decimal(first);
        if ("tostring".equals(op)) return String.valueOf((long) decimal(first));
        if ("tostringwithdecimal".equals(op)) return String.valueOf(decimal(first));
        if ("tostringformat".equals(op)) return new java.text.DecimalFormat(String.valueOf(second)).format(decimal(first));
        if ("currenttime".equals(op)) return (double) System.currentTimeMillis();
        if ("mathpi".equals(op)) return Math.PI;
        if ("mathe".equals(op)) return Math.E;
        if ("mathpow".equals(op)) return Math.pow(decimal(first), decimal(second));
        if ("mathmin".equals(op)) return Math.min(decimal(first), decimal(second));
        if ("mathmax".equals(op)) return Math.max(decimal(first), decimal(second));
        if ("mathsqrt".equals(op)) return Math.sqrt(decimal(first));
        if ("mathabs".equals(op)) return Math.abs(decimal(first));
        if ("mathround".equals(op)) return (double) Math.round(decimal(first));
        if ("mathceil".equals(op)) return Math.ceil(decimal(first));
        if ("mathfloor".equals(op)) return Math.floor(decimal(first));
        if ("mathsin".equals(op)) return Math.sin(decimal(first));
        if ("mathcos".equals(op)) return Math.cos(decimal(first));
        if ("mathtan".equals(op)) return Math.tan(decimal(first));
        if ("mathasin".equals(op)) return Math.asin(decimal(first));
        if ("mathacos".equals(op)) return Math.acos(decimal(first));
        if ("mathatan".equals(op)) return Math.atan(decimal(first));
        if ("mathexp".equals(op)) return Math.exp(decimal(first));
        if ("mathlog".equals(op)) return Math.log(decimal(first));
        if ("mathlog10".equals(op)) return Math.log10(decimal(first));
        if ("mathtoradian".equals(op)) return Math.toRadians(decimal(first));
        if ("mathtodegree".equals(op)) return Math.toDegrees(decimal(first));
        if ("mathgetdip".equals(op)) return deviceMetricValue("dip", first);
        if ("mathgetdisplaywidth".equals(op)) return deviceMetricValue("display_width", null);
        if ("mathgetdisplayheight".equals(op)) return deviceMetricValue("display_height", null);
        if ("mediaplayergetcurrent".equals(op)) return mediaValue(first, "current_position");
        if ("mediaplayergetduration".equals(op)) return mediaValue(first, "duration");
        if ("mediaplayerisplaying".equals(op)) return mediaValue(first, "is_playing");
        if ("mediaplayerislooping".equals(op)) return mediaValue(first, "is_looping");
        if ("fileutilread".equals(op)) return fileValue(first, "read", "content");
        if ("fileutilisexist".equals(op)) return fileValue(first, "exists", "value");
        if ("fileutilisdir".equals(op)) return fileValue(first, "is_dir", "value");
        if ("fileutilisfile".equals(op)) return fileValue(first, "is_file", "value");
        if ("fileutillength".equals(op)) return fileValue(first, "length", "value");
        if ("getjpegrotate".equals(op)) return bitmapValue(first, "jpeg_rotate");
        if ("getexternalstoragedir".equals(op)) return filePathValue("get_external_storage_dir", null);
        if ("getpackagedatadir".equals(op)) return filePathValue("get_package_data_dir", null);
        if ("getpublicdir".equals(op)) return filePathValue("get_public_dir", first);
        if ("calendargettime".equals(op)) return calendarTimestamp(first);
        if ("calendarformat".equals(op)) {
            Object timestamp = calendarTimestamp(first);
            if (timestamp == null) return null;
            String pattern = second == null || String.valueOf(second).isEmpty()
                    ? "yyyy/MM/dd hh:mm:ss" : String.valueOf(second);
            try {
                return new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                        .format(new java.util.Date(number(timestamp)));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if ("calendardiff".equals(op)) {
            Object left = calendarTimestamp(first);
            Object right = calendarTimestamp(second);
            return left == null || right == null ? null : (double) (number(left) - number(right));
        }
        if ("firebasegetpushkey".equals(op)) return firebaseValue(engine, first, "push_key", "key");
        if ("firebaseauthisloggedin".equals(op)) return firebaseAuthValue("signedIn");
        if ("firebaseauthgetcurrentuser".equals(op)) return firebaseAuthValue("email");
        if ("firebaseauthgetuid".equals(op)) return firebaseAuthValue("uid");
        if ("bluetoothconnectgetrandomuuid".equals(op)) return bluetoothValue(first, "random_uuid", "uuid");
        if ("bluetoothconnectisbluetoothactivated".equals(op)) return bluetoothValue(first, "status", "activated");
        if ("bluetoothconnectisbluetoothenabled".equals(op)) return bluetoothValue(first, "status", "enabled");
        if ("objectanimatorisrunning".equals(op)) return animatorValue(first, "is_running", "value");
        if ("texttospeechisspeaking".equals(op)) return textToSpeechValue(first, "is_speaking", "value");
        if ("filegetdata".equals(op)) return storageValue(first, second, "get", "value");
        if ("intentgetstring".equals(op)) return intentValue(first, second, "get_string", "value");
        if ("getresstr".equals(op)) return CreatorRuntimeResourceValues.resolveString(engine.getCurrent(),
                "@string/" + literalName(rawArguments, first));
        if ("maptostr".equals(op) || "listmaptostr".equals(op)) return new com.google.gson.Gson().toJson(first);
        if ("getvar".equals(op)) return engine.getCurrent().getState().get(literalName(rawArguments, first));
        if ("gettext".equals(op)) return String.valueOf(liveWidgetValue(engine, first, "get_text", "text", ""));
        if ("getenable".equals(op)) return booleanValue(liveWidgetValue(engine, first, "get_enabled", "enabled", true));
        if ("getchecked".equals(op)) return booleanValue(liveWidgetValue(engine, first, "get_checked", "checked", false));
        if ("getalpha".equals(op)) return decimal(liveWidgetValue(engine, first, "get_alpha", "alpha", 1d));
        if ("getrotate".equals(op)) return decimal(liveWidgetValue(engine, first, "get_rotation", "rotation", 0d));
        if ("gettranslationx".equals(op)) return decimal(liveWidgetValue(engine, first, "get_translation_x", "translationX", 0d));
        if ("gettranslationy".equals(op)) return decimal(liveWidgetValue(engine, first, "get_translation_y", "translationY", 0d));
        if ("getlocationx".equals(op)) return widgetQueryValue(first, "location_x");
        if ("getlocationy".equals(op)) return widgetQueryValue(first, "location_y");
        if ("getscalex".equals(op)) return decimal(liveWidgetValue(engine, first, "get_scale_x", "scaleX", 1d));
        if ("getscaley".equals(op)) return decimal(liveWidgetValue(engine, first, "get_scale_y", "scaleY", 1d));
        if ("seekbargetmax".equals(op)) return decimal(liveWidgetValue(engine, first, "seek_max", "max", 100d));
        if ("seekbargetprogress".equals(op)) return decimal(liveWidgetValue(engine, first, "seek_progress", "progress", 0d));
        if ("getrating".equals(op) || "ratingbargetrating".equals(op)) {
            return decimal(liveWidgetValue(engine, first, "rating_value", "value", 0d));
        }
        if ("getnumstars".equals(op) || "ratingbargetnumstars".equals(op)) {
            return decimal(liveWidgetValue(engine, first, "rating_num_stars", "value", 0d));
        }
        if ("getstepsize".equals(op) || "ratingbargetstepsize".equals(op)) {
            return decimal(liveWidgetValue(engine, first, "rating_step_size", "value", 0d));
        }
        if ("searchviewgetquery".equals(op) || "searchgetquery".equals(op)) {
            return String.valueOf(liveWidgetValue(engine, first, "search_query", "value", ""));
        }
        if ("getformat12hour".equals(op) || "textclockgetformat12hour".equals(op)) {
            return String.valueOf(liveWidgetValue(engine, first, "clock_get_format_12h", "value", ""));
        }
        if ("getformat24hour".equals(op) || "textclockgetformat24hour".equals(op)) {
            return String.valueOf(liveWidgetValue(engine, first, "clock_get_format_24h", "value", ""));
        }
        if ("spngetselection".equals(op)) return decimal(liveWidgetValue(engine, first, "spinner_selection", "selectedIndex", 0d));
        if ("webviewgeturl".equals(op)) return String.valueOf(liveWidgetValue(engine, first, "web_url", "url", ""));
        if ("webviewcangoback".equals(op)) return widgetQueryValue(first, "web_can_go_back");
        if ("webviewcangoforward".equals(op)) return widgetQueryValue(first, "web_can_go_forward");
        if ("listgetcheckedposition".equals(op)) return widgetQueryValue(first, "list_checked_position");
        if ("listgetcheckedcount".equals(op)) return widgetQueryValue(first, "list_checked_count");
        if ("isdraweropen".equals(op)) return booleanValue(drawerValue());
        if ("calendarviewgetdate".equals(op)) return decimal(liveWidgetValue(engine, first, "calendar_date", "date", 0d));
        if ("lengthlist".equals(op)) return (double) listValue(first).size();
        if ("getatlistint".equals(op) || "getatliststr".equals(op)) {
            return at(listValue(second), (int) decimal(first));
        }
        if ("getatlistmap".equals(op)) {
            Object row = at(listValue(values.size() < 3 ? null : values.get(2)), (int) decimal(first));
            Object value = mapValue(row).get(String.valueOf(second));
            return value == null ? null : String.valueOf(value);
        }
        if ("getmapatposlistmap".equals(op)) {
            return mapValue(at(listValue(second), (int) decimal(first)));
        }
        if ("containlistint".equals(op) || "containliststr".equals(op)) return listValue(first).contains(second);
        if ("indexlistint".equals(op) || "indexliststr".equals(op)) return (double) listValue(second).indexOf(first);
        if ("containlistmap".equals(op)) {
            Object row = at(listValue(first), (int) decimal(second));
            return mapValue(row).containsKey(String.valueOf(values.size() < 3 ? null : values.get(2)));
        }
        if ("mapget".equals(op)) return mapValue(first).get(String.valueOf(second));
        if ("mapcontainkey".equals(op)) return mapValue(first).containsKey(String.valueOf(second));
        if ("mapcontainvalue".equals(op)) return mapValue(first).containsValue(second);
        if ("mapsize".equals(op)) return (double) mapValue(first).size();
        if ("mapisempty".equals(op)) return mapValue(first).isEmpty();
        if ("hashmapgetnumber".equals(op)) return decimal(mapValue(first).get(String.valueOf(second)));
        if ("hashmapgetboolean".equals(op)) return booleanValue(mapValue(first).get(String.valueOf(second)));
        if ("hashmapgetmap".equals(op)) return mapValue(mapValue(first).get(String.valueOf(second)));
        if ("hashmapliststr".equals(op) || "hashmapgetlistmap".equals(op)) {
            return new ArrayList<Object>(listValue(mapValue(first).get(String.valueOf(second))));
        }
        return null;
    }

    private Object mediaValue(Object id, String action) {
        if (runtimeServices == null || id == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("media",
                CreatorRuntimeServiceArguments.output("id", String.valueOf(id), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("value") : null;
    }

    private Object deviceMetricValue(String action, Object input) {
        if (runtimeServices == null) return null;
        Map<String, Object> arguments = CreatorRuntimeServiceArguments.output("action", action);
        if (input != null) arguments.put("input", input);
        CreatorRuntimeService.Result result = runtimeServices.dispatch("device_metrics", arguments);
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("value") : null;
    }

    private Object fileValue(Object path, String action, String outputKey) {
        if (runtimeServices == null || path == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("file",
                CreatorRuntimeServiceArguments.output("path", String.valueOf(path), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object bitmapValue(Object path, String action) {
        if (runtimeServices == null || path == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("bitmap",
                CreatorRuntimeServiceArguments.output("path", String.valueOf(path), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("value") : null;
    }

    private Object filePathValue(String action, Object directory) {
        if (runtimeServices == null) return null;
        Map<String, Object> arguments = CreatorRuntimeServiceArguments.output("action", action);
        if (directory != null) arguments.put("directory", String.valueOf(directory));
        CreatorRuntimeService.Result result = runtimeServices.dispatch("file", arguments);
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("path") : null;
    }

    private Object widgetQueryValue(Object widgetId, String action) {
        if (runtimeServices == null || widgetId == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("widget",
                CreatorRuntimeServiceArguments.output("widgetId", String.valueOf(widgetId), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("value") : null;
    }

    private Object drawerValue() {
        if (runtimeServices == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("drawer",
                CreatorRuntimeServiceArguments.output("action", "is_open"));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("value") : null;
    }

    private Object invokeMoreBlock(CreatorRuntimeEngine engine, String rawFunctionId, List<Object> values,
                                   List<Effect> effects) {
        String functionId = moreBlockId(rawFunctionId);
        if (engine == null || functionId.isEmpty()) return null;
        if (customFunctionDepth >= MAX_CUSTOM_FUNCTION_DEPTH) {
            effects.add(new Effect("more_block", "depth_capped:" + functionId));
            return null;
        }
        Object rawDefinitions = engine.getCurrent().getState().get("legacy.moreBlocks");
        if (!(rawDefinitions instanceof Map)) return null;
        Object rawDefinition = ((Map<?, ?>) rawDefinitions).get(functionId);
        if (!(rawDefinition instanceof Map)) return null;
        CreatorEventBinding binding = engine.getCurrent().getEvents().get("legacy_moreblock_" + functionId);
        if (binding == null) return null;
        @SuppressWarnings("unchecked") Map<String, Object> definition = (Map<String, Object>) rawDefinition;
        Map<String, Object> frame = new LinkedHashMap<>();
        Object rawArguments = definition.get("arguments");
        if (rawArguments instanceof List) {
            List<?> names = (List<?>) rawArguments;
            for (int i = 0; i < names.size(); i++) frame.put(String.valueOf(names.get(i)),
                    i < values.size() ? values.get(i) : defaultArgumentValue(definition.get("returnType")));
        }
        customFunctionDepth++;
        customFunctionFrames.push(frame);
        Object returnValue = null;
        try {
            Flow flow = executeBlocks(engine, binding.getBlocks(), effects);
            if (flow == Flow.RETURN) returnValue = frame.get(RETURN_VALUE_KEY);
        } finally {
            customFunctionFrames.pop();
            customFunctionDepth--;
        }
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    private List<Object> expressionValues(Object raw, CreatorRuntimeEngine engine) {
        if (!(raw instanceof List)) return Collections.emptyList();
        List<Object> values = new ArrayList<>();
        for (Object expression : (List<Object>) raw) values.add(evaluate(expression, engine));
        return values;
    }

    private Object currentCustomArgument(String name) {
        if (customFunctionFrames.isEmpty()) return null;
        return customFunctionFrames.peek().get(name == null ? "" : name.trim());
    }

    private static Object defaultArgumentValue(Object returnType) {
        String type = String.valueOf(returnType);
        if ("boolean".equalsIgnoreCase(type) || "b".equalsIgnoreCase(type)) return false;
        if ("double".equalsIgnoreCase(type) || "d".equalsIgnoreCase(type)) return 0d;
        return "";
    }

    private static String moreBlockId(String value) {
        if (value == null) return "";
        String result = value.trim();
        int space = result.indexOf(' ');
        if (space >= 0) result = result.substring(0, space);
        int bracket = result.indexOf('[');
        return (bracket >= 0 ? result.substring(0, bracket) : result).trim();
    }

    private Object liveWidgetValue(CreatorRuntimeEngine engine, Object widgetId, String action,
                                   String property, Object fallback) {
        Object live = widgetQueryValue(widgetId, action);
        return live == null ? widgetValue(engine, widgetId, property, fallback) : live;
    }

    private Object calendarTimestamp(Object componentId) {
        if (runtimeServices == null || componentId == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("calendar",
                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", "get_time"));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get("timestamp") : null;
    }

    private Object animatorValue(Object componentId, String action, String outputKey) {
        if (runtimeServices == null || componentId == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("animator",
                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object textToSpeechValue(Object componentId, String action, String outputKey) {
        if (runtimeServices == null || componentId == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("text_to_speech",
                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object storageValue(Object componentId, Object key, String action, String outputKey) {
        if (runtimeServices == null || componentId == null || key == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("local_storage",
                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action,
                        "key", String.valueOf(key)));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object intentValue(Object intentId, Object key, String action, String outputKey) {
        if (runtimeServices == null || intentId == null || key == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("intent",
                CreatorRuntimeServiceArguments.output("intentId", String.valueOf(intentId), "action", action,
                        "key", String.valueOf(key)));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object firebaseAuthValue(String outputKey) {
        if (runtimeServices == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("firebase_auth",
                CreatorRuntimeServiceArguments.output("action", "status"));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private Object bluetoothValue(Object componentId, String action, String outputKey) {
        if (runtimeServices == null || componentId == null) return null;
        CreatorRuntimeService.Result result = runtimeServices.dispatch("bluetooth",
                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    @SuppressWarnings("unchecked")
    private Object firebaseValue(CreatorRuntimeEngine engine, Object componentId, String action, String outputKey) {
        if (runtimeServices == null || componentId == null) return null;
        String id = String.valueOf(componentId);
        String path = "";
        Object rawComponents = engine.getCurrent().getState().get("legacy.components");
        if (rawComponents instanceof Map) {
            Object rawDescriptor = ((Map<?, ?>) rawComponents).get(id);
            if (rawDescriptor instanceof Map) {
                Object rawBase = ((Map<?, ?>) rawDescriptor).get("param1");
                path = rawBase == null ? "" : String.valueOf(rawBase).trim();
            }
        }
        while (path.startsWith("/")) path = path.substring(1);
        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        CreatorRuntimeService.Result result = runtimeServices.dispatch("firebase",
                CreatorRuntimeServiceArguments.output("componentId", id, "action", action, "path", path));
        return result.getStatus() == CreatorRuntimeService.Status.SUCCEEDED ? result.getOutput().get(outputKey) : null;
    }

    private static Object widgetValue(CreatorRuntimeEngine engine, Object widgetId, String property, Object fallback) {
        if (engine == null || widgetId == null) return fallback;
        CreatorWidget widget = engine.getCurrent().getWidgets().get(String.valueOf(widgetId));
        if (widget == null) return fallback;
        Object value = widget.getProperties().get(property);
        return value == null ? fallback : value;
    }

    @SuppressWarnings("unchecked")
    private static String literalName(Object rawArguments, Object evaluatedFallback) {
        if (rawArguments instanceof List && !((List<?>) rawArguments).isEmpty()) {
            Object rawFirst = ((List<?>) rawArguments).get(0);
            if (rawFirst instanceof Map) {
                Map<String, Object> literal = (Map<String, Object>) rawFirst;
                if ("literal".equals(literal.get("kind")) && literal.get("value") != null) {
                    return String.valueOf(literal.get("value"));
                }
            }
        }
        return String.valueOf(evaluatedFallback);
    }

    private static int boundedIndex(double value, int length) {
        if (Double.isNaN(value)) return 0;
        return Math.max(0, Math.min(length, (int) value));
    }

    private static Object randomInclusive(Object minimum, Object maximum) {
        long min = (long) decimal(minimum);
        long max = (long) decimal(maximum);
        long span = max - min + 1L;
        if (max < min || span <= 0L || span > Integer.MAX_VALUE) return null;
        return (double) (min + java.util.concurrent.ThreadLocalRandom.current().nextInt((int) span));
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static double decimal(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ignored) { return 0d; }
    }

    private static long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value == null) return 0L;
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private static java.util.List<Object> list(Object value) {
        java.util.List<Object> result = new ArrayList<>();
        if (value instanceof java.util.List) result.addAll((java.util.List<?>) value);
        return result;
    }

    private static Map<String, Object> map(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static List<?> listValue(Object value) {
        return value instanceof List ? (List<?>) value : Collections.emptyList();
    }

    private static Object at(List<?> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static Map<String, Object> mapValue(Object value) {
        return map(value);
    }
}
