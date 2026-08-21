package com.sketchware.ai.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.context.AgenticCompactor;
import com.sketchware.ai.context.BasicCompactor;
import com.sketchware.ai.context.Compactor;
import com.sketchware.ai.context.ContextTruncator;
import com.sketchware.ai.context.OhMyPiCompactor;
import com.sketchware.ai.context.SnapCompactCompactor;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.UsageTracker;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;
import com.sketchware.ai.llm.storage.ProviderConfigStore;
import com.sketchware.ai.prompt.PlanActPrompts;
import com.sketchware.ai.prompt.SystemPromptBuilder;
import com.sketchware.ai.prompt.UserInputModeWrapper;
import com.sketchware.ai.tools.AutoApprover;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolExecutor;
import com.sketchware.ai.tools.ToolPermissionGate;
import com.sketchware.ai.tools.ToolRegistry;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.JsonSchemaValidator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main agent loop. Mirrors Cline's
 * {@code sdk/packages/agents/src/agent-runtime.ts} - a single while loop
 * with {@code maxIterations} cap, {@link AbortController} for cancellation,
 * overflow recovery via {@link BasicCompactor}, and tool execution.
 *
 * <p>Threading model:
 * <ul>
 *   <li>{@link #execute(String, AgentListener)} runs on a background thread from
 *       a single-threaded {@link ExecutorService}.</li>
 *   <li>{@link AgentListener} callbacks are invoked on the same background
 *       thread; UI listeners must post to the main thread themselves.</li>
 * </ul>
 */
public final class AgentRuntime {

    private static final int DEFAULT_MAX_ITERATIONS = 50;
    private static final double COMPACTION_TRIGGER_RATIO = 0.9;
    private static final int COMPACTION_PRESERVE_RECENT = 20;
    /** Max compaction-recovery cycles per run before giving up with onError. */
    private static final int MAX_COMPACTION_RETRIES = 3;

    private final LlmProvider provider;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    /**
     * Enhanced permission gate with per-tool/action/path rules. Replaces the
     * older {@link ToolPermissionGate} which only supported per-tool overrides.
     * Kept as a field alongside {@link #legacyGate} for backwards compatibility
     * — the legacy gate is still queried when the constructor that takes a
     * {@code ToolPermissionGate} is used, so existing callers (e.g.
     * {@code ChatFragment}) don't need to be rewritten.
     */
    private AutoApprover autoApprover;
    private final ToolPermissionGate legacyGate;
    private final ProviderConfigStore.Profile profile;
    private final String systemPrompt;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<Future<?>> currentRun = new AtomicReference<>();
    private final AbortController abortController = new AbortController();

    /**
     * Current run's listener. Set at the start of {@link #run(String, AgentListener)}
     * and cleared in {@code finally}. Null-safe accessors use {@link #warnListener(String)}.
     */
    private volatile AgentListener currentListener;

    /**
     * Loop detector — tracks repeated identical tool calls and injects warnings.
     * Mirrors Cline's {@code core/task/loop-detection.ts}.
     */
    private final LoopDetector loopDetector = new LoopDetector();

    /**
     * Usage tracker — accumulates token usage and cost across the session.
     * Exposed for the /cost slash command and UI cost display.
     */
    private final UsageTracker usageTracker = new UsageTracker();

    /**
     * Tool execution context. Stored as a plain instance field (NOT a
     * ThreadLocal) because {@code setContext()} is called from the UI thread
     * but {@code executeTool()} runs on the agent's background executor —
     * a ThreadLocal set on the UI thread is invisible to the executor thread,
     * which previously caused every tool call to fail with
     * "No tool context available."
     */
    private volatile SketchwareToolContext toolContext;

    private final LinkedList<AgentMessage> conversationHistory = new LinkedList<>();
    private AgentMode mode = AgentMode.ACT;
    /**
     * Mode that was active before the most recent {@link #setMode} call.
     * Used by {@link #run} to prepend a {@code <mode_notice>} block to
     * the next user message when the user toggled Plan/Act mid-
     * conversation. Reset to the current mode after each user message is
     * wrapped, so subsequent messages in the same mode do not get a
     * notice. Mirrors Cline's {@code previousMode} tracking in the
     * runtime's turn-preparation step.
     */
    private volatile AgentMode pendingModeSwitchFrom = null;

    /**
     * Research summary captured from the most recent RESEARCH-mode turn.
     * When the user toggles RESEARCH → PLAN, this summary is prepended
     * to the next user message as a {@code <prior_research>} block so
     * the planner starts from the researcher's findings. Mirrors Cline
     * 3.x's research-summary handoff.
     */
    private volatile String pendingResearchSummary = null;

    private int maxIterations = DEFAULT_MAX_ITERATIONS;

    public AgentRuntime(LlmProvider provider,
                        ToolRegistry toolRegistry,
                        ToolPermissionGate permissionGate,
                        ProviderConfigStore.Profile profile,
                        String systemPrompt) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = new ToolExecutor(toolRegistry);
        this.legacyGate = permissionGate;
        // Wrap the legacy gate in an AutoApprover with default rules. The
        // legacy gate is still consulted for per-tool overrides the user may
        // have set via the older UI; the AutoApprover adds per-action/path
        // rules on top.
        this.autoApprover = AutoApprover.withDefaults();
        this.profile = profile;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Construct with an explicit {@link AutoApprover} for full per-tool /
     * per-action / per-path rule control. Caller is responsible for setting
     * the mode on the AutoApprover; {@link #setMode(AgentMode)} propagates
     * to both the AutoApprover and any legacy gate.
     */
    public AgentRuntime(LlmProvider provider,
                        ToolRegistry toolRegistry,
                        AutoApprover autoApprover,
                        ProviderConfigStore.Profile profile,
                        String systemPrompt) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = new ToolExecutor(toolRegistry);
        this.autoApprover = autoApprover != null ? autoApprover : AutoApprover.withDefaults();
        this.legacyGate = null;
        this.profile = profile;
        this.systemPrompt = systemPrompt;
    }

    public void setMode(AgentMode mode) {
        if (mode == null) mode = AgentMode.ACT;
        if (mode != this.mode) {
            // Record the switch so the next user message gets a
            // <mode_notice> block prepended via UserInputModeWrapper.
            // Only set when the mode actually changes — calling setMode
            // with the same value (which the UI does on every rebuild)
            // must NOT inject a notice, or every message would carry one.
            this.pendingModeSwitchFrom = this.mode;
        }
        this.mode = mode;
        autoApprover.setMode(mode);
        if (legacyGate != null) legacyGate.setMode(mode);
    }
    public AgentMode getMode() { return mode; }

    /**
     * Replace the AutoApprover at runtime. Used by ChatFragment when the
     * user toggles the YOLO / auto-approve switch — the new policy takes
     * effect on the next tool call without needing to rebuild the agent.
     */
    public void setAutoApprover(com.sketchware.ai.tools.AutoApprover approver) {
        if (approver != null) {
            this.autoApprover = approver;
            this.autoApprover.setMode(this.mode);
        }
    }

    public void setMaxIterations(int max) { this.maxIterations = max; }

    /** Get the usage tracker (for /cost command and UI cost display). */
    public UsageTracker getUsageTracker() { return usageTracker; }

    /** Get the loop detector (for testing / debugging). */
    public LoopDetector getLoopDetector() { return loopDetector; }

    /** Get the AutoApprover (for adding/removing rules at runtime). */
    public AutoApprover getAutoApprover() { return autoApprover; }

    /**
     * Get a snapshot of the current conversation history (for task save).
     * Returns a copy — mutations to the returned list do not affect the
     * agent's internal state.
     */
    public LinkedList<AgentMessage> getConversationHistory() {
        synchronized (conversationHistory) {
            return new LinkedList<>(conversationHistory);
        }
    }

    /**
     * Replace the conversation history (e.g. when loading a saved task).
     * The next {@link #execute(String, AgentListener)} call will append to
     * this restored history. Caller is responsible for ensuring the agent
     * is not currently running (call {@link #abort()} first if needed).
     */
    public void setConversationHistory(LinkedList<AgentMessage> history) {
        synchronized (conversationHistory) {
            conversationHistory.clear();
            if (history != null) conversationHistory.addAll(history);
        }
        // Reset the pending mode-switch flag so a freshly-resumed chat
        // does not get a spurious <mode_notice> on its first message
        // after load. The notice only makes sense for live mid-
        // conversation switches, not for the boundary between a saved
        // session and the next turn.
        this.pendingModeSwitchFrom = null;
    }

    /** Reset the loop detector and TODO list (call when starting a new task). */
    public void resetSession() {
        loopDetector.reset();
        usageTracker.reset();
        synchronized (conversationHistory) {
            conversationHistory.clear();
        }
        com.sketchware.ai.tools.meta.TodoListTool.resetSession();
    }

    /**
     * Set the tool execution context. Must be called before {@link #execute}
     * (or before the first tool call is dispatched). The context is stored as
     * a plain instance field so it is visible across threads (the agent loop
     * runs on a background executor, not the UI thread that calls this).
     */
    public void setContext(SketchwareToolContext ctx) {
        this.toolContext = ctx;
    }

    /**
     * Start the agent loop with a new user message.
     * Returns immediately; the loop runs on a background thread.
     */
    public void execute(String userInput, AgentListener listener) {
        execute(userInput, null, listener);
    }

    /**
     * Overload that accepts an optional list of base64-encoded image
     * attachments. When {@code images} is non-null and non-empty, the user
     * message is created via {@link AgentMessage#userWithImages(String, List)}
     * so the LLM can see the images. When {@code images} is null or empty,
     * behaviour is identical to {@link #execute(String, AgentListener)}.
     */
    public void execute(String userInput, List<String> images, AgentListener listener) {
        if (currentRun.get() != null && !currentRun.get().isDone()) {
            listener.onError(new IllegalStateException("A run is already in progress. Call abort() first."));
            return;
        }
        // CRITICAL: reset the abort flag so a previously-aborted runtime can
        // start a fresh run. Without this, the loop's first-iteration check
        // exits immediately and the listener is left with no callback.
        abortController.reset();
        Future<?> f = executor.submit(() -> run(userInput, images, listener));
        currentRun.set(f);
    }

    /** Abort the current run. */
    public void abort() {
        abortController.abort();
        provider.abort();
        Future<?> f = currentRun.get();
        if (f != null) f.cancel(true);
    }

    private void run(String userInput, List<String> images, AgentListener listener) {
        this.currentListener = listener;
        try {
            // Wrap the user message in a <user_input mode="..."> tag and
            // (when the user just toggled Plan/Act) prepend a
            // <mode_notice> block. The system prompt's MODE_TAG_INSTRUCTIONS
            // tells the model to expect this wrapper — without it, a
            // mid-conversation mode switch is an invisible system-prompt
            // swap the model cannot diff, and it has no way to tell which
            // mode was active when an earlier message was sent. Direct
            // port of Cline's prepareTurnInput / formatUserInputBlock.
            //
            // We capture the pending switch flag BEFORE adding the message
            // (so it gets consumed by this turn) and pass the previous
            // mode to the wrapper. After wrapping, the flag is cleared —
            // subsequent messages in the same mode do not get a notice.
            AgentMode previousMode = this.pendingModeSwitchFrom;
            this.pendingModeSwitchFrom = null;
            String wrappedInput = UserInputModeWrapper.wrap(userInput, mode, previousMode);

            // RESEARCH → PLAN handoff: if the user just switched from
            // RESEARCH to PLAN and we have a stashed research summary,
            // prepend it to the user's message as a <prior_research>
            // block. The system prompt's PRIOR_RESEARCH_HEADER tells the
            // planner to build on these findings instead of redoing the
            // research. Mirrors Cline 3.x's research-summary handoff.
            if (mode == AgentMode.PLAN
                    && previousMode == AgentMode.RESEARCH
                    && pendingResearchSummary != null) {
                String priorResearchBlock = PlanActPrompts.PRIOR_RESEARCH_HEADER
                        + "<prior_research>\n"
                        + pendingResearchSummary
                        + "\n</prior_research>\n\n";
                wrappedInput = priorResearchBlock + wrappedInput;
                // Clear the stash so a subsequent PLAN→PLAN message
                // doesn't get the same prior_research block twice.
                pendingResearchSummary = null;
            }

            if (images != null && !images.isEmpty()) {
                conversationHistory.add(AgentMessage.userWithImages(wrappedInput, images));
            } else {
                conversationHistory.add(AgentMessage.user(wrappedInput));
            }

            ModelInfo model = provider.getModel(profile.modelId);
            int iteration = 0;
            // Budget for compaction-recovery cycles. Each time the stream
            // fails with an overflow error we compact and retry; if the retry
            // ALSO fails we used to call onError + return (which stopped the
            // agent dead). Now we let the outer while loop retry from the
            // compacted history, up to this many times, before giving up.
            int compactionRetries = 0;
            while (iteration++ < maxIterations && !abortController.isAborted()) {
                // Build request.
                ReasoningRequest reasoning = buildReasoningRequest();
                LlmRequest req = new LlmRequest(
                        provider.getProviderId(),
                        profile.baseUrl,
                        profile.apiKey,
                        model,
                        systemPrompt,
                        conversationHistory,
                        toolRegistry.toAgentJsonSchemas(),
                        reasoning,
                        profile.maxOutputTokens > 0 ? profile.maxOutputTokens : model.maxOutputTokens,
                        profile.enableStreaming,
                        toExtraHeaders(profile.customHeaders),
                        profile.forceFlatToolFormat);

                // Stream.
                StringBuilder textBuf = new StringBuilder();
                StringBuilder reasonBuf = new StringBuilder();
                List<AgentMessage.ToolCall> pendingToolCalls = new ArrayList<>();
                int[] usage = new int[5]; // in, out, reasoning, cacheRead, cacheWrite

                try {
                    for (ApiStreamChunk chunk : provider.stream(req)) {
                        if (abortController.isAborted()) break;
                        if (chunk.isText()) {
                            textBuf.append(chunk.asText().text);
                            listener.onTextDelta(chunk.asText().text);
                        } else if (chunk.isReasoning()) {
                            reasonBuf.append(chunk.asReasoning().text);
                            listener.onReasoningDelta(chunk.asReasoning().text);
                        } else if (chunk.isToolCalls()) {
                            pendingToolCalls.addAll(chunk.asToolCalls().calls);
                        } else if (chunk.isUsage()) {
                            ApiStreamChunk.Usage u = chunk.asUsage();
                            usage[0] = u.inputTokens;
                            usage[1] = u.outputTokens;
                            usage[2] = u.reasoningTokens;
                            usage[3] = u.cacheReadTokens;
                            usage[4] = u.cacheWriteTokens;
                            // Record in the usage tracker for /cost command and UI display.
                            usageTracker.record(provider.getProviderId(), profile.modelId,
                                    u.inputTokens, u.outputTokens, u.reasoningTokens,
                                    u.cacheReadTokens, u.cacheWriteTokens, u.cost);
                            listener.onUsage(u.inputTokens, u.outputTokens, u.reasoningTokens, u.cost);
                        } else if (chunk.isDone()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Overflow recovery: compact and retry. Previously this
                    // was a single-shot retry — if it failed, onError+return
                    // stopped the agent for good, leaving the user with a dead
                    // chat after every compaction. Now we compact, retry, and
                    // if the retry ALSO fails we let the outer while loop take
                    // another crack at the (now smaller) history instead of
                    // terminating. A retry budget prevents infinite loops.
                    listener.onWarning("Stream error: " + e.getMessage() + ". Trying compaction.");
                    compactConversation(model.maxInputTokens);
                    compactionRetries++;
                    // CRITICAL: clear all buffers before retrying. The first
                    // attempt may have streamed partial text/reasoning/tool_calls
                    // before failing; if we don't reset, the retry would APPEND
                    // to the existing buffers, producing duplicated text and
                    // phantom tool calls in both the conversation history and
                    // the UI (which already received onTextDelta callbacks for
                    // the partial text).
                    textBuf.setLength(0);
                    reasonBuf.setLength(0);
                    pendingToolCalls.clear();
                    // Retry once.
                    try {
                        LlmRequest retry = rebuildRequest(model, reasoning);
                        for (ApiStreamChunk chunk : provider.stream(retry)) {
                            if (abortController.isAborted()) break;
                            if (chunk.isText()) {
                                textBuf.append(chunk.asText().text);
                                listener.onTextDelta(chunk.asText().text);
                            } else if (chunk.isReasoning()) {
                                reasonBuf.append(chunk.asReasoning().text);
                                listener.onReasoningDelta(chunk.asReasoning().text);
                            } else if (chunk.isToolCalls()) {
                                pendingToolCalls.addAll(chunk.asToolCalls().calls);
                            } else if (chunk.isUsage()) {
                                ApiStreamChunk.Usage u = chunk.asUsage();
                                usageTracker.record(provider.getProviderId(), profile.modelId,
                                        u.inputTokens, u.outputTokens, u.reasoningTokens,
                                        u.cacheReadTokens, u.cacheWriteTokens, u.cost);
                                listener.onUsage(u.inputTokens, u.outputTokens, u.reasoningTokens, u.cost);
                            } else if (chunk.isDone()) break;
                        }
                    } catch (Exception e2) {
                        // Retry also failed. Instead of killing the agent,
                        // let the outer while loop rebuild the request from
                        // the compacted history and try again — but only if we
                        // still have compaction budget left. This is what
                        // fixes "agent stops after compression": the system
                        // now keeps retrying with the new (compacted) context
                        // instead of giving up after one failed retry.
                        if (compactionRetries < MAX_COMPACTION_RETRIES
                                && !abortController.isAborted()
                                && estimateTokens() > 0) {
                            listener.onWarning("Retry failed after compaction ("
                                    + e2.getMessage()
                                    + "). Restarting agent loop with compacted context (attempt "
                                    + (compactionRetries + 1) + "/" + MAX_COMPACTION_RETRIES + ").");
                            continue;  // outer while — rebuilds request from compacted history
                        }
                        listener.onError(e2);
                        return;
                    }
                }

                // If the user aborted mid-stream, surface the partial text
                // and exit the loop. Previously the loop just exited silently
                // and the listener was left in a "streaming" UI state with no
                // terminal callback.
                if (abortController.isAborted()) {
                    listener.onAborted(textBuf.toString());
                    return;
                }

                // The stream succeeded (or was recovered via compaction).
                // Reset the compaction-retry budget so a future overflow in a
                // later iteration gets a fresh set of retries.
                compactionRetries = 0;

                // Tool-name inference: some proxies (notably AgentRouter when
                // proxying Claude) occasionally emit "name":"" in the tool_call
                // delta. The OpenAiProvider parser converts this to "unknown".
                // When we see an empty or "unknown" name, try to infer the
                // intended tool by matching the argument keys against every
                // registered tool's JSON schema. If exactly one tool matches,
                // replace the call with a corrected version. This happens
                // BEFORE the assistant message is added to history so that
                // both the conversation history and the execution loop see
                // the corrected name — otherwise the next LLM round-trip
                // would receive an assistant message with tool_calls:[{name:""}]
                // which confuses the model into retrying the empty-name call.
                if (!pendingToolCalls.isEmpty()) {
                    List<AgentMessage.ToolCall> resolved = new ArrayList<>(pendingToolCalls.size());
                    for (AgentMessage.ToolCall c : pendingToolCalls) {
                        resolved.add(resolveToolName(c, listener));
                    }
                    pendingToolCalls = resolved;
                }

                // Add assistant message to history.
                conversationHistory.add(AgentMessage.assistant(
                        textBuf.toString(),
                        reasonBuf.toString(),
                        pendingToolCalls.isEmpty() ? null : pendingToolCalls));

                // RESEARCH mode: extract any <research_summary> block from
                // the assistant's text and stash it for the next PLAN
                // session. When the user toggles RESEARCH → PLAN, the
                // next user message will be prepended with a
                // <prior_research> block so the planner starts from the
                // researcher's findings instead of from scratch.
                // Mirrors Cline 3.x's research-summary handoff.
                if (mode == AgentMode.RESEARCH) {
                    String summary = extractResearchSummary(textBuf.toString());
                    if (summary != null) {
                        pendingResearchSummary = summary;
                        listener.onWarning("Research summary captured. Toggle to Plan mode to build on it.");
                    }
                }

                // No tool calls = end of turn.
                if (pendingToolCalls.isEmpty()) {
                    listener.onComplete(textBuf.toString());
                    return;
                }

                // Deduplicate tool calls by signature BEFORE execution.
                // Many providers/proxies (OpenRouter, vLLM, Z.AI, ...) emit
                // the same tool_calls array in successive SSE deltas, or the
                // LLM itself re-emits an identical call. Without dedup every
                // duplicate executes — e.g. view_add_widget creates a second
                // identical-looking Button. We collapse identical (name+args)
                // calls into a single execution and feed a synthetic "skipped"
                // result back to the LLM so it knows the duplicate was ignored.
                List<AgentMessage.ToolCall> dedupedCalls = new ArrayList<>();
                List<AgentMessage.ToolCall> skippedDupes = new ArrayList<>();
                Set<String> seenSignatures = new LinkedHashSet<>();
                for (AgentMessage.ToolCall call : pendingToolCalls) {
                    String sig = LoopDetector.signature(call.name, call.argumentsJson);
                    if (seenSignatures.add(sig)) {
                        dedupedCalls.add(call);
                    } else {
                        skippedDupes.add(call);
                    }
                }
                if (!skippedDupes.isEmpty()) {
                    listener.onWarning("Skipped " + skippedDupes.size()
                            + " duplicate tool call(s) in this batch to prevent repeated execution.");
                }
                // Replace the pending list with the deduped one so the
                // assistant message recorded in history also reflects only
                // the calls that actually ran.
                pendingToolCalls = dedupedCalls;

                listener.onToolCalls(pendingToolCalls);

                // TodoList active-todo guard (Cline 3.x requireTodosInRange).
                // Before executing any non-meta tool, check that at least
                // one TODO item is in_progress. If not, inject a warning
                // and a synthetic tool_result telling the LLM to mark one
                // in_progress. This forces the agent to track progress
                // through multi-step tasks instead of running tools
                // blindly, which was a major source of "agent did 12
                // things, none of them what I asked for" complaints.
                //
                // The guard is skipped:
                //   - when the TODO list is empty (the LLM may not need
                //     a list for trivial single-step tasks),
                //   - for meta tools (todo_list itself, ask_question,
                //     submit_and_exit) which manage the list,
                //   - in RESEARCH mode (research is exploratory, not
                //     task-tracked).
                if (mode != AgentMode.RESEARCH
                        && !com.sketchware.ai.tools.meta.TodoListTool.isEmpty()
                        && !com.sketchware.ai.tools.meta.TodoListTool.hasActiveTodo()
                        && pendingToolCalls.stream().anyMatch(c -> isNonMetaTool(c.name))) {
                    String nudge = "No TODO item is marked in_progress, but you have a TODO list. "
                            + "Before calling any tool, mark exactly one TODO item as in_progress "
                            + "with action=update (or action=write) and provide an active_form. "
                            + "This tracks what you are doing right now so the user can follow along.";
                    listener.onWarning(nudge);
                    // Inject a synthetic tool_result for every pending call
                    // so the LLM sees explicit feedback that its calls were
                    // skipped — instead of silently executing them.
                    List<AgentMessage.ToolResultContent> skippedResults = new ArrayList<>();
                    for (AgentMessage.ToolCall c : pendingToolCalls) {
                        if (!isNonMetaTool(c.name)) continue;
                        skippedResults.add(new AgentMessage.ToolResultContent(
                                c.id, c.name, nudge, true));
                    }
                    if (!skippedResults.isEmpty()) {
                        conversationHistory.add(AgentMessage.toolResult(skippedResults));
                        // Skip actual execution this iteration — the LLM
                        // will see the warning and (hopefully) call
                        // todo_list first, then retry.
                        continue;
                    }
                }

                // Execute tools sequentially.
                List<AgentMessage.ToolResultContent> results = new ArrayList<>();
                boolean submitAndExit = false;
                for (int callIdx = 0; callIdx < pendingToolCalls.size(); callIdx++) {
                    AgentMessage.ToolCall call = pendingToolCalls.get(callIdx);
                    if (abortController.isAborted()) break;
                    listener.onToolStart(call.id, call.name, call.argumentsJson);
                    // Loop detection: observe the call before executing.
                    LoopDetector.LoopResult loop = loopDetector.observe(call.name, call.argumentsJson);
                    if (loop.softWarning || loop.hardEscalation) {
                        String warning = loop.warningText(call.name);
                        if (warning != null) listener.onWarning(warning);
                        // If hard escalation + shouldAbort, stop the run.
                        if (loop.shouldAbort) {
                            AgentMessage.ToolResultContent tr = new AgentMessage.ToolResultContent(
                                    call.id, call.name, warning, true);
                            results.add(tr);
                            listener.onToolResult(call.id, tr);
                            listener.onError(new IllegalStateException(
                                    "Loop detected: tool '" + call.name + "' called " + loop.repeatCount
                                            + " times with identical arguments. Aborting to prevent infinite loop."));
                            return;
                        }
                    }
                    // Record tool call for telemetry.
                    usageTracker.recordToolCall(call.name);
                    ToolResult result = executeTool(call, listener);
                    AgentMessage.ToolResultContent tr = new AgentMessage.ToolResultContent(
                            call.id, call.name,
                            result == null ? "" : result.toLLMString(),
                            result != null && result.isError());
                    // If a loop warning was issued, append it to the tool result so the LLM sees it.
                    if (loop.softWarning || loop.hardEscalation) {
                        String warning = loop.warningText(call.name);
                        if (warning != null) {
                            tr = new AgentMessage.ToolResultContent(
                                    call.id, call.name,
                                    tr.output + "\n\n" + warning,
                                    tr.isError);
                        }
                    }
                    results.add(tr);
                    listener.onToolResult(call.id, tr);
                    // submit_and_exit signals task completion; the loop must
                    // exit after the LLM sees the tool_result, NOT continue
                    // with another LLM round-trip. We still execute any
                    // remaining tool calls in this batch first (the LLM
                    // issued them together, so they're presumably part of
                    // the same logical step).
                    if ("submit_and_exit".equals(call.name)) {
                        submitAndExit = true;
                    }
                }
                // Append synthetic "skipped" results for any duplicate tool
                // calls we collapsed, so the LLM sees explicit feedback that
                // its duplicate was ignored (instead of silently dropping it).
                for (AgentMessage.ToolCall dup : skippedDupes) {
                    results.add(new AgentMessage.ToolResultContent(
                            dup.id, dup.name,
                            "Skipped: this tool call is an exact duplicate of an earlier call in the same batch and was not executed again. Do not repeat identical tool calls.",
                            false));
                }
                // Only add a tool_result message if at least one tool produced
                // a result. If every call was skipped due to abort, an empty
                // toolResult message would confuse the LLM (most providers
                // reject empty tool_result content).
                if (!results.isEmpty()) {
                    conversationHistory.add(AgentMessage.toolResult(results));
                }

                // submit_and_exit: emit the summary as the final assistant
                // text and stop the loop. The tool_result was already added
                // so the conversation history is consistent.
                if (submitAndExit) {
                    String summary = extractSummary(results);
                    listener.onComplete(summary);
                    return;
                }

                // If abort happened during tool execution, surface partial state.
                if (abortController.isAborted()) {
                    listener.onAborted(textBuf.toString());
                    return;
                }

                // Check for context overflow using smart truncation strategy.
                int estimatedTokens = estimateTokens();
                ContextTruncator.TruncationRange truncation = ContextTruncator.decide(
                        conversationHistory.size(), estimatedTokens, model.maxInputTokens);
                if (truncation.needsTruncation) {
                    String notice = ContextTruncator.truncationNotice(truncation);
                    if (notice != null) listener.onWarning(notice);
                    compactConversation(model.maxInputTokens);
                }
            }

            if (abortController.isAborted()) {
                // Loop exited due to abort at iteration boundary.
                listener.onAborted("");
            } else if (iteration > maxIterations) {
                listener.onMaxIterationsReached(maxIterations);
            }
        } catch (Throwable t) {
            listener.onError(t);
        } finally {
            currentRun.set(null);
            this.currentListener = null;
        }
    }

    private ReasoningRequest buildReasoningRequest() {
        if (!profile.enableReasoning) {
            return new ReasoningRequest(false, ReasoningEffort.NONE, null);
        }
        ReasoningEffort effort = ReasoningEffort.parse(profile.reasoningEffort);
        return ReasoningRequest.fromEffort(effort, profile.maxOutputTokens);
    }

    private LlmRequest rebuildRequest(ModelInfo model, ReasoningRequest reasoning) {
        return new LlmRequest(
                provider.getProviderId(), profile.baseUrl, profile.apiKey, model,
                systemPrompt, conversationHistory, toolRegistry.toAgentJsonSchemas(),
                reasoning, profile.maxOutputTokens > 0 ? profile.maxOutputTokens : model.maxOutputTokens,
                profile.enableStreaming, toExtraHeaders(profile.customHeaders),
                profile.forceFlatToolFormat);
    }

    /**
     * Resolve the effective tool name for a call, applying schema-matching
     * inference when the provider returned an empty or "unknown" name.
     *
     * <p>Symptom this fixes: AgentRouter (and possibly other proxies) sometimes
     * emit {@code "name":""} in the tool_call delta when proxying Claude. The
     * OpenAiProvider parser converts this to "unknown". Without inference, the
     * tool executor returns "Unknown tool: ''", the model retries the same
     * empty-name call, and the loop detector escalates to an abort — leaving
     * the user with a dead chat.
     *
     * <p>If inference succeeds (exactly one registered tool's schema matches
     * the call's arguments), a NEW ToolCall with the corrected name is
     * returned. Otherwise the original call is returned unchanged. The
     * listener is notified with a warning when inference fires, so the user
     * can see what happened in the chat UI.
     *
     * @see ToolRegistry#inferFromArgs(String)
     */
    private AgentMessage.ToolCall resolveToolName(AgentMessage.ToolCall call, AgentListener listener) {
        String name = call.name;
        if (name != null && !name.isEmpty() && !"unknown".equals(name)) {
            return call;  // name is valid — no inference needed
        }
        SketchwareTool inferred = toolRegistry.inferFromArgs(call.argumentsJson);
        if (inferred == null) {
            return call;  // ambiguous or no match — leave as-is, let executeTool error out
        }
        String inferredName = inferred.name();
        listener.onWarning("Inferred tool '" + inferredName
                + "' from arguments (provider returned empty/unknown name).");
        // Return a new ToolCall with the corrected name. The id and
        // argumentsJson are preserved so the provider's tool_call_id
        // correlation and the argument payload remain intact.
        return new AgentMessage.ToolCall(call.id, inferredName, call.argumentsJson);
    }

    private ToolResult executeTool(AgentMessage.ToolCall call, AgentListener listener) {
        // NOTE: tool-name inference happens in resolveToolName() at the top
        // of the execute loop, BEFORE this method is called. By the time we
        // get here, call.name has already been rewritten to the inferred
        // name (if inference was needed). See resolveToolName() for the
        // full rationale.
        SketchwareTool tool = toolRegistry.get(call.name);
        if (tool == null) {
            return ToolResult.error("Unknown tool: '" + call.name
                    + "'. Available tools: " + toolRegistry.toolNamesSample());
        }
        // Parse and validate args first — AutoApprover uses the parsed args
        // to evaluate per-action/path rules.
        JsonObject args;
        try {
            args = call.argumentsJson == null || call.argumentsJson.isEmpty()
                    ? new JsonObject()
                    : JsonParser.parseString(call.argumentsJson).getAsJsonObject();
        } catch (Exception e) {
            return ToolResult.error("Invalid arguments JSON for '" + call.name + "': " + e.getMessage()
                    + " (raw: " + truncateForError(call.argumentsJson) + ")");
        }
        // Permission gate: use AutoApprover for fine-grained per-tool/action/path
        // rules. Falls back to legacy gate's per-tool + per-subcategory overrides
        // if set.
        AutoApprover.Decision decision = autoApprover.decide(tool, args);
        // Legacy gate override: if the user explicitly set a per-tool or
        // per-tool+subcategory override in the old UI, honor it (only when
        // AutoApprover didn't already DENY).
        if (decision != AutoApprover.Decision.DENY && legacyGate != null) {
            // Per-tool+subcategory override takes precedence (umbrella tools).
            String subcategory = null;
            if (args != null && args.has("subcategory") && !args.get("subcategory").isJsonNull()) {
                subcategory = args.get("subcategory").getAsString();
            }
            Boolean override = (subcategory != null)
                    ? legacyGate.getToolSubcategoryAutoApprove(call.name, subcategory)
                    : null;
            if (override == null) {
                override = legacyGate.getToolAutoApprove(call.name);
            }
            if (override != null) {
                decision = override ? AutoApprover.Decision.AUTO_APPROVE : AutoApprover.Decision.REQUIRE_APPROVAL;
            }
        }
        if (decision == AutoApprover.Decision.DENY) {
            return ToolResult.error("Tool '" + call.name + "' is not allowed in current mode ("
                    + autoApprover.getMode() + ").");
        }
        if (decision == AutoApprover.Decision.REQUIRE_APPROVAL) {
            // Ask the listener whether to proceed. The default implementation
            // returns true (auto-approve) for backward compatibility with the
            // previous MVP behaviour. A real ChatFragment listener should
            // override requestApproval() to show a confirmation dialog.
            boolean approved = false;
            try {
                approved = listener.requestApproval(call);
            } catch (Throwable t) {
                return ToolResult.error("Approval request threw: " + t.getMessage());
            }
            if (!approved) {
                return ToolResult.error("User denied permission to execute tool '" + call.name + "'.");
            }
        }
        JsonSchemaValidator.ValidationResult v = JsonSchemaValidator.validate(args, tool.jsonSchema());
        if (!v.ok) {
            return ToolResult.error("Validation failed for '" + call.name + "': " + v.error);
        }
        // Execute - the context is set by the caller via setContext().
        // Previously this used a ThreadLocal which silently returned null on
        // the background executor thread, causing every tool call to fail
        // with "No tool context available."
        SketchwareToolContext ctx = this.toolContext;
        if (ctx == null) {
            return ToolResult.error("No tool context available. Call AgentRuntime.setContext() before execute().");
        }
        ToolResult result = toolExecutor.execute(call.name, args, ctx);
        // Refresh UI
        try {
            ctx.refreshAllEditors();
        } catch (Throwable ignored) {
            // UI refresh failures should not affect the tool result.
        }
        return result;
    }

    /**
     * Extract the summary text from a submit_and_exit tool result. Falls back
     * to concatenating the first non-error result's output if the structure
     * is unexpected.
     */
    private static String extractSummary(List<AgentMessage.ToolResultContent> results) {
        for (AgentMessage.ToolResultContent r : results) {
            if (r.output != null && !r.output.isEmpty() && !r.isError) {
                return r.output;
            }
        }
        return "Task complete.";
    }

    /**
     * Regex used to extract a {@code <research_summary>...</research_summary>}
     * block from the assistant's text. Non-greedy so it stops at the first
     * closing tag. Mirrors Cline 3.x's research-summary extractor.
     */
    private static final java.util.regex.Pattern RESEARCH_SUMMARY_RE =
        java.util.regex.Pattern.compile("<research_summary>\\s*([\\s\\S]*?)\\s*</research_summary>");

    /**
     * Extract the contents of a {@code <research_summary>} block from
     * the assistant's text. Returns null if no block is present.
     */
    private static String extractResearchSummary(String text) {
        if (text == null || text.isEmpty()) return null;
        java.util.regex.Matcher m = RESEARCH_SUMMARY_RE.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String truncateForError(String s) {
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...(" + s.length() + " chars)";
    }

    private int estimateTokens() {
        // Use the per-model TokenEstimator (CJK-aware, family-specific
        // ratios) instead of the legacy chars/4 heuristic. The legacy
        // estimator under-counts CJK by 4x and Claude by 12%, which
        // caused the compaction trigger to fire too late on mixed
        // conversations. The estimator is a no-op when profile.modelId
        // is empty (falls back to the generic family with chars/4).
        String modelId = profile != null ? profile.modelId : null;
        return com.sketchware.ai.llm.TokenEstimator.estimateTokens(conversationHistory, modelId);
    }

    /**
     * Return true if the tool name is NOT one of the meta tools that
     * manage the TODO list or terminate the conversation. Used by the
     * TodoList active-todo guard to skip the guard when the LLM is
     * itself trying to update the TODO list.
     */
    private static boolean isNonMetaTool(String name) {
        if (name == null) return false;
        switch (name) {
            case "todo_list":
            case "ask_question":
            case "submit_and_exit":
                return false;
            default:
                return true;
        }
    }

    /** Forward a warning to the listener if one is attached. Null-safe. */
    private void warnListener(String message) {
        AgentListener l = currentListener;
        if (l != null && message != null) {
            try {
                l.onWarning(message);
            } catch (Throwable ignored) {
                // Listener exceptions must not break the compaction pipeline.
            }
        }
    }

    private void compactConversation(int maxInputTokens) {
        // Strategy selection: the user's "Context Compaction" dropdown in
        // Advanced settings (Profile.compactionStrategy) picks the strategy.
        // "auto" (default) mirrors the oh-my-pi pipeline:
        //   - Vision-capable model (supportsImages): SnapCompactCompactor —
        //     renders discarded history into PNG frames that the LLM reads
        //     back directly. No LLM call during compaction; fully local.
        //   - Reasoning-enabled profile (non-vision): OhMyPiCompactor
        //     (context-full LLM summarizer). Falls back to shake internally
        //     on summarizer failure.
        //   - Reasoning-disabled profile (non-vision): BasicCompactor (shake).
        //     No LLM call — safe for overflow recovery where a second
        //     failing LLM call would only compound the problem.
        // Explicit overrides:
        //   - "snapcompact" forces SnapCompactCompactor (skipped if model
        //     is not vision-capable — falls through to auto's non-vision
        //     branch with a warning).
        //   - "context-full" forces OhMyPiCompactor.
        //   - "shake" forces BasicCompactor.
        //   - "agentic-legacy" forces AgenticCompactor.
        String strategyPref = profile.compactionStrategy;
        if (strategyPref == null || strategyPref.isEmpty()) strategyPref = "auto";

        ModelInfo model = null;
        try {
            model = provider.getModel(profile.modelId);
        } catch (Throwable ignored) {
            // getModel may throw on unknown ids; we fall through to the
            // safe BasicCompactor path.
        }
        boolean modelSupportsImages = model != null && model.supportsImages;

        Compactor c;
        switch (strategyPref) {
            case "snapcompact":
                if (modelSupportsImages && toolContext != null) {
                    SnapCompactCompactor.Listener listener = event ->
                        warnListener("Compaction (snapcompact): " + event);
                    c = new SnapCompactCompactor(
                            profile.modelId,
                            toolContext.getContext(),
                            SnapCompactCompactor.DEFAULT_KEEP_RECENT_TOKENS,
                            listener);
                } else {
                    // User asked for snapcompact but the model is not
                    // vision-capable — fall back to the auto path's
                    // non-vision branch and warn.
                    warnListener("Compaction: snapcompact requested but model '"
                            + profile.modelId + "' is not vision-capable; "
                            + "falling back to "
                            + (profile.enableReasoning ? "context-full" : "shake")
                            + ".");
                    c = profile.enableReasoning
                        ? new OhMyPiCompactor(provider, profile.apiKey, profile.modelId,
                                OhMyPiCompactor.DEFAULT_KEEP_RECENT_TOKENS,
                                event -> warnListener("Compaction (context-full): " + event))
                        : new BasicCompactor();
                }
                break;
            case "context-full":
                c = new OhMyPiCompactor(provider, profile.apiKey, profile.modelId,
                        OhMyPiCompactor.DEFAULT_KEEP_RECENT_TOKENS,
                        event -> warnListener("Compaction (context-full): " + event));
                break;
            case "shake":
                c = new BasicCompactor();
                break;
            case "agentic-legacy":
                c = new AgenticCompactor(provider, profile.apiKey, profile.modelId);
                break;
            case "auto":
            default:
                if (modelSupportsImages && toolContext != null) {
                    SnapCompactCompactor.Listener listener = event ->
                        warnListener("Compaction (snapcompact): " + event);
                    c = new SnapCompactCompactor(
                            profile.modelId,
                            toolContext.getContext(),
                            SnapCompactCompactor.DEFAULT_KEEP_RECENT_TOKENS,
                            listener);
                } else if (profile.enableReasoning) {
                    OhMyPiCompactor.Listener listener = event ->
                        warnListener("Compaction (context-full): " + event);
                    c = new OhMyPiCompactor(provider, profile.apiKey, profile.modelId,
                            OhMyPiCompactor.DEFAULT_KEEP_RECENT_TOKENS, listener);
                } else {
                    c = new BasicCompactor();
                }
                break;
        }
        String strategy = c.strategyName();
        int before = estimateTokens();
        LinkedList<AgentMessage> compacted = c.compact(conversationHistory, maxInputTokens, COMPACTION_PRESERVE_RECENT);
        conversationHistory.clear();
        conversationHistory.addAll(compacted);
        int after = estimateTokens();
        warnListener("Context compacted (" + strategy + "): "
                + before + " -> " + after + " tokens, "
                + compacted.size() + " messages retained.");
    }

    private List<LlmRequest.ExtraHeader> toExtraHeaders(List<ProviderConfigStore.ExtraHeader> headers) {
        if (headers == null) return null;
        List<LlmRequest.ExtraHeader> result = new ArrayList<>();
        for (ProviderConfigStore.ExtraHeader h : headers) {
            result.add(new LlmRequest.ExtraHeader(h.name, h.value));
        }
        return result;
    }
}
