package com.sketchware.ai.agent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cooperative cancellation flag for the agent loop.
 * Mirrors Cline's AbortController usage. The agent loop polls {@link #isAborted()}
 * at every iteration boundary and the provider's HTTP call is cancelled via
 * {@link #abortHttpCall()}.
 */
public final class AbortController {
    private final AtomicBoolean aborted = new AtomicBoolean(false);
    private final AtomicReference<Runnable> httpCancellation = new AtomicReference<>();

    public boolean isAborted() {
        return aborted.get();
    }

    /** Cooperative abort - signals the loop and cancels the in-flight HTTP call if any. */
    public void abort() {
        aborted.set(true);
        Runnable r = httpCancellation.get();
        if (r != null) {
            try { r.run(); } catch (Throwable ignored) {}
        }
    }

    /** Register a callback that cancels the in-flight HTTP call. */
    public void setHttpCancellation(Runnable r) {
        httpCancellation.set(r);
    }

    public void clearHttpCancellation() {
        httpCancellation.set(null);
    }
}
