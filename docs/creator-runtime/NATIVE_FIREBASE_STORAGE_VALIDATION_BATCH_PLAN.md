# Native Firebase Storage Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic input-validation boundary of the Creator Runtime Firebase Storage service. The service remains R1 runtime-native and uses Firebase Storage only through the reviewed service; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported or missing `action` | `UNSUPPORTED_ARGUMENT` | No Firebase Storage request |
| `delete_url` without URL | `UNSUPPORTED_ARGUMENT` | No delete request |
| `delete_url` with malformed URL | `UNSUPPORTED_ARGUMENT` | No delete request |
| `download_file` without URL/file path | `UNSUPPORTED_ARGUMENT` | No download request or destination work |
| `upload_uri` without URI | `UNSUPPORTED_ARGUMENT` | No upload request |
| `upload_file` without an existing local file | `UNSUPPORTED_ARGUMENT` | No upload request |
| `download_url` without path | `UNSUPPORTED_ARGUMENT` | No download-URL request |
| Valid upload/download/delete | Typed started result or async error event | Requires Firebase Storage configuration, credentials, network, and file/device state |

## Test evidence

`CreatorRuntimeNativeWidgetTest.firebaseStorageRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFirebaseStorageService` and verifies malformed or incomplete inputs return before asynchronous Firebase Storage operations. The test uses a null environment only on early-return paths that must not publish events or access an Activity.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid Storage bucket configuration, credentials/rules, network, upload/download/delete behavior, progress events, file URI semantics, and device filesystem behavior remain open integration/device gates. This validation slice intentionally does not claim those gates.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
