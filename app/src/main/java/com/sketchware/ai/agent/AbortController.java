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

    /**
     * Reset the abort flag so a new run can proceed.
     *
     * <p>CRITICAL: without this, the {@code aborted} flag stays true forever
     * after the first abort(), and every subsequent {@code AgentRuntime.execute()}
     * call exits the loop on the first iteration check, producing a no-op run
     * with no listener callback. The user clicks "Stop" once and the agent
     * never works again until the activity is recreated.
     *
     * <p>Called by {@code AgentRuntime.execute()} at the start of every new run.
     */
    public void reset() {
        aborted.set(false);
        httpCancellation.set(null);
    }

    /** Register a callback that cancels the in-flight HTTP call. */
    public void setHttpCancellation(Runnable r) {
        httpCancellation.set(r);
    }

    public void clearHttpCancellation() {
        httpCancellation.set(null);
    }
}
