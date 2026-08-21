# Original Sketchware UI / Creator Runtime audit findings

## Original editor surface

`DesignActivity` uses `design.xml` and a `ViewPager` with exactly four tabs: View, Events, Components, and AI. The layout contains the toolbar, tab layout, full-screen pager, file/screen selector, property panel, right-side `DesignDrawer`, bottom options menu, and the `btn_run` Material button inside the bottom split button. The original View tab is `ViewEditorFragment`; the Events tab is `rs`; the Components tab is `br`; and the AI tab is `com.sketchware.ai.ui.chat.ChatFragment`.

## Visual View editor

`ViewEditorFragment.onCreateView()` inflates `fr_graphic_editor`, creates the original `ViewEditor`, initializes `WidgetsCreatorManager`, connects `ViewProperty`, and initializes the built-in widget palette. Property event clicks call `toLogicEditorActivity()`, which launches `LogicEditorActivity` with `sc_id`, event id/name, `ProjectFileBean`, and event text. The widget palette and property panel therefore remain original Sketchware UI and should not be replaced by a runtime-only screen.

## Confirmed crash

On the Redmi SDK 33 log, `WidgetsCreatorManager.initializeCategoriesList()` iterates `widgetConfigurationsList`, but Gson can return null for an empty or literal `null` widgets JSON file. The existing error path preserved null and later crashed at `ArrayList.iterator()`. This is a real startup blocker for the original View tab.

## Confirmed synchronization gap

`DesignActivity.onResume()` calls `importLegacyRuntimeBoundary()`, but the original editor's save-and-close path (`SaveChangesProjectCloser`) saves legacy stores and calls `activity.finish()` without importing the resulting legacy snapshot into `CreatorRuntimeSession`. Consequently, the Creator home may show the pre-editor runtime document after returning. A runtime import must happen after legacy save completes and before the activity finishes, or the home must import the snapshot on resume using the stable bridge mapping.

## Block editor entry audit

The original visual block editor is `LogicEditorActivity`. The currently visible entry from the View editor is the property panel event click. The next audit step is to verify event rows in the Events tab and their click listeners, plus LogicEditor startup extras and runtime event/block import/export. A direct, tested event-row path must exist for entering the visual blocks screen.

## Requested UI change

The bottom `btn_run` button is present in `design.xml` and wired in `DesignActivity.onCreate()`. It must be removed from the original editor's bottom bar while preserving the options button and automatic runtime behavior. This should be implemented as a UI-only removal/visibility change, not by replacing the original editor surface.

## Workflow constraint

No GitHub Actions should be triggered until the UI coverage audit, runtime wiring, tests, and local verification are complete. The branch should use only the Creator Runtime Android workflow with native tests for the eventual single push.

## Additional confirmed gaps

`rs.openEvent()` launches `LogicEditorActivity` with legacy extras only. `ViewEditorFragment.toLogicEditorActivity()` does the same. Neither path forwarded `creator_runtime_project_id`, and LogicEditorActivity checked `super.isStoragePermissionGranted()` directly in `onCreate()` and `onResume()`. On Android 13 this can immediately finish the visual block editor even when DesignActivity was runtime-safe.

`LogicEditorActivity` saves its canvas through `E()` during `ProjectSaver`, which runs from the normal Back path, so DesignActivity's resume/import boundary can consume the saved blocks after returning. DesignActivity itself did not import the final legacy snapshot inside its `finish()` after save-close, so returning to CreatorHome could leave the runtime document stale.

The bottom Run button was wired directly in `DesignActivity.onCreate()` and mutated by BuildTask progress updates. Removing the button requires a nullable legacy reference and guards around listener/progress updates; otherwise the original editor crashes when the view is absent.

`CreatorProjectActivity` remains a custom runtime preview/editor shell with a bespoke header, add-widget controls, preview canvas and sidebar. CreatorHomeActivity currently launches DesignActivity, so this custom shell is not the intended project editing window, but its renderer is the existing reusable reference for showing the runtime document on the post-editor main surface.
