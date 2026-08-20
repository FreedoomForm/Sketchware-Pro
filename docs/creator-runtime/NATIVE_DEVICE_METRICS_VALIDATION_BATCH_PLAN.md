# Native Device Metrics Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the typed query boundary of the Creator Runtime device-metrics service. The service remains R1 runtime-native and reads display metrics only through the reviewed Activity environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No metric access |
| `display_width` | `SUCCEEDED` with typed numeric value | Reads native display metrics |
| `display_height` | `SUCCEEDED` with typed numeric value | Reads native display metrics |
| `dip` with numeric input | `SUCCEEDED` with typed converted value | Uses native density |
| `dip` with malformed input | Deterministic typed conversion fallback | No external I/O |

## Test evidence

`CreatorRuntimeNativeWidgetTest.deviceMetricsQueriesTypedValuesOnNativeRuntime` launches the production `CreatorProjectActivity`, constructs a production `CreatorRuntimeEnvironment`, and invokes the production `CreatorDeviceMetricsService` for unsupported action and display width/height/dip paths.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Exact density/scaling parity across API 30/API 34, configuration changes, window insets, multi-window behavior, and device display behavior remain open device gates. This batch claims only typed query execution through the production environment.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
