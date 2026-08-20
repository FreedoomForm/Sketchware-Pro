# Native UI Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action and input boundary of the Creator Runtime UI service. The service remains R1 runtime-native and uses Activity title and ClipboardManager only through the reviewed environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No Activity/clipboard access |
| `set_title` without title | `UNSUPPORTED_ARGUMENT` | No title mutation |
| `copy_text` without text | `UNSUPPORTED_ARGUMENT` | No clipboard mutation |
| Valid `set_title` | Typed success | Activity title is updated |
| Valid `copy_text` | Typed success or clipboard failure | Clipboard primary clip is updated when available |

## Test evidence

`CreatorRuntimeNativeWidgetTest.uiRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorUiService` and verifies unsupported action plus missing title/text paths before Activity or clipboard access.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual Activity title behavior, clipboard availability, primary-clip semantics, privacy/OEM policy, lifecycle, and device parity remain open device/integration gates. This batch claims only typed action/input validation.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
