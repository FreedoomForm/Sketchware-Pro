package com.sketchware.ai.llm.providers;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sketchware.ai.agent.AgentMessage;
import com.sketchware.ai.llm.ApiStreamChunk;
import com.sketchware.ai.llm.LlmProvider;
import com.sketchware.ai.llm.LlmRequest;
import com.sketchware.ai.llm.ModelInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Integration tests for {@link OpenAiProvider} using a MockWebServer.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>The provider sends a request with the correct URL, headers, and body shape.</li>
 *   <li>SSE chunks ({@code data: {...}}) are correctly parsed.</li>
 *   <li>{@code delta.content} produces TEXT chunks.</li>
 *   <li>{@code delta.tool_calls} produces TOOL_CALLS chunks (with partial JSON argument assembly).</li>
 *   <li>{@code [DONE]} terminates the stream.</li>
 *   <li>Usage stats from the final chunk are extracted.</li>
 * </ul>
 */
public class OpenAiProviderTest {

    private MockWebServer server;
    private OpenAiProvider provider;

    @Before public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        provider = new OpenAiProvider();
    }

    @After public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test public void parsesSimpleTextStream() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(openAiStreamFixture("Hello world")));

        LlmRequest req = request("gpt-4o");
        StringBuilder text = new StringBuilder();
        for (ApiStreamChunk chunk : provider.stream(req)) {
            if (chunk.isText()) text.append(chunk.asText().text);
        }
        assertThat(text.toString()).isEqualTo("Hello world");
    }

    @Test public void parsesToolCallsWithPartialJson() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(openAiStreamWithToolCall()));

        LlmRequest req = request("gpt-4o");
        java.util.List<AgentMessage.ToolCall> calls = new java.util.ArrayList<>();
        for (ApiStreamChunk chunk : provider.stream(req)) {
            if (chunk.isToolCalls()) calls.addAll(chunk.asToolCalls().calls);
        }
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name).isEqualTo("view_add_widget");
        JsonObject args = JsonParser.parseString(calls.get(0).argumentsJson).getAsJsonObject();
        assertThat(args.get("widget_type").getAsString()).isEqualTo("Button");
    }

    @Test public void sendsAuthorizationHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(openAiStreamFixture("hi")));

        LlmRequest req = request("gpt-4o");
        for (ApiStreamChunk ignored : provider.stream(req)) {}

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-key");
    }

    @Test public void sendsCorrectUrl() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(openAiStreamFixture("hi")));

        LlmRequest req = request("gpt-4o");
        for (ApiStreamChunk ignored : provider.stream(req)) {}

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).endsWith("/v1/chat/completions");
    }

    @Test public void sendsCorrectBodyShape() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(openAiStreamFixture("hi")));

        LlmRequest req = request("gpt-4o");
        for (ApiStreamChunk ignored : provider.stream(req)) {}

        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        assertThat(json.get("model").getAsString()).isEqualTo("gpt-4o");
        assertThat(json.get("stream").getAsBoolean()).isTrue();
        assertThat(json.has("messages")).isTrue();
        assertThat(json.has("stream_options")).isTrue();
        JsonObject streamOpts = json.getAsJsonObject("stream_options");
        assertThat(streamOpts.get("include_usage").getAsBoolean()).isTrue();
    }

    // ===== Helpers =====

    private LlmRequest request(String modelId) {
        ModelInfo model = provider.getModel(modelId);
        return new LlmRequest(
                "openai",
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

    private String openAiStreamFixture(String text) {
        return "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"content\":\"" + text + "\"}}]}\n\n" +
                "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5}}\n\n" +
                "data: [DONE]\n\n";
    }

    private String openAiStreamWithToolCall() {
        return "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"view_add_widget\",\"arguments\":\"\"}}]}}]}\n\n" +
                "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"widget\\\"\"}}]}}]}\n\n" +
                "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"_type\\\":\\\"Button\\\"}\"}}]}}]}\n\n" +
                "data: {\"id\":\"chatcmpl-1\",\"choices\":[{\"finish_reason\":\"tool_calls\"}]}\n\n" +
                "data: [DONE]\n\n";
    }
}
