package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;

import org.junit.Test;

/**
 * Unit tests for {@link ToolRegistry} and {@link ToolExecutor}.
 */
public class ToolRegistryTest {

    @Test public void registerAndLookupTool() {
        ToolRegistry r = new ToolRegistry();
        SketchwareTool tool = new FakeTool("test_tool", "test");
        r.register(tool);
        assertThat(r.has("test_tool")).isTrue();
        assertThat(r.get("test_tool")).isSameInstanceAs(tool);
        assertThat(r.size()).isEqualTo(1);
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateRegistrationThrows() {
        ToolRegistry r = new ToolRegistry();
        r.register(new FakeTool("dup", "test"));
        r.register(new FakeTool("dup", "test"));
    }

    @Test public void unknownToolReturnsNull() {
        ToolRegistry r = new ToolRegistry();
        assertThat(r.get("missing")).isNull();
        assertThat(r.has("missing")).isFalse();
    }

    @Test public void toJsonSchemasProducesValidJsonArray() {
        ToolRegistry r = new ToolRegistry();
        r.register(new FakeTool("a", "cat"));
        r.register(new FakeTool("b", "cat"));
        String json = r.toJsonSchemas();
        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        assertThat(json).contains("\"name\":\"a\"");
        assertThat(json).contains("\"name\":\"b\"");
        assertThat(json).contains("\"description\":\"fake\"");
        assertThat(json).contains("\"inputSchema\":");
    }

    @Test public void executorReturnsErrorForUnknownTool() {
        ToolRegistry r = new ToolRegistry();
        ToolExecutor ex = new ToolExecutor(r);
        ToolResult result = ex.execute("nonexistent", new JsonObject(), null);
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).contains("Unknown tool");
    }

    @Test public void executorCatchesToolException() {
        ToolRegistry r = new ToolRegistry();
        r.register(new ThrowingTool());
        ToolExecutor ex = new ToolExecutor(r);
        ToolResult result = ex.execute("throw", new JsonObject(), null);
        assertThat(result.isError()).isTrue();
        assertThat(result.getError()).contains("boom");
    }

    @Test public void executorReturnsSuccessOnHappyPath() {
        ToolRegistry r = new ToolRegistry();
        r.register(new FakeTool("good", "test"));
        ToolExecutor ex = new ToolExecutor(r);
        ToolResult result = ex.execute("good", new JsonObject(), null);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo("ok");
    }

    static final class FakeTool implements SketchwareTool {
        private final String name;
        private final String category;
        FakeTool(String name, String category) { this.name = name; this.category = category; }
        @Override public String name() { return name; }
        @Override public String description() { return "fake"; }
        @Override public String category() { return category; }
        @Override public JsonObject jsonSchema() {
            JsonObject s = new JsonObject();
            s.addProperty("type", "object");
            s.add("properties", new JsonObject());
            return s;
        }
        @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) {
            return ToolResult.success("ok");
        }
        @Override public boolean isReadOnly() { return true; }
    }

    static final class ThrowingTool implements SketchwareTool {
        @Override public String name() { return "throw"; }
        @Override public String description() { return "throws"; }
        @Override public String category() { return "test"; }
        @Override public JsonObject jsonSchema() { return new JsonObject(); }
        @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) {
            throw new RuntimeException("boom");
        }
    }
}
