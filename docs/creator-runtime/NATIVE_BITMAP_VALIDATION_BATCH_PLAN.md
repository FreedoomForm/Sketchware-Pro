# Native Bitmap Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic input and source-resolution boundary of the Creator Runtime Bitmap service. The service remains R1 runtime-native and uses reviewed file/bitmap APIs only; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing action/path | `UNSUPPORTED_ARGUMENT` | No filesystem/bitmap access |
| Empty path | `UNSUPPORTED_ARGUMENT` | No filesystem/bitmap access |
| Missing source file | `FAILED` | No transformation/write |
| Valid transform with source/destination | Typed success or documented failure | Requires real bitmap and filesystem access |
| JPEG EXIF rotation query | Typed rotation value | Requires readable JPEG/EXIF source |
| External-storage path without permission | `PERMISSION_REQUIRED` | Requests declared storage permission |

## Test evidence

`CreatorRuntimeNativeWidgetTest.bitmapRejectsInvalidInputsOnNativeRuntime` launches the production `CreatorProjectActivity`, constructs the production runtime environment, and invokes `CreatorBitmapService` for missing action/path and missing-source cases.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual bitmap decoding/transforms, destination encoding, EXIF behavior, external-storage permission UX, large-image memory behavior, and device filesystem parity remain open integration/device gates. This batch claims only typed input/source validation.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
