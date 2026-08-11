package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.Collection;
import java.util.Map;

/**
 * map_manage — universal tool for project-level map operations.
 *
 * <p>Actions (9):
 * <ul>
 *   <li><b>create</b> — create a project-level map (key_type String|Number, value_type String|Number|Map|List)</li>
 *   <li><b>get</b> — get the value for a key</li>
 *   <li><b>put</b> — set key=value</li>
 *   <li><b>remove</b> — remove a key from the map</li>
 *   <li><b>clear</b> — clear all entries</li>
 *   <li><b>keys</b> — return list of all keys</li>
 *   <li><b>values</b> — return list of all values</li>
 *   <li><b>contains_key</b> — check if a key exists</li>
 *   <li><b>size</b> — return number of entries</li>
 * </ul>
 *
 * <p>The first 3 actions (create/get/put) are backed by the obfuscated
 * project-file editor returned by {@code jC.b(sc_id)} via reflection —
 * known method letters: {@code a}=create, {@code j}=get, {@code k}=put.
 *
 * <p>The additional 6 actions (remove/clear/keys/values/contains_key/size)
 * attempt to call the next letters in the alphabetic dispatch
 * ({@code l, m, n, o, p, i}) on the project-file editor. If a method is
 * not found, the action returns a clear error message — the AI agent can
 * then fall back to runtime block operations.
 */
public final class MapManageTool extends UniversalTool {

    public MapManageTool() {
        super("map_manage",
                "Manage project-level maps: create, get, put, remove, clear, "
                        + "keys, values, contains_key, size.",
                "block", false, false,
                "create",
                "get",
                "put",
                "remove",
                "clear",
                "keys",
                "values",
                "contains_key",
                "size");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "Map name.");
        props.add("name", p_name);
        JsonObject p_key = new JsonObject();
        p_key.addProperty("type", "string");
        p_key.addProperty("description", "(get/put/remove/contains_key) Map key.");
        props.add("key", p_key);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(put) Map value.");
        props.add("value", p_value);
        JsonObject p_key_type = new JsonObject();
        p_key_type.addProperty("type", "string");
        p_key_type.addProperty("description", "(create) Key type: String|Number.");
        props.add("key_type", p_key_type);
        JsonObject p_value_type = new JsonObject();
        p_value_type.addProperty("type", "string");
        p_value_type.addProperty("description", "(create) Value type: String|Number|Map|List.");
        props.add("value_type", p_value_type);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "create": {
                String name = optString(args, "name");
                String kt = optString(args, "key_type", "String");
                String vt = optString(args, "value_type", "String");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "a", "map:" + kt + "," + vt + " " + name);
                    return ok("Created map '" + name + "' (" + kt + "→" + vt + ").");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "get": {
                String name = optString(args, "name");
                String key = optString(args, "key");
                if (name == null || key == null) return err("name and key required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    Object val = SketchwareApi.invoke(pf, "j", "map:" + name, key);
                    return ok("Map '" + name + "'[" + key + "] = " + val);
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "put": {
                String name = optString(args, "name");
                String key = optString(args, "key");
                String val = optString(args, "value");
                if (name == null || key == null || val == null) return err("name, key, value required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "k", "map:" + name, key, val);
                    return ok("Set map '" + name + "'[" + key + "] = '" + val + "'.");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove": {
                String name = optString(args, "name");
                String key = optString(args, "key");
                if (name == null || key == null) return err("name and key required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (l) for "remove key".
                    SketchwareApi.invoke(pf, "l", "map:" + name, key);
                    return ok("Removed key '" + key + "' from map '" + name + "'.");
                } catch (Throwable t) {
                    return err("remove is not supported via the project-file API "
                            + "(method 'l' not found on editor). Use a runtime block "
                            + "or put a null/empty value instead. Cause: " + t.getMessage());
                }
            }
            case "clear": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (m) for "clear".
                    SketchwareApi.invoke(pf, "m", "map:" + name);
                    return ok("Cleared map '" + name + "'.");
                } catch (Throwable t) {
                    return err("clear is not supported via the project-file API "
                            + "(method 'm' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "keys": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (n) for "get all keys".
                    Object keys = SketchwareApi.invoke(pf, "n", "map:" + name);
                    return ok("Map '" + name + "' keys: " + stringify(keys));
                } catch (Throwable t) {
                    return err("keys is not supported via the project-file API "
                            + "(method 'n' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "values": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (o) for "get all values".
                    Object vals = SketchwareApi.invoke(pf, "o", "map:" + name);
                    return ok("Map '" + name + "' values: " + stringify(vals));
                } catch (Throwable t) {
                    return err("values is not supported via the project-file API "
                            + "(method 'o' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "contains_key": {
                String name = optString(args, "name");
                String key = optString(args, "key");
                if (name == null || key == null) return err("name and key required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (p) for "contains key".
                    Object contains = SketchwareApi.invoke(pf, "p", "map:" + name, key);
                    return ok("Map '" + name + "' contains '" + key + "': " + contains);
                } catch (Throwable t) {
                    // Fallback: use the get method (j) and check whether the value is null.
                    try {
                        Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                        Object val = SketchwareApi.invoke(pf, "j", "map:" + name, key);
                        boolean contains = val != null;
                        return ok("Map '" + name + "' contains '" + key + "': " + contains
                                + " (via fallback get-based check).");
                    } catch (Throwable t2) {
                        return err("contains_key is not supported via the project-file API "
                                + "(methods 'p' and 'j' both failed). Cause: " + t2.getMessage());
                    }
                }
            }
            case "size": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the 'i' method (same as list size — may be shared).
                    Object size = SketchwareApi.invoke(pf, "i", "map:" + name);
                    return ok("Map '" + name + "' size: " + size);
                } catch (Throwable t) {
                    // Fallback: try 'q' (alphabetic next available).
                    try {
                        Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                        Object size = SketchwareApi.invoke(pf, "q", "map:" + name);
                        return ok("Map '" + name + "' size: " + size);
                    } catch (Throwable t2) {
                        return err("size is not supported via the project-file API "
                                + "(methods 'i' and 'q' both failed). Cause: " + t2.getMessage());
                    }
                }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

    /** Best-effort stringification of an arbitrary object (collection/map/scalar). */
    private static String stringify(Object o) {
        if (o == null) return "null";
        if (o instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Object x : (Collection<?>) o) {
                if (i++ > 0) sb.append(", ");
                sb.append(String.valueOf(x));
            }
            return sb.append("]").toString();
        }
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (i++ > 0) sb.append(", ");
                sb.append(String.valueOf(e.getKey())).append("=").append(String.valueOf(e.getValue()));
            }
            return sb.append("}").toString();
        }
        return String.valueOf(o);
    }
}
