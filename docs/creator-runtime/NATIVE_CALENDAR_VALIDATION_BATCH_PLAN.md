# Native Calendar Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic component-scoped state and field/format boundary of the Creator Runtime Calendar service. The service remains R1 runtime-native and uses Java Calendar only through the reviewed runtime service; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No external I/O |
| `add`/`set` without or with unsupported field | `UNSUPPORTED_ARGUMENT` | Calendar state unchanged by invalid operation |
| Malformed format pattern | `UNSUPPORTED_ARGUMENT` | No formatted result |
| Diff with unknown component | `UNSUPPORTED_ARGUMENT` | No cross-component state access |
| `set_time`/`get_time` | Typed success with component-scoped timestamp fields | In-memory runtime state only |
| Valid now/reset/add/set/format/diff | Typed success | In-memory runtime state only |

## Test evidence

`CreatorRuntimeNativeWidgetTest.calendarRejectsInvalidInputsAndReturnsTypedStateOnNativeRuntime` invokes the production `CreatorCalendarService` and verifies unsupported action/field, malformed format, unknown diff component, and typed `set_time`/`get_time` paths.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Timezone/locale behavior, calendar leniency, timestamp precision, component persistence across Activity/process lifecycle, and device/API parity remain open behavior gates. This batch claims only typed in-memory state and validation behavior.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
