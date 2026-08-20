# Native Widget Query Validation Batch

## Scope

This batch validates the production `CreatorWidgetQueryService` against the runtime widget registry. It covers required widget/action arguments, missing widget IDs, unsupported query names, and a successful typed text reporter query. No arbitrary host-view access, generated Java, plugin, R2, or R3 fallback is permitted.

## Acceptance matrix

| Case | Expected typed result | Evidence |
|---|---|---|
| Missing widget ID/action | `UNSUPPORTED_ARGUMENT` | `widgetQueryRejectsInvalidInputsOnNativeRuntime` |
| Unknown widget ID | `UNSUPPORTED_ARGUMENT` | Native test |
| Unsupported query type | `UNSUPPORTED_ARGUMENT` | Native test |
| `get_text` on seeded Button | `SUCCEEDED` with `Increment` | Native test through production Activity fixture |
| Existing reporter queries | Typed values for seek/progress/rating/search/clock/video/calendar/date/time/list | `typedWidgetEventsAndDrawerSurviveNativeRerender` |

## Gates

The local JVM/APK/androidTest Java compilation command is `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon`. API 30/API 34 emulator execution remains a CI/device gate. Widget lifecycle, layout timing, WebView/media behavior, and Android API/OEM parity remain integration/device gates.

## Architecture disposition

The service is runtime-native R1 and restricted to canonical Sketchware reporter/mutator actions. Invalid queries return typed `UNSUPPORTED_ARGUMENT`; no R2/R3 fallback or arbitrary Java execution is introduced.

## Evidence rule

The validation must execute through the production `CreatorProjectActivity`, `CreatorRuntimeEnvironment`, and persisted fixture. Remote publication is intentionally withheld until the complete capability matrix reaches 100%.
