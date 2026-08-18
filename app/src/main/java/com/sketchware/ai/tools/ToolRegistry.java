package com.sketchware.ai.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of all tools available to the AI agent.
 * Mirrors Cline's {@code Map<String, AgentTool>}.
 *
 * <p>Registration order is preserved so that the system prompt lists tools
 * in a stable, predictable order.
 */
public final class ToolRegistry {

    private final Map<String, SketchwareTool> tools = new LinkedHashMap<>();

    public ToolRegistry register(SketchwareTool tool) {
        if (tool == null || tool.name() == null) {
            throw new IllegalArgumentException("tool and tool.name() must not be null");
        }
        if (tools.containsKey(tool.name())) {
            throw new IllegalStateException("Tool already registered: " + tool.name());
        }
        tools.put(tool.name(), tool);
        return this;
    }

    public SketchwareTool get(String name) {
        return tools.get(name);
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    public Collection<SketchwareTool> all() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public int size() {
        return tools.size();
    }

    /**
     * Comma-separated sample of registered tool names for inclusion in error
     * messages (e.g. "Unknown tool: 'foo'. Available tools: view_add_widget,
     * view_set_property, ..."). Truncated to the first 20 names to avoid
     * overflowing the LLM context window.
     */
    public String toolNamesSample() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SketchwareTool t : tools.values()) {
            if (count > 0) sb.append(", ");
            if (count >= 20) {
                sb.append("... (").append(tools.size() - 20).append(" more)");
                break;
            }
            sb.append(t.name());
            count++;
        }
        return sb.toString();
    }

    /**
     * Build a JSON array of tool definitions in the AI-SDK canonical shape.
     * Each entry: {@code { name, description, inputSchema: <jsonSchema> }}.
     */
    public String toJsonSchemas() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (SketchwareTool t : tools.values()) {
            if (!first) sb.append(",");
            first = false;
            JsonObject entry = new JsonObject();
            entry.addProperty("name", t.name());
            entry.addProperty("description", t.description());
            entry.add("inputSchema", t.jsonSchema());
            sb.append(entry.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Attempt to infer the intended tool from its arguments when the provider
     * returned an empty or {@code "unknown"} tool name.
     *
     * <p>This is a fallback for proxies (notably AgentRouter when proxying
     * Claude) that occasionally emit {@code "name":""} in tool_call deltas.
     * The model clearly knows which tool it wanted (the arguments match a
     * specific tool's schema), but the name was stripped in transit. Without
     * this inference, the tool executor returns {@code "Unknown tool: ''"},
     * the model retries the same empty-name call, and the loop detector
     * escalates to an abort — leaving the user with a dead chat.
     *
     * <p><b>Matching algorithm</b> (conservative,优先 precision over recall):
     * <ol>
     *   <li>Parse {@code argsJson} as a JSON object. If parsing fails, return
     *       {@code null} (no inference possible).</li>
     *   <li>For each registered tool, extract its schema's property names
     *       (from the {@code properties} object) and required-field names.</li>
     *   <li>A tool is a <b>candidate</b> if:
     *       <ul>
     *         <li>Every required field in the schema is present in {@code argsJson}, AND</li>
     *         <li>Every key in {@code argsJson} exists in the schema's properties.</li>
     *       </ul>
     *       This means the args are a valid instantiation of the schema
     *       (modulo type-checking, which we skip for performance).</li>
     *   <li>If exactly one candidate remains, return it. If zero or multiple
     *       candidates remain, return {@code null} — ambiguous inference is
     *       worse than no inference because it could silently invoke the
     *       wrong tool.</li>
     * </ol>
     *
     * <p>Example: args {@code {"question":"..."}} matches only
     * {@code ask_question} (the only tool with a {@code question} property),
     * so the inference succeeds. Args {@code {"action":"delete"}} would match
     * many universal tools and thus return {@code null}.
     *
     * @param argsJson the raw JSON arguments string from the tool_call
     * @return the inferred tool, or {@code null} if inference is ambiguous
     */
    public SketchwareTool inferFromArgs(String argsJson) {
        if (argsJson == null || argsJson.isEmpty() || "{}".equals(argsJson)) {
            return null;
        }
        JsonObject args;
        try {
            args = JsonParser.parseString(argsJson).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
        if (args.size() == 0) return null;

        // Umbrella calls carry the public `subcategory` selector plus
        // action-specific arguments that intentionally are not enumerated in
        // the umbrella's outer schema. Resolve a unique matching umbrella
        // before the generic schema matcher, which cannot distinguish shared
        // action names such as "list" or "create" by itself.
        SketchwareTool umbrella = inferUmbrellaFromSubcategory(args);
        if (umbrella != null) return umbrella;

        SketchwareTool match = null;
        int matchCount = 0;
        for (SketchwareTool t : tools.values()) {
            if (isCandidate(args, t)) {
                match = t;
                matchCount++;
                if (matchCount > 1) return null;  // ambiguous — bail
            }
        }
        return match;
    }

    private SketchwareTool inferUmbrellaFromSubcategory(JsonObject args) {
        if (!args.has("subcategory") || args.get("subcategory").isJsonNull()) return null;
        String requested;
        try {
            requested = args.get("subcategory").getAsString();
        } catch (Exception ignored) {
            return null;
        }
        SketchwareTool match = null;
        for (SketchwareTool tool : tools.values()) {
            if (!(tool instanceof CategoryUmbrellaTool)) continue;
            JsonObject schema = tool.jsonSchema();
            if (schema == null || !schema.has("properties")) continue;
            JsonObject props = schema.getAsJsonObject("properties");
            if (!props.has("subcategory")) continue;
            JsonObject subcategory = props.getAsJsonObject("subcategory");
            if (!subcategory.has("enum")) continue;
            boolean supportsRequested = false;
            for (JsonElement value : subcategory.getAsJsonArray("enum")) {
                if (value.isJsonPrimitive() && requested.equals(value.getAsString())) {
                    supportsRequested = true;
                    break;
                }
            }
            if (!supportsRequested) continue;
            if (match != null) return null; // ambiguous selector: do not guess.
            match = tool;
        }
        return match;
    }

    /**
     * Check whether {@code args} is a valid instantiation of {@code tool}'s
     * schema: every required field is present, and every arg key exists in
     * the schema's properties.
     */
    private boolean isCandidate(JsonObject args, SketchwareTool tool) {
        JsonObject schema;
        try {
            schema = tool.jsonSchema();
        } catch (Throwable t) {
            return false;
        }
        if (schema == null || !schema.has("properties")) {
            // No properties declared — only matches if args is empty (which
            // we already excluded in the caller).
            return false;
        }
        JsonObject props = schema.getAsJsonObject("properties");
        // Every key in args must exist in schema properties unless the tool
        // explicitly accepts action-specific additional properties. Category
        // umbrellas deliberately use this to forward the selected
        // subtool's parameters after matching their `subcategory` + `action`.
        boolean allowsAdditionalProperties = !schema.has("additionalProperties")
                || (schema.get("additionalProperties").isJsonPrimitive()
                && schema.get("additionalProperties").getAsBoolean());
        for (String key : args.keySet()) {
            if (!props.has(key) && !allowsAdditionalProperties) return false;
        }
        // Every required field must be present in args.
        if (schema.has("required") && schema.get("required").isJsonArray()) {
            for (JsonElement e : schema.getAsJsonArray("required")) {
                if (!e.isJsonPrimitive()) continue;
                String req = e.getAsString();
                if (!args.has(req)) return false;
            }
        }
        return true;
    }
}
