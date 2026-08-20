# Native Firebase Realtime Database Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic path and local-lifecycle boundary of the Creator Runtime Firebase Realtime Database service. The service remains R1 runtime-native and uses Firebase Database only through the reviewed service; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action with relative path | `UNSUPPORTED_ARGUMENT` | No database request |
| Missing path | `UNSUPPORTED_ARGUMENT` | No database request |
| Absolute path | `UNSUPPORTED_ARGUMENT` | No database request |
| `push_key` with relative path | `SUCCEEDED` with typed key/path | Generates a local Firebase reference key; no write |
| `stop_listen` with relative path | `SUCCEEDED` with `listening=false` | Removes any tracked listener for that path |
| Valid set/get/update/remove/listen | Typed started/success or async error event | Requires Firebase project configuration, rules, network, and data state |

## Test evidence

`CreatorRuntimeNativeWidgetTest.firebaseDatabaseRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFirebaseDatabaseService` and verifies path validation, unsupported action rejection, offline-safe `push_key`, and listener stop lifecycle. The tested early-return/local paths do not require a live database read or write.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid Firebase Database initialization, rules/credentials, network, set/update/remove/get behavior, child listener events, cancellation, and data-shape parity remain open integration/device gates. This validation slice intentionally does not claim those gates.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
