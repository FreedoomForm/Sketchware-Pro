# Native Calendar and Timer Runtime Batch Plan

## Scope

The next runtime batch covers two deterministic legacy capabilities that already have typed JVM contracts but no device-level evidence: the native `CalendarView` bridge and the runtime-native timer scheduler. The batch must use the production `CreatorProjectActivity`, `CreatorRuntimeServices`, `CreatorRuntimeSession`, and typed `RUNTIME_SERVICE_CALL` blocks. It must not use generated Java, arbitrary source execution, network access, or hidden fallback behavior.

## Acceptance matrix

| Capability | Typed contract | Native emulator assertion |
|---|---|---|
| CalendarView date query and update | `widget` service actions `calendar_date` and `calendar_set_date` operate on the registered native `CalendarView` | A seeded `calendar` widget is rendered in the real activity; a typed service event sets a deterministic date and a follow-up typed query stores the native date in runtime state |
| Calendar component service | `calendar` service actions `set_time`, `add`, and `format` preserve component-scoped typed state | A typed event invokes `calendar` with a fixed timestamp and format, and the resulting formatted value is persisted in the production runtime document |
| Timer one-shot lifecycle | `timer` service action `after` schedules a typed tick, dispatching the timer target event through `CreatorProjectActivity` | A seeded timer event schedules a short one-shot timer; the native activity receives the service event and a typed `INCREMENT_STATE` block changes runtime state without generated code |
| Timer cancellation | `timer` service action `cancel` removes the scheduled task and reports typed cancellation output | A second typed action cancels the timer before it can fire; the test verifies no post-cancel state increment |

## Fixture and isolation

The instrumentation fixture clears the `creator_runtime` SharedPreferences document, writes a complete typed document through `CreatorProjectDocumentCodec`, and launches `CreatorProjectActivity` normally. It uses fixed UTC timestamps and short delays bounded by a polling timeout. The test removes or cancels every scheduled timer and closes the activity in teardown so emulator jobs remain isolated.

The calendar assertions inspect both the rendered native `CalendarView` and the persisted typed runtime state. Timer assertions observe the production `CreatorRuntimeSession` state after the activity receives the timer service event. No private activity fields or test-only injection points are allowed.

## Evidence gate

The batch is verified only after local production/JVM/androidTest compilation, a GitHub Actions run with successful APK/JVM tests, and successful API 30 and API 34 native jobs. A failure caused by emulator infrastructure may be retried, but an assertion or lifecycle failure must result in a code/test fix before the evidence register is updated.
