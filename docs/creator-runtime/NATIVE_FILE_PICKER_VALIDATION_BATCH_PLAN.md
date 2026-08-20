# Native File Picker Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action boundary of the Creator Runtime File Picker service. The service remains R1 runtime-native and launches Android's persisted document-provider contract only through the reviewed environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No picker intent or Activity interaction |
| `pick` without MIME type | Typed success with `*/*` | Launches open-document intent when an Activity host is available |
| `pick` with MIME type | Typed success with requested MIME | Launches filtered open-document intent |
| Result returned by provider | Typed `selected` event with persisted URI handling | Requires document provider and user selection |

## Test evidence

`CreatorRuntimeNativeWidgetTest.filePickerRejectsUnsupportedActionOnNativeRuntime` invokes the production `CreatorFilePickerService` and verifies unsupported-action rejection before intent construction or Activity access. The test does not claim a user-driven document selection.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual picker UI, MIME filtering, document-provider availability, persisted URI permissions, returned-data handling, and device UX parity remain open device/integration gates. This batch claims only the typed action boundary.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
