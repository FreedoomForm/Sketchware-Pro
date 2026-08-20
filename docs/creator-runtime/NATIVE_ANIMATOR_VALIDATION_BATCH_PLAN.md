# Native Animator Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic configuration boundary of the Creator Runtime ObjectAnimator service. The service remains R1 runtime-native and animates only reviewed rendered widgets; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action or missing component ID | `UNSUPPORTED_ARGUMENT` | No animation state mutation |
| `set_target` without widget ID | `UNSUPPORTED_ARGUMENT` | No widget lookup |
| `set_duration` outside 0–60000 ms | `UNSUPPORTED_ARGUMENT` | No animator creation |
| `set_repeat_count` outside -1–1000 | `UNSUPPORTED_ARGUMENT` | No animator creation |
| `is_running` for configured component | Typed `SUCCEEDED` result | Reads runtime animator state |
| `cancel` with no active animator | Typed `SUCCEEDED` result | No host interaction required |
| Valid start against rendered widget | Typed success | Requires live widget and Activity UI thread |

## Test evidence

`CreatorRuntimeNativeWidgetTest.animatorRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorAnimatorService` and verifies component/target validation, duration/repeat bounds, and offline-safe configured status/cancel paths before a rendered widget is required.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual rendered-widget property animation, timing, interpolation, repeat behavior, cancellation, UI-thread lifecycle, and device visual parity remain open device/integration gates. This batch claims only typed configuration/state behavior.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
