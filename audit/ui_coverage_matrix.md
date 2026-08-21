# Sketchware Pro original UI on Creator Runtime — coverage matrix

| Original UI surface or action | Original implementation | Runtime status after audit | Verification / remaining work |
|---|---|---|---|
| Creator launcher / white entry screen | `CreatorHomeActivity` + `activity_creator_home.xml` | Connected | FAB and body launch `DesignActivity`; initial screen remains white |
| Next FAB | `creator_entry_control` | Connected | Existing instrumentation click test verifies resumed `DesignActivity` |
| Original editor toolbar | `DesignActivity` + `design.xml` | Connected | Original toolbar retained |
| View tab | `ViewEditorFragment` + `ViewEditor` | Connected | Original palette/property surface retained; widget-config null crash fixed |
| Built-in widget palette | `ViewEditorFragment.e()` + `WidgetsCreatorManager` | Connected | Null/empty Gson list normalized; JVM regression test added |
| Property panel | `ViewProperty` | Connected | Original `design.xml` panel and callbacks retained |
| Events tab | `rs` | Connected | Original event list retained; event row opens `LogicEditorActivity` |
| Components tab | `br` | Connected | Original component adapter retained |
| AI tab | Original `ChatFragment` | Connected | Existing runtime AI tools work is retained from earlier commits |
| Visual block editor | `LogicEditorActivity` + `logic_editor.xml` | Patched for runtime | Runtime project ID now forwarded from View/Events; Android 13 storage guard bypassed; instrumentation launch test added |
| Logic block save | `LogicEditorActivity.ProjectSaver` / `eC` | Connected through legacy bridge | DesignActivity imports final legacy snapshot on `finish()`; needs native test confirmation |
| Design drawer | `DesignDrawer` | Connected | Original 19 manager/source/resource actions remain; existing instrumentation checks IDs |
| Screen/file selector | `file_name_container` + `ViewSelectorActivity` | Connected | Original callback retained |
| Options menu | `btn_options` + `PopupMenu` | Connected | Retained as sole bottom action button |
| Run button | `btn_run` in original bottom bar | Removed as requested | BuildTask remains legacy-safe with nullable reference; no Run view is inflated |
| Main/live application surface after editor | Runtime native renderer in `CreatorProjectActivity` | Connected | CreatorHome launches live-only surface after DesignActivity returns |
| Entry control inside live application | `creator_project_entry_control` | Connected | Live-only click now opens original `DesignActivity`, not custom editor shell |
| Custom runtime editor shell | `CreatorProjectActivity` non-live mode | Demoted / bypassed for project entry | Home and live-only entry use original DesignActivity; shell remains for compatibility/tests |
| Runtime widget projection | `CreatorLegacyProjectBridge.projectRuntimeViews()` | Connected | Legacy ViewBeans are projected before original editor opens |
| Runtime snapshot import | `CreatorLegacyProjectBridge.importLegacyProject()` | Connected | Called on DesignActivity resume and final finish after save |
| Runtime metadata | `syncRuntimeMetadata()` | Connected | Stable project identity and metadata synchronized via bridge |
| Original resource/library/manifest screens | Legacy activities + DesignDrawer | Connected through original drawer | Must be covered by final native smoke matrix |
| Original block collections/custom blocks | Legacy drawer/LogicEditor | Connected through original drawer and LogicEditor | Must be covered by final native smoke matrix |

## Audit inventory

The repository contains an inventory of 78 original Activity classes, 191 editor-related layout/menu resources, and 86 extracted original editor action/navigation references in `audit/original_activities.txt`, `audit/original_editor_resources.txt`, and `audit/original_editor_actions.txt`. Detailed findings are in `audit/ui_runtime_findings.md` and `audit/screen_coverage_details.txt`.

## Main remaining verification gates before push

The final local gate must compile all source and instrumentation tests, run all JVM tests, verify the original DesignActivity and LogicEditorActivity launch tests, verify live-only return to DesignActivity, and perform static resource/diff checks. GitHub Actions must remain untouched until these gates and all code changes are complete; then only `Creator Runtime Android` should be triggered by the single push.
