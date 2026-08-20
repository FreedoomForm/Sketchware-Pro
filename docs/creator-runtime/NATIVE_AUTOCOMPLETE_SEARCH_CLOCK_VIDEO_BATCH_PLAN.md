# Native AutoComplete/SearchView/TextClock/VideoView Batch Plan

## Scope

This batch closes typed runtime coverage for four already-rendered Android capabilities: AutoCompleteTextView data and threshold, AppCompat SearchView query and hint, TextClock format setters, and VideoView URL/playback controls. All actions are allow-listed runtime service calls over registered production widgets; no project Java is generated and no R2/R3 fallback is introduced.

| Capability | Typed action(s) | Acceptance evidence |
|---|---|---|
| AutoComplete data | `autocomplete_set_data` | Native emulator replaces the real adapter from a runtime state list and verifies item count |
| AutoComplete threshold | `autocomplete_threshold` | Native emulator sets and queries the real threshold |
| SearchView query | `search_set_query`, `search_query` | Native emulator mutates the real SearchView query and stores the typed query |
| SearchView hint | `search_set_hint` | Native emulator verifies the real query hint after mutation |
| TextClock formats | `clock_format_12h`, `clock_format_24h`, `clock_get_format_12h`, `clock_get_format_24h` | Native emulator verifies both real TextClock format values and runtime state |
| VideoView URL/control | `video_set_url`, `video_start`, `video_pause`, `video_stop`, `video_is_playing` | Native emulator dispatches URL and lifecycle controls against the real VideoView and verifies the final non-playing state |
| Legacy import | `autoComSetData`, `setThreshold`, SearchView setters, TextClock format setters, VideoView controls | JVM importer regression verifies typed widget service calls and typed argument keys |
| RatingBar legacy bridge | `getRating`, `setRating`, `setNumStars`, `setStepSize` | JVM importer regression verifies typed query/setter service calls; reporter execution remains live-query based |
| Invalid values | Explicit invalid result | AutoComplete threshold and VideoView URL validation reject invalid values instead of silently falling back |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest` persists a fixture through the `creator_runtime` production store, launches `CreatorProjectActivity`, locates the real rendered AutoCompleteTextView, AppCompat SearchView, TextClock, and VideoView, and dispatches a typed control event. The test asserts both native widget state and persisted Creator Runtime state.

## JVM acceptance test

`CreatorLegacyArtifactImporterTest.importsLegacyRatingAndNextWidgetActionsAsTypedRuntimeCalls` verifies that RatingBar reporter/setter blocks and the new widget setter blocks are converted to `widget` runtime service calls with typed actions and arguments. The full JVM suite and Android test Java compilation are required before push.

## Fallback policy

Unsupported or invalid values return visible runtime-invalid results. The batch does not generate Java, execute arbitrary source, or introduce R2/R3 behavior. `addSourceDirectly` remains an explicit R0 blocked exception.
