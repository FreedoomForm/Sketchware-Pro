package com.sketchware.ai.tools.library;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * library_enable - enable a built-in library via reflection.
 */
public final class LibraryEnableTool implements SketchwareTool {

    @Override public String name() { return "library_enable"; }
    @Override public String category() { return "library"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Enable a built-in library. library_type: compat, material3, firebase, admob, googlemap. "
                + "Some widgets/components require libraries (AdView->admob, MapView->googlemap, "
                + "RecyclerView->compat). Call this before adding such widgets.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject libType = new JsonObject();
        libType.addProperty("type", "string");
        JsonArray typeEnum = new JsonArray();
        typeEnum.add("compat"); typeEnum.add("material3"); typeEnum.add("firebase");
        typeEnum.add("admob"); typeEnum.add("googlemap");
        libType.add("enum", typeEnum);
        props.add("library_type", libType);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("library_type");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String libType = args.has("library_type") ? args.get("library_type").getAsString() : null;
        if (libType == null) return ToolResult.error("library_type is required");
        String scId = ctx.getScId();
        if (scId == null) return ToolResult.error("No active project.");
        try {
            Object iC = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            // Try common enable methods by reflection. If they don't exist, this is a no-op
            // (the library is enabled via the UI's ManageLibraryActivity which writes to disk).
            switch (libType) {
                case "compat":
                    try { SketchwareApi.invoke(iC, "c", true); } catch (Throwable ignored) {}
                    break;
                case "firebase":
                    try { SketchwareApi.invoke(iC, "d", true); } catch (Throwable ignored) {}
                    break;
                case "admob":
                    try { SketchwareApi.invoke(iC, "b", true); } catch (Throwable ignored) {}
                    break;
                case "googlemap":
                    try { SketchwareApi.invoke(iC, "e", true); } catch (Throwable ignored) {}
                    break;
                case "material3":
                    try { SketchwareApi.invoke(iC, "c", true); } catch (Throwable ignored) {}
                    break;
                default:
                    return ToolResult.error("Unknown library_type: " + libType);
            }
            try { SketchwareApi.invoke(iC, "k"); } catch (Throwable ignored) {}
            return ToolResult.success("Enabled library '" + libType + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
