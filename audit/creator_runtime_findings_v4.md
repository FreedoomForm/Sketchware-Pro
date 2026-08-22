# Creator Runtime — Findings v4 (working audit)

This is a working evidence log. It must be rewritten into the final audit after corrective changes and final CI.

## Confirmed findings

| ID | Severity | Evidence | Status |
|---|---|---|---|
| F-001 | High | `AddViewActivity.handleCreateFile()` constructs a new `ProjectFileBean` from toolbar/status/FAB/drawer flags but omits `featureLocked`, although the runtime-only Locked row is shown and `handleEditFile()` persists the lock bit. | Confirmed defect: newly created activities cannot retain the selected lock state. |
| F-002 | High | Existing native tests assert runtime document, legacy records, projection from runtime to legacy, and editor-to-live transition, but do not mutate a legacy `ViewBean` in `main.xml`, persist it through the exact editor lifecycle, assert live canvas has both Continue and the new button, then reopen DesignActivity and assert the button remains. | Coverage gap matching the user's reported device failure. |
| F-003 | Medium | `CreatorLegacyArtifactImporter` maps legacy `intentSetScreen` to an `intent/configure_screen` service call and `startActivity` to `intent/start`; it does not rewrite to a direct `open_creator_editor` opcode. `CreatorIntentService.start` special-cases intent ID `creator_editor` before navigation, so the starter pair can still open the editor if the ID is preserved. | Semantically plausible but lacks a dedicated importer+executor regression test. |
| F-004 | High | `CreatorRuntimeSession.importLegacySnapshot()` persists and notifies synchronously, while `DesignActivity` listener schedules projection/refresh on the UI thread. The current onResume ordering avoids the prior stale projection, but exact child-activity save/finish ordering remains unproven by an end-to-end test. | Requires native round-trip test. |
| F-005 | High | `CreatorLegacyProjectBridge.projectScreen()` supports only runtime widget types mapped by `toLegacyType`; unsupported runtime types are silently skipped (`legacyType < 0`). | Must be classified against the full Creator widget catalog; cannot claim all runtime widgets round-trip. |
| F-006 | High | `CreatorProjectActivity.renderWidget()` has explicit renderers for many types but falls back to a generic `LinearLayout` for unknown types, which can make a projected widget visually present but behaviorally/visually incorrect. | Requires capability matrix and explicit unsupported behavior. |
| F-007 | Medium | `CreatorLegacyArtifactImporter` stores unsupported executable block chains in `legacy.deferredEvents` and does not create executable bindings for those chains. | Correctly non-silent metadata behavior, but user-visible support is incomplete and must be reported per opcode. |
| F-008 | Medium | Existing `CreatorRuntimeDefaults` v2 migration intentionally repairs missing starter content only before current version; after current version it preserves deletion. This is correct only if old-marker repair is completed before user edits are considered current. | Semantics covered by JVM tests; still requires install-upgrade scenario evidence. |
| F-009 | Medium | `ProjectFileBean.@ActivityOption` originally omitted `OPTION_ACTIVITY_LOCKED`, even though the bit and helper existed. This weakens static contract checking around the new lock feature. | Repaired locally; needs compile and native create-lock coverage. |
| F-010 | High | `CreatorOperationValidator.validateEventAttach()` requires `targetWidgetId` to exist in the widget map. Imported activity/component events are intentionally represented with non-widget targets, but AI-created activity/component event bindings would be rejected by the same public operation path. | Scope gap: runtime event model and validator do not share one target-kind contract. |
| F-011 | High | `CreatorRuntimeCompatibilityInspector` and `CreatorRuntimeWidgetCatalog` classify a type as R1 based on vocabulary, while `CreatorProjectActivity.renderWidget()` falls back to a generic `LinearLayout` for unknown/unhandled cases and `CreatorLegacyProjectBridge.toLegacyType()` silently skips unknown types. | Must reconcile catalog, renderer, and projection with a generated coverage check. |
| F-012 | Medium | `ToolVisibilityPolicy` hides legacy aliases from agent schemas, but `ToolExecutor.execute()` dispatches every registered tool by name without checking visibility. Hidden aliases remain executable for old conversations; this is intentional compatibility, but it means tools that should be removed from the user-facing surface are not actually removed from the executable surface. | Requires explicit policy decision and tests distinguishing compatibility aliases from forbidden tools. |
| F-013 | High | `allLegacyViewTypesImportThroughProductionRuntimeOnNativeRuntime()` asserts only that 49 records import and the canvas is non-null. It does not verify each type's native class, properties, click behavior, or reverse projection. | Coverage is enumeration-level, not full functional coverage; previous “all types” implication is too strong. |
| F-014 | High | `SketchwareToolContext.persistViewToDisk()` catches every throwable and only logs it; `ViewAddWidgetTool` can therefore return success even when the legacy view cache was not serialized. | Persistence must return/throw a checked failure before reporting a successful AI mutation. |

## Evidence grades at this point

- F-001: B/C (create-path repair is implemented and compiles; dedicated native create-lock interaction test remains open).
- F-002: B (exact native legacy-ViewBean → Back → live canvas → reopen test is now present; emulator execution remains required).
- F-003: C (source/dataflow only).
- F-004: C (source/dataflow only).
- F-005: C (static conversion map only).
- F-006: C (renderer source only).
- F-007: B/C (exact Intent opcode importer regression is present; native executor/live click evidence remains required).
- F-008: B (JVM migration tests; upgrade persistence not native-tested).
- F-009: B/C (annotation and create-path repairs compile; native coverage remains open).
- F-010: B/C (validator now accepts activity sentinel and imported component targets; executor/native behavior remains open).
- F-011: C (static vocabulary comparison; generated coverage pending).
- F-012: C (source proof; policy test pending).
- F-013: C (native count-only test; per-type behavioral coverage remains open).
- F-014: B/C (persistence failure now propagates instead of being silently reported as success; failure-path test remains open).

## Immediate corrective priorities

1. Run the new native legacy round-trip and dedicated lock-create test on API 30/API 34 emulators.
2. Add native executor/live-click evidence for imported `intentSetScreen(creator_editor, DesignActivity)` plus `startActivity(creator_editor)`.
3. Build a complete legacy-widget mapping table and mark unsupported renderer/projection pairs explicitly.
4. Decide and test whether hidden AI aliases remain executable compatibility routes or must be removed from the runtime registry.
5. Strengthen all-types coverage from count-only to per-type renderer/projection assertions where the production contract permits.
6. Inspect all original editor child paths and runtime-only suppression points for lifecycle, permission, and context propagation.
