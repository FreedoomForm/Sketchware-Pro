package com.sketchware.ai.llm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Accumulates token usage and cost across an agent session. Mirrors Cline's
 * {@code shared/getApiMetrics.ts} and {@code utils/cost.ts}.
 *
 * <p>Tracks:
 * <ul>
 *   <li>Per-request input/output/reasoning/cache tokens and cost.</li>
 *   <li>Cumulative totals across the whole session.</li>
 *   <li>Per-provider, per-model breakdown (for cost analytics).</li>
 *   <li>Per-tool invocation count (for telemetry / debugging).</li>
 * </ul>
 *
 * <p>Thread-safe via AtomicInteger/Long/DoubleAdder. Safe to share across
 * the agent background thread and the UI thread.
 *
 * <p>Cost calculation uses a simple model-catalog lookup. Production apps
 * should externalize this to a config file; for now it's hardcoded for the
 * common models Cline supports.
 */
public final class UsageTracker {

    private final AtomicInteger totalInputTokens = new AtomicInteger();
    private final AtomicInteger totalOutputTokens = new AtomicInteger();
    private final AtomicInteger totalReasoningTokens = new AtomicInteger();
    private final AtomicInteger totalCacheReadTokens = new AtomicInteger();
    private final AtomicInteger totalCacheWriteTokens = new AtomicInteger();
    private final DoubleAdder totalCost = new DoubleAdder();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicLong startedAt = new AtomicLong(System.currentTimeMillis());

    private final Map<String, ModelUsage> perModel = new LinkedHashMap<>();
    private final Map<String, AtomicInteger> perToolCount = new LinkedHashMap<>();

    /** Record a single LLM API call's usage. */
    public void record(String providerId, String modelId,
                       int inputTokens, int outputTokens, int reasoningTokens,
                       int cacheReadTokens, int cacheWriteTokens,
                       double cost) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalReasoningTokens.addAndGet(reasoningTokens);
        totalCacheReadTokens.addAndGet(cacheReadTokens);
        totalCacheWriteTokens.addAndGet(cacheWriteTokens);
        if (cost > 0) totalCost.add(cost);
        requestCount.incrementAndGet();

        String key = (providerId == null ? "?" : providerId) + "/" + (modelId == null ? "?" : modelId);
        ModelUsage mu;
        synchronized (perModel) {
            mu = perModel.get(key);
            if (mu == null) {
                mu = new ModelUsage(providerId, modelId);
                perModel.put(key, mu);
            }
        }
        mu.record(inputTokens, outputTokens, reasoningTokens, cacheReadTokens, cacheWriteTokens, cost);
    }

    /** Record a tool invocation (called once per tool execution). */
    public void recordToolCall(String toolName) {
        if (toolName == null) return;
        synchronized (perToolCount) {
            AtomicInteger c = perToolCount.get(toolName);
            if (c == null) {
                c = new AtomicInteger();
                perToolCount.put(toolName, c);
            }
            c.incrementAndGet();
        }
    }

    /** Snapshot the cumulative totals. */
    public Snapshot snapshot() {
        synchronized (perModel) {
            synchronized (perToolCount) {
                return new Snapshot(
                        totalInputTokens.get(),
                        totalOutputTokens.get(),
                        totalReasoningTokens.get(),
                        totalCacheReadTokens.get(),
                        totalCacheWriteTokens.get(),
                        totalCost.sum(),
                        requestCount.get(),
                        startedAt.get(),
                        System.currentTimeMillis(),
                        new LinkedHashMap<>(perModel),
                        new LinkedHashMap<>(perToolCount));
            }
        }
    }

    /** Reset all counters (e.g. when starting a new task). */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalReasoningTokens.set(0);
        totalCacheReadTokens.set(0);
        totalCacheWriteTokens.set(0);
        totalCost.reset();
        requestCount.set(0);
        startedAt.set(System.currentTimeMillis());
        synchronized (perModel) { perModel.clear(); }
        synchronized (perToolCount) { perToolCount.clear(); }
    }

    /**
     * Estimate cost for a single API call. Pricing is per 1M tokens, in USD.
     * Returns 0 for unknown models (the actual usage chunk may carry a
     * precomputed cost from the provider — that takes precedence).
     */
    public static double estimateCost(String providerId, String modelId,
                                      int inputTokens, int outputTokens) {
        if (providerId == null || modelId == null) return 0;
        String m = modelId.toLowerCase();
        // Rough pricing as of 2025-Q4. Adjust as needed.
        switch (providerId.toLowerCase()) {
            case "openai":
            case "openai-compatible":
                if (m.contains("gpt-4o-mini")) return (inputTokens * 0.15 + outputTokens * 0.60) / 1_000_000;
                if (m.contains("gpt-4o"))      return (inputTokens * 2.50 + outputTokens * 10.0) / 1_000_000;
                if (m.contains("gpt-4-turbo")) return (inputTokens * 10.0 + outputTokens * 30.0) / 1_000_000;
                if (m.contains("gpt-4"))       return (inputTokens * 30.0 + outputTokens * 60.0) / 1_000_000;
                if (m.contains("gpt-3.5"))     return (inputTokens * 0.50 + outputTokens * 1.50) / 1_000_000;
                if (m.contains("o1-mini"))     return (inputTokens * 3.0 + outputTokens * 12.0) / 1_000_000;
                if (m.contains("o1"))          return (inputTokens * 15.0 + outputTokens * 60.0) / 1_000_000;
                break;
            case "anthropic":
                if (m.contains("claude-3-5-sonnet") || m.contains("claude-3.5-sonnet"))
                    return (inputTokens * 3.0 + outputTokens * 15.0) / 1_000_000;
                if (m.contains("claude-3-5-haiku") || m.contains("claude-3.5-haiku"))
                    return (inputTokens * 0.80 + outputTokens * 4.0) / 1_000_000;
                if (m.contains("claude-3-opus"))
                    return (inputTokens * 15.0 + outputTokens * 75.0) / 1_000_000;
                if (m.contains("claude-3-sonnet"))
                    return (inputTokens * 3.0 + outputTokens * 15.0) / 1_000_000;
                if (m.contains("claude-3-haiku"))
                    return (inputTokens * 0.25 + outputTokens * 1.25) / 1_000_000;
                break;
            case "gemini":
                if (m.contains("gemini-1.5-pro"))   return (inputTokens * 1.25 + outputTokens * 5.0) / 1_000_000;
                if (m.contains("gemini-1.5-flash")) return (inputTokens * 0.075 + outputTokens * 0.30) / 1_000_000;
                if (m.contains("gemini-2"))         return (inputTokens * 1.25 + outputTokens * 5.0) / 1_000_000;
                break;
            case "ollama":
                // Local models have no monetary cost.
                return 0;
        }
        // Default fallback: $1/M input, $3/M output.
        return (inputTokens * 1.0 + outputTokens * 3.0) / 1_000_000;
    }

    /** Per-model usage breakdown. */
    public static final class ModelUsage {
        public final String providerId;
        public final String modelId;
        public final AtomicInteger inputTokens = new AtomicInteger();
        public final AtomicInteger outputTokens = new AtomicInteger();
        public final AtomicInteger reasoningTokens = new AtomicInteger();
        public final AtomicInteger cacheReadTokens = new AtomicInteger();
        public final AtomicInteger cacheWriteTokens = new AtomicInteger();
        public final DoubleAdder cost = new DoubleAdder();
        public final AtomicInteger calls = new AtomicInteger();

        ModelUsage(String providerId, String modelId) {
            this.providerId = providerId;
            this.modelId = modelId;
        }

        void record(int in, int out, int reason, int cacheR, int cacheW, double c) {
            inputTokens.addAndGet(in);
            outputTokens.addAndGet(out);
            reasoningTokens.addAndGet(reason);
            cacheReadTokens.addAndGet(cacheR);
            cacheWriteTokens.addAndGet(cacheW);
            if (c > 0) cost.add(c);
            calls.incrementAndGet();
        }
    }

    /** Immutable snapshot of cumulative usage. */
    public static final class Snapshot {
        public final int totalInputTokens;
        public final int totalOutputTokens;
        public final int totalReasoningTokens;
        public final int totalCacheReadTokens;
        public final int totalCacheWriteTokens;
        public final double totalCost;
        public final int requestCount;
        public final long startedAt;
        public final long snapshotAt;
        public final Map<String, ModelUsage> perModel;
        public final Map<String, AtomicInteger> perToolCount;

        public Snapshot(int totalInputTokens, int totalOutputTokens, int totalReasoningTokens,
                        int totalCacheReadTokens, int totalCacheWriteTokens, double totalCost,
                        int requestCount, long startedAt, long snapshotAt,
                        Map<String, ModelUsage> perModel, Map<String, AtomicInteger> perToolCount) {
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.totalReasoningTokens = totalReasoningTokens;
            this.totalCacheReadTokens = totalCacheReadTokens;
            this.totalCacheWriteTokens = totalCacheWriteTokens;
            this.totalCost = totalCost;
            this.requestCount = requestCount;
            this.startedAt = startedAt;
            this.snapshotAt = snapshotAt;
            this.perModel = perModel;
            this.perToolCount = perToolCount;
        }

        public int totalTokens() {
            return totalInputTokens + totalOutputTokens + totalReasoningTokens;
        }

        public long elapsedMs() {
            return snapshotAt - startedAt;
        }

        /** Human-readable summary suitable for the /cost slash command. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Token usage:\n");
            sb.append("  Input:       ").append(format(totalInputTokens)).append("\n");
            sb.append("  Output:      ").append(format(totalOutputTokens)).append("\n");
            if (totalReasoningTokens > 0) {
                sb.append("  Reasoning:   ").append(format(totalReasoningTokens)).append("\n");
            }
            if (totalCacheReadTokens > 0) {
                sb.append("  Cache read:  ").append(format(totalCacheReadTokens)).append("\n");
            }
            if (totalCacheWriteTokens > 0) {
                sb.append("  Cache write: ").append(format(totalCacheWriteTokens)).append("\n");
            }
            sb.append("  Total:       ").append(format(totalTokens())).append("\n");
            sb.append("  Requests:    ").append(requestCount).append("\n");
            sb.append("  Cost:        $").append(String.format("%.4f", totalCost)).append("\n");
            sb.append("  Elapsed:     ").append(formatMs(elapsedMs())).append("\n");
            if (!perModel.isEmpty()) {
                sb.append("\nPer model:\n");
                for (ModelUsage mu : perModel.values()) {
                    sb.append("  ").append(mu.providerId).append('/').append(mu.modelId)
                      .append(": in=").append(format(mu.inputTokens.get()))
                      .append(" out=").append(format(mu.outputTokens.get()))
                      .append(" calls=").append(mu.calls.get())
                      .append(" $").append(String.format("%.4f", mu.cost.sum()))
                      .append("\n");
                }
            }
            return sb.toString();
        }

        private static String format(int n) {
            return String.format("%,d", n);
        }

        private static String formatMs(long ms) {
            long s = ms / 1000;
            long m = s / 60;
            long h = m / 60;
            if (h > 0) return h + "h " + (m % 60) + "m " + (s % 60) + "s";
            if (m > 0) return m + "m " + (s % 60) + "s";
            return s + "s";
        }
    }
}
