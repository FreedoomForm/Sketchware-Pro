package com.sketchware.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

/**
 * Unit tests for {@link JsonSchemaValidator}.
 */
public class JsonSchemaValidatorTest {

    @Test public void acceptsValidObjectWithRequiredFields() {
        JsonObject schema = schemaWithRequired("widget_type");
        JsonObject value = JsonParser.parseString("{\"widget_type\":\"Button\"}").getAsJsonObject();
        JsonSchemaValidator.ValidationResult r = JsonSchemaValidator.validate(value, schema);
        assertThat(r.ok).isTrue();
    }

    @Test public void rejectsMissingRequiredField() {
        JsonObject schema = schemaWithRequired("widget_type");
        JsonObject value = JsonParser.parseString("{}").getAsJsonObject();
        JsonSchemaValidator.ValidationResult r = JsonSchemaValidator.validate(value, schema);
        assertThat(r.ok).isFalse();
        assertThat(r.error).contains("widget_type");
    }

    @Test public void acceptsStringType() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        JsonSchemaValidator.ValidationResult r = JsonSchemaValidator.validate(JsonParser.parseString("\"hello\""), schema);
        assertThat(r.ok).isTrue();
    }

    @Test public void rejectsStringWhenObjectExpected() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonSchemaValidator.ValidationResult r =
                JsonSchemaValidator.validate(JsonParser.parseString("\"hello\""), schema);
        assertThat(r.ok).isFalse();
    }

    @Test public void validatesEnum() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.add("enum", JsonParser.parseString("[\"view\",\"component\",\"activity\"]"));
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("\"view\""), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("\"unknown\""), schema).ok).isFalse();
    }

    @Test public void validatesIntegerRange() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "integer");
        schema.addProperty("minimum", 0);
        schema.addProperty("maximum", 100);
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("50"), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("0"), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("100"), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("-1"), schema).ok).isFalse();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("101"), schema).ok).isFalse();
    }

    @Test public void validatesStringPattern() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("pattern", "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("\"com.example.app\""), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("\"Com.Example\""), schema).ok).isFalse();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("\"com\""), schema).ok).isFalse();
    }

    @Test public void validatesArrayItems() {
        JsonObject itemsSchema = new JsonObject();
        itemsSchema.addProperty("type", "string");
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "array");
        schema.add("items", itemsSchema);
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("[\"a\",\"b\"]"), schema).ok).isTrue();
        assertThat(JsonSchemaValidator.validate(JsonParser.parseString("[1,2]"), schema).ok).isFalse();
    }

    @Test public void nullSchemaAlwaysOk() {
        JsonSchemaValidator.ValidationResult r =
                JsonSchemaValidator.validate(JsonParser.parseString("42"), null);
        assertThat(r.ok).isTrue();
    }

    @Test public void nullValueRejectedWhenTypeSpecified() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        JsonSchemaValidator.ValidationResult r =
                JsonSchemaValidator.validate(JsonParser.parseString("null"), schema);
        assertThat(r.ok).isFalse();
    }

    private JsonObject schemaWithRequired(String field) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        props.add(field, prop);
        schema.add("properties", props);
        schema.add("required", JsonParser.parseString("[\"" + field + "\"]"));
        return schema;
    }
}
