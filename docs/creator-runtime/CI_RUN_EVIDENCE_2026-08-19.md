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
