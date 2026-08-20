# Native Drawer Validation Batch

## Scope

This checkpoint adds native Android-runner evidence for the deterministic action and DrawerLayout-registration boundary of the Creator Runtime Drawer service. The service remains R1 runtime-native and operates only on a registered rendered DrawerLayout; no generated Java, R2/R3 fallback, or direct project-code execution is introduced.

## Acceptance contract

| Case | Expected result | External side effect |
|---|---|---|
| Missing action | `UNSUPPORTED_ARGUMENT` | No DrawerLayout access |
| Unsupported action | `UNSUPPORTED_ARGUMENT` | No DrawerLayout access |
| Supported action before registration | `UNSUPPORTED_ARGUMENT` | No UI mutation |
| `open`/`close`/`is_open` after registration | Typed success | Requires live DrawerLayout host |

## Test evidence

`CreatorRuntimeNativeWidgetTest.drawerRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorDrawerService` and verifies missing action, unsupported action, and supported-action-before-registration paths without UI interaction.

The local pre-push command is:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The push-triggered workflow must execute the debug APK/JVM job and native emulator jobs on API 30 and API 34. Local compilation is not a substitute for remote device results.

## Open gates

Actual DrawerLayout registration, open/close state, gravity, animation, lifecycle, accessibility, and device UI parity remain open device/integration gates. This batch claims only typed action/registration validation.

## Evidence status

- Production service: existing R1 runtime-native implementation.
- Native validation: added in `CreatorRuntimeNativeWidgetTest`.
- Local JVM/APK/androidTest compilation: passed before checkpoint commit.
- Remote API 30/API 34 execution: pending until the workflow for the pushed commit completes.
