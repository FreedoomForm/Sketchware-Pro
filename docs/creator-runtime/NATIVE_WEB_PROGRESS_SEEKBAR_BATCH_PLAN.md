# Native WebView/ProgressBar/SeekBar Batch Plan

## Scope

This batch closes typed runtime coverage for three already-rendered Android capabilities: WebView URL reporting, ProgressBar indeterminate state, and SeekBar max/progress mutation and reporting. All actions are allow-listed runtime service calls over the registered production widgets; no project Java is generated and no R2/R3 fallback is introduced.

| Capability | Typed action(s) | Acceptance evidence |
|---|---|---|
| WebView URL reporter | `web_url` | Native emulator queries the URL from the rendered WebView into runtime state |
| ProgressBar indeterminate | `progress_set_indeterminate`, `progress_indeterminate` | Native emulator mutates the real ProgressBar and reads the resulting state |
| SeekBar maximum | `seek_set_max`, `seek_max` | Native emulator sets and reads the real SeekBar maximum |
| SeekBar progress | `seek_set_progress`, `seek_progress` | Native emulator sets and reads the real SeekBar progress |
| Legacy import | `seekbarSetMax`, `seekbarSetProgress` | JVM importer test verifies typed widget service calls and arguments |
| Invalid values | Explicit invalid result | SeekBar max/progress bounds are rejected instead of silently falling back |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest` persists a fixture through the `creator_runtime` production store, launches `CreatorProjectActivity`, locates the real rendered WebView, ProgressBar, and SeekBar, and dispatches a typed control event. The test then asserts both native widget state and persisted Creator Runtime state.

## JVM acceptance test

`CreatorLegacyArtifactImporterTest.importsLegacySeekBarSettersAsTypedWidgetServiceCalls` verifies that legacy setter blocks are converted to `seek_set_max` and `seek_set_progress` service calls with typed argument keys. The full JVM suite and Android test Java compilation are required before push.

## Fallback policy

Unsupported or invalid values return visible runtime-invalid results. The batch does not generate Java, execute arbitrary source, or introduce R2/R3 behavior. `addSourceDirectly` remains an explicit R0 blocked exception.
