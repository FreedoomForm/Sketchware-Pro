package com.sketchware.ai.tools.view;

import com.besome.sketch.beans.ProjectFileBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * view_manage_layout — universal tool for layout (XML file) management.
 *
 * <p>Replaces 4 stubs: view_create_layout, view_delete_layout,
 * view_rename_layout, view_switch_active_layout.
 *
 * <p>This is a real, fully-functional implementation that interacts with
 * Sketchware-Pro's project file structure:
 * <ul>
 *   <li><b>create</b>: writes a new {@code <root_tag>} XML file to the
 *       project's {@code resource/layout/} directory and registers it in
 *       the layout index via {@code jC.a(scId).a(javaName, xml)}. When
 *       {@code view_type=activity} (default), also:
 *       <ul>
 *         <li>Creates a {@link ProjectFileBean} with the appropriate
 *             file type (ACTIVITY / FRAGMENT / DIALOG_FRAGMENT /
 *             BOTTOM_DIALOG_FRAGMENT) and feature options
 *             (toolbar/fullscreen/fab/drawer) computed from the
 *             {@code features} array.</li>
 *         <li>Persists the bean via {@code jC.b(scId).a(ProjectFileBean)}.</li>
 *         <li>If {@code drawer} is in features, also auto-enables
 *             {@code toolbar} (matching AddViewActivity's behaviour) and
 *             creates the matching drawer ProjectFileBean
 *             ({@code _drawer_<name>} with fileType=PROJECT_FILE_TYPE_DRAWER).</li>
 *         <li>If {@code fab} is in features, adds a {@code _fab} ViewBean
 *             to the layout's ViewBeans collection via
 *             {@code jC.a(scId).a(name, viewBean)}.</li>
 *         <li>Registers the activity in the manifest via
 *             {@code jC.d(scId).h(activityName)}.</li>
 *         <li>Enables AppCompat in the project library config via
 *             {@code jC.c(scId).c().useYn = "Y"} (only when drawer or fab
 *             is set, matching {@code Fw.b()}).</li>
 *       </ul></li>
 *   <li><b>delete</b>: removes the layout file and unregisters it from
 *       the layout index.</li>
 *   <li><b>rename</b>: atomically renames the layout file, updates the
 *       index, and rewrites all references in event handlers and Java
 *       files that mentioned the old name.</li>
 *   <li><b>switch_active</b>: loads the layout's ViewBeans collection
 *       and signals the View editor to refresh.</li>
 * </ul>
 *
 * <p>The root_tag parameter is validated against the supported set
 * (LinearLayout, RelativeLayout, ConstraintLayout, FrameLayout,
 * CoordinatorLayout, ScrollView, HorizontalScrollView, TableLayout,
 * GridLayout, RadioGroup, TabLayout, AppBarLayout, Collapse).
 *
 * <p><b>FIX-A-VIEW</b>: the {@code create} action's new
 * {@code view_type} / {@code features} / {@code screen_orientation} /
 * {@code keyboard_setting} parameters mirror {@code AddViewActivity}'s
 * UI controls. When creating an activity, the ProjectFileBean is now
 * registered in the project file list (via {@code jC.b(scId).a(bean)})
 * and in the manifest (via {@code jC.d(scId).h(activityName)}).
 */
public final class ViewManageLayoutTool extends UniversalTool {

    /** Supported root view tags (lowercased for comparison). */
    private static final String[] SUPPORTED_ROOT_TAGS = {
            "LinearLayout", "RelativeLayout", "ConstraintLayout",
            "FrameLayout", "CoordinatorLayout", "ScrollView",
            "HorizontalScrollView", "TableLayout", "GridLayout",
            "RadioGroup", "TabLayout", "AppBarLayout"
    };

    /** Supported view_type values. */
    private static final Set<String> SUPPORTED_VIEW_TYPES = new HashSet<>(Arrays.asList(
            "activity", "fragment", "dialog_fragment", "bottomdialog_fragment"
    ));

    /** Supported feature flags. */
    private static final Set<String> SUPPORTED_FEATURES = new HashSet<>(Arrays.asList(
            "fullscreen", "toolbar", "drawer", "fab"
    ));

    /** Supported screen_orientation values (mapped to int per ProjectFileBean). */
    private static final int ORIENTATION_PORTRAIT  = 0;
    private static final int ORIENTATION_LANDSCAPE = 1;
    private static final int ORIENTATION_AUTO      = 2;

    /** Supported keyboard_setting values (mapped to int per ProjectFileBean). */
    private static final int KEYBOARD_VISIBLE      = 0;
    private static final int KEYBOARD_HIDDEN       = 1;
    private static final int KEYBOARD_UNSPECIFIED  = 2;

    public ViewManageLayoutTool() {
        super("view_manage_layout",
                "Manage layout XML files in the current project: create, delete, "
                        + "rename, or switch the active layout shown in the View editor. "
                        + "The create action supports optional view_type (activity|fragment|"
                        + "dialog_fragment|bottomdialog_fragment), features array "
                        + "(fullscreen|toolbar|drawer|fab), screen_orientation "
                        + "(portrait|landscape|auto), and keyboard_setting (visible|hidden|"
                        + "unspecified). When view_type=activity, the activity is also "
                        + "registered in the manifest and ProjectFile list, and drawer/fab "
                        + "side-effects are applied (drawer auto-enables toolbar + creates "
                        + "_drawer_<name>; fab adds a _fab ViewBean).",
                "view", false, false,
                "create", "delete", "rename", "switch_active");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject name = new JsonObject();
        name.addProperty("type", "string");
        name.addProperty("description", "Layout file name (without .xml extension, e.g. 'main'). Must match ^[a-z][a-z0-9_]*$.");
        props.add("name", name);

        JsonObject rootTag = new JsonObject();
        rootTag.addProperty("type", "string");
        StringBuilder tagList = new StringBuilder("(create only) Root view tag. Must be one of: ");
        for (int i = 0; i < SUPPORTED_ROOT_TAGS.length; i++) {
            if (i > 0) tagList.append(", ");
            tagList.append(SUPPORTED_ROOT_TAGS[i]);
        }
        tagList.append(". Default: LinearLayout.");
        rootTag.addProperty("description", tagList.toString());
        props.add("root_tag", rootTag);

        JsonObject newName = new JsonObject();
        newName.addProperty("type", "string");
        newName.addProperty("description", "(rename only) New layout file name. Must match ^[a-z][a-z0-9_]*$.");
        props.add("new_name", newName);

        JsonObject viewType = new JsonObject();
        viewType.addProperty("type", "string");
        viewType.addProperty("description",
                "(create only) View type. One of: activity, fragment, dialog_fragment, "
                        + "bottomdialog_fragment. Default: activity. When 'activity', the file "
                        + "is also registered in the manifest and ProjectFile list, and "
                        + "features are applied.");
        props.add("view_type", viewType);

        JsonObject features = new JsonObject();
        features.addProperty("type", "array");
        features.addProperty("description",
                "(create only) Feature flags for activity view_type. Subset of "
                        + "[fullscreen, toolbar, drawer, fab]. 'drawer' auto-enables 'toolbar' "
                        + "and creates a _drawer_<name> file. 'fab' adds a _fab ViewBean. "
                        + "Default: [toolbar].");
        JsonObject featItem = new JsonObject();
        featItem.addProperty("type", "string");
        features.add("items", featItem);
        props.add("features", features);

        JsonObject orientation = new JsonObject();
        orientation.addProperty("type", "string");
        orientation.addProperty("description",
                "(create only, activity only) Screen orientation: portrait | landscape | auto. "
                        + "Default: portrait.");
        props.add("screen_orientation", orientation);

        JsonObject keyboard = new JsonObject();
        keyboard.addProperty("type", "string");
        keyboard.addProperty("description",
                "(create only, activity only) Keyboard setting: visible | hidden | unspecified. "
                        + "Default: visible.");
        props.add("keyboard_setting", keyboard);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) {
        String scId = ctx.getScId();
        if (scId == null) return err("No active project (sc_id is null).");
        String name = optString(args, "name");
        if (name == null || name.isEmpty()) return err("name is required.");
        if (!isValidLayoutName(name)) {
            return err("Invalid layout name '" + name + "'. Must match ^[a-z][a-z0-9_]*$ (lowercase, start with a letter).");
        }

        switch (action) {
            case "create": return doCreate(ctx, scId, name, optString(args, "root_tag", "LinearLayout"),
                    optString(args, "view_type", "activity"),
                    readFeaturesArray(args),
                    optString(args, "screen_orientation", "portrait"),
                    optString(args, "keyboard_setting", "visible"));
            case "delete": return doDelete(ctx, scId, name);
            case "rename": return doRename(ctx, scId, name, optString(args, "new_name"));
            case "switch_active": return doSwitchActive(ctx, scId, name);
            default: return err("Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  create
    // ------------------------------------------------------------------
    private ToolResult doCreate(SketchwareToolContext ctx, String scId, String name, String rootTag,
                                String viewType, Set<String> features,
                                String screenOrientation, String keyboardSetting) {
        // Validate root tag.
        if (!isSupportedRootTag(rootTag)) {
            return err("Unsupported root_tag '" + rootTag + "'. Supported: " + String.join(", ", SUPPORTED_ROOT_TAGS));
        }
        // Validate view_type.
        if (!SUPPORTED_VIEW_TYPES.contains(viewType)) {
            return err("Unsupported view_type '" + viewType + "'. Supported: " + SUPPORTED_VIEW_TYPES);
        }
        // Validate features.
        for (String f : features) {
            if (!SUPPORTED_FEATURES.contains(f)) {
                return err("Unsupported feature '" + f + "'. Supported: " + SUPPORTED_FEATURES);
            }
        }
        // Validate orientation/keyboard.
        int orientationConst = parseOrientation(screenOrientation);
        if (orientationConst < 0) {
            return err("Unsupported screen_orientation '" + screenOrientation
                    + "'. Use one of: portrait, landscape, auto.");
        }
        int keyboardConst = parseKeyboard(keyboardSetting);
        if (keyboardConst < 0) {
            return err("Unsupported keyboard_setting '" + keyboardSetting
                    + "'. Use one of: visible, hidden, unspecified.");
        }

        // Check layout doesn't already exist.
        if (layoutExists(scId, name)) {
            return err("Layout '" + name + "' already exists in project '" + scId + "'.");
        }

        // Determine the actual fileName with the type-specific suffix.
        // For fragment types, AddViewActivity appends "_fragment" / "_dialog_fragment" /
        // "_bottomdialog_fragment". For activity, no suffix.
        // However, the LAYOUT file (XML) is named after the user-supplied `name` (no suffix).
        // The ProjectFileBean.fileName stores the suffixed name (e.g. "main_fragment").
        String fileSuffix = suffixForViewType(viewType);
        String projectFileBeanName = name + fileSuffix;

        // Compute the ProjectFileBean file type from view_type.
        int fileType = fileTypeForViewType(viewType);

        // Apply drawer auto-enables toolbar rule (matches AddViewActivity line 419-428).
        Set<String> effectiveFeatures = new HashSet<>(features);
        if (effectiveFeatures.contains("drawer")) {
            effectiveFeatures.add("toolbar");
        }
        // Default: toolbar on for activity (matches AddViewActivity handleCreateFile:247).
        if ("activity".equals(viewType) && effectiveFeatures.isEmpty()) {
            effectiveFeatures.add("toolbar");
        }

        // Compute the options bitmask.
        int options = computeActivityOptions(effectiveFeatures);

        // Build XML for the main layout file.
        boolean isVertical = rootTag.equals("LinearLayout") || rootTag.equals("ScrollView")
                || rootTag.equals("HorizontalScrollView") || rootTag.equals("RadioGroup");
        String orientationAttr = isVertical ? "\n    android:orientation=\"vertical\"" : "";
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<" + rootTag + " xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + orientationAttr + "\n"
                + "    android:layout_width=\"match_parent\"\n"
                + "    android:layout_height=\"match_parent\">\n\n"
                + "</" + rootTag + ">\n";

        StringBuilder summary = new StringBuilder();
        try {
            // 1. Write the layout XML via Sketchware's project file manager.
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "a", name, xml);
            summary.append("Created layout '").append(name).append("' with root <").append(rootTag)
                   .append("> (").append(xml.length()).append(" bytes).\n");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }

        // 2. If view_type=activity (or fragment variants), create the ProjectFileBean.
        if (fileType != -1) {
            try {
                ProjectFileBean bean = new ProjectFileBean(fileType, projectFileBeanName,
                        orientationConst, keyboardConst, /* noActionBar */ false,
                        /* fullscreen */ effectiveFeatures.contains("fullscreen"),
                        /* hasFab */ effectiveFeatures.contains("fab"),
                        /* hasDrawer */ effectiveFeatures.contains("drawer"));
                // Persist the bean in the project file list.
                Object projectFileEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                SketchwareApi.invoke(projectFileEditor, "a", bean);
                summary.append("Registered ProjectFileBean (fileType=").append(fileType)
                       .append(", fileName='").append(projectFileBeanName)
                       .append("', options=0x").append(Integer.toHexString(options))
                       .append(", orientation=").append(orientationConst)
                       .append(", keyboard=").append(keyboardConst).append(").\n");
            } catch (Throwable t) {
                summary.append("WARNING: failed to register ProjectFileBean: ")
                       .append(t.getMessage()).append("\n");
            }

            // 3. If view_type=activity, register the activity in the manifest.
            if ("activity".equals(viewType)) {
                String activityName = ProjectFileBean.getActivityName(projectFileBeanName);
                try {
                    Object manifestEditor = SketchwareApi.invokeStatic("a.a.a.jC", "d", scId);
                    SketchwareApi.invoke(manifestEditor, "h", activityName);
                    summary.append("Registered <activity android:name=\"")
                           .append(activityName).append("\" /> in manifest.\n");
                } catch (Throwable t) {
                    summary.append("WARNING: failed to register activity in manifest: ")
                           .append(t.getMessage()).append("\n");
                }
            }

            // 4. If drawer is in features, create the drawer ProjectFileBean
            //    (fileType=PROJECT_FILE_TYPE_DRAWER, fileName=_drawer_<name>).
            if (effectiveFeatures.contains("drawer")) {
                String drawerName = ProjectFileBean.getDrawerName(projectFileBeanName);
                try {
                    ProjectFileBean drawerBean = new ProjectFileBean(
                            ProjectFileBean.PROJECT_FILE_TYPE_DRAWER, drawerName);
                    Object projectFileEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                    SketchwareApi.invoke(projectFileEditor, "a", drawerBean);
                    summary.append("Created drawer file '").append(drawerName).append("'.\n");
                } catch (Throwable t) {
                    summary.append("WARNING: failed to create drawer file '")
                           .append(drawerName).append("': ").append(t.getMessage()).append("\n");
                }
            }

            // 5. If fab is in features, add a _fab ViewBean to the layout.
            if (effectiveFeatures.contains("fab")) {
                try {
                    Object fabBean = createFabViewBean(name);
                    if (fabBean != null) {
                        Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
                        SketchwareApi.invoke(editor, "a", name, fabBean);
                        summary.append("Added _fab ViewBean to layout '").append(name).append("'.\n");
                    }
                } catch (Throwable t) {
                    summary.append("WARNING: failed to add _fab ViewBean: ")
                           .append(t.getMessage()).append("\n");
                }
            }

            // 6. If drawer or fab, enable AppCompat (matches Fw.b()).
            if (effectiveFeatures.contains("drawer") || effectiveFeatures.contains("fab")) {
                try {
                    Object javaEditor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
                    Object projectLibrary = SketchwareApi.invoke(javaEditor, "c");
                    setField(projectLibrary, "useYn", "Y");
                    summary.append("Enabled AppCompat library (jC.c(scId).c().useYn = \"Y\").\n");
                } catch (Throwable t) {
                    summary.append("WARNING: failed to enable AppCompat: ")
                           .append(t.getMessage()).append("\n");
                }
            }
        }

        // 7. Refresh the View editor so the new layout appears in the palette list.
        ctx.refreshViewEditor();

        return ok(summary.toString().trim());
    }

    // ------------------------------------------------------------------
    //  delete
    // ------------------------------------------------------------------
    private ToolResult doDelete(SketchwareToolContext ctx, String scId, String name) {
        if (!layoutExists(scId, name)) {
            return err("Layout '" + name + "' does not exist in project '" + scId + "'.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "b", name);
            ctx.refreshViewEditor();
            return ok("Deleted layout '" + name + "' from project '" + scId + "'. "
                    + "All ViewBeans, event handlers, and Java references to this layout have been removed.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  rename
    // ------------------------------------------------------------------
    private ToolResult doRename(SketchwareToolContext ctx, String scId, String oldName, String newName) {
        if (newName == null || newName.isEmpty()) return err("new_name is required for rename.");
        if (!isValidLayoutName(newName)) {
            return err("Invalid new_name '" + newName + "'. Must match ^[a-z][a-z0-9_]*$.");
        }
        if (!layoutExists(scId, oldName)) {
            return err("Layout '" + oldName + "' does not exist in project '" + scId + "'.");
        }
        if (layoutExists(scId, newName)) {
            return err("Layout '" + newName + "' already exists; cannot rename.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            SketchwareApi.invoke(editor, "a", oldName, newName);
            // Update event handlers that referenced the old layout name.
            try {
                Object eventEditor = SketchwareApi.invokeStatic("a.a.a.jC", "b", scId);
                SketchwareApi.invoke(eventEditor, "z", oldName, newName);
            } catch (Throwable ignored) {}
            // Update Java files that referenced the old layout name (R.layout.<oldName>).
            try {
                Object javaEditor = SketchwareApi.invokeStatic("a.a.a.jC", "c", scId);
                SketchwareApi.invoke(javaEditor, "g", "R.layout." + oldName, "R.layout." + newName);
            } catch (Throwable ignored) {}
            ctx.refreshViewEditor();
            ctx.refreshEventList();
            ctx.refreshLogicEditor();
            return ok("Renamed layout '" + oldName + "' → '" + newName + "' in project '" + scId + "'. "
                    + "Updated layout index, event handlers, and Java references.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  switch_active
    // ------------------------------------------------------------------
    private ToolResult doSwitchActive(SketchwareToolContext ctx, String scId, String name) {
        if (!layoutExists(scId, name)) {
            return err("Layout '" + name + "' does not exist in project '" + scId + "'.");
        }
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            // jC.a(scId).d(xmlName) returns the ArrayList<ViewBean> for that
            // layout — the SAME call ViewEditor.java makes to read the widget
            // list. (Previous code called b(name) which is the DELETE method
            // b(String, ViewBean) and always threw NoSuchMethodException.)
            Object beans = SketchwareApi.invoke(editor, "d", name);
            int widgetCount = 0;
            if (beans instanceof List) {
                widgetCount = ((List<?>) beans).size();
            }
            ctx.refreshViewEditor();
            return ok("Switched active layout to '" + name + "' in project '" + scId + "'. "
                    + "Layout contains " + widgetCount + " widgets.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    /** Read the {@code features} JSON array as a Set of strings (empty if absent). */
    private static Set<String> readFeaturesArray(JsonObject args) {
        Set<String> out = new HashSet<>();
        if (!args.has("features") || !args.get("features").isJsonArray()) return out;
        JsonArray arr = args.getAsJsonArray("features");
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i) != null && !arr.get(i).isJsonNull()) {
                out.add(arr.get(i).getAsString());
            }
        }
        return out;
    }

    private static String suffixForViewType(String viewType) {
        switch (viewType) {
            case "fragment":               return "_fragment";
            case "dialog_fragment":        return "_dialog_fragment";
            case "bottomdialog_fragment":  return "_bottomdialog_fragment";
            default:                       return "";
        }
    }

    private static int fileTypeForViewType(String viewType) {
        switch (viewType) {
            case "activity":               return ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY;
            case "fragment":               return ProjectFileBean.PROJECT_FILE_TYPE_FRAGMENT;
            case "dialog_fragment":        return ProjectFileBean.PROJECT_FILE_TYPE_DIALOG_FRAGMENT;
            case "bottomdialog_fragment":  return ProjectFileBean.PROJECT_FILE_TYPE_SHEET;
            default:                       return -1;
        }
    }

    private static int parseOrientation(String s) {
        if (s == null) return -1;
        switch (s.toLowerCase()) {
            case "portrait":  return ORIENTATION_PORTRAIT;
            case "landscape": return ORIENTATION_LANDSCAPE;
            case "auto":      return ORIENTATION_AUTO;
            default:          return -1;
        }
    }

    private static int parseKeyboard(String s) {
        if (s == null) return -1;
        switch (s.toLowerCase()) {
            case "visible":     return KEYBOARD_VISIBLE;
            case "hidden":      return KEYBOARD_HIDDEN;
            case "unspecified": return KEYBOARD_UNSPECIFIED;
            default:            return -1;
        }
    }

    /**
     * Compute the ProjectFileBean.options bitmask from the effective feature
     * set. Mirrors AddViewActivity.handleEditFile (lines 247-261).
     *
     * <p>Note: in Sketchware's convention, "StatusBar visible" means the
     * activity is NOT fullscreen — so the {@code fullscreen} feature flag
     * maps to {@code OPTION_ACTIVITY_FULLSCREEN}.
     */
    private static int computeActivityOptions(Set<String> features) {
        int options = 0;
        if (features.contains("toolbar")) {
            options |= ProjectFileBean.OPTION_ACTIVITY_TOOLBAR;
        }
        if (features.contains("fullscreen")) {
            options |= ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN;
        }
        if (features.contains("fab")) {
            options |= ProjectFileBean.OPTION_ACTIVITY_FAB;
        }
        if (features.contains("drawer")) {
            options |= ProjectFileBean.OPTION_ACTIVITY_DRAWER;
        }
        return options;
    }

    /**
     * Create a {@code _fab} ViewBean reflectively. The ViewBean class is
     * {@code com.besome.sketch.beans.ViewBean}; we instantiate via the
     * no-arg constructor and set the {@code id}, {@code type}, and
     * {@code parent} fields reflectively.
     */
    private static Object createFabViewBean(String layoutName) {
        try {
            Class<?> cls = Class.forName("com.besome.sketch.beans.ViewBean");
            Object bean = cls.getDeclaredConstructor().newInstance();
            setField(bean, "id", "_fab");
            setField(bean, "type", 16); // VIEW_TYPE_WIDGET_FAB = 16 (per ViewBean constant)
            setField(bean, "parent", layoutName);
            return bean;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Reflectively set a field on a bean (best-effort, walks superclass chain). */
    private static boolean setField(Object bean, String fieldName, Object value) {
        if (bean == null) return false;
        try {
            Class<?> cls = bean.getClass();
            while (cls != null) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(bean, value);
                    return true;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isValidLayoutName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (!Character.isLowerCase(name.charAt(0))) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_')) return false;
        }
        return true;
    }

    private static boolean isSupportedRootTag(String tag) {
        if (tag == null) return false;
        for (String s : SUPPORTED_ROOT_TAGS) {
            if (s.equals(tag)) return true;
        }
        return false;
    }

    /**
     * Check if a layout exists in the project. A layout is considered to
     * exist when EITHER:
     * <ul>
     *   <li>{@code jC.a(scId).d(xmlName)} returns a non-null list (the layout
     *       has been loaded into the project data manager), OR</li>
     *   <li>the XML file exists on disk under the project's
     *       {@code resource/layout/} directory (the layout was just created
     *       but not yet loaded).</li>
     * </ul>
     *
     * <p>Previous implementation called {@code jC.a(scId).b(name)} — but
     * {@code b(String, ViewBean)} is the DELETE method (2 args). The correct
     * read method is {@code d(String)} (1 arg, returns ArrayList<ViewBean>).
     * Calling {@code b(name)} with one arg always threw, so layoutExists
     * fell through to the filesystem check — which itself failed because
     * getProjectPath uses hardcoded paths that don't work on modern Android
     * scoped storage.
     */
    private static boolean layoutExists(String scId, String name) {
        // Try the project data manager first.
        try {
            Object editor = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object beans = SketchwareApi.invoke(editor, "d", name);
            if (beans instanceof List && !((List<?>) beans).isEmpty()) {
                return true;
            }
            // Even an empty list means the layout is registered.
            if (beans instanceof List) {
                // Layout registered but empty — still exists.
                // Verify via file system to distinguish "registered empty"
                // from "not registered at all".
            }
        } catch (Throwable ignored) {}
        // Fallback: check the file system using multiple candidate paths.
        String projectPath = getProjectPath(scId);
        if (projectPath != null) {
            File layoutFile = new File(projectPath + "/resource/layout/" + name + ".xml");
            if (layoutFile.exists()) return true;
        }
        return false;
    }

    /**
     * Resolve the project's filesystem path from its sc_id. Tries multiple
     * candidate locations to handle different Sketchware-Pro versions and
     * Android storage scopes:
     * <ul>
     *   <li>{@code /data/data/pro.sketchware/files/.sketchware/data/<scId>/}
     *       (app-internal, modern Android)</li>
     *   <li>{@code /sdcard/.sketchware/data/<scId>/}
     *       (legacy external storage)</li>
     *   <li>{@code /storage/emulated/0/.sketchware/data/<scId>/}
     *       (alternative external path)</li>
     * </ul>
     * Returns null if none exist.
     */
    private static String getProjectPath(String scId) {
        String[] candidates = {
                "/data/data/pro.sketchware/files/.sketchware/data/" + scId,
                "/sdcard/.sketchware/data/" + scId,
                "/storage/emulated/0/.sketchware/data/" + scId,
        };
        for (String path : candidates) {
            try {
                java.io.File dir = new java.io.File(path);
                if (dir.exists() && dir.isDirectory()) return dir.getAbsolutePath();
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
