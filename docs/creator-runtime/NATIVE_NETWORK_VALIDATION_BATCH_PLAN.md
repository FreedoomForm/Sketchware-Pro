# Native Network Validation Batch

## Scope

This batch validates the production `CreatorNetworkService` for the Sketchware RequestNetwork capability. It covers action/component configuration requirements and rejects malformed URLs before any asynchronous HTTP request is enqueued. No generated Java, plugin, R2, or R3 fallback is permitted.

## Acceptance matrix

| Case | Expected typed result | Evidence |
|---|---|---|
| Missing URL/action | `UNSUPPORTED_ARGUMENT` | `networkRejectsInvalidInputsBeforeRequestOnNativeRuntime` |
| `set_params` without component ID | `UNSUPPORTED_ARGUMENT` | Native test |
| `start` without configured URL | `UNSUPPORTED_ARGUMENT` | Native test |
| Non-http(s) URL | `UNSUPPORTED_ARGUMENT` | Native test |
| Valid HTTP request path | Async `SUCCEEDED` start plus published response/error event | Existing production service contract; device/network gate |

## Gates

The local JVM/APK/androidTest Java compilation command is `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon`. API 30/API 34 emulator execution and live network behavior remain CI/device gates. DNS, TLS, server response, timeout, redirect, and callback delivery behavior remain integration gates.

## Architecture disposition

The service is runtime-native R1. Invalid inputs are typed `UNSUPPORTED_ARGUMENT` before network access; no R2/R3 fallback or arbitrary source execution is introduced.

## Evidence rule

The test must remain a production-service test, not a mock-only shortcut for validation ordering. Remote publication is intentionally withheld until the complete capability matrix reaches 100%.
