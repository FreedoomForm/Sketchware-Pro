# Native Bluetooth Action Validation Batch

## Scope

This batch validates the production `CreatorBluetoothService` action boundary for status, enable request, paired-device query, server/client connection, stop, and send-data actions. Required connection arguments are rejected before Bluetooth adapter and permission access. No generated Java, plugin, R2, or R3 fallback is permitted.

## Acceptance matrix

| Case | Expected typed result | Evidence |
|---|---|---|
| Missing action | `UNSUPPORTED_ARGUMENT` | `bluetoothRejectsInvalidActionsOnNativeRuntime` |
| Unsupported action | `UNSUPPORTED_ARGUMENT` | Native test |
| `ready_connection` without tag | `UNSUPPORTED_ARGUMENT` | Native test |
| `start_connection` without tag/address | `UNSUPPORTED_ARGUMENT` | Native test |
| `stop_connection` without tag | `UNSUPPORTED_ARGUMENT` | Native test |
| `send_data` without tag/data | `UNSUPPORTED_ARGUMENT` | Native test |
| Random UUID | Existing production fixture returns a non-empty UUID | `typedWidgetEventsAndDrawerSurviveNativeRerender` |

## Gates

The local JVM/APK/androidTest Java compilation command is `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon`. API 30/API 34 emulator execution remains a CI/device gate. Adapter availability, Android 12+ Bluetooth permissions, pairing, connection, socket I/O, and OEM behavior remain integration/device gates.

## Architecture disposition

The service is runtime-native R1. Invalid action and argument combinations return typed `UNSUPPORTED_ARGUMENT` before hardware/permission work; no R2/R3 fallback or arbitrary source execution is introduced.

## Evidence rule

The production preflight must remain covered by the native test and the matching audit/CI records. Remote publication is intentionally withheld until complete Sketchware capability coverage is confirmed.
