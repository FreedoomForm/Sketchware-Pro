package com.sketchware.ai.tools;

import com.google.gson.JsonObject;

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
}
