# Native Vibrator Validation Batch

## Scope

This checkpoint strengthens the R1 Creator Runtime Vibrator boundary by validating `durationMs` before hardware lookup and adding native Android-runner evidence. Invalid project arguments are therefore deterministic even on devices without vibration hardware; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing duration | Uses default 40 ms, then succeeds or returns hardware-absent `FAILED` | Vibration only when hardware exists |
| Non-numeric `durationMs` | `UNSUPPORTED_ARGUMENT` | No hardware call |
| `durationMs <= 0` | `UNSUPPORTED_ARGUMENT` | No hardware call |
| `durationMs > 10000` | `UNSUPPORTED_ARGUMENT` | No hardware call |
| Valid duration on a vibrating device | `SUCCEEDED` with typed duration | One-shot vibration |
| Valid duration without vibration hardware | `FAILED` | No vibration |

## Test evidence

`CreatorRuntimeNativeWidgetTest.vibratorValidatesDurationBeforeHardwareOnNativeRuntime` invokes the production `CreatorVibratorService` with malformed and out-of-range durations, then allows the documented hardware-dependent status for a valid duration. The production change makes argument validation precede `Vibrator.hasVibrator()` so invalid inputs are not masked by hardware absence.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual vibration effect, amplitude/device policy, haptic timing, Android version differences, and hardware parity remain open device gates. This batch claims only typed argument validation and documented hardware-dependent status behavior.

## Evidence status

- Production service: R1 runtime-native validation order strengthened.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit; javac emits only the existing deprecation notice for Android vibration APIs.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
