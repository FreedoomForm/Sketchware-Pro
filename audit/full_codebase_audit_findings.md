# Full codebase audit findings

## Scope

The audit covers the Creator Runtime production code under `app/src/main/java/pro/sketchware/creator` and `.../runtime`, the original Sketchware boundary files (`DesignActivity`, `LogicEditorActivity`, `br`, `rs`, `BaseAppCompatActivity`), runtime resources, instrumentation/JVM tests, and both workflows under `.github/workflows`.

## Initial findings from inventory

1. Current HEAD at audit start is `88054d587f11e9eb025c4c21a2fc066ac13118f5`.
2. `CreatorRuntimeDefaults` now creates a persisted `creator_continue_button` widget and runtime click binding, while `CreatorLegacyProjectBridge` seeds a legacy Intent component and `intentSetScreen`/`startActivity` blocks.
3. `CreatorHomeActivity` renders a host-level Continue control from the persisted widget properties and now owns shake fallback.
4. `CreatorProjectActivity` renders the persisted widget tree in `creator_preview_canvas`, but still contains a large non-live editor mode and historical entry-control methods; these must be checked for contradictions and dead paths.
5. `DesignActivity` has two runtime boundary sync paths (`onCreate` listener and `onResume`) and an autosave/back path; both must be checked for ordering and duplicate import/projection.
6. The inventory scan shows only Creator Runtime Android workflow is intended for `creator-runtime`; `android.yml` must be checked for branch-ignore and no duplicate trigger.
7. A generated inventory file is currently untracked: `audit/full_codebase_audit_inventory.md`. It is audit evidence and should be included only if the final audit artifact is desired; it must not be mixed into product code accidentally.

## Audit discipline

For each reviewed area, record: requirement, relevant file/line range, current behavior, contradiction or missing behavior, exact correction, test evidence, and residual limitation. Do not claim line-by-line correctness from compilation alone.

## Confirmed model mismatch: mutation vocabulary

The original operation vocabulary had no `WIDGET_REMOVE`, `EVENT_REPLACE`, or `EVENT_DETACH`. Consequently, a claim that the user can delete Continue, replace it, or assign editor navigation to another button was not representable as an authoritative runtime revision. The audit correction adds these operation types, validator rules, reducer behavior, recursive subtree removal, parent-child cleanup, and target-event cleanup. This must be covered by JVM tests before commit.

## Confirmed bridge mismatch: event round-trip

`CreatorLegacyArtifactImporter` has a tested legacy-to-runtime conversion for Intent and callback blocks, including `intentSetScreen` and `startActivity`. The bridge's `projectRuntimeViews` projects ViewBeans but does not project runtime `CreatorEventBinding`/`CreatorRuntimeBlock` records back into the legacy EventBean/BlockBean store. Therefore, a runtime/AI event reassignment can be authoritative in the runtime document while the original Logic editor still shows stale legacy blocks. The exact `eC` event-store deletion/update API is not present in source or the local javac/runtime classes inspected, so no guessed inverse writer should be introduced. This remains a residual gap requiring either the original eC dependency API or a dedicated tested bridge adapter.

## Confirmed executor mismatch

The runtime executor originally handled `ATTACH_EVENT` only. It now also handles `REPLACE_EVENT` and `DETACH_EVENT` typed blocks, with tests covering top-level operation reducer behavior and AI schema/mapping. Inverse legacy projection remains separate and unresolved.

## Host control and behavior-block findings

The old `CreatorProjectActivity.editEntryControl()` dialog mutated the separate `CreatorEntryControl` metadata rather than the editable button widget. This contradicted the requirement that the visible Continue be an ordinary Sketchware button that can be edited or deleted in the original editor. The dialog and placement helpers were removed from the live surface; Home now mirrors the persisted widget for label/visibility while shake remains host-owned.

The typed block enum and executor originally supported `ATTACH_EVENT` only. `REPLACE_EVENT` and `DETACH_EVENT` are now present in the block enum, executor, AI schema, mapper, operation validator, reducer, and tests. This enables behavior-level reassignment, but it does not by itself solve inverse projection into the legacy Logic editor; that bridge limitation remains explicitly tracked above.
