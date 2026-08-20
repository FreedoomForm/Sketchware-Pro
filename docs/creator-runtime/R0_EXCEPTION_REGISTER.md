# R0 Exception Register

## Purpose

R0 is a visible blocked disposition, not an execution fallback. This register prevents invalid, untrusted, or intentionally unsupported legacy input from being silently counted as R1. No R2/R3 execution tier is permitted.

## Enumerated exceptions

| Stable exception family | Trigger | Runtime disposition | Evidence |
|---|---|---|---|
| `opcode:addSourceDirectly` | Arbitrary Java source injection from a legacy block | Visible `R0_UNSUPPORTED`; never compiled, interpreted, or delegated | `OPCODE_AUDIT.md`, importer rejection tests |
| `block:unrecognized-opcode` | Executable legacy opcode outside the audited typed set | Event/block import is blocked with an actionable compatibility report | `CreatorLegacyArtifactImporter` and artifact importer tests |
| `moreblock:unsupported-body` | More Block body contains an unrecognized executable opcode | More Block definition is blocked visibly; no generated Java | Artifact importer tests and scoped-frame contract |
| `component:unknown-type` | Component type has no registered Creator Runtime service | Component is not imported as executable behavior | `CreatorRuntimeComponentServiceMatrix` and importer tests |
| `component:missing-id` | Component lacks a stable identifier | Component import is blocked before registration | Artifact importer validation |
| `event:missing-target-or-name` | Event lacks stable target or event name | Event is blocked before binding | Artifact importer validation |
| `project-file:missing-name` | Project file lacks stable filename | File relationship is blocked before Project IR insertion | Project metadata importer validation |
| `library:local-or-native` | Arbitrary local/native library request | Library is blocked because trusted runtime execution cannot be established | Project metadata importer tests |
| `resource:missing-name-or-source` | Resource lacks stable name/source | Resource is blocked before runtime metadata insertion | Resource importer validation |
| `value-resource:unsupported-family` | Value XML is not strings/colors/styles/themes/arrays | XML is blocked visibly and not compiled | Value-resource importer tests |
| `value-resource:malformed-xml` | Allow-listed value family contains malformed XML | XML is blocked visibly with parse reason | Value-resource importer tests |
| `view:unknown-type` | Legacy view ID is outside the explicit 0–48 matrix | View import is blocked; no silent widget downgrade | `CreatorLegacyViewImporter` and view matrix tests |
| `runtime:invalid-arguments` | A registered service receives missing, malformed, or unsupported action data | Typed `UNSUPPORTED_ARGUMENT`/`FAILED`; no host side effect before validation | Native service validation batches |
| `compatibility:unknown-feature` | Compatibility analyzer receives a feature without a reviewed service/operation mapping | Visible R0 report; no plugin/native-build request | Compatibility analyzer tests |

## Release interpretation

The presence of an entry in this register is not permission to claim that the corresponding legacy behavior was transferred. The entry means the behavior is **explicitly blocked and user-visible**. A 100% release review may proceed only when every in-scope legacy item is either backed by R1 import and behavior evidence or appears here with a stable identifier and actionable reason, and when no executable R2/R3 path exists.

The current review is not yet 100% complete because device behavior, broad editor-action inventory, and some live project/resource parity gates remain open. This file does not authorize a GitHub push.
