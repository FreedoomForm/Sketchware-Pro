package com.sketchware.ai.tools;

import com.google.gson.JsonObject;

/**
 * Dispatches tool calls to the registered {@link SketchwareTool} instances.
 * Mirrors Cline's {@code executeTool} flow.
 */
public class ToolExecutor {

    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * Execute a tool by name with the given arguments.
     *
     * <p>Catches all exceptions and converts them to {@link ToolResult#error(String)}.
     */
    public ToolResult execute(String toolName, JsonObject args, SketchwareToolContext ctx) {
        SketchwareTool tool = registry.get(toolName);
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + toolName);
        }
        try {
            return tool.execute(args, ctx);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    public ToolRegistry getRegistry() {
        return registry;
    }
}
