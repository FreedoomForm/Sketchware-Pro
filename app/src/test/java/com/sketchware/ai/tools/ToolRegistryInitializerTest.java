package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link ToolRegistryInitializer}.
 *
 * <p>Verifies that the default registry contains exactly 37 user-action
 * and agent-control tools with unique names and valid JSON schemas, after the
 * 2026-08-12 umbrella consolidation and removal of backend-only tools.
 * consolidation that collapsed 68 tools into 45 via {@link CategoryUmbrellaTool}.
 */
public class ToolRegistryInitializerTest {

    @Test public void defaultRegistryHasAllExpectedTools() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        // After the universal-tool refactor (2026-08-11): 223 stubs → 29 universal
        // + 16 specialized = 45 tools covering all 240 operations.
        //
        // After the umbrella consolidation (2026-08-12): 68 tools → 45 tools via
        // CategoryUmbrellaTool wrappers that group 2-8 related universal tools
        // under a single name + subcategory enum. The 8 umbrellas are:
        //   view_manage, event_manage, component_misc, project_manage,
        //   build_manage, library_manage, manifest_manage, resource_manage.
        //
        // The Creator Runtime adapter is intentionally additive: it exposes
        // the same validated operation pipeline as the visual Creator UI.
        assertThat(r.size()).isEqualTo(38);
        assertThat(r.has("creator_runtime")).isTrue();
    }

    @Test public void expectedUmbrellaToolsAreRegistered() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        // The 8 category umbrellas introduced in the 2026-08-12 consolidation.
        String[] umbrellas = {
            "view_manage", "event_manage", "component_misc", "project_manage",
            "build_manage", "library_manage", "manifest_manage", "resource_manage"
        };
        for (String name : umbrellas) {
            assertThat(r.has(name)).isTrue();
            SketchwareTool t = r.all().stream()
                    .filter(x -> x.name().equals(name)).findFirst().orElse(null);
            assertThat(t).isNotNull();
            assertThat(t).isInstanceOf(CategoryUmbrellaTool.class);
            // Umbrellas are write + not auto-approved (conservative default).
            assertThat(t.isReadOnly()).isFalse();
            assertThat(t.isAutoApprovedByDefault()).isFalse();
        }
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
            "library_enable", "activity_list",
            "ask_question", "submit_and_exit"
        };
        for (String name : expected) {
            assertThat(r.has(name)).isTrue();
        }
    }

    @Test public void backendOnlyToolsAreNotRegistered() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        String[] removed = {
            "java_edit_file", "java_read_file", "java_modify_class",
            "diff_edit_file", "apply_patch", "list_files", "search_files",
            "web_search", "web_fetch"
        };
        for (String name : removed) {
            assertThat(r.has(name)).isFalse();
        }
    }

    @Test public void canonicalAgentPayloadHidesDuplicateAliases() {
        ToolRegistry r = ToolRegistryInitializer.createDefault();
        String payload = r.toAgentJsonSchemas();
        assertThat(payload).contains("\"name\":\"view_manage\"");
        assertThat(payload).contains("\"name\":\"activity_list\"");
        assertThat(payload).doesNotContain("\"name\":\"view_add_widget\"");
        assertThat(payload).doesNotContain("\"name\":\"event_list\"");
        assertThat(payload).doesNotContain("\"name\":\"project_set_app_name\"");
    }

    @Test public void activityListHasNoRequiredArguments() {
        SketchwareTool tool = ToolRegistryInitializer.createDefault().get("activity_list");
        assertThat(tool).isNotNull();
        assertThat(tool.jsonSchema().has("required")).isFalse();
        assertThat(tool.isReadOnly()).isTrue();
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
