package com.sketchware.ai.tools;

import com.google.gson.JsonObject;

/**
 * One tool exposed to the AI agent. Mirrors the {@code AgentTool} interface
 * from Cline's {@code sdk/packages/core/src/extensions/tools/definitions.ts}.
 *
 * <p>Every concrete tool is registered in {@link ToolRegistry} and invoked
 * through {@link ToolExecutor#execute(String, JsonObject, SketchwareToolContext)}.
 *
 * <p><b>Crucial design principle</b>: tools MUST only mutate Sketchware-Pro's
 * project state via the existing singletons {@code jC.a/b/c/d(sc_id)} — they
 * MUST NEVER write to project files directly. This guarantees that every
 * change the AI makes is visible in real time in the Sketchware UI.
 */
public interface SketchwareTool {

    /** Snake-case identifier used in tool calls, e.g. {@code "view_add_widget"}. */
    String name();

    /** Human-readable description for the system prompt. */
    String description();

    /** Category for grouping in the UI ("view", "component", "event", "block", ...). */
    String category();

    /**
     * JSON Schema describing the arguments. Will be serialized to the LLM's
     * tool/function definition.
     */
    JsonObject jsonSchema();

    /**
     * Execute the tool with the given arguments. MUST be non-blocking for
     * short operations; for long operations, the executor service handles
     * threading.
     *
     * @param args parsed JSON arguments (validated against {@link #jsonSchema()})
     * @param ctx  the project context (sc_id, current layout, etc.)
     * @return the result; either success with output text or an error
     */
    ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception;

    /** Whether this tool is auto-approved by default (read-only tools typically true). */
    default boolean isAutoApprovedByDefault() {
        return false;
    }

    /** Whether this tool is read-only (does not mutate any project state). */
    default boolean isReadOnly() {
        return false;
    }
}
