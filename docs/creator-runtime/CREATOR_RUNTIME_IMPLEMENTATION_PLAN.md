# Creator Runtime Implementation Plan

## Chosen architecture

The existing `CreatorProjectActivity` already contains the production native renderer, runtime event dispatch, service lifecycle, and protected Creator entry control. The safest implementation is to reuse that renderer rather than create a second preview renderer.

`CreatorHomeActivity` becomes the launcher and the white first screen. Pressing its circular entry control opens `CreatorProjectActivity` in editor mode. When the user leaves the editor, the activity starts the same `CreatorProjectActivity` in `LIVE_ONLY` mode. This live-only mode hides editor controls but uses the same session, renderer, runtime executor, native Android views, events, persistence, entry overlay, and shake recovery. The live-only screen is therefore the actual Creator Runtime application surface, not a preview.

The live-only entry control opens a fresh editor-mode `CreatorProjectActivity`. The system Back button from live-only returns to Creator Home. This preserves the requested loop without duplicating widget behavior or maintaining a separate preview model.

## State and navigation

| State | Activity/mode | Visible behavior |
|---|---|---|
| Creator Home | `CreatorHomeActivity` | White first screen, legacy/sidebar access, circular entry control. |
| Editor | `CreatorProjectActivity` without `LIVE_ONLY` | Runtime-native canvas plus human controls, AI may mutate the same `CreatorRuntimeSession`. |
| Live application | `CreatorProjectActivity` with `LIVE_ONLY` | Native project surface, host-owned Creator overlay, shake recovery; editor controls hidden. |
| Recovery | Host-owned sheet | Opens editor, resets protected overlay, or returns Home. |

## Required code changes

1. Add a launcher intent filter to `CreatorHomeActivity` and remove the MAIN/LAUNCHER filter from legacy `MainActivity` while preserving its VIEW import filters.
2. Add an explicit `LIVE_ONLY` intent extra and `liveOnly` field to `CreatorProjectActivity`.
3. In live-only mode hide the editor header and mutation controls, make the canvas fill the activity, and make the Creator overlay open editor mode instead of opening the appearance dialog.
4. In editor mode, Back starts live-only mode rather than returning to summary-only Home. On live-only Back, finish to Creator Home.
5. Keep `CreatorRuntimeSession` as the single application-scoped authoritative state and ensure activity recreation re-renders from persisted document.
6. Improve `CreatorEntryControl` with a protected host descriptor while keeping ordinary project placement/label changes valid.
7. Move shake lifecycle/onboarding semantics so both editor and live-only modes have recovery, and add deterministic detector logic that can be unit-tested.
8. Add Home/live/editor instrumentation tests and runtime unit tests before the final push.

## Explicit boundary

This implementation does not introduce a generated APK or a separate visual preview. The live-only activity is the runtime executor itself. Static compilation remains available only for legacy workflows or unsupported capabilities outside the Creator Runtime contract; it is not part of the normal Creator edit → Back flow.

## Acceptance sequence

`MAIN/LAUNCHER → CreatorHomeActivity → editor mode → runtime mutation → editor Back → live-only native activity → overlay opens editor → shake opens recovery → live Back → Creator Home`.
