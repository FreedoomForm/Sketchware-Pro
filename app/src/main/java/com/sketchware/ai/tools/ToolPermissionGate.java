package com.sketchware.ai.tools;

import com.google.gson.JsonObject;
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
 *   <li>If a per-tool+subcategory override exists (for
 *       {@link CategoryUmbrellaTool} invocations) - use it.</li>
 *   <li>Otherwise fall back to the tool's {@link SketchwareTool#isAutoApprovedByDefault()}.</li>
 * </ol>
 *
 * <h2>Umbrella support (since 2026-08-12)</h2>
 *
 * <p>When the {@link CategoryUmbrellaTool} consolidation was introduced,
 * the gate gained the ability to set per-subcategory auto-approve
 * overrides. The key format is {@code "<umbrellaName>:<subcategory>"}
 * (e.g. {@code "manifest_manage:appcompat"}). The new
 * {@link #setToolSubcategoryAutoApprove} setter accepts the umbrella
 * name, subcategory, and boolean; {@link #decide(SketchwareTool, JsonObject)}
 * extracts the subcategory from the args (when present) and looks up
 * the fine-grained override before falling back to the per-tool override
 * or the tool's default.
 */
public class ToolPermissionGate {

    public enum Decision { AUTO_APPROVE, REQUIRE_APPROVAL, DENY }

    private AgentMode mode = AgentMode.ACT;
    private final Map<String, Boolean> perToolAutoApprove = new HashMap<>();
    /** Key format: "<umbrellaName>:<subcategory>". */
    private final Map<String, Boolean> perToolSubcategoryAutoApprove = new HashMap<>();

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

    /**
     * Set per-tool+subcategory auto-approve override. Only meaningful for
     * {@link CategoryUmbrellaTool} invocations — the subcategory is read
     * from the args' {@code "subcategory"} field at decision time.
     */
    public void setToolSubcategoryAutoApprove(String toolName, String subcategory, boolean autoApprove) {
        perToolSubcategoryAutoApprove.put(toolName + ":" + subcategory, autoApprove);
    }

    /** Get per-tool+subcategory override, or null if not set. */
    public Boolean getToolSubcategoryAutoApprove(String toolName, String subcategory) {
        if (subcategory == null) return null;
        return perToolSubcategoryAutoApprove.get(toolName + ":" + subcategory);
    }

    /** Legacy decide() — does not consult subcategory overrides. */
    public Decision decide(SketchwareTool tool) {
        return decide(tool, null);
    }

    /**
     * Decide with args — consults subcategory overrides when the tool
     * is a {@link CategoryUmbrellaTool} and the args contain a
     * {@code "subcategory"} field.
     */
    public Decision decide(SketchwareTool tool, JsonObject args) {
        if (tool == null) return Decision.DENY;
        // Both RESEARCH and PLAN modes are read-only — only read-only
        // tools are allowed. Mirrors AutoApprover's RESEARCH+PLAN gate.
        if (mode == AgentMode.PLAN || mode == AgentMode.RESEARCH) {
            return tool.isReadOnly() ? Decision.AUTO_APPROVE : Decision.DENY;
        }
        if (mode == AgentMode.YOLO) {
            return Decision.AUTO_APPROVE;
        }
        // Per-tool+subcategory override (highest precedence).
        if (args != null && args.has("subcategory") && !args.get("subcategory").isJsonNull()) {
            String subcat = args.get("subcategory").getAsString();
            Boolean subOverride = perToolSubcategoryAutoApprove.get(tool.name() + ":" + subcat);
            if (subOverride != null) {
                return subOverride ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
            }
        }
        // Per-tool override.
        Boolean override = perToolAutoApprove.get(tool.name());
        if (override != null) {
            return override ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
        }
        return tool.isAutoApprovedByDefault() ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
    }
}
