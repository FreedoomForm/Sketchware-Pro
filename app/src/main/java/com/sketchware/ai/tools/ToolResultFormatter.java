package com.sketchware.ai.tools;

/**
 * Standardized tool-result formatting utilities. Mirrors Cline's
 * {@code core/prompts/responses.ts} {@code formatResponse.*} helpers.
 *
 * <p>The goal: produce consistent, LLM-recoverable error messages across all
 * tools. Without this, each tool invented its own error format ("ERROR: foo",
 * "[tool] foo", "foo (failed)", etc.) which made the LLM's recovery logic
 * unreliable.
 *
 * <h2>Standard formats</h2>
 * <ul>
 *   <li><b>Missing argument</b>: {@code "Missing required argument 'X' for tool 'T'. The 'X' parameter is needed to <purpose>."}</li>
 *   <li><b>Invalid argument</b>: {@code "Invalid argument 'X' for tool 'T': <reason>. Expected: <expected>."}</li>
 *   <li><b>Not found</b>: {@code "<ResourceType> '<name>' not found in <scope>. Available: <list>."}</li>
 *   <li><b>Permission denied</b>: {@code "Permission denied: tool 'T' is not allowed in <mode> mode. <reason>."}</li>
 *   <li><b>Partial match</b>: {@code "No exact match for '<query>'. Did you mean: <suggestion>?"}</li>
 *   <li><b>Tool error (recoverable)</b>: {@code "Tool 'T' failed: <message>. <suggestion for recovery>."}</li>
 * </ul>
 *
 * <p>Every error message ends with a concrete recovery suggestion so the
 * LLM has a clear next step. This is critical for autonomous recovery —
 * without it, the LLM often retries the same call expecting different
 * results.
 */
public final class ToolResultFormatter {

    private ToolResultFormatter() {}

    /** Standard "missing required argument" error. */
    public static String missingArgument(String toolName, String argName, String purpose) {
        StringBuilder sb = new StringBuilder();
        sb.append("Missing required argument '").append(argName).append("' for tool '").append(toolName).append("'.");
        if (purpose != null && !purpose.isEmpty()) {
            sb.append(" The '").append(argName).append("' parameter is needed to ").append(purpose).append(".");
        }
        sb.append(" Provide a value and retry.");
        return sb.toString();
    }

    /** Standard "invalid argument" error. */
    public static String invalidArgument(String toolName, String argName, String reason, String expected) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid argument '").append(argName).append("' for tool '").append(toolName).append("'");
        if (reason != null && !reason.isEmpty()) sb.append(": ").append(reason);
        sb.append(".");
        if (expected != null && !expected.isEmpty()) {
            sb.append(" Expected: ").append(expected).append(".");
        }
        sb.append(" Correct the value and retry.");
        return sb.toString();
    }

    /** Standard "not found" error. */
    public static String notFound(String resourceType, String name, String scope, String available) {
        StringBuilder sb = new StringBuilder();
        sb.append(resourceType == null ? "Resource" : resourceType);
        sb.append(" '").append(name).append("' not found");
        if (scope != null && !scope.isEmpty()) sb.append(" in ").append(scope);
        sb.append(".");
        if (available != null && !available.isEmpty()) {
            sb.append(" Available: ").append(available).append(".");
        }
        sb.append(" Use a valid name from the list above.");
        return sb.toString();
    }

    /** Standard "permission denied" error. */
    public static String permissionDenied(String toolName, String mode, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission denied: tool '").append(toolName).append("' is not allowed in ");
        sb.append(mode == null ? "current" : mode).append(" mode.");
        if (reason != null && !reason.isEmpty()) {
            sb.append(" ").append(reason);
        }
        sb.append(" Switch to ACT mode or request user approval.");
        return sb.toString();
    }

    /** Standard "partial match / did you mean" suggestion. */
    public static String didYouMean(String query, String[] candidates, int maxSuggestions) {
        if (candidates == null || candidates.length == 0) {
            return "No matches for '" + query + "'. No candidates available.";
        }
        // Simple Levenshtein-based suggestion.
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String c : candidates) {
            int d = levenshtein(query, c);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("No exact match for '").append(query).append("'.");
        if (best != null && bestDist <= Math.max(3, query.length() / 2)) {
            sb.append(" Did you mean: '").append(best).append("'?");
        } else {
            sb.append(" Closest candidates: ");
            int n = Math.min(maxSuggestions, candidates.length);
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append("'").append(candidates[i]).append("'");
            }
        }
        return sb.toString();
    }

    /** Standard "tool error" with recovery suggestion. */
    public static String toolError(String toolName, String message, String recovery) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool '").append(toolName).append("' failed: ").append(message == null ? "unknown error" : message);
        sb.append(".");
        if (recovery != null && !recovery.isEmpty()) {
            sb.append(" ").append(recovery);
        }
        return sb.toString();
    }

    /** Standard "tool success" with optional output. */
    public static String toolSuccess(String toolName, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(toolName).append("] ");
        sb.append(summary == null || summary.isEmpty() ? "OK" : summary);
        return sb.toString();
    }

    /** Format a list of strings for inclusion in an error message. */
    public static String formatList(String[] items, int max) {
        if (items == null || items.length == 0) return "(none)";
        int n = Math.min(max, items.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(items[i]);
        }
        if (items.length > n) sb.append(" (and ").append(items.length - n).append(" more)");
        return sb.toString();
    }

    /** Truncate a string for inclusion in an error message. */
    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(" + s.length() + " chars)";
    }

    /** Iterative Levenshtein distance. */
    private static int levenshtein(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[b.length()];
    }
}
