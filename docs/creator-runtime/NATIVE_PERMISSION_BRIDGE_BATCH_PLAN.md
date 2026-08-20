# Native Permission Bridge Batch Plan

## Scope

This incremental batch adds native emulator regression coverage for the existing `CreatorRuntimePermissionBridge` state machine. The bridge distinguishes unsupported capabilities, missing host context, request-required state, denial, and explicit grant; it never treats a capability declaration as an automatic permission grant and introduces no R2/R3 fallback.

| Capability | Typed contract | Acceptance evidence |
|---|---|---|
| Unsupported capability | `UNSUPPORTED` | Native test rejects a capability outside the supported set |
| Missing host | `NO_HOST` | Native test confirms no-host state is visible and non-executable |
| First access | `REQUEST_REQUIRED` | Native test confirms an explicit user decision is required |
| Denial | `DENIED` | Native test confirms denial does not persist as a grant |
| Explicit grant | `GRANTED` | Native test confirms only `resolve(..., true)` grants the capability |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest.permissionBridgeRequiresExplicitDecisionOnNativeRuntime` executes the reviewed permission state machine under the Android test runner. This is deterministic and does not trigger a real system permission dialog; hardware/service-specific permission behavior remains governed by the existing `CreatorRuntimeEnvironment` request/result path.

## Fallback policy

No permission is silently granted, and no generated Java or APK fallback is used. Unsupported capability values remain visible typed results. `addSourceDirectly` remains a visible R0 blocked exception.
