package com.sketchware.ai.tools;

import com.sketchware.ai.agent.AgentMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves whether a given tool invocation should be auto-approved or
 * requires explicit user approval. Mirrors Cline's
 * {@code ToolPolicy { enabled, autoApprove }} semantics.
 *
 * <p>Resolution rules (in priority order):
 * <ol>
 *   <li>If mode is {@link AgentMode#YOLO} - always auto-approve.</li>
 *   <li>If mode is {@link AgentMode#PLAN} - never approve (read-only only).</li>
 *   <li>If a per-tool override exists in user settings - use it.</li>
 *   <li>Otherwise fall back to the tool's {@link SketchwareTool#isAutoApprovedByDefault()}.</li>
 * </ol>
 */
public class ToolPermissionGate {

    public enum Decision { AUTO_APPROVE, REQUIRE_APPROVAL, DENY }

    private AgentMode mode = AgentMode.ACT;
    private final Map<String, Boolean> perToolAutoApprove = new HashMap<>();

    public void setMode(AgentMode mode) {
        this.mode = mode;
    }

    public AgentMode getMode() {
        return mode;
    }

    /** Set per-tool auto-approve override (from user settings). */
    public void setToolAutoApprove(String toolName, boolean autoApprove) {
        perToolAutoApprove.put(toolName, autoApprove);
    }

    /** Get per-tool override, or null if not set. */
    public Boolean getToolAutoApprove(String toolName) {
        return perToolAutoApprove.get(toolName);
    }

    public Decision decide(SketchwareTool tool) {
        if (tool == null) return Decision.DENY;
        if (mode == AgentMode.PLAN) {
            // In plan mode only read-only tools are allowed.
            return tool.isReadOnly() ? Decision.AUTO_APPROVE : Decision.DENY;
        }
        if (mode == AgentMode.YOLO) {
            return Decision.AUTO_APPROVE;
        }
        Boolean override = perToolAutoApprove.get(tool.name());
        if (override != null) {
            return override ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
        }
        return tool.isAutoApprovedByDefault() ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
    }
}
