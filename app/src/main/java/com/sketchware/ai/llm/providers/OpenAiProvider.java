package com.sketchware.ai.llm.providers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.http.HttpClient;
import com.sketchware.ai.llm.http.SseEvent;
import com.sketchware.ai.llm.http.SseParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

/**
 * OpenAI Chat Completions provider.
 * Endpoint: {@code POST /v1/chat/completions} with SSE streaming.
 *
 * <p>Also serves as the base class for {@link OpenAiCompatProvider} (any
 * OpenAI-compatible endpoint like OpenRouter, DeepSeek, GLM, Together, etc.).
 *
 * <p>SSE: each line starts with {@code data: } followed by JSON.
 * Stream ends with {@code data: [DONE]}.
 */
public class OpenAiProvider implements LlmProvider {

    protected final Gson gson = new Gson();

    @Override public String getProviderId() { return "openai"; }

    @Override public ModelInfo getModel(String modelId) {
        switch (modelId) {
            case "gpt-4o":
            case "gpt-4o-2024-08-06":
                return new ModelInfo(modelId, "GPT-4o",
                        128_000, 128_000, 16_384,
                        true, true, false,
                        2.50, 10.0, 1.25, 2.50);
            case "gpt-4o-mini":
                return new ModelInfo(modelId, "GPT-4o mini",
                        128_000, 128_000, 16_384,
                        true, true, false,
                        0.15, 0.60, 0.075, 0.15);
            case "o1":
            case "o1-2024-12-17":
                return new ModelInfo(modelId, "o1",
                        200_000, 128_000, 100_000,
                        true, false, true,
                        15.0, 60.0, 7.50, 15.0);
            case "o3-mini":
            case "o3-mini-2025-01-31":
                return new ModelInfo(modelId, "o3-mini",
                        200_000, 200_000, 100_000,
                        true, false, true,
                        3.0, 12.0, 1.50, 3.0);
            default:
                return ModelInfo.defaultFor(modelId);
        }
    }

    @Override public Iterable<ApiStreamChunk> stream(LlmRequest request) throws Exception {
        String url = request.baseUrl == null || request.baseUrl.isEmpty()
                ? "https://api.openai.com"
                : request.baseUrl;
        url = url.replaceAll("/+$", "");
        if (url.endsWith("/chat/completions")) {
            // Already a full endpoint URL - use as-is.
        } else if (url.endsWith("/v1")) {
            // baseUrl already includes the /v1 segment - just append /chat/completions.
            url = url + "/chat/completions";
        } else {
            // Append the full OpenAI default path.
            url = url + "/v1/chat/completions";
        }

        JsonObject body = buildRequestBody(request);
        Response response = HttpClient.postStream(url, body.toString(), request.apiKey, request.extraHeaders);
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            int code = response.code();
            response.close();
            throw new RuntimeException("OpenAI HTTP " + code + ": " + errBody);
        }

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);
        final SseParser parser = new SseParser(in);

        return () -> new IteratorImpl(parser, response, in);
    }

    @Override public void abort() {
        com.sketchware.ai.llm.http.HttpClient.abortCurrent();
    }

    /**
     * Whether to serialize tools in the OpenAI Responses API <b>flat</b> format
     * ({@code {"type":"function","name":"...","description":"...","parameters":{...}}})
     * instead of the Chat Completions <b>wrapped</b> format
     * ({@code {"type":"function","function":{...},"strict":false}}).
     *
     * <p>Some OpenAI-compatible servers (notably Z.AI's GLM API at
     * {@code open.bigmodel.cn}) use a Pydantic union schema for the {@code tools}
     * field that includes built-in tool types (WebSearchTool,
     * CodeInterpreterTool, etc.) AND a generic Tool type. Their generic Tool
     * type requires the <b>flat</b> format and rejects the {@code function}
     * wrapper as an extra field, returning HTTP 422 with errors like:
     * <pre>
     * {"detail":[{"type":"literal_error","loc":[...,"WebSearchTool","type"],
     * "msg":"Input should be 'web_search'","input":"function"},
     * {"type":"extra_forbidden","loc":[...,"WebSearchTool","function"],
     * "msg":"Extra inputs are not permitted","input":{"name":"view_add_widget",...}}]}
     * </pre>
     *
     * <p>The base implementation auto-detects Z.AI/GLM endpoints by inspecting
     * {@code request.baseUrl} and {@code request.model.id}, so the flat format
     * is applied even when the user configured the provider as "openai" or
     * "openai-compat" but pointed the baseUrl at a Z.AI/GLM server. Subclasses
     * can override to force-enable or force-disable the flat format.
     *
     * @param request the active request (used to inspect baseUrl/host)
     * @return {@code true} if the request targets a Z.AI/GLM endpoint
     */
    protected boolean useFlatToolFormat(LlmRequest request) {
        // Auto-detect Z.AI / GLM endpoints even when the user selected the
        // generic "openai" or "openai-compat" provider but pointed baseUrl
        // at a Z.AI/GLM-compatible server. This catches the common misconfig
        // where the user enters https://api.z.ai/... or
        // https://open.bigmodel.cn/... in the OpenAI provider settings.
        if (request != null) {
            String url = request.baseUrl;
            if (url != null) {
                String lower = url.toLowerCase();
                if (lower.contains("z.ai") || lower.contains("bigmodel.cn") || lower.contains("/glm")) {
                    return true;
                }
            }
            if (request.model != null && request.model.id != null
                    && request.model.id.toLowerCase().contains("glm")) {
                return true;
            }
        }
        return false;
    }

    protected JsonObject buildRequestBody(LlmRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.model.id);
        root.addProperty("stream", request.enableStreaming);
        if (request.enableStreaming) {
            JsonObject streamOptions = new JsonObject();
            streamOptions.addProperty("include_usage", true);
            root.add("stream_options", streamOptions);
        }
        if (request.maxTokens > 0) {
            // OpenAI's o1 / o3 family uses max_completion_tokens instead of max_tokens.
            // Sending max_tokens to those models causes HTTP 400.
            String modelId = request.model != null && request.model.id != null
                    ? request.model.id.toLowerCase() : "";
            if (modelId.startsWith("o1") || modelId.startsWith("o3")
                    || modelId.startsWith("o4")) {
                root.addProperty("max_completion_tokens", request.maxTokens);
            } else {
                root.addProperty("max_tokens", request.maxTokens);
            }
        }

        // Messages
        JsonArray messages = new JsonArray();
        if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", request.systemPrompt);
            messages.add(sys);
        }
        for (AgentMessage m : request.messages) {
            if (AgentMessage.ROLE_SYSTEM.equals(m.role)) continue;
            // NOTE: assistant tool_calls are ALWAYS serialized in the wrapped
            // Chat Completions format ({id,type,function:{name,arguments}})
            // regardless of useFlatToolFormat(). The flat format only applies
            // to the top-level `tools` array definition — assistant message
            // tool_calls follow the standard OpenAI shape on every
            // /v1/chat/completions endpoint (including Z.AI's).
            messages.add(toOpenAiMessage(m, false));
        }
        root.add("messages", messages);

        // Tools
        if (request.toolsJson != null && !request.toolsJson.isEmpty() && !"[]".equals(request.toolsJson)) {
            JsonArray tools = JsonParser.parseString(request.toolsJson).getAsJsonArray();
            JsonArray mapped = new JsonArray();
            boolean flat = useFlatToolFormat(request);
            for (JsonElement t : tools) {
                JsonObject tool = t.getAsJsonObject();
                String name = tool.get("name").getAsString();
                String desc = tool.get("description").getAsString();
                JsonObject schema = tool.has("inputSchema")
                        ? tool.getAsJsonObject("inputSchema")
                        : new JsonObject();
                JsonObject out = new JsonObject();
                out.addProperty("type", "function");
                if (flat) {
                    // Responses API flat format — name/description/parameters at top level.
                    // Required by Z.AI's GLM API (open.bigmodel.cn) which uses a Pydantic
                    // union schema rejecting the `function` wrapper as extra_forbidden.
                    out.addProperty("name", name);
                    out.addProperty("description", desc);
                    out.add("parameters", schema);
                } else {
                    // Chat Completions wrapped format — name/description/parameters nested under function.
                    JsonObject fn = new JsonObject();
                    fn.addProperty("name", name);
                    fn.addProperty("description", desc);
                    fn.add("parameters", schema);
                    out.add("function", fn);
                    out.addProperty("strict", false);
                }
                mapped.add(out);
            }
            root.add("tools", mapped);
            // parallel_tool_calls is OpenAI-specific; some strict OpenAI-compat
            // servers (e.g. Z.AI's Pydantic schema) reject unknown fields as
            // extra_forbidden. Only emit it for the native OpenAI provider AND
            // when the request isn't auto-detected as a Z.AI/GLM endpoint.
            if ("openai".equals(getProviderId()) && !useFlatToolFormat(request)) {
                root.addProperty("parallel_tool_calls", false);
            }
        }

        // Reasoning effort (OpenAI Responses / o1 / o3)
        if (request.reasoning != null && request.reasoning.effort != null
                && request.reasoning.effort != com.sketchware.ai.llm.reasoning.ReasoningEffort.NONE) {
            root.addProperty("reasoning_effort", request.reasoning.effort.name().toLowerCase());
        }

        return root;
    }

    protected JsonObject toOpenAiMessage(AgentMessage m, boolean flatToolFormat) {
        JsonObject obj = new JsonObject();

        if (m.hasToolResults()) {
            // OpenAI tool result format: role=tool, content=text, tool_call_id=...
            // If multiple results, emit multiple messages.
            // We emit the first as the message; callers should treat multi-result
            // messages appropriately.
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                obj.addProperty("role", "tool");
                obj.addProperty("tool_call_id", r.toolCallId);
                obj.addProperty("content", r.isError ? ("ERROR: " + r.output) : r.output);
                break; // one per message; multi-result not supported by OpenAI
            }
            return obj;
        }

        if ("assistant".equals(m.role)) {
            obj.addProperty("role", "assistant");
            if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
                JsonArray arr = new JsonArray();
                for (AgentMessage.ToolCall tc : m.toolCalls) {
                    JsonObject call = new JsonObject();
                    call.addProperty("id", tc.id);
                    call.addProperty("type", "function");
                    // ALWAYS use the wrapped format for assistant tool_calls.
                    // The OpenAI Chat Completions spec requires
                    // {id, type, function: {name, arguments}} regardless of how
                    // the `tools` array is shaped. (The flatToolFormat flag
                    // applies only to the `tools` array, NOT to assistant
                    // message tool_calls.) Sending flat assistant tool_calls
                    // to /v1/chat/completions causes HTTP 400/422 on every
                    // OpenAI-compat server we tested.
                    JsonObject fn = new JsonObject();
                    fn.addProperty("name", tc.name);
                    fn.addProperty("arguments", tc.argumentsJson == null ? "{}" : tc.argumentsJson);
                    call.add("function", fn);
                    arr.add(call);
                }
                obj.add("tool_calls", arr);
            }
            if (m.text != null && !m.text.isEmpty()) {
                obj.addProperty("content", m.text);
            }
            return obj;
        }

        // user
        obj.addProperty("role", "user");
        if (m.images != null && !m.images.isEmpty()) {
            JsonArray content = new JsonArray();
            if (m.text != null && !m.text.isEmpty()) {
                JsonObject t = new JsonObject();
                t.addProperty("type", "text");
                t.addProperty("text", m.text);
                content.add(t);
            }
            for (String img : m.images) {
                JsonObject im = new JsonObject();
                im.addProperty("type", "image_url");
                JsonObject url = new JsonObject();
                url.addProperty("url", img.startsWith("data:") ? img : "data:image/png;base64," + img);
                im.add("image_url", url);
                content.add(im);
            }
            obj.add("content", content);
        } else {
            obj.addProperty("content", m.text == null ? "" : m.text);
        }
        return obj;
    }

    protected final class IteratorImpl implements java.util.Iterator<ApiStreamChunk> {
        private final SseParser parser;
        private final Response response;
        private final InputStream stream;
        private ApiStreamChunk next;
        private boolean done;
        // Pending error to surface to the caller. When readNext() throws a
        // non-IOException (e.g. server error mid-stream), we store it here so
        // hasNext() can rethrow it instead of silently ending the stream.
        private Throwable pendingError;
        // Pending tool calls indexed by OpenAI-assigned index.
        private final java.util.Map<Integer, String> toolIds = new java.util.HashMap<>();
        private final java.util.Map<Integer, String> toolNames = new java.util.HashMap<>();
        private final java.util.Map<Integer, StringBuilder> toolArgs = new java.util.HashMap<>();
        private int inputTokens = 0;
        private int outputTokens = 0;
        private int reasoningTokens = 0;
        private int cacheReadTokens = 0;
        private int cacheWriteTokens = 0;
        private boolean usageEmitted = false;
        private boolean doneEmitted = false;

        IteratorImpl(SseParser parser, Response response, InputStream stream) {
            this.parser = parser;
            this.response = response;
            this.stream = stream;
        }

        @Override public boolean hasNext() {
            if (next != null) return true;
            if (done) return false;
            try {
                next = readNext();
            } catch (Exception e) {
                done = true;
                closeQuietly();
                if (!(e instanceof java.io.IOException)) {
                    pendingError = e;
                }
                return false;
            }
            if (next == null) {
                done = true;
                closeQuietly();
                return false;
            }
            return true;
        }

        @Override public ApiStreamChunk next() {
            if (!hasNext()) {
                if (pendingError != null) {
                    if (pendingError instanceof RuntimeException) {
                        throw (RuntimeException) pendingError;
                    }
                    throw new RuntimeException(pendingError);
                }
                throw new java.util.NoSuchElementException();
            }
            ApiStreamChunk result = next;
            next = null;
            return result;
        }

        private ApiStreamChunk readNext() throws Exception {
            for (SseEvent ev : parser) {
                if (ev.data == null || ev.data.isEmpty() || "[DONE]".equals(ev.data)) {
                    // Emit any pending tool calls + usage + done
                    ApiStreamChunk r = emitPendingToolCalls();
                    if (r != null) return r;
                    if (!usageEmitted) {
                        usageEmitted = true;
                        double cost = computeCost();
                        return new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                                cacheReadTokens, cacheWriteTokens, cost);
                    }
                    if (!doneEmitted) {
                        doneEmitted = true;
                        return new ApiStreamChunk.Done();
                    }
                    return null;
                }
                JsonObject obj = JsonParser.parseString(ev.data).getAsJsonObject();
                // Process usage FIRST so we don't lose it when the same chunk
                // also carries text/tool_calls (some servers bundle them).
                if (obj.has("usage")) {
                    JsonObject u = obj.getAsJsonObject("usage");
                    if (u.has("prompt_tokens")) inputTokens = u.get("prompt_tokens").getAsInt();
                    if (u.has("completion_tokens")) outputTokens = u.get("completion_tokens").getAsInt();
                    if (u.has("prompt_tokens_details") && u.getAsJsonObject("prompt_tokens_details").has("cached_tokens")) {
                        cacheReadTokens = u.getAsJsonObject("prompt_tokens_details").get("cached_tokens").getAsInt();
                    }
                    if (u.has("completion_tokens_details") && u.getAsJsonObject("completion_tokens_details").has("reasoning_tokens")) {
                        reasoningTokens = u.getAsJsonObject("completion_tokens_details").get("reasoning_tokens").getAsInt();
                    }
                }
                if (obj.has("choices")) {
                    JsonArray choices = obj.getAsJsonArray("choices");
                    if (choices.size() == 0) continue;
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject delta = choice.has("delta") ? choice.getAsJsonObject("delta") : null;
                    if (delta != null) {
                        // CRITICAL: buffer tool_calls FIRST, before the
                        // content/reasoning return statements below. Some
                        // servers (vLLM, Mistral, OpenRouter, Z.AI) emit a
                        // single delta carrying BOTH `content` AND `tool_calls`.
                        // If we `return Text` before buffering the tool_calls,
                        // the tool_calls in that delta are silently lost —
                        // the agent runtime never sees them, the LLM's
                        // intended tool call is dropped, and the conversation
                        // ends prematurely with no tool execution.
                        if (delta.has("tool_calls")) {
                            JsonArray arr = delta.getAsJsonArray("tool_calls");
                            for (JsonElement e : arr) {
                                JsonObject tc = e.getAsJsonObject();
                                int idx = tc.has("index") ? tc.get("index").getAsInt() : 0;
                                if (tc.has("id") && !tc.get("id").isJsonNull()) {
                                    toolIds.put(idx, tc.get("id").getAsString());
                                }
                                // Accept BOTH wrapped ({function:{name,arguments}}) and
                                // flat ({name,arguments}) tool_call shapes — servers differ.
                                // Some servers (e.g. Mistral, older vLLM) omit the `type`
                                // field entirely, so don't gate on type == "function".
                                JsonObject fn = tc.has("function") ? tc.getAsJsonObject("function") : tc;
                                if (fn.has("name") && !fn.get("name").isJsonNull()) {
                                    toolNames.put(idx, fn.get("name").getAsString());
                                }
                                if (fn.has("arguments") && !fn.get("arguments").isJsonNull()) {
                                    String partial = fn.get("arguments").getAsString();
                                    toolArgs.computeIfAbsent(idx, k -> new StringBuilder()).append(partial);
                                }
                                // If the server sent name+arguments but no id, generate
                                // a synthetic id so emitPendingToolCalls() actually fires.
                                // Without this, toolIds stays empty and the accumulated
                                // tool call is silently dropped.
                                if (!toolIds.containsKey(idx)) {
                                    toolIds.put(idx, "call_" + idx + "_" + System.currentTimeMillis());
                                }
                            }
                        }
                        if (delta.has("content")) {
                            JsonElement c = delta.get("content");
                            if (c != null && !c.isJsonNull() && c.isJsonPrimitive()) {
                                String text = c.getAsString();
                                if (!text.isEmpty()) return new ApiStreamChunk.Text(text);
                            }
                        }
                        if (delta.has("reasoning_content")) {
                            JsonElement r = delta.get("reasoning_content");
                            if (r != null && !r.isJsonNull() && r.isJsonPrimitive()) {
                                String text = r.getAsString();
                                if (!text.isEmpty()) return new ApiStreamChunk.Reasoning(text);
                            }
                        }
                        // DeepSeek reasoning field alternative
                        if (delta.has("reasoning")) {
                            JsonElement r = delta.get("reasoning");
                            if (r != null && !r.isJsonNull() && r.isJsonPrimitive()) {
                                String text = r.getAsString();
                                if (!text.isEmpty()) return new ApiStreamChunk.Reasoning(text);
                            }
                        }
                    }
                    if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                        String reason = choice.get("finish_reason").getAsString();
                        ApiStreamChunk r = emitPendingToolCalls();
                        if (r != null) return r;
                        // Don't break — keep reading; usage chunk usually follows.
                    }
                }
            }
            // End of stream
            ApiStreamChunk r = emitPendingToolCalls();
            if (r != null) return r;
            if (!usageEmitted) {
                usageEmitted = true;
                double cost = computeCost();
                return new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                        cacheReadTokens, cacheWriteTokens, cost);
            }
            if (!doneEmitted) {
                doneEmitted = true;
                return new ApiStreamChunk.Done();
            }
            return null;
        }

        private ApiStreamChunk emitPendingToolCalls() {
            // Check toolNames/toolArgs too — some servers send name+arguments
            // without an id (we synthesize one in the accumulator, but be
            // defensive here in case the id path was never hit).
            if (toolIds.isEmpty() && toolNames.isEmpty() && toolArgs.isEmpty()) return null;
            List<AgentMessage.ToolCall> calls = new ArrayList<>();
            // Union of all indexes we've seen, sorted for deterministic order.
            java.util.Set<Integer> idxSet = new java.util.TreeSet<>();
            idxSet.addAll(toolIds.keySet());
            idxSet.addAll(toolNames.keySet());
            idxSet.addAll(toolArgs.keySet());
            for (int idx : idxSet) {
                String id = toolIds.get(idx);
                if (id == null || id.isEmpty()) {
                    id = "call_" + idx + "_" + System.currentTimeMillis();
                }
                String name = toolNames.getOrDefault(idx, "unknown");
                StringBuilder args = toolArgs.get(idx);
                String argsJson = args == null ? "{}" : args.toString();
                if (argsJson.isEmpty()) argsJson = "{}";
                calls.add(new AgentMessage.ToolCall(id, name, argsJson));
            }
            toolIds.clear();
            toolNames.clear();
            toolArgs.clear();
            return new ApiStreamChunk.ToolCalls(calls);
        }

        private double computeCost() {
            // Walk back up to the provider to get the ModelInfo for pricing.
            // OpenAiProvider.this.model is not stored; we accept 0 cost if unknown.
            // (Subclasses with explicit model catalogs can override.)
            return 0;
        }

        private void closeQuietly() {
            try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            try { if (response != null) response.close(); } catch (Exception ignored) {}
        }
    }
}
