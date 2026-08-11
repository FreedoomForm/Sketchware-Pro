package com.sketchware.ai.tools;

import com.google.gson.JsonObject;

/**
 * Enhanced permission gate that supports per-tool, per-action, and per-path
 * rules. Mirrors Cline's {@code AutoApprove} + {@code AutoApprovalSettings}.
 *
 * <p>This is a backwards-compatible replacement for {@link ToolPermissionGate}
 * that adds finer-grained control:
 *
 * <ul>
 *   <li><b>Per-tool rules</b>: auto-approve or deny a specific tool regardless of its default.</li>
 *   <li><b>Per-action rules</b>: for universal tools, auto-approve or deny specific actions
 *       (e.g. auto-approve {@code view_list_widgets} but require approval for {@code view_delete_widget}).</li>
 *   <li><b>Per-path rules</b>: for file-editing tools, auto-approve edits to specific paths
 *       (e.g. auto-approve edits under {@code resource/values/} but require approval for
 *       {@code AndroidManifest.xml}).</li>
 *   <li><b>Per-mode rules</b>: in YOLO mode everything is auto-approved; in PLAN mode only
 *       read-only tools are allowed.</li>
 * </ul>
 *
 * <h2>Resolution order</h2>
 * <ol>
 *   <li>If mode is YOLO -&gt; AUTO_APPROVE.</li>
 *   <li>If mode is PLAN and tool is not read-only -&gt; DENY.</li>
 *   <li>If a per-tool+action+path rule matches -&gt; use it.</li>
 *   <li>If a per-tool+action rule matches -&gt; use it.</li>
 *   <li>If a per-tool rule matches -&gt; use it.</li>
 *   <li>Otherwise fall back to the tool's default.</li>
 * </ol>
 *
 * <p>Thread-safety: not synchronized. Intended for use from a single thread
 * (the agent loop). Settings updates from the UI thread should be posted to
 * the agent thread.
 */
public class AutoApprover {

    public enum Decision { AUTO_APPROVE, REQUIRE_APPROVAL, DENY }

    /** A rule that matches a (tool, action, pathPrefix) tuple. */
    public static final class Rule {
        public final String toolName;       // null = wildcard
        public final String action;         // null = wildcard
        public final String pathPrefix;     // null = wildcard
        public final Decision decision;

        public Rule(String toolName, String action, String pathPrefix, Decision decision) {
            this.toolName = toolName;
            this.action = action;
            this.pathPrefix = pathPrefix;
            this.decision = decision;
        }

        boolean matches(String toolName, String action, String path) {
            if (this.toolName != null && !this.toolName.equals(toolName)) return false;
            if (this.action != null && !this.action.equals(action)) return false;
            if (this.pathPrefix != null) {
                if (path == null) return false;
                if (!path.startsWith(this.pathPrefix)) return false;
            }
            return true;
        }
    }

    private final java.util.List<Rule> rules = new java.util.ArrayList<>();
    private com.sketchware.ai.agent.AgentMode mode = com.sketchware.ai.agent.AgentMode.ACT;

    public void setMode(com.sketchware.ai.agent.AgentMode mode) {
        this.mode = mode;
    }

    public com.sketchware.ai.agent.AgentMode getMode() { return mode; }

    /** Add a rule. Rules are evaluated in the order added; first match wins. */
    public void addRule(Rule rule) {
        if (rule != null) rules.add(rule);
    }

    /** Remove all rules matching the given tool name (or all if null). */
    public void clearRules(String toolName) {
        if (toolName == null) {
            rules.clear();
            return;
        }
        rules.removeIf(r -> toolName.equals(r.toolName));
    }

    /** Get a snapshot of all rules. */
    public java.util.List<Rule> getRules() {
        return new java.util.ArrayList<>(rules);
    }

    /**
     * Decide whether a tool invocation should be auto-approved, require
     * approval, or be denied.
     *
     * @param tool   the tool being invoked.
     * @param args   the parsed arguments (used to extract action and path).
     * @return the decision.
     */
    public Decision decide(SketchwareTool tool, JsonObject args) {
        if (tool == null) return Decision.DENY;
        if (mode == com.sketchware.ai.agent.AgentMode.YOLO) return Decision.AUTO_APPROVE;
        if (mode == com.sketchware.ai.agent.AgentMode.PLAN) {
            return tool.isReadOnly() ? Decision.AUTO_APPROVE : Decision.DENY;
        }

        String action = extractAction(args);
        String path = extractPath(args);

        // First-match-wins over rules.
        for (Rule r : rules) {
            if (r.matches(tool.name(), action, path)) {
                return r.decision;
            }
        }
        return tool.isAutoApprovedByDefault() ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
    }

    /** Convenience overload without args (no action/path rules apply). */
    public Decision decide(SketchwareTool tool) {
        return decide(tool, null);
    }

    /** Extract the {@code action} argument from a universal tool's args. */
    private static String extractAction(JsonObject args) {
        if (args == null) return null;
        if (args.has("action") && !args.get("action").isJsonNull()) {
            return args.get("action").getAsString();
        }
        return null;
    }

    /** Extract the {@code file_path} or {@code path} argument for path-based rules. */
    private static String extractPath(JsonObject args) {
        if (args == null) return null;
        if (args.has("file_path") && !args.get("file_path").isJsonNull()) {
            return args.get("file_path").getAsString();
        }
        if (args.has("path") && !args.get("path").isJsonNull()) {
            return args.get("path").getAsString();
        }
        return null;
    }

    // -------- Convenience factory methods for common rule sets --------

    /**
     * Build a default rule set that's a sensible starting point for most users.
     * Auto-approves all read-only tools + safe mutations (list, get, show_source),
     * requires approval for destructive ops (delete, reset, clear).
     */
    public static AutoApprover withDefaults() {
        AutoApprover a = new AutoApprover();
        // Auto-approve read-only actions on universal tools.
        a.addRule(new Rule(null, "list", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, "get", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, "show_source", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, "list_presets", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, "get_current", null, Decision.AUTO_APPROVE));
        // Require approval for destructive actions.
        a.addRule(new Rule(null, "delete", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule(null, "reset", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule(null, "clear", null, Decision.REQUIRE_APPROVAL));
        // Always require approval for manifest edits.
        a.addRule(new Rule("manifest_manage", null, null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("appcompat_manage", null, null, Decision.REQUIRE_APPROVAL));
        // Require approval for Java/package name changes.
        a.addRule(new Rule("project_set_package_name", null, null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("project_set_app_name", null, null, Decision.REQUIRE_APPROVAL));
        return a;
    }
}
