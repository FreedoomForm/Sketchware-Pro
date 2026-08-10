package com.sketchware.ai.agent;

/**
 * Mode of the AI agent, mirroring Cline's AgentMode.
 *  - ACT: normal mode; tools require approval unless auto-approved
 *  - PLAN: read-only; agent only produces a plan, no tool calls
 *  - YOLO: auto-approve every tool call
 */
public enum AgentMode {
    ACT,
    PLAN,
    YOLO
}
