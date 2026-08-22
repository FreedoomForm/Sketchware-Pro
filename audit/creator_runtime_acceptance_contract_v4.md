# Creator Runtime — Acceptance Contract v4

## Purpose

This contract defines the evidence required before Creator Runtime may be described as functionally complete for the requested product flow. A compile, unit-test pass, or the existence of a class is not sufficient evidence. Every criterion must be traced to a user-visible path, persisted state, and a test or explicit limitation.

## Product invariants

| ID | Invariant | Required observable evidence | Failure meaning |
|---|---|---|---|
| P-01 | There is exactly one active Creator Runtime project in the normal product flow. | No project creation/list/switch screen is required to reach the editor; startup resolves the single project deterministically. | Product flow diverges from the one-project requirement. |
| P-02 | The first user-visible surface is the original Sketchware `DesignActivity` on `main`. | Manifest and startup path reach the original editor; no replacement Home screen appears first. | Runtime is not Sketchware editor-first. |
| P-03 | The original Sketchware editor remains the editor UI. | View, Logic, Events, Components, activity manager, drawers, dialogs, and block editor are reachable through original activities/layouts. | A custom imitation has replaced required editor behavior. |
| P-04 | Runtime is the authoritative live projection after editing. | A legacy editor mutation survives Back and is rendered by the native runtime without compile/save button or restore dialog. | Projection/lifecycle boundary is broken. |
| P-05 | The native live surface and the editor reopen to the same persisted `main` state. | Round-trip test compares widget IDs/properties, screens, components, events, and relevant blocks before and after Back/reopen. | Runtime and editor have divergent state. |

## First-run scaffold contract

| ID | Requirement | Required evidence |
|---|---|---|
| S-01 | First run shows a blank white `main` canvas with only the built-in Continue widget. | Native screenshot/accessibility/tree assertion plus persisted document assertion; no title or extra starter widgets. |
| S-02 | Continue is an ordinary editable main widget. | Original View editor can locate/select/edit/move/delete it; no renderer-only special case hides it. |
| S-03 | Continue has a real Sketchware Intent component from first run. | Components store contains the expected Java-name activity record and Intent component; Components UI can display it. |
| S-04 | Continue has a real Sketchware click event and blocks. | Events/Logic store contains the click event and block sequence; Logic editor can open it. |
| S-05 | Continue enters the project editor. | Native click dispatch resolves the runtime action and opens original `DesignActivity` for the same project. |
| S-06 | Shake remains a reserve editor entry. | Shake handler is reachable in the application lifecycle and opens the same editor context without creating a second project. |
| S-07 | Starter repair is versioned and deletion is stable. | Old broken marker receives one repair; after current version, deliberate deletion is not recreated on every load. |

## Editor surface contract

| ID | Requirement | Required evidence |
|---|---|---|
| E-01 | `main` and `editor` activities exist from first run. | ViewSelector and ManageView list both contain both activities; legacy files and runtime screens agree. |
| E-02 | `editor` is locked by default. | Persisted runtime model and legacy ProjectFileBean option both contain locked state. |
| E-03 | Lock indicators are visible beside every activity. | ViewSelector popup and ManageView rows expose visible lock/unlock indicators in API 30 and API 34 tests. |
| E-04 | Lock can be changed from the original activity editor. | AddViewActivity exposes the runtime lock row; save/reopen changes the persisted bit and runtime model. |
| E-05 | Activity add/edit/delete follows original Sketchware behavior while respecting lock. | Runtime manager reaches child AddViewActivity; locked editor cannot be deleted accidentally; unlocked behavior is test-covered. |
| E-06 | Continue is visible and editable in the first-run View tab. | Test enters the original View editor and asserts the widget row/canvas is present. |
| E-07 | Components, Events, Logic, and visual blocks are reachable. | Native navigation test opens each relevant tab/activity and confirms non-empty starter records and editor accessibility. |
| E-08 | Original appearance and tabs are retained. | No custom replacement editor is used; resource/activity path is original. Visual parity claims are limited to inspected paths. |
| E-09 | Right sidebar contains Versions linked to original changelog/version page. | Drawer opens in runtime mode, Versions item is visible/clickable, and target route is `AboutActivity` changelog. |
| E-10 | Lower down-arrow/options control and Run control are hidden in runtime editor mode. | Native view visibility assertions cover both controls on fresh and resumed editor instances. |
| E-11 | Swipe-right does not leave the intended runtime editor path. | Gesture/navigation handling is traced and tested where the original UI exposes it. |

## Round-trip data contract

| ID | Boundary | Required evidence |
|---|---|---|
| R-01 | Runtime document → legacy project | Every required runtime screen/view is projected to correct `eC` keys and serialized with the real store API. |
| R-02 | Legacy View mutation → runtime document | Direct ViewBean mutation and/or stable UI mutation imports after save/Back without being overwritten by resume. |
| R-03 | Legacy Components mutation → runtime document | Component records use Java-name keys and survive editor lifecycle. |
| R-04 | Legacy Events/Blocks mutation → runtime document | Event and block records are imported or reconstructed; starter `setScreen`/`startActivity` maps to runtime editor action. |
| R-05 | Child activity resume | Opening/closing ViewSelector, ManageView, AddView, Logic, Events, and Components does not project stale runtime state over fresh legacy edits. |
| R-06 | Back/finish | Existing save task completes before final import and live renderer launch; no save/restore prompt interrupts the flow. |
| R-07 | Reopen | Reopening DesignActivity projects/imports consistently and preserves the last live state. |
| R-08 | Process recreation | State survives activity recreation and application restart within the one active project. |
| R-09 | Error handling | Null stores, absent markers, malformed/old documents, permission bypass, and missing legacy records degrade safely or repair deterministically. |

## Runtime behavior contract

| ID | Requirement | Required evidence |
|---|---|---|
| B-01 | Native renderer shows the same main layout as editor. | Widget IDs, order, text, visibility, geometry, and supported properties match after round-trip. |
| B-02 | Added button remains visible after Back. | The exact reported regression is covered by an Android test that mutates a legacy ViewBean and asserts live canvas plus reopen. |
| B-03 | Button event behavior remains visible/functional where runtime supports it. | Event binding is preserved and clicking the added button dispatches its supported runtime action. Unsupported behavior is documented, not silently claimed. |
| B-04 | Continue and custom editor-entry buttons dispatch the same runtime action. | Runtime action mapping test covers starter and user-assigned editor-entry intent. |
| B-05 | Locked editor is still reachable by Continue/shake but not editable/deletable through ordinary manager actions. | Navigation and lock-policy tests cover both paths. |
| B-06 | Runtime services match the actions exposed to users. | Inventory maps each exposed tool/action to implementation and test; no tool is advertised without a corresponding UI path. |

## Audit method and evidence grades

| Grade | Meaning |
|---|---|
| A | User-visible end-to-end native evidence on API 30 and API 34, with persisted-state assertions. |
| B | Native or JVM integration evidence across the exact boundary, but not a full UI gesture/action. |
| C | Static source/dataflow evidence only. |
| D | Inferred from naming or unverified implementation. |
| F | Contradicted by a reproducible failure or missing path. |

A requirement may be called **implemented** only with Grade A or B evidence. Grade C is an audit finding requiring stronger testing. Grade D is unverified. Grade F requires a corrective change or explicit known limitation.

## Non-claims

This contract does not permit a claim of literal 100% line-by-line coverage of the entire Sketchware Pro repository. It permits a narrower, auditable claim only for the specified Creator Runtime user flow and the original editor paths that participate in it, with remaining unverified subsystems listed explicitly.

## Final gate

Before final delivery, the repository must contain:

1. this contract;
2. an inventory of all relevant activities, layouts, stores, bridges, services, and lifecycle edges;
3. a requirement-to-code-to-test matrix with evidence grade and status;
4. an end-to-end native round-trip test for an added button and starter Intent/blocks;
5. local build/test evidence;
6. one pushed `Creator Runtime Android` workflow result covering Build/JVM, API 30, and API 34;
7. a final report that separates verified behavior, repaired behavior, unverified behavior, and device-specific validation still required.
