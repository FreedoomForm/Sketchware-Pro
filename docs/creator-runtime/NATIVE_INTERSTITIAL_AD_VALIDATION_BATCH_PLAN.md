# Native Interstitial Ad Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime interstitial-ad service. The implementation remains R1 runtime-native and keeps component identity in typed runtime state; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | No ad SDK work |
| `load` without `adUnitId` | `UNSUPPORTED_ARGUMENT` | No ad load request |
| `show` before a component ad is loaded | `FAILED` | No activity/ad presentation |
| `create` with component ID | `SUCCEEDED` with `created=true` and component ID | Typed component identity only |
| Valid `load`/`show` | Typed success or documented SDK failure | Requires ad SDK initialization, network, policy state, and device display lifecycle |

## Test evidence

`CreatorRuntimeNativeWidgetTest.interstitialAdRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorInterstitialAdService` through the Android test runner. It verifies unsupported action, missing ad-unit validation, show-before-load failure, and component creation without requiring a configured ad SDK or activity for the early-return cases.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid interstitial loading and presentation require ad SDK initialization, valid unit configuration, consent/policy state, network behavior, loaded-ad timing, activity lifecycle, and device display behavior. Those integration and device gates remain open and are not claimed by this validation slice.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
