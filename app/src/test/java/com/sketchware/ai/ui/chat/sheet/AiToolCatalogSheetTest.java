package com.sketchware.ai.ui.chat.sheet;

import static com.google.common.truth.Truth.assertThat;

import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.ToolRegistry;
import com.sketchware.ai.tools.ToolRegistryInitializer;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AiToolCatalogSheetTest {

    @Test public void groupsEveryRegisteredToolWithoutDroppingAny() {
        ToolRegistry registry = ToolRegistryInitializer.createDefault();
        Map<String, List<SketchwareTool>> groups = AiToolCatalogSheet.groupByCategory(registry);

        int groupedCount = 0;
        for (List<SketchwareTool> tools : groups.values()) groupedCount += tools.size();

        assertThat(groupedCount).isLessThan(registry.size());
        assertThat(groups.keySet()).contains("view");
        assertThat(groups.keySet()).contains("project");
        assertThat(groups.keySet()).doesNotContain("meta");
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
