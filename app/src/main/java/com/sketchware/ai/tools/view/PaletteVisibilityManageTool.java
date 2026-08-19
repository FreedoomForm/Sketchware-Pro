package com.sketchware.ai.tools.view;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * palette_visibility_manage - universal tool for managing the visibility and
 * ordering of palette categories in the Sketchware View editor.
 *
 * <p>Sketchware's View editor palette has multiple categories
 * ({@code basic}, {@code layout}, {@code media}, {@code advanced},
 * {@code widget}, {@code custom}). Users can:
 * <ul>
 *   <li>Show/hide entire categories</li>
 *   <li>Reorder categories</li>
 *   <li>Pin favorite widgets to a "favorites" section at the top
 *       (handled separately by {@link ViewManageFavoritesTool})</li>
 *   <li>Show/hide individual widgets within a category</li>
 * </ul>
 *
 * <p><b>Storage model</b>: palette category visibility/ordering is persisted
 * via Android {@link SharedPreferences} under the name
 * {@link #PREFS_NAME} ({@code sketchware_ai_palette_config}). The keys are:
 * <ul>
 *   <li>{@code category_<name>_visible} (boolean, default true)</li>
 *   <li>{@code category_order_<name>} (int, default = index in
 *       {@link #DEFAULT_CATEGORIES})</li>
 *   <li>{@code widget_<type>_visible} (boolean, default true) - per-widget
 *       visibility within a category</li>
 * </ul>
 *
 * <p><b>API quirk discovered during research</b>: neither
 * {@code WidgetsCreatorManager} (which manages custom palette widgets at
 * {@code .sketchware/resources/widgets/widgets.json}) nor
 * {@code PaletteWidget} (which is the in-memory view object) nor
 * {@code ViewEditorFragment.e()} (which builds the palette by issuing
 * hardcoded {@code extraTitle("AndroidX", 0)} etc. calls) currently reads
 * category visibility/ordering from SharedPreferences. The Sketchware UI
 * hardcodes the category layout in
 * {@code ViewEditorFragment.e()} (Layouts, AndroidX, Widgets, List, Library,
 * Google, Date &amp; Time). This tool stores the visibility/ordering settings
 * for the AI agent's use; future hook points in {@code ViewEditorFragment.e()}
 * (or a fork of it) can read these prefs to apply the customization. The
 * {@code reset} action clears all such prefs.
 *
 * <p><b>Validation</b>: {@code category} must be one of
 * {@link #SUPPORTED_CATEGORIES}. {@code new_position} for
 * {@code reorder_category} must be in {@code [0, SUPPORTED_CATEGORIES.size()-1]}.
 *
 * <p>Actions (6):
 * <ul>
 *   <li><b>set_category_visible</b> - show or hide a palette category
 *       (params: {@code category} required, {@code visible} required).</li>
 *   <li><b>get_category_visible</b> - get visibility of a category
 *       (params: {@code category} required).</li>
 *   <li><b>list_categories</b> - list all palette categories with their
 *       current visibility and order.</li>
 *   <li><b>reorder_category</b> - change the display order of a category
 *       (params: {@code category} required, {@code new_position} required).
 *       Other categories shift to fill the gap.</li>
 *   <li><b>set_widget_visible</b> - show or hide an individual widget
 *       (params: {@code widget_type} required, {@code visible} required).</li>
 *   <li><b>reset</b> - reset all palette visibility/ordering settings to
 *       defaults (no params).</li>
 * </ul>
 *
 * <p>The existing {@link ViewManageFavoritesTool} handles per-widget
 * <b>favorites</b> (the pinned "favorites" section), so this tool focuses on
 * category-level visibility and ordering, plus per-widget show/hide.
 */
public final class PaletteVisibilityManageTool extends UniversalTool {

    /** SharedPreferences file name where palette config is persisted. */
    private static final String PREFS_NAME = "sketchware_ai_palette_config";

    /** Key prefix for category visibility flags. */
    private static final String KEY_CAT_VISIBLE = "category_%s_visible";

    /** Key prefix for category ordering. */
    private static final String KEY_CAT_ORDER = "category_order_%s";

    /** Key prefix for per-widget visibility flags. */
    private static final String KEY_WIDGET_VISIBLE = "widget_%s_visible";

    /**
     * Supported palette category names. These match the values used by the
     * existing {@link ViewPaletteActionTool#SUPPORTED_GROUPS} for consistency.
     */
    private static final List<String> SUPPORTED_CATEGORIES = Arrays.asList(
            "basic", "layout", "media", "advanced", "widget", "custom"
    );

    /** Set form for fast contains() checks. */
    private static final Set<String> SUPPORTED_CATEGORIES_SET =
            new HashSet<>(SUPPORTED_CATEGORIES);

    /**
     * Default display order: same as {@link #SUPPORTED_CATEGORIES}. Index 0
     * is rendered at the top of the palette.
     */
    private static final List<String> DEFAULT_CATEGORIES = SUPPORTED_CATEGORIES;

    public PaletteVisibilityManageTool() {
        super("palette_visibility_manage",
                "Manage palette category visibility and ordering in the View "
                        + "editor: show/hide a category, get visibility, list "
                        + "categories, reorder a category, show/hide an "
                        + "individual widget, or reset all settings. "
                        + "Categories: basic, layout, media, advanced, widget, custom.",
                "view", /* readOnly */ false, /* autoApproved */ false,
                "set_category_visible", "get_category_visible", "list_categories",
                "reorder_category", "set_widget_visible", "reset");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pCategory = new JsonObject();
        pCategory.addProperty("type", "string");
        pCategory.addProperty("description",
                "Palette category name. Must be one of: basic, layout, media, "
                        + "advanced, widget, custom. Required for all actions "
                        + "except list_categories and reset.");
        props.add("category", pCategory);

        JsonObject pVisible = new JsonObject();
        pVisible.addProperty("type", "boolean");
        pVisible.addProperty("description",
                "(set_category_visible / set_widget_visible) Whether the "
                        + "category or widget should be visible.");
        props.add("visible", pVisible);

        JsonObject pNewPos = new JsonObject();
        pNewPos.addProperty("type", "integer");
        pNewPos.addProperty("description",
                "(reorder_category) New 0-indexed position for the category. "
                        + "Must be in [0, 5] (size of the supported categories list). "
                        + "Other categories shift to fill the gap.");
        props.add("new_position", pNewPos);

        JsonObject pWidgetType = new JsonObject();
        pWidgetType.addProperty("type", "string");
        pWidgetType.addProperty("description",
                "(set_widget_visible) Widget type name (e.g. 'Button', 'TextView'). "
                        + "Must match ^[A-Za-z][A-Za-z0-9_]*$.");
        props.add("widget_type", pWidgetType);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        if (ctx == null || ctx.getContext() == null) {
            return err("No context available - cannot access SharedPreferences.");
        }
        SharedPreferences prefs = ctx.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        switch (action) {
            case "set_category_visible": return doSetCategoryVisible(prefs, args);
            case "get_category_visible": return doGetCategoryVisible(prefs, args);
            case "list_categories":      return doListCategories(prefs, args);
            case "reorder_category":     return doReorderCategory(prefs, args);
            case "set_widget_visible":   return doSetWidgetVisible(prefs, args);
            case "reset":                return doReset(prefs, args);
            default:                     return err("Unknown action: " + action);
        }
    }

    // ==================================================================
    //  set_category_visible
    // ==================================================================

    /**
     * Show or hide a palette category. Persists the boolean flag in
     * SharedPreferences under key {@code category_<name>_visible}.
     */
    private ToolResult doSetCategoryVisible(SharedPreferences prefs, JsonObject args) {
        String category = optString(args, "category");
        if (!isValidCategory(category)) {
            return err("Invalid or missing category. Supported: " + SUPPORTED_CATEGORIES + ".");
        }
        boolean visible = optBool(args, "visible", true);
        try {
            prefs.edit().putBoolean(String.format(KEY_CAT_VISIBLE, category), visible).apply();
            return ok("Set category '" + category + "' visibility to " + visible + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  get_category_visible
    // ==================================================================

    /**
     * Get visibility of a category. Reads the boolean flag from
     * SharedPreferences; defaults to true if unset.
     */
    private ToolResult doGetCategoryVisible(SharedPreferences prefs, JsonObject args) {
        String category = optString(args, "category");
        if (!isValidCategory(category)) {
            return err("Invalid or missing category. Supported: " + SUPPORTED_CATEGORIES + ".");
        }
        try {
            boolean visible = prefs.getBoolean(String.format(KEY_CAT_VISIBLE, category), true);
            return ok("Category '" + category + "' visible=" + visible + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  list_categories
    // ==================================================================

    /**
     * List all palette categories with their current visibility and order.
     * The list is sorted by the stored order value (ascending); ties broken
     * by the default category index.
     */
    private ToolResult doListCategories(SharedPreferences prefs, JsonObject args) {
        try {
            // Build a list of category entries sorted by stored order.
            List<CategoryEntry> entries = new ArrayList<>();
            for (String cat : SUPPORTED_CATEGORIES) {
                boolean visible = prefs.getBoolean(String.format(KEY_CAT_VISIBLE, cat), true);
                int order = prefs.getInt(String.format(KEY_CAT_ORDER, cat),
                        DEFAULT_CATEGORIES.indexOf(cat));
                entries.add(new CategoryEntry(cat, visible, order));
            }
            Collections.sort(entries);
            StringBuilder sb = new StringBuilder();
            sb.append("Palette categories (").append(entries.size()).append("):\n");
            for (int i = 0; i < entries.size(); i++) {
                CategoryEntry e = entries.get(i);
                sb.append("  [").append(i).append("] name='").append(e.name)
                  .append("' visible=").append(e.visible)
                  .append(" order=").append(e.order)
                  .append(e.visible ? "" : " (HIDDEN)")
                  .append("\n");
            }
            return ok(sb.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  reorder_category
    // ==================================================================

    /**
     * Change the display order of a category. The category at the target
     * position (and any categories between the source and target) shift
     * to fill the gap. All categories' order values are recomputed and
     * persisted so the resulting order is dense and contiguous
     * ({@code 0, 1, 2, ..., N-1}).
     */
    private ToolResult doReorderCategory(SharedPreferences prefs, JsonObject args) {
        String category = optString(args, "category");
        if (!isValidCategory(category)) {
            return err("Invalid or missing category. Supported: " + SUPPORTED_CATEGORIES + ".");
        }
        int newPosition = optInt(args, "new_position", -1);
        int maxIndex = SUPPORTED_CATEGORIES.size() - 1;
        if (newPosition < 0 || newPosition > maxIndex) {
            return err("Invalid new_position " + newPosition + ". Must be in [0, " + maxIndex + "].");
        }
        try {
            // Build the current ordered list of categories (sorted by stored order).
            List<CategoryEntry> entries = new ArrayList<>();
            for (String cat : SUPPORTED_CATEGORIES) {
                boolean visible = prefs.getBoolean(String.format(KEY_CAT_VISIBLE, cat), true);
                int order = prefs.getInt(String.format(KEY_CAT_ORDER, cat),
                        DEFAULT_CATEGORIES.indexOf(cat));
                entries.add(new CategoryEntry(cat, visible, order));
            }
            Collections.sort(entries);

            // Find the source position, remove, re-insert at newPosition.
            int srcIdx = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name.equals(category)) { srcIdx = i; break; }
            }
            if (srcIdx < 0) {
                return err("Internal error: category '" + category + "' not found in entries list.");
            }
            CategoryEntry moved = entries.remove(srcIdx);
            entries.add(Math.min(newPosition, entries.size()), moved);

            // Reassign dense contiguous order values (0..N-1) and persist.
            SharedPreferences.Editor ed = prefs.edit();
            for (int i = 0; i < entries.size(); i++) {
                ed.putInt(String.format(KEY_CAT_ORDER, entries.get(i).name), i);
            }
            ed.apply();
            return ok("Moved category '" + category + "' to position " + newPosition
                    + ". New order: " + orderedNames(entries) + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  set_widget_visible
    // ==================================================================

    /**
     * Show or hide an individual widget within a category. The
     * {@code widget_type} should match a known Sketchware widget class name
     * (Button, TextView, ImageView, etc.). Validated against
     * {@code ^[A-Za-z][A-Za-z0-9_]*$} for Java identifier safety.
     */
    private ToolResult doSetWidgetVisible(SharedPreferences prefs, JsonObject args) {
        String widgetType = optString(args, "widget_type");
        if (widgetType == null || widgetType.isEmpty()) {
            return err("widget_type is required.");
        }
        if (!widgetType.matches("^[A-Za-z][A-Za-z0-9_]*$")) {
            return err("Invalid widget_type '" + widgetType
                    + "'. Must match ^[A-Za-z][A-Za-z0-9_]*$.");
        }
        boolean visible = optBool(args, "visible", true);
        try {
            prefs.edit().putBoolean(String.format(KEY_WIDGET_VISIBLE, widgetType), visible).apply();
            return ok("Set widget '" + widgetType + "' visibility to " + visible + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  reset
    // ==================================================================

    /**
     * Reset all palette visibility/ordering settings to defaults. Clears
     * every key with the {@code category_*_visible}, {@code category_order_*},
     * and {@code widget_*_visible} prefixes from SharedPreferences.
     */
    private ToolResult doReset(SharedPreferences prefs, JsonObject args) {
        try {
            SharedPreferences.Editor ed = prefs.edit();
            // Clear known category keys.
            for (String cat : SUPPORTED_CATEGORIES) {
                ed.remove(String.format(KEY_CAT_VISIBLE, cat));
                ed.remove(String.format(KEY_CAT_ORDER, cat));
            }
            // Clear per-widget visibility keys (we don't know the full set,
            // so iterate over all keys in the prefs and remove matching ones).
            for (String key : prefs.getAll().keySet()) {
                if (key.startsWith("widget_") && key.endsWith("_visible")) {
                    ed.remove(key);
                }
            }
            ed.apply();
            return ok("Reset all palette visibility/ordering settings to defaults. "
                    + "All categories are now visible with default ordering: " + DEFAULT_CATEGORIES + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static boolean isValidCategory(String category) {
        return category != null && SUPPORTED_CATEGORIES_SET.contains(category);
    }

    private static List<String> orderedNames(List<CategoryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (CategoryEntry e : entries) names.add(e.name);
        return names;
    }

    /**
     * Lightweight holder for a category's runtime state, used during sorting
     * and reordering. Implements {@link Comparable} so it can be sorted by
     * ascending order value; ties are broken by the default category index
     * (i.e. the order in {@link #DEFAULT_CATEGORIES}) for deterministic output.
     */
    private static final class CategoryEntry implements Comparable<CategoryEntry> {
        final String name;
        final boolean visible;
        final int order;

        CategoryEntry(String name, boolean visible, int order) {
            this.name = name;
            this.visible = visible;
            this.order = order;
        }

        @Override public int compareTo(CategoryEntry other) {
            if (this.order != other.order) return Integer.compare(this.order, other.order);
            return Integer.compare(
                    DEFAULT_CATEGORIES.indexOf(this.name),
                    DEFAULT_CATEGORIES.indexOf(other.name));
        }
    }
}
