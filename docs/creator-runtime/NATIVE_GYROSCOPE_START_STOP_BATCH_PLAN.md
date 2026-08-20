# Native Gyroscope Start/Stop Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic lifecycle boundary of the Creator Runtime gyroscope service. The service remains R1 runtime-native and uses Android's sensor manager directly through the reviewed runtime environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | No sensor registration |
| `start` on a device with a gyroscope | `SUCCEEDED` with `listening=true` | Sensor listener registered |
| `start` on a device without a gyroscope | `FAILED` | No listener registration |
| `stop` after either start outcome | `SUCCEEDED` with `listening=false` | Listener unregistered when present |

## Test evidence

`CreatorRuntimeNativeWidgetTest.gyroscopeStartStopContractOnNativeRuntime` launches the production `CreatorProjectActivity`, constructs a production `CreatorRuntimeEnvironment`, and invokes the production `CreatorGyroscopeService`. It verifies the unsupported-action result, permits the hardware-dependent start result to be either the documented success or failure status, and verifies the deterministic stop result and output.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Sensor event stream fidelity, event timing, Android permission/device policy, and hardware parity remain open device gates. The lifecycle validation intentionally does not claim continuous sensor behavior on every target device.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
