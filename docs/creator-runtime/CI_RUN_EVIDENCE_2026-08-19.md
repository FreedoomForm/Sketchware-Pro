# CI Run Evidence — 2026-08-19

## Run 32292148741

- Repository: `FreedoomForm/Sketchware-Pro`
- Branch: `creator-runtime`
- Commit: `b40ae79c235c96ee9cb81761aeceed8bf89c2448`
- Workflow: `Creator Runtime Android`
- URL: https://github.com/FreedoomForm/Sketchware-Pro/actions/runs/32292148741
- Status at observation: `in_progress`
- `Build debug APK and JVM tests`: `success`
- Artifacts observed: `creator-runtime-debug-apk` (144 MB), `creator-runtime-jvm-reports` (99.2 KB)
- Native jobs: API 29 and API 35 both running; 0/2 completed at observation.

## Previous run 32291060465

- Commit: `e0713269c110dd03ba6227bd4e90d644d8c4788c`
- Build and JVM tests: `success`
- Native API 29 and API 35 failed before tests because `avdmanager` could not find the requested `Pixel_2` device profile.
- The workflow was changed in commit `b40ae79c2` to omit the unsupported explicit profile and use the runner action default profile.

## Earlier build failure

- Commit: `186ac11b74d70c3d274469ee3cd52e6779fd2da0`
- `:app:mergeExtDexDebug` failed with `ERROR: D8: java.lang.OutOfMemoryError: GC overhead limit exceeded`.
- Commit `e0713269c` raised Gradle heap to 5 GB and limited Gradle workers to 2; the next build passed.

## Native test source fixes prepared

Run `32292148741` successfully built the APK and JVM reports, then reached native emulator jobs. API 29 exposed four source-compatibility issues in the existing instrumentation suite: a `ProviderConfigStore.Profile` variable was declared as `ProviderConfigStore`, `findViewById` assertions were ambiguous under Truth, the current `FragmentScenario` API requires a `Bundle` argument, and `SketchwareApiTest` lacked its production import. The popup fallback also referenced a removed `isPlatformPopupWindow()` method.

These issues were corrected in the four affected test files. Local validation now passes with `:app:compileDebugAndroidTestJavaWithJavac` (`BUILD SUCCESSFUL`, 33 actionable tasks: 2 executed, 31 up-to-date). The fixes are ready for the next push-triggered remote native run.

## Latest native runtime diagnosis

The corrected run `32300301256` successfully built the APK and JVM reports and executed 21 instrumentation tests on API 30. The remaining two failures were stale UI expectations: `AISettingsActivity` now opens `ProvidersListFragment`, not the retired `ApiConfigurationFragment`, and the chat test depended on the retired provider form and an external API call. The native tests were updated to assert `providers_recycler` and to use a deterministic local chat UI smoke flow. Local `:app:compileDebugAndroidTestJavaWithJavac` passes after these changes.

## Latest UI runtime diagnosis

Run `32302496900` built the APK/JVM reports and executed native API 30 tests. The two remaining failures were environmental/test-harness issues rather than production crashes: `FragmentScenario` required the debug manifest to declare `androidx.fragment.app.testing.EmptyFragmentActivity`, and the provider-list assertion used an Espresso focus-sensitive root while the drawer was settling. A debug-only `EmptyFragmentActivity` declaration and an `ActivityScenario.onActivity` assertion were added. Local `compileDebugAndroidTestJavaWithJavac` and `processDebugManifest` both pass.

## Second native matrix diagnosis

The rerun reached the native matrix and API 30 reduced from two failures to one: `AISettingsActivityTest.defaultFragmentIsProvidersList` passed after the lifecycle-safe assertion and debug activity declaration. The remaining API 30 failure was `ChatFragmentE2ETest.chatInputSendAddsUserMessage`, caused by Espresso's focused-root picker while `FragmentScenario` was settling. The test now sets the input text and invokes the send button directly from `scenario.onFragment`, preserving the local user-row assertion without relying on window focus. Local Android test Java compilation and debug manifest processing pass again.

## Fully successful remote creator-runtime run

Run `32307303663` for commit `c9e4f90a7` completed successfully on `FreedoomForm/Sketchware-Pro` branch `creator-runtime`.

| Job | Job ID | Result | Evidence |
|---|---:|---|---|
| Build debug APK and JVM tests | `96243897383` | success | Debug APK and JVM reports uploaded |
| Native Android tests (API 30) | `96245581884` | success | `connectedDebugAndroidTest` and native reports uploaded |
| Native Android tests (API 34) | `96245581899` | success | `connectedDebugAndroidTest` and native reports uploaded |

The workflow therefore satisfies the required push-triggered pipeline: debug APK assembly, JVM unit tests, and full native emulator testing on both API 30 and API 34.
