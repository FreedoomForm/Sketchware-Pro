package com.sketchware.ai.tools.creator;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

import java.util.Map;

import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;
import pro.sketchware.creator.runtime.CreatorScreen;

/** Lists Creator Runtime screens/activities without requiring a target name. */
public final class ActivityListTool implements SketchwareTool {
    @Override public String name() { return "activity_list"; }
    @Override public String category() { return "creator"; }
    @Override public boolean isReadOnly() { return true; }
    @Override public boolean isAutoApprovedByDefault() { return true; }

    @Override public String description() {
        return "List every available Creator Runtime activity/screen, route, and root widget. "
                + "This discovery operation takes no required arguments.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext context) {
        if (context == null || context.getContext() == null) {
            return ToolResult.error("Creator Runtime requires an Android context.");
        }
        try {
            CreatorProjectDocument document = CreatorRuntimeSession.get(context.getContext()).getDocument();
            StringBuilder result = new StringBuilder("Activities/screens in project '")
                    .append(document.getName()).append("' (")
                    .append(document.getScreens().size()).append("):\n");
            if (document.getScreens().isEmpty()) {
                result.append("- No screens exist yet. Use creator_runtime action=create_screen.");
            } else {
                for (Map.Entry<String, CreatorScreen> entry : document.getScreens().entrySet()) {
                    CreatorScreen screen = entry.getValue();
                    result.append("- id=").append(screen.getId())
                            .append(" route=").append(screen.getRoute())
                            .append(" root_widget=").append(screen.getRootWidgetId())
                            .append("\n");
                }
            }
            return ToolResult.success(result.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
