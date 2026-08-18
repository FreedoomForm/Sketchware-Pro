# Sketchware Capability Audit Inventory

## Audit rule

No capability is considered migrated merely because it has a name in a UI. Every row must ultimately contain: its authoritative legacy source, serialized input representation, Creator Runtime execution tier, user-visible representation, automated evidence, and any permission/native-build requirement.

| Domain | Legacy authority | Known scope | Current audit status |
|---|---|---|---|
| Core view types | `com.besome.sketch.beans.ViewBean` | Types 0–18: layouts, text, input, image, web, progress/list/spinner, selection controls, scrolling, FAB, ads, map | Inventory captured; R1 importer/renderer covers a subset; remainder unclassified for final audit. |
| Extension view types | `mod.agus.jcoderz.beans.ViewBeans` | Types 19–48: 30 extension widgets/layouts | Inventory captured; final tier and test evidence pending. |
| Events and logic | `EventBean`, block generator/manager packages, legacy event controller | Activity/view/component/drawer events, blocks, more blocks | Runtime v1 has explicit bindings + four blocks; parity audit pending. |
| Components | Component beans, component tool registry, project libraries | Device/API/component configuration and callbacks | Inventory pending. |
| Resources/styles | `ProjectResourceBean`, XML builders, view/layout/image/text beans | Images, colors, dimensions, styles, manifest resources | Inventory pending. |
| Project files | `ProjectBean`, `ProjectFileBean`, storage managers | Activities, fragments, custom views, drawer, source/logic/resource joins | Inventory pending. |
| Build/export | `ProjectBuilder`, compiler and resource/dex pipelines | Debug/release APK, AAB, source generation, compiler diagnostics | Inventory pending. |
| Editor/AI/recovery | Main/design/editor, AI tool registry, Creator Runtime | Human actions, agent parity, history, recovery, native handoff | Creator Runtime evidence exists; full parity audit pending. |

## Base view inventory

| Legacy range | Examples | Required audit treatment |
|---|---|---|
| Layouts | LinearLayout, RelativeLayout, Horizontal/VerticalScrollView | R1 renderer or explicit native fallback with preserved layout data. |
| Basic widgets | Button, TextView, EditText, ImageView, CheckBox, Switch | R1 renderer, property diff test, event test, import test. |
| Collections/controls | ListView, Spinner, GridView, RecyclerView, SeekBar, RatingBar | Runtime collection/control implementation or R3 fallback; no silent downgrade. |
| Device/media/web | WebView, MapView, VideoView, AdView, YoutubePlayerView | Audited R2 plugin or R3 fallback with permission/build provenance. |
| Advanced layouts | Card, tabs, pager, bottom nav, collapsing toolbar, refresh, text-input | R1/R2/R3 decision plus structural import test. |
| Extension/specialized | OTP, CodeView, Lottie, PatternLock, WaveSideBar, custom Google controls | Explicit tier, plugin dependency evidence, and functional behavior fixture. |

## Acceptance evidence per capability

1. A unit or instrumentation test imports a legacy representation without losing its stable identifier or parent relationship.
2. A runtime or plugin test exercises the principal behavior, not merely object construction.
3. A user-visible compatibility report contains the exact tier and reason.
4. Any R3 capability creates a revision-pinned native-build request; any R0 capability blocks migration with an actionable explanation.
5. AI and user actions use the same operation contract if the capability can be changed through Creator Runtime.

## Prohibited audit shortcuts

The audit may not mark a capability R1 because a similar Android widget exists, mark a plugin R2 before permission/error paths are tested, or count a legacy file as migrated when its events or resources are ignored. The final report must list every exception by stable capability identifier.
