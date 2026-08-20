# Native Widget Runtime Batch Plan

## Scope

The first post-audit batch closes device-level behavior evidence for the most deterministic interactive runtime surface: a typed Button event with a state effect, ListView and Spinner data/selection behavior, and DrawerLayout open/close/back precedence. The batch uses the production `CreatorRuntimeProjectStore` and `CreatorProjectActivity`; it must not add test-only injection points, generated Java, plugin execution, or native-build fallback paths.

## Runtime contract

| Capability | Typed runtime contract | Native acceptance assertion | Failure policy |
|---|---|---|---|
| Button click event | `CreatorEventBinding(targetWidgetId, eventName=click)` dispatches typed `INCREMENT_STATE` or `SET_STATE` blocks through `CreatorRuntimeExecutor` | Launch seeded document, click the real rendered `MaterialButton`, and verify the persisted runtime state changes through `CreatorRuntimeSession` | A missing binding, missing state effect, or generated-code path is a test failure |
| ListView data and selection | `listSetData`/`listSetItemChecked`/`listGetCheckedPosition` and `listGetCheckedCount` use the registered native `ListView` through the allow-listed `widget` service | Seed typed list data, render a real `ListView`, select/check a row, and verify the runtime query returns the native selection | No document-only stale-value fallback when the native view is registered |
| Spinner data and selection | `spnSetData`/`spnSetSelection`/`spnGetSelection` use the registered native `Spinner` through the same typed `widget` service | Seed typed spinner data, render the real `Spinner`, select a non-default item, and verify the typed query returns its position | Invalid or unregistered targets must return a visible typed service error, not execute source |
| Drawer open/close/back | `CreatorDrawerService` exposes only typed `open`, `close`, and `is_open`; `CreatorProjectActivity.onBackPressed()` closes an open drawer before activity navigation | Seed activity drawer metadata, invoke the typed drawer service, assert the native `DrawerLayout` opens, then invoke back and assert it closes while the activity remains present | Drawer actions without a registered drawer remain invalid and visible; no fallback path |

## Fixture contract

The instrumentation fixture will clear `creator_runtime/active_document`, encode a `CreatorProjectDocument` containing one entry screen, a column root, button/list/spinner children, drawer metadata plus a `_drawer_home` screen, typed list/spinner state, and a button event binding. It will write the encoded document through the same SharedPreferences key used by `CreatorRuntimeProjectStore`, then launch `CreatorProjectActivity` normally.

Assertions must obtain views from the rendered hierarchy and runtime state from the production `CreatorRuntimeSession`. The test may use `ActivityScenario.onActivity` and direct main-thread view operations where Espresso focus is unstable, following the existing native harness pattern. It must not reach into private activity fields or bypass the renderer.

## Evidence required before commit

The batch is complete only when the JVM suite still passes, the new native test compiles, API 30 and API 34 emulator jobs execute the fixture, and the CI evidence register records the exact commit and job outcomes. Any emulator-only infrastructure disconnect may be retried, but a test assertion failure must be fixed before the batch is marked verified.
