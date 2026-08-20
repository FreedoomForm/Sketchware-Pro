# Native Notification Action Validation Batch

## Scope

This checkpoint complements the existing Android SDK notification-permission gate with native Android-runner evidence for the deterministic action boundary of the Creator Runtime Notification service. The service remains R1 runtime-native and uses NotificationManager only through the reviewed environment; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No permission or NotificationManager access |
| `show` on API 33+ without permission | `PERMISSION_REQUIRED` | No notification posted |
| `cancel` with available manager | Typed success | Notification ID canceled |
| Valid `show` with permission | Typed success | Channel/notification posted through NotificationManager |

## Test evidence

`CreatorRuntimeNativeWidgetTest.notificationRejectsUnsupportedActionOnNativeRuntime` invokes the production `CreatorNotificationService` and verifies unsupported-action rejection before environment access. The existing `notificationPermissionGateMatchesAndroidSdkOnNativeRuntime` test remains the SDK predicate evidence for API 32/33/36 contract cases.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual system permission request/result UX, channel creation, notification posting/cancel behavior, OEM policy, API-level differences, and device notification behavior remain open device gates. This batch claims only typed action validation plus the existing permission predicate evidence.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
