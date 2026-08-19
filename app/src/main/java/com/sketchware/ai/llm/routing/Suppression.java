package com.sketchware.ai.llm.routing;

/**
 * Suppressions a rule can declare. Mirrors Cline's
 * {@code ProviderOptionSuppression} type.
 */
public final class Suppression {
    public final boolean genericThinking;
    public final boolean genericFanout;

    public Suppression(boolean genericThinking, boolean genericFanout) {
        this.genericThinking = genericThinking;
        this.genericFanout = genericFanout;
    }

    public static Suppression none() { return new Suppression(false, false); }
    public static Suppression genericThinking() { return new Suppression(true, false); }
    public static Suppression genericFanout() { return new Suppression(false, true); }
    public static Suppression all() { return new Suppression(true, true); }
}
