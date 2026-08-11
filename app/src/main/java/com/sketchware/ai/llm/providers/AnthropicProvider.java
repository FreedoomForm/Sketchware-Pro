package com.sketchware.ai.llm.providers;

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
 * Anthropic Messages API provider.
 *
 * <p>Endpoint: {@code POST /v1/messages} with SSE streaming.
 *
 * <p>Wire format captured from Cline VCR fixture
 * {@code tests/provider-vcr/anthropic-claude-sonnet-4-6.json}.
 *
 * <p>SSE events:
 * <ul>
 *   <li>{@code message_start} - opening metadata (id, model, usage)</li>
 *   <li>{@code content_block_start} - opens a text/tool_use/thinking block</li>
 *   <li>{@code content_block_delta} - delta for the open block (text_delta / input_json_delta / thinking_delta)</li>
 *   <li>{@code content_block_stop} - closes the current block</li>
 *   <li>{@code message_delta} - final usage (output_tokens)</li>
 *   <li>{@code message_stop} - end of stream</li>
 * </ul>
 */
public final class AnthropicProvider implements LlmProvider {

    @Override public String getProviderId() { return "anthropic"; }

    @Override public ModelInfo getModel(String modelId) {
        // Common Anthropic models.
        switch (modelId) {
            case "claude-3-5-sonnet-20241022":
            case "claude-3-5-sonnet-latest":
                return new ModelInfo(modelId, "Claude 3.5 Sonnet",
                        200_000, 200_000, 8192,
                        true, true, false,
                        3.0, 15.0, 0.30, 3.75);
            case "claude-3-5-haiku-20241022":
            case "claude-3-5-haiku-latest":
                return new ModelInfo(modelId, "Claude 3.5 Haiku",
                        200_000, 200_000, 8192,
                        true, true, false,
                        0.80, 4.0, 0.08, 1.0);
            case "claude-sonnet-4-20250514":
            case "claude-sonnet-4":
                return new ModelInfo(modelId, "Claude Sonnet 4",
                        200_000, 200_000, 16_384,
                        true, true, true,
                        3.0, 15.0, 0.30, 3.75);
            case "claude-opus-4-20250514":
            case "claude-opus-4":
                return new ModelInfo(modelId, "Claude Opus 4",
                        200_000, 200_000, 32_768,
                        true, true, true,
                        15.0, 75.0, 1.50, 18.75);
            default:
                return ModelInfo.defaultFor(modelId);
        }
    }

    @Override public Iterable<ApiStreamChunk> stream(LlmRequest request) throws Exception {
        String url = request.baseUrl == null || request.baseUrl.isEmpty()
                ? "https://api.anthropic.com"
                : request.baseUrl;
        if (!url.endsWith("/v1/messages")) {
            url = url.replaceAll("/+$", "") + "/v1/messages";
        }

        JsonObject body = buildRequestBody(request);
        List<LlmRequest.ExtraHeader> headers = new ArrayList<>();
        headers.add(new LlmRequest.ExtraHeader("x-api-key", request.apiKey == null ? "" : request.apiKey));
        headers.add(new LlmRequest.ExtraHeader("anthropic-version", "2023-06-01"));
        if (request.extraHeaders != null) headers.addAll(request.extraHeaders);

        // Note: HttpClient adds Bearer auth by default - we want x-api-key only.
        // We pass null apiKey so HttpClient doesn't add Authorization, then
        // x-api-key comes from extraHeaders.
        Response response = HttpClient.postStream(url, body.toString(), null, headers);
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            response.close();
            throw new RuntimeException("Anthropic HTTP " + response.code() + ": " + errBody);
        }

        // Track the in-flight call so abort() can cancel it.
        // (HttpClient doesn't expose the Call - for cancellation we rely on the
        //  loop polling abort and on okhttp interrupting the reader when the
        //  OkHttp client is shut down. For a more robust impl, HttpClient should
        //  return the Call.)

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);
        final SseParser parser = new SseParser(in);

        return () -> new IteratorImpl(parser, response, in);
    }

    @Override public void abort() {
        com.sketchware.ai.llm.http.HttpClient.abortCurrent();
    }

    private JsonObject buildRequestBody(LlmRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.model.id);
        root.addProperty("max_tokens", request.maxTokens > 0 ? request.maxTokens : 4096);
        root.addProperty("stream", true);

        // System prompt
        if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
            JsonObject system = new JsonObject();
            system.addProperty("type", "text");
            system.addProperty("text", request.systemPrompt);
            JsonArray arr = new JsonArray();
            arr.add(system);
            root.add("system", arr);
        }

        // Messages
        JsonArray messages = new JsonArray();
        for (AgentMessage m : request.messages) {
            if (AgentMessage.ROLE_SYSTEM.equals(m.role)) continue;
            messages.add(toAnthropicMessage(m));
        }
        root.add("messages", messages);

        // Tools
        if (request.toolsJson != null && !request.toolsJson.isEmpty() && !"[]".equals(request.toolsJson)) {
            JsonArray tools = JsonParser.parseString(request.toolsJson).getAsJsonArray();
            JsonArray mapped = new JsonArray();
            for (JsonElement t : tools) {
                JsonObject tool = t.getAsJsonObject();
                JsonObject mappedTool = new JsonObject();
                mappedTool.addProperty("name", tool.get("name").getAsString());
                mappedTool.addProperty("description", tool.get("description").getAsString());
                JsonObject schema = tool.has("inputSchema")
                        ? tool.getAsJsonObject("inputSchema")
                        : new JsonObject();
                mappedTool.add("input_schema", schema);
                mapped.add(mappedTool);
            }
            root.add("tools", mapped);
        }

        // Reasoning (thinking) - only if model supports it
        if (request.reasoning != null && request.reasoning.isReasoningEnabled()
                && request.model != null && request.model.supportsReasoning) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "enabled");
            if (request.reasoning.budgetTokens != null) {
                thinking.addProperty("budget_tokens", request.reasoning.budgetTokens);
            } else {
                // Default budget = max_tokens - 1 (Anthropic quirk: max_tokens must be > budget)
                int budget = Math.max(1024, request.maxTokens - 1);
                thinking.addProperty("budget_tokens", budget);
            }
            root.add("thinking", thinking);
        }

        return root;
    }

    private JsonObject toAnthropicMessage(AgentMessage m) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "user".equals(m.role) || "tool".equals(m.role) ? "user" : "assistant");
        JsonArray content = new JsonArray();

        if (m.hasToolResults()) {
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                JsonObject tr = new JsonObject();
                tr.addProperty("type", "tool_result");
                tr.addProperty("tool_use_id", r.toolCallId);
                JsonObject rContent = new JsonObject();
                rContent.addProperty("type", "text");
                rContent.addProperty("text", r.isError ? ("ERROR: " + r.output) : r.output);
                tr.add("content", rContent);
                content.add(tr);
            }
        }

        if (m.toolCalls != null) {
            for (AgentMessage.ToolCall tc : m.toolCalls) {
                JsonObject tu = new JsonObject();
                tu.addProperty("type", "tool_use");
                tu.addProperty("id", tc.id);
                tu.addProperty("name", tc.name);
                JsonObject args = tc.argumentsJson == null || tc.argumentsJson.isEmpty()
                        ? new JsonObject()
                        : JsonParser.parseString(tc.argumentsJson).getAsJsonObject();
                tu.add("input", args);
                content.add(tu);
            }
        }

        if (m.reasoning != null && !m.reasoning.isEmpty()) {
            JsonObject th = new JsonObject();
            th.addProperty("type", "thinking");
            th.addProperty("thinking", m.reasoning);
            content.add(th);
        }

        if (m.text != null && !m.text.isEmpty()) {
            JsonObject t = new JsonObject();
            t.addProperty("type", "text");
            t.addProperty("text", m.text);
            content.add(t);
        }

        if (m.images != null && !m.images.isEmpty()) {
            for (String img : m.images) {
                JsonObject im = new JsonObject();
                im.addProperty("type", "image");
                JsonObject src = new JsonObject();
                src.addProperty("type", "base64");
                String[] parts = img.split(",", 2);
                String mediaType = parts.length > 1 && parts[0].contains("image/")
                        ? parts[0].replaceAll(".*:(.*?);.*", "$1")
                        : "image/png";
                String data = parts.length > 1 ? parts[1] : img;
                src.addProperty("media_type", mediaType);
                src.addProperty("data", data);
                im.add("source", src);
                content.add(im);
            }
        }

        if (content.size() == 0) {
            // Anthropic requires non-empty content.
            JsonObject empty = new JsonObject();
            empty.addProperty("type", "text");
            empty.addProperty("text", " ");
            content.add(empty);
        }
        obj.add("content", content);
        return obj;
    }

    /** Iterator over SSE events producing ApiStreamChunk. */
    private final class IteratorImpl implements java.util.Iterator<ApiStreamChunk> {
        private final SseParser parser;
        private final Response response;
        private final InputStream stream;
        private ApiStreamChunk next;
        private boolean done;
        // Pending error to surface to the caller. When readNext() throws a
        // non-IOException (e.g. Anthropic error event), we store it here so
        // hasNext() can rethrow it instead of silently ending the stream.
        private Throwable pendingError;

        // State for assembling tool_use blocks.
        private String currentToolId;
        private String currentToolName;
        private final StringBuilder currentToolArgs = new StringBuilder();
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
                // Surface non-IO exceptions to the caller as a RuntimeException.
                // IOExceptions are expected on stream end / cancellation.
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
                // If we terminated due to an error, rethrow it here so the
                // caller's for-loop sees the exception.
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
                if (ev.event == null && ev.data == null) continue;
                String event = ev.event == null ? "data" : ev.event;
                String data = ev.data == null ? "" : ev.data;
                if (data.equals("[DONE]")) return new ApiStreamChunk.Done();

                if ("data".equals(event)) {
                    // OpenAI-style streaming (e.g. used by proxies) - parse minimal.
                    JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                    if (obj.has("choices")) {
                        // TODO: minimal OpenAI-compat handling if needed
                    }
                    continue;
                }

                JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
                switch (event) {
                    case "message_start": {
                        JsonObject msg = obj.has("message") ? obj.getAsJsonObject("message") : obj;
                        if (msg.has("usage")) {
                            JsonObject u = msg.getAsJsonObject("usage");
                            inputTokens = u.has("input_tokens") ? u.get("input_tokens").getAsInt() : 0;
                            cacheReadTokens = u.has("cache_read_input_tokens") ? u.get("cache_read_input_tokens").getAsInt() : 0;
                            cacheWriteTokens = u.has("cache_creation_input_tokens") ? u.get("cache_creation_input_tokens").getAsInt() : 0;
                        }
                        break;
                    }
                    case "content_block_start": {
                        JsonObject block = obj.has("content_block") ? obj.getAsJsonObject("content_block") : obj;
                        String type = block.has("type") ? block.get("type").getAsString() : "";
                        if ("tool_use".equals(type)) {
                            currentToolId = block.has("id") ? block.get("id").getAsString() : "tool_" + System.currentTimeMillis();
                            currentToolName = block.has("name") ? block.get("name").getAsString() : "unknown";
                            currentToolArgs.setLength(0);
                        }
                        break;
                    }
                    case "content_block_delta": {
                        JsonObject delta = obj.has("delta") ? obj.getAsJsonObject("delta") : obj;
                        String type = delta.has("type") ? delta.get("type").getAsString() : "";
                        if ("text_delta".equals(type)) {
                            String text = delta.has("text") ? delta.get("text").getAsString() : "";
                            return new ApiStreamChunk.Text(text);
                        } else if ("thinking_delta".equals(type)) {
                            String text = delta.has("thinking") ? delta.get("thinking").getAsString() : "";
                            return new ApiStreamChunk.Reasoning(text);
                        } else if ("input_json_delta".equals(type)) {
                            String partial = delta.has("partial_json") ? delta.get("partial_json").getAsString() : "";
                            currentToolArgs.append(partial);
                        }
                        break;
                    }
                    case "content_block_stop": {
                        if (currentToolId != null) {
                            List<AgentMessage.ToolCall> calls = new ArrayList<>(1);
                            String argsJson = currentToolArgs.toString();
                            if (argsJson.isEmpty()) argsJson = "{}";
                            calls.add(new AgentMessage.ToolCall(currentToolId, currentToolName, argsJson));
                            currentToolId = null;
                            currentToolName = null;
                            return new ApiStreamChunk.ToolCalls(calls);
                        }
                        break;
                    }
                    case "message_delta": {
                        if (obj.has("usage")) {
                            JsonObject u = obj.getAsJsonObject("usage");
                            outputTokens = u.has("output_tokens") ? u.get("output_tokens").getAsInt() : outputTokens;
                        }
                        break;
                    }
                    case "message_stop": {
                        // Emit usage first (if not yet emitted), then Done on
                        // the next call. This keeps the chunk sequence
                        // consistent with the other providers (Usage → Done).
                        if (!usageEmitted) {
                            usageEmitted = true;
                            double cost = computeCost();
                            return new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                                    cacheReadTokens, cacheWriteTokens, cost);
                        }
                        doneEmitted = true;
                        return new ApiStreamChunk.Done();
                    }
                    case "error": {
                        JsonObject err = obj.has("error") ? obj.getAsJsonObject("error") : obj;
                        String msg = err.has("message") ? err.get("message").getAsString() : data;
                        throw new RuntimeException("Anthropic error: " + msg);
                    }
                }
            }
            // End of stream: emit Usage then Done if not already emitted.
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

        private double computeCost() {
            if (inputTokens == 0 && outputTokens == 0) return 0;
            return (inputTokens * 3.0 + outputTokens * 15.0 + cacheReadTokens * 0.30 + cacheWriteTokens * 3.75) / 1_000_000.0;
        }

        private void closeQuietly() {
            try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            try { if (response != null) response.close(); } catch (Exception ignored) {}
        }
    }
}
