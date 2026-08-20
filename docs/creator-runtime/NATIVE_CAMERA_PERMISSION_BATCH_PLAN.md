# Native Camera Permission Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action and permission boundary of the Creator Runtime Camera service. The service remains R1 runtime-native and launches Android capture only through the reviewed environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No permission request or capture intent |
| `capture` without camera permission | `PERMISSION_REQUIRED` | Runtime requests the declared camera permission |
| `capture` with permission and camera activity | `SUCCEEDED` with started result flow | Capture intent launched |
| `capture` with permission but no camera activity | `FAILED` | No result flow can be launched |

## Test evidence

`CreatorRuntimeNativeWidgetTest.cameraRejectsUnsupportedActionOnNativeRuntime` invokes the production `CreatorCameraService` and verifies unsupported-action rejection plus the documented permission-gate status for capture. The test intentionally does not complete a real camera capture or rely on a camera application.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual camera permission dialog/result behavior, capture intent resolution, returned media data, camera hardware, orientation, and device parity remain open device gates. This batch claims only action validation and permission-gate status.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
