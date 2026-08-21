# AI and Legacy Function Gap Matrix

## Audit scope

This audit compares the user-facing AI and primary Sketchware entry surfaces with the current Creator Runtime/editor window. It intentionally distinguishes **engine capability**, **tool discoverability**, and **navigation availability**. A capability existing in an old activity or tool class is not considered covered until the user can reach it from the Creator editor and the AI can invoke the same supported operation without an unrelated required argument.

## AI tools

| Area | Current finding | Required correction |
|---|---|---|
| Registry duplication | The registry intentionally contains 37 top-level names, including standalone compatibility tools and umbrella tools. The underlying umbrella subtools are not separately registered, but the catalog exposes internal names and descriptions that can look duplicated to users. | Add a canonical visible capability descriptor layer. Group standalone compatibility aliases under their canonical umbrella capability in the catalog and system prompt; keep only one invocation route in the agent-facing schema where safe. Preserve aliases only for backward compatibility and mark them hidden. |
| Tool catalog | `AiToolCatalogSheet.groupByCategory` lists registry entries directly, so compatibility aliases and umbrella entries can appear as separate rows even when they represent the same user action family. | Deduplicate by canonical capability key and show a human-readable capability row with supported actions. Add tests that no canonical key appears twice. |
| Activity/screen discovery | There is no dedicated zero-argument read-only `activity_list`/`screen_list` tool. Some event tools operate against the current Java/layout context and require an active name, which makes discovery fail when the user only wants to see available activities/screens. | Add `activity_list` (or `screen_list` with explicit alias) with no required arguments. It must return all project screens/activities and stable IDs. Make event listing accept an optional target; when omitted, list all project events. |
| Required name semantics | Generic universal tools legitimately require `name` for create/update/delete operations, but list/read operations must not inherit that requirement. | Enforce action-specific schemas: `name` required only for targeted mutation actions; list/get actions must have no name requirement unless a filter is explicitly chosen. Add schema tests for every list action. |
| Tool context mismatch | `SketchwareToolContext.currentJavaName` is a layout/XML context despite legacy naming. Tools that call it as a current Java/activity context can produce misleading “name required” or wrong-scope errors. | Rename internal semantics to current layout/activity target or provide separate `currentLayoutName` and `currentActivityName` accessors. Resolve list operations from project metadata when no target is selected. |
| Tool errors | Many tools return raw “name is required” messages from nested actions, making the agent retry with an irrelevant name. | Standardize validation errors with action, accepted fields, and a user-readable next step. Do not ask for an activity name when the requested operation is a list. |
| AI tools section | Meta controls are correctly hidden from catalog, but the catalog still uses raw technical tool names. | Display canonical labels and one-line user capabilities while retaining machine tool names only in expandable diagnostics. |

## AI update/download page

`AISettingsActivity` exposes `VersionsFragment` through `nav_ai_versions` in `ai_settings_menu.xml`. `VersionsFragment` fetches GitHub releases and `ReleasesAdapter` exposes APK download actions. This is outside the Creator Runtime editing contract and must be removed from the AI settings drawer.

The required change is to remove the menu item, the fragment route and the AI versions strings/layout references from active navigation. The release classes may remain temporarily unused if they are referenced by non-AI legacy screens; they must not be reachable from the AI settings section.

## Legacy primary Sketchware page

The old `MainActivity` combines two bottom navigation tabs and a drawer. The drawer is implemented by `MainDrawer` and contains the following actions:

| Legacy action | Existing handler | Creator editor sidebar route |
|---|---|---|
| Projects | `ProjectsFragment` via `item_projects` | Open project/session manager surface. |
| Sketchub | `ProjectsStoreFragment` via `item_sketchub` | Open Sketchub/store surface without leaving Creator session. |
| About the team | `AboutActivity` | Launch existing About activity in a child route. |
| Changelog | `AboutActivity` with `select=changelog` | Launch existing changelog route. |
| App information | `ProgramInfoActivity` | Launch existing program info route. |
| Create keystore | `NewKeyStoreActivity` | Launch existing keystore route. |
| Settings | `AppSettings` | Launch existing app settings route. |
| SwAssist | External URL | Preserve as external link with explicit confirmation/return path. |
| Discord/Telegram/GitHub | External URLs | Preserve under a Related links group. |
| Creator Runtime | `CreatorHomeActivity` | Replace self-launch with the current editor/home route. |
| New project | `MainActivity.binding.createNewProject` | Add a sidebar `New project` action that creates a runtime document and opens it in the same editor session. |

The sidebar must be inside `CreatorProjectActivity` and must be host-owned. It must remain available in editor and live-only modes, while the visible Creator recovery control remains separately protected from project mutations.

## Runtime migration gaps

The existing R1 audit proves broad service/opcode/view coverage, but the following user-facing capabilities still need explicit runtime equivalents or guarded routes:

| Gap | Required runtime treatment |
|---|---|
| Project list/switching | Add project manager state and sidebar route; preserve active session on return. |
| Sketchub browsing | Keep legacy store activity reachable from sidebar or implement a runtime store host; do not silently remove it. |
| New project | Create an empty runtime document through the same transaction/session path. |
| About/changelog/program info/settings/keystore | Reuse existing native activities from the sidebar, with back navigation to the editor. These are host functions, not project graph operations. |
| External links | Reuse existing intents and show a return-safe route. |
| Activity/screen discovery | Add read-only runtime metadata operation and AI tool. |
| AI catalog canonicalization | Add capability key/alias metadata and deduplicated display. |
| Update/download page | Remove from AI navigation; do not expose APK download as an AI capability. |

## Acceptance criteria

The work is complete only when the editor sidebar exposes all legacy primary actions, the AI settings drawer no longer contains the Versions/download page, list operations work without an activity/name argument, canonical tool capabilities are shown once, and tests prove that aliases cannot create duplicate user-visible entries.

## Implementation status after this pass

The following corrections are now implemented in the working tree. The LLM receives a canonical deduplicated schema payload through `ToolRegistry.toAgentJsonSchemas()`, while old alias names remain registry-compatible for previously saved conversations. The catalog and system prompt use the same canonical visibility policy. A zero-argument `activity_list` tool lists Creator Runtime screen IDs, routes, and root widgets without requiring an activity or layout name.

The AI Versions/download page has been removed from `AISettingsActivity`, `ai_settings_menu.xml`, `AppSettings`, `ChatFragment` automatic update flow, and the unused release/update implementation and resources. The editor now contains a host-owned sidebar exposing New Project, Projects, Sketchub, About, Changelog, App information, Create keystore, Settings, SwAssist, Discord, Telegram, and GitHub. New Project creates a fresh persisted Creator Runtime document through `CreatorRuntimeSession.createNewProject()`; Projects and Sketchub reuse the existing native MainActivity routes, while application information/settings and external links reuse their existing handlers.

The remaining boundary is deliberate: legacy project/store activities are now reachable from the editor sidebar, but they are not yet rewritten as native Creator Runtime graph operations. Their functionality remains available through the existing stable activities while the runtime equivalents can be migrated incrementally without silently removing user capabilities.
