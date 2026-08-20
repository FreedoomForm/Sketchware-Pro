# Native Media Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic input and unloaded-resource boundary of the Creator Runtime MediaPlayer/SoundPool service. The service remains R1 runtime-native and uses Android media APIs only through the reviewed service; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing media `id` | `UNSUPPORTED_ARGUMENT` | No media object access |
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No media I/O |
| `load` without source URI | `UNSUPPORTED_ARGUMENT` | No player creation |
| Player operation before load | `UNSUPPORTED_ARGUMENT` | No player operation |
| `sound_create` with maxStreams outside 1–64 | `UNSUPPORTED_ARGUMENT` | No SoundPool creation |
| Sound operation before load | `UNSUPPORTED_ARGUMENT` | No sound playback |
| Stream stop without stream ID | `UNSUPPORTED_ARGUMENT` | No stream operation |
| Valid load/play/sound paths | Typed success or documented media failure | Requires valid URI/resource and device audio lifecycle |

## Test evidence

`CreatorRuntimeNativeWidgetTest.mediaRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorMediaService` and verifies missing IDs/sources, unsupported actions, unloaded player/sound paths, SoundPool bounds, and missing stream IDs before media I/O.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Valid URI/resource resolution, player preparation, playback/pause/seek/completion, SoundPool loading/playback, audio focus, timing, codec support, and device audio behavior remain open integration/device gates. This validation slice claims only the typed input/unloaded-resource boundary.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
