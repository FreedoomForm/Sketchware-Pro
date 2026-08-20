# Native Firebase Auth Phone Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime Firebase Phone Authentication service. The service remains runtime-native (R1); no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | Network/Firebase SDK side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | None expected |
| `send_code` without `phoneNumber` | `UNSUPPORTED_ARGUMENT` | None expected |
| `confirm_code` with `code` but no `verificationId` | `UNSUPPORTED_ARGUMENT` | None expected |
| `confirm_code` with `verificationId` but no `code` | `UNSUPPORTED_ARGUMENT` | None expected |
| Valid `send_code` with E.164 phone number | Runtime starts Firebase verification | Requires Firebase project configuration, reCAPTCHA/SMS and network |
| Valid `confirm_code` with verification ID and code | Runtime starts credential sign-in | Requires Firebase verification state and network |

## Test evidence

`CreatorRuntimeNativeWidgetTest.firebaseAuthPhoneRejectsInvalidInputsOnNativeRuntime` executes the first four rows through the production `CreatorFirebaseAuthPhoneService` class on the Android test runner. The test passes a null environment only for invalid-input cases; the service must return before dereferencing the activity or invoking Firebase verification.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The remote push-triggered workflow must still execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. A successful local compilation is not treated as a substitute for those remote device results.

## Open gates

Valid phone-number verification, SMS delivery, reCAPTCHA behavior, Firebase project initialization, credential configuration, quota behavior, and network/device behavior remain open integration gates. This validation slice intentionally does not claim those gates are closed. No R0 exception was added, and no R2/R3 fallback was introduced.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
