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

## Confirmed first-run failure and correction

The first focused workflow run failed on both API 30 and API 34 before Gradle tests started. The emulator runner executed the multiline `script` through `/bin/sh`; `set -o pipefail` is not supported by that shell and exited with code 2. A second run exposed that the runner invokes multiline entries as separate `/bin/sh -c` commands, so a multiline `if` lost its closing context and failed with `Syntax error: end of file unexpected (expecting "fi")`. The workflow now uses one single-line `bash -lc` command, making the control flow explicit and portable for this action. Both failures were test-pipeline failures, not evidence that the Creator Runtime smoke flow itself failed.

## Evidence limits

The smoke test is stronger than the previous count-only and isolated assertions, but it still runs on hosted emulators rather than the user's Redmi M2101K7BG. A successful emulator run proves the tested code path works in that emulator configuration; it does not prove OEM-specific behavior, installed-data migration, permission state, or a physical-device screen layout. Those remain explicitly open until device evidence is available.

## Verified focused run 138

Run `32594746584` for commit `45c6179ef60f9aaf9600555fb69c5c9061105d04` completed successfully on 2026-08-22. The Build/JVM prerequisite completed successfully. API 30 job `97084583306` ran `editorRoundTripPreservesUserButtonAndRunsItsClickBehavior`, logged `Starting 1 tests`, `Finished 1 tests`, and `BUILD SUCCESSFUL in 6m 3s`. API 34 job `97084583278` ran the same single method, logged `Starting 1 tests`, `Finished 1 tests`, and `BUILD SUCCESSFUL in 7m 41s`. The workflow conclusion was `success`; the hosted-emulator housekeeping lines `Unable to connect to adb daemon on port: 5037` and `stop: Not implemented` were outside the test result and did not fail either job.

This run validates the focused editor-to-live-to-reopen smoke path on API 30 and API 34 emulators, including transient live click execution without overwriting the authored editor snapshot. It is not Redmi M2101K7BG/OEM validation and does not establish full Sketchware feature coverage.
