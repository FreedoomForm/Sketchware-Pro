# Creator Runtime Codebase Audit v2

**Repository:** `FreedoomForm/Sketchware-Pro`  
**Branch:** `creator-runtime`  
**Audit baseline:** `0ac0cefd0` plus the uncommitted corrections listed below.  
**Purpose:** Compare the implementation with the agreed Creator Runtime product idea: one active project, a white first/live screen, a widget-owned Continue/Intent entry into the original Sketchware editor, autosave, live runtime updates, shake fallback, and user/AI operations constrained to visible runtime capabilities.

## Scope and honesty of completion

This audit is deliberately **traceable rather than rhetorical**. The repository inventory contains 85 Creator Runtime/creator production Java files (approximately 10,700 lines), 50 Java test files (approximately 8,758 lines), seven large original-editor boundary files, nine relevant Creator Runtime resource files, and five workflow files. The inventory and production-to-test matrix are stored beside this report.

A literal human reading and semantic verification of every line in the entire Sketchware Pro repository cannot be honestly claimed from this session. The work therefore covers every relevant Creator Runtime class family, the startup/live/editor bridge, AI tool registry/schema paths, the affected original-editor boundary, and the tests connected to those paths. Unrelated legacy screens outside the runtime boundary are not represented as fully audited. This distinction is intentional and is a release risk, not a hidden success claim.

## Acceptance criteria

| Requirement | Audit result | Evidence or limitation |
|---|---|---|
| Exactly one active Creator Runtime project | Implemented and retained | Single-project session/store path and removal of create/list/switch actions were preserved. |
| First launcher screen is white with one Continue entry | Implemented with model-driven host rendering | `CreatorHomeActivity` now resolves the actual widget/event that opens the editor instead of hardcoding only `creator_continue_button`. |
| Live screen is the same editable screen model | Implemented for runtime widget rendering | `CreatorProjectActivity` renders persisted widgets in the white canvas; native instrumentation coverage exists. |
| Continue is an ordinary editable Sketchware button | Implemented for starter provisioning | Bridge provisions a normal button, Intent component, and visible `intentSetScreen`/`startActivity` blocks. |
| User may delete/reassign editor entry | Implemented in authoritative runtime IR | `WIDGET_REMOVE`, `EVENT_REPLACE`, and `EVENT_DETACH` are validated/reduced and exposed to AI/runtime blocks; starter marker does not recreate a deleted button. |
| Autosave and live mutation persistence | Existing implementation retained and regression-tested | Session-aware executor and DesignActivity runtime boundary persist mutations; full JVM suite passes. |
| Shake is a reserve editor entry path | Implemented and hardened | Shake detector tests cover threshold/debounce/invalid/reset; recovery wrapper now ignores callbacks after stop and resets debounce state. |
| No obsolete host EntryControl dialog | Removed | Obsolete dialog/helpers and unused strings were removed; stale workflow test was updated. |
| AI can discover screens without a required name | Implemented | `activity_list` has a no-argument schema and registry/catalog tests pass. |
| Only user-capable runtime tools are exposed | Partially verified | Registry/schema and capability matrices pass; each service implementation still requires ongoing behavior review beyond this audit slice. |
| Lossless runtime-to-original-editor event round-trip | **Residual limitation** | Legacy-to-runtime import and starter event provisioning exist. A safe inverse deletion/replacement API for obfuscated `eC` event storage was not available in source/build artifacts, so stale legacy events after arbitrary reassign/delete are not claimed as fully solved. |
| Exactly one Creator Runtime workflow | Prepared | Stale prior runs were cancelled; the final push must be followed by one push-triggered Creator Runtime Android workflow only. |

## Confirmed corrections in this audit

The core persistence audit found that `CreatorRevisionStore` bounded document snapshots but allowed operation-result and checkpoint maps to grow indefinitely. Both auxiliary maps are now bounded by the configured capacity, and checkpoint eviction occurs at checkpoint creation. A regression test initially exposed pruning order and is now green.

The startup audit found that the Home activity selected the old hardcoded starter ID even if a user moved the editor behavior to another button. Home now scans click bindings for the editor Intent action and uses the target widget's label, visibility, placement, and ID. The live canvas remains sourced from the persisted document widget tree.

The shake audit found that queued sensor callbacks could arrive after `stop()` and that debounce state crossed lifecycle pauses. The wrapper now has an active-registration guard and resets detector state on stop. A null-safe permission-result correction was also made in the shared runtime environment boundary.

The resource audit removed obsolete Home title/detail, host EntryControl dialog, placement chooser, and recovery dialog strings. An unrelated existing URL-template warning was corrected by marking the resource `formatted=false`.

The test audit added coverage for expression/condition evaluation, starter migration and deletion persistence, bounded revisions/checkpoints, executor event replacement/detachment, and the updated widget-owned live-edit workflow. The stale workflow test no longer treats `ENTRY_CONTROL_UPDATE` as the canonical visible entry behavior.

## Test evidence completed before final push

The following local checks passed during the audit: targeted Creator Runtime engine tests, expression/condition tests, shake detector tests, capability/service matrix tests, storage/file service tests, AI tool registry/catalog/schema tests, the updated Creator Runtime workflow test, the executor suite including replacement/detachment, repeated compile and androidTest Java compilation, and the full `testDebugUnitTest` suite.

The final pre-push gate must still run after this report is added and should include JVM tests, debug APK assembly, debug androidTest APK assembly, and androidTest Java compilation. Native emulator validation belongs to the single final Creator Runtime Android workflow and must not be inferred from local JVM success.

## Files supporting the audit

The following files are generated or maintained as audit evidence: `full_audit_inventory_v2.md`, `runtime_production_test_matrix_v2.txt`, `core_persistence_risk_scan_v2.txt`, and `full_codebase_audit_findings.md`. Earlier audit matrices remain in the directory for historical comparison.

## Release decision

The audited Creator Runtime slice is materially closer to the product idea and has green local JVM/compile evidence. It is **not honest to label the entire Sketchware Pro repository 100% line-audited or the runtime-to-legacy event round-trip lossless**. The final CI result must be reported together with this residual limitation.
