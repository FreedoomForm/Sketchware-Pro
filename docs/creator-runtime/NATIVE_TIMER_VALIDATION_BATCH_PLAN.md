# Native Timer Validation Batch

## Scope

This batch validates the production `CreatorTimerService` through the typed Creator Runtime contract. It covers required timer identifiers, action selection, non-negative delays, `after` versus `every` period semantics, and cancellation. No generated Java, plugin, R2, or R3 fallback is permitted.

## Acceptance matrix

| Case | Expected typed result | Evidence |
|---|---|---|
| Missing timer ID | `UNSUPPORTED_ARGUMENT` | `timerRejectsInvalidInputsOnNativeRuntime` |
| Missing delay for schedule/after | `UNSUPPORTED_ARGUMENT` | Native test |
| Non-numeric delay | `UNSUPPORTED_ARGUMENT` | Native test |
| `after` with period | `UNSUPPORTED_ARGUMENT` | Native test |
| `every` without positive period | `UNSUPPORTED_ARGUMENT` | Native test |
| Cancel unknown timer | `SUCCEEDED` with `cancelled=false` | Native test |
| Valid scheduling lifecycle | Existing production fixture schedules `timer1` and observes `timer-tick` | `typedWidgetEventsAndDrawerSurviveNativeRerender` |

## Gates

The local JVM/APK/androidTest Java compilation command is `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon`. API 30/API 34 emulator execution remains a CI/device gate. Scheduler timing, process death, background execution limits, and device timing variance remain integration gates; they do not change the typed R1 contract.

## Architecture disposition

The service is runtime-native R1. Invalid inputs are reported as typed `UNSUPPORTED_ARGUMENT`; no R2/R3 fallback or arbitrary source execution is introduced.

## Evidence rule

This plan is not complete until the native test, audit register, and CI evidence register all reference the same production method and test method. Remote publication is intentionally withheld until the complete Sketchware capability matrix reaches 100%.
