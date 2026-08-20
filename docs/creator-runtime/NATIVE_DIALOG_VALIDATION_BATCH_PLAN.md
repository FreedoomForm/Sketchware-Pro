# Native Dialog Validation Batch

## Scope

This checkpoint strengthens the R1 Creator Runtime Dialog boundary by converting missing dialog configuration into typed `UNSUPPORTED_ARGUMENT` results and adds native Android-runner evidence. No generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No dialog/UI access |
| `set_title` without dialog ID or value | `UNSUPPORTED_ARGUMENT` | No state/UI mutation |
| `set_message` without dialog ID or value | `UNSUPPORTED_ARGUMENT` | No state/UI mutation |
| Button setter without dialog ID or label | `UNSUPPORTED_ARGUMENT` | No state/UI mutation |
| Valid setters/show/dismiss | Typed success | Requires Activity UI host |
| Button selection | Typed `button` event | Requires dialog interaction |

## Test evidence

`CreatorRuntimeNativeWidgetTest.dialogRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorDialogService` and verifies unsupported action plus missing `dialogId`, `value`, and `label` paths. The production change prevents a missing dialog ID from escaping as an unchecked exception.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual dialog rendering, progress-dialog behavior, button callbacks, UI-thread lifecycle, theme/locale behavior, and device parity remain open device/integration gates. This batch claims only typed argument validation.

## Evidence status

- Production service: R1 runtime-native validation strengthened.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
