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
        Response response = com.sketchware.ai.llm.http.HttpClient.getClient().newCall(rb.build()).execute();
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            response.close();
            throw new RuntimeException("Ollama HTTP " + response.code() + ": " + errBody);
        }

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        return () -> new IteratorImpl(reader, response, in);
    }

    @Override public void abort() {}

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
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user".equals(m.role) ? "user" : "assistant");
            StringBuilder content = new StringBuilder();
            if (m.text != null) content.append(m.text);
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
            if (!hasNext()) throw new java.util.NoSuchElementException();
            ApiStreamChunk result = next;
            next = null;
            return result;
        }

        private ApiStreamChunk readNext() throws Exception {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                if (obj.has("message")) {
                    JsonObject msg = obj.getAsJsonObject("message");
                    if (msg.has("content")) {
                        String text = msg.get("content").getAsString();
                        if (!text.isEmpty()) return new ApiStreamChunk.Text(text);
                    }
                }
                if (obj.has("tool_calls")) {
                    JsonArray arr = obj.getAsJsonArray("tool_calls");
                    List<AgentMessage.ToolCall> calls = new ArrayList<>();
                    for (JsonElement e : arr) {
                        JsonObject tc = e.getAsJsonObject();
                        JsonObject function = tc.has("function") ? tc.getAsJsonObject("function") : tc;
                        String id = tc.has("id") ? tc.get("id").getAsString() : "tool_" + System.currentTimeMillis();
                        String name = function.has("name") ? function.get("name").getAsString() : "unknown";
                        String args = function.has("arguments")
                                ? function.get("arguments").getAsString()
                                : "{}";
                        calls.add(new AgentMessage.ToolCall(id, name, args));
                    }
                    if (!calls.isEmpty()) return new ApiStreamChunk.ToolCalls(calls);
                }
                if (obj.has("done") && obj.get("done").getAsBoolean()) {
                    if (obj.has("prompt_eval_count")) {
                        int in = obj.get("prompt_eval_count").getAsInt();
                        int out = obj.has("eval_count") ? obj.get("eval_count").getAsInt() : 0;
                        return new ApiStreamChunk.Usage(in, out, 0, 0, 0, 0.0);
                    }
                    return new ApiStreamChunk.Done();
                }
            }
            return null;
        }

        private void closeQuietly() {
            try { if (stream != null) stream.close(); } catch (Exception ignored) {}
            try { if (response != null) response.close(); } catch (Exception ignored) {}
        }
    }
}
