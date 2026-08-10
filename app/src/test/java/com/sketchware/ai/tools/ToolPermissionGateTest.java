package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.sketchware.ai.agent.AgentMode;

/**
 * Unit tests for {@link ToolPermissionGate}.
 */
public class ToolPermissionGateTest {

    @Test public void yoloAlwaysAutoApproves() {
        ToolPermissionGate gate = new ToolPermissionGate();
        gate.setMode(AgentMode.YOLO);
        SketchwareTool writeTool = new FakeTool("write_thing", false);
        assertThat(gate.decide(writeTool)).isEqualTo(ToolPermissionGate.Decision.AUTO_APPROVE);
    }

    @Test public void planDeniesWriteTools() {
        ToolPermissionGate gate = new ToolPermissionGate();
        gate.setMode(AgentMode.PLAN);
        SketchwareTool writeTool = new FakeTool("write_thing", false);
        assertThat(gate.decide(writeTool)).isEqualTo(ToolPermissionGate.Decision.DENY);
    }

    @Test public void planAllowsReadOnlyTools() {
        ToolPermissionGate gate = new ToolPermissionGate();
        gate.setMode(AgentMode.PLAN);
        SketchwareTool readTool = new FakeTool("list_things", true);
        assertThat(gate.decide(readTool)).isEqualTo(ToolPermissionGate.Decision.AUTO_APPROVE);
    }

    @Test public void actUsesToolDefaultWhenNoOverride() {
        ToolPermissionGate gate = new ToolPermissionGate();
        gate.setMode(AgentMode.ACT);
        SketchwareTool autoApproveTool = new FakeTool("safe_tool", true);
        SketchwareTool requireApprovalTool = new FakeTool("dangerous_tool", false);
        assertThat(gate.decide(autoApproveTool)).isEqualTo(ToolPermissionGate.Decision.AUTO_APPROVE);
        assertThat(gate.decide(requireApprovalTool)).isEqualTo(ToolPermissionGate.Decision.REQUIRE_APPROVAL);
    }

    @Test public void actOverridesToolDefault() {
        ToolPermissionGate gate = new ToolPermissionGate();
        gate.setMode(AgentMode.ACT);
        SketchwareTool requireApprovalTool = new FakeTool("dangerous_tool", false);
        assertThat(gate.decide(requireApprovalTool)).isEqualTo(ToolPermissionGate.Decision.REQUIRE_APPROVAL);
        gate.setToolAutoApprove("dangerous_tool", true);
        assertThat(gate.decide(requireApprovalTool)).isEqualTo(ToolPermissionGate.Decision.AUTO_APPROVE);
        SketchwareTool autoApproveTool = new FakeTool("safe_tool", true);
        gate.setToolAutoApprove("safe_tool", false);
        assertThat(gate.decide(autoApproveTool)).isEqualTo(ToolPermissionGate.Decision.REQUIRE_APPROVAL);
    }

    @Test public void nullToolReturnsDeny() {
        ToolPermissionGate gate = new ToolPermissionGate();
        assertThat(gate.decide(null)).isEqualTo(ToolPermissionGate.Decision.DENY);
    }

    static final class FakeTool implements SketchwareTool {
        private final String name;
        private final boolean readOnly;
        FakeTool(String name, boolean readOnly) { this.name = name; this.readOnly = readOnly; }
        @Override public String name() { return name; }
        @Override public String description() { return "fake"; }
        @Override public String category() { return "test"; }
        @Override public com.google.gson.JsonObject jsonSchema() { return new com.google.gson.JsonObject(); }
        @Override public ToolResult execute(com.google.gson.JsonObject args, SketchwareToolContext ctx) {
            return ToolResult.success("ok");
        }
        @Override public boolean isReadOnly() { return readOnly; }
        @Override public boolean isAutoApprovedByDefault() { return readOnly; }
    }
}
