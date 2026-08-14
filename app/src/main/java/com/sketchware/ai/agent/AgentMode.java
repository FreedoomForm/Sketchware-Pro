package com.sketchware.ai.agent;

/**
 * Mode of the AI agent, mirroring Cline's AgentMode + the new RESEARCH
 * mode added in Cline 3.x.
 *
 * <p>Mode ladder (in order of capability escalation):
 * <ul>
 *   <li><b>RESEARCH</b> — read-only exploration. The agent gathers
 *       context via web_search, web_fetch, list_files, search_files,
 *       read_file, view_list_widgets, view_list_layouts, java_read_file.
 *       No edits, no state-changing commands. At the end of research,
 *       the agent produces a structured research summary that's carried
 *       forward into the next PLAN session. New in Cline 3.x.</li>
 *   <li><b>PLAN</b> — read-only analysis + plan. The agent reads files,
 *       asks clarifying questions, and presents a structured plan. No
 *       edits, no state-changing commands. Once the user approves, they
 *       toggle to ACT.</li>
 *   <li><b>ACT</b> — normal implementation mode. Write tools require
 *       approval unless auto-approved.</li>
 *   <li><b>YOLO</b> — auto-approve every tool call. Use with care.</li>
 * </ul>
 *
 * <p>Typical flow: RESEARCH → PLAN → ACT. The user can skip any stage
 * (e.g. start in ACT for a quick fix) or move back (e.g. ACT → PLAN
 * when the implementation reveals a missing requirement).
 */
public enum AgentMode {
    RESEARCH,
    PLAN,
    ACT,
    YOLO
}
