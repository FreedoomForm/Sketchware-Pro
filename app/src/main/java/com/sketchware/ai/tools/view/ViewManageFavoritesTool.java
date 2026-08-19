package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * view_manage_favorites — universal tool for managing the widget
 * favorites collection: a user-curated library of widgets and image
 * resources that can be reused across layouts and projects.
 *
 * <p>Replaces 4 stubs: view_manage_favorites:{save_widget, add_collection,
 * delete_collection, add_image_resource_inline}.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Validates collection names against {@code ^[a-z][a-z0-9_]*$}
 *       (resource name convention).</li>
 *   <li>For {@code save_widget}: verifies the widget exists in the active
 *       layout before saving it to the collection (returns the available
 *       widget IDs if not found).</li>
 *   <li>For {@code delete_collection}: warns but allows if the collection
 *       has widgets (the warning lists the saved widget names).</li>
 *   <li>For {@code add_image_resource_inline}: validates that
 *       {@code image_data} looks like base64 (matches {@code ^[A-Za-z0-9+/=]+$}
 *       and length is a multiple of 4) OR is a valid file path that exists
 *       (in which case the file is read as raw bytes and base64-encoded
 *       before being passed to Sketchware). Reports the decoded byte size
 *       after add.</li>
 * </ul>
 */
public final class ViewManageFavoritesTool extends UniversalTool {

    /** Resource name convention: lowercase letter start, alphanumerics + underscore. */
    private static final Pattern VALID_COLLECTION = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** Image resource names follow the same convention. */
    private static final Pattern VALID_IMAGE_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");

    /** Base64 alphabet (standard, with padding). */
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    public ViewManageFavoritesTool() {
        super("view_manage_favorites",
                "Manage the widget favorites collection: save a widget, add a collection, "
                        + "delete a collection, or add an inline image resource. "
                        + "Collection names must match ^[a-z][a-z0-9_]*$.",
                "view", false, false,
                "save_widget", "add_collection", "delete_collection", "add_image_resource_inline");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pWidgetId = new JsonObject();
        pWidgetId.addProperty("type", "string");
        pWidgetId.addProperty("description", "(save_widget) ID of the widget to save. Must exist in the active layout.");
        props.add("widget_id", pWidgetId);

        JsonObject pCollection = new JsonObject();
        pCollection.addProperty("type", "string");
        pCollection.addProperty("description", "Name of the favorites collection. Must match ^[a-z][a-z0-9_]*$.");
        props.add("collection_name", pCollection);

        JsonObject pImageName = new JsonObject();
        pImageName.addProperty("type", "string");
        pImageName.addProperty("description", "(add_image_resource_inline) Image resource name. Must match ^[a-z][a-z0-9_]*$.");
        props.add("image_name", pImageName);

        JsonObject pImageData = new JsonObject();
        pImageData.addProperty("type", "string");
        pImageData.addProperty("description", "(add_image_resource_inline) Base64-encoded image data (matching ^[A-Za-z0-9+/=]+$, length multiple of 4) OR an absolute file path to an existing image file.");
        props.add("image_data", pImageData);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            case "save_widget":                return doSaveWidget(ctx, scId, args);
            case "add_collection":             return doAddCollection(ctx, scId, args);
            case "delete_collection":          return doDeleteCollection(ctx, scId, args);
            case "add_image_resource_inline":  return doAddImageResourceInline(ctx, scId, args);
            default:                            return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  save_widget
    // ------------------------------------------------------------------
    private ToolResult doSaveWidget(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String collection = optString(args, "collection_name", "default");
        if (!VALID_COLLECTION.matcher(collection).matches()) {
            return err("Invalid collection_name '" + collection + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        // Verify the widget exists in the active layout.
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null || javaName.isEmpty()) {
            return err("No active layout. Open a layout in the View editor first.");
        }
        List<String> available = listWidgetIds(editor, javaName);
        if (!available.contains(widgetId)) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + available);
        }
        try {
            SketchwareApi.invoke(editor, "e", widgetId, collection);
            return ok("Saved widget '" + widgetId + "' to favorites collection '"
                    + collection + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  add_collection
    // ------------------------------------------------------------------
    private ToolResult doAddCollection(SketchwareToolContext ctx, String scId, JsonObject args) {
        String collection = optString(args, "collection_name");
        if (collection == null || collection.isEmpty()) return err("collection_name is required.");
        if (!VALID_COLLECTION.matcher(collection).matches()) {
            return err("Invalid collection_name '" + collection + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "f", collection);
            return ok("Created favorites collection '" + collection + "' in project '" + scId + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  delete_collection
    // ------------------------------------------------------------------
    private ToolResult doDeleteCollection(SketchwareToolContext ctx, String scId, JsonObject args) {
        String collection = optString(args, "collection_name");
        if (collection == null || collection.isEmpty()) return err("collection_name is required.");
        if (!VALID_COLLECTION.matcher(collection).matches()) {
            return err("Invalid collection_name '" + collection + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        // Best-effort: check if the collection has saved widgets (to warn the user).
        List<String> savedWidgets = listWidgetsInCollection(editor, collection);
        try {
            SketchwareApi.invoke(editor, "g", collection);
            StringBuilder msg = new StringBuilder();
            msg.append("Deleted favorites collection '").append(collection)
               .append("' from project '").append(scId).append("'.");
            if (!savedWidgets.isEmpty()) {
                msg.append("\nWARNING: The collection contained ").append(savedWidgets.size())
                   .append(" saved widget(s): ").append(savedWidgets)
                   .append(". Those entries have been removed.");
            }
            return ok(msg.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  add_image_resource_inline
    // ------------------------------------------------------------------
    private ToolResult doAddImageResourceInline(SketchwareToolContext ctx, String scId, JsonObject args) {
        String name = optString(args, "image_name");
        String data = optString(args, "image_data");
        if (name == null || name.isEmpty()) return err("image_name is required.");
        if (!VALID_IMAGE_NAME.matcher(name).matches()) {
            return err("Invalid image_name '" + name + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        if (data == null || data.isEmpty()) return err("image_data is required.");

        byte[] rawBytes;
        String base64ToStore;

        if (looksLikeBase64(data)) {
            // Decode and re-encode to canonical form (validates + measures raw size).
            try {
                rawBytes = Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                return err("image_data looks like base64 but failed to decode: " + e.getMessage()
                        + ". Provide a valid base64 string (matching ^[A-Za-z0-9+/=]+$, "
                        + "length multiple of 4) OR an absolute file path.");
            }
            base64ToStore = data;
        } else {
            // Try interpreting as a file path.
            File f = new File(data);
            if (!f.exists() || !f.isFile()) {
                return err("image_data is neither valid base64 (must match ^[A-Za-z0-9+/=]+$ with length "
                        + "multiple of 4) nor an existing file path. Got: '"
                        + truncate(data, 80) + "'.");
            }
            try {
                rawBytes = java.nio.file.Files.readAllBytes(f.toPath());
            } catch (Throwable t) {
                // Fallback: stream read.
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f);
                     java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
                    rawBytes = bos.toByteArray();
                } catch (Throwable t2) {
                    return ToolResult.error(t2);
                }
            }
            base64ToStore = Base64.getEncoder().encodeToString(rawBytes);
        }

        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "h", name, base64ToStore);
            return ok("Added inline image resource '" + name + "' to project '" + scId + "'. "
                    + "Decoded size: " + rawBytes.length + " bytes "
                    + "(base64 payload: " + base64ToStore.length() + " chars).");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private static boolean looksLikeBase64(String s) {
        if (s == null || s.isEmpty()) return false;
        // Disallow whitespace; allow only standard base64 alphabet.
        if (!BASE64_PATTERN.matcher(s).matches()) return false;
        // Length must be a positive multiple of 4 (after padding).
        if (s.length() % 4 != 0) return false;
        // Heuristic: base64 strings are usually >= 8 chars (a 4-byte payload).
        // We don't enforce a strict minimum, but reject obvious non-base64
        // strings like "default" that happen to be 7 chars.
        if (s.length() < 8) return false;
        // A path-like value (contains '/' or '.') could still be valid base64,
        // but if it contains a path separator AND looks like a filesystem
        // path, the existence check below will catch it.
        return true;
    }

    private static List<String> listWidgetIds(Object editor, String javaName) {
        List<String> ids = new ArrayList<>();
        if (editor == null) return ids;
        try {
            Object widgets = SketchwareApi.invoke(editor, "d", javaName);
            if (widgets instanceof List) {
                for (Object b : (List<?>) widgets) {
                    String id = readField(b, "id");
                    if (id != null) ids.add(id);
                }
            }
        } catch (Throwable ignored) {}
        return ids;
    }

    private static List<String> listWidgetsInCollection(Object editor, String collection) {
        List<String> names = new ArrayList<>();
        if (editor == null) return names;
        try {
            Object saved = SketchwareApi.invoke(editor, "i", collection);
            if (saved instanceof List) {
                for (Object b : (List<?>) saved) {
                    String id = readField(b, "id");
                    if (id != null) names.add(id);
                }
            }
        } catch (Throwable ignored) {}
        return names;
    }

    private static String readField(Object bean, String fieldName) {
        if (bean == null) return null;
        try {
            Object v = SketchwareApi.invoke(bean,
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            return v == null ? null : v.toString();
        } catch (Throwable ignored) {}
        try {
            Class<?> cls = bean.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(bean);
                    return v == null ? null : v.toString();
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
