# Native Date Picker Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action and numeric-input boundary of the Creator Runtime DatePickerDialog service. The service remains R1 runtime-native and creates dialogs only through the reviewed Activity environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No dialog or Activity access |
| Non-numeric year/month/day | `UNSUPPORTED_ARGUMENT` | No dialog creation |
| `show` with valid/default numbers | Typed success with `shown=true` | Dialog is shown on the Activity UI thread |
| User selection | Typed `selected` event with year/month/day | Requires dialog UI interaction |

## Test evidence

`CreatorRuntimeNativeWidgetTest.datePickerRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorDatePickerService` and verifies unsupported action and malformed year rejection before dialog creation or Activity access.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual dialog rendering, date bounds, UI-thread behavior, user selection event, locale/theme behavior, and device parity remain open device/integration gates. This batch claims only the typed action/numeric boundary.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
