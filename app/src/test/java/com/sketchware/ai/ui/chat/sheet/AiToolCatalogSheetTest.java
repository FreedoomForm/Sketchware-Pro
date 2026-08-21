package com.sketchware.ai.ui.chat.sheet;

import static com.google.common.truth.Truth.assertThat;

import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.ToolRegistry;
import com.sketchware.ai.tools.ToolRegistryInitializer;
import com.sketchware.ai.tools.ToolVisibilityPolicy;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AiToolCatalogSheetTest {

    @Test public void groupsEveryCanonicalVisibleToolWithoutDuplicates() {
        ToolRegistry registry = ToolRegistryInitializer.createDefault();
        Map<String, List<SketchwareTool>> groups = AiToolCatalogSheet.groupByCategory(registry);

        int groupedCount = 0;
        for (List<SketchwareTool> tools : groups.values()) groupedCount += tools.size();

        assertThat(groupedCount).isEqualTo(ToolVisibilityPolicy.catalogTools(registry).size());
        assertThat(groups.keySet()).contains("view");
        assertThat(groups.keySet()).contains("project");
        assertThat(groups.keySet()).doesNotContain("meta");
        java.util.Set<String> visibleNames = new java.util.HashSet<>();
        for (List<SketchwareTool> tools : groups.values()) {
            for (SketchwareTool tool : tools) visibleNames.add(tool.name());
        }
        assertThat(visibleNames).contains("activity_list");
        assertThat(visibleNames).doesNotContain("view_add_widget");
    }

    @Test public void summaryExplainsToolCountAndApprovalBoundary() {
        ToolRegistry registry = ToolRegistryInitializer.createDefault();
        String summary = AiToolCatalogSheet.summary(registry);

        assertThat(summary).contains("Editor tools (");
        assertThat(summary).doesNotContain("java_edit_file");
        assertThat(summary).contains("Read-only tools run automatically");
        assertThat(summary).contains("Tap the tools button");
    }
}
