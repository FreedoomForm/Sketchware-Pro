package com.sketchware.ai.prompt;

import static com.google.common.truth.Truth.assertThat;

import com.sketchware.ai.agent.AgentMode;
import com.sketchware.ai.tools.ToolRegistryInitializer;

import org.junit.Test;

/** Ensures prompt examples use the public Sketchware tool contracts, not stale subtool names. */
public class SystemPromptBuilderTest {

    @Test public void planPromptUsesThePublicLayoutUmbrellaContract() {
        String prompt = SystemPromptBuilder.build(AgentMode.PLAN,
                ToolRegistryInitializer.createDefault(), "/project", "Demo", "com.example", 26, 36);

        assertThat(prompt).contains("view_manage(subcategory=\"layout\", action=\"list\")");
        assertThat(prompt).doesNotContain("view_manage_layout");
        assertThat(prompt).doesNotContain("view_list_layouts");
        assertThat(prompt).doesNotContain("(view_manage_layout)");
        assertThat(prompt).doesNotContain("run_commands tool remains available");
    }

    @Test public void promptTellsAgentToUseToolResultsAsTheSourceOfTruth() {
        String prompt = SystemPromptBuilder.build(AgentMode.ACT,
                ToolRegistryInitializer.createDefault(), "/project", "Demo", "com.example", 26, 36);

        assertThat(prompt).contains("Treat every tool result as the source of truth");
        assertThat(prompt).contains("never call their hidden implementation names directly");
    }
}
