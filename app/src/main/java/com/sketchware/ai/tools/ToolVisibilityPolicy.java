package com.sketchware.ai.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines the public AI capability surface separately from the backward-compatible
 * registry lookup surface. Alias tools remain executable for old conversations,
 * but the model and catalog receive one canonical route per capability family.
 */
public final class ToolVisibilityPolicy {
    private static final Set<String> HIDDEN_AGENT_ALIASES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "view_add_widget", "view_set_property", "view_delete_widget", "view_list_widgets",
            "view_undo", "view_redo",
            "event_attach", "event_list",
            "component_add",
            "project_set_app_name", "project_set_package_name",
            "library_enable"
    )));

    private ToolVisibilityPolicy() { }

    public static boolean isAgentVisible(SketchwareTool tool) {
        return tool != null && !HIDDEN_AGENT_ALIASES.contains(tool.name());
    }

    public static boolean isCatalogVisible(SketchwareTool tool) {
        return isAgentVisible(tool) && tool.category() != null
                && !"meta".equalsIgnoreCase(tool.category());
    }

    public static String canonicalKey(SketchwareTool tool) {
        if (tool == null) return "";
        String name = tool.name();
        if (HIDDEN_AGENT_ALIASES.contains(name)) {
            if (name.startsWith("view_")) return "view_manage";
            if (name.startsWith("event_")) return "event_manage";
            if ("component_add".equals(name)) return "component_misc";
            if (name.startsWith("project_set_")) return "project_manage";
            if ("library_enable".equals(name)) return "library_manage";
        }
        return name;
    }

    public static List<SketchwareTool> canonicalTools(ToolRegistry registry) {
        if (registry == null) return Collections.emptyList();
        java.util.ArrayList<SketchwareTool> result = new java.util.ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (SketchwareTool tool : registry.all()) {
            if (!isAgentVisible(tool)) continue;
            if (keys.add(canonicalKey(tool))) result.add(tool);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<SketchwareTool> catalogTools(ToolRegistry registry) {
        if (registry == null) return Collections.emptyList();
        java.util.ArrayList<SketchwareTool> result = new java.util.ArrayList<>();
        for (SketchwareTool tool : canonicalTools(registry)) {
            if (isCatalogVisible(tool)) result.add(tool);
        }
        return Collections.unmodifiableList(result);
    }
}
