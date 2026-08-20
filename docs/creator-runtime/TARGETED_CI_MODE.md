# Targeted Creator Runtime CI Mode

The repository now has a manual `Creator Runtime Targeted Android` workflow for fast diagnosis. It accepts one API level (`30` or `34`), one fully qualified instrumentation test class, and an optional single test method. The workflow assembles the debug and androidTest APKs once, starts only the selected emulator, and passes Android runner class/method filters to `connectedDebugAndroidTest`.

This mode is intentionally separate from `.github/workflows/creator-runtime-android.yml`. Pushes to `creator-runtime` continue to run the required full APK/JVM plus API 30/API 34 matrix. Targeted runs are used after a failure to shorten the fix loop; the full matrix remains the release-gate evidence before declaring CI green.

Example manual selections:

```text
API 34 + pro.sketchware.creator.CreatorRuntimeNativeWidgetTest#typedWidgetEventsAndDrawerSurviveNativeRerender
API 30 + pro.sketchware.creator.CreatorRuntimeNativeWidgetTest#timerRejectsInvalidInputsOnNativeRuntime
```

The targeted workflow has a 30-minute job timeout and uploads the same connected-test report structure as the full matrix. It does not replace the full matrix and does not weaken the R1/no-fallback acceptance gate.
