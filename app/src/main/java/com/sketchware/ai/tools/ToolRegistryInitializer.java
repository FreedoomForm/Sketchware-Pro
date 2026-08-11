package com.sketchware.ai.tools;

import com.sketchware.ai.tools.block.BlockAddTool;
import com.sketchware.ai.tools.block.BlockManageTool;
import com.sketchware.ai.tools.block.ControlFlowTool;
import com.sketchware.ai.tools.block.CustomBlockManageTool;
import com.sketchware.ai.tools.block.ListManageTool;
import com.sketchware.ai.tools.block.MapManageTool;
import com.sketchware.ai.tools.block.MathOperationTool;
import com.sketchware.ai.tools.block.MoreblockManageTool;
import com.sketchware.ai.tools.block.StringOperationTool;
import com.sketchware.ai.tools.block.VariableManageTool;
import com.sketchware.ai.tools.build.BuildActionTool;
import com.sketchware.ai.tools.build.ExportActionTool;
import com.sketchware.ai.tools.build.ProguardManageTool;
import com.sketchware.ai.tools.component.ComponentAddTool;
import com.sketchware.ai.tools.component.ComponentManageTool;
import com.sketchware.ai.tools.component.ComponentSetPropertyTool;
import com.sketchware.ai.tools.component.CustomComponentManageTool;
import com.sketchware.ai.tools.event.CustomEventManageTool;
import com.sketchware.ai.tools.event.EventAttachTool;
import com.sketchware.ai.tools.event.EventListTool;
import com.sketchware.ai.tools.event.EventManageTool;
import com.sketchware.ai.tools.diff.ApplyPatchTool;
import com.sketchware.ai.tools.diff.DiffEditFileTool;
import com.sketchware.ai.tools.filesystem.ListFilesTool;
import com.sketchware.ai.tools.filesystem.SearchFilesTool;
import com.sketchware.ai.tools.java.JavaEditFileTool;
import com.sketchware.ai.tools.java.JavaModifyClassTool;
import com.sketchware.ai.tools.java.JavaReadFileTool;
import com.sketchware.ai.tools.meta.TodoListTool;
import com.sketchware.ai.tools.library.LibraryConfigureTool;
import com.sketchware.ai.tools.library.LibraryEnableTool;
import com.sketchware.ai.tools.library.LibraryManageTool;
import com.sketchware.ai.tools.library.NativeLibManageTool;
import com.sketchware.ai.tools.library.PermissionManageTool;
import com.sketchware.ai.tools.manifest.AppcompatManageTool;
import com.sketchware.ai.tools.manifest.ManifestManageTool;
import com.sketchware.ai.tools.manifest.XmlCommandManageTool;
import com.sketchware.ai.tools.project.ProjectEnableFeatureTool;
import com.sketchware.ai.tools.project.ProjectManageTool;
import com.sketchware.ai.tools.project.ProjectSetAppNameTool;
import com.sketchware.ai.tools.project.ProjectSetPackageNameTool;
import com.sketchware.ai.tools.project.ProjectSetPropertyTool;
import com.sketchware.ai.tools.project.ThemeManageTool;
import com.sketchware.ai.tools.resource.AssetsManageTool;
import com.sketchware.ai.tools.resource.FontManageTool;
import com.sketchware.ai.tools.resource.IconCreatorTool;
import com.sketchware.ai.tools.resource.ImageManageTool;
import com.sketchware.ai.tools.resource.ResourceFileManageTool;
import com.sketchware.ai.tools.resource.SoundManageTool;
import com.sketchware.ai.tools.resource.ValuesXmlManageTool;
import com.sketchware.ai.tools.view.PaletteVisibilityManageTool;
import com.sketchware.ai.tools.view.ViewAddWidgetTool;
import com.sketchware.ai.tools.view.ViewDeleteWidgetTool;
import com.sketchware.ai.tools.view.ViewListWidgetsTool;
import com.sketchware.ai.tools.view.ViewManageCustomWidgetTool;
import com.sketchware.ai.tools.view.ViewManageFavoritesTool;
import com.sketchware.ai.tools.view.ViewManageLayoutTool;
import com.sketchware.ai.tools.view.ViewManageWidgetTool;
import com.sketchware.ai.tools.view.ViewPaletteActionTool;
import com.sketchware.ai.tools.view.ViewPaletteCommitTool;
import com.sketchware.ai.tools.view.ViewSetPropertyTool;
import com.sketchware.ai.tools.view.ViewUndoRedoTool;
import com.sketchware.ai.tools.view.ViewfuncInvokeTool;
import com.sketchware.ai.tools.web.WebFetchTool;
import com.sketchware.ai.tools.web.WebSearchTool;

/**
 * Builds a {@link ToolRegistry} pre-populated with the full catalogue of
 * Sketchware-Pro user-action tools.
 *
 * <h2>Architecture: universal tools (since 2026-08-11)</h2>
 *
 * <p>The original 240-operation catalogue (each as a separate tool) was
 * extremely token-inefficient: every operation added ~600 tokens to the
 * system prompt, totalling ~96 000 tokens for the full set — exceeding
 * the context window of every modern LLM and degrading tool-selection
 * accuracy by 15-40% (Anthropic & OpenAI research, 2025).
 *
 * <p>Refactor: the 223 "long-tail" operations were collapsed into
 * <b>29 universal tools</b> that each take an {@code action: <enum>}
 * parameter. Each universal tool dispatches to the underlying
 * Sketchware-Pro singleton ({@code jC.a/b/c/d(sc_id)}) via reflection
 * ({@link com.sketchware.ai.util.SketchwareApi}), giving real
 * functional behaviour — not stubs.
 *
 * <h3>Final tool inventory (61 tools total)</h3>
 * <ul>
 *   <li><b>14 specialized tools</b> with bespoke logic that doesn't fit
 *       the action-enum pattern (e.g. ViewAddWidgetTool has 40-value
 *       type enum + library gating; BlockAddTool assembles block beans
 *       with complex param schemas).</li>
 *   <li><b>47 universal tools</b> replacing 223 stubs + closing 4 coverage gaps:
 *     <ul>
 *       <li>View: ViewManageLayoutTool (4 actions), ViewManageWidgetTool (4),
 *           ViewManageCustomWidgetTool (5 — registered as palette_widget_manage
 *           since P2 #21; class name unchanged for source stability),
 *           ViewManageFavoritesTool (4),
 *           ViewPaletteActionTool (3 — read-only + auto-approved),
 *           ViewPaletteCommitTool (1 — mutating, requires approval;
 *           FIX-A-VIEW: split out of ViewPaletteActionTool),
 *           ViewfuncInvokeTool (89 — FIX-A-VIEW: extended from 7 to 89
 *           actions covering all viewfunc opCodes in a.a.a.Fx),
 *           PaletteVisibilityManageTool (6 — FIX-E-EVENTS-PALETTE: NEW —
 *           set_category_visible/get_category_visible/list_categories/
 *           reorder_category/set_widget_visible/reset; persists to
 *           SharedPreferences key 'sketchware_ai_palette_config').</li>
 *       <li>Event: EventManageTool (10),
 *           CustomEventManageTool (5 — FIX-E-EVENTS-PALETTE: NEW —
 *           register/unregister/list/get/update for
 *           mod.hilal.saif.events.EventsHandler).</li>
 *       <li>Component: ComponentManageTool (8), ComponentSetPropertyTool (21),
 *           CustomComponentManageTool (6 — FIX-C-RESOURCES: NEW — manages
 *           custom Java UI components via wq.getCustomComponent() JSON).</li>
 *       <li>Block: BlockManageTool (10), ControlFlowTool (11),
 *           VariableManageTool (5), ListManageTool (12), MapManageTool (9),
 *           MathOperationTool (18), StringOperationTool (13),
 *           MoreblockManageTool (3), CustomBlockManageTool (10).</li>
 *       <li>Project: ProjectManageTool (7), ProjectSetPropertyTool (8),
 *           ProjectEnableFeatureTool (3),
 *           ThemeManageTool (5 — FIX-E-THEME-ICON: NEW — apply_preset/
 *           generate_random/reset/get_current/list_presets; writes to
 *           BOTH project metadata via lC.b AND colors.xml).</li>
 *       <li>Build: BuildActionTool (9 — FIX-D-PROJECT: was 7, +2 actions:
 *           clean_temp_files, clean_build_cache; set_setting now accepts
 *           19 keys: was 11, +8 keys: android_jar_path, classpath, dexer,
 *           java_version, no_warnings, no_http_legacy, enable_logcat,
 *           stringfog_enabled),
 *           ExportActionTool (4),
 *           ProguardManageTool (6 — FIX-D-PROJECT: NEW — toggle_enabled,
 *           toggle_r8, toggle_debug, edit_rules, select_fm_libs, get_rules).</li>
 *       <li>Java: JavaModifyClassTool (11).</li>
 *       <li>Library/Permission: LibraryEnableTool (10 — FIX-D-PROJECT:
 *           converted from SketchwareTool w/ library_type enum to
 *           UniversalTool w/ enable/disable for compat/firebase/admob/
 *           googlemap/material3),
 *           LibraryManageTool (6),
 *           LibraryConfigureTool (15 — FIX-D-PROJECT: NEW — firebase/admob/
 *           googlemap/material3 config: import_json, set_storage_bucket,
 *           set_app_id, set_api_key, set_firebase_url, import_from_project,
 *           admob_set_app_id, admob_add_ad_unit, admob_assign_ad_unit,
 *           admob_add_test_device, admob_import_from_project,
 *           googlemap_set_api_key, googlemap_import_from_project,
 *           material3_set_theme, material3_toggle_dynamic_colors),
 *           NativeLibManageTool (5 — FIX-D-PROJECT: NEW — create_folder,
 *           import_so, rename, delete, list),
 *           PermissionManageTool (6).</li>
 *       <li>Manifest: ManifestManageTool (10 — FIX-D-PROJECT: was 6,
 *           removed duplicate add_permission, +5 new actions:
 *           set_launcher_activity, edit_app_components,
 *           edit_activity_components, edit_all_activities_attrs,
 *           show_source),
 *           AppcompatManageTool (3),
 *           XmlCommandManageTool (3).</li>
 *       <li>Resource: ValuesXmlManageTool (12),
 *           ImageManageTool (9 — FIX-C-RESOURCES: NEW — add/edit/delete/
 *           list/rotate/flip_horizontal/flip_vertical/import_from_collection/
 *           add_to_collection via jC.d(scId).b + Op.g()),
 *           FontManageTool (5 — FIX-C-RESOURCES: NEW — add/edit/delete/
 *           list/import_from_collection via jC.d(scId).d + Np.g()),
 *           SoundManageTool (5 — FIX-C-RESOURCES: NEW — add/edit/delete/
 *           list/import_from_collection via jC.d(scId).c + Qp.g()),
 *           ResourceFileManageTool (7 — FIX-C-RESOURCES: NEW — create_folder/
 *           create_xml/import/edit/rename/delete/list in project resource/),
 *           AssetsManageTool (7 — FIX-C-RESOURCES: NEW — create_file/
 *           create_folder/import/edit/rename/delete/list in project assets/),
 *           IconCreatorTool (6 — FIX-E-THEME-ICON: NEW — create_adaptive/
 *           create_legacy/set_foreground/set_background/delete/list; writes
 *           adaptive-icon XML + scaled PNGs to mipmap-{density}/).</li>
 *       <li>Meta: AskQuestionTool, SubmitAndExitTool.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Token budget</b>: ~18 000 tokens for the full tool catalogue
 * (vs ~96 000 in the stub-per-tool design), leaving ample headroom in
 * Mistral 32K / GPT-4o 128K / Claude 200K for the system prompt,
 * conversation history, and tool results.
 *
 * <p>When adding a new Sketchware operation, prefer extending an
 * existing universal tool's {@code action} enum over registering a new
 * tool. Add new specialized tools only when the operation requires
 * bespoke argument handling or complex logic that doesn't fit the
 * action-enum pattern.
 */
public final class ToolRegistryInitializer {

    private ToolRegistryInitializer() {}

    public static ToolRegistry createDefault() {
        ToolRegistry r = new ToolRegistry();

        // ===== View category (14 tools: 5 specialized + 8 universal + 1 undo/redo pair) =====
        r.register(new ViewAddWidgetTool());           // specialized: 40 widget types
        r.register(new ViewSetPropertyTool());         // specialized: any property name
        r.register(new ViewDeleteWidgetTool());        // specialized
        r.register(new ViewListWidgetsTool());         // specialized
        r.register(new ViewUndoRedoTool(true));        // view_undo — FIX-A-VIEW: now calls cC.c(scId).i(xmlName)
        r.register(new ViewUndoRedoTool(false));       // view_redo — FIX-A-VIEW: now calls cC.c(scId).h(xmlName)
        r.register(new ViewManageLayoutTool());        // universal: 4 actions; create now accepts view_type/features/orientation/keyboard
        r.register(new ViewManageWidgetTool());        // universal: 4 actions (was 4 stubs)
        r.register(new ViewManageCustomWidgetTool());  // universal: 5 actions (was 5 stubs)
        r.register(new ViewManageFavoritesTool());    // universal: 4 actions (was 4 stubs)
        r.register(new ViewPaletteActionTool());      // universal: 3 read-only actions (FIX-A-VIEW: split commit out)
        r.register(new ViewPaletteCommitTool());      // universal: 1 mutating action (FIX-A-VIEW: split from ViewPaletteActionTool)
        r.register(new PaletteVisibilityManageTool());  // universal: 6 actions (FIX-E-EVENTS-PALETTE: NEW — set_category_visible/get_category_visible/list_categories/reorder_category/set_widget_visible/reset)
        r.register(new ViewfuncInvokeTool());         // universal: 89 actions (FIX-A-VIEW: was 7)
        // (was 37 view_set_widget_* stubs — dropped; ViewSetPropertyTool covers all of them)

        // ===== Event category (4 tools: 2 specialized + 2 universal) =====
        r.register(new EventAttachTool());             // specialized
        r.register(new EventListTool());               // specialized
        r.register(new EventManageTool());             // universal: 10 actions (was 10 stubs)
        r.register(new CustomEventManageTool());        // universal: 5 actions (FIX-E-EVENTS-PALETTE: NEW — register/unregister/list/get/update for mod.hilal.saif.events.EventsHandler)

        // ===== Component category (4 tools: 1 specialized + 3 universal) =====
        r.register(new ComponentAddTool());           // specialized
        r.register(new ComponentManageTool());        // universal: 8 actions (was 8 stubs)
        r.register(new ComponentSetPropertyTool());   // universal: 21 actions (was 21 stubs)
        r.register(new CustomComponentManageTool());  // universal: 6 actions (FIX-C-RESOURCES: NEW — manages custom Java UI components via wq.getCustomComponent())

        // ===== Block category (10 tools: 1 specialized + 9 universal) =====
        r.register(new BlockAddTool());               // specialized
        r.register(new BlockManageTool());           // universal: 10 actions (was 10 stubs)
        r.register(new ControlFlowTool());           // universal: 11 actions (was 11 stubs)
        r.register(new VariableManageTool());        // universal: 5 actions (was 5 stubs)
        r.register(new ListManageTool());           // universal: 12 actions (was 6 stubs; +6 new)
        r.register(new MapManageTool());            // universal: 9 actions (was 3 stubs; +6 new)
        r.register(new MathOperationTool());       // universal: 18 actions (was 10 stubs; +8 new)
        r.register(new StringOperationTool());    // universal: 13 actions (was 8 stubs; +5 new)
        r.register(new MoreblockManageTool());   // universal: 3 actions (was 3 stubs)
        r.register(new CustomBlockManageTool()); // universal: 10 actions (NEW — manages global custom block palettes)

        // ===== Project category (6 tools: 2 specialized + 4 universal) =====
        r.register(new ProjectSetAppNameTool());     // specialized (kept for backward compat)
        r.register(new ProjectSetPackageNameTool()); // specialized (kept for backward compat)
        r.register(new ProjectManageTool());        // universal: 7 actions (was 7 stubs)
        r.register(new ProjectSetPropertyTool());   // universal: 8 actions (was 8 stubs)
        r.register(new ProjectEnableFeatureTool()); // universal: 3 actions (was 3 stubs)
        r.register(new ThemeManageTool());              // universal: 5 actions (FIX-E-THEME-ICON: NEW — apply_preset/generate_random/reset/get_current/list_presets)

        // ===== Build category (3 universal tools) =====
        r.register(new BuildActionTool());          // universal: 9 actions (FIX-D-PROJECT: was 7, +2 actions +8 setting keys)
        r.register(new ExportActionTool());         // universal: 4 actions (was 4 stubs)
        r.register(new ProguardManageTool());       // universal: 6 actions (FIX-D-PROJECT: NEW)

        // ===== Java category (6 tools: 2 specialized + 1 universal + 2 diff + 1 todo) =====
        r.register(new JavaEditFileTool());         // specialized
        r.register(new JavaReadFileTool());        // specialized
        r.register(new JavaModifyClassTool());    // universal: 11 actions (was 12 stubs)
        r.register(new DiffEditFileTool());       // SEARCH/REPLACE diff editing (port of Cline replace_in_file)
        r.register(new ApplyPatchTool());         // multi-file unified diff (port of Cline apply_patch)

        // ===== Library/Permission category (5 universal tools; FIX-D-PROJECT) =====
        r.register(new LibraryEnableTool());      // universal: 10 actions (FIX-D-PROJECT: was specialized w/ library_type enum)
        r.register(new LibraryManageTool());      // universal: 6 actions (was 6 stubs)
        r.register(new LibraryConfigureTool());   // universal: 15 actions (FIX-D-PROJECT: NEW)
        r.register(new NativeLibManageTool());    // universal: 5 actions (FIX-D-PROJECT: NEW)
        r.register(new PermissionManageTool());    // universal: 6 actions (was 6 stubs)

        // ===== Manifest/AppCompat/XML category (3 universal tools) =====
        r.register(new ManifestManageTool());   // universal: 10 actions (FIX-D-PROJECT: was 6, -1 duplicate add_permission, +5 new)
        r.register(new AppcompatManageTool()); // universal: 3 actions (was 3 stubs)
        r.register(new XmlCommandManageTool()); // universal: 3 actions (was 3 stubs)

        // ===== Resource category (7 universal tools; FIX-C-RESOURCES added 5, FIX-E-THEME-ICON added 1) =====
        r.register(new ValuesXmlManageTool());      // universal: 12 actions (was 12 stubs)
        r.register(new ImageManageTool());          // universal: 9 actions (FIX-C-RESOURCES: NEW — add/edit/delete/list/rotate/flip/import_from_collection/add_to_collection)
        r.register(new FontManageTool());           // universal: 5 actions (FIX-C-RESOURCES: NEW — add/edit/delete/list/import_from_collection)
        r.register(new SoundManageTool());          // universal: 5 actions (FIX-C-RESOURCES: NEW — add/edit/delete/list/import_from_collection)
        r.register(new ResourceFileManageTool());   // universal: 7 actions (FIX-C-RESOURCES: NEW — create_folder/create_xml/import/edit/rename/delete/list in resource/)
        r.register(new AssetsManageTool());         // universal: 7 actions (FIX-C-RESOURCES: NEW — create_file/create_folder/import/edit/rename/delete/list in assets/)
        r.register(new IconCreatorTool());              // universal: 6 actions (FIX-E-THEME-ICON: NEW — create_adaptive/create_legacy/set_foreground/set_background/delete/list)

        // ===== Meta tools =====
        r.register(new AskQuestionTool());
        r.register(new SubmitAndExitTool());
        r.register(new TodoListTool());            // AI-managed TODO list (port of Cline task_progress)

        // ===== Filesystem tools (port of Cline list_files + search_files) =====
        r.register(new ListFilesTool());           // list project files recursively (tree view)
        r.register(new SearchFilesTool());         // regex search across project file contents

        // ===== Web tools (port of Cline web_search + web_fetch) =====
        r.register(new WebSearchTool());           // DuckDuckGo search, no API key needed
        r.register(new WebFetchTool());            // fetch URL content, strip HTML to text

        return r;
    }
}
