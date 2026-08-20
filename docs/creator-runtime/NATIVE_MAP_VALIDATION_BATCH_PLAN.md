# Native Map Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic widget-availability and action boundary of the Creator Runtime Map service. The service remains R1 runtime-native and operates only on registered rendered MapView widgets; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing widget ID or action | `UNSUPPORTED_ARGUMENT` | No map lookup or SDK access |
| Unsupported action on a declared widget ID | `UNSUPPORTED_ARGUMENT` when widget is unavailable | No map mutation |
| Supported action without registered MapView | `UNSUPPORTED_ARGUMENT` | No map mutation |
| Supported action with registered MapView | Typed queued/updated success | Requires MapView and Google Maps lifecycle |
| Marker/camera operation | Typed result or documented map SDK behavior | Requires configured map service/device/API key |

## Test evidence

`CreatorRuntimeNativeWidgetTest.mapRejectsUnavailableWidgetAndInvalidActionOnNativeRuntime` invokes the production `CreatorMapService` and verifies missing widget/action, unsupported action, and unavailable rendered-map paths before Google Maps SDK interaction.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Google Maps API-key/configuration, map readiness, camera/marker behavior, lifecycle, renderer/device parity, and network behavior remain open integration/device gates. This batch claims only typed widget/action validation.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
