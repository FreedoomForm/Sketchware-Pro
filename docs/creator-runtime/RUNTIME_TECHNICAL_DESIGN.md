# Creator Runtime Technical Design

## 1. Component model

The implementation uses Java packages beneath `pro.sketchware.creator.runtime`. The runtime core must be Android-framework-light so its state and operation rules can be tested as ordinary JVM code.

| Component | Responsibility | Must not do |
|---|---|---|
| `ProjectDocument` | Immutable versioned representation of one Creator project | Directly mutate global Sketchware files |
| `ProjectOperation` | Typed requested transition from one revision to another | Render UI or contain provider-specific AI data |
| `OperationValidator` | Validate schema, references, host safety, and compatibility tier | Silently repair destructive requests |
| `OperationReducer` | Deterministically apply an approved operation | Touch Android UI or filesystem |
| `RevisionStore` | Append successful revisions, checkpoints, and bounded undo/redo history | Accept unvalidated state |
| `RuntimeEventLog` | Record structured, redacted diagnostics | Store prompts, secrets, or raw private content by default |
| `RuntimeRenderer` | Render the Project IR inside Creator preview | Change the IR as a side effect |
| `PluginBridge` | Invoke allow-listed device capabilities | Dynamically load arbitrary executable code |
| `NativeBuildQueue` | Build a specifically pinned revision asynchronously | Become the source of truth for live preview |
| `RecoveryController` | Handle shake gesture and safe editor return | Depend on a user-editable widget remaining clickable |

## 2. Project IR v1

The first schema deliberately covers a constrained core. It is a stable contract, not a copy of every internal Sketchware storage file.

```json
{
  "schemaVersion": 1,
  "projectId": "project_01...",
  "revision": 7,
  "metadata": {
    "name": "My app",
    "entryScreenId": "screen_home",
    "theme": {"primaryColor": "#6750A4"}
  },
  "screens": [{
    "id": "screen_home",
    "route": "/",
    "rootWidgetId": "root_home"
  }],
  "widgets": [{
    "id": "root_home",
    "type": "column",
    "parentId": null,
    "children": ["title", "continue"],
    "properties": {"padding": 24}
  }],
  "state": {"variables": []},
  "logic": {"events": []},
  "resources": [],
  "plugins": [],
  "hostControls": {
    "entryControl": {
      "enabled": true,
      "label": "Continue",
      "placement": "bottom_end"
    }
  }
}
```

### Invariants

1. `projectId`, `schemaVersion`, and `revision` are mandatory.
2. Every screen has exactly one root widget and every referenced widget exists.
3. A widget belongs to at most one parent; root widgets have no parent.
4. Widget IDs, screen IDs, routes, variables, and event IDs are unique in their respective namespaces.
5. The entry screen exists and is reachable.
6. Plugins are allow-listed capability IDs with explicit configuration.
7. Host recovery is logically enabled even if the user hides the visible entry control.
8. The model is serializable in a deterministic order for hashing, diffing, and replay.

## 3. Operation contract

### Envelope

```json
{
  "operationId": "op_01...",
  "projectId": "project_01...",
  "baseRevision": 7,
  "actor": {"kind": "user", "id": "local"},
  "type": "widget.add",
  "payload": {
    "widget": {"id": "button1", "type": "button", "properties": {"text": "Continue"}},
    "parentId": "root_home",
    "index": 1
  },
  "requestedAtEpochMs": 0
}
```

### Lifecycle

```mermaid
sequenceDiagram
  participant Actor as User or AI
  participant API as Operation Pipeline
  participant V as Validator
  participant R as Reducer
  participant S as Revision Store
  participant P as Preview
  participant L as Event Log

  Actor->>API: ProjectOperation(base revision)
  API->>V: Validate invariants, capability, safety
  alt rejected
    V-->>Actor: Machine-readable validation failure
    API->>L: operation.rejected
  else accepted
    V->>R: Apply operation
    R->>S: Append immutable revision
    S->>P: Render committed revision
    S->>L: operation.applied + render result
    P-->>Actor: Updated preview
  end
```

### Rules

| Rule | Requirement |
|---|---|
| Optimistic concurrency | `baseRevision` must equal the current revision. Otherwise the result is `STALE_REVISION`; the caller reloads and retries explicitly. |
| Idempotency | Replaying an already-applied `operationId` returns the existing result rather than applying twice. |
| Atomicity | One accepted operation produces one new revision. Compound UI changes use a named transaction operation or a batch that validates fully before commit. |
| No direct mutation | Renderers, AI adapters, plugins, and native build code cannot modify Project IR. They must emit operations. |
| User approval | Destructive, permission-changing, or native-fallback operations require approval policy evaluation before reduction. |
| Explainability | Each operation type has a localized human-readable description and a machine-readable diff summary. |

## 4. Recovery design

Creator Runtime has two independent routes to the editor:

1. **Editable visible route:** the Entry Control is projected over the preview by the host. Its descriptor is in Project IR and can be changed with an operation.
2. **Non-editable emergency route:** a shake detector registered by Creator Home/Preview opens `CreatorRecoverySheet`. The sheet offers Open Editor, Reset Entry Control, Restore Last Checkpoint, and Open Historical Sketchware Editor.

The visible control may never be the only recovery mechanism. A renderer failure opens the same recovery sheet automatically with the last valid revision rather than a blank dead-end.

## 5. Diagnostics and log schema

`RuntimeEventLog` records append-only event objects in a local ring buffer. Each event is designed for debugging without retaining raw user content.

```json
{
  "eventId": "evt_01...",
  "timestampEpochMs": 0,
  "projectId": "project_01...",
  "revision": 8,
  "category": "operation|render|plugin|recovery|build|ai",
  "name": "operation.applied",
  "severity": "debug|info|warning|error",
  "correlationId": "op_01...",
  "attributes": {
    "operationType": "widget.add",
    "durationMs": 4,
    "result": "success"
  }
}
```

### Required event names

| Category | Event names |
|---|---|
| Operation | `operation.requested`, `operation.rejected`, `operation.applied`, `operation.replayed`, `operation.stale_revision` |
| Revision | `revision.checkpoint_created`, `revision.undo`, `revision.redo`, `revision.restore` |
| Render | `render.started`, `render.completed`, `render.failed`, `render.fallback_shown` |
| Recovery | `recovery.shake_detected`, `recovery.sheet_opened`, `recovery.entry_control_reset`, `recovery.checkpoint_restored` |
| Plugin | `plugin.requested`, `plugin.approved`, `plugin.denied`, `plugin.completed`, `plugin.failed` |
| AI | `ai.operation_requested`, `ai.operation_approved`, `ai.operation_rejected` |
| Build | `build.queued`, `build.started`, `build.completed`, `build.failed`, `build.cancelled` |

### Privacy rules

The default log excludes raw prompts, text-entry values, credentials, tokens, image bytes, HTTP bodies, and full source files. Export is explicit and allows users to review the proposed diagnostic bundle before sharing it.

## 6. Native fallback and migration policy

The live runtime never executes arbitrary Java, Kotlin, Dex, or native binaries from a project. A project feature classified R3 remains visible in the inspector and can be opened in the historical Sketchware editor. The background native build queue produces an artifact from an immutable revision and writes a provenance record containing the revision hash and compatibility report.

Migration is incremental. An importer maps known Sketchware widgets, blocks, resources, components, and events into Project IR. Each input element creates one of these outcomes: `MIGRATED_RUNTIME_NATIVE`, `MIGRATED_PLUGIN`, `PRESERVED_NATIVE_FALLBACK`, or `UNSUPPORTED`. The user sees the complete report before committing the migration.

## 7. First executable slice

The first implementation slice is deliberately narrow and testable:

1. `ProjectDocument` with project metadata, one screen, a widget tree, and host-entry-control descriptor.
2. `ProjectOperation` for `project.create`, `screen.create`, `widget.add`, `widget.set_property`, `host_entry_control.update`, and `revision.restore`.
3. Deterministic validator/reducer/revision store with undo/redo and bounded event log.
4. JVM tests for replay determinism, stale revisions, invalid tree rejection, recovery descriptor protection, and redacted logs.

No Android navigation or visual runtime is added until these contracts work in isolation.
