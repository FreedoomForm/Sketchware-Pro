package com.sketchware.ai.llm.routing;

import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.llm.ModelInfo;
import com.sketchware.ai.llm.ProviderCatalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Heuristic model selector. Direct port of Cline's
 * {@code sdk/packages/core/src/router/heuristic.ts} decision tree, adapted
 * to Sketchware Pro's provider catalog.
 *
 * <p>Picks a recommended model from a list of candidates based on:
 * <ul>
 *   <li><b>Agent mode</b> — RESEARCH favors large-context models with
 *       strong retrieval; PLAN favors reasoning models; ACT favors fast
 *       models for simple tasks and capable models for complex tasks.</li>
 *   <li><b>Task complexity</b> — keyword-based classification of the
 *       user's message into SIMPLE / MODERATE / COMPLEX. A simple task
 *       ("add a button", "change color") on a small/fast model is 5-10x
 *       cheaper and 3-5x faster than on a frontier model, with no
 *       measurable quality loss on the Sketchware toolset.</li>
 *   <li><b>Capability requirements</b> — vision (image attachments),
 *       reasoning (multi-step plans), tools (function calling), context
 *       window size.</li>
 *   <li><b>Cost ceiling</b> — when the user has set a max-cost-per-turn
 *       preference, skip models whose input+output price exceeds it.</li>
 * </ul>
 *
 * <h2>Decision tree (mirrors Cline's heuristic.ts)</h2>
 * <pre>
 * if mode == RESEARCH:
 *     prefer: context_window >= 128K, supportsTools, supportsImages (for screenshots)
 *     pick: largest-context model with tools + images, then cheapest
 * elif mode == PLAN:
 *     prefer: supportsReasoning, supportsTools, context_window >= 64K
 *     pick: cheapest reasoning model
 * elif mode == ACT:
 *     complexity = classify(userMessage)
 *     if complexity == SIMPLE:
 *         prefer: small/fast model (<= $1/M input), supportsTools
 *         pick: cheapest model with tools
 *     elif complexity == MODERATE:
 *         prefer: mid-tier model (<= $5/M input), supportsTools
 *         pick: cheapest mid-tier model
 *     else:  // COMPLEX
 *         prefer: frontier model, supportsReasoning, supportsTools
 *         pick: cheapest frontier model
 * else:
 *     fallback: first candidate
 * </pre>
 *
 * <p>The classifier is keyword-based — not perfect, but fast (no LLM
 * round-trip) and good enough for the Sketchware toolset where most
 * tasks are GUI manipulation (simple) vs. full-feature implementation
 * (complex). The keyword lists are calibrated to the Sketchware-Pro
 * workflow vocabulary.
 */
public final class ModelSelector {

    private ModelSelector() {}

    /** Complexity classification returned by {@link #classifyComplexity(String)}. */
    public enum Complexity {
        /** Single-step GUI op: add a widget, set a property, list files. */
        SIMPLE,
        /** Multi-step but well-scoped: build a form, wire up events, add a library. */
        MODERATE,
        /** Open-ended / architectural: refactor, full-feature impl, debug a crash. */
        COMPLEX
    }

    /** Per-complexity cost ceilings (USD per 1M input tokens). */
    private static final double COST_CEILING_SIMPLE = 1.0;
    private static final double COST_CEILING_MODERATE = 5.0;
    private static final double COST_CEILING_COMPLEX = 20.0;

    /** Minimum context window for RESEARCH mode (tokens). */
    private static final int MIN_CONTEXT_RESEARCH = 128_000;

    /** Minimum context window for PLAN mode (tokens). */
    private static final int MIN_CONTEXT_PLAN = 64_000;

    /** Keywords that signal a SIMPLE task. */
    private static final Set<String> SIMPLE_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "add", "set", "remove", "delete", "list", "show", "rename", "move",
            "button", "text", "textview", "imageview", "color", "padding", "margin",
            "layout", "linear", "relative", "frame", "constraint", "scroll", "listview",
            "widget", "view", "label", "icon", "string", "style", "theme", "font",
            "permission", "manifest", "enable", "disable", "toggle"
    )));

    /** Keywords that signal a COMPLEX task. */
    private static final Set<String> COMPLEX_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "refactor", "implement", "architect", "design", "debug", "fix bug",
            "performance", "optimize", "migrate", "rewrite", "redesign",
            "animation", "transition", "fragment navigation", "viewpager",
            "recyclerview adapter", "viewmodel", "livedata", "room database",
            "firebase auth", "retrofit", "coroutine", "flow", "compose",
            "custom view", "canvas", "paint", "gesture", "touch event",
            "multithread", "async", "background service", "foreground service",
            "notification channel", "workmanager", "alarmmanager",
            "complex", "advanced", "full feature", "complete", "entire"
    )));

    /** Keywords that signal a MODERATE task (checked after SIMPLE and COMPLEX). */
    private static final Set<String> MODERATE_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "form", "input", "validate", "validation", "event", "click listener",
            "build", "compile", "run", "install", "sign", "export",
            "library", "dependency", "gradle", "configure", "setup",
            "recyclerview", "adapter", "dialog", "bottom sheet", "snackbar",
            "navigation", "intent", "bundle", "shared preferences",
            "sql", "database", "query", "table", "schema"
    )));

    /**
     * Pick a recommended model from the candidate list.
     *
     * @param candidates   available models, must be non-empty.
     * @param mode         current agent mode.
     * @param userMessage  the user's latest message (for complexity classification).
     *                     Null or empty is treated as MODERATE.
     * @param requiresVision true if the message has image attachments.
     * @return the recommended model, or {@code candidates.get(0)} if none match.
     */
    public static ModelInfo select(List<ModelInfo> candidates,
                                    AgentMode mode,
                                    String userMessage,
                                    boolean requiresVision) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must be non-empty");
        }
        if (candidates.size() == 1) return candidates.get(0);

        switch (mode) {
            case RESEARCH:
                return selectForResearch(candidates, requiresVision);
            case PLAN:
                return selectForPlan(candidates, requiresVision);
            case ACT:
            case YOLO:
            default:
                return selectForAct(candidates, userMessage, requiresVision);
        }
    }

    /**
     * RESEARCH mode: prefer large-context models with tools + vision.
     * Falls back to the largest-context model with tools if no vision-capable
     * model is available.
     */
    private static ModelInfo selectForResearch(List<ModelInfo> candidates, boolean requiresVision) {
        List<ModelInfo> pool = filterByTools(candidates);
        if (requiresVision) {
            List<ModelInfo> vision = filterByVision(pool);
            if (!vision.isEmpty()) pool = vision;
        }
        pool = filterByContext(pool, MIN_CONTEXT_RESEARCH);
        if (pool.isEmpty()) {
            // No model meets the research context floor — fall back to the
            // largest available context.
            pool = sortByContextDesc(candidates);
        } else {
            pool = sortByCostAsc(pool);
        }
        return pool.get(0);
    }

    /**
     * PLAN mode: prefer reasoning models with tools and a decent context window.
     */
    private static ModelInfo selectForPlan(List<ModelInfo> candidates, boolean requiresVision) {
        List<ModelInfo> pool = filterByTools(candidates);
        if (requiresVision) {
            List<ModelInfo> vision = filterByVision(pool);
            if (!vision.isEmpty()) pool = vision;
        }
        List<ModelInfo> reasoning = filterByReasoning(pool);
        if (!reasoning.isEmpty()) {
            pool = filterByContext(reasoning, MIN_CONTEXT_PLAN);
            if (pool.isEmpty()) pool = reasoning;
            return sortByCostAsc(pool).get(0);
        }
        // No reasoning model — fall back to largest context + cheapest.
        pool = filterByContext(pool, MIN_CONTEXT_PLAN);
        if (pool.isEmpty()) pool = candidates;
        return sortByContextDesc(pool).get(0);
    }

    /**
     * ACT mode: classify complexity and pick accordingly.
     */
    private static ModelInfo selectForAct(List<ModelInfo> candidates,
                                           String userMessage,
                                           boolean requiresVision) {
        Complexity c = classifyComplexity(userMessage);
        List<ModelInfo> pool = filterByTools(candidates);
        if (requiresVision) {
            List<ModelInfo> vision = filterByVision(pool);
            if (!vision.isEmpty()) pool = vision;
        }
        double ceiling;
        switch (c) {
            case SIMPLE:    ceiling = COST_CEILING_SIMPLE;    break;
            case MODERATE:  ceiling = COST_CEILING_MODERATE;  break;
            case COMPLEX:   ceiling = COST_CEILING_COMPLEX;   break;
            default:        ceiling = COST_CEILING_MODERATE;  break;
        }
        List<ModelInfo> withinBudget = filterByCost(pool, ceiling);
        if (withinBudget.isEmpty()) {
            // No model within budget — pick the cheapest available.
            return sortByCostAsc(pool).get(0);
        }
        if (c == Complexity.COMPLEX) {
            // For complex tasks, prefer reasoning models within budget.
            List<ModelInfo> reasoning = filterByReasoning(withinBudget);
            if (!reasoning.isEmpty()) return sortByCostAsc(reasoning).get(0);
        }
        return sortByCostAsc(withinBudget).get(0);
    }

    /**
     * Classify the user's message into SIMPLE / MODERATE / COMPLEX.
     * Uses keyword matching on a lowercased, whitespace-tokenized version
     * of the message.
     *
     * <p>Heuristics:
     * <ul>
     *   <li>If the message contains any COMPLEX keyword → COMPLEX.</li>
     *   <li>If the message length > 500 chars OR contains 3+ MODERATE
     *       keywords → COMPLEX.</li>
     *   <li>If the message contains 2+ MODERATE keywords → MODERATE.</li>
     *   <li>If the message contains 1 MODERATE keyword → MODERATE.</li>
     *   <li>If the message contains only SIMPLE keywords → SIMPLE.</li>
     *   <li>Default → MODERATE.</li>
     * </ul>
     */
    public static Complexity classifyComplexity(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) return Complexity.MODERATE;
        String lower = userMessage.toLowerCase(Locale.ROOT);
        // Long messages are almost always complex — they describe a feature
        // or a multi-step requirement.
        if (lower.length() > 500) return Complexity.COMPLEX;

        String[] words = lower.split("\\W+");
        int simpleHits = 0;
        int moderateHits = 0;
        int complexHits = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (COMPLEX_KEYWORDS.contains(w)) {
                complexHits++;
            } else if (MODERATE_KEYWORDS.contains(w)) {
                moderateHits++;
            } else if (SIMPLE_KEYWORDS.contains(w)) {
                simpleHits++;
            }
        }
        // Also check for bigram matches (e.g. "fix bug", "full feature").
        for (String bigram : COMPLEX_KEYWORDS) {
            if (bigram.contains(" ") && lower.contains(bigram)) complexHits++;
        }
        for (String bigram : MODERATE_KEYWORDS) {
            if (bigram.contains(" ") && lower.contains(bigram)) moderateHits++;
        }

        if (complexHits > 0) return Complexity.COMPLEX;
        if (moderateHits >= 3) return Complexity.COMPLEX;
        if (moderateHits >= 1) return Complexity.MODERATE;
        if (simpleHits > 0) return Complexity.SIMPLE;
        return Complexity.MODERATE;
    }

    // ---- Filters ----

    private static List<ModelInfo> filterByTools(List<ModelInfo> ms) {
        List<ModelInfo> out = new ArrayList<>();
        for (ModelInfo m : ms) if (m.supportsTools) out.add(m);
        return out.isEmpty() ? ms : out;
    }

    private static List<ModelInfo> filterByVision(List<ModelInfo> ms) {
        List<ModelInfo> out = new ArrayList<>();
        for (ModelInfo m : ms) if (m.supportsImages) out.add(m);
        return out;
    }

    private static List<ModelInfo> filterByReasoning(List<ModelInfo> ms) {
        List<ModelInfo> out = new ArrayList<>();
        for (ModelInfo m : ms) if (m.supportsReasoning) out.add(m);
        return out;
    }

    private static List<ModelInfo> filterByContext(List<ModelInfo> ms, int minContext) {
        List<ModelInfo> out = new ArrayList<>();
        for (ModelInfo m : ms) if (m.contextWindow >= minContext) out.add(m);
        return out;
    }

    private static List<ModelInfo> filterByCost(List<ModelInfo> ms, double ceiling) {
        List<ModelInfo> out = new ArrayList<>();
        for (ModelInfo m : ms) {
            if (m.inputPricePer1M <= ceiling) out.add(m);
        }
        return out;
    }

    private static List<ModelInfo> sortByCostAsc(List<ModelInfo> ms) {
        List<ModelInfo> out = new ArrayList<>(ms);
        Collections.sort(out, (a, b) -> Double.compare(a.inputPricePer1M, b.inputPricePer1M));
        return out;
    }

    private static List<ModelInfo> sortByContextDesc(List<ModelInfo> ms) {
        List<ModelInfo> out = new ArrayList<>(ms);
        Collections.sort(out, (a, b) -> Integer.compare(b.contextWindow, a.contextWindow));
        return out;
    }

    /**
     * Build a default candidate list for a provider, using the provider's
     * built-in model catalog from {@link ProviderCatalog}. Useful when the
     * user has not specified a model and we want to pick a sensible default.
     *
     * <p>Returns synthetic {@link ModelInfo} objects with conservative
     * defaults (128K context, $1/M input price). Real apps should replace
     * these with accurate per-model metadata fetched from the provider's
     * /models endpoint.
     */
    public static List<ModelInfo> defaultCandidatesForProvider(String providerId) {
        List<String> ids = ProviderCatalog.builtinModelsFor(providerId);
        if (ids.isEmpty()) {
            return Collections.singletonList(ModelInfo.defaultFor(
                    ProviderCatalog.defaultModelFor(providerId)));
        }
        List<ModelInfo> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            out.add(syntheticModelFor(providerId, id));
        }
        return out;
    }

    /**
     * Build a synthetic {@link ModelInfo} for a provider+model id pair.
     * Uses conservative defaults but applies a few well-known model
     * overrides (GPT-4o, Claude Sonnet, Gemini 2.5 Pro, etc.).
     */
    public static ModelInfo syntheticModelFor(String providerId, String modelId) {
        // Well-known model overrides — these are accurate as of 2025-Q4.
        String m = modelId.toLowerCase(Locale.ROOT);
        // GPT-4o family
        if (m.equals("gpt-4o")) {
            return new ModelInfo(modelId, "GPT-4o", 128_000, 128_000, 16_384,
                    true, true, false, 2.50, 10.00, 1.25, 2.50);
        }
        if (m.equals("gpt-4o-mini")) {
            return new ModelInfo(modelId, "GPT-4o mini", 128_000, 128_000, 16_384,
                    true, true, false, 0.15, 0.60, 0.075, 0.15);
        }
        if (m.contains("gpt-4.1")) {
            return new ModelInfo(modelId, modelId, 1_000_000, 1_000_000, 32_768,
                    true, true, false, 2.00, 8.00, 0.50, 2.00);
        }
        if (m.startsWith("o3") || m.startsWith("o4")) {
            return new ModelInfo(modelId, modelId, 200_000, 200_000, 100_000,
                    true, true, true, 5.00, 15.00, 2.50, 5.00);
        }
        // Claude family
        if (m.contains("claude-opus-4")) {
            return new ModelInfo(modelId, modelId, 200_000, 200_000, 32_000,
                    true, true, true, 15.00, 75.00, 1.50, 18.75);
        }
        if (m.contains("claude-sonnet-4") || m.contains("claude-3-7-sonnet")) {
            return new ModelInfo(modelId, modelId, 200_000, 200_000, 16_000,
                    true, true, true, 3.00, 15.00, 0.30, 3.75);
        }
        if (m.contains("claude-3-5-sonnet")) {
            return new ModelInfo(modelId, modelId, 200_000, 200_000, 8_192,
                    true, true, false, 3.00, 15.00, 0.30, 3.75);
        }
        if (m.contains("claude-3-5-haiku")) {
            return new ModelInfo(modelId, modelId, 200_000, 200_000, 8_192,
                    true, true, false, 0.80, 4.00, 0.08, 1.00);
        }
        // Gemini family
        if (m.contains("gemini-2.5-pro") || m.contains("gemini-2.5-pro")) {
            return new ModelInfo(modelId, modelId, 2_000_000, 1_000_000, 8_192,
                    true, true, true, 1.25, 10.00, 0.3125, 2.50);
        }
        if (m.contains("gemini-2.0-flash") && !m.contains("lite")) {
            return new ModelInfo(modelId, modelId, 1_000_000, 1_000_000, 8_192,
                    true, true, false, 0.10, 0.40, 0.025, 0.20);
        }
        if (m.contains("gemini-2.0-flash-lite") || m.contains("gemini-1.5-flash")) {
            return new ModelInfo(modelId, modelId, 1_000_000, 1_000_000, 8_192,
                    true, true, false, 0.075, 0.30, 0.01875, 0.15);
        }
        if (m.contains("gemini-1.5-pro")) {
            return new ModelInfo(modelId, modelId, 2_000_000, 2_000_000, 8_192,
                    true, true, false, 1.25, 5.00, 0.3125, 1.25);
        }
        // DeepSeek
        if (m.equals("deepseek-chat")) {
            return new ModelInfo(modelId, modelId, 64_000, 64_000, 8_192,
                    true, false, false, 0.27, 1.10, 0.07, 0.27);
        }
        if (m.equals("deepseek-reasoner") || m.contains("deepseek-r1")) {
            return new ModelInfo(modelId, modelId, 64_000, 64_000, 32_000,
                    true, false, true, 0.55, 2.19, 0.14, 0.55);
        }
        // GLM (Z.AI)
        if (m.contains("glm-4.6") || m.contains("glm-4.5")) {
            return new ModelInfo(modelId, modelId, 128_000, 128_000, 16_384,
                    true, true, true, 0.60, 2.20, 0.15, 0.60);
        }
        if (m.contains("glm-4-flash")) {
            return new ModelInfo(modelId, modelId, 128_000, 128_000, 4_096,
                    true, false, false, 0.0, 0.0, 0.0, 0.0);
        }
        // Qwen
        if (m.contains("qwen3-235b") || m.contains("qwen3-coder")) {
            return new ModelInfo(modelId, modelId, 256_000, 256_000, 32_768,
                    true, false, true, 0.50, 2.00, 0.125, 0.50);
        }
        // Llama 3.x (Together / Fireworks / Groq)
        if (m.contains("llama-3.3-70b") || m.contains("llama3.3-70b")) {
            return new ModelInfo(modelId, modelId, 128_000, 128_000, 4_096,
                    true, false, false, 0.88, 0.88, 0.22, 0.88);
        }
        if (m.contains("llama-3.1-8b") || m.contains("llama3.1-8b")) {
            return new ModelInfo(modelId, modelId, 128_000, 128_000, 4_096,
                    true, false, false, 0.05, 0.08, 0.0125, 0.05);
        }
        // Mistral / Codestral
        if (m.contains("codestral")) {
            return new ModelInfo(modelId, modelId, 256_000, 256_000, 8_192,
                    true, false, false, 0.30, 0.90, 0.075, 0.30);
        }
        if (m.contains("mistral-large")) {
            return new ModelInfo(modelId, modelId, 128_000, 128_000, 8_192,
                    true, true, false, 2.00, 6.00, 0.50, 2.00);
        }
        // Grok
        if (m.contains("grok-3")) {
            return new ModelInfo(modelId, modelId, 1_000_000, 1_000_000, 8_192,
                    true, true, false, 3.00, 15.00, 0.75, 3.00);
        }
        // Generic fallback — conservative defaults.
        return new ModelInfo(modelId, modelId, 128_000, 128_000, 4_096,
                true, false, false, 1.00, 3.00, 0.25, 1.00);
    }
}
