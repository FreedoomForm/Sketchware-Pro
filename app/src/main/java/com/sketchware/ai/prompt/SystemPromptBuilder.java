package com.sketchware.ai.prompt;

import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.tools.ToolRegistry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Builds the system prompt for the AI agent. Mirrors Cline's
 * {@code DEFAULT_CLINE_SYSTEM_PROMPT} + template variables + MODE_TAG_INSTRUCTIONS.
 */
public final class SystemPromptBuilder {

    private SystemPromptBuilder() {}

    public static String build(AgentMode mode,
                               ToolRegistry tools,
                               String projectWorkspacePath,
                               String projectName,
                               String packageName,
                               int minSdk,
                               int targetSdk) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("You are Sketchware AI, an interactive agent integrated into the Sketchware-Pro Android app. ")
          .append("You help the user build Android apps visually by using the SAME tools the user can access through the Sketchware UI. ")
          .append("You MUST NEVER write JSON, Java, Kotlin, or XML directly to disk - you can ONLY invoke the tools provided to you. ")
          .append("Every change you make via a tool is reflected in real time in the Sketchware UI, so the user can see and verify each step.\n\n");

        // Workspace metadata
        sb.append("# Workspace Configuration\n");
        sb.append("- Platform: Sketchware-Pro Android\n");
        sb.append("- IDE: Sketchware-Pro\n");
        if (projectWorkspacePath != null) sb.append("- Project root: ").append(projectWorkspacePath).append("\n");
        if (projectName != null) sb.append("- Project name: ").append(projectName).append("\n");
        if (packageName != null) sb.append("- Package: ").append(packageName).append("\n");
        sb.append("- Min SDK: ").append(minSdk).append("\n");
        sb.append("- Target SDK: ").append(targetSdk).append("\n");
        sb.append("- Current date: ").append(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())).append("\n\n");

        // Operating rules
        sb.append("# Operating Rules\n");
        sb.append("1. ONLY use the tools provided. Never write code, JSON, or XML to disk directly.\n");
        sb.append("2. After each tool call, the user sees the change in real time in the Sketchware UI.\n");
        sb.append("3. Explain in the user's language what you did and what you plan to do next.\n");
        sb.append("4. If a tool returns 'library_required', enable the library first via library_enable.\n");
        sb.append("5. If a tool requires a widget ID you don't know, call view_list_widgets() first.\n");
        sb.append("6. Use ask_question(text) to clarify with the user when ambiguous.\n");
        sb.append("7. Use submit_and_exit(summary) to finish the task.\n");
        sb.append("8. Prefer the simplest tool that achieves the goal. Avoid chained edits when one tool call suffices.\n");
        sb.append("9. When generating Java code via java_edit_file, use the project's package name as the package declaration.\n");
        sb.append("10. Be conservative: ask before performing destructive operations (delete widget, delete file, reset blocks).\n\n");

        // Mode tag instructions
        sb.append("# Mode\n");
        if (mode == AgentMode.PLAN) {
            sb.append("You are in PLAN mode. Do not invoke any write tools. ")
              .append("Read the project, ask clarifying questions, and produce a step-by-step plan. ")
              .append("When the user approves, switch to ACT mode.\n");
        } else if (mode == AgentMode.YOLO) {
            sb.append("You are in YOLO mode. All tool calls are auto-approved. Be efficient but careful.\n");
        } else {
            sb.append("You are in ACT mode. Write operations require user approval. ")
              .append("Explain each step before performing it so the user understands what will happen.\n");
        }
        sb.append("\n");

        // Tools list
        if (tools != null && tools.size() > 0) {
            sb.append("# Tools\n\n");
            sb.append("You have access to the following tools. Each tool name is in `code`. ")
              .append("Arguments are JSON objects with the listed properties.\n\n");

            String currentCategory = "";
            for (com.sketchware.ai.tools.SketchwareTool t : tools.all()) {
                if (!t.category().equals(currentCategory)) {
                    currentCategory = t.category();
                    sb.append("\n## ").append(currentCategory).append("\n\n");
                }
                sb.append("- `").append(t.name()).append("`");
                if (t.isReadOnly()) sb.append(" (read-only)");
                sb.append(": ").append(t.description()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("# Communication\n");
        sb.append("Address the user in their language. Use clear, concise language. ")
          .append("When you finish a tool call, briefly state the result (one sentence). ")
          .append("When you finish the task, summarise what was done.\n");

        return sb.toString();
    }
}
