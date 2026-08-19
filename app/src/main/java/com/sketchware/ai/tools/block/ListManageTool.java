package com.sketchware.ai.tools.block;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * list_manage — universal tool for project-level list operations.
 *
 * <p>Actions (12):
 * <ul>
 *   <li><b>create</b> — create a project-level list (item_type: String|Map|Number)</li>
 *   <li><b>delete</b> — delete a project-level list</li>
 *   <li><b>add_item</b> — append an item</li>
 *   <li><b>remove_item</b> — remove item at index</li>
 *   <li><b>clear</b> — clear all items</li>
 *   <li><b>size</b> — get item count</li>
 *   <li><b>get_item</b> — get item at index</li>
 *   <li><b>set_item</b> — set item at index</li>
 *   <li><b>index_of</b> — find first index of value</li>
 *   <li><b>contains</b> — check if value exists (derived from index_of)</li>
 *   <li><b>sort</b> — sort the list (numeric/lexicographic)</li>
 *   <li><b>reverse</b> — reverse the list order</li>
 * </ul>
 *
 * <p>The first 6 actions (create/delete/add_item/remove_item/clear/size)
 * use the obfuscated project-file editor returned by {@code jC.b(sc_id)}
 * via reflection — known method letters: {@code a}=create, {@code b}=delete,
 * {@code f}=add_item, {@code g}=remove_item, {@code h}=clear, {@code i}=size.
 *
 * <p>The additional 6 actions (get_item/set_item/index_of/contains/sort/reverse)
 * attempt to call the unused method letters in the alphabetic dispatch
 * ({@code c, d, e, j, k, m}) on the project-file editor. If a method is not
 * found, the action returns a clear error message — the AI agent can then
 * fall back to runtime block operations.
 */
public final class ListManageTool extends UniversalTool {

    public ListManageTool() {
        super("list_manage",
                "Manage project-level lists: create, delete, add_item, "
                        + "remove_item, clear, size, get_item, set_item, "
                        + "index_of, contains, sort, reverse.",
                "block", false, false,
                "add_item",
                "clear",
                "create",
                "delete",
                "remove_item",
                "size",
                "get_item",
                "set_item",
                "index_of",
                "contains",
                "sort",
                "reverse");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_name = new JsonObject();
        p_name.addProperty("type", "string");
        p_name.addProperty("description", "List name.");
        props.add("name", p_name);
        JsonObject p_item_type = new JsonObject();
        p_item_type.addProperty("type", "string");
        p_item_type.addProperty("description", "(create) Item type: String|Map|Number.");
        props.add("item_type", p_item_type);
        JsonObject p_index = new JsonObject();
        p_index.addProperty("type", "integer");
        p_index.addProperty("description", "(remove_item/get_item/set_item) Index of item.");
        props.add("index", p_index);
        JsonObject p_value = new JsonObject();
        p_value.addProperty("type", "string");
        p_value.addProperty("description", "(add_item/set_item/index_of/contains) Item value.");
        props.add("value", p_value);
        JsonObject p_order = new JsonObject();
        p_order.addProperty("type", "string");
        p_order.addProperty("description", "(sort) Sort order: asc|desc. Default: asc.");
        props.add("order", p_order);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "add_item": {
                String name = optString(args, "name");
                String value = optString(args, "value");
                if (name == null || value == null) return err("name and value required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "f", "list:" + name, value);
                    return ok("Added item '" + value + "' to list '" + name + "'.");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "clear": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "h", "list:" + name);
                    return ok("Cleared list '" + name + "'.");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "create": {
                String name = optString(args, "name");
                String type = optString(args, "item_type", "String");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "a", "list:" + type + " " + name);
                    return ok("Created list '" + name + "' of " + type + ".");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "delete": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "b", "list:" + name);
                    return ok("Deleted list '" + name + "'.");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "remove_item": {
                String name = optString(args, "name");
                int idx = optInt(args, "index", -1);
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    SketchwareApi.invoke(pf, "g", "list:" + name, idx);
                    return ok("Removed item at index " + idx + " from list '" + name + "'.");
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "size": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    Object size = SketchwareApi.invoke(pf, "i", "list:" + name);
                    return ok("List '" + name + "' size: " + size);
                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "get_item": {
                String name = optString(args, "name");
                int idx = optInt(args, "index", -1);
                if (name == null) return err("name is required");
                if (idx < 0) return err("index is required and must be >= 0");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (c) for "get item at index".
                    Object item = SketchwareApi.invoke(pf, "c", "list:" + name, idx);
                    return ok("List '" + name + "'[" + idx + "] = " + item);
                } catch (Throwable t) {
                    return err("get_item is not supported via the project-file API "
                            + "(method 'c' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "set_item": {
                String name = optString(args, "name");
                int idx = optInt(args, "index", -1);
                String value = optString(args, "value");
                if (name == null) return err("name is required");
                if (idx < 0) return err("index is required and must be >= 0");
                if (value == null) return err("value is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (d) for "set item at index".
                    SketchwareApi.invoke(pf, "d", "list:" + name, idx, value);
                    return ok("Set list '" + name + "'[" + idx + "] = '" + value + "'.");
                } catch (Throwable t) {
                    return err("set_item is not supported via the project-file API "
                            + "(method 'd' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "index_of": {
                String name = optString(args, "name");
                String value = optString(args, "value");
                if (name == null || value == null) return err("name and value required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try the alphabetic next-letter method (e) for "index of value".
                    Object idx = SketchwareApi.invoke(pf, "e", "list:" + name, value);
                    return ok("List '" + name + "' indexOf('" + value + "') = " + idx);
                } catch (Throwable t) {
                    return err("index_of is not supported via the project-file API "
                            + "(method 'e' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "contains": {
                String name = optString(args, "name");
                String value = optString(args, "value");
                if (name == null || value == null) return err("name and value required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Derive contains from index_of: result >= 0 means the value exists.
                    Object idx = SketchwareApi.invoke(pf, "e", "list:" + name, value);
                    boolean contains = false;
                    if (idx instanceof Number) {
                        contains = ((Number) idx).intValue() >= 0;
                    } else if (idx != null) {
                        try { contains = Integer.parseInt(idx.toString()) >= 0; }
                        catch (NumberFormatException ignored) {}
                    }
                    return ok("List '" + name + "' contains '" + value + "': " + contains);
                } catch (Throwable t) {
                    return err("contains is not supported via the project-file API "
                            + "(method 'e' not found on editor). Cause: " + t.getMessage());
                }
            }
            case "sort": {
                String name = optString(args, "name");
                String order = optString(args, "order", "asc");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try method 'j' for "sort" (alphabetic next available).
                    SketchwareApi.invoke(pf, "j", "list:" + name, order);
                    return ok("Sorted list '" + name + "' (" + order + ").");
                } catch (Throwable t) {
                    return err("sort is not supported via the project-file API "
                            + "(method 'j' not found on editor). Use a runtime block "
                            + "with Collections.sort() instead. Cause: " + t.getMessage());
                }
            }
            case "reverse": {
                String name = optString(args, "name");
                if (name == null) return err("name is required");
                try {
                    Object pf = SketchwareApi.invokeStatic("a.a.a.jC", "b", ctx.getScId());
                    // Try method 'k' for "reverse" (alphabetic next available).
                    SketchwareApi.invoke(pf, "k", "list:" + name);
                    return ok("Reversed list '" + name + "'.");
                } catch (Throwable t) {
                    return err("reverse is not supported via the project-file API "
                            + "(method 'k' not found on editor). Use a runtime block "
                            + "with Collections.reverse() instead. Cause: " + t.getMessage());
                }
            }
            default:
                return err("Unknown action: " + action);
        }
    }
}
