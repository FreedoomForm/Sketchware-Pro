# Native Notification Permission Batch Plan

## Scope

This incremental batch adds Android-runner coverage for the production notification permission predicate. Android 12 and earlier do not require `POST_NOTIFICATIONS`; Android 13+ requires an explicit granted permission before showing a notification. The predicate is exposed as a reviewed typed runtime contract and does not generate Java or introduce R2/R3 fallback.

| Capability | Typed contract | Acceptance evidence |
|---|---|---|
| Pre-Android 13 | `requiresNotificationPermission(32, denied) == false` | Native runner verifies no notification permission gate before API 33 |
| Android 13+ denied | `requiresNotificationPermission(33, denied) == true` | Native runner verifies explicit permission is required |
| Android 13+ granted | `requiresNotificationPermission(36, granted) == false` | Native runner verifies a granted permission clears the gate |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest.notificationPermissionGateMatchesAndroidSdkOnNativeRuntime` executes the production predicate under the Android test runner. It is deterministic and does not launch a system permission dialog; actual request/result delivery remains covered by `CreatorRuntimeEnvironment` and open device behavior gates.

## Fallback policy

Notification delivery remains runtime-native through `CreatorNotificationService`. A denied permission produces a visible typed permission-required result; it never silently grants access and never routes through generated Java, R2, or R3 execution.
