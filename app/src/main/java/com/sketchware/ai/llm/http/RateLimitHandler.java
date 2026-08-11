package com.sketchware.ai.llm.http;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.Response;

/**
 * HTTP 429 / 5xx retry helper with exponential backoff + jitter.
 *
 * <p>Mirrors Cline's retry strategy in {@code core/api/index.ts}: honor the
 * server's {@code Retry-After} header when present, otherwise use exponential
 * backoff (1s → 2s → 4s → 8s …) capped at 30s, with up to {@value #MAX_RETRIES}
 * attempts. Adds a small random jitter (0-250 ms) so concurrent clients don't
 * thunder-herd the server on retry.
 *
 * <p>Thread-safety: stateless. Safe to call from any thread.
 */
public final class RateLimitHandler {

    /** Maximum retry attempts for transient errors (429, 5xx). */
    public static final int MAX_RETRIES = 4;

    /** Base backoff in millis. The first retry waits ~this long (plus jitter). */
    public static final long BASE_BACKOFF_MS = 1_000L;

    /** Cap on per-retry backoff. Even after many retries, never wait longer than this. */
    public static final long MAX_BACKOFF_MS = 30_000L;

    /** Maximum jitter added to each backoff, in millis. */
    public static final long MAX_JITTER_MS = 250L;

    private RateLimitHandler() {}

    /**
     * Decide whether an HTTP response is retryable (429 or 5xx).
     */
    public static boolean isRetryable(int code) {
        return code == 429 || (code >= 500 && code <= 599);
    }

    /**
     * Compute the backoff delay for the given retry attempt (0-indexed).
     *
     * <p>If the server provided a {@code Retry-After} header, honor it
     * (capped at {@link #MAX_BACKOFF_MS}). Otherwise use exponential backoff:
     * {@code BASE_BACKOFF_MS * 2^attempt}, capped at {@link #MAX_BACKOFF_MS},
     * plus a random jitter of 0-{@link #MAX_JITTER_MS}.
     *
     * @param response     the failed response (may be null if the request itself threw)
     * @param attemptIndex 0 for the first retry, 1 for the second, etc.
     * @return delay in milliseconds
     */
    public static long computeBackoff(Response response, int attemptIndex) {
        long retryAfter = parseRetryAfter(response);
        if (retryAfter > 0) {
            return Math.min(retryAfter, MAX_BACKOFF_MS);
        }
        long exp = BASE_BACKOFF_MS * (1L << Math.min(attemptIndex, 10));
        long capped = Math.min(exp, MAX_BACKOFF_MS);
        long jitter = ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MS);
        return capped + jitter;
    }

    /**
     * Overload for computing backoff when we only have the HTTP status code
     * (no Retry-After header available).
     */
    public static long computeBackoff(int attemptIndex) {
        long exp = BASE_BACKOFF_MS * (1L << Math.min(attemptIndex, 10));
        long capped = Math.min(exp, MAX_BACKOFF_MS);
        long jitter = ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MS);
        return capped + jitter;
    }

    /**
     * Parse the {@code Retry-After} header. Supports both integer (seconds)
     * and HTTP-date formats. Returns -1 if absent or unparseable.
     */
    private static long parseRetryAfter(Response response) {
        if (response == null) return -1;
        String header = response.header("Retry-After");
        if (header == null || header.isEmpty()) return -1;
        // Integer seconds (most common).
        try {
            int seconds = Integer.parseInt(header.trim());
            return seconds * 1000L;
        } catch (NumberFormatException ignored) {
            // Fall through to HTTP-date parsing.
        }
        // HTTP-date format (RFC 7231 §7.1.3) — e.g. "Wed, 21 Oct 2015 07:28:00 GMT".
        // We use Java's SimpleDateFormat with the standard RFC 1123 pattern
        // instead of OkHttp's internal HttpDate class (which is not part of
        // the public API and may change between versions).
        try {
            java.text.SimpleDateFormat rfc1123 = new java.text.SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
            rfc1123.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
            java.util.Date parsed = rfc1123.parse(header.trim());
            if (parsed != null) {
                long delay = parsed.getTime() - System.currentTimeMillis();
                return delay > 0 ? delay : 0;
            }
        } catch (Throwable ignored) {
            // Ignore unparseable date.
        }
        return -1;
    }

    /**
     * Sleep for the given delay, interruptible. Returns true if slept the
     * full duration, false if interrupted.
     */
    public static boolean sleepInterruptible(long delayMs) {
        if (delayMs <= 0) return true;
        try {
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Build a human-readable description of the retry decision, for logging.
     */
    public static String describeRetry(int attemptIndex, int httpCode, long delayMs) {
        return "HTTP " + httpCode + " (attempt " + (attemptIndex + 1) + "/"
                + MAX_RETRIES + ") — retrying in " + delayMs + "ms";
    }
}
