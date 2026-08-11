package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class for "universal" tools: one tool that dispatches to many actions
 * via an {@code action} enum parameter. This collapses the original catalogue
 * of 240 tiny specialized tools into ~30 universal tools, which is far more
 * token-efficient for the LLM while preserving full functional coverage.
 *
 * <p>Subclasses declare the supported actions via {@link #supportedActions()}
 * and implement {@link #dispatch(String, JsonObject, SketchwareToolContext)}.
 * Argument validation against the JSON schema is handled here.
 *
 * <p>Each action corresponds to a previously-stubbed operation in the long
 * tail of Sketchware user actions (move widget, attach event, build APK,
 * edit manifest, etc.). The dispatch method invokes Sketchware's native
 * singletons ({@code jC.a/b/c/d(sc_id)}) via reflection through
 * {@link com.sketchware.ai.util.SketchwareApi}.
 */
public abstract class UniversalTool implements SketchwareTool {

    private final String name;
    private final String description;
    private final String category;
    private final boolean readOnly;
    private final boolean autoApproved;
    private final String[] actions;

    protected UniversalTool(String name, String description, String category,
                            boolean readOnly, boolean autoApproved,
                            String... actions) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name");
        if (actions == null || actions.length == 0)
            throw new IllegalArgumentException("actions");
        this.name = name;
        this.description = description == null ? "" : description;
        this.category = category == null ? "misc" : category;
        this.readOnly = readOnly;
        this.autoApproved = autoApproved;
        // Deduplicate while preserving insertion order.
        Set<String> seen = new LinkedHashSet<>(Arrays.asList(actions));
        this.actions = seen.toArray(new String[0]);
    }

    @Override public final String name() { return name; }
    @Override public final String description() { return description; }
    @Override public final String category() { return category; }
    @Override public final boolean isReadOnly() { return readOnly; }
    @Override public final boolean isAutoApprovedByDefault() { return autoApproved; }

    /** Sorted, deduplicated list of supported actions (used in JSON schema enum). */
    public final String[] getActions() { return actions.clone(); }

    @Override
    public final JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description", "Operation to perform. Must be one of the enum values.");
        JsonArray actionEnum = new JsonArray();
        for (String a : actions) actionEnum.add(a);
        actionProp.add("enum", actionEnum);
        props.add("action", actionProp);

        // Allow subclasses to inject additional property schemas (e.g. widget_id,
        // target_id, value, etc.). Subclasses should add properties keyed by
        // name; the "action" enum is always present.
        addExtraProperties(props);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        return schema;
    }

    @Override
    public final ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (args == null) return ToolResult.error("args is null");
        JsonSchemaValidator.ValidationResult v = JsonSchemaValidator.validate(args, jsonSchema());
        if (!v.ok) {
            return ToolResult.error("Validation failed for '" + name + "': " + v.error);
        }
        String action = args.has("action") && !args.get("action").isJsonNull()
                ? args.get("action").getAsString() : null;
        if (action == null) return ToolResult.error("action is required");
        boolean found = false;
        for (String a : actions) if (a.equals(action)) { found = true; break; }
        if (!found) return ToolResult.error("Unknown action '" + action + "'. Supported: " + Arrays.toString(actions));
        return dispatch(action, args, ctx);
    }

    /** Override to add extra property schemas beyond the mandatory "action" enum. */
    protected void addExtraProperties(JsonObject props) {}

    /** Subclass dispatch: perform the requested action. */
    protected abstract ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception;

    // ---- shared helpers for subclasses ----

    /** Read a string argument, returning null if absent or null. */
    protected static String optString(JsonObject args, String key) {
        if (args.has(key) && !args.get(key).isJsonNull()) {
            return args.get(key).getAsString();
        }
        return null;
    }

    /** Read a string argument, returning the default if absent or null. */
    protected static String optString(JsonObject args, String key, String def) {
        String v = optString(args, key);
        return v == null ? def : v;
    }

    /** Read an int argument, returning the default if absent. */
    protected static int optInt(JsonObject args, String key, int def) {
        if (args.has(key) && !args.get(key).isJsonNull()) {
            try { return args.get(key).getAsInt(); } catch (Throwable ignored) {}
        }
        return def;
    }

    /** Read a boolean argument, returning the default if absent. */
    protected static boolean optBool(JsonObject args, String key, boolean def) {
        if (args.has(key) && !args.get(key).isJsonNull()) {
            try { return args.get(key).getAsBoolean(); } catch (Throwable ignored) {}
        }
        return def;
    }

    /**
     * Require a string argument: returns the value if present and non-empty,
     * otherwise throws an IllegalStateException with a useful message.
     *
     * <p>Previous version was a no-op stub that always returned null — the
     * comment said "caller checks for null", but callers had no way to
     * distinguish "missing" from "present". This is now a proper helper.
     *
     * <p>Usage: {@code String id = requireString(args, "widget_id");}
     */
    protected static String requireString(JsonObject args, String key) {
        String v = optString(args, key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Missing required argument: " + key);
        }
        return v;
    }

    /** Convenience: error result with the tool name prefix. */
    protected final ToolResult err(String msg) {
        return ToolResult.error("[" + name + "] " + msg);
    }

    /** Convenience: success result with the tool name prefix. */
    protected final ToolResult ok(String msg) {
        return ToolResult.success("[" + name + "] " + msg);
    }
}
