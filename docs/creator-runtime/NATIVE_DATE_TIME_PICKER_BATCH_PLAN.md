# Native DatePicker/TimePicker Batch Plan

## Scope

This batch closes the typed runtime bridge for native `DatePicker` and `TimePicker` widgets. The implementation remains runtime-native: it uses allow-listed Android widget APIs, typed service arguments, and the existing event dispatcher. It does not generate project Java and does not introduce R2/R3 fallback behavior.

| Capability | Typed action | Acceptance evidence |
|---|---|---|
| Read DatePicker year | `date_picker_year` | Native emulator reads the selected year into runtime state |
| Read DatePicker month | `date_picker_month` | Native emulator reads a 1-based month into runtime state |
| Read DatePicker day | `date_picker_day` | Native emulator reads the selected day into runtime state |
| Set DatePicker date | `date_picker_set_date` | Native emulator updates the real rendered DatePicker through production event path |
| Read TimePicker hour | `time_picker_hour` | Native emulator reads the selected hour into runtime state |
| Read TimePicker minute | `time_picker_minute` | Typed service branch is present; native coverage remains paired with the hour callback path |
| Set TimePicker time | `time_picker_set_time` | Typed service validates bounds and updates the real rendered TimePicker |
| Invalid values | Explicit invalid result | Values outside the Android picker ranges are rejected visibly as runtime errors |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest` renders both widgets from a persisted production `CreatorProjectDocument`. The test changes the actual Android widgets, receives their registered callbacks, dispatches typed widget queries, and asserts the resulting runtime state. This keeps the evidence on the declared Creator Runtime path rather than an injected test-only view hierarchy.

## JVM acceptance test

The legacy importer JVM suite already asserts that `datepickerdialogshow` and `timepickerdialogshow` become typed `date_picker` and `time_picker` service calls. The Android compilation gate additionally verifies the widget service and instrumentation test compile against the configured SDK.

## Fallback policy

Unsupported or invalid picker actions return a visible invalid runtime result. There is no generated Java escape hatch and no R2/R3 fallback branch. Arbitrary `addSourceDirectly` remains an explicit R0 blocked exception outside this batch.
