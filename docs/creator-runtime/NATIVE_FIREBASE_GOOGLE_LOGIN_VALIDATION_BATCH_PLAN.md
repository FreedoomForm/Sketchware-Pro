# Native Firebase Google Login Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime Google Sign-In service. The implementation remains R1 runtime-native and launches a reviewed sign-in intent only after a configured web client ID is supplied; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | No Google Sign-In client or intent is created |
| `sign_in` without `webClientId` | `UNSUPPORTED_ARGUMENT` | No sign-in intent is launched |
| Valid `sign_in` with configured web client ID | Runtime starts the reviewed Google Sign-In activity result flow | Requires Firebase/Google configuration, account state, and device services |

## Test evidence

`CreatorRuntimeNativeWidgetTest.firebaseGoogleLoginRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFirebaseGoogleLoginService` through the Android test runner. The service receives a null environment only for invalid-input cases and must return before accessing the activity or constructing the Google Sign-In client.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation does not substitute for remote device results.

## Open gates

Valid Google sign-in requires a configured Firebase project, matching OAuth web client ID, SHA configuration, account/device availability, Play services behavior, and network. Those integration and device gates remain open and are not claimed by this validation slice.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
