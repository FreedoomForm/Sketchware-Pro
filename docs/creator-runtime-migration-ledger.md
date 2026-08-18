# Creator Runtime Migration Ledger

## Snapshot

This ledger tracks the migration of Sketchware Pro behavior from the legacy Java generator into the typed Creator Runtime. The authoritative legacy executable surface is the opcode switch in `app/src/main/java/a/a/a/Fx.java`.

| Area | Current inventory | Current runtime status |
|---|---:|---|
| Legacy component types | 30 | All 30 have an explicit component-to-service mapping; implementation depth still varies by service |
| Legacy view/widget capability entries | 78 expected by the current matrix | Classified as runtime-native in the matrix; importer and widget behavior need exhaustive behavioral verification |
| Legacy executable opcodes in `Fx.java` | 326 unique cases | Only a small typed subset is currently imported/executed by `CreatorLegacyArtifactImporter` and `CreatorRuntimeExecutor` |
| Runtime block types | 6 | `SET_WIDGET_PROPERTY`, `SET_STATE`, `SHOW_MESSAGE`, `NAVIGATE`, `RUNTIME_SERVICE_CALL`, `IF_STATE_EQUALS` |
| Runtime service IDs | 28 unique IDs in the component matrix | Registered in `CreatorRuntimeServices`; argument and callback parity is incomplete |

## Migration rules

Legacy Java generation must not remain an execution fallback. Each migrated opcode must either become a typed runtime operation/block with behavior-focused tests or remain explicitly reported as unsupported. `addSourceDirectly` and arbitrary custom Java/native libraries are intentionally not executable in the runtime because they would reintroduce code execution outside the typed model.

## Priority backlog

### 1. Typed data and expression runtime

Migrate variable reads/writes, numeric and boolean expressions, string operations, list operations, map operations, conversion operations, and math operations into a typed value/evaluation layer. This is the foundation required by control flow and service arguments.

Representative opcodes include `getVar`, `setVarBoolean`, `setVarInt`, `setVarString`, `increaseInt`, `decreaseInt`, `mapCreateNew`, `mapPut`, `mapGet`, `mapRemoveKey`, `mapSize`, `addListInt`, `insertListStr`, `getAtListMap`, `deleteList`, `+`, `-`, `*`, `/`, `%`, `=`, `&&`, `||`, `random`, `stringLength`, `stringJoin`, `stringSub`, `toNumber`, `toString`, `strToMap`, and the `math*` family.

### 2. Control-flow parity

Extend the typed graph beyond `if_state_equals` to support the legacy `if`, `ifElse`, `repeat`, `forever`, `break`, and safe bounded execution. The runtime must validate cycles, nesting depth, and execution budgets before running imported graphs.

### 3. Exhaustive widget operations

Replace the current small property subset with typed widget properties and effects for enabled state, text, colors, images, visibility, focus, checked state, alpha, rotation, translation, scale, list/spinner operations, WebView operations, calendar-view operations, map operations, adapters, and event listeners.

### 4. Service argument and callback parity

Map legacy service opcodes to structured service calls rather than empty argument maps. Cover intent, local storage/file APIs, calendar, vibrator, timer cancellation, Firebase database/auth/storage/callbacks, media player/sound pool, camera/file picker, networking, speech, Bluetooth, location, ads, dialogs, notifications, and fragment adapters.

### 5. Lifecycle and callback events

Import activity lifecycle events such as `initializeLogic`, activity result callbacks, component callbacks, drawer events, authentication callbacks, camera/file-picker results, and service listener callbacks. Preserve event order and callback payloads in the runtime event log.

### 6. Resource, project, and custom-block behavior

Continue live resource resolution and metadata import. Import custom block definitions as typed templates only when their bodies can be represented without arbitrary Java. Keep unsupported custom/native code visible in the compatibility report.

## Immediate implementation slice

The next implementation slice is the typed data/expression foundation plus control-flow parity. It will introduce a runtime value model and typed evaluation operations, then add `if`, `ifElse`, `repeat`, `forever`, and `break` with validation and execution-budget tests. Subsequent slices will consume this foundation for widget and service argument migration.

## Completed in the current development session

The runtime now imports legacy `if` and `ifElse` blocks as typed conditions, imports `repeat`, `forever`, and `break`, evaluates state-backed comparisons without source execution, and enforces a global execution-step budget plus a repeat-count limit. Behavior-focused tests cover importer conversion, repeated effects, typed state conditions, and terminating `forever` loops.

The Android unit-test task was attempted but is currently blocked before compilation because the sandbox has no Android SDK and no `ANDROID_HOME` or `local.properties` SDK path.

## Verification gates

Every slice must pass `git diff --check`, focused unit tests, the compatibility matrix tests, importer tests, executor tests, and a repository-wide stale-fallback search. Full Android compilation remains blocked until an Android SDK is available in the environment; this limitation must be reported separately from source-level verification.

## Current gap map

The current importer recognizes 20 opcode aliases, while the legacy generator exposes 326 unique opcode cases. The largest uncovered groups are:

| Family | Examples | Migration implication |
|---|---|---|
| Typed values and expressions | `getVar`, `setVarBoolean`, `map*`, `list*`, `string*`, `math*`, `random`, conversions | Requires a runtime value/evaluation model before service arguments can be migrated faithfully |
| Widget effects and reads | `setEnable`, `setHint`, `setVisible`, `setAlpha`, `getText`, list/spinner/WebView/calendar/map operations | Requires a typed widget-effect contract and readback values |
| Intent and file APIs | `intentSetAction`, `intentPutExtra`, `startActivity`, `fileGetData`, `fileSetData`, `fileutil*` | Requires structured argument maps and host permission boundaries |
| Async services | `firebase*`, `requestnetwork*`, `speechToText*`, `locationManager*`, Bluetooth, camera and picker callbacks | Requires callback event bindings and observable async results |
| Media, dialogs, ads and notifications | `mediaplayer*`, `soundpool*`, `dialog*`, ad loading/showing, notification operations | Requires lifecycle-aware service state and callback-safe effects |

This gap map confirms that component coverage in the matrix is not equivalent to behavioral parity. The next implementation group should therefore expand the typed block/value contract rather than adding more nominal service registrations.
