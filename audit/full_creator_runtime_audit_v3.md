# Creator Runtime Full Audit v3

## Executive finding

The previous implementation was not a valid end-to-end Creator Runtime flow. The green build and prior native workflow did not prove the actual user path because several critical boundaries were not exercised together. The most important failure was a destructive lifecycle loop: `DesignActivity.onResume()` called `syncCreatorRuntimeBoundary()`, which projected the previous runtime document back into the legacy stores every time the editor resumed. That could overwrite a button or activity change made in the original Sketchware editor before the runtime import occurred.

The second major failure was that the real activity-manager paths were not consistently runtime-aware. `DesignActivity.launchActivity()` propagated the runtime project ID, but the manually created `ViewSelectorActivity` intent did not. `ManageViewActivity` and its fragment also used explicit legacy permission checks, and the manager checked permission before reading the runtime extra. On Android 13 this could close the activity manager before the runtime-specific editor path was reached.

A third failure was migration idempotency. The old starter marker could already be set while the legacy Intent/component/event records were absent. The previous one-time gate then refused to repair the broken installation. The current migration introduces a versioned repair, after which deletion is preserved.

## Requirements-to-path matrix

| Requirement | Authoritative path | Audit result before v3 | v3 correction |
|---|---|---|---|
| First surface is original editor main activity | Manifest -> `DesignActivity.prepareCreatorRuntimeLaunch()` | Launcher was changed, but actual runtime manager/lifecycle path was incomplete | Preserve launcher-aware `DesignActivity` and repair downstream context propagation |
| Continue is editable in original View editor | Runtime document -> bridge -> `eC` view store -> `ViewEditorFragment` | Projection could be overwritten on resume; old marker could prevent repair | Remove resume re-projection and add versioned starter migration |
| Continue has Intent component and click blocks | `ensureLegacyStarterIntent()` using `mainFile.getJavaName()` | Correct key was present, but seed could be skipped permanently on broken installs | Add starter seed version 2 migration |
| Editor activity exists from start | `CreatorRuntimeDefaults` + `ensureLegacyStores()` | Existing installations could retain one-activity state | Add `/editor` screen and `editor.xml` provisioning |
| Editor activity is locked | `CreatorScreen.locked` + `ProjectFileBean` option bit | No persisted lock model existed | Add runtime JSON lock field, legacy lock bit, indicators and edit toggle |
| Lock is visible beside every activity | `manage_view_list_item.xml` and ViewSelector popup | New icon could be pushed off-screen by `match_parent` row width | Use weighted row layout and add indicators to both lists |
| Lock can be changed in editor | `AddViewActivity` feature list | No runtime-only lock control | Add `Locked` feature row and persist option |
| Activity manager works in Creator Runtime | `ManageViewActivity` -> `Fw` -> `AddViewActivity` | Explicit `super` permission checks bypassed runtime override; extra was read too late | Read extra before permission check, call override, propagate context |
| ViewSelector add/edit works in runtime | `DesignActivity.showAvailableViews()` -> `ViewSelectorActivity` | Runtime extra was missing from manual path | Propagate and restore runtime ID, pass it to AddViewActivity |
| Back preserves newly edited main screen | `SaveChangesProjectCloser` -> `DesignActivity.finish()` -> runtime import | `onResume` could overwrite fresh legacy state before import | `onResume` imports legacy only; initial projection remains in `onCreate` |
| No old save/restore prompt in runtime | `ProjectLoader` | Legacy unsaved-data dialog could appear during runtime startup | Suppress restore dialog in runtime mode |
| Versions is in original right sidebar | `DesignDrawer` -> `AboutActivity(select=changelog)` | Item existed but drawer could be locked closed | Unlock runtime drawer and retain existing changelog route |
| Lower chevron absent | `DesignActivity` bottom options view | Existing runtime hide path retained | Native assertion verifies `btn_options=GONE` |

## Concrete evidence inspected

`AndroidManifest.xml` declares `DesignActivity` as the only `MAIN`/`LAUNCHER`. `DesignActivity.prepareCreatorRuntimeLaunch()` provisions the single runtime project and attaches `sc_id` plus `creator_runtime_project_id`. `DesignActivity.onCreate()` performs the initial runtime-to-legacy projection, while the corrected `onResume()` no longer repeats that destructive projection. `SaveChangesProjectCloser` persists `jC.d`, `jC.b`, `jC.a`, and `jC.c` before calling `finish()`, after which `finish()` imports the final legacy snapshot and starts the live native surface.

`ViewEditorFragment` reads the active layout from `jC.a(sc_id).d(projectFileBean.getXmlName())`. `br` reads components from `jC.a(sc_id).e(projectFile.getJavaName())`. `rs` reads events from `jC.a(sc_id).g(currentActivity.getJavaName())`. This establishes that missing View/Components/Events records are upstream bridge or lifecycle failures, not renderer failures. The bridge now uses the Java-name key for starter component/event seeding and keeps the view store serialization path through `eC.n(...)`.

`BaseAppCompatActivity` already propagates `creator_runtime_project_id` through its `startActivity` and `startActivityForResult` overrides. The audit found that this was insufficient for manual launcher paths and explicit permission calls, so those paths are now fixed directly as well.

## Corrective architecture

The runtime document remains the authoritative model. On initial `DesignActivity` creation, the document is migrated and projected once into the app-private legacy Sketchware stores. The original editor owns its ordinary UI and save lifecycle while open. During editor child-activity resumes, legacy state is imported into the runtime document rather than overwritten from the previous runtime snapshot. On Back, the existing asynchronous save task completes first; only then does `finish()` import the final legacy state and open the native live renderer.

The starter migration is now versioned. Documents with the old initialized marker but no starter version receive one repair migration that restores absent starter content. After version 2 is persisted, the system does not recreate a user-deleted Continue widget or event. The legacy Intent seed has an independent versioned migration so old installations with a stale seed marker can be repaired once.

The runtime document now contains `main` and `editor` screens. `main` is the entry screen and is unlocked by default. `editor` is locked by default. The lock is serialized in the runtime document and mirrored to the legacy `ProjectFileBean` option bit. The original activity manager and ViewSelector show shield indicators; the existing activity edit screen exposes the lock toggle in Creator Runtime mode.

## Test and acceptance plan

The required acceptance flow is: launch the app; verify the original DesignActivity opens on `main`; verify the activity list contains `main` and `editor`; verify `editor` is locked; verify `main` contains Continue in the View tab; verify Components contains the `creator_editor` Intent; verify Events/Logic contains the Continue click event and blocks; add a new button and configure behavior; press Back; verify the native live screen contains both the original Continue and the new button with the changed behavior; reopen the editor; verify the same main activity and records remain; open Versions from the right drawer; and verify the lower chevron is absent.

Local gates currently pass: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:compileDebugAndroidTestJavaWithJavac`. Native coverage includes launcher/editor structure, legacy provisioning, starter Intent/component/event records, locked editor activity, runtime activity-manager reachability, drawer availability, and the no-chevron contract. A separate local connected test cannot run without a connected device; the repository workflow remains the native emulator gate.

## Remaining limitations

This audit covers the complete changed Creator Runtime boundary and the critical original-editor paths implicated by the reported failures. It is not a literal line-by-line audit of every file in Sketchware Pro. The legacy obfuscated store API still limits complete inverse projection of every event mutation, and not every individual runtime service implementation has independent behavioral coverage. Those limitations must not be described as 100% repository coverage.
