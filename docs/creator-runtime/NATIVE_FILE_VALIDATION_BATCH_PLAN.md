# Native File Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action, path, and public-directory boundary of the Creator Runtime File service. The service remains R1 runtime-native and restricts legacy paths to reviewed app/private and shared-storage roots; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing action | `UNSUPPORTED_ARGUMENT` | No filesystem access |
| Action without path | `UNSUPPORTED_ARGUMENT` | No filesystem access |
| Unsupported public directory | `UNSUPPORTED_ARGUMENT` | No filesystem access |
| Missing source on `read` | `FAILED` | No write/mutation |
| Valid private read/write/exists/list | Typed success or filesystem error | Uses permitted app roots |
| External path without permission | `PERMISSION_REQUIRED` | Requests declared storage permission |
| Path outside permitted roots | Typed failure | No traversal outside runtime roots |

## Test evidence

`CreatorRuntimeNativeWidgetTest.fileRejectsInvalidInputsOnNativeRuntime` launches the production `CreatorProjectActivity`, constructs the production runtime environment, and invokes `CreatorFileService` for missing action/path, invalid public directory, and missing-source cases.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual private-file read/write/copy/move/delete behavior, path-root edge cases, storage permission UX, scoped-storage differences, directory traversal protections under all encodings, and device filesystem parity remain open integration/device gates. This batch claims only typed input/source validation.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
