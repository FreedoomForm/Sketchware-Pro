# Native Firebase Auth Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation and local-state boundary of the Creator Runtime Firebase Authentication service. The service remains R1 runtime-native and uses FirebaseAuth only through the reviewed service; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` | `UNSUPPORTED_ARGUMENT` | No Firebase network request |
| `sign_in` without email or password | `UNSUPPORTED_ARGUMENT` | No sign-in request |
| `register` without email or password | `UNSUPPORTED_ARGUMENT` | No registration request |
| `reset_password` without email | `UNSUPPORTED_ARGUMENT` | No reset request |
| `status` | `SUCCEEDED` with typed signed-in/UID/email output | Reads local FirebaseAuth state |
| `sign_out` | `SUCCEEDED` with `signedOut=true` | Clears FirebaseAuth local session |
| Valid sign-in/register/reset/anonymous | Typed started result | Requires Firebase project, credentials, network, and configuration |

## Test evidence

`CreatorRuntimeNativeWidgetTest.firebaseAuthRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFirebaseAuthService` and verifies unsupported/incomplete arguments plus deterministic `status` and `sign_out` paths. Invalid cases return before Firebase network operations; the status/sign-out cases exercise local FirebaseAuth state behavior.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid email/password, anonymous, reset, Firebase project initialization, credentials, network, quota, auth persistence, and real account behavior remain open integration/device gates. This validation slice intentionally does not claim those gates.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
