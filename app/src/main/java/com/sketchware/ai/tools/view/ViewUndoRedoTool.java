package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;

/**
 * view_undo / view_redo - undo or redo the last action in the View editor.
 * For MVP, this is a hint to the UI - the actual undo/redo is triggered by
 * the user via the toolbar.
 */
public final class ViewUndoRedoTool implements SketchwareTool {

    private final boolean undo;

    public ViewUndoRedoTool(boolean undo) { this.undo = undo; }

    @Override public String name() { return undo ? "view_undo" : "view_redo"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return undo
                ? "Undo the last action in the View editor (add/delete/move/property change)."
                : "Redo the previously undone action in the View editor.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        return ToolResult.success(undo ? "Undo requested - user will see the change in View editor."
                                       : "Redo requested - user will see the change in View editor.");
    }
}
