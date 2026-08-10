package com.sketchware.ai.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON Schema validator. Cline uses Zod on the TypeScript side; on
 * Java we don't pull in a full JSON Schema library, we do structural checks
 * for the patterns our tools actually use.
 *
 * <p>Supported keywords: type, required, properties, enum, items, minimum,
 * maximum, minLength, maxLength, pattern.
 */
public final class JsonSchemaValidator {

    private JsonSchemaValidator() {}

    public static ValidationResult validate(JsonElement value, JsonObject schema) {
        if (schema == null) return ValidationResult.ok();
        return validateValue(value, schema, "");
    }

    private static ValidationResult validateValue(JsonElement value, JsonObject schema, String path) {
        if (value == null || value.isJsonNull()) {
            if (schema.has("type")) {
                // null is not allowed if a type is specified
                return ValidationResult.error(path + " is null but schema requires " + schema.get("type"));
            }
            return ValidationResult.ok();
        }

        if (schema.has("type")) {
            String type = schema.get("type").getAsString();
            String err = checkType(value, type, path);
            if (err != null) return ValidationResult.error(err);
        }

        if (schema.has("enum") && value.isJsonPrimitive()) {
            JsonArray allowed = schema.getAsJsonArray("enum");
            boolean ok = false;
            for (JsonElement e : allowed) {
                if (e.equals(value)) { ok = true; break; }
            }
            if (!ok) return ValidationResult.error(path + " value " + value + " not in enum " + allowed);
        }

        if (value.isJsonObject() && schema.has("properties")) {
            JsonObject obj = value.getAsJsonObject();
            JsonObject props = schema.getAsJsonObject("properties");
            if (schema.has("required")) {
                for (JsonElement r : schema.getAsJsonArray("required")) {
                    String name = r.getAsString();
                    if (!obj.has(name)) return ValidationResult.error(path + " missing required property: " + name);
                }
            }
            for (Map.Entry<String, JsonElement> e : props.entrySet()) {
                if (obj.has(e.getKey())) {
                    ValidationResult v = validateValue(obj.get(e.getKey()), e.getValue().getAsJsonObject(), path + "." + e.getKey());
                    if (!v.ok) return v;
                }
            }
        }

        if (value.isJsonArray() && schema.has("items")) {
            JsonObject itemSchema = schema.getAsJsonObject("items");
            int i = 0;
            for (JsonElement item : value.getAsJsonArray()) {
                ValidationResult v = validateValue(item, itemSchema, path + "[" + i + "]");
                if (!v.ok) return v;
                i++;
            }
        }

        if (value.isJsonPrimitive() && value.isJsonPrimitive()) {
            JsonPrimitive p = value.getAsJsonPrimitive();
            if (schema.has("minimum") && p.isNumber()) {
                if (p.getAsDouble() < schema.get("minimum").getAsDouble()) {
                    return ValidationResult.error(path + " value " + p + " < minimum " + schema.get("minimum"));
                }
            }
            if (schema.has("maximum") && p.isNumber()) {
                if (p.getAsDouble() > schema.get("maximum").getAsDouble()) {
                    return ValidationResult.error(path + " value " + p + " > maximum " + schema.get("maximum"));
                }
            }
            if (schema.has("minLength") && p.isString()) {
                if (p.getAsString().length() < schema.get("minLength").getAsInt()) {
                    return ValidationResult.error(path + " too short");
                }
            }
            if (schema.has("maxLength") && p.isString()) {
                if (p.getAsString().length() > schema.get("maxLength").getAsInt()) {
                    return ValidationResult.error(path + " too long");
                }
            }
            if (schema.has("pattern") && p.isString()) {
                String pat = schema.get("pattern").getAsString();
                if (!p.getAsString().matches(pat)) {
                    return ValidationResult.error(path + " does not match pattern " + pat);
                }
            }
        }

        return ValidationResult.ok();
    }

    private static String checkType(JsonElement value, String type, String path) {
        switch (type) {
            case "string":  return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? null : path + " is not a string";
            case "number":  return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()  ? null : path + " is not a number";
            case "integer": return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && (value.getAsJsonPrimitive().getAsDouble() % 1 == 0) ? null : path + " is not an integer";
            case "boolean": return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() ? null : path + " is not a boolean";
            case "object":  return value.isJsonObject()  ? null : path + " is not an object";
            case "array":   return value.isJsonArray()   ? null : path + " is not an array";
            default:        return null; // unknown types pass
        }
    }

    public static final class ValidationResult {
        public final boolean ok;
        public final String error;
        private ValidationResult(boolean ok, String error) {
            this.ok = ok; this.error = error;
        }
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg); }
    }
}
