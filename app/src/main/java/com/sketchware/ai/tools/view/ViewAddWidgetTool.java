package com.sketchware.ai.tools.view;

import com.besome.sketch.beans.ViewBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.List;

/**
 * view_add_widget - add a built-in widget to the current layout file.
 *
 * <p>Uses {@link ViewBean} (com.besome.sketch.beans.ViewBean) — the SAME bean
 * class used by Sketchware-Pro's own {@code ViewEditor}. The bean is persisted
 * via {@code jC.a(scId).a(xmlName, viewBean)} — the same call Sketchware's
 * ViewEditor makes when the user drags a widget onto the canvas.
 *
 * <p>Previous implementation used {@code mod.agus.jcoderz.beans.ViewBeans} (note
 * the trailing 's') — a DIFFERENT class that the {@code eC} project-data
 * manager does not accept. That triggered
 * {@code SketchwareApi.invoke: no method a(String, ViewBeans) on a.a.a.eC}
 * on every call.
 *
 * <p>Reflection is still used for the {@code jC.a(scId)} lookup to avoid
 * compile-time coupling to obfuscated classes.
 */
public final class ViewAddWidgetTool implements SketchwareTool {

    @Override public String name() { return "view_add_widget"; }
    @Override public String category() { return "view"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Add a built-in widget to the current layout file's canvas. "
                + "Widget ID is auto-generated. "
                + "Available widget types: LinearLayout, LinearLayout-Vertical, ScrollView, "
                + "ScrollView-Vertical, RadioGroup, RelativeLayout, TabLayout, BottomNavigationView, "
                + "CollapsingToolbarLayout, CardView, TextInputLayout, SwipeRefreshLayout, TextView, "
                + "EditText, AutoCompleteTextView, MultiAutoCompleteTextView, Button, MaterialButton, "
                + "ImageView, CircleImageView, CheckBox, RadioButton, Switch, SeekBar, ProgressBar, "
                + "RatingBar, SearchView, VideoView, WebView, ListView, GridView, RecyclerView, Spinner, "
                + "ViewPager, AdView, MapView, SignInButton, YoutubePlayer, AnalogClock, DigitalClock, "
                + "TimePicker, DatePicker, CalendarView.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject widgetType = new JsonObject();
        widgetType.addProperty("type", "string");
        widgetType.addProperty("description", "Widget type name");
        props.add("widget_type", widgetType);
        JsonObject parentId = new JsonObject();
        parentId.addProperty("type", "string");
        parentId.addProperty("description", "Parent container widget ID (optional)");
        props.add("parent_id", parentId);
        JsonObject x = new JsonObject();
        x.addProperty("type", "integer");
        x.addProperty("default", 100);
        props.add("x", x);
        JsonObject y = new JsonObject();
        y.addProperty("type", "integer");
        y.addProperty("default", 100);
        props.add("y", y);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("widget_type");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String widgetType = args.has("widget_type") ? args.get("widget_type").getAsString() : null;
        if (widgetType == null || widgetType.isEmpty()) return ToolResult.error("widget_type is required");
        String scId = ctx.getScId();
        String xmlName = ctx.getCurrentJavaName();
        if (scId == null || xmlName == null || xmlName.isEmpty()) {
            return ToolResult.error("No active project/layout (scId=" + scId + ", xmlName=" + xmlName + ").");
        }
        String parentId = args.has("parent_id") && !args.get("parent_id").isJsonNull()
                ? args.get("parent_id").getAsString() : null;

        // Check library gates reflectively (AdMob / Google Maps).
        try {
            Object iC = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
            if ("AdView".equals(widgetType) || "MapView".equals(widgetType)
                    || "SignInButton".equals(widgetType) || "YoutubePlayer".equals(widgetType)) {
                String libMethod = "AdView".equals(widgetType) ? "b" : "e";
                Object enabled = SketchwareApi.invoke(iC, libMethod);
                if (!Boolean.TRUE.equals(enabled)) {
                    String libName = "AdView".equals(widgetType) ? "admob" : "googlemap";
                    return ToolResult.error("library_required: widget '" + widgetType
                            + "' requires library '" + libName + "'. Call library_enable first.");
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);

            // List existing widgets to find next ID (same prefix-based scheme
            // as ViewEditor.generateWidgetId, but simplified).
            int maxN = 0;
            String prefix = widgetType.toLowerCase().replaceAll("[^a-z]", "");
            if (prefix.isEmpty()) prefix = "view";
            try {
                Object existing = SketchwareApi.invoke(eC, "d", xmlName);
                if (existing instanceof List) {
                    for (Object b : (List<?>) existing) {
                        try {
                            String id = null;
                            if (b instanceof ViewBean) {
                                id = ((ViewBean) b).id;
                            } else {
                                // Reflective fallback for non-ViewBean returns.
                                Object idObj = SketchwareApi.readField(b, "id");
                                if (idObj != null) id = idObj.toString();
                            }
                            if (id != null && id.startsWith(prefix)) {
                                try {
                                    int n = Integer.parseInt(id.substring(prefix.length()));
                                    if (n > maxN) maxN = n;
                                } catch (NumberFormatException ignored) {}
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
                // d(xmlName) may throw on a fresh layout with no widgets — that's fine.
            }
            String newId = prefix + (maxN + 1);

            // Build a ViewBean using the SAME class Sketchware's own ViewEditor uses.
            // ViewBean.getViewTypeByTypeName handles all standard + extended widget types.
            int typeCode = ViewBean.getViewTypeByTypeName(widgetType);
            if (typeCode == -1) {
                return ToolResult.error("Unknown widget type: '" + widgetType
                        + "'. Check the description for the list of supported types.");
            }
            ViewBean bean = new ViewBean(newId, typeCode);
            // Apply parent / coordinates.
            if (parentId != null && !parentId.isEmpty()) {
                bean.parent = parentId;
                bean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR; // best-effort
            }
            // Default layout: match_parent x wrap_content (sensible defaults for
            // a fresh widget inside a LinearLayout root). LayoutBean stores
            // width/height as int encoded values (LAYOUT_MATCH_PARENT = -1,
            // LAYOUT_WRAP_CONTENT = -2).
            if (bean.layout != null) {
                bean.layout.width = com.besome.sketch.beans.LayoutBean.LAYOUT_MATCH_PARENT;
                bean.layout.height = com.besome.sketch.beans.LayoutBean.LAYOUT_WRAP_CONTENT;
            }

            // Persist: jC.a(scId).a(xmlName, viewBean) — same call ViewEditor makes
            // when the user drags a widget onto the canvas.
            SketchwareApi.invoke(eC, "a", xmlName, bean);

            // Refresh the editor so the new widget is visible.
            ctx.refreshViewEditor();

            return ToolResult.success("Added " + widgetType + " with id='" + newId
                    + "' (type=" + typeCode + ") to layout '" + xmlName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }
}
