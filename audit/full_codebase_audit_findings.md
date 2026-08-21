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

## Core persistence finding from v2 audit

`CreatorRevisionStore` bounded document snapshots to its configured capacity, but `operationResults` and named `checkpoints` were previously unbounded. In a long-lived single-project session this could grow without limit. The correction bounds both auxiliary maps and applies checkpoint eviction at checkpoint creation time. The regression test `revisionHistoryAndCheckpointsRemainBounded` initially caught a pruning-order bug; after moving eviction into `checkpoint()`, the targeted `CreatorRuntimeEngineTest` suite passed.

## Scope honesty

The v2 inventory measures 85 Creator Runtime/creator production Java files (about 10,700 lines), 50 test files (about 8,758 lines), seven large original-boundary files totalling about 8,000 lines, nine relevant resource files, and five workflow files. These numbers make a literal complete line-by-line audit a multi-phase activity; compilation and a focused test run are not evidence that every line is correct.

## Startup/shake audit findings

The manifest exposes `CreatorHomeActivity` as the only `MAIN`/`LAUNCHER` activity; `DesignActivity` is not a launcher, and `CreatorProjectActivity` is not exported. Creator Home and Project use the AppCompat-compatible splash/Starting theme, while DesignActivity uses the original small-toolbar theme.

The pure `CreatorShakeDetector` already had direct unit coverage for threshold, debounce, invalid samples, and reset. The Activity-facing `CreatorShakeRecovery` had no active-registration guard and did not reset debounce state when stopped. It now ignores queued sensor callbacks after `stop()`, resets detector state on stop, and passed compile plus the existing detector test suite.

## Capability/service matrix evidence

Existing unit tests cover the 30-entry legacy component capability matrix, the 30-entry runtime component-to-service mapping, and compatibility analyzer behavior. These tests passed in the v2 audit. They prove completeness of declarations and mapping, not full behavioral correctness of all 30 Android service implementations; those implementations remain a separate sequential review area.

## Storage/file service evidence

The storage review covered project-scoped SharedPreferences selection, external permission gating, canonical-root validation, copy/move/delete recursion, and error status conversion. Existing `CreatorFileServiceTest` and `CreatorStorageServiceTest` were located and passed in the targeted JVM run. This confirms tested behavior for those services but does not close audit coverage for all remaining service implementations.

## Service contract finding

The shared service contract uses explicit statuses (`SUCCEEDED`, `PERMISSION_REQUIRED`, `DENIED`, `UNSUPPORTED_ARGUMENT`, `FAILED`) and immutable output maps. The dispatcher only invokes registered services and project documents cannot load arbitrary classes. This matches the safety requirement that user/AI actions operate through reviewed runtime capabilities; behavioral review of each implementation remains in progress.

## Widget query service review

`CreatorWidgetQueryService` was reviewed across its read and mutation branches. It requires a widget ID/action, resolves only registered runtime widgets, gates typed operations with `instanceof`, validates range-sensitive values for ratings, seek bars, date/time pickers, and uses explicit unsupported-argument results. Existing native widget tests cover broad widget/service behavior. No source change was made in this pass without a failing regression case.

## Starter migration evidence

The v2 audit added `CreatorRuntimeDefaultsTest`. It verifies that a blank document receives one main screen, one `creator_continue_button`, one editor click binding, and a persisted initialization marker; a second migration is a no-op. It also verifies that a marked document with the button removed is not repopulated. The test passed.

## Home source-of-truth finding

The first audit pass found that `CreatorHomeActivity` hardcoded `creator_continue_button` as the visible host control. That contradicted the requirement that users can delete Continue or assign the editor transition to another button. Home now scans click bindings for the editor Intent service action, uses the binding target widget's label/visibility/layout properties, hides the host control when no editor-bound widget exists, and tags the host view with the actual target widget ID. The first compile exposed and fixed a missing import; the subsequent compile, androidTest compilation, and JVM suite passed.

## Runtime environment boundary finding

`CreatorRuntimeEnvironment.handlePermissionResult()` assumed Android would always provide non-null permission/result arrays. The audit added null-safe handling while preserving the pending-action event contract. The correction passed compile and the full JVM suite.

## Stale test contract finding

`CreatorRuntimeWorkflowTest` was still validating the obsolete flow in which an independent `ENTRY_CONTROL_UPDATE` changed the visible entry label/placement. It now creates an ordinary button widget and attaches a typed Intent service click block, then asserts the event target and keeps the legacy metadata at its default compatibility value. The updated workflow test passed.

## AI tools audit evidence

`ActivityListTool` exposes a no-argument discovery schema and reads all current Creator Runtime screens from the single session. Registry, catalog-visibility, and Creator Runtime schema tests passed, confirming that `activity_list` and `creator_runtime` are registered and visible once. This verifies discovery/tool exposure; it does not by itself prove every AI execution branch is behaviorally correct.

## Original editor bridge audit

The bridge was read through view projection, legacy import, resource collection, component provisioning, and starter Intent block seeding. Root parents are normalized to the legacy literal `root`, all reviewed runtime widget types map through `toLegacyType`, and the original editor receives an Intent component plus visible `intentSetScreen`/`startActivity` blocks when the starter button exists.

A residual limitation is confirmed rather than hidden: runtime event bindings are not fully projected back into the obfuscated legacy `eC` event store, and the bridge does not safely delete stale legacy event/component records when a user removes or reassigns the editor button. The available source-level API did not expose a safe inverse deletion method, so no guessed call was added. This remains an explicit blocker for claiming lossless runtime-to-legacy event round-trip.

## Executor mutation coverage

The executor test gap identified in the inventory was closed with two behavior tests. `REPLACE_EVENT` now has coverage for reassigning an existing binding and dispatching its new blocks; `DETACH_EVENT` has coverage for removing a binding through an ordinary click behavior. The full `CreatorRuntimeExecutorTest` suite passed.
