package com.sketchware.ai.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detects repeated identical tool calls — a strong signal that the LLM is
 * stuck in a loop. Mirrors Cline's {@code core/task/loop-detection.ts}.
 *
 * <p>The detector maintains a sliding window of the last N tool-call
 * signatures. A signature is the tool name + a normalized version of its
 * arguments (keys sorted, string values truncated to 80 chars, numeric
 * values preserved). When the same signature appears {@code >= SOFT_THRESHOLD}
 * times in the window, a soft warning is emitted; at {@code >= HARD_THRESHOLD}
 * a hard escalation is triggered.
 *
 * <p><b>Soft warning</b>: injects a text block into the conversation telling
 * the LLM it's repeating itself. The LLM gets a chance to recover.
 *
 * <p><b>Hard escalation</b>: bumps the agent's {@code consecutiveMistakeCount}
 * toward the {@code maxConsecutiveMistakes} threshold; once that's reached,
 * {@link AgentRuntime} cancels the run.
 *
 * <p>Thread-safety: this class is intended for use from a single background
 * thread (the agent loop). No synchronization.
 */
public final class LoopDetector {

    /** Soft warning threshold: at this many identical calls, inject a hint. */
    public static final int SOFT_THRESHOLD = 2;
    /** Hard escalation threshold: at this many identical calls, count as a mistake. */
    public static final int HARD_THRESHOLD = 3;
    /** Sliding window size. */
    private static final int WINDOW = 8;

    private final Deque<String> recentSignatures = new ArrayDeque<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private int consecutiveMistakeCount = 0;
    private final int maxConsecutiveMistakes;

    public LoopDetector() {
        this(2);
    }

    public LoopDetector(int maxConsecutiveMistakes) {
        this.maxConsecutiveMistakes = maxConsecutiveMistakes;
    }

    /**
     * Record a tool call and return the action the agent should take.
     *
     * @param toolName the tool name being invoked.
     * @param argsJson the raw JSON arguments string (may be null/empty).
     * @return a {@link LoopResult} indicating what to do.
     */
    public LoopResult observe(String toolName, String argsJson) {
        String sig = signature(toolName, argsJson);
        recentSignatures.addLast(sig);
        counts.merge(sig, 1, Integer::sum);
        if (recentSignatures.size() > WINDOW) {
            String evicted = recentSignatures.removeFirst();
            Integer c = counts.get(evicted);
            if (c != null) {
                if (c <= 1) counts.remove(evicted);
                else counts.put(evicted, c - 1);
            }
        }
        int count = counts.getOrDefault(sig, 0);
        boolean soft = count == SOFT_THRESHOLD;
        boolean hard = count >= HARD_THRESHOLD;
        if (hard) {
            consecutiveMistakeCount++;
        } else if (count < SOFT_THRESHOLD) {
            // A non-looping call clears the streak — otherwise a single loop
            // early in the session dooms the agent forever (every subsequent
            // different tool call still had consecutiveMistakeCount >= max
            // from the prior escalation, so shouldAbort stayed true).
            consecutiveMistakeCount = 0;
        }
        return new LoopResult(soft, hard, count, consecutiveMistakeCount, maxConsecutiveMistakes);
    }

    /** Reset the detector (e.g. when starting a new task). */
    public void reset() {
        recentSignatures.clear();
        counts.clear();
        consecutiveMistakeCount = 0;
    }

    /** Build a normalized signature for a tool call. */
    public static String signature(String toolName, String argsJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(toolName == null ? "<null>" : toolName);
        sb.append('(');
        if (argsJson != null && !argsJson.isEmpty()) {
            try {
                JsonObject obj = JsonParser.parseString(argsJson).getAsJsonObject();
                // Sort keys for stable signature.
                java.util.List<String> keys = new java.util.ArrayList<>();
                for (String k : obj.keySet()) keys.add(k);
                java.util.Collections.sort(keys);
                boolean first = true;
                for (String k : keys) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(k).append('=');
                    String v = stringify(obj.get(k));
                    sb.append(v);
                }
            } catch (Throwable t) {
                // If argsJson isn't valid JSON, fall back to a truncated hash.
                sb.append("raw:").append(truncate(argsJson, 80));
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private static String stringify(com.google.gson.JsonElement el) {
        if (el == null || el.isJsonNull()) return "null";
        if (el.isJsonPrimitive()) {
            String s = el.getAsString();
            return truncate(s, 80);
        }
        if (el.isJsonObject() || el.isJsonArray()) {
            return "<" + el.getClass().getSimpleName() + ">";
        }
        return "?";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Result of a loop-detection observation. */
    public static final class LoopResult {
        public final boolean softWarning;
        public final boolean hardEscalation;
        public final int repeatCount;
        public final int consecutiveMistakeCount;
        public final int maxConsecutiveMistakes;
        public final boolean shouldAbort;

        public LoopResult(boolean softWarning, boolean hardEscalation, int repeatCount,
                          int consecutiveMistakeCount, int maxConsecutiveMistakes) {
            this.softWarning = softWarning;
            this.hardEscalation = hardEscalation;
            this.repeatCount = repeatCount;
            this.consecutiveMistakeCount = consecutiveMistakeCount;
            this.maxConsecutiveMistakes = maxConsecutiveMistakes;
            this.shouldAbort = consecutiveMistakeCount >= maxConsecutiveMistakes;
        }

        /** The text to inject into the conversation if a soft warning is needed. */
        public String warningText(String toolName) {
            if (!softWarning && !hardEscalation) return null;
            if (hardEscalation) {
                return "[LOOP DETECTED] You have called `" + toolName + "` with the same arguments "
                        + repeatCount + " times. This is a hard escalation. The task will be aborted "
                        + "if you continue repeating. Reconsider your approach: try a different tool, "
                        + "ask the user for clarification, or use submit_and_exit if the task is done.";
            }
            return "[LOOP DETECTED] You have called `" + toolName + "` with the same arguments "
                    + repeatCount + " times. If this is intentional, explain why. Otherwise, try a "
                    + "different approach.";
        }
    }
}
