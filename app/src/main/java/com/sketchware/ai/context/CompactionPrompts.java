package com.sketchware.ai.context;

/**
 * Prompt templates for context compaction. Ported from oh-my-pi's
 * {@code packages/agent/src/compaction/prompts/} directory.
 *
 * <p>Three core templates:
 * <ul>
 *   <li>{@link #SUMMARIZATION_SYSTEM} — strict instructions to the summarizer
 *       model: never continue the conversation, only output the structured
 *       summary. Mirrors {@code summarization-system.md}.</li>
 *   <li>{@link #COMPACTION_SUMMARY} — first-pass summary template with
 *       sections Goal / Constraints / Progress / Key Decisions / Next Steps /
 *       Critical Context / Additional Notes. Mirrors
 *       {@code compaction-summary.md}.</li>
 *   <li>{@link #COMPACTION_UPDATE_SUMMARY} — iterative update template used
 *       when a previous summary already exists. Preserves prior information
 *       and only adds new progress. Mirrors
 *       {@code compaction-update-summary.md}.</li>
 * </ul>
 *
 * <p>The {@code compaction-summary-context.md} wrapper is applied by
 * {@link OhMyPiCompactor} when injecting the summary into the post-compaction
 * conversation.
 */
public final class CompactionPrompts {

    private CompactionPrompts() {}

    /**
     * System prompt for the summarizer model. Strict: never continue the
     * conversation, only output the structured summary.
     */
    public static final String SUMMARIZATION_SYSTEM =
        "Summarize user\u2013AI coding-assistant conversations in the exact specified structured format.\n\n" +
        "NEVER continue the conversation or answer its questions. Output ONLY the structured summary.\n" +
        "You MUST summarize the conversation above into a structured handoff summary for another LLM to resume the task.\n\n" +
        "IMPORTANT: If the conversation ends with an unanswered question or a request awaiting user response " +
        "(e.g., \"Please run command and paste output\"), you MUST preserve that exact question/request.\n\n" +
        "You MUST preserve exact file paths, function names, error messages, and relevant tool outputs or " +
        "command results. You MUST include repository state changes (branch, uncommitted changes) if mentioned.";

    /**
     * First-pass summary template. Used when no previous summary exists.
     * Expects a {@code <conversation>} block to be appended by the caller.
     */
    public static final String COMPACTION_SUMMARY =
        "You MUST use this format (sections can be omitted if not applicable):\n\n" +
        "## Goal\n" +
        "[User goals; list multiple if session covers different tasks.]\n\n" +
        "## Constraints & Preferences\n" +
        "- [Constraints or requirements mentioned]\n\n" +
        "## Progress\n\n" +
        "### Done\n" +
        "- [x] [Completed tasks/changes]\n\n" +
        "### In Progress\n" +
        "- [ ] [Current work]\n\n" +
        "### Blocked\n" +
        "- [Issues preventing progress]\n\n" +
        "## Key Decisions\n" +
        "- **[Decision]**: [Brief rationale]\n\n" +
        "## Next Steps\n" +
        "1. [Ordered list of next actions]\n\n" +
        "## Critical Context\n" +
        "- [Important data, pending questions, references]\n\n" +
        "## Additional Notes\n" +
        "[Anything else important not covered above]\n\n" +
        "You MUST output only the structured summary; you NEVER include extra text.\n\n" +
        "Sections MUST be kept concise. You MUST preserve exact file paths, function names, error messages, " +
        "and relevant tool outputs or command results. You MUST include repository state changes (branch, " +
        "uncommitted changes) if mentioned.\n\n" +
        "<conversation>\n";

    /**
     * Iterative update template. Used when a previous summary already exists.
     * The previous summary is injected inside {@code <previous-summary>} tags.
     */
    public static final String COMPACTION_UPDATE_SUMMARY =
        "Update existing handoff summary in <previous-summary> tags from new messages above for another LLM to resume.\n\n" +
        "MUST:\n" +
        "- preserve all previous-summary information; add new progress, decisions, context.\n" +
        "- Progress: move completed \"In Progress\" items to \"Done\".\n" +
        "- update \"Next Steps\" for completed work.\n" +
        "- preserve exact file paths, function names, error messages.\n" +
        "- MAY remove irrelevant content.\n" +
        "- If new messages end with an unanswered user question/request: add it to Critical Context; " +
        "replace any previous pending question if answered.\n" +
        "- output only the structured summary; NEVER extra text.\n" +
        "- keep sections concise.\n" +
        "- preserve relevant tool outputs/command results.\n" +
        "- include mentioned repository state changes (branch, uncommitted changes).\n\n" +
        "Format (omit inapplicable sections):\n\n" +
        "## Goal\n" +
        "[Preserve existing goals; add new ones if task expanded]\n\n" +
        "## Constraints & Preferences\n" +
        "- [Preserve existing; add new ones discovered]\n\n" +
        "## Progress\n\n" +
        "### Done\n" +
        "- [x] [Include previously done and newly completed items]\n\n" +
        "### In Progress\n" +
        "- [ ] [Current work\u2014update based on progress]\n\n" +
        "### Blocked\n" +
        "- [Current blockers\u2014remove if resolved]\n\n" +
        "## Key Decisions\n" +
        "- **[Decision]**: [Brief rationale] (preserve all previous, add new)\n\n" +
        "## Next Steps\n" +
        "1. [Update based on current state]\n\n" +
        "## Critical Context\n" +
        "- [Preserve important context; add new if needed]\n\n" +
        "## Additional Notes\n" +
        "[Other important info not fitting above]\n\n" +
        "<previous-summary>\n";

    /**
     * Wrapper for injecting the summary into the post-compaction conversation.
     * Mirrors {@code compaction-summary-context.md}.
     */
    public static final String COMPACTION_SUMMARY_CONTEXT =
        "Prior model work/tool state available.\n" +
        "MUST build on prior work; NEVER duplicate prior work.\n\n" +
        "<summary>\n%s\n</summary>";

    /**
     * Short summary prompt for UI display. Mirrors
     * {@code compaction-short-summary.md}.
     */
    public static final String COMPACTION_SHORT_SUMMARY =
        "Write a one-paragraph (max 2 sentences) summary of the conversation above. " +
        "Focus on what was accomplished and what is in progress. " +
        "Output ONLY the summary text, no extra formatting.";
}
