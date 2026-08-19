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
        // Use the appropriate Accept header: text/event-stream for streaming,
        // application/json for non-streaming. Some servers (Mistral) return
        // a regular JSON body when stream=false regardless of Accept, but
        // sending the correct Accept avoids ambiguity.
        //
        // Use postStreamWithRetry to get automatic 429/5xx retry with
        // exponential backoff + Retry-After header honoring. This prevents
        // the "retry also gets 429" cascade that previously caused the agent
        // to abort on the first rate-limit response.
        boolean sse = request.enableStreaming;
        Response response = HttpClient.postStreamWithRetry(url, body.toString(), request.apiKey, request.extraHeaders, sse);
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            int code = response.code();
            response.close();
            throw new RuntimeException("OpenAI HTTP " + code + ": " + errBody);
        }

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);

        if (!sse) {
            // Non-streaming: parse a single JSON response object.
            final String responseBody;
            try (java.util.Scanner scanner = new java.util.Scanner(in, java.nio.charset.StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                responseBody = scanner.hasNext() ? scanner.next() : "";
            }
            response.close();
            return () -> new SingleShotIterator(responseBody);
        }

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
     * ({@code {"type":"function","function":{...}}}).
     *
     * <p><b>IMPORTANT:</b> Z.AI's GLM API (despite using a Pydantic union schema
     * for tools that includes built-in types like WebSearchTool) accepts the
     * standard <b>wrapped</b> format for its generic {@code Tool} type. The
     * only fields it rejects are {@code strict}, {@code parallel_tool_calls},
     * and {@code stream_options} — those are already suppressed for non-native
     * OpenAI providers in {@link #buildRequestBody(LlmRequest)}.
     *
     * <p>The previous implementation auto-detected Z.AI/GLM endpoints by URL
     * and forced the flat format. That was <b>wrong</b> — it caused HTTP 422
     * {@code extra_forbidden} on every tool call because Z.AI's generic
     * {@code Tool} type does NOT accept top-level {@code name}/
     * {@code description}/{@code parameters} fields.
     *
     * <p>Flat format is now opt-in ONLY via {@link LlmRequest#forceFlatToolFormat}
     * (user toggle in API settings). It should not be needed for any known
     * provider, but is kept as an escape hatch.
     *
     * @param request the active request
     * @return {@code true} only if the user explicitly enabled flat format
     */
    protected boolean useFlatToolFormat(LlmRequest request) {
        // IMPORTANT: Z.AI's GLM API uses the STANDARD OpenAI wrapped format
        // {type:"function", function:{name, description, parameters}} — NOT
        // the flat Responses API format. The previous auto-detection that
        // forced flat format for z.ai/bigmodel.cn URLs was WRONG and caused
        // HTTP 422 extra_forbidden errors on every tool call.
        //
        // The only OpenAI-specific field that Z.AI rejects is `strict` (and
        // `parallel_tool_calls`, `stream_options`) — those are already
        // suppressed for non-native-OpenAI providers in buildRequestBody().
        //
        // The flat format is now opt-in ONLY via the user toggle
        // (Profile.forceFlatToolFormat). It should not be needed for any
        // known provider, but is kept as an escape hatch for hypothetical
        // servers that genuinely require it.
        return request != null && request.forceFlatToolFormat;
    }

    protected JsonObject buildRequestBody(LlmRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.model.id);
        root.addProperty("stream", request.enableStreaming);
        boolean flat = useFlatToolFormat(request);
        // stream_options.include_usage is OpenAI-specific. Most OpenAI-compat
        // servers (Mistral, DeepSeek, Together, Fireworks, OpenRouter) either
        // reject it as HTTP 400 or silently ignore it. Only send it for the
        // native OpenAI provider AND when the request isn't targeting a
        // flat-format (Z.AI/GLM) endpoint. The usage data will simply be
        // absent from the SSE stream for other providers — acceptable
        // trade-off for not getting HTTP 400/422.
        boolean isOpenAiNative = "openai".equals(getProviderId()) && !flat;
        if (request.enableStreaming && isOpenAiNative) {
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
            //
            // CRITICAL: when an assistant message issued N tool_calls, the
            // OpenAI Chat Completions API requires N separate role=tool
            // messages — one per tool_call_id. The previous implementation
            // emitted only the first result and `break`ed out, dropping the
            // rest, which caused HTTP 400 "Not the same number of function
            // calls and responses" on every multi-tool turn.
            if (m.hasToolResults() && m.toolResults.size() > 1) {
                for (AgentMessage.ToolResultContent r : m.toolResults) {
                    messages.add(toOpenAiToolResultMessage(r));
                }
            } else {
                messages.add(toOpenAiMessage(m, false));
            }
        }
        root.add("messages", messages);

        // Tools
        if (request.toolsJson != null && !request.toolsJson.isEmpty() && !"[]".equals(request.toolsJson)) {
            JsonArray tools = JsonParser.parseString(request.toolsJson).getAsJsonArray();
            JsonArray mapped = new JsonArray();
            // `flat` was computed once at the top of buildRequestBody(); reuse it here.
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
                    // `strict` is OpenAI's structured-outputs field. Sending it
                    // to OpenAI-compat servers (Mistral, DeepSeek, etc.) can
                    // cause HTTP 400 "unknown field". Only emit for native OpenAI.
                    if (isOpenAiNative) {
                        out.addProperty("strict", false);
                    }
                }
                mapped.add(out);
            }
            root.add("tools", mapped);
            // parallel_tool_calls is OpenAI-specific; some strict OpenAI-compat
            // servers (e.g. Z.AI's Pydantic schema) reject unknown fields as
            // extra_forbidden. Only emit it for the native OpenAI provider AND
            // when the request isn't auto-detected as a Z.AI/GLM endpoint.
            if (isOpenAiNative) {
                root.addProperty("parallel_tool_calls", false);
            }
        }

        // Reasoning effort (OpenAI Responses / o1 / o3).
        // Suppress for non-native OpenAI-compat endpoints: Z.AI's GLM uses
        // `thinking.type` (added by OpenAiCompatProvider.buildRequestBody) and
        // rejects `reasoning_effort` as extra_forbidden (HTTP 422). DeepSeek
        // and OpenRouter have their own reasoning fields and treat the bare
        // `reasoning_effort` string as an unknown field. Only emit it for the
        // native OpenAI provider — compat providers own their reasoning
        // serialization in their own buildRequestBody override.
        if (isOpenAiNative && request.reasoning != null && request.reasoning.effort != null
                && request.reasoning.effort != com.sketchware.ai.llm.reasoning.ReasoningEffort.NONE) {
            root.addProperty("reasoning_effort", request.reasoning.effort.name().toLowerCase());
        }

        return root;
    }

    /**
     * Build a single {@code role=tool} message for one tool result. The OpenAI
     * Chat Completions API requires ONE message per {@code tool_call_id} —
     * when an assistant message issued N tool_calls, the next turn MUST
     * contain N role=tool messages, each carrying its own
     * {@code tool_call_id}. Bundling them into one message (or dropping all
     * but the first) triggers HTTP 400
     * "Not the same number of function calls and responses".
     */
    protected JsonObject toOpenAiToolResultMessage(AgentMessage.ToolResultContent r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "tool");
        obj.addProperty("tool_call_id", r.toolCallId);
        // String.valueOf(null) returns the literal "null" — better than NPE
        // from JsonObject.addProperty(String, String) which rejects null. A
        // tool returning null output is rare but possible (e.g. a formatter
        // bug), and crashing the request build is worse than sending "null".
        String safeOutput = String.valueOf(r.output);
        obj.addProperty("content", r.isError ? ("ERROR: " + safeOutput) : safeOutput);
        return obj;
    }

    protected JsonObject toOpenAiMessage(AgentMessage m, boolean flatToolFormat) {
        JsonObject obj = new JsonObject();

        if (m.hasToolResults()) {
            // Single-result tool_result message. Multi-result batches are
            // unrolled by the caller (buildRequestBody) into N separate
            // role=tool messages via toOpenAiToolResultMessage().
            AgentMessage.ToolResultContent r = m.toolResults.get(0);
            return toOpenAiToolResultMessage(r);
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
                                    String n = fn.get("name").getAsString();
                                    // CRITICAL: don't store empty strings as tool names.
                                    // Some proxies (notably AgentRouter when proxying
                                    // Claude) occasionally emit "name":"" in the
                                    // tool_call delta. If we store "", the downstream
                                    // toolNames.getOrDefault(idx,"unknown") returns ""
                                    // (the key exists), and the tool executor fails
                                    // with "Unknown tool: ''" — triggering an infinite
                                    // loop as the model retries the same empty-name
                                    // call. By skipping empty names here, we let the
                                    // "unknown" fallback kick in, and AgentRuntime's
                                    // schema-matching inference can then identify the
                                    // intended tool from its arguments.
                                    if (n != null && !n.isEmpty()) {
                                        toolNames.put(idx, n);
                                    }
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

    /**
     * Iterator for non-streaming responses (when {@code stream:false} is sent).
     * Parses a single JSON Chat Completions response object and emits the
     * appropriate chunks: Text (or ToolCalls), Usage, Done.
     *
     * <p>This is used by the {@link com.sketchware.ai.context.AgenticCompactor}
     * which sets {@code enableStreaming=false} for its summarization call.
     * Previously, non-streaming responses were fed to {@link SseParser}, which
     * expected SSE format and silently returned no events — resulting in an
     * empty summary and a confusing "no text" result for the user.
     */
    protected final class SingleShotIterator implements java.util.Iterator<ApiStreamChunk> {
        private final String json;
        private ApiStreamChunk next;
        private boolean done = false;
        private boolean usageEmitted = false;
        private boolean doneEmitted = false;
        private boolean parsed = false;
        // Extracted from JSON on first hasNext() call.
        private String textContent = "";
        private java.util.List<AgentMessage.ToolCall> toolCalls = null;
        private int inputTokens = 0;
        private int outputTokens = 0;
        private int reasoningTokens = 0;
        private int cacheReadTokens = 0;
        private int cacheWriteTokens = 0;

        SingleShotIterator(String responseBody) {
            this.json = responseBody == null ? "" : responseBody;
        }

        private void parseOnce() {
            if (parsed) return;
            parsed = true;
            if (json.isEmpty()) return;
            try {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
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
                    if (choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject msg = choice.has("message") ? choice.getAsJsonObject("message") : null;
                        if (msg != null) {
                            if (msg.has("content") && !msg.get("content").isJsonNull()) {
                                JsonElement c = msg.get("content");
                                if (c.isJsonPrimitive()) {
                                    textContent = c.getAsString();
                                }
                            }
                            if (msg.has("tool_calls") && !msg.get("tool_calls").isJsonNull()) {
                                JsonArray arr = msg.getAsJsonArray("tool_calls");
                                toolCalls = new java.util.ArrayList<>();
                                for (JsonElement e : arr) {
                                    JsonObject tc = e.getAsJsonObject();
                                    String id = tc.has("id") && !tc.get("id").isJsonNull()
                                            ? tc.get("id").getAsString()
                                            : "call_" + toolCalls.size() + "_" + System.currentTimeMillis();
                                    JsonObject fn = tc.has("function") ? tc.getAsJsonObject("function") : tc;
                                    String name = fn.has("name") && !fn.get("name").isJsonNull()
                                            ? fn.get("name").getAsString() : "";
                                    // Don't pass empty string through — let the
                                    // "unknown" fallback trigger schema-matching
                                    // inference in AgentRuntime. See streaming
                                    // parser above for the full rationale.
                                    if (name == null || name.isEmpty()) {
                                        name = "unknown";
                                    }
                                    String args = fn.has("arguments") && !fn.get("arguments").isJsonNull()
                                            ? fn.get("arguments").getAsString() : "{}";
                                    toolCalls.add(new AgentMessage.ToolCall(id, name, args));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Malformed JSON — leave fields at defaults.
            }
        }

        @Override public boolean hasNext() {
            if (next != null) return true;
            if (done) return false;
            parseOnce();
            // Emit order: ToolCalls (if any) OR Text, then Usage, then Done.
            if (toolCalls != null && !toolCalls.isEmpty()) {
                next = new ApiStreamChunk.ToolCalls(toolCalls);
                toolCalls = null;
                return true;
            }
            if (!textContent.isEmpty()) {
                next = new ApiStreamChunk.Text(textContent);
                textContent = "";
                return true;
            }
            if (!usageEmitted) {
                usageEmitted = true;
                next = new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                        cacheReadTokens, cacheWriteTokens, 0.0);
                return true;
            }
            if (!doneEmitted) {
                doneEmitted = true;
                next = new ApiStreamChunk.Done();
                return true;
            }
            done = true;
            return false;
        }

        @Override public ApiStreamChunk next() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
            ApiStreamChunk result = next;
            next = null;
            return result;
        }
    }
}
