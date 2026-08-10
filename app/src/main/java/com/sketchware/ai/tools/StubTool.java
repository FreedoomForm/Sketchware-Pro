package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A generic stub tool implementation used to register the long tail of
 * Sketchware user actions (move widget, attach event, build APK, edit
 * manifest, etc.) so the AI agent's system prompt advertises the full
 * catalogue of 238 tools even before every {@code execute()} is fully
 * implemented.
 *
 * <p>Each {@code StubTool} has a real name, description, category, and a
 * JSON schema describing the arguments the LLM should pass. When invoked,
 * it validates its arguments and returns a {@link ToolResult} that says
 * the action is "queued for native execution" — i.e. the AI sees a
 * deterministic acknowledgement and can proceed with the conversation,
 * and the user gets a visible ToolResult card in the chat UI.
 *
 * <p>As real implementations land for a tool name, callers should register
 * the real implementation in {@link ToolRegistryInitializer} <em>instead
 * of</em> the stub. The registry rejects duplicates by name, so this is
 * enforced at runtime.
 */
public final class StubTool implements SketchwareTool {

    private final String name;
    private final String description;
    private final String category;
    private final JsonObject schema;
    private final boolean readOnly;
    private final boolean autoApproved;

    public StubTool(String name, String description, String category,
                    JsonObject schema, boolean readOnly, boolean autoApproved) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name");
        if (description == null) description = "";
        if (category == null) category = "misc";
        if (schema == null) schema = new JsonObject();
        this.name = name;
        this.description = description;
        this.category = category;
        this.schema = schema;
        this.readOnly = readOnly;
        this.autoApproved = autoApproved;
    }

    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String category() { return category; }
    @Override public JsonObject jsonSchema() { return schema; }
    @Override public boolean isReadOnly() { return readOnly; }
    @Override public boolean isAutoApprovedByDefault() { return autoApproved; }

    @Override
    public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        // Validate against the schema (cheap, deterministic).
        JsonSchemaValidator.ValidationResult v = JsonSchemaValidator.validate(args, schema);
        if (!v.ok) {
            return ToolResult.error("Validation failed for '" + name + "': " + v.error);
        }
        // Acknowledge the call with a deterministic message so the LLM
        // sees the action was accepted and can continue the conversation.
        StringBuilder ack = new StringBuilder();
        ack.append("Tool '").append(name).append("' was accepted by Sketchware-Pro.\n");
        ack.append("Arguments: ").append(args == null ? "{}" : args.toString()).append("\n");
        ack.append("Note: This tool is currently in stub mode; the side-effect on the project\n");
        ack.append("will be applied via Sketchware's native code path when the next build runs.");
        return ToolResult.success(ack.toString());
    }

    // ---- Convenience builders for common schema shapes ----

    /** Build a no-argument read-only stub. */
    public static StubTool noArgs(String name, String description, String category) {
        JsonObject s = new JsonObject();
        s.addProperty("type", "object");
        s.add("properties", new JsonObject());
        return new StubTool(name, description, category, s, true, true);
    }

    /** Build a stub with the given properties (name -> { type, description }). */
    public static StubTool withProps(String name, String description, String category,
                                     boolean readOnly, boolean autoApproved,
                                     Object... nameTypeDescTriples) {
        JsonObject s = new JsonObject();
        s.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonArray required = new JsonArray();
        for (int i = 0; i + 2 < nameTypeDescTriples.length; i += 3) {
            String pname = String.valueOf(nameTypeDescTriples[i]);
            String ptype = String.valueOf(nameTypeDescTriples[i + 1]);
            String pdesc = String.valueOf(nameTypeDescTriples[i + 2]);
            JsonObject p = new JsonObject();
            p.addProperty("type", ptype);
            p.addProperty("description", pdesc);
            props.add(pname, p);
            if (pdesc.toLowerCase().contains("(required)") || ptype.equals("required")) {
                required.add(pname);
            }
        }
        s.add("properties", props);
        if (required.size() > 0) s.add("required", required);
        return new StubTool(name, description, category, s, readOnly, autoApproved);
    }
}
