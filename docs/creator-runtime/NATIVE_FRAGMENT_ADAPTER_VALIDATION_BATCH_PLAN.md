# Native Fragment Adapter Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime Fragment Adapter service. The service remains R1 runtime-native and operates only on a live allow-listed pager widget; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` without `widgetId` | `UNSUPPORTED_ARGUMENT` | No widget lookup or page mutation |
| `page_count` without `widgetId` | `UNSUPPORTED_ARGUMENT` | No pager access |
| `select_page` without `widgetId` | `UNSUPPORTED_ARGUMENT` | No pager access |
| `select_page` with non-pager or out-of-range page | `UNSUPPORTED_ARGUMENT` | No page mutation |
| Valid `page_count`/`select_page` against a live pager | Typed success result | Requires a rendered pager and configured adapter |

## Test evidence

`CreatorRuntimeNativeWidgetTest.fragmentAdapterRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFragmentAdapterService` through the Android test runner. The service receives a null environment only for missing-widget cases and must return before dereferencing the runtime environment.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Live pager rendering, adapter population, page selection animation, fragment lifecycle parity, and device-level UI behavior remain open integration/device gates. This validation slice intentionally claims only the deterministic invalid-input boundary.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
