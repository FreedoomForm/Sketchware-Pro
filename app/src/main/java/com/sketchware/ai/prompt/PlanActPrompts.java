package com.sketchware.ai.prompt;

/**
 * Plan / Act mode prompt sections. Direct port of Cline's
 * {@code sdk/packages/shared/src/prompt/cline.ts}:
 * <ul>
 *   <li>{@link #MODE_TAG_INSTRUCTIONS} — verbatim port of
 *       {@code MODE_TAG_INSTRUCTIONS}. Explains the
 *       {@code <user_input mode="...">} wrapper and {@code <mode_notice>}
 *       blocks the runtime stamps on user messages. Included for BOTH
 *       modes, since after a switch the transcript still contains
 *       messages tagged with the other mode.</li>
 *   <li>{@link #PLAN_MODE_INSTRUCTIONS} — verbatim port of
 *       {@code PLAN_MODE_INSTRUCTIONS}. The plan-mode behavioral
 *       contract, with {@code switch_to_act_mode} tool semantics
 *       (CLI-style).</li>
 *   <li>{@link #PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH} — verbatim port
 *       of {@code PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH}. Same contract
 *       but for hosts that do NOT expose {@code switch_to_act_mode}
 *       (the VS Code extension). Sketchware Pro uses this variant —
 *       the user toggles Plan/Act manually via the segmented control
 *       in the chat bar, there is no {@code switch_to_act_mode} tool
 *       in the toolset.</li>
 * </ul>
 *
 * <p>Why port verbatim instead of paraphrasing: Cline's prompts are the
 * product of many iterations of model-feedback tuning. The wording
 * ("explore, analyze, and plan -- not to execute", "the run_commands tool
 * remains available in plan mode strictly for read-only inspection",
 * "never treat the original task request as approval") is calibrated to
 * make frontier models actually obey the plan-mode contract. Paraphrasing
 * risks losing the precise behavioral cues that make the contract stick.
 *
 * <p>Behavioral contract this enables:
 * <ul>
 *   <li>In PLAN mode, the agent reads files, searches, asks clarifying
 *       questions, and presents a structured plan. It does NOT edit
 *       files or run state-changing commands.</li>
 *   <li>The {@link com.sketchware.ai.tools.ToolPermissionGate} enforces
 *       this hard — write tools return {@code Decision.DENY} in PLAN
 *       mode, so even if the model tries to call them, they fail. The
 *       prompt is the first line of defense; the gate is the hard
 *       backstop. Mirrors Cline's "prompting is the first line of
 *       defense; the plan-mode command-guard hook is the hard
 *       backstop".</li>
 *   <li>When the user approves the plan, they flip the segmented toggle
 *       to Act. The next user message is wrapped with
 *       {@code mode="act"}, the system prompt is rebuilt with the
 *       plan-mode contract removed, and the agent begins execution.</li>
 * </ul>
 *
 * <p>Source: https://github.com/cline/cline/blob/main/sdk/packages/shared/src/prompt/cline.ts
 * Lines 22-63 (as of commit on the main branch at port time).
 */
public final class PlanActPrompts {

    private PlanActPrompts() {}

    /**
     * Explains the {@code <user_input mode="...">} wrapper and
     * {@code <mode_notice>} elements the runtime stamps on user messages.
     * Every host that sends through the SDK runtime produces those tags,
     * so every host's system prompt must explain them: without this
     * section the model has no idea what the attribute means, and a
     * mid-conversation mode switch is an invisible system-prompt swap it
     * cannot diff. Included for BOTH modes, since after a switch the
     * transcript still contains messages tagged with the other mode.
     *
     * <p>Verbatim port of {@code MODE_TAG_INSTRUCTIONS} from
     * {@code cline.ts}.
     */
    public static final String MODE_TAG_INSTRUCTIONS =
        "# Plan / Act Modes\n\n" +
        "User messages arrive wrapped in a <user_input mode=\"...\"> tag. The mode attribute is the interaction mode the user was in when they sent that message: \"plan\" means plan-mode constraints applied (explore, analyze, and align on a plan -- no edits or state-changing commands), while \"act\" (or \"yolo\") means implementation was allowed. If the mode attribute changes between messages, the user switched modes -- the newest message's mode is what governs right now, regardless of what earlier messages allowed. A <mode_notice> block inside a message marks exactly when such a switch happened.";

    /**
     * Base plan-mode contract (without the switch-to-act tail). Verbatim
     * port of {@code PLAN_MODE_INSTRUCTIONS_BASE} from {@code cline.ts}.
     * Used as the building block for both {@link #PLAN_MODE_INSTRUCTIONS}
     * and {@link #PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH}.
     *
     * <p>The {@code run_commands} tool in Cline maps to our
     * {@code run_command} tool. In Sketchware Pro the equivalent
     * state-changing tools are {@code write_file}, {@code edit_file},
     * {@code apply_patch}, {@code diff_edit_file}, {@code view_add_widget},
     * {@code view_set_property}, {@code view_delete_widget},
     * {@code java_edit_file}, {@code manifest_manage}, etc. — all
     * mutating tools are blocked by {@link ToolPermissionGate} in
     * PLAN mode. Read-only tools ({@code read_file}, {@code list_files},
     * {@code search_files}, {@code view_list_widgets},
     * {@code view_list_layouts}, {@code web_search}, {@code web_fetch})
     * remain available, matching Cline's "run_commands remains available
     * strictly for read-only inspection" semantics.
     */
    static final String PLAN_MODE_INSTRUCTIONS_BASE =
        "# Plan Mode\n\n" +
        "You are in Plan mode. Your role is to explore, analyze, and plan -- not to execute.\n\n" +
        "- Read files, search the codebase, and gather context to understand the problem\n" +
        "- Ask clarifying questions when requirements are ambiguous\n" +
        "- Present your plan as a structured outline with clear steps\n" +
        "- Explain tradeoffs between different approaches when they exist\n" +
        "- Do NOT edit files, write code, run destructive commands, or make any changes\n" +
        "- Do NOT implement anything -- focus on understanding and alignment first\n\n" +
        "The run_commands tool remains available in plan mode strictly for read-only inspection -- listing files, searching (grep), reading configs, inspecting git history and diffs, checking tool versions, and the like. Never use it to change anything: no creating, modifying, or deleting files, no writing scripts that make changes, and no state-changing commands (installs, migrations, database or schema changes, container commands that mutate state, etc.). File-editing commands (rm/mv/cp, in-place edits like sed -i, output redirection to files outside /tmp, git commands that change the working tree, package installs) are hard-blocked in plan mode: they are not executed and return a tool error instead, so do not attempt them. If the task requires a mutation, put it in the plan; it happens only after the user switches to act mode.";

    /**
     * Plan-mode contract with the {@code switch_to_act_mode} tool tail.
     * Used by hosts that expose the {@code switch_to_act_mode} tool in
     * plan mode (Cline CLI default).
     *
     * <p>Sketchware Pro does NOT use this variant — there is no
     * {@code switch_to_act_mode} tool in the toolset. We use
     * {@link #PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH} instead. This
     * constant is kept for completeness and for future use if a
     * {@code switch_to_act_mode} tool is ever added.
     *
     * <p>Verbatim port of {@code PLAN_MODE_INSTRUCTIONS} from
     * {@code cline.ts}.
     */
    public static final String PLAN_MODE_INSTRUCTIONS =
        PLAN_MODE_INSTRUCTIONS_BASE + "\n\n" +
        "Once the user has reviewed your plan and explicitly approved it in a follow-up message, use the switch_to_act_mode tool to switch to act mode and begin implementation. Calling switch_to_act_mode immediately starts execution, so never call it in the same turn you present a plan and never treat the original task request as approval -- end your turn after presenting the plan and wait for the user's response.";

    /**
     * Plan-mode contract for hosts that do NOT expose the
     * {@code switch_to_act_mode} tool. The model must direct the user to
     * flip the Plan/Act toggle instead of calling a tool that does not
     * exist in its toolset.
     *
     * <p>This is the variant Sketchware Pro uses — the segmented
     * Act/Plan toggle in the chat bar is the only way to switch modes,
     * there is no {@code switch_to_act_mode} tool. Mirrors the VS Code
     * extension's behavior (matching the legacy extension's behavior,
     * per the Cline source comment).
     *
     * <p>Verbatim port of {@code PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH}
     * from {@code cline.ts}.
     */
    public static final String PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH =
        PLAN_MODE_INSTRUCTIONS_BASE + "\n\n" +
        "Once you have presented your plan, end your turn and wait for the user's response. You do NOT have the ability to switch to act mode yourself -- the user must do it manually with the Plan/Act toggle once they are satisfied with the plan. If the task requires tools that are only available in act mode, ask the user to \"toggle to Act mode\" (use those words).";
}
