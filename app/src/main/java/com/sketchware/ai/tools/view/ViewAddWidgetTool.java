package com.sketchware.ai.tools.view;

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
 * <p>Uses reflection via {@link SketchwareApi} to invoke
 * {@code jC.a(scId).a(javaName, bean)} - this avoids compile-time coupling
 * to obfuscated classes (which can shift between Sketchware versions).
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
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        String parentId = args.has("parent_id") && !args.get("parent_id").isJsonNull()
                ? args.get("parent_id").getAsString() : null;

        // Check library gates reflectively.
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
            // List existing widgets to find next ID.
            Object existing = SketchwareApi.invoke(eC, "d", javaName);
            int maxN = 0;
            String prefix = widgetType.toLowerCase().replaceAll("[^a-z]", "");
            if (prefix.isEmpty()) prefix = "view";
            if (existing instanceof List) {
                for (Object b : (List<?>) existing) {
                    try {
                        Object id = SketchwareApi.invoke(b, "getId");
                        if (id == null) {
                            java.lang.reflect.Field f = b.getClass().getDeclaredField("id");
                            f.setAccessible(true);
                            id = f.get(b);
                        }
                        if (id != null && id.toString().startsWith(prefix)) {
                            try {
                                int n = Integer.parseInt(id.toString().substring(prefix.length()));
                                if (n > maxN) maxN = n;
                            } catch (NumberFormatException ignored) {}
                        }
                    } catch (Throwable ignored) {}
                }
            }
            String newId = prefix + (maxN + 1);

            // Build a new ViewBeans via reflection.
            Class<?> viewBeansClass = Class.forName("mod.agus.jcoderz.beans.ViewBeans");
            Object bean = viewBeansClass.getDeclaredConstructor().newInstance();
            // Set type / id / parent fields by reflection.
            setField(bean, "type", lookupViewType(widgetType));
            setField(bean, "id", newId);
            setField(bean, "parent", parentId != null ? parentId : "");
            setField(bean, "parentType", parentId != null ? 0 : -1);
            setField(bean, "index", -1);
            setField(bean, "enabled", true);
            setField(bean, "convert", "");
            setField(bean, "inject", "");
            setField(bean, "customView", "");
            // Default layout bean
            Object layoutObj = viewBeansClass.getDeclaredClasses().length > 0 ? null : null;
            java.lang.reflect.Field layoutField = null;
            try { layoutField = viewBeansClass.getDeclaredField("layout"); } catch (Exception ignored) {}
            if (layoutField != null) {
                layoutField.setAccessible(true);
                Object layoutBean = layoutField.get(bean);
                if (layoutBean == null) {
                    Class<?> layoutClass = Class.forName("mod.agus.jcoderz.beans.ViewBeans$Layout");
                    layoutBean = layoutClass.getDeclaredConstructor().newInstance();
                    layoutField.set(bean, layoutBean);
                }
                setField(layoutBean, "width", "match_parent");
                setField(layoutBean, "height", "wrap_content");
            }

            // Invoke jC.a(scId).a(javaName, bean) to persist.
            SketchwareApi.invoke(eC, "a", javaName, bean);

            return ToolResult.success("Added " + widgetType + " with id='" + newId + "' to layout '" + javaName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private void setField(Object bean, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f;
            try {
                f = bean.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                f = bean.getClass().getSuperclass().getDeclaredField(fieldName);
            }
            f.setAccessible(true);
            // Try to widen primitives
            if (value instanceof Integer && f.getType() == int.class) f.setInt(bean, (Integer) value);
            else if (value instanceof Boolean && f.getType() == boolean.class) f.setBoolean(bean, (Boolean) value);
            else f.set(bean, value);
        } catch (Throwable ignored) {}
    }

    private int lookupViewType(String name) {
        switch (name.toLowerCase().replace("-", "").replace(" ", "")) {
            case "linearlayout":         return 0;
            case "linearlayoutvertical": return 1;
            case "scrollview":           return 2;
            case "scrollviewvertical":   return 3;
            case "radiogroup":           return 4;
            case "relativelayout":       return 5;
            case "tablayout":            return 6;
            case "bottomnavigationview": return 7;
            case "collapsingtoolbarlayout": return 8;
            case "cardview":             return 9;
            case "textinputlayout":      return 10;
            case "swiperefreshlayout":   return 11;
            case "textview":             return 12;
            case "edittext":             return 13;
            case "autocompletetextview": return 14;
            case "multiautocompletetextview": return 15;
            case "button":               return 16;
            case "materialbutton":      return 17;
            case "imageview":            return 18;
            case "circleimageview":      return 19;
            case "checkbox":             return 20;
            case "radiobutton":          return 21;
            case "switch":               return 22;
            case "seekbar":              return 23;
            case "progressbar":          return 24;
            case "ratingbar":            return 25;
            case "searchview":           return 26;
            case "videoview":            return 27;
            case "webview":              return 28;
            case "listview":             return 29;
            case "gridview":             return 30;
            case "recyclerview":         return 31;
            case "spinner":              return 32;
            case "viewpager":            return 33;
            case "adview":               return 34;
            case "mapview":              return 35;
            case "signinbutton":         return 36;
            case "youtubeplayer":        return 37;
            case "analogclock":          return 38;
            case "digitalclock":         return 39;
            case "timepicker":           return 40;
            case "datepicker":           return 41;
            case "calendarview":         return 42;
            default: return -1;
        }
    }
}
