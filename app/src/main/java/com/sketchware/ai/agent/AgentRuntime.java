package com.sketchware.ai.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.context.AgenticCompactor;
import com.sketchware.ai.context.BasicCompactor;
import com.sketchware.ai.context.Compactor;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.reasoning.ReasoningEffort;
import com.sketchware.ai.llm.reasoning.ReasoningRequest;
import com.sketchware.ai.llm.storage.ProviderConfigStore;
import com.sketchware.ai.prompt.SystemPromptBuilder;
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
    private final ToolPermissionGate permissionGate;
    private final ProviderConfigStore.Profile profile;
    private final String systemPrompt;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<Future<?>> currentRun = new AtomicReference<>();
    private final AbortController abortController = new AbortController();

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
        this.permissionGate = permissionGate;
        this.profile = profile;
        this.systemPrompt = systemPrompt;
    }

    public void setMode(AgentMode mode) { this.mode = mode; permissionGate.setMode(mode); }
    public AgentMode getMode() { return mode; }

    public void setMaxIterations(int max) { this.maxIterations = max; }

    /**
     * Start the agent loop with a new user message.
     * Returns immediately; the loop runs on a background thread.
     */
    public void execute(String userInput, AgentListener listener) {
        if (currentRun.get() != null && !currentRun.get().isDone()) {
            listener.onError(new IllegalStateException("A run is already in progress. Call abort() first."));
            return;
        }
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
                        toExtraHeaders(profile.customHeaders));

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
                            listener.onUsage(u.inputTokens, u.outputTokens, u.reasoningTokens, u.cost);
                        } else if (chunk.isDone()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Try overflow recovery once.
                    listener.onWarning("Stream error: " + e.getMessage() + ". Trying compaction.");
                    compactConversation(model.maxInputTokens);
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
                                listener.onUsage(u.inputTokens, u.outputTokens, u.reasoningTokens, u.cost);
                            } else if (chunk.isDone()) break;
                        }
                    } catch (Exception e2) {
                        listener.onError(e2);
                        return;
                    }
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
                for (AgentMessage.ToolCall call : pendingToolCalls) {
                    if (abortController.isAborted()) break;
                    listener.onToolStart(call.id, call.name, call.argumentsJson);
                    ToolResult result = executeTool(call, listener);
                    AgentMessage.ToolResultContent tr = new AgentMessage.ToolResultContent(
                            call.id, call.name,
                            result == null ? "" : result.toLLMString(),
                            result != null && result.isError());
                    results.add(tr);
                    listener.onToolResult(call.id, tr);
                }
                conversationHistory.add(AgentMessage.toolResult(results));

                // Check for context overflow.
                if (estimateTokens() > COMPACTION_TRIGGER_RATIO * model.maxInputTokens) {
                    compactConversation(model.maxInputTokens);
                }
            }

            if (iteration > maxIterations) {
                listener.onMaxIterationsReached(maxIterations);
            }
        } catch (Throwable t) {
            listener.onError(t);
        } finally {
            currentRun.set(null);
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
                profile.enableStreaming, toExtraHeaders(profile.customHeaders));
    }

    private ToolResult executeTool(AgentMessage.ToolCall call, AgentListener listener) {
        SketchwareTool tool = toolRegistry.get(call.name);
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + call.name);
        }
        // Permission gate
        ToolPermissionGate.Decision decision = permissionGate.decide(tool);
        if (decision == ToolPermissionGate.Decision.DENY) {
            return ToolResult.error("Tool '" + call.name + "' is not allowed in current mode.");
        }
        if (decision == ToolPermissionGate.Decision.REQUIRE_APPROVAL) {
            // For MVP: auto-approve if user is in YOLO. In ACT mode, we'd need a UI prompt.
            // Since this runs on a background thread and the UI prompt is async,
            // we currently treat it as auto-approve (the user can undo via Sketchware's
            // own undo/redo buttons). A proper approve dialog will be wired up in
            // the ChatFragment.
            // TODO: implement async approval flow via listener.onApprovalRequired(call)
        }
        // Parse and validate args.
        JsonObject args;
        try {
            args = call.argumentsJson == null || call.argumentsJson.isEmpty()
                    ? new JsonObject()
                    : JsonParser.parseString(call.argumentsJson).getAsJsonObject();
        } catch (Exception e) {
            return ToolResult.error("Invalid arguments JSON: " + e.getMessage());
        }
        JsonSchemaValidator.ValidationResult v = JsonSchemaValidator.validate(args, tool.jsonSchema());
        if (!v.ok) {
            return ToolResult.error("Validation failed: " + v.error);
        }
        // Execute - the context is provided by the caller via ThreadLocal or
        // constructor. For MVP we use a thread-local context set by ChatFragment.
        SketchwareToolContext ctx = currentContext.get();
        if (ctx == null) {
            return ToolResult.error("No tool context available.");
        }
        ToolResult result = toolExecutor.execute(call.name, args, ctx);
        // Refresh UI
        ctx.refreshAllEditors();
        return result;
    }

    // Thread-local tool context (set by ChatFragment before calling execute).
    private static final ThreadLocal<SketchwareToolContext> currentContext = new ThreadLocal<>();
    public static void setContext(SketchwareToolContext ctx) { currentContext.set(ctx); }
    public static void clearContext() { currentContext.remove(); }

    private int estimateTokens() {
        int sum = 0;
        for (AgentMessage m : conversationHistory) sum += m.estimateTokens();
        return sum;
    }

    private void compactConversation(int maxInputTokens) {
        Compactor c;
        if (profile.enableReasoning) {
            c = new AgenticCompactor(provider, profile.apiKey, profile.modelId);
        } else {
            c = new BasicCompactor();
        }
        LinkedList<AgentMessage> compacted = c.compact(conversationHistory, maxInputTokens, COMPACTION_PRESERVE_RECENT);
        conversationHistory.clear();
        conversationHistory.addAll(compacted);
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
