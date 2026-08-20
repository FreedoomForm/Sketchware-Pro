# Native Firebase Cloud Message Validation Batch Plan

## Scope

This incremental batch adds deterministic native-runner validation for the Firebase Cloud Message runtime service's invalid-input contract. It intentionally does not call Firebase network APIs: valid token/topic delivery remains an environment-dependent component gate, while malformed actions and missing topics must be rejected locally as typed runtime results.

| Capability | Typed contract | Acceptance evidence |
|---|---|---|
| Unsupported action | `UNSUPPORTED_ARGUMENT` | Native runner verifies an unknown action is rejected before Firebase access |
| Missing subscribe topic | `UNSUPPORTED_ARGUMENT` | Native runner verifies `subscribe` without a topic is rejected |
| Missing unsubscribe topic | `UNSUPPORTED_ARGUMENT` | Native runner verifies `unsubscribe` without a topic is rejected |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest.firebaseCloudMessageRejectsInvalidInputsOnNativeRuntime` invokes the production `CreatorFirebaseCloudMessageService` under the Android test runner with invalid inputs and a null environment. The tested paths return before any Firebase SDK call, making the regression deterministic and network-independent.

## Open component gate

Valid FCM token retrieval and topic subscription/unsubscription remain open device/configuration evidence because they require Firebase initialization, network access, and project credentials. This batch does not claim those paths complete and does not introduce generated Java, R2, or R3 fallback.
