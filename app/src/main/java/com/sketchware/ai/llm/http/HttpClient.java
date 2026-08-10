package com.sketchware.ai.llm.http;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Thin wrapper around OkHttp for streaming POST requests with SSE.
 * Reuses the OkHttpClient already available in Sketchware-Pro's dependencies.
 */
public final class HttpClient {

    private static OkHttpClient sharedClient;

    /** A client with long read timeouts (LLM streams can be slow). */
    public static synchronized OkHttpClient getClient() {
        if (sharedClient == null) {
            sharedClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
        return sharedClient;
    }

    /**
     * Execute a streaming POST and return the raw Response.
     * Caller is responsible for closing the response body.
     */
    public static Response postStream(String url,
                                      String jsonBody,
                                      String apiKey,
                                      java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders)
            throws Exception {
        Request.Builder rb = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream");
        if (apiKey != null && !apiKey.isEmpty()) {
            rb.header("Authorization", "Bearer " + apiKey);
        }
        if (extraHeaders != null) {
            for (com.sketchware.ai.llm.LlmRequest.ExtraHeader h : extraHeaders) {
                rb.header(h.name, h.value);
            }
        }
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        rb.post(body);
        return getClient().newCall(rb.build()).execute();
    }

    /**
     * Execute a non-streaming POST and return the response body as a string.
     */
    public static String postJson(String url,
                                  String jsonBody,
                                  String apiKey,
                                  java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders)
            throws Exception {
        try (Response resp = postStream(url, jsonBody, apiKey, extraHeaders)) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                throw new RuntimeException("HTTP " + resp.code() + ": " + errBody);
            }
            return resp.body() != null ? resp.body().string() : "";
        }
    }
}
