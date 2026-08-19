package com.sketchware.ai.tools;

import com.google.gson.JsonObject;

/**
 * Enhanced permission gate that supports per-tool, per-action, per-subcategory,
 * and per-path rules. Mirrors Cline's {@code AutoApprove} +
 * {@code AutoApprovalSettings}.
 *
 * <p>This is a backwards-compatible replacement for {@link ToolPermissionGate}
 * that adds finer-grained control:
 *
 * <ul>
 *   <li><b>Per-tool rules</b>: auto-approve or deny a specific tool regardless of its default.</li>
 *   <li><b>Per-action rules</b>: for universal tools, auto-approve or deny specific actions
 *       (e.g. auto-approve {@code view_list_widgets} but require approval for {@code view_delete_widget}).</li>
 *   <li><b>Per-subcategory rules</b>: for {@link CategoryUmbrellaTool} umbrellas,
 *       auto-approve or deny specific subcategories (e.g. auto-approve
 *       {@code resource_manage:values_xml} but require approval for
 *       {@code resource_manage:assets}).</li>
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
 *   <li>If a per-tool+subcategory+action+path rule matches -&gt; use it.</li>
 *   <li>If a per-tool+subcategory+action rule matches -&gt; use it.</li>
 *   <li>If a per-tool+subcategory rule matches -&gt; use it.</li>
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

    /** A rule that matches a (tool, subcategory, action, pathPrefix) tuple. */
    public static final class Rule {
        public final String toolName;       // null = wildcard
        public final String subcategory;    // null = wildcard (umbrella subcategory)
        public final String action;         // null = wildcard
        public final String pathPrefix;     // null = wildcard
        public final Decision decision;

        public Rule(String toolName, String action, String pathPrefix, Decision decision) {
            // Backwards-compatible 4-arg constructor — subcategory is null.
            this(toolName, null, action, pathPrefix, decision);
        }

        public Rule(String toolName, String subcategory, String action,
                    String pathPrefix, Decision decision) {
            this.toolName = toolName;
            this.subcategory = subcategory;
            this.action = action;
            this.pathPrefix = pathPrefix;
            this.decision = decision;
        }

        boolean matches(String toolName, String subcategory, String action, String path) {
            if (this.toolName != null && !this.toolName.equals(toolName)) return false;
            if (this.subcategory != null && !this.subcategory.equals(subcategory)) return false;
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
     * @param args   the parsed arguments (used to extract subcategory, action, and path).
     * @return the decision.
     */
    public Decision decide(SketchwareTool tool, JsonObject args) {
        if (tool == null) return Decision.DENY;
        if (mode == com.sketchware.ai.agent.AgentMode.YOLO) return Decision.AUTO_APPROVE;
        // Both RESEARCH and PLAN modes are read-only — only tools whose
        // isReadOnly() flag is true are allowed. RESEARCH is identical to
        // PLAN from the permission-gate perspective; the difference is the
        // prompt contract (research produces a <research_summary>, plan
        // produces a plan). Mirrors Cline 3.x's research-mode gate.
        if (mode == com.sketchware.ai.agent.AgentMode.RESEARCH
                || mode == com.sketchware.ai.agent.AgentMode.PLAN) {
            return tool.isReadOnly() ? Decision.AUTO_APPROVE : Decision.DENY;
        }

        String subcategory = extractSubcategory(args);
        String action = extractAction(args);
        String path = extractPath(args);

        // First-match-wins over rules.
        for (Rule r : rules) {
            if (r.matches(tool.name(), subcategory, action, path)) {
                return r.decision;
            }
        }
        return tool.isAutoApprovedByDefault() ? Decision.AUTO_APPROVE : Decision.REQUIRE_APPROVAL;
    }

    /** Convenience overload without args (no subcategory/action/path rules apply). */
    public Decision decide(SketchwareTool tool) {
        return decide(tool, null);
    }

    /** Extract the {@code subcategory} argument from an umbrella tool's args. */
    private static String extractSubcategory(JsonObject args) {
        if (args == null) return null;
        if (args.has("subcategory") && !args.get("subcategory").isJsonNull()) {
            return args.get("subcategory").getAsString();
        }
        return null;
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
     *
     * <p>Updated for the 2026-08-12 umbrella consolidation: rules now target
     * umbrella tool names (e.g. {@code manifest_manage} instead of the old
     * standalone {@code manifest_manage}/{@code appcompat_manage}/{@code xml_command_manage}).
     * Subcategory-specific rules use the new 5-arg constructor.
     */
    public static AutoApprover withDefaults() {
        AutoApprover a = new AutoApprover();
        // Auto-approve read-only actions on universal + umbrella tools.
        a.addRule(new Rule(null, null, "list", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "get", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "show_source", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "list_presets", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "get_current", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "get_rules", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "list_categories", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "list_widgets", null, Decision.AUTO_APPROVE));
        a.addRule(new Rule(null, null, "list_events", null, Decision.AUTO_APPROVE));
        // Require approval for destructive actions.
        a.addRule(new Rule(null, null, "delete", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule(null, null, "reset", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule(null, null, "clear", null, Decision.REQUIRE_APPROVAL));
        // Always require approval for manifest edits (umbrella-level rule).
        a.addRule(new Rule("manifest_manage", null, null, null, Decision.REQUIRE_APPROVAL));
        // Java/package name changes always require approval.
        a.addRule(new Rule("project_set_package_name", null, null, null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("project_set_app_name", null, null, null, Decision.REQUIRE_APPROVAL));
        // ProGuard rule edits require approval (build_manage:proguard:edit_rules).
        a.addRule(new Rule("build_manage", "proguard", "edit_rules", null, Decision.REQUIRE_APPROVAL));
        // Asset/resource deletions require approval (umbrella subcategory-specific).
        a.addRule(new Rule("resource_manage", "assets", "delete", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("resource_manage", "resource_file", "delete", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("resource_manage", "font", "delete", null, Decision.REQUIRE_APPROVAL));
        a.addRule(new Rule("resource_manage", "sound", "delete", null, Decision.REQUIRE_APPROVAL));
        return a;
    }
}
