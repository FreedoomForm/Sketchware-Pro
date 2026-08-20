# Native Local Storage Validation Batch

## Scope

This batch validates the production `CreatorStorageService` as the runtime-native replacement for Sketchware SharedPreferences storage. It covers store configuration, required key/action validation, and typed get/set/remove paths. No generated Java, plugin, R2, or R3 fallback is permitted.

## Acceptance matrix

| Case | Expected typed result | Evidence |
|---|---|---|
| Missing action/key | `UNSUPPORTED_ARGUMENT` | `storageRejectsInvalidInputsAndSupportsTypedPathsOnNativeRuntime` |
| Incomplete configure request | `UNSUPPORTED_ARGUMENT` | Native test |
| Unsupported action | `UNSUPPORTED_ARGUMENT` | Native test |
| Configure component store | `SUCCEEDED` with component/store output | Native test |
| Set and get string value | `SUCCEEDED`, exact typed value | Native test |
| Remove value | `SUCCEEDED` | Native test |

## Gates

The local JVM/APK/androidTest Java compilation command is `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon`. API 30/API 34 emulator execution remains a CI/device gate. SharedPreferences persistence, process restart behavior, store-name sanitization, and OEM storage behavior remain integration gates.

## Architecture disposition

The service is runtime-native R1. Invalid storage requests return typed `UNSUPPORTED_ARGUMENT`; no R2/R3 fallback or arbitrary source execution is introduced.

## Evidence rule

The acceptance record is valid only when the native test, final audit register, and CI evidence register identify the same production service and test method. Remote publication is intentionally withheld until full Sketchware capability coverage is confirmed.
