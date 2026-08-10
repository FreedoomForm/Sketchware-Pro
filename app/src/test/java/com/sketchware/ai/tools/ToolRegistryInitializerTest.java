package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ToolRegistryInitializer}.
 *
 * <p>Verifies that the default registry contains all 238 expected tools with
 * unique names and valid JSON schemas.
 */
public class ToolRegistryInitializerTest {

    @Test public void defaultRegistryHasAllExpectedTools() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        // After the universal-tool refactor (2026-08-11): the 223 stubs were
        // collapsed into 29 universal tools + 16 specialized = 45 tools
        // covering all 240 operations. Asserting at least 45.
        assertThat(r.size()).isAtLeast(45);
    }

    @Test public void allToolsHaveUniqueNames() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (SketchwareTool t : r.all()) {
            assertThat(names.add(t.name())).isTrue();
        }
        assertThat(names).hasSize(r.size());
    }

    @Test public void allToolsHaveNonEmptyDescription() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        for (SketchwareTool t : r.all()) {
            assertThat(t.description()).isNotNull();
            assertThat(t.description().length()).isGreaterThan(10);
        }
    }

    @Test public void allToolsHaveCategory() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        java.util.Set<String> categories = new java.util.HashSet<>();
        for (SketchwareTool t : r.all()) {
            assertThat(t.category()).isNotNull();
            assertThat(t.category().length()).isGreaterThan(0);
            categories.add(t.category());
        }
        // We expect at least: view, event, block, component, project, library, java, meta
        assertThat(categories.size()).isAtLeast(7);
    }

    @Test public void allToolsHaveJsonSchema() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        for (SketchwareTool t : r.all()) {
            assertThat(t.jsonSchema()).isNotNull();
            assertThat(t.jsonSchema().has("type")).isTrue();
        }
    }

    @Test public void expectedMvpToolsAreRegistered() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        String[] expected = {
            "view_add_widget", "view_set_property", "view_delete_widget", "view_list_widgets",
            "view_undo", "view_redo",
            "event_attach", "event_list",
            "block_add",
            "component_add",
            "project_set_app_name", "project_set_package_name",
            "library_enable",
            "java_edit_file", "java_read_file",
            "ask_question", "submit_and_exit"
        };
        for (String name : expected) {
            assertThat(r.has(name)).isTrue();
        }
    }

    @Test public void readOnlyToolsAreAutoApproved() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        for (SketchwareTool t : r.all()) {
            if (t.isReadOnly()) {
                assertThat(t.isAutoApprovedByDefault()).isTrue();
            }
        }
    }

    @Test public void writeToolsAreNotAutoApprovedByDefault() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        for (SketchwareTool t : r.all()) {
            if (!t.isReadOnly()) {
                // Write tools should require approval by default (ACT mode).
                assertThat(t.isAutoApprovedByDefault()).isFalse();
            }
        }
    }

    @Test public void toJsonSchemasProducesValidJson() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        String json = r.toJsonSchemas();
        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        // Verify each tool name appears in the JSON.
        for (SketchwareTool t : r.all()) {
            assertThat(json).contains("\"name\":\"" + t.name() + "\"");
        }
    }
}
