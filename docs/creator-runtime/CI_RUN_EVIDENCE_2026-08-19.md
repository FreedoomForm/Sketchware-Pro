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

## Final push-triggered rerun evidence

The documentation push `97e143287` created run `32331071126`. Its APK/JVM job succeeded, API 34 succeeded, and the first API 30 attempt failed only because the GitHub-hosted emulator disconnected (`ShellCommandUnresponsiveException` followed by `No compatible devices connected`), with no test assertion failure. A run-level rerun of failed jobs was requested and completed successfully:

| Rerun job | Job ID | Result |
|---|---:|---|
| Native Android tests (API 30), rerun | `96317389283` | success |
| Native Android tests (API 34), original job | `96317413019` | success |

This confirms both native emulator matrix legs pass after retrying the transient runner failure.


## New typed widget batch — commit 7dd1a8286

The push to `FreedoomForm/Sketchware-Pro` branch `creator-runtime` created the required workflows at `2026-08-20T09:49:45Z`:

| Workflow | Run | Observation | Evidence |
|---|---:|---|---|
| Creator Runtime Android | `32355865972` | `pending` at first poll | Native matrix is queued behind the APK/JVM job; API 30/API 34 results are not yet confirmed |
| Android CI | `32355865947` | `in_progress` at first poll | Release APK build was running; Update App Data completed successfully without changing `origin/creator-runtime` |

Local pre-push evidence for `7dd1a8286` is complete: `./gradlew test`, `assembleDebug`, `testDebugUnitTest`, and `compileDebugAndroidTestJavaWithJavac` passed, and the working tree and `origin/creator-runtime` both resolve to `7dd1a8286`. The corresponding native emulator gate remains open until the Creator Runtime Android run reports successful API 30 and API 34 jobs.


## Bluetooth UUID native slice — pending next push

The next local increment adds a production native event binding for Bluetooth `random_uuid` and a non-empty runtime-state assertion in `CreatorRuntimeNativeWidgetTest`. Local `testDebugUnitTest`, `assembleDebug`, and `compileDebugAndroidTestJavaWithJavac` pass. The code and evidence documents will be pushed together; native hardware status, permission, paired-device, and RFCOMM transport behavior remain separate open gates.


## Android CI success — commit 8d3e41ad0

Public workflow status now reports `Android CI` run `32356402263` for commit `8d3e41ad0` as `success`. This confirms the general Android CI path for the Bluetooth UUID native slice. The specialized `Creator Runtime Android` run `32356402215`, which carries the required debug APK/JVM plus API 30/API 34 native matrix, remains `pending`; its native evidence gate is therefore still open.


## Permission bridge native batch — next checkpoint

The permission bridge batch adds a deterministic Android-test-runner regression for explicit `NO_HOST`, `REQUEST_REQUIRED`, `DENIED`, `GRANTED`, and `UNSUPPORTED` outcomes. Local `testDebugUnitTest`, `assembleDebug`, and `compileDebugAndroidTestJavaWithJavac` pass. The batch will be committed and pushed as a separate checkpoint; remote API 30/API 34 native execution remains open until the corresponding workflow completes.


## Notification permission native batch — next checkpoint

The notification batch adds Android-runner coverage for the production `POST_NOTIFICATIONS` gate across API 32, API 33, and API 36 contract cases. Local `testDebugUnitTest`, `assembleDebug`, and `compileDebugAndroidTestJavaWithJavac` pass. The batch will be committed and pushed separately; actual system permission request/result behavior and API 30/API 34 workflow execution remain open until remote evidence completes.


## Android CI success — notification permission checkpoint bd08207ba

Public workflow status reports `Android CI` run `32358158139` for commit `bd08207ba` as `success`. The specialized `Creator Runtime Android` run `32358158157`, which carries the required debug APK/JVM and API 30/API 34 native matrix, remains `pending`; native device evidence is therefore still open.


## Firebase Cloud Message validation batch — next checkpoint

The Firebase Cloud Message batch adds deterministic Android-runner validation for unsupported actions and missing subscribe/unsubscribe topics. Local `testDebugUnitTest`, `assembleDebug`, and `compileDebugAndroidTestJavaWithJavac` pass. Valid FCM token/topic operations remain an open Firebase configuration and device/network gate; this validation batch will be committed and pushed separately.

## Firebase Auth Phone validation batch — commit 5ffb33c07

The Firebase Auth Phone batch adds `CreatorRuntimeNativeWidgetTest.firebaseAuthPhoneRejectsInvalidInputsOnNativeRuntime` for unsupported action and incomplete `send_code`/`confirm_code` arguments. Local `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon` completed successfully. Commit `5ffb33c07` was pushed to `FreedoomForm/Sketchware-Pro:creator-runtime`; Creator Runtime Android run `32359775216` was pending and Android CI run `32359775219` was in progress at the first public poll. API 30/API 34 native evidence remains open until the remote workflow completes. Valid phone verification, Firebase project configuration, SMS/reCAPTCHA, credential, network, and device behavior remain separate open integration gates.

## Firebase Google Login validation batch — next checkpoint

The Firebase Google Login batch adds `CreatorRuntimeNativeWidgetTest.firebaseGoogleLoginRejectsInvalidInputsOnNativeRuntime` for unsupported action and missing `webClientId`. Local `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon` completed successfully. The batch is ready for a separate commit and push; the currently running workflows for `31ce5bff9` remain independent. Valid OAuth/Firebase configuration, account, Play services, network, and device behavior remain open integration gates.

## Rewarded Ad validation batch — next checkpoint

The Rewarded Ad batch adds `CreatorRuntimeNativeWidgetTest.rewardedAdRejectsInvalidInputsOnNativeRuntime` for unsupported action, missing `adUnitId`, and showing before an ad is loaded. Local `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon` completed successfully. The batch is ready for a separate commit and push; remote API 30/API 34 evidence remains open until its workflow completes. Valid ad SDK initialization, consent/policy, configuration, network, timing, and display behavior remain open integration gates.

## Fragment Adapter validation batch — next checkpoint

The Fragment Adapter batch adds `CreatorRuntimeNativeWidgetTest.fragmentAdapterRejectsInvalidInputsOnNativeRuntime` for missing pager `widgetId` and unsupported action paths. Local `./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon` completed successfully. The batch is ready for a separate commit and push; remote API 30/API 34 evidence remains open until its workflow completes. Live pager rendering, adapter population, fragment lifecycle, animation, and device UI behavior remain open integration gates.
