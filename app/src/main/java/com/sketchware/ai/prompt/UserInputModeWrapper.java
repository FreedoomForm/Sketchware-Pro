package com.sketchware.ai.prompt;

import com.sketchware.ai.agent.AgentMode;

/**
 * Wraps user input in a {@code <user_input mode="...">} tag and formats
 * {@code <mode_notice>} blocks for mode switches. Direct port of Cline's
 * {@code prepareTurnInput} / {@code formatUserInputBlock} /
 * {@code formatModeNotice} conventions from
 * {@code sdk/packages/core/src/runtime/}.
 *
 * <p>Why this matters: the system prompt's
 * {@link PlanActPrompts#MODE_TAG_INSTRUCTIONS} tells the model that user
 * messages arrive wrapped in {@code <user_input mode="...">} tags. Without
 * actually wrapping them, the model sees an untagged user message after
 * being told to expect a tag — it then has no way to tell which mode was
 * active when an earlier message was sent, and a mid-conversation mode
 * switch becomes invisible to it. The wrapper is the runtime half of the
 * contract; the prompt is the instruction half. Both must be present or
 * neither makes sense.
 *
 * <p>Wire format (matches Cline byte-for-byte):
 * <pre>{@code
 * <user_input mode="plan">
 * <mode_notice>
 * Mode switched from Act to Plan. Plan-mode constraints now apply: no
 * edits or state-changing commands until the user toggles back to Act.
 * </mode_notice>
 *
 * Actual user message text here.
 * </user_input>
 * }</pre>
 *
 * <p>The {@code <mode_notice>} block is only prepended when this message
 * is the first one sent after a mode switch (i.e. the runtime tracks the
 * previous mode and inserts a notice when it differs from the new one).
 * On subsequent messages in the same mode, no notice is inserted.
 *
 * <p>Mode names match Cline's vocabulary: {@code "plan"}, {@code "act"},
 * {@code "yolo"}. The {@code "yolo"} value is Cline's term for
 * auto-approve-all mode; our {@link AgentMode#YOLO} maps to it directly.
 */
public final class UserInputModeWrapper {

    private UserInputModeWrapper() {}

    /**
     * Wrap a user message with the {@code <user_input mode="...">} tag.
     * No mode notice is inserted — use
     * {@link #wrap(String, AgentMode, AgentMode)} when you need to
     * signal a mode switch.
     *
     * @param userInput the raw user message text. May be empty but not
     *                  null. Empty messages are still wrapped so the
     *                  model sees the mode tag (important for mode-only
     *                  turns where the user just toggles mode and sends
     *                  an empty message to resume).
     * @param mode      the mode active when the user sent this message.
     *                  Maps to the {@code mode="..."} attribute.
     * @return the wrapped message, ready to be added to conversation
     *         history as a user turn.
     */
    public static String wrap(String userInput, AgentMode mode) {
        return wrap(userInput, mode, null);
    }

    /**
     * Wrap a user message with the {@code <user_input mode="...">} tag
     * and optionally prepend a {@code <mode_notice>} block when the mode
     * has just switched.
     *
     * @param userInput   the raw user message text. May be empty but not
     *                    null.
     * @param newMode     the mode active when the user sent this message.
     * @param previousMode the mode that was active before this message.
     *                    When {@code null} or equal to {@code newMode},
     *                    no notice is inserted. When different, a
     *                    {@code <mode_notice>} block is prepended inside
     *                    the {@code <user_input>} wrapper.
     * @return the wrapped message, ready to be added to conversation
     *         history as a user turn.
     */
    public static String wrap(String userInput, AgentMode newMode, AgentMode previousMode) {
        if (userInput == null) userInput = "";
        String modeAttr = modeName(newMode);
        StringBuilder sb = new StringBuilder();
        sb.append("<user_input mode=\"").append(modeAttr).append("\">\n");
        if (previousMode != null && previousMode != newMode) {
            sb.append(formatModeNotice(previousMode, newMode)).append('\n');
        }
        sb.append(userInput);
        if (!userInput.isEmpty() && !userInput.endsWith("\n")) {
            sb.append('\n');
        }
        sb.append("</user_input>");
        return sb.toString();
    }

    /**
     * Format a {@code <mode_notice>} block. Called when the mode has just
     * switched — the runtime passes the previous and new modes and this
     * returns the block that gets prepended to the next user message.
     *
     * <p>The notice text is intentionally brief: it tells the model (a)
     * that the mode switched, (b) what the new mode is, and (c) what the
     * new mode's constraints are in one sentence. The full contract is
     * already in the system prompt; the notice is just a marker so the
     * model can locate the exact turn where the switch happened.
     *
     * @param from the previous mode.
     * @param to   the new mode.
     * @return the {@code <mode_notice>} block, including opening and
     *         closing tags.
     */
    public static String formatModeNotice(AgentMode from, AgentMode to) {
        if (from == to) return "";
        String fromName = modeName(from);
        String toName = modeName(to);
        String constraint = modeConstraintSummary(to);
        return "<mode_notice>\n" +
               "Mode switched from " + capitalize(fromName) + " to " + capitalize(toName) + ". " +
               constraint + "\n" +
               "</mode_notice>";
    }

    /**
     * Map an {@link AgentMode} to the wire name used in the
     * {@code mode="..."} attribute. Matches Cline's vocabulary:
     * {@code "plan"}, {@code "act"}, {@code "yolo"}.
     */
    static String modeName(AgentMode mode) {
        if (mode == null) return "act";
        switch (mode) {
            case PLAN: return "plan";
            case YOLO: return "yolo";
            case ACT:
            default:   return "act";
        }
    }

    /**
     * One-sentence summary of a mode's constraints, used inside
     * {@code <mode_notice>}. Keeps the notice scannable — the full
     * contract lives in the system prompt via
     * {@link PlanActPrompts#PLAN_MODE_INSTRUCTIONS_MANUAL_SWITCH}.
     */
    static String modeConstraintSummary(AgentMode mode) {
        if (mode == null) return "Act-mode constraints apply.";
        switch (mode) {
            case PLAN:
                return "Plan-mode constraints now apply: no edits or state-changing commands until the user toggles to Act.";
            case YOLO:
                return "Yolo-mode constraints now apply: all tool calls are auto-approved; execute freely.";
            case ACT:
            default:
                return "Act-mode constraints now apply: write operations require user approval.";
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
