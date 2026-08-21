# Creator Runtime Requirements Specification

## Purpose

Creator Runtime is a single installed Android runtime in which users create, inspect, edit, and execute Sketchware projects without compiling a new APK after every project change. The project model, editor, AI agent, and live application surface must operate on one authoritative runtime state.

> The application shown after leaving the project editor is not a preview and is not a separately generated APK. It is the native runtime execution of the same project model that the editor and AI modify.

## Normative requirements

| ID | Requirement | Acceptance evidence |
|---|---|---|
| CR-01 | One installed Creator Runtime can host and execute supported Sketchware projects without per-change APK compilation. | Runtime can load a project model and render/execute it directly; project mutation tests do not invoke Gradle or APK generation. |
| CR-02 | Creator Home replaces the ordinary first screen for the Creator experience. | Launcher/entry navigation opens a clean home surface with the Creator entry control; legacy project management remains reachable from sidebar/project navigation. |
| CR-03 | The clean home surface contains a circular entry button in the lower-right area. | Android UI test finds the button, verifies placement/visibility, and opens the active/new project session. |
| CR-04 | The project window is a real editor for the active project, available to both a human and the AI agent. | UI and AI mutation tests invoke the same runtime transaction path and update the same project state. |
| CR-05 | Project model and native application surface are synchronized live. | A supported mutation changes actual native views/event bindings without a compile step; integration test verifies model, view tree, and persisted state. |
| CR-06 | Back from the project editor returns to the changed application surface, not to a stale preview. | Navigation test mutates a project, presses Back, and verifies the live native result and Creator recovery control. |
| CR-07 | Save and compile buttons are not required for the normal user flow. | Valid mutations are automatically persisted; no manual save/compile action is required to observe the result. |
| CR-08 | Autosave is durable and safe. | Debounced autosave, flush-on-background/back, atomic persistence, dirty-state handling, and recovery after recreation are covered by tests. |
| CR-09 | The Creator “Next” button exists inside the hosted application surface. | Runtime overlay is present after entering the application surface and routes back to the project/editor session. |
| CR-10 | The Creator button can be moved and visually configured through the project interface. | Position/style descriptor is persisted and reapplied after recreation; movement does not remove the protected recovery route. |
| CR-11 | The Creator button cannot permanently lock the user out of the editor. | Protected host/runtime recovery metadata is separate from ordinary project views; a reset/recovery action restores the control. |
| CR-12 | Shake recovery opens the project/editor recovery flow if the button is unavailable. | Sensor test or deterministic recovery abstraction verifies threshold, cooldown, false-positive filtering, and editor navigation. |
| CR-13 | First-run onboarding explains the shake recovery mechanism. | First-run state displays the recovery instruction once and persists acknowledgement/completion. |
| CR-14 | The runtime supports the transferred Sketchware feature surface. | Existing runtime coverage/audit remains green: registered services, opcode rows, legacy view types, AI actions, typed block instruments, validation, and corresponding tests. |
| CR-15 | AI tools expose only capabilities available through the real Sketchware UI/runtime. | Registry and catalog tests reject backend-only tools; AI mutation path uses runtime/editor transactions. |
| CR-16 | Project switching and sidebar navigation remain available. | Navigation tests open another project, preserve session state, and return to Creator Home without data loss. |
| CR-17 | Runtime failures preserve user data and provide recovery. | Invalid transactions are rejected atomically; autosave retains the last valid model and surfaces a recoverable error. |
| CR-18 | The final implementation is validated locally and by one final GitHub Actions run. | Full local suite passes before the final push; the final workflow run reaches success, with any CI failure fixed and rerun as required. |

## Architectural invariants

1. **One authoritative model.** Editor, AI, live native surface, autosave, and navigation must reference the same active project session rather than maintaining independent preview state.
2. **One transaction boundary.** Human UI mutations and AI mutations must enter through the same validated runtime transaction API.
3. **No hidden backend capability.** AI cannot gain filesystem, Java patching, or web capabilities that a Sketchware user cannot exercise through the application UI.
4. **Protected recovery.** The Creator overlay’s recovery descriptor is host-owned and cannot be deleted or made permanently inaccessible by an ordinary project mutation.
5. **Runtime-first execution.** Supported changes update native runtime state directly. Compilation is outside the normal edit-and-return flow and is not an acceptance criterion for supported runtime features.
6. **Durable state.** A successful transaction is persisted atomically and can be restored after process death, activity recreation, or navigation.
7. **Last-valid recovery.** A failed transaction cannot partially corrupt the active model or remove the user’s route back to the editor.

## Required test layers

| Layer | Required checks |
|---|---|
| JVM unit | Session state transitions, transaction atomicity, autosave debounce/flush, overlay descriptor validation, shake detector thresholds/cooldown, tool registry restrictions. |
| Android integration | Project load, runtime model-to-view binding, event/block binding updates, live back navigation, overlay recovery, project switching, recreation persistence. |
| Instrumentation | Creator Home, lower-right entry button, editor-to-live-surface navigation, no manual compile/save flow, Creator overlay, edit/move overlay, first-run shake onboarding, shake recovery. |
| Regression | Existing Creator Runtime service/opcode/view/AI coverage and existing chat action/tool registry tests. |

## Out-of-scope boundary for the first complete implementation

The runtime may explicitly reject capabilities that require arbitrary new native classes, third-party binaries, unavailable Android services, or project-specific manifest/package changes not represented in the transferred runtime. Such rejection must be visible, atomic, and recoverable. It must not be described as silently supported by the live runtime.

## Definition of complete coverage

The requirements are complete only when every CR item has an implementation mapping, a test mapping, and a passing result. A feature is not considered covered merely because a class or layout exists; the test must demonstrate the observable user behavior and the invariant that protects project state and editor recovery.
