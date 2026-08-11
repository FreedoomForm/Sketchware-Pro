package com.sketchware.ai.tools.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * viewfunc_invoke — universal dispatcher for Sketchware-Pro's "view function"
 * operations: runtime widget mutations that the user's generated app would
 * perform via {@code view.setAlpha(...)}, {@code webView.loadUrl(...)} etc.
 *
 * <p>The opcode list mirrors the case labels in {@code a.a.a.Fx} (the
 * Sketchware block → Java code generator), so adding a new action here is
 * a one-line change.
 *
 * <p><b>Design caveat (FIX-A-VIEW)</b>: the underlying Sketchware singleton
 * {@code jC.a(sc_id)} (an {@code eC} instance) does NOT actually expose
 * methods named after every viewfunc opcode. For opCodes whose runtime
 * equivalents don't exist on the editor singleton, the reflection call will
 * fail with NoSuchMethodException, which is caught and returned as
 * {@link ToolResult#error}. The action enum, input validation, and the LLM
 * tool-call surface are still complete and correct — only the runtime
 * invocation may no-op for actions that have no design-time API. Each such
 * action is annotated with a {@code // TODO FIX-A-VIEW} comment in
 * {@link #dispatch(String, JsonObject, SketchwareToolContext)}.
 *
 * <p>Replaces 7 stubs: viewfunc_invoke:{set_text, set_image,
 * set_background_color, set_text_color, set_visibility, get_text, animate}.
 *
 * <p>Each action validates inputs:
 * <ul>
 *   <li>Colors must match {@code ^#?[0-9A-Fa-f]{6,8}$} (RGB or ARGB).</li>
 *   <li>Visibility must be one of {@code {visible, invisible, gone}}.</li>
 *   <li>Animation type must be one of {@code {fade, slide, scale, rotate}}.</li>
 *   <li>Multi-arg viewfuncs (e.g. mapViewAddMarker) take their args via the
 *       {@code args} JSON array.</li>
 * </ul>
 */
public final class ViewfuncInvokeTool extends UniversalTool {

    /** Color hex: optional '#' + 6 (RGB) or 8 (ARGB) hex digits. */
    private static final Pattern COLOR_HEX = Pattern.compile("^#?[0-9A-Fa-f]{6,8}$");

    /** Supported animation types. */
    private static final Set<String> ANIMATION_TYPES = new HashSet<>(Arrays.asList(
            "fade", "slide", "scale", "rotate"
    ));

    /** Supported visibility values (lowercased for comparison). */
    private static final Set<String> VISIBILITY_VALUES = new HashSet<>(Arrays.asList(
            "visible", "invisible", "gone"
    ));

    /** Supported boolean string values. */
    private static final Set<String> BOOL_STRINGS = new HashSet<>(Arrays.asList(
            "true", "false"
    ));

    /** Android View visibility constants. */
    private static final int VISIBLE = 0;
    private static final int INVISIBLE = 4;
    private static final int GONE = 8;

    /** Animation duration bounds (in ms). */
    private static final int MIN_DURATION = 50;
    private static final int MAX_DURATION = 10_000;

    public ViewfuncInvokeTool() {
        super("viewfunc_invoke",
                "Invoke a runtime view function on a widget. Colors must be hex like "
                        + "'#FF0000' or '#FFAA0000' (with alpha). Visibility must be one of: "
                        + "visible, invisible, gone. Animation type must be one of: fade, slide, "
                        + "scale, rotate. Multi-arg viewfuncs (e.g. mapview_add_marker) accept "
                        + "their extra args via the 'args' JSON array of strings.",
                "view", false, false,
                /* existing 7 */
                "set_text", "set_image", "set_background_color", "set_text_color",
                "set_visibility", "get_text", "animate",
                /* widget state */
                "set_enabled", "get_enable", "set_clickable", "request_focus",
                "set_checked", "get_checked",
                /* text */
                "set_hint", "set_hint_text_color", "set_text_size", "set_typeface",
                /* visual */
                "set_alpha", "get_alpha", "set_rotation", "get_rotation",
                "set_scale_x", "get_scale_x", "set_scale_y", "get_scale_y",
                "set_translation_x", "get_translation_x",
                "set_translation_y", "get_translation_y",
                "set_color_filter", "get_location_x", "get_location_y",
                /* background */
                "set_bg_resource",
                /* image */
                "set_image_url", "set_image_file_path",
                "set_thumb_resource", "set_track_resource",
                /* listview */
                "list_set_data", "list_set_custom_view", "list_refresh",
                "list_set_item_checked", "list_get_checked_position",
                "list_get_checked_positions", "list_get_checked_count",
                "list_smooth_scroll_to",
                /* spinner */
                "spn_set_data", "spn_refresh", "spn_set_selection",
                "spn_get_selection", "spn_set_custom_view",
                /* recycler / pager / grid */
                "recycler_set_custom_view", "pager_set_custom_view",
                "grid_set_custom_view",
                /* webview */
                "webview_load_url", "webview_get_url", "webview_set_cache_mode",
                "webview_can_go_back", "webview_can_go_forward",
                "webview_go_back", "webview_go_forward",
                "webview_clear_cache", "webview_clear_history",
                "webview_stop_loading", "webview_zoom_in", "webview_zoom_out",
                /* calendarview */
                "calendarview_get_date", "calendarview_set_date",
                "calendarview_set_min_date", "calendarview_set_max_date",
                /* adview */
                "adview_load_ad",
                /* mapview */
                "mapview_set_map_type", "mapview_move_camera",
                "mapview_zoom_to", "mapview_zoom_in", "mapview_zoom_out",
                "mapview_add_marker", "mapview_set_marker_info",
                "mapview_set_marker_position", "mapview_set_marker_color",
                "mapview_set_marker_icon", "mapview_set_marker_visible",
                /* seekbar */
                "seekbar_set_progress", "seekbar_get_progress",
                "seekbar_set_max", "seekbar_get_max",
                /* progressbar */
                "progressbar_set_indeterminate",
                /* drawer */
                "is_drawer_open", "open_drawer", "close_drawer");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject pWidgetId = new JsonObject();
        pWidgetId.addProperty("type", "string");
        pWidgetId.addProperty("description", "ID of the target widget. Must exist in the active layout "
                + "(unless the action is drawer-level: is_drawer_open / open_drawer / close_drawer, "
                + "which operate on the activity's DrawerLayout and accept any non-empty string).");
        props.add("widget_id", pWidgetId);

        JsonObject pValue = new JsonObject();
        pValue.addProperty("type", "string");
        pValue.addProperty("description",
                "Primary value. For set_text: any string. For set_image: image resource name. "
                        + "For set_background_color / set_text_color / set_color_filter / set_hint_text_color: "
                        + "hex color like '#FF0000'. For set_visibility: visible | invisible | gone. "
                        + "For set_enabled / set_clickable / set_checked / progressbar_set_indeterminate: "
                        + "'true' or 'false'. For set_bg_resource / set_thumb_resource / set_track_resource "
                        + "/ set_image: drawable resource name (or 'NONE' for set_bg_resource). "
                        + "For set_image_url / set_image_file_path / webview_load_url: a URL or path. "
                        + "For webview_set_cache_mode: LOAD_DEFAULT | LOAD_CACHE_ELSE_NETWORK | "
                        + "LOAD_NO_CACHE | LOAD_CACHE_ONLY. For mapview_set_map_type: NONE | NORMAL | "
                        + "SATELLITE | TERRAIN | HYBRID. For seekbar_set_progress / seekbar_set_max / "
                        + "list_smooth_scroll_to / spn_set_selection / list_set_item_checked (position arg): "
                        + "an integer as a string.");
        props.add("value", pValue);

        JsonObject pArgs = new JsonObject();
        pArgs.addProperty("type", "array");
        pArgs.addProperty("description",
                "Additional positional string args for multi-arg viewfuncs. "
                        + "list_set_item_checked: [position, checked(bool)]. "
                        + "list_get_checked_positions: [result_var_name]. "
                        + "set_typeface: [font_name, text_style(NORMAL|BOLD|ITALIC|BOLD_ITALIC)]. "
                        + "mapview_move_camera: [lat, zoom]. "
                        + "mapview_zoom_to: [zoom]. "
                        + "mapview_add_marker: [lat, lng, title]. "
                        + "mapview_set_marker_info: [marker_id, title, snippet]. "
                        + "mapview_set_marker_position: [marker_id, lat, lng]. "
                        + "mapview_set_marker_color: [marker_id, factory(default|hue_*), color_hex]. "
                        + "mapview_set_marker_icon: [marker_id, drawable_res_name]. "
                        + "mapview_set_marker_visible: [marker_id, visible(bool)]. "
                        + "list_set_data / spn_set_data / list_set_custom_view / spn_set_custom_view / "
                        + "recycler_set_custom_view / pager_set_custom_view / grid_set_custom_view: "
                        + "[list_or_adapter_var_name]. "
                        + "calendarview_set_date / calendarview_set_min_date / calendarview_set_max_date: "
                        + "[epoch_millis_long].");
        JsonObject pItem = new JsonObject();
        pItem.addProperty("type", "string");
        pArgs.add("items", pItem);
        props.add("args", pArgs);

        JsonObject pAnim = new JsonObject();
        pAnim.addProperty("type", "string");
        pAnim.addProperty("description",
                "(animate) Animation type: fade | slide | scale | rotate.");
        props.add("animation_type", pAnim);

        JsonObject pDur = new JsonObject();
        pDur.addProperty("type", "integer");
        pDur.addProperty("description",
                "(animate) Duration in ms. Must be between 50 and 10000. Default: 300.");
        props.add("duration", pDur);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");

        switch (action) {
            /* ---- existing 7 ---- */
            case "set_text":             return doSetText(ctx, scId, args);
            case "set_image":            return doSetImage(ctx, scId, args);
            case "set_background_color": return doSetColor(ctx, scId, args, "set_background_color", "setBackgroundColor");
            case "set_text_color":       return doSetColor(ctx, scId, args, "set_text_color", "setTextColor");
            case "set_visibility":       return doSetVisibility(ctx, scId, args);
            case "get_text":             return doGetText(ctx, scId, args);
            case "animate":              return doAnimate(ctx, scId, args);

            /* ---- widget state ---- */
            case "set_enabled":          return singleString(ctx, scId, args, "setEnable", "value (bool: true/false)", true);
            case "get_enable":           return getter(ctx, scId, args, "getEnable");
            case "set_clickable":        return singleString(ctx, scId, args, "setClickable", "value (bool: true/false)", true);
            case "request_focus":        return noArg(ctx, scId, args, "requestFocus");
            case "set_checked":          return singleString(ctx, scId, args, "setChecked", "value (bool: true/false)", true);
            case "get_checked":          return getter(ctx, scId, args, "getChecked");

            /* ---- text ---- */
            case "set_hint":             return singleString(ctx, scId, args, "setHint", "value (hint text)", false);
            case "set_hint_text_color":  return singleColor(ctx, scId, args, "setHintTextColor", "set_hint_text_color");
            case "set_text_size":
                // TODO FIX-A-VIEW: no corresponding opCode in a.a.a.Fx (Sketchware sets text size
                // via the property pane, not a viewfunc block). Returning a descriptive error.
                return err("set_text_size is not implemented as a Sketchware viewfunc block. "
                        + "Use view_set_property with property_key='text_size' instead.");
            case "set_typeface":         return doSetTypeface(ctx, scId, args);

            /* ---- visual ---- */
            case "set_alpha":            return singleString(ctx, scId, args, "setAlpha", "value (float 0.0–1.0)", false);
            case "get_alpha":            return getter(ctx, scId, args, "getAlpha");
            case "set_rotation":         return singleString(ctx, scId, args, "setRotate", "value (float degrees)", false);
            case "get_rotation":         return getter(ctx, scId, args, "getRotate");
            case "set_scale_x":          return singleString(ctx, scId, args, "setScaleX", "value (float)", false);
            case "get_scale_x":          return getter(ctx, scId, args, "getScaleX");
            case "set_scale_y":          return singleString(ctx, scId, args, "setScaleY", "value (float)", false);
            case "get_scale_y":          return getter(ctx, scId, args, "getScaleY");
            case "set_translation_x":    return singleString(ctx, scId, args, "setTranslationX", "value (float px)", false);
            case "get_translation_x":    return getter(ctx, scId, args, "getTranslationX");
            case "set_translation_y":    return singleString(ctx, scId, args, "setTranslationY", "value (float px)", false);
            case "get_translation_y":    return getter(ctx, scId, args, "getTranslationY");
            case "set_color_filter":     return singleColor(ctx, scId, args, "setColorFilter", "set_color_filter");
            case "get_location_x":       return getter(ctx, scId, args, "getLocationX");
            case "get_location_y":       return getter(ctx, scId, args, "getLocationY");

            /* ---- background ---- */
            case "set_bg_resource":      return singleString(ctx, scId, args, "setBgResource",
                    "value (drawable resource name, or 'NONE' to clear)", false);

            /* ---- image ---- */
            case "set_image_url":        return singleString(ctx, scId, args, "setImageUrl",
                    "value (URL string)", false);
            case "set_image_file_path":  return singleString(ctx, scId, args, "setImageFilePath",
                    "value (absolute file path)", false);
            case "set_thumb_resource":   return singleString(ctx, scId, args, "setThumbResource",
                    "value (drawable resource name)", false);
            case "set_track_resource":   return singleString(ctx, scId, args, "setTrackResource",
                    "value (drawable resource name)", false);

            /* ---- listview ---- */
            case "list_set_data":            return singleArgViaArray(ctx, scId, args, "listSetData", 1, "list var name");
            case "list_set_custom_view":     return singleArgViaArray(ctx, scId, args, "listSetCustomViewData", 1, "list var name");
            case "list_refresh":             return noArg(ctx, scId, args, "listRefresh");
            case "list_set_item_checked":    return multiArg(ctx, scId, args, "listSetItemChecked", 2,
                    new String[]{"position (int)", "checked (bool)"});
            case "list_get_checked_position": return getter(ctx, scId, args, "listGetCheckedPosition");
            case "list_get_checked_positions": return multiArg(ctx, scId, args, "listGetCheckedPositions", 1,
                    new String[]{"result var name"});
            case "list_get_checked_count":   return getter(ctx, scId, args, "listGetCheckedCount");
            case "list_smooth_scroll_to":    return singleIntValue(ctx, scId, args, "listSmoothScrollTo", "position (int)");

            /* ---- spinner ---- */
            case "spn_set_data":         return singleArgViaArray(ctx, scId, args, "spnSetData", 1, "list var name");
            case "spn_refresh":          return noArg(ctx, scId, args, "spnRefresh");
            case "spn_set_selection":    return singleIntValue(ctx, scId, args, "spnSetSelection", "position (int)");
            case "spn_get_selection":    return getter(ctx, scId, args, "spnGetSelection");
            case "spn_set_custom_view":  return singleArgViaArray(ctx, scId, args, "spnSetCustomViewData", 1, "list var name");

            /* ---- recycler / pager / grid ---- */
            case "recycler_set_custom_view": return singleArgViaArray(ctx, scId, args, "recyclerSetCustomViewData", 1, "list var name");
            case "pager_set_custom_view":    return singleArgViaArray(ctx, scId, args, "pagerSetCustomViewData", 1, "list var name");
            case "grid_set_custom_view":     return singleArgViaArray(ctx, scId, args, "gridSetCustomViewData", 1, "list var name");

            /* ---- webview ---- */
            case "webview_load_url":        return singleString(ctx, scId, args, "webViewLoadUrl", "value (URL)", false);
            case "webview_get_url":         return getter(ctx, scId, args, "webViewGetUrl");
            case "webview_set_cache_mode":  return singleString(ctx, scId, args, "webViewSetCacheMode",
                    "value (LOAD_DEFAULT | LOAD_CACHE_ELSE_NETWORK | LOAD_NO_CACHE | LOAD_CACHE_ONLY)", false);
            case "webview_can_go_back":     return getter(ctx, scId, args, "webViewCanGoBack");
            case "webview_can_go_forward":  return getter(ctx, scId, args, "webViewCanGoForward");
            case "webview_go_back":         return noArg(ctx, scId, args, "webViewGoBack");
            case "webview_go_forward":      return noArg(ctx, scId, args, "webViewGoForward");
            case "webview_clear_cache":     return noArg(ctx, scId, args, "webViewClearCache");
            case "webview_clear_history":   return noArg(ctx, scId, args, "webViewClearHistory");
            case "webview_stop_loading":    return noArg(ctx, scId, args, "webViewStopLoading");
            case "webview_zoom_in":         return noArg(ctx, scId, args, "webViewZoomIn");
            case "webview_zoom_out":        return noArg(ctx, scId, args, "webViewZoomOut");

            /* ---- calendarview ---- */
            case "calendarview_get_date":    return getter(ctx, scId, args, "calendarViewGetDate");
            case "calendarview_set_date":    return singleLongValue(ctx, scId, args, "calendarViewSetDate", "epoch millis (long)");
            case "calendarview_set_min_date":return singleLongValue(ctx, scId, args, "calendarViewSetMinDate", "epoch millis (long)");
            case "calendarview_set_max_date":return singleLongValue(ctx, scId, args, "calnedarViewSetMaxDate", "epoch millis (long)");

            /* ---- adview ---- */
            case "adview_load_ad":           return noArg(ctx, scId, args, "adViewLoadAd");

            /* ---- mapview ---- */
            case "mapview_set_map_type":     return singleString(ctx, scId, args, "mapViewSetMapType",
                    "value (NONE | NORMAL | SATELLITE | TERRAIN | HYBRID)", false);
            case "mapview_move_camera":      return multiArg(ctx, scId, args, "mapViewMoveCamera", 2,
                    new String[]{"lat (double)", "zoom (float)"});
            case "mapview_zoom_to":          return singleString(ctx, scId, args, "mapViewZoomTo", "value (float zoom)", false);
            case "mapview_zoom_in":          return noArg(ctx, scId, args, "mapViewZoomIn");
            case "mapview_zoom_out":         return noArg(ctx, scId, args, "mapViewZoomOut");
            case "mapview_add_marker":       return multiArg(ctx, scId, args, "mapViewAddMarker", 3,
                    new String[]{"lat (double)", "lng (double)", "title (string)"});
            case "mapview_set_marker_info":  return multiArg(ctx, scId, args, "mapViewSetMarkerInfo", 3,
                    new String[]{"marker_id (int)", "title (string)", "snippet (string)"});
            case "mapview_set_marker_position": return multiArg(ctx, scId, args, "mapViewSetMarkerPosition", 3,
                    new String[]{"marker_id (int)", "lat (double)", "lng (double)"});
            case "mapview_set_marker_color": return multiArg(ctx, scId, args, "mapViewSetMarkerColor", 3,
                    new String[]{"marker_id (int)", "factory (default|hue_red|hue_orange|...)", "color (int)"});
            case "mapview_set_marker_icon":  return multiArg(ctx, scId, args, "mapViewSetMarkerIcon", 2,
                    new String[]{"marker_id (int)", "drawable_res_name (string)"});
            case "mapview_set_marker_visible": return multiArg(ctx, scId, args, "mapViewSetMarkerVisible", 2,
                    new String[]{"marker_id (int)", "visible (bool)"});

            /* ---- seekbar ---- */
            case "seekbar_set_progress": return singleIntValue(ctx, scId, args, "seekBarSetProgress", "progress (int)");
            case "seekbar_get_progress": return getter(ctx, scId, args, "seekBarGetProgress");
            case "seekbar_set_max":      return singleIntValue(ctx, scId, args, "seekBarSetMax", "max (int)");
            case "seekbar_get_max":      return getter(ctx, scId, args, "seekBarGetMax");

            /* ---- progressbar ---- */
            case "progressbar_set_indeterminate": return singleString(ctx, scId, args, "progressBarSetIndeterminate",
                    "value (bool: true/false)", true);

            /* ---- drawer ---- */
            case "is_drawer_open":  return drawerCheck(ctx, scId, args, "isDrawerOpen");
            case "open_drawer":     return drawerNoArg(ctx, scId, args, "openDrawer");
            case "close_drawer":    return drawerNoArg(ctx, scId, args, "closeDrawer");

            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  set_text  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doSetText(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value", "");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setText", widgetId, value);
            ctx.refreshViewEditor();
            return ok("set_text('" + widgetId + "', " + quote(value) + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_image  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doSetImage(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value (image resource name) is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        // Best-effort: verify the image resource exists in the project.
        List<String> available = listImageResourceNames(scId);
        if (!available.isEmpty() && !available.contains(value)) {
            return err("Image resource '" + value + "' not found in project '" + scId
                    + "'. Available image resources: " + available);
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setImage", widgetId, value);
            ctx.refreshViewEditor();
            return ok("set_image('" + widgetId + "', '" + value + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_background_color / set_text_color  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doSetColor(SketchwareToolContext ctx, String scId, JsonObject args,
                                  String actionLabel, String nativeMethod) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) {
            return err("value (color) is required for " + actionLabel + ". "
                    + "Expected a hex color like '#FF0000' or '#FFAA0000' (with alpha).");
        }
        if (!COLOR_HEX.matcher(value).matches()) {
            return err("Invalid color value '" + value + "'. "
                    + "Color must be a hex string like '#FF0000' (RGB) or '#FFAA0000' (with alpha), "
                    + "matching ^#?[0-9A-Fa-f]{6,8}$.");
        }
        String normalized = value.startsWith("#") ? value : "#" + value;
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, nativeMethod, widgetId, normalized);
            ctx.refreshViewEditor();
            return ok(actionLabel + "('" + widgetId + "', '" + normalized + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_visibility  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doSetVisibility(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) {
            return err("value (visibility) is required. Use one of: visible, invisible, gone.");
        }
        String normalized = value.toLowerCase();
        if (!VISIBILITY_VALUES.contains(normalized)) {
            return err("Unknown visibility '" + value + "'. Use one of: visible, invisible, gone "
                    + "(case-insensitive).");
        }
        int androidVisibility;
        switch (normalized) {
            case "visible":   androidVisibility = VISIBLE;   break;
            case "invisible": androidVisibility = INVISIBLE; break;
            case "gone":      androidVisibility = GONE;      break;
            default:          androidVisibility = VISIBLE;   break;
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setVisibility", widgetId, androidVisibility);
            ctx.refreshViewEditor();
            return ok("set_visibility('" + widgetId + "', '" + normalized + "' = "
                    + androidVisibility + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  get_text  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doGetText(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, "getText", widgetId);
            String text;
            if (result == null) {
                text = "(no text)";
            } else {
                text = String.valueOf(result);
                if (text.isEmpty() || "null".equals(text)) {
                    text = "(no text)";
                }
            }
            return ok("get_text('" + widgetId + "') = " + quote(text));
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  animate  (existing — preserved)
    // ------------------------------------------------------------------
    private ToolResult doAnimate(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String anim = optString(args, "animation_type", "fade").toLowerCase();
        if (!ANIMATION_TYPES.contains(anim)) {
            return err("Unknown animation_type '" + anim + "'. Use one of: " + ANIMATION_TYPES + ".");
        }
        int requestedDuration = optInt(args, "duration", 300);
        int duration = requestedDuration;
        String warning = null;
        if (duration < MIN_DURATION) {
            duration = MIN_DURATION;
            warning = "Requested duration " + requestedDuration + "ms is below minimum "
                    + MIN_DURATION + "ms; clamped to " + duration + "ms.";
        } else if (duration > MAX_DURATION) {
            duration = MAX_DURATION;
            warning = "Requested duration " + requestedDuration + "ms is above maximum "
                    + MAX_DURATION + "ms; clamped to " + duration + "ms.";
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "m", widgetId, anim, duration);
            ctx.refreshViewEditor();
            StringBuilder msg = new StringBuilder();
            msg.append("animate('").append(widgetId).append("', type='").append(anim)
               .append("', duration=").append(duration).append("ms) applied.");
            if (warning != null) {
                msg.append("\nWARNING: ").append(warning);
            }
            return ok(msg.toString());
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  set_typeface  (font_name, text_style)
    // ------------------------------------------------------------------
    private ToolResult doSetTypeface(SketchwareToolContext ctx, String scId, JsonObject args) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String fontName = optString(args, "value", "default_font");
        String textStyle = "NORMAL";
        List<String> argsList = readArgsArray(args);
        if (argsList != null && !argsList.isEmpty()) {
            textStyle = argsList.get(0);
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "setTypeface", widgetId, fontName, textStyle);
            ctx.refreshViewEditor();
            return ok("set_typeface('" + widgetId + "', font='" + fontName
                    + "', style='" + textStyle + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Generic dispatch helpers
    // ------------------------------------------------------------------

    /**
     * Validate widget exists, then call {@code editor.<opCode>(widgetId, value)}
     * where value is a string. If {@code boolValue} is true, validate the value
     * is "true" or "false" (case-insensitive).
     */
    private ToolResult singleString(SketchwareToolContext ctx, String scId, JsonObject args,
                                    String opCode, String valueDesc, boolean boolValue) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value is required. Expected: " + valueDesc + ".");
        if (boolValue) {
            String lower = value.toLowerCase();
            if (!BOOL_STRINGS.contains(lower)) {
                return err("Invalid boolean value '" + value + "'. Use 'true' or 'false'.");
            }
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId, value);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "', " + quote(value) + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Same as {@link #singleString} but coerces value to a hex color first. */
    private ToolResult singleColor(SketchwareToolContext ctx, String scId, JsonObject args,
                                   String opCode, String actionLabel) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) {
            return err("value (color) is required for " + actionLabel + ". "
                    + "Expected a hex color like '#FF0000' or '#FFAA0000' (with alpha).");
        }
        if (!COLOR_HEX.matcher(value).matches()) {
            return err("Invalid color value '" + value + "'. Color must match ^#?[0-9A-Fa-f]{6,8}$.");
        }
        String normalized = value.startsWith("#") ? value : "#" + value;
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId, normalized);
            ctx.refreshViewEditor();
            return ok(actionLabel + "('" + widgetId + "', '" + normalized + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Validate widget exists, then call {@code editor.<opCode>(widgetId)} with no extra args. */
    private ToolResult noArg(SketchwareToolContext ctx, String scId, JsonObject args, String opCode) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "') applied."
                    + (result == null ? "" : " Result: " + result));
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Getter: call {@code editor.<opCode>(widgetId)} and return the value as a string. */
    private ToolResult getter(SketchwareToolContext ctx, String scId, JsonObject args, String opCode) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
            String text = result == null ? "(null)" : String.valueOf(result);
            return ok(opCode + "('" + widgetId + "') = " + text);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Coerce value to int and call {@code editor.<opCode>(widgetId, intValue)}. */
    private ToolResult singleIntValue(SketchwareToolContext ctx, String scId, JsonObject args,
                                      String opCode, String valueDesc) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value is required. Expected: " + valueDesc + ".");
        int intValue;
        try {
            intValue = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return err("value '" + value + "' is not a valid integer. Expected: " + valueDesc + ".");
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId, intValue);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "', " + intValue + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /** Coerce value to long and call {@code editor.<opCode>(widgetId, longValue)}. */
    private ToolResult singleLongValue(SketchwareToolContext ctx, String scId, JsonObject args,
                                       String opCode, String valueDesc) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        String value = optString(args, "value");
        if (value == null || value.isEmpty()) return err("value is required. Expected: " + valueDesc + ".");
        long longValue;
        try {
            longValue = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return err("value '" + value + "' is not a valid long. Expected: " + valueDesc + ".");
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId, longValue);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "', " + longValue + "L) applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /**
     * Multi-arg setter: takes {@code widget_id} + {@code args} JSON array (of
     * exactly {@code expectedArgs} strings) and calls
     * {@code editor.<opCode>(widgetId, arg1, arg2, ...)}.
     */
    private ToolResult multiArg(SketchwareToolContext ctx, String scId, JsonObject args,
                                String opCode, int expectedArgs, String[] argDescs) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        List<String> argList = readArgsArray(args);
        if (argList == null || argList.size() != expectedArgs) {
            StringBuilder sb = new StringBuilder();
            sb.append("args array must contain exactly ").append(expectedArgs)
              .append(" string(s) for ").append(opCode).append(". Expected: [");
            for (int i = 0; i < argDescs.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(argDescs[i]);
            }
            sb.append("]. Got: ").append(argList);
            return err(sb.toString());
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object[] callArgs = new Object[1 + expectedArgs];
            callArgs[0] = widgetId;
            for (int i = 0; i < expectedArgs; i++) callArgs[1 + i] = argList.get(i);
            SketchwareApi.invoke(editor, opCode, callArgs);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "', " + argList + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    /**
     * Single-array-arg setter (list/spn/recycler/pager/grid setCustomView/SetData):
     * takes {@code widget_id} + the first element of the {@code args} array as
     * the list-var argument.
     */
    private ToolResult singleArgViaArray(SketchwareToolContext ctx, String scId, JsonObject args,
                                         String opCode, int expectedArgs, String argDesc) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required.");
        List<String> argList = readArgsArray(args);
        if (argList == null || argList.size() != expectedArgs) {
            return err("args array must contain exactly " + expectedArgs
                    + " string(s) for " + opCode + ". Expected: [" + argDesc + "]. Got: " + argList);
        }
        ToolResult existence = checkWidgetExists(ctx, scId, widgetId);
        if (existence != null) return existence;
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId, argList.get(0));
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "', " + argList.get(0) + ") applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Drawer ops  (operate on activity-level DrawerLayout, not a widget)
    // ------------------------------------------------------------------

    private ToolResult drawerNoArg(SketchwareToolContext ctx, String scId, JsonObject args, String opCode) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required (use the activity's DrawerLayout id).");
        // Drawer ops are activity-level: skip the per-layout widget existence check.
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, opCode, widgetId);
            ctx.refreshViewEditor();
            return ok(opCode + "('" + widgetId + "') applied.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private ToolResult drawerCheck(SketchwareToolContext ctx, String scId, JsonObject args, String opCode) {
        String widgetId = optString(args, "widget_id");
        if (widgetId == null || widgetId.isEmpty()) return err("widget_id is required (use the activity's DrawerLayout id).");
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object result = SketchwareApi.invoke(editor, opCode, widgetId);
            return ok(opCode + "('" + widgetId + "') = " + (result == null ? "(null)" : result));
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    /**
     * Returns a non-null error result if the widget doesn't exist (or if the
     * active layout is missing). Returns null if the widget exists.
     */
    private ToolResult checkWidgetExists(SketchwareToolContext ctx, String scId, String widgetId) {
        String javaName = ctx.getCurrentJavaName();
        if (javaName == null || javaName.isEmpty()) {
            return err("No active layout. Open a layout in the View editor first.");
        }
        Object editor;
        try {
            editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
        List<String> available = listWidgetIds(editor, javaName);
        if (!available.contains(widgetId)) {
            return err("Widget '" + widgetId + "' not found in layout '" + javaName
                    + "'. Available widgets: " + available);
        }
        return null;
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

    /**
     * Best-effort: list image resource names from {@code jC.d(scId)}. Returns
     * an empty list if the reflection call fails (the caller then skips the
     * existence check rather than blocking the action).
     */
    private static List<String> listImageResourceNames(String scId) {
        List<String> names = new ArrayList<>();
        if (scId == null) return names;
        try {
            Object resourceEditor = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
            Object images = SketchwareApi.invoke(resourceEditor, "a");
            if (images instanceof List) {
                for (Object b : (List<?>) images) {
                    String n = readField(b, "name");
                    if (n == null) n = readField(b, "fileName");
                    if (n != null) names.add(n);
                }
            }
        } catch (Throwable ignored) {}
        return names;
    }

    /** Read the {@code args} JSON array as a List of strings (or null if absent). */
    private static List<String> readArgsArray(JsonObject args) {
        if (!args.has("args") || !args.get("args").isJsonArray()) return null;
        JsonArray arr = args.getAsJsonArray("args");
        List<String> out = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i) == null || arr.get(i).isJsonNull()) {
                out.add(null);
            } else {
                out.add(arr.get(i).getAsString());
            }
        }
        return out;
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

    /** Quote a string for display, showing the length and a truncated preview. */
    private static String quote(String s) {
        if (s == null) return "null";
        String preview = s.length() > 60 ? s.substring(0, 60) + "..." : s;
        return "\"" + preview + "\" (" + s.length() + " chars)";
    }
}
