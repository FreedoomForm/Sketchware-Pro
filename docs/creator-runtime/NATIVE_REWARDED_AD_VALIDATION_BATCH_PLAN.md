# Native Rewarded Ad Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime rewarded-ad service. The implementation remains R1 runtime-native and does not introduce generated Java, R2/R3 fallback, or direct project-code execution.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | No ad SDK work |
| `load` without `adUnitId` | `UNSUPPORTED_ARGUMENT` | No ad load request |
| `show` before a rewarded ad is loaded | `FAILED` | No activity/ad presentation |
| Valid `load` with configured unit ID | `SUCCEEDED` with `started=true` | Requires ad SDK initialization, network, and valid unit configuration |
| Valid `show` after load | `SUCCEEDED` with `started=true` | Requires loaded ad, activity, and device display lifecycle |

## Test evidence

`CreatorRuntimeNativeWidgetTest.rewardedAdRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorRewardedAdService` through the Android test runner. The test uses a null environment only for paths that must return before activity access and verifies the typed `UNSUPPORTED_ARGUMENT` and `FAILED` statuses.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid rewarded-ad loading and presentation require ad SDK initialization, valid ad unit configuration, consent/policy state, network behavior, loaded-ad timing, and device display lifecycle. Those integration and device gates remain open and are not claimed by this validation slice.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
