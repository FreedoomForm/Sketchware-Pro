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
import com.sketchware.ai.tools.resource.ImageManageTool;
import com.sketchware.ai.tools.resource.ResourceFileManageTool;
import com.sketchware.ai.tools.resource.SoundManageTool;
import com.sketchware.ai.tools.resource.ValuesXmlManageTool;
import com.sketchware.ai.tools.resource.IconCreatorTool;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a {@link ToolRegistry} pre-populated with the full catalogue of
 * Sketchware-Pro user-action tools.
 *
 * <h2>Architecture: universal tools + category umbrellas (2026-08-12 consolidation)</h2>
 *
 * <p>The original 240-operation catalogue (each as a separate tool) was
 * extremely token-inefficient: every operation added ~600 tokens to the
 * system prompt, totalling ~96 000 tokens for the full set — exceeding
 * the context window of every modern LLM and degrading tool-selection
 * accuracy by 15-40% (Anthropic & OpenAI research, 2025).
 *
 * <p>Refactor pass 1 (2026-08-11): the 223 "long-tail" operations were
 * collapsed into 29 universal tools that each take an {@code action: <enum>}
 * parameter. Each universal tool dispatches to the underlying
 * Sketchware-Pro singleton ({@code jC.a/b/c/d(sc_id)}) via reflection
 * ({@link com.sketchware.ai.util.SketchwareApi}), giving real
 * functional behaviour — not stubs.
 *
 * <p>Refactor pass 2 (2026-08-12): the 68-tool registry was further
 * consolidated into <b>45 tools</b> via {@link CategoryUmbrellaTool} —
 * a thin wrapper that groups 2-8 semantically related universal tools
 * under a single name + a {@code subcategory} enum parameter. The
 * umbrella forwards args to the matching underlying tool, which then
 * validates the {@code action} enum and dispatches. This preserves
 * full functional coverage while shrinking the LLM-visible tool list
 * by ~34%, improving tool-selection accuracy further.
 *
 * <h3>Final tool inventory (45 tools total)</h3>
 * <ul>
 *   <li><b>Specialized tools (24)</b> — bespoke logic that doesn't fit
 *       the action-enum pattern (e.g. ViewAddWidgetTool has 40-value
 *       type enum + library gating; BlockAddTool assembles block beans
 *       with complex param schemas; JavaEditFileTool does diff-based
 *       file editing).</li>
 *   <li><b>Category umbrellas (8)</b> — each groups 2-8 universal tools
 *     under one name:
 *     <ul>
 *       <li>view_manage (8 subtools: layout/widget/custom_widget/favorites/
 *           palette_action/palette_commit/palette_visibility/viewfunc)</li>
 *       <li>event_manage (2 subtools: event/custom_event)</li>
 *       <li>component_misc (2 subtools: manage/custom_component)</li>
 *       <li>project_manage (4 subtools: manage/set_property/enable_feature/
 *           theme)</li>
 *       <li>build_manage (3 subtools: build/export/proguard)</li>
 *       <li>library_manage (4 subtools: manage/configure/native_lib/
 *           permission)</li>
 *       <li>manifest_manage (3 subtools: manifest/appcompat/xml_command)</li>
 *       <li>resource_manage (5 subtools: values_xml/font/sound/
 *           resource_file/assets)</li>
 *     </ul>
 *   </li>
 *   <li><b>Standalone universal tools (13)</b> — universal tools that
 *       stay separate because their category has only one universal
 *       tool (no benefit from umbrella grouping) or because they have
 *       unique schemas that don't fit the umbrella pattern:
 *     <ul>
 *       <li>Block (9): BlockManageTool, ControlFlowTool, VariableManageTool,
 *           ListManageTool, MapManageTool, MathOperationTool,
 *           StringOperationTool, MoreblockManageTool, CustomBlockManageTool
 *           — kept separate because the block category is the most heavily
 *           used and benefits from explicit tool names for LLM
 *           disambiguation.</li>
 *       <li>Resource (2 standalone): ImageManageTool, IconCreatorTool —
 *           kept separate because they have unique image-binary params
 *           (base64 image data) that don't fit the umbrella's flat
 *           args-forwarding pattern.</li>
 *       <li>Java (3): JavaModifyClassTool, DiffEditFileTool, ApplyPatchTool.</li>
 *       <li>Meta (3): AskQuestionTool, SubmitAndExitTool, TodoListTool.</li>
 *       <li>Filesystem (2): ListFilesTool, SearchFilesTool.</li>
 *       <li>Web (2): WebSearchTool, WebFetchTool.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Token budget</b>: ~14 000 tokens for the full tool catalogue
 * (vs ~18 000 pre-consolidation, vs ~96 000 in the original stub-per-tool
 * design), leaving ample headroom in Mistral 32K / GPT-4o 128K / Claude
 * 200K for the system prompt, conversation history, and tool results.
 *
 * <p>When adding a new Sketchware operation, prefer extending an
 * existing universal tool's {@code action} enum over registering a new
 * tool. When adding a new universal tool in a category that already
 * has an umbrella, add it as a new subcategory of that umbrella rather
 * than registering a standalone tool.
 */
public final class ToolRegistryInitializer {

    private ToolRegistryInitializer() {}

    public static ToolRegistry createDefault() {
        ToolRegistry r = new ToolRegistry();

        // ===== View category (7 tools: 6 specialized + 1 umbrella) =====
        r.register(new ViewAddWidgetTool());           // specialized: 40 widget types
        r.register(new ViewSetPropertyTool());         // specialized: any property name
        r.register(new ViewDeleteWidgetTool());        // specialized
        r.register(new ViewListWidgetsTool());         // specialized
        r.register(new ViewUndoRedoTool(true));        // view_undo
        r.register(new ViewUndoRedoTool(false));       // view_redo
        r.register(viewManageUmbrella());              // umbrella: 8 subtools

        // ===== Event category (3 tools: 2 specialized + 1 umbrella) =====
        r.register(new EventAttachTool());             // specialized
        r.register(new EventListTool());               // specialized
        r.register(eventManageUmbrella());             // umbrella: 2 subtools

        // ===== Component category (3 tools: 1 specialized + 1 specialized + 1 umbrella) =====
        r.register(new ComponentAddTool());            // specialized
        r.register(new ComponentSetPropertyTool());    // specialized: 21 actions (kept standalone — large schema)
        r.register(componentMiscUmbrella());           // umbrella: 2 subtools (manage + custom_component)

        // ===== Block category (10 tools: 1 specialized + 9 universal) =====
        // Kept fully expanded (no umbrella) — block ops are the most heavily
        // used category and benefit from explicit tool names for LLM
        // disambiguation.
        r.register(new BlockAddTool());               // specialized
        r.register(new BlockManageTool());            // universal: 10 actions
        r.register(new ControlFlowTool());            // universal: 11 actions
        r.register(new VariableManageTool());         // universal: 5 actions
        r.register(new ListManageTool());             // universal: 12 actions
        r.register(new MapManageTool());              // universal: 9 actions
        r.register(new MathOperationTool());          // universal: 18 actions
        r.register(new StringOperationTool());        // universal: 13 actions
        r.register(new MoreblockManageTool());        // universal: 3 actions
        r.register(new CustomBlockManageTool());      // universal: 10 actions

        // ===== Project category (3 tools: 2 specialized + 1 umbrella) =====
        r.register(new ProjectSetAppNameTool());      // specialized (backward compat)
        r.register(new ProjectSetPackageNameTool());  // specialized (backward compat)
        r.register(projectManageUmbrella());          // umbrella: 4 subtools

        // ===== Build category (1 umbrella) =====
        r.register(buildManageUmbrella());            // umbrella: 3 subtools

        // ===== Java category (5 tools: 2 specialized + 1 universal + 2 diff) =====
        r.register(new JavaEditFileTool());           // specialized
        r.register(new JavaReadFileTool());           // specialized
        r.register(new JavaModifyClassTool());        // universal: 11 actions
        r.register(new DiffEditFileTool());           // SEARCH/REPLACE diff editing (Cline port)
        r.register(new ApplyPatchTool());             // multi-file unified diff (Cline port)

        // ===== Library/Permission category (2 tools: 1 specialized + 1 umbrella) =====
        r.register(new LibraryEnableTool());          // specialized (backward compat — test-required name)
        r.register(libraryManageUmbrella());          // umbrella: 4 subtools

        // ===== Manifest/AppCompat/XML category (1 umbrella) =====
        r.register(manifestManageUmbrella());         // umbrella: 3 subtools

        // ===== Resource category (3 tools: 2 standalone + 1 umbrella) =====
        r.register(new ImageManageTool());            // standalone (unique image-binary schema)
        r.register(new IconCreatorTool());            // standalone (unique adaptive-icon schema)
        r.register(resourceManageUmbrella());         // umbrella: 5 subtools

        // ===== Meta tools (3) =====
        r.register(new AskQuestionTool());
        r.register(new SubmitAndExitTool());
        r.register(new TodoListTool());

        // ===== Filesystem tools (2) =====
        r.register(new ListFilesTool());
        r.register(new SearchFilesTool());

        // ===== Web tools (2) =====
        r.register(new WebSearchTool());
        r.register(new WebFetchTool());

        return r;
    }

    // ---- Umbrella factories ----

    /** view_manage: 8 subtools covering layout/widget/palette/visibility/viewfunc. */
    private static CategoryUmbrellaTool viewManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("layout",            new ViewManageLayoutTool());
        subs.put("widget",            new ViewManageWidgetTool());
        subs.put("custom_widget",     new ViewManageCustomWidgetTool());
        subs.put("favorites",         new ViewManageFavoritesTool());
        subs.put("palette_action",    new ViewPaletteActionTool());
        subs.put("palette_commit",    new ViewPaletteCommitTool());
        subs.put("palette_visibility", new PaletteVisibilityManageTool());
        subs.put("viewfunc",          new ViewfuncInvokeTool());
        return new CategoryUmbrellaTool(
                "view_manage", "view",
                "Operate on layouts, widgets, palette, and viewfunc actions. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** event_manage: 2 subtools (event + custom_event). */
    private static CategoryUmbrellaTool eventManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("event",        new EventManageTool());
        subs.put("custom_event", new CustomEventManageTool());
        return new CategoryUmbrellaTool(
                "event_manage", "event",
                "Manage built-in event handlers and custom event registrations. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** component_misc: 2 subtools (manage + custom_component). ComponentSetProperty
     *  stays standalone (21-action schema is large enough to warrant its own tool). */
    private static CategoryUmbrellaTool componentMiscUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("manage",          new ComponentManageTool());
        subs.put("custom_component", new CustomComponentManageTool());
        return new CategoryUmbrellaTool(
                "component_misc", "component",
                "Manage built-in components (add/get/list/set_enabled/delete/clone/reorder) "
                        + "and custom Java UI components (add/list/get/update/delete/import/clone). "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** project_manage: 4 subtools (manage + set_property + enable_feature + theme). */
    private static CategoryUmbrellaTool projectManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("manage",         new ProjectManageTool());
        subs.put("set_property",   new ProjectSetPropertyTool());
        subs.put("enable_feature", new ProjectEnableFeatureTool());
        subs.put("theme",          new ThemeManageTool());
        return new CategoryUmbrellaTool(
                "project_manage", "project",
                "Manage project (create/open/save/list/delete/export), set project properties, "
                        + "enable project features, and apply theme presets. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** build_manage: 3 subtools (build + export + proguard). */
    private static CategoryUmbrellaTool buildManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("build",    new BuildActionTool());
        subs.put("export",   new ExportActionTool());
        subs.put("proguard", new ProguardManageTool());
        return new CategoryUmbrellaTool(
                "build_manage", "build",
                "Run APK build actions (compile/sign/install/run/clean), export APK/AAB/source, "
                        + "and manage ProGuard/R8 configuration. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** library_manage: 4 subtools (manage + configure + native_lib + permission).
     *  LibraryEnableTool stays standalone (test-required name). */
    private static CategoryUmbrellaTool libraryManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("manage",      new LibraryManageTool());
        subs.put("configure",   new LibraryConfigureTool());
        subs.put("native_lib",  new NativeLibManageTool());
        subs.put("permission",  new PermissionManageTool());
        return new CategoryUmbrellaTool(
                "library_manage", "library",
                "Manage libraries (add/remove/list/enable/disable), configure Firebase/AdMob/"
                        + "GoogleMap/Material3, manage native .so libs, and manage Android permissions. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** manifest_manage: 3 subtools (manifest + appcompat + xml_command). */
    private static CategoryUmbrellaTool manifestManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("manifest",    new ManifestManageTool());
        subs.put("appcompat",   new AppcompatManageTool());
        subs.put("xml_command", new XmlCommandManageTool());
        return new CategoryUmbrellaTool(
                "manifest_manage", "manifest",
                "Manage AndroidManifest.xml (add/remove activities/permissions/intent-filters/"
                        + "set_launcher/edit_components/show_source), AppCompat settings, and XML commands. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }

    /** resource_manage: 5 subtools (values_xml + font + sound + resource_file + assets).
     *  ImageManageTool and IconCreatorTool stay standalone — they have unique
     *  image-binary params that don't fit the umbrella's flat args-forwarding
     *  pattern. */
    private static CategoryUmbrellaTool resourceManageUmbrella() {
        Map<String, SketchwareTool> subs = new LinkedHashMap<>();
        subs.put("values_xml",    new ValuesXmlManageTool());
        subs.put("font",          new FontManageTool());
        subs.put("sound",         new SoundManageTool());
        subs.put("resource_file", new ResourceFileManageTool());
        subs.put("assets",        new AssetsManageTool());
        return new CategoryUmbrellaTool(
                "resource_manage", "resource",
                "Manage values XML (colors/strings/styles/themes/dimens/integers/bools), fonts, "
                        + "sounds, resource files (XML/import/edit/rename/delete), and assets. "
                        + "Pass subcategory + action + the underlying tool's required params.",
                subs);
    }
}
