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
 * Google Gemini provider.
 *
 * <p>Endpoint:
 * {@code POST /v1beta/models/{model}:streamGenerateContent?alt=sse&key=API_KEY}
 *
 * <p>SSE: each event has {@code data: {...}} JSON; each chunk contains
 * {@code candidates[0].content.parts[]} with {@code text} or {@code functionCall} parts.
 */
public final class GeminiProvider implements LlmProvider {

    @Override public String getProviderId() { return "gemini"; }

    @Override public ModelInfo getModel(String modelId) {
        switch (modelId) {
            case "gemini-2.0-flash":
            case "gemini-2.0-flash-001":
                return new ModelInfo(modelId, "Gemini 2.0 Flash",
                        1_048_576, 1_048_576, 8_192,
                        true, true, false,
                        0.10, 0.40, 0.025, 0.10);
            case "gemini-2.0-flash-thinking-exp":
            case "gemini-2.0-flash-thinking-exp-01-21":
                return new ModelInfo(modelId, "Gemini 2.0 Flash Thinking",
                        1_048_576, 1_048_576, 8_192,
                        true, true, true,
                        0.15, 0.60, 0.0375, 0.15);
            case "gemini-1.5-pro":
            case "gemini-1.5-pro-latest":
                return new ModelInfo(modelId, "Gemini 1.5 Pro",
                        2_097_152, 2_097_152, 8_192,
                        true, true, false,
                        1.25, 5.0, 0.3125, 1.25);
            case "gemini-1.5-flash":
            case "gemini-1.5-flash-latest":
                return new ModelInfo(modelId, "Gemini 1.5 Flash",
                        1_048_576, 1_048_576, 8_192,
                        true, true, false,
                        0.075, 0.30, 0.0188, 0.075);
            default:
                return ModelInfo.defaultFor(modelId);
        }
    }

    @Override public Iterable<ApiStreamChunk> stream(LlmRequest request) throws Exception {
        String baseUrl = request.baseUrl == null || request.baseUrl.isEmpty()
                ? "https://generativelanguage.googleapis.com"
                : request.baseUrl;
        // Strip trailing slashes and any already-included /v1beta prefix so
        // we don't double-append it when the user configured the base URL with
        // the version segment included.
        String base = baseUrl.replaceAll("/+$", "");
        if (base.endsWith("/v1beta")) {
            base = base.substring(0, base.length() - "/v1beta".length());
        } else if (base.endsWith("/v1")) {
            // Tolerate /v1 (older endpoint alias) — strip and use /v1beta below.
            base = base.substring(0, base.length() - "/v1".length());
        }
        String url = base + "/v1beta/models/" + request.model.id
                + ":streamGenerateContent?alt=sse&key=" + (request.apiKey == null ? "" : request.apiKey);

        JsonObject body = buildRequestBody(request);
        Response response = HttpClient.postStream(url, body.toString(), null, request.extraHeaders);
        if (!response.isSuccessful()) {
            String errBody = response.body() != null ? response.body().string() : "";
            int code = response.code();
            response.close();
            throw new RuntimeException("Gemini HTTP " + code + ": " + errBody);
        }

        final InputStream in = response.body() != null ? response.body().byteStream() : new java.io.ByteArrayInputStream(new byte[0]);
        final SseParser parser = new SseParser(in);
        return () -> new IteratorImpl(parser, response, in);
    }

    @Override public void abort() {
        com.sketchware.ai.llm.http.HttpClient.abortCurrent();
    }

    private JsonObject buildRequestBody(LlmRequest request) {
        JsonObject root = new JsonObject();

        // contents
        JsonArray contents = new JsonArray();
        if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "user");
            JsonArray parts = new JsonArray();
            JsonObject t = new JsonObject();
            t.addProperty("text", request.systemPrompt);
            parts.add(t);
            sys.add("parts", parts);
            contents.add(sys);
        }
        for (AgentMessage m : request.messages) {
            if (AgentMessage.ROLE_SYSTEM.equals(m.role)) continue;
            contents.add(toGeminiMessage(m));
        }
        root.add("contents", contents);

        // generation config
        JsonObject config = new JsonObject();
        if (request.maxTokens > 0) config.addProperty("maxOutputTokens", request.maxTokens);
        if (request.reasoning != null && request.reasoning.budgetTokens != null
                && request.model != null && request.model.supportsReasoning) {
            JsonObject thinkingConfig = new JsonObject();
            thinkingConfig.addProperty("thinkingBudget", request.reasoning.budgetTokens);
            thinkingConfig.addProperty("includeThoughts", true);
            config.add("thinkingConfig", thinkingConfig);
        }
        if (config.size() > 0) root.add("generationConfig", config);

        // tools
        if (request.toolsJson != null && !request.toolsJson.isEmpty() && !"[]".equals(request.toolsJson)) {
            JsonArray tools = JsonParser.parseString(request.toolsJson).getAsJsonArray();
            JsonArray declarations = new JsonArray();
            for (JsonElement t : tools) {
                JsonObject tool = t.getAsJsonObject();
                JsonObject decl = new JsonObject();
                decl.addProperty("name", tool.get("name").getAsString());
                decl.addProperty("description", tool.get("description").getAsString());
                JsonObject schema = tool.has("inputSchema") ? tool.getAsJsonObject("inputSchema") : new JsonObject();
                decl.add("parameters", schema);
                declarations.add(decl);
            }
            JsonObject toolEntry = new JsonObject();
            toolEntry.add("functionDeclarations", declarations);
            JsonArray toolsArr = new JsonArray();
            toolsArr.add(toolEntry);
            root.add("tools", toolsArr);
        }

        return root;
    }

    private JsonObject toGeminiMessage(AgentMessage m) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", "assistant".equals(m.role) ? "model" : "user");
        JsonArray parts = new JsonArray();

        if (m.hasToolResults()) {
            for (AgentMessage.ToolResultContent r : m.toolResults) {
                JsonObject resp = new JsonObject();
                JsonObject fr = new JsonObject();
                fr.addProperty("name", r.toolName);
                JsonObject response = new JsonObject();
                JsonObject content = new JsonObject();
                content.addProperty("output", r.isError ? ("ERROR: " + r.output) : r.output);
                response.add("content", content);
                fr.add("response", response);
                resp.add("functionResponse", fr);
                parts.add(resp);
            }
        }

        if (m.toolCalls != null) {
            for (AgentMessage.ToolCall tc : m.toolCalls) {
                JsonObject fc = new JsonObject();
                JsonObject call = new JsonObject();
                call.addProperty("name", tc.name);
                JsonObject args = tc.argumentsJson == null || tc.argumentsJson.isEmpty()
                        ? new JsonObject()
                        : JsonParser.parseString(tc.argumentsJson).getAsJsonObject();
                call.add("args", args);
                fc.add("functionCall", call);
                parts.add(fc);
            }
        }

        if (m.text != null && !m.text.isEmpty()) {
            JsonObject t = new JsonObject();
            t.addProperty("text", m.text);
            parts.add(t);
        }

        if (m.images != null && !m.images.isEmpty()) {
            for (String img : m.images) {
                JsonObject im = new JsonObject();
                JsonObject inline = new JsonObject();
                String[] split = img.split(",", 2);
                String mime = split.length > 1 && split[0].contains("image/")
                        ? split[0].replaceAll(".*:(.*?);.*", "$1")
                        : "image/png";
                String data = split.length > 1 ? split[1] : img;
                inline.addProperty("mimeType", mime);
                inline.addProperty("data", data);
                im.add("inlineData", inline);
                parts.add(im);
            }
        }

        if (parts.size() == 0) {
            JsonObject t = new JsonObject();
            t.addProperty("text", " ");
            parts.add(t);
        }
        obj.add("parts", parts);
        return obj;
    }

    private final class IteratorImpl implements java.util.Iterator<ApiStreamChunk> {
        private final SseParser parser;
        private final Response response;
        private final InputStream stream;
        private ApiStreamChunk next;
        private boolean done;
        // Pending error to surface to the caller. When readNext() throws a
        // non-IOException (e.g. Gemini error event), we store it here so
        // hasNext() can rethrow it instead of silently ending the stream.
        private Throwable pendingError;
        // FIFO queue of chunks decoded from the current SSE event but not
        // yet returned to the caller. A single Gemini SSE event can carry
        // multiple parts (text + functionCall + thoughtText), so we buffer
        // them and return one per readNext() call.
        private final java.util.ArrayDeque<ApiStreamChunk> pending = new java.util.ArrayDeque<>();
        private int inputTokens = 0, outputTokens = 0, reasoningTokens = 0;
        private int cacheReadTokens = 0, cacheWriteTokens = 0;
        private boolean usageEmitted = false;
        private boolean doneEmitted = false;
        // Monotonic counter for synthesizing tool-call ids when Gemini's
        // response doesn't include one (which is the common case). Using a
        // counter instead of System.currentTimeMillis() guarantees
        // uniqueness across multiple tool calls in the same chunk.
        private int toolIdCounter = 0;

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
            // Drain any buffered chunks first.
            if (!pending.isEmpty()) return pending.poll();
            for (SseEvent ev : parser) {
                if (ev.data == null || ev.data.isEmpty() || "[DONE]".equals(ev.data)) {
                    if (!usageEmitted) {
                        usageEmitted = true;
                        return new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                                cacheReadTokens, cacheWriteTokens, 0.0);
                    }
                    if (!doneEmitted) {
                        doneEmitted = true;
                        return new ApiStreamChunk.Done();
                    }
                    return null;
                }
                JsonObject obj = JsonParser.parseString(ev.data).getAsJsonObject();

                // Process usageMetadata FIRST so we don't lose it when the
                // same chunk also carries content.
                if (obj.has("usageMetadata")) {
                    JsonObject u = obj.getAsJsonObject("usageMetadata");
                    if (u.has("promptTokenCount")) inputTokens = u.get("promptTokenCount").getAsInt();
                    if (u.has("candidatesTokenCount")) outputTokens = u.get("candidatesTokenCount").getAsInt();
                    if (u.has("thoughtsTokenCount")) reasoningTokens = u.get("thoughtsTokenCount").getAsInt();
                    if (u.has("cachedContentTokenCount")) cacheReadTokens = u.get("cachedContentTokenCount").getAsInt();
                }

                // Gemini error responses are sometimes wrapped in `error`.
                if (obj.has("error")) {
                    JsonObject err = obj.getAsJsonObject("error");
                    String msg = err.has("message") ? err.get("message").getAsString() : ev.data;
                    throw new RuntimeException("Gemini error: " + msg);
                }

                if (obj.has("candidates")) {
                    JsonArray cands = obj.getAsJsonArray("candidates");
                    if (cands.size() == 0) continue;
                    JsonObject cand = cands.get(0).getAsJsonObject();
                    // Check finishReason for safety/error states.
                    if (cand.has("finishReason")) {
                        String fr = cand.get("finishReason").getAsString();
                        if ("SAFETY".equals(fr) || "RECITATION".equals(fr)
                                || "BLOCKLIST".equals(fr) || "PROHIBITED_CONTENT".equals(fr)) {
                            // Buffer a warning text so the caller sees something.
                            pending.add(new ApiStreamChunk.Text("[blocked: " + fr + "]"));
                        }
                    }
                    if (cand.has("content")) {
                        JsonObject content = cand.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            List<AgentMessage.ToolCall> calls = new ArrayList<>();
                            for (JsonElement p : parts) {
                                JsonObject part = p.getAsJsonObject();
                                if (part.has("text")) {
                                    String text = part.get("text").getAsString();
                                    if (!text.isEmpty()) {
                                        pending.add(new ApiStreamChunk.Text(text));
                                    }
                                } else if (part.has("thoughtText")) {
                                    String text = part.get("thoughtText").getAsString();
                                    if (!text.isEmpty()) {
                                        pending.add(new ApiStreamChunk.Reasoning(text));
                                    }
                                } else if (part.has("thought")) {
                                    // Variant: some SDKs use `thought` for the
                                    // reasoning text instead of `thoughtText`.
                                    String text = part.get("thought").getAsString();
                                    if (!text.isEmpty()) {
                                        pending.add(new ApiStreamChunk.Reasoning(text));
                                    }
                                } else if (part.has("functionCall")) {
                                    JsonObject fc = part.getAsJsonObject("functionCall");
                                    String id = fc.has("id") && !fc.get("id").isJsonNull()
                                            ? fc.get("id").getAsString()
                                            : "tool_" + (toolIdCounter++);
                                    String name = fc.has("name") ? fc.get("name").getAsString() : "unknown";
                                    JsonObject args = fc.has("args") && fc.get("args").isJsonObject()
                                            ? fc.getAsJsonObject("args") : new JsonObject();
                                    calls.add(new AgentMessage.ToolCall(id, name, args.toString()));
                                }
                            }
                            if (!calls.isEmpty()) {
                                pending.add(new ApiStreamChunk.ToolCalls(calls));
                            }
                        }
                    }
                }
                // If we buffered any chunks this iteration, return the first.
                if (!pending.isEmpty()) return pending.poll();
            }
            // End of stream.
            if (!usageEmitted) {
                usageEmitted = true;
                return new ApiStreamChunk.Usage(inputTokens, outputTokens, reasoningTokens,
                        cacheReadTokens, cacheWriteTokens, 0.0);
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
