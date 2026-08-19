package com.sketchware.ai.llm.providers;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

import java.io.IOException;
import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Integration tests for {@link GeminiProvider} using a MockWebServer.
 */
public class GeminiProviderTest {

    private MockWebServer server;
    private GeminiProvider provider;

    @Before public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        provider = new GeminiProvider();
    }

    @After public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test public void parsesSimpleTextStream() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(geminiStreamFixture("Hello from Gemini")));

        LlmRequest req = request("gemini-2.0-flash");
        StringBuilder text = new StringBuilder();
        for (ApiStreamChunk chunk : provider.stream(req)) {
            if (chunk.isText()) text.append(chunk.asText().text);
        }
        assertThat(text.toString()).isEqualTo("Hello from Gemini");
    }

    @Test public void parsesFunctionCall() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(geminiStreamWithFunctionCall()));

        LlmRequest req = request("gemini-2.0-flash");
        java.util.List<AgentMessage.ToolCall> calls = new java.util.ArrayList<>();
        for (ApiStreamChunk chunk : provider.stream(req)) {
            if (chunk.isToolCalls()) calls.addAll(chunk.asToolCalls().calls);
        }
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name).isEqualTo("view_add_widget");
        JsonObject args = JsonParser.parseString(calls.get(0).argumentsJson).getAsJsonObject();
        assertThat(args.get("widget_type").getAsString()).isEqualTo("Button");
    }

    @Test public void sendsApiKeyInSecretSafeHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(geminiStreamFixture("hi")));

        LlmRequest req = request("gemini-2.0-flash");
        for (ApiStreamChunk ignored : provider.stream(req)) {}

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).contains("alt=sse");
        assertThat(recorded.getPath()).contains("gemini-2.0-flash");
        assertThat(recorded.getPath()).doesNotContain("key=test-key");
        assertThat(recorded.getHeader("x-goog-api-key")).isEqualTo("test-key");
    }

    @Test public void sendsCorrectBodyShape() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(geminiStreamFixture("hi")));

        LlmRequest req = request("gemini-2.0-flash");
        for (ApiStreamChunk ignored : provider.stream(req)) {}

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        assertThat(json.has("contents")).isTrue();
        assertThat(json.has("generationConfig")).isTrue();
    }

    // ===== Helpers =====

    private LlmRequest request(String modelId) {
        ModelInfo model = provider.getModel(modelId);
        return new LlmRequest(
                "gemini",
                server.url("/").toString().replaceAll("/$", ""),
                "test-key",
                model,
                "You are a helpful assistant.",
                Collections.singletonList(AgentMessage.user("hi")),
                "[]",
                null,
                4096,
                true,
                null);
    }

    private String geminiStreamFixture(String text) {
        return "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + text + "\"}]}}]}\n\n" +
                "data: {\"usageMetadata\":{\"promptTokenCount\":10,\"candidatesTokenCount\":5}}\n\n";
    }

    private String geminiStreamWithFunctionCall() {
        return "data: {\"candidates\":[{\"content\":{\"parts\":[{\"functionCall\":{\"name\":\"view_add_widget\",\"args\":{\"widget_type\":\"Button\"}}}]}}]}\n\n" +
                "data: {\"usageMetadata\":{\"promptTokenCount\":20,\"candidatesTokenCount\":10}}\n\n";
    }
}
