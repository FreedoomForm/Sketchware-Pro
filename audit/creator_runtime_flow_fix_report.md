# Creator Runtime Main Activity Flow Fix Report

## Final repository state

The changes are on `FreedoomForm/Sketchware-Pro`, branch `creator-runtime`. The final remote commit is `9e8363f99376c4c07ff949a9eb33c6db7a8e0b74` (`9e8363f99`). The working tree is clean.

## User flow implemented

`DesignActivity` is now the only `MAIN`/`LAUNCHER` activity. When Android starts it without project extras, it provisions the single Creator Runtime project and opens the original Sketchware editor on the persisted `main` activity. `CreatorHomeActivity` remains only as a compatibility redirect and never renders a separate Home surface. The sidebar Creator Runtime entry uses the same DesignActivity path.

The initial runtime document contains two activities: an unlocked `main` activity and a locked `editor` activity. The editor activity is represented in runtime JSON with `locked=true`, and legacy provisioning creates `editor.xml` with the persisted locked activity option. The original View Manager displays an open or closed shield beside every activity. In Creator Runtime, the existing activity edit window exposes a `Locked` toggle; saving it persists the activity option. Locked activities cannot be selected for deletion, while the lock itself remains editable through the activity edit window.

The starter Continue widget remains part of `main`, with its real Intent component and click event blocks seeded under the activity Java-name key used by the original Components/Events editor. The starter migration remains one-time, so deleting Continue does not recreate it. The same persisted main document is imported after editor autosave and rendered by the native live surface after Back; the runtime canvas uses the persisted widget tree and no separate centered Continue overlay.

The original DesignActivity right drawer is unlocked in Creator Runtime and now contains a `Versions` item. That item opens the existing Sketchware `AboutActivity` changelog/version surface using its native `select=changelog` contract. The lower `btn_options` chevron remains hidden in Creator Runtime mode.

## Verification

Local verification passed with `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:compileDebugAndroidTestJavaWithJavac`. Added/updated coverage checks the two activity defaults, locked editor codec round-trip, legacy editor provisioning, Continue view/component/event projection, launcher DesignActivity selection, Back to native surface, hidden lower options control, and accessible Versions drawer.

The final push was validated by [Creator Runtime Android workflow run 115](https://github.com/FreedoomForm/Sketchware-Pro/actions/runs/32554238525): Build/JVM, Native API 30, and Native API 34 all completed successfully. A local focused connected test could not run because the sandbox had no connected device; the CI emulator matrix supplied the native validation.

## Scope note

This report verifies the changed Creator Runtime paths and their regression coverage. It does not claim a literal line-by-line review of the entire Sketchware Pro repository or complete behavioral coverage of every legacy service implementation.
