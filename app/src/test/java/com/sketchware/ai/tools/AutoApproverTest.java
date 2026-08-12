package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Unit tests for {@link AutoApprover}, focusing on the subcategory-aware
 * rule matching introduced in the 2026-08-12 umbrella consolidation.
 */
public class AutoApproverTest {

    @Test public void perToolSubcategoryRuleMatches() {
        AutoApprover a = new AutoApprover();
        a.addRule(new AutoApprover.Rule("manifest_manage", "appcompat", null, null,
                AutoApprover.Decision.AUTO_APPROVE));
        SketchwareTool umbrella = new FakeTool("manifest_manage");
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "appcompat");
        args.addProperty("action", "enable");
        assertThat(a.decide(umbrella, args)).isEqualTo(AutoApprover.Decision.AUTO_APPROVE);
    }

    @Test public void perToolSubcategoryRuleDoesNotMatchOtherSubcategory() {
        AutoApprover a = new AutoApprover();
        a.addRule(new AutoApprover.Rule("manifest_manage", "appcompat", null, null,
                AutoApprover.Decision.AUTO_APPROVE));
        SketchwareTool umbrella = new FakeTool("manifest_manage");
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "manifest");  // different subcategory
        args.addProperty("action", "add_permission");
        // No rule matches manifest_manage:manifest → falls back to tool default.
        assertThat(a.decide(umbrella, args)).isEqualTo(AutoApprover.Decision.REQUIRE_APPROVAL);
    }

    @Test public void perToolActionRuleStillMatches() {
        AutoApprover a = new AutoApprover();
        // Per-action rule uses exact-match semantics. After the subcategory
        // support was added to Rule.matches, the action field still works
        // the same way — this test verifies that adding subcategory support
        // did not break the pre-existing per-action matching path.
        a.addRule(new AutoApprover.Rule(null, null, "list_events", null,
                AutoApprover.Decision.AUTO_APPROVE));
        SketchwareTool umbrella = new FakeTool("view_manage");
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "widget");
        args.addProperty("action", "list_events");
        assertThat(a.decide(umbrella, args)).isEqualTo(AutoApprover.Decision.AUTO_APPROVE);
    }

    @Test public void withDefaultsAutoApprovesListActions() {
        AutoApprover a = AutoApprover.withDefaults();
        SketchwareTool umbrella = new FakeTool("view_manage");
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "widget");
        args.addProperty("action", "list_events");
        assertThat(a.decide(umbrella, args)).isEqualTo(AutoApprover.Decision.AUTO_APPROVE);
    }

    @Test public void withDefaultsRequiresApprovalForManifestUmbrella() {
        AutoApprover a = AutoApprover.withDefaults();
        SketchwareTool umbrella = new FakeTool("manifest_manage");
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "manifest");
        args.addProperty("action", "add_permission");
        // withDefaults has a manifest_manage umbrella-level rule → REQUIRE_APPROVAL.
        assertThat(a.decide(umbrella, args)).isEqualTo(AutoApprover.Decision.REQUIRE_APPROVAL);
    }

    @Test public void withDefaultsRequiresApprovalForResourceDeleteActions() {
        AutoApprover a = AutoApprover.withDefaults();
        SketchwareTool umbrella = new FakeTool("resource_manage");
        JsonObject deleteArgs = new JsonObject();
        deleteArgs.addProperty("subcategory", "assets");
        deleteArgs.addProperty("action", "delete");
        assertThat(a.decide(umbrella, deleteArgs)).isEqualTo(AutoApprover.Decision.REQUIRE_APPROVAL);

        JsonObject listArgs = new JsonObject();
        listArgs.addProperty("subcategory", "assets");
        listArgs.addProperty("action", "list");
        // The "list" action auto-approve rule has no subcategory constraint.
        assertThat(a.decide(umbrella, listArgs)).isEqualTo(AutoApprover.Decision.AUTO_APPROVE);
    }

    static final class FakeTool implements SketchwareTool {
        private final String name;
        FakeTool(String name) { this.name = name; }
        @Override public String name() { return name; }
        @Override public String description() { return "fake umbrella for testing"; }
        @Override public String category() { return "test"; }
        @Override public JsonObject jsonSchema() { return new JsonObject(); }
        @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) {
            return ToolResult.success("ok");
        }
        @Override public boolean isReadOnly() { return false; }
        @Override public boolean isAutoApprovedByDefault() { return false; }
    }
}
