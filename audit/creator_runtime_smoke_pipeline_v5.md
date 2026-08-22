# Creator Runtime smoke pipeline v5

## Purpose

The previous push workflow executed the entire Android-test suite on both API 30 and API 34. It could spend tens of minutes in emulator setup/test execution and still provide weak evidence for the specific user failure because many tests checked isolated records or non-null views rather than one complete editor-to-runtime round-trip.

## Push gate

Every push to `creator-runtime` runs exactly one test class and one test method on both API 30 and API 34:

`pro.sketchware.creator.CreatorRuntimeSmokeTest#editorRoundTripPreservesUserButtonAndRunsItsClickBehavior`

The smoke flow checks the installed launcher component, original `DesignActivity`, ProjectLoader completion, starter Continue in the visible legacy `main.xml` store, runtime editor controls, a legacy ViewBean button mutation, an original Sketchware `onClick` `setText` block, the Back lifecycle path, native live-canvas rendering, click behavior after projection, and persistence after reopening `DesignActivity`.

The push test command has a 900-second Gradle test timeout per emulator job. Emulator boot timeout is 300 seconds. Logcat and connected-test reports are uploaded on success or failure.

## Manual diagnostics

A manually dispatched workflow with `test-class` and optional `test-method` runs the requested focused class/method. A manually dispatched workflow with both filters empty runs the broad regression suite with the longer 2700-second test timeout. This keeps broad coverage available without making every push wait for unrelated AI, widget-catalog, and compatibility tests.

## Evidence limits

The smoke test is stronger than the previous count-only and isolated assertions, but it still runs on hosted emulators rather than the user's Redmi M2101K7BG. A successful emulator run proves the tested code path works in that emulator configuration; it does not prove OEM-specific behavior, installed-data migration, permission state, or a physical-device screen layout. Those remain explicitly open until device evidence is available.
