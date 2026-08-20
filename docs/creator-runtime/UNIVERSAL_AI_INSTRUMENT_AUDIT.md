# Universal AI Instrument Coverage Audit

## Scope

This audit verifies that the AI-facing `creator_runtime` instrument reaches the same typed Creator Runtime operation pipeline as the visual editor. The AI adapter maps normalized JSON into `CreatorProjectOperation` with `ActorKind.AI`; accepted operations are persisted by `CreatorRuntimeSession`, and event blocks execute through `CreatorRuntimeExecutor` and the registered typed service dispatcher. No R2/R3 fallback or Java source-generation path is used.

## Top-level AI actions

| AI action | Typed operation | Schema | Mapper test |
|---|---|---:|---:|
| `create_screen` | `SCREEN_CREATE` | covered | covered |
| `add_widget` | `WIDGET_ADD` | covered | covered |
| `set_widget_property` | `WIDGET_SET_PROPERTY` | covered | covered |
| `set_state` | `STATE_SET` | covered | covered |
| `update_entry_control` | `ENTRY_CONTROL_UPDATE` | covered | covered |
| `attach_event` | `EVENT_ATTACH` | covered | covered |
| `restore_revision` | `REVISION_RESTORE` | covered | covered |

**Top-level result: 7/7 actions covered.**

## Typed runtime block instruments

The AI schema now explicitly declares every `CreatorRuntimeBlock.Type` value in its block `type` enum. The mapper test constructs all 16 values and confirms that each is accepted by the same typed mapper used by the visual editor.

| Block instrument family | Coverage |
|---|---:|
| Widget/state mutation: `set_widget_property`, `set_state`, `increment_state` | 3/3 |
| Collection mutation: `list_mutate`, `map_mutate` | 2/2 |
| Event/effect: `attach_event`, `show_message`, `navigate`, `runtime_service_call` | 4/4 |
| More Block/function flow: `custom_function_call`, `return` | 2/2 |
| Conditional/loop flow: `if_state_equals`, `if_boolean`, `repeat`, `forever`, `break` | 5/5 |

**Schema and mapper result: 16/16 block instruments covered.** The executor test suite already covers all block families; a dedicated `NAVIGATE` effect test was added because it previously had no direct executor-test reference.

## Universal pipeline boundary

The AI tool does not write Java, mutate legacy files directly, or invoke a parallel renderer. It calls `CreatorRuntimeOperationMapper`, creates an AI-attributed typed operation, submits it to `CreatorRuntimeSession`, and returns success only after the operation is applied. `runtime_service_call` blocks route through `CreatorRuntimeServiceDispatcher`, which is the same R1 service registry used by native runtime execution.

## Evidence

Local evidence currently passes:

```text
./gradlew testDebugUnitTest test --no-daemon
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
```

The native API 30/API 34 evidence remains governed by the active GitHub Actions matrix. The current follow-up commit contains this audit's schema/test changes locally and must receive CI confirmation before universal AI coverage is marked release-green.

## Verdict

Within the defined universal AI instrument surface, coverage is **100% at schema, mapper, and executor-dispatch levels: 7/7 top-level actions and 16/16 block instruments**. Release sign-off still requires the corrective GitHub Actions matrix to finish successfully and the separate exhaustive Sketchware-to-R1 capability audit to remain gap-free.
