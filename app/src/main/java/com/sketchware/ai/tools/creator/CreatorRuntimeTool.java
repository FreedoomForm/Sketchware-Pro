package com.sketchware.ai.tools.creator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import pro.sketchware.creator.runtime.CreatorApplyResult;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectOperation;
import pro.sketchware.creator.runtime.CreatorRuntimeOperationMapper;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/**
 * AI-only adapter to the same Creator Runtime operation pipeline used by the
 * visual editor. It contains no alternative file-write or renderer path.
 */
public final class CreatorRuntimeTool implements SketchwareTool {
    @Override public String name() { return "creator_runtime"; }
    @Override public String category() { return "creator"; }
    @Override public boolean isReadOnly() { return false; }
    @Override public boolean isAutoApprovedByDefault() { return false; }

    @Override public String description() {
        return "Apply one transparent Creator Runtime operation to the live, user-editable project. "
                + "Actions: create_screen, add_widget, set_widget_property, update_entry_control, restore_revision. "
                + "Every accepted call creates a visible revision and audit record; never claim success unless the result says applied.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray actions = new JsonArray();
        actions.add("create_screen");
        actions.add("add_widget");
        actions.add("set_widget_property");
        actions.add("update_entry_control");
        actions.add("restore_revision");
        action.add("enum", actions);
        properties.add("action", action);
        addString(properties, "screen_id", "ID for a new screen.");
        addString(properties, "route", "Screen route starting with '/'.");
        addString(properties, "root_widget_id", "ID for the new screen root widget.");
        addString(properties, "root_widget_type", "Optional root widget type; defaults to column.");
        addString(properties, "widget_id", "ID for the widget.");
        addString(properties, "widget_type", "Runtime widget type, such as text or button.");
        addString(properties, "parent_id", "ID of the parent widget.");
        JsonObject index = new JsonObject();
        index.addProperty("type", "integer");
        properties.add("index", index);
        JsonObject propertiesObject = new JsonObject();
        propertiesObject.addProperty("type", "object");
        propertiesObject.addProperty("description", "Simple visual properties for the widget.");
        properties.add("properties", propertiesObject);
        addString(properties, "property", "Widget property name.");
        JsonObject value = new JsonObject();
        value.addProperty("description", "New property value.");
        properties.add("value", value);
        JsonObject visible = new JsonObject();
        visible.addProperty("type", "boolean");
        properties.add("visible", visible);
        addString(properties, "label", "Visible Creator entry-control label.");
        addString(properties, "placement", "Entry control placement: bottom_end, bottom_start, top_end, top_start, or center.");
        JsonObject targetRevision = new JsonObject();
        targetRevision.addProperty("type", "integer");
        properties.add("target_revision", targetRevision);
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
        schema.addProperty("additionalProperties", true);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext context) {
        if (context == null || context.getContext() == null) {
            return ToolResult.error("Creator Runtime requires an Android context.");
        }
        try {
            CreatorRuntimeSession session = CreatorRuntimeSession.get(context.getContext());
            CreatorProjectOperation operation = CreatorRuntimeOperationMapper.map(args, session.getDocument(),
                    CreatorProjectOperation.ActorKind.AI);
            CreatorApplyResult result = session.apply(operation);
            if (!result.isApplied()) {
                return ToolResult.error("Creator Runtime rejected " + operation.getType().name() + ": "
                        + result.getValidation().getCode().name() + " — " + result.getValidation().getMessage());
            }
            CreatorProjectDocument document = result.getDocument();
            return ToolResult.success("Creator Runtime applied " + operation.getType().name()
                    + " at revision " + document.getRevision()
                    + ". Screens=" + document.getScreens().size()
                    + ", widgets=" + document.getWidgets().size() + ".");
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Creator Runtime arguments are invalid: " + e.getMessage());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private static void addString(JsonObject properties, String key, String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", "string");
        property.addProperty("description", description);
        properties.add(key, property);
    }
}
