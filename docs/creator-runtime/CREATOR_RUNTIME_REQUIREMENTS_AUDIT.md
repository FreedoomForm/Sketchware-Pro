# Creator Runtime Requirements Audit

## Executive finding

The repository already contains a substantial Creator Runtime vertical slice. The core engine has an authoritative project document, validated operations, revision/checkpoint support, persistence, AI operation mapping, native widget rendering, service dispatch, and a protected entry-control model. Existing JVM and Android tests cover many runtime services and all imported legacy view types.

The product requirements are **not yet fully covered**. The largest gaps are not the R1 runtime engine itself; they are the installed-app experience around it: Creator Home is not currently the launcher, the Home screen is a metadata/summary surface rather than the live native application surface, Back from the editor therefore returns to a stale/non-live Home, recovery is currently wired only while the editor activity is open, and the new experience lacks end-to-end instrumentation coverage. Autosave is immediate synchronous persistence rather than a named debounced session pipeline, and the entry control currently supports label/placement rather than a complete protected style descriptor.

## Coverage matrix

| Requirement | Status | Evidence | Gap or required action |
|---|---|---|---|
| CR-01 one installed runtime executes supported projects without per-change APK compile | **Partial** | `CreatorRuntimeEngine`, `CreatorRuntimeSession`, `CreatorProjectActivity`, `CreatorRuntimeNativeWidgetTest` | Live runtime exists, but the installed app still enters legacy MainActivity and the full user journey is not wired around the live surface. Keep compile outside the Creator edit path and prove it with integration tests. |
| CR-02 clean Creator Home replaces first screen | **Not covered** | `CreatorHomeActivity` exists, but `AndroidManifest.xml:109-117` gives MAIN/LAUNCHER to `MainActivity` | Make Creator Home the launcher while preserving legacy project management through an explicit sidebar/legacy route. |
| CR-03 lower-right circular entry control | **Partial** | `activity_creator_home.xml:50-61`, `CreatorHomeActivity` | A rounded MaterialButton exists and opens the project, but it is not the launcher experience and is not tested in the installed entry path. Use a clearly circular FAB-like control and add instrumentation. |
| CR-04 project editor for human and AI using one path | **Mostly covered** | `CreatorRuntimeSession.apply`, `CreatorRuntimeEngine.apply`, `CreatorProjectActivity.apply`, `CreatorRuntimeWorkflowTest` | Add a shared transaction facade/actor coverage showing UI and AI operations update exactly the same session and screen. |
| CR-05 model and native surface live synchronized | **Mostly covered** | `CreatorProjectActivity.render`, session listener, `typedWidgetEventsAndDrawerSurviveNativeRerender` | Home must render the same live document after Back; add a shared live-surface renderer instead of summary text. |
| CR-06 Back returns to changed native application surface | **Not covered** | `CreatorProjectActivity.onBackPressed` finishes; Home only renders title/detail in `CreatorHomeActivity.renderDocument` | Implement a live application host/surface in Creator Home or a host activity that is the Back destination, with protected Creator overlay. |
| CR-07 no manual Save/Compile in normal flow | **Mostly covered** | `CreatorProjectActivity` has no Save/Compile in live edit path; session persists accepted operations | Add UI test proving a mutation, Back, recreation, and live result without invoking compile. |
| CR-08 durable autosave and safe restore | **Partial** | `CreatorRuntimeSession.apply` immediately calls `CreatorRuntimeProjectStore.save`; codec/store tests exist | Add explicit session lifecycle flush/recovery, corruption fallback diagnostics, transaction save tests, and a named autosave/recovery abstraction if debounce semantics are required. |
| CR-09 protected Creator button inside hosted application | **Partial** | `CreatorEntryControl`, `CreatorProjectActivity.entryControl` | It exists in the editor activity, not yet in the post-Back hosted application surface. Add host-owned overlay to live Home/app surface. |
| CR-10 move/configure Creator control | **Partial** | `CreatorEntryControl` supports `visible`, `label`, five placements; editor dialog applies placement | Add style descriptor (size/color/icon/appearance), persist it, and test recreation. Keep host recovery metadata separate from project widgets. |
| CR-11 cannot permanently lock out editor | **Mostly covered at model level** | `CreatorEntryControl` deliberately has no recovery toggle; `ENTRY_CONTROL_UPDATE` is validated and reset flow exists | Hide/restrict ordinary `visible=false` behavior for the host recovery route or add an always-available host recovery control. Test a project mutation cannot remove access. |
| CR-12 shake recovery | **Partial** | `CreatorShakeRecovery` is wired in `CreatorProjectActivity` with threshold/debounce | Move recovery listener to the hosted app/home lifecycle, add filter/cooldown abstraction tests, and verify shake from the live application surface. |
| CR-13 first-run shake onboarding | **Partial** | `CreatorProjectActivity.showRecoveryOnboardingOnce` and `recovery_onboarded` preference | Onboarding must appear from the installed Creator Home/host experience and be instrumented. |
| CR-14 transferred Sketchware runtime feature surface | **Mostly covered / prior audit green** | Existing final coverage docs, runtime service/opcode/view tests, 49/49 legacy view native test, 35/35 service audit | Preserve existing coverage and add Creator Home/live-host regression tests; do not claim product completion from engine-only coverage. |
| CR-15 AI tools match real UI capability | **Covered for registry direction** | `ToolRegistryInitializer`, catalog filtering, registry tests, ChatFragment actions | Add a Creator transaction integration test for AI mutations and keep backend-only tools absent from registry/catalog. |
| CR-16 project switching/sidebar | **Not covered in Creator experience** | Legacy `MainActivity`/drawer exists; Creator Home has `creator_open_legacy` button | Add Creator sidebar with project switching and active-session restoration; preserve legacy entry as an explicit compatibility route. |
| CR-17 failure preserves data/recovery | **Mostly covered in core** | Engine validation is atomic; revision store/checkpoints and compatibility inspector exist | Add malformed/corrupt document recovery UI and failed-operation Android test that verifies live surface and recovery route remain usable. |
| CR-18 local suite plus one final CI validation | **Pending** | Previous local suites exist; current new requirements have no complete test suite yet | Finish implementation and tests first, run full local validation, then make one final push/CI launch; fix any CI failures and rerun only when needed. |

## Confirmed architectural strengths

The runtime already has a good foundation for the requested product. `CreatorRuntimeSession` is application-scoped and exposes one `CreatorRuntimeEngine` to screens. `CreatorRuntimeEngine.apply` validates, deduplicates, reduces, revisions, and logs operations. `CreatorRuntimeProjectStore` persists the encoded document on every accepted operation. `CreatorProjectActivity` renders actual Android views and dispatches their events back through the runtime executor. `CreatorEntryControl` is host-owned and deliberately does not expose a recovery toggle. These are the correct primitives for a runtime-first product.

## Confirmed product gaps

The installed application still starts in legacy `MainActivity`, not `CreatorHomeActivity`. The current Creator Home contains summary `TextView` values and a button, but it does not render the project’s native widget tree. Consequently, pressing Back from `CreatorProjectActivity` cannot yet reveal the changed application itself. The current shake detector is attached to the editor activity, so it is unavailable precisely when the user is outside the editor and has lost the visible route back. The existing Android test suite validates the runtime canvas but does not validate the launcher, Home, Back-to-live-surface, project switching, overlay recovery, or shake onboarding requirements.

## Required implementation order

1. Introduce a reusable `CreatorLiveSurface` renderer/controller that can render the authoritative document in both the editor canvas and the post-editor host surface without duplicating widget behavior.
2. Make `CreatorHomeActivity` the installed launcher and turn it into the live application host, while keeping legacy project management reachable from a sidebar route.
3. Add a host-owned Creator overlay to the live host surface and route its action to `CreatorProjectActivity`.
4. Move shake recovery and first-run onboarding to the host lifecycle, preserving editor recovery when the editor is open.
5. Strengthen `ActiveProjectSession`/autosave lifecycle semantics and add project switching.
6. Add JVM and instrumentation coverage for every matrix row marked Partial or Not covered.
7. Run the full local suite, perform one final push/CI validation, and update this audit with the final evidence.
