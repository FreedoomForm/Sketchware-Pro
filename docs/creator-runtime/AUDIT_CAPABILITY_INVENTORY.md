# Sketchware Capability Audit Inventory

## Audit rule

No capability is considered migrated merely because it has a name in a UI. Every row must ultimately contain: its authoritative legacy source, serialized input representation, Creator Runtime execution evidence, user-visible representation, and automated behavior evidence. The active architecture permits only runtime-native execution (R1) or a visible blocked exception (R0); it does not permit plugins or native-build fallbacks.

| Domain | Legacy authority | Known scope | Current audit status |
|---|---|---|---|
| Core view types | `com.besome.sketch.beans.ViewBean` | Types 0–18: layouts, text, input, image, web, progress/list/spinner, selection controls, scrolling, FAB, ads, map | All types have explicit R1 importer/renderer mappings; device behavior evidence is pending. |
| Extension view types | `mod.agus.jcoderz.beans.ViewBeans` | Types 19–48: 30 extension widgets/layouts | All types have explicit R1 importer/renderer mappings; device behavior evidence is pending. |
| Events and logic | `EventBean`, block generator/manager packages, legacy event controller | Activity/view/component/drawer events, blocks, more blocks | Artifact importer converts a supported typed subset and blocks unrecognized executable opcodes as visible R0; parity audit pending. |
| Components | Component beans, component tool registry, project libraries | Device/API/component configuration and callbacks | All 30 IDs map to R1 runtime service IDs; device/configuration behavior evidence is pending. |
| Resources/styles | `ProjectResourceBean`, XML builders, view/layout/image/text beans | Images, colors, dimensions, styles, manifest resources | Inventory pending. |
| Project files | `ProjectBean`, `ProjectFileBean`, storage managers | Activities, fragments, custom views, drawer, source/logic/resource joins | Inventory pending. |
| Build/export | `ProjectBuilder`, compiler and resource/dex pipelines | Debug/release APK, AAB, source generation, compiler diagnostics | Inventory pending. |
| Editor/AI/recovery | Main/design/editor, AI tool registry, Creator Runtime | Human actions, agent parity, history, recovery, native handoff | Creator Runtime evidence exists; full parity audit pending. |

## Base view inventory

| Legacy range | Examples | Required audit treatment |
|---|---|---|
| Layouts | LinearLayout, RelativeLayout, Horizontal/VerticalScrollView | R1 renderer with preserved layout data; otherwise explicit R0 block. |
| Basic widgets | Button, TextView, EditText, ImageView, CheckBox, Switch | R1 renderer, property diff test, event test, import test. |
| Collections/controls | ListView, Spinner, GridView, RecyclerView, SeekBar, RatingBar | Typed R1 runtime collection/control implementation; no silent downgrade. |
| Device/media/web | WebView, MapView, VideoView, AdView, YoutubePlayerView | Direct R1 runtime implementation with permission/configuration and device-behavior evidence. |
| Advanced layouts | Card, tabs, pager, bottom nav, collapsing toolbar, refresh, text-input | Typed R1 structural import test plus device behavior evidence. |
| Extension/specialized | OTP, CodeView, Lottie, PatternLock, WaveSideBar, custom Google controls | R1 runtime renderer plus functional behavior fixture. |

## Acceptance evidence per capability

1. A unit or instrumentation test imports a legacy representation without losing its stable identifier or parent relationship.
2. A runtime test exercises the principal behavior, not merely object construction.
3. A user-visible compatibility report contains the exact tier and reason.
4. Any R0 capability blocks migration with an actionable explanation; no native-build request may be created.
5. AI and user actions use the same operation contract if the capability can be changed through Creator Runtime.

## Prohibited audit shortcuts

The audit may not mark a capability R1 because a similar Android widget exists, or count a legacy file as migrated when its events or resources are ignored. The final report must list every R0 exception by stable capability identifier and must not declare 100% while any such exception remains.
