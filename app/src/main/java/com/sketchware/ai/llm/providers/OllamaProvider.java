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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Ollama local provider.
 *
 * <p>Endpoint: {@code POST /api/chat} (NDJSON streaming, one JSON object per line).
 *
 * <p>Wire shape:
 * <pre>{@code
 * { "model": "...", "message": { "role": "assistant", "content": "..." }, "done": false }
 * }</pre>
 */
public final class OllamaProvider implements LlmProvider {

    @Override public String getProviderId() { return "ollama"; }

    @Override public ModelInfo getModel(String modelId) {
        return new ModelInfo(modelId, "Ollama " + modelId,
                8192, 8192, 4096,
                true, true, false,
                0, 0, 0, 0);
    }

    @Override public Iterable<ApiStreamChunk> stream(LlmRequest request) throws Exception {
        String baseUrl = request.baseUrl == null || request.baseUrl.isEmpty()
                ? "http://localhost:11434"
                : request.baseUrl;
        String url = baseUrl.replaceAll("/+$", "") + "/api/chat";

        JsonObject body = buildRequestBody(request);
        Request.Builder rb = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json");
        if (request.extraHeaders != null) {
            for (LlmRequest.ExtraHeader h : request.extraHeaders) rb.header(h.name, h.value);
        }
        rb.post(RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")));
        Response response = com.sketchware.ai.llm.http.HttpClient.postStream(rb);
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            response.close();
            throw new RuntimeException("Ollama HTTP " + response.code() + ": " + errBody);
        }

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        return () -> new IteratorImpl(reader, response, in);
    }

    @Override public void abort() {
        com.sketchware.ai.llm.http.HttpClient.abortCurrent();
    }

    private JsonObject buildRequestBody(LlmRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.model.id);
        root.addProperty("stream", true);

        JsonArray messages = new JsonArray();
        if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", request.systemPrompt);
            messages.add(sys);
        }
        for (AgentMessage m : request.messages) {
            if (AgentMessage.ROLE_SYSTEM.equals(m.role)) continue;
            // Tool-result messages must use role="tool" so Ollama knows they
            // are outputs of a previous tool invocation, not user text.
            String role;
            if (m.hasToolResults()) {
                role = "tool";
            } else if ("assistant".equals(m.role)) {
                role = "assistant";
            } else {
                role = "user";
            }
            JsonObject msg = new JsonObject();
            msg.addProperty("role", role);
            StringBuilder content = new StringBuilder();
            if (m.text != null) content.append(m.text);
            // For tool-result messages, put the result output into `content`
            // (Ollama's /api/chat expects the tool result text there).
            if (m.hasToolResults()) {
                for (AgentMessage.ToolResultContent r : m.toolResults) {
                    if (content.length() > 0) content.append("\n");
                    content.append(r.isError ? ("ERROR: " + r.output) : r.output);
                }
            }
            msg.addProperty("content", content.toString());
            if (m.images != null && !m.images.isEmpty()) {
                JsonArray imgs = new JsonArray();
                for (String img : m.images) {
                    String[] split = img.split(",", 2);
                    String data = split.length > 1 ? split[1] : img;
                    imgs.add(data);
                }
                msg.add("images", imgs);
            }
            // Assistant messages with tool_calls must include the `tool_calls`
            // field — otherwise Ollama has no way to know what tool was called
            // in the previous turn, and the conversation history is broken.
            if ("assistant".equals(role) && m.toolCalls != null && !m.toolCalls.isEmpty()) {
                JsonArray tcs = new JsonArray();
                for (AgentMessage.ToolCall tc : m.toolCalls) {
                    JsonObject tcObj = new JsonObject();
                    JsonObject fn = new JsonObject();
                    fn.addProperty("name", tc.name);
                    fn.addProperty("arguments", tc.argumentsJson == null ? "{}" : tc.argumentsJson);
                    tcObj.add("function", fn);
                    tcs.add(tcObj);
                }
                msg.add("tool_calls", tcs);
            }
            messages.add(msg);
        }
        root.add("messages", messages);

        JsonObject options = new JsonObject();
        if (request.maxTokens > 0) options.addProperty("num_predict", request.maxTokens);
        if (request.model != null && request.model.contextWindow > 0) {
            options.addProperty("num_ctx", request.model.contextWindow);
        }
        if (options.size() > 0) root.add("options", options);

        if (request.toolsJson != null && !request.toolsJson.isEmpty() && !"[]".equals(request.toolsJson)) {
            JsonArray tools = JsonParser.parseString(request.toolsJson).getAsJsonArray();
            JsonArray mapped = new JsonArray();
            for (JsonElement t : tools) {
                JsonObject tool = t.getAsJsonObject();
                JsonObject fn = new JsonObject();
                fn.addProperty("type", "function");
                JsonObject function = new JsonObject();
                function.addProperty("name", tool.get("name").getAsString());
                function.addProperty("description", tool.get("description").getAsString());
                JsonObject schema = tool.has("inputSchema") ? tool.getAsJsonObject("inputSchema") : new JsonObject();
                function.add("parameters", schema);
                fn.add("function", function);
                mapped.add(fn);
            }
            root.add("tools", mapped);
        }

        return root;
    }

    private final class IteratorImpl implements java.util.Iterator<ApiStreamChunk> {
        private final BufferedReader reader;
        private final Response response;
        private final InputStream stream;
        private ApiStreamChunk next;
        private boolean done;
        private Throwable pendingError;
        private boolean usageEmitted = false;
        private boolean doneEmitted = false;
        // Monotonic counter for synthesizing tool-call ids (Ollama doesn't
        // always return one). Using a counter avoids collisions when multiple
        // tool calls arrive in the same NDJSON line.
        private int toolIdCounter = 0;
        private int inputTokens = 0, outputTokens = 0;

        IteratorImpl(BufferedReader reader, Response response, InputStream stream) {
            this.reader = reader;
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
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                if (obj.has("error")) {
                    String msg = obj.get("error").isJsonPrimitive()
                            ? obj.get("error").getAsString()
                            : obj.toString();
                    throw new RuntimeException("Ollama error: " + msg);
                }
                if (obj.has("message")) {
                    JsonObject msg = obj.getAsJsonObject("message");
                    if (msg.has("content")) {
                        JsonElement c = msg.get("content");
                        if (c != null && !c.isJsonNull() && c.isJsonPrimitive()) {
                            String text = c.getAsString();
                            if (!text.isEmpty()) return new ApiStreamChunk.Text(text);
                        }
                    }
                }
                if (obj.has("tool_calls")) {
                    JsonArray arr = obj.getAsJsonArray("tool_calls");
                    List<AgentMessage.ToolCall> calls = new ArrayList<>();
                    for (JsonElement e : arr) {
                        JsonObject tc = e.getAsJsonObject();
                        JsonObject function = tc.has("function") ? tc.getAsJsonObject("function") : tc;
                        String id = tc.has("id") && !tc.get("id").isJsonNull()
                                ? tc.get("id").getAsString()
                                : "tool_" + (toolIdCounter++);
                        String name = function.has("name") ? function.get("name").getAsString() : "unknown";
                        String args = function.has("arguments") && !function.get("arguments").isJsonNull()
                                ? function.get("arguments").getAsString()
                                : "{}";
                        calls.add(new AgentMessage.ToolCall(id, name, args));
                    }
                    if (!calls.isEmpty()) return new ApiStreamChunk.ToolCalls(calls);
                }
                if (obj.has("done") && obj.get("done").getAsBoolean()) {
                    if (obj.has("prompt_eval_count")) {
                        inputTokens = obj.get("prompt_eval_count").getAsInt();
                        outputTokens = obj.has("eval_count") ? obj.get("eval_count").getAsInt() : 0;
                    }
                    if (!usageEmitted) {
                        usageEmitted = true;
                        return new ApiStreamChunk.Usage(inputTokens, outputTokens, 0, 0, 0, 0.0);
                    }
                    if (!doneEmitted) {
                        doneEmitted = true;
                        return new ApiStreamChunk.Done();
                    }
                    return null;
                }
            }
            // Stream ended without an explicit done:true. Emit Usage (if we
            // have stats) then Done so the consumer sees a clean end.
            if (!usageEmitted && (inputTokens > 0 || outputTokens > 0)) {
                usageEmitted = true;
                return new ApiStreamChunk.Usage(inputTokens, outputTokens, 0, 0, 0, 0.0);
            }
            if (!doneEmitted) {
                doneEmitted = true;
                return new ApiStreamChunk.Done();
            }
            return null;
        }

        private void closeQuietly() {
            try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            try { if (response != null) response.close(); } catch (Exception ignored) {}
        }
    }
}
