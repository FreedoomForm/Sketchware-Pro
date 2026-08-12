package com.sketchware.ai.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.context.AgenticCompactor;
import com.sketchware.ai.context.BasicCompactor;
import com.sketchware.ai.context.Compactor;
import com.sketchware.ai.context.ContextTruncator;
import com.sketchware.ai.context.OhMyPiCompactor;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.UsageTracker;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;
import com.sketchware.ai.llm.storage.ProviderConfigStore;
import com.sketchware.ai.prompt.SystemPromptBuilder;
import com.sketchware.ai.tools.AutoApprover;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolExecutor;
import com.sketchware.ai.tools.ToolPermissionGate;
import com.sketchware.ai.tools.ToolRegistry;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.JsonSchemaValidator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
        if (currentRun.get() != null && !currentRun.get().isDone()) {
            listener.onError(new IllegalStateException("A run is already in progress. Call abort() first."));
            return;
        }
        // CRITICAL: reset the abort flag so a previously-aborted runtime can
        // start a fresh run. Without this, the loop's first-iteration check
        // exits immediately and the listener is left with no callback.
        abortController.reset();
        Future<?> f = executor.submit(() -> run(userInput, listener));
        currentRun.set(f);
    }

    /** Abort the current run. */
    public void abort() {
        abortController.abort();
        provider.abort();
        Future<?> f = currentRun.get();
        if (f != null) f.cancel(true);
    }

    private void run(String userInput, AgentListener listener) {
        this.currentListener = listener;
        try {
            conversationHistory.add(AgentMessage.user(userInput));

            ModelInfo model = provider.getModel(profile.modelId);
            int iteration = 0;
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
                        toolRegistry.toJsonSchemas(),
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
                    // Try overflow recovery once.
                    listener.onWarning("Stream error: " + e.getMessage() + ". Trying compaction.");
                    compactConversation(model.maxInputTokens);
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

                // Add assistant message to history.
                conversationHistory.add(AgentMessage.assistant(
                        textBuf.toString(),
                        reasonBuf.toString(),
                        pendingToolCalls.isEmpty() ? null : pendingToolCalls));

                // No tool calls = end of turn.
                if (pendingToolCalls.isEmpty()) {
                    listener.onComplete(textBuf.toString());
                    return;
                }

                listener.onToolCalls(pendingToolCalls);

                // Execute tools sequentially.
                List<AgentMessage.ToolResultContent> results = new ArrayList<>();
                boolean submitAndExit = false;
                for (AgentMessage.ToolCall call : pendingToolCalls) {
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
                systemPrompt, conversationHistory, toolRegistry.toJsonSchemas(),
                reasoning, profile.maxOutputTokens > 0 ? profile.maxOutputTokens : model.maxOutputTokens,
                profile.enableStreaming, toExtraHeaders(profile.customHeaders),
                profile.forceFlatToolFormat);
    }

    private ToolResult executeTool(AgentMessage.ToolCall call, AgentListener listener) {
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
        // rules. Falls back to legacy gate's per-tool overrides if set.
        AutoApprover.Decision decision = autoApprover.decide(tool, args);
        // Legacy gate override: if the user explicitly set a per-tool override
        // in the old UI, honor it (only when AutoApprover didn't already DENY).
        if (decision != AutoApprover.Decision.DENY && legacyGate != null) {
            Boolean override = legacyGate.getToolAutoApprove(call.name);
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

    private static String truncateForError(String s) {
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...(" + s.length() + " chars)";
    }

    private int estimateTokens() {
        int sum = 0;
        for (AgentMessage m : conversationHistory) sum += m.estimateTokens();
        return sum;
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
        // Strategy selection (mirrors oh-my-pi's compaction pipeline):
        //  - Reasoning-enabled profile: try context-full (LLM summarizer) first.
        //    On summarizer failure, OhMyPiCompactor falls back to shake internally.
        //  - Reasoning-disabled profile: use shake (BasicCompactor) directly.
        //    No LLM call is made — safe for overflow recovery where a second
        //    failing LLM call would only compound the problem.
        Compactor c;
        if (profile.enableReasoning) {
            OhMyPiCompactor.Listener listener = event ->
                warnListener("Compaction (context-full): " + event);
            c = new OhMyPiCompactor(provider, profile.apiKey, profile.modelId,
                    OhMyPiCompactor.DEFAULT_KEEP_RECENT_TOKENS, listener);
        } else {
            c = new BasicCompactor();
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
