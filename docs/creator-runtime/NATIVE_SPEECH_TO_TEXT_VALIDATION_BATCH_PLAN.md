# Native Speech-to-Text Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action and local lifecycle boundary of the Creator Runtime Speech-to-Text service. The service remains R1 runtime-native and uses microphone/recognizer APIs only through the reviewed Activity environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No microphone/recognizer access |
| `stop` before listening | `SUCCEEDED` with stopped state | No recognizer operation |
| `shutdown` before listening | `SUCCEEDED` with shutdown state | No recognizer operation |
| `listen` without microphone permission | `PERMISSION_REQUIRED` | Requests declared audio permission |
| `listen` without recognizer service | `FAILED` | No listening session |
| Valid listen/recognition callbacks | Typed started/events | Requires recognizer, microphone, permission, and device service |

## Test evidence

`CreatorRuntimeNativeWidgetTest.speechToTextRejectsInvalidActionAndSupportsLifecycleOnNativeRuntime` invokes the production `CreatorSpeechToTextService` for unsupported action and stop/shutdown paths without accessing a live recognizer.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual microphone permission UX, recognizer availability, listening/result/partial/error callbacks, language behavior, audio routing, lifecycle timing, and device/engine parity remain open device/integration gates. This batch claims only typed action and local lifecycle behavior.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
