# Native Text-to-Speech Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action/text boundary of the Creator Runtime Text-to-Speech service. The service remains R1 runtime-native and uses Android TextToSpeech only through the reviewed Activity environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No TTS command |
| `speak` without text | `UNSUPPORTED_ARGUMENT` | No TTS command |
| `is_speaking` | Typed `SUCCEEDED` boolean | Reads local engine state |
| `shutdown` | Typed `SUCCEEDED` | Shuts down engine and clears ready state |
| Valid speak/set pitch/set rate | Typed success, failure, or initializing status | Requires installed/ready TTS engine and language support |

## Test evidence

`CreatorRuntimeNativeWidgetTest.textToSpeechRejectsInvalidInputsOnNativeRuntime` launches the production `CreatorProjectActivity`, creates the production runtime environment and `CreatorTextToSpeechService`, and verifies unsupported action, missing text, local state query, and shutdown paths.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual TTS engine initialization, language availability, speech queueing, pitch/rate behavior, audio output, lifecycle timing, and device/engine parity remain open device/integration gates. This batch claims only typed input and local lifecycle behavior.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
