package com.sketchware.ai.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Category-level umbrella tool that dispatches to one of several underlying
 * {@link SketchwareTool}s based on a {@code subcategory} parameter.
 *
 * <p>This is the consolidation mechanism that collapses the 68-tool
 * catalogue down to 45 tools while preserving full functional coverage.
 * Each umbrella groups 2-8 semantically related universal tools under a
 * single name (e.g. {@code view_manage} wraps layout/widget/palette/
 * visibility/viewfunc sub-tools; {@code project_manage} wraps manage/
 * set_property/enable_feature/theme).
 *
 * <p>JSON schema exposed to the LLM:
 * <pre>{@code
 * {
 *   "type": "object",
 *   "properties": {
 *     "subcategory": {
 *       "type": "string",
 *       "enum": [ "layout", "widget", "palette", ... ]
 *     },
 *     "action": { "type": "string" },
 *     ... (per-subcategory params forwarded as-is)
 *   },
 *   "required": ["subcategory", "action"]
 * }
 * }</pre>
 *
 * <p>At dispatch time the umbrella reads {@code subcategory}, looks up
 * the matching underlying tool, and forwards the entire args object
 * (with {@code subcategory} removed) to the underlying tool's
 * {@link SketchwareTool#execute}. The underlying tool then validates
 * {@code action} against its own enum and performs the operation.
 *
 * <p><b>Permission policy</b>: an umbrella is considered write
 * (not read-only) and not auto-approved by default — even if some of
 * its sub-tools are read-only. The conservative default ensures the
 * user is prompted before any potentially-mutating operation; the
 * {@link ToolPermissionGate} can still override per-tool if the user
 * configures it. (Read-only sub-tools like list/get are still
 * reasonably safe under an approval prompt — the user just gets a
 * chance to see what the LLM is about to do.)
 *
 * <p><b>Description string</b>: built dynamically from the underlying
 * tools' names + action lists so the LLM sees the full menu in the
 * system prompt without needing a separate per-subcategory schema.
 */
public final class CategoryUmbrellaTool implements SketchwareTool {

    private final String name;
    private final String category;
    private final String description;
    private final Map<String, SketchwareTool> subtools = new LinkedHashMap<>();

    /**
     * Build an umbrella.
     *
     * @param name        tool name (e.g. {@code "view_manage"})
     * @param category    category label (e.g. {@code "view"})
     * @param description short one-line description; the per-subcategory
     *                    action list is appended automatically
     * @param subtools    ordered map of subcategory name → underlying tool.
     *                    The insertion order is preserved in the schema
     *                    enum so the LLM sees a stable, predictable list.
     */
    public CategoryUmbrellaTool(String name, String category, String description,
                                Map<String, SketchwareTool> subtools) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name");
        if (category == null || category.isEmpty()) throw new IllegalArgumentException("category");
        if (subtools == null || subtools.isEmpty()) throw new IllegalArgumentException("subtools");
        this.name = name;
        this.category = category;
        this.description = description == null ? "" : description;
        this.subtools.putAll(subtools);
    }

    @Override public String name() { return name; }

    @Override public String category() { return category; }

    @Override public String description() {
        StringBuilder sb = new StringBuilder(description);
        sb.append("\n\nSubcategories and their actions:\n");
        for (Map.Entry<String, SketchwareTool> e : subtools.entrySet()) {
            SketchwareTool t = e.getValue();
            sb.append("- ").append(e.getKey())
                    .append(": ");
            if (t instanceof UniversalTool) {
                String[] actions = ((UniversalTool) t).getActions();
                sb.append(String.join(", ", actions));
            } else {
                sb.append("(specialized — see schema)");
            }
            sb.append('\n');
        }
        sb.append("\nPass subcategory + the underlying tool's action + any "
                + "additional params the action requires.");
        return sb.toString();
    }

    @Override public boolean isReadOnly() {
        // Conservative: treat umbrella as write even if some subtools are read-only.
        return false;
    }

    @Override public boolean isAutoApprovedByDefault() {
        // Conservative: umbrellas always require approval in ACT mode.
        return false;
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject subcatProp = new JsonObject();
        subcatProp.addProperty("type", "string");
        subcatProp.addProperty("description",
                "Subcategory selecting which underlying tool to dispatch to.");
        JsonArray subcatEnum = new JsonArray();
        for (String s : subtools.keySet()) subcatEnum.add(s);
        subcatProp.add("enum", subcatEnum);
        props.add("subcategory", subcatProp);

        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description",
                "Action enum value from the chosen subcategory's underlying tool. "
                        + "See the tool description for the per-subcategory action lists.");
        props.add("action", actionProp);

        // Allow action-specific arguments at the OBJECT level — the umbrella
        // forwards them as-is to the underlying tool, which validates them
        // against its own schema. Keeping this keyword inside `properties`
        // made it look like a malformed boolean-valued argument named
        // `additionalProperties`, so function-calling providers could reject
        // legitimate layout/widget arguments before the tool ever ran.
        schema.addProperty("additionalProperties", true);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("subcategory");
        required.add("action");
        schema.add("required", required);

        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (args == null) return ToolResult.error("args is null");
        if (!args.has("subcategory") || args.get("subcategory").isJsonNull()) {
            return ToolResult.error("subcategory is required");
        }
        String subcat = args.get("subcategory").getAsString();
        SketchwareTool subtool = subtools.get(subcat);
        if (subtool == null) {
            return ToolResult.error("Unknown subcategory '" + subcat
                    + "'. Supported: " + subtools.keySet());
        }
        // Forward a copy of args without the "subcategory" field — the
        // underlying tool's schema doesn't expect it.
        JsonObject forwarded = args.deepCopy();
        forwarded.remove("subcategory");
        return subtool.execute(forwarded, ctx);
    }
}
