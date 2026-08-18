package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/** Regression coverage for umbrella calls that carry action-specific arguments. */
public class CategoryUmbrellaToolTest {

    @Test public void umbrellaSchemaAllowsForwardedActionArgumentsAtTheObjectLevel() {
        ToolRegistry registry = ToolRegistryInitializer.createDefault();
        JsonObject schema = registry.get("view_manage").jsonSchema();

        assertThat(schema.get("additionalProperties").getAsBoolean()).isTrue();
        assertThat(schema.getAsJsonObject("properties").has("additionalProperties")).isFalse();
    }

    @Test public void toolInferenceRecognizesAnUmbrellaWithActionSpecificArguments() {
        ToolRegistry registry = ToolRegistryInitializer.createDefault();
        SketchwareTool inferred = registry.inferFromArgs(
                "{\"subcategory\":\"layout\",\"action\":\"create\",\"name\":\"details\"}");

        assertThat(inferred).isNotNull();
        assertThat(inferred.name()).isEqualTo("view_manage");
    }

    @Test public void umbrellaForwardsActionSpecificArgumentsWithoutLeakingSubcategory() throws Exception {
        final JsonObject[] received = new JsonObject[1];
        SketchwareTool layoutTool = new SketchwareTool() {
            @Override public String name() { return "hidden_layout_tool"; }
            @Override public String description() { return "Test layout operation."; }
            @Override public String category() { return "view"; }
            @Override public JsonObject jsonSchema() {
                JsonObject schema = new JsonObject();
                schema.addProperty("type", "object");
                return schema;
            }
            @Override public ToolResult execute(JsonObject args, SketchwareToolContext context) {
                received[0] = args;
                return ToolResult.success("forwarded");
            }
        };
        Map<String, SketchwareTool> subtools = new LinkedHashMap<>();
        subtools.put("layout", layoutTool);
        CategoryUmbrellaTool umbrella = new CategoryUmbrellaTool(
                "view_manage", "view", "Test umbrella.", subtools);
        JsonObject args = new JsonObject();
        args.addProperty("subcategory", "layout");
        args.addProperty("action", "create");
        args.addProperty("name", "details");

        ToolResult result = umbrella.execute(args, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(received[0]).isNotNull();
        assertThat(received[0].has("subcategory")).isFalse();
        assertThat(received[0].get("action").getAsString()).isEqualTo("create");
        assertThat(received[0].get("name").getAsString()).isEqualTo("details");
    }
}
