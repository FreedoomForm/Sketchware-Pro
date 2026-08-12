package com.sketchware.ai.llm.http;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
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
    // Track the most recently started Call so providers can cancel it via
    // abort(). This is sufficient for Sketchware-Pro's single-request-at-a-time
    // usage model (the agent runtime issues one LLM stream at a time).
    private static final AtomicReference<Call> lastCall = new AtomicReference<>();
    // Track the thread currently sleeping inside postStreamWithRetry's retry
    // backoff. OkHttp's Call.cancel() only interrupts threads *inside*
    // Call.execute(); once we've returned the 429/5xx response and are sleeping
    // in Thread.sleep() between retries, Call.cancel() is a no-op. To make the
    // user's Stop button actually abort the backoff, abortCurrent() interrupts
    // this thread directly.
    private static final AtomicReference<Thread> sleepingThread = new AtomicReference<>();

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
     *
     * @deprecated prefer {@link #postStreamWithRetry} which handles HTTP 429
     *     and 5xx responses with exponential backoff.
     */
    @Deprecated
    public static Response postStream(String url,
                                      String jsonBody,
                                      String apiKey,
                                      java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders)
            throws Exception {
        return postStream(url, jsonBody, apiKey, extraHeaders, true);
    }

    /**
     * Execute a POST and return the raw Response. When {@code sse} is true,
     * sends {@code Accept: text/event-stream} (for streaming requests). When
     * false, sends {@code Accept: application/json} (for non-streaming
     * requests — used by the compactor which sets {@code stream:false}).
     */
    public static Response postStream(String url,
                                      String jsonBody,
                                      String apiKey,
                                      java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders,
                                      boolean sse)
            throws Exception {
        Request.Builder rb = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", sse ? "text/event-stream" : "application/json");
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
        Call call = getClient().newCall(rb.build());
        // Remember this call so abort() can cancel it. Replace any previous
        // call (which by contract should already be done).
        lastCall.set(call);
        return call.execute();
    }

    /**
     * Execute a POST with automatic retry on HTTP 429 (rate limited) and 5xx
     * (server error) responses. Honors the {@code Retry-After} header when
     * present, otherwise uses exponential backoff with jitter.
     *
     * <p>Mirrors Cline's retry logic in {@code core/api/index.ts}: up to
     * {@link RateLimitHandler#MAX_RETRIES} retries, capped backoff of
     * {@link RateLimitHandler#MAX_BACKOFF_MS}, random jitter to avoid
     * thundering-herd. The retry loop is interruptible — calling
     * {@code Thread.interrupt()} (which OkHttp does on {@code Call.cancel()})
     * aborts the sleep immediately.
     *
     * @param url          endpoint URL
     * @param jsonBody     request body JSON
     * @param apiKey       Bearer token (or null for none)
     * @param extraHeaders extra headers to add
     * @param sse          true for SSE streaming, false for plain JSON
     * @return the successful response (caller must close)
     * @throws RateLimitExceededException if all retries are exhausted on 429
     * @throws ServerErrorException       if all retries are exhausted on 5xx
     * @throws RuntimeException           for non-retryable HTTP errors (4xx other than 429)
     */
    public static Response postStreamWithRetry(String url,
                                               String jsonBody,
                                               String apiKey,
                                               java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders,
                                               boolean sse)
            throws Exception {
        Response lastResponse = null;
        int lastCode = 0;
        String lastErrBody = "";

        for (int attempt = 0; attempt <= RateLimitHandler.MAX_RETRIES; attempt++) {
            // Close any previous response before retrying.
            if (lastResponse != null) {
                try { lastResponse.close(); } catch (Exception ignored) {}
            }

            lastResponse = postStream(url, jsonBody, apiKey, extraHeaders, sse);
            lastCode = lastResponse.code();

            if (lastResponse.isSuccessful()) {
                return lastResponse;
            }

            // Non-retryable: 4xx other than 429 — close and throw immediately.
            if (!RateLimitHandler.isRetryable(lastCode)) {
                lastErrBody = lastResponse.body() != null ? lastResponse.body().string() : "";
                lastResponse.close();
                throw new RuntimeException("HTTP " + lastCode + ": " + lastErrBody);
            }

            // Retryable (429 or 5xx). Decide whether to retry or give up.
            if (attempt >= RateLimitHandler.MAX_RETRIES) {
                lastErrBody = lastResponse.body() != null ? lastResponse.body().string() : "";
                lastResponse.close();
                String msg = "HTTP " + lastCode + " after " + (attempt + 1) + " attempts: " + lastErrBody;
                if (lastCode == 429) throw new RateLimitExceededException(msg);
                throw new ServerErrorException(msg);
            }

            // Compute backoff and sleep.
            long delay = RateLimitHandler.computeBackoff(lastResponse, attempt);
            // Close the response body before retrying — we don't need the body
            // for the retry decision, only the status code (which we already
            // captured). Reading the body here would consume the stream and
            // prevent OkHttp from reusing the connection.
            try { lastResponse.close(); } catch (Exception ignored) {}
            lastResponse = null;

            // Sleep (interruptible). If interrupted (e.g. user aborted),
            // surface as a RuntimeException so the provider's caller sees it.
            // We register the current thread on sleepingThread so abortCurrent()
            // can interrupt us — OkHttp's Call.cancel() alone cannot break a
            // Thread.sleep() between retries.
            Thread current = Thread.currentThread();
            sleepingThread.set(current);
            try {
                if (!RateLimitHandler.sleepInterruptible(delay)) {
                    throw new RuntimeException("Request aborted during retry backoff");
                }
            } finally {
                sleepingThread.compareAndSet(current, null);
            }
        }

        // Should never reach here — the loop above returns or throws on every path.
        throw new RuntimeException("HTTP " + lastCode + ": exhausted retries");
    }

    /**
     * Execute a streaming POST with a custom OkHttp Request.Builder (used by
     * providers that need fine-grained control over headers / body, e.g.
     * Ollama which omits the Accept: text/event-stream header).
     */
    public static Response postStream(Request.Builder rb) throws Exception {
        Call call = getClient().newCall(rb.build());
        lastCall.set(call);
        return call.execute();
    }

    /**
     * Cancel the most recently started in-flight call, if any. Safe to call
     * repeatedly. Implemented by providers' {@code abort()} methods.
     */
    public static void abortCurrent() {
        // Cancel any in-flight HTTP call. This interrupts threads inside
        // Call.execute() (OkHttp's internal I/O wait).
        Call c = lastCall.getAndSet(null);
        if (c != null && !c.isCanceled()) {
            c.cancel();
        }
        // Also interrupt the thread sleeping in retry backoff, if any.
        // Call.cancel() alone is a no-op there — the thread is parked in
        // Thread.sleep(), not inside Call.execute().
        Thread t = sleepingThread.getAndSet(null);
        if (t != null && t != Thread.currentThread()) {
            t.interrupt();
        }
    }

    /**
     * Execute a non-streaming POST and return the response body as a string.
     * Uses {@link #postStreamWithRetry} for automatic 429/5xx retry handling.
     */
    public static String postJson(String url,
                                  String jsonBody,
                                  String apiKey,
                                  java.util.List<com.sketchware.ai.llm.LlmRequest.ExtraHeader> extraHeaders)
            throws Exception {
        try (Response resp = postStreamWithRetry(url, jsonBody, apiKey, extraHeaders, false)) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                throw new RuntimeException("HTTP " + resp.code() + ": " + errBody);
            }
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    /** Thrown when HTTP 429 (Too Many Requests) persists across all retries. */
    public static final class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
    }

    /** Thrown when HTTP 5xx (Server Error) persists across all retries. */
    public static final class ServerErrorException extends RuntimeException {
        public ServerErrorException(String message) { super(message); }
    }
}
