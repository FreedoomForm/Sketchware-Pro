# Native Location Invalid-Provider Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic validation boundary of the Creator Runtime location service. The test grants the declared location permissions through the instrumentation rule, then exercises the production Activity host and LocationManager service. No generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported `action` with permission available | `UNSUPPORTED_ARGUMENT` | No location request |
| `start` with an invalid provider | `FAILED` | No location listener registration |
| `stop` after the invalid start | `SUCCEEDED` with `listening=false` | Any existing listener is removed |
| Valid provider start | `SUCCEEDED` or documented permission/provider failure | Requires enabled provider and device location support |
| `last_known` | Typed location result or documented failure | Requires provider state and an available location fix |

## Test evidence

`CreatorRuntimeNativeWidgetTest.locationRejectsInvalidProviderOnNativeRuntime` uses `GrantPermissionRule` for the declared fine/coarse location permissions, launches the production `CreatorProjectActivity`, constructs the production `CreatorRuntimeEnvironment`, and invokes `CreatorLocationService`. It verifies unsupported action, invalid provider failure, and deterministic stop behavior without relying on a real GPS fix or network.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Real provider enablement, permission-dialog/result UX, GPS/network fixes, `last_known` semantics, interval/distance behavior, and device location parity remain open device/integration gates. This validation slice claims only the deterministic invalid-provider boundary.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
