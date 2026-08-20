# Local Native Corrective Fixes — Push-Gated

## Scope

This checkpoint addresses the four native API 30 failures observed in GitHub Actions for the previously pushed runtime commit. The fixes are local only; no additional GitHub push is authorized by the current task constraint.

| Failure | Root cause | Local correction |
|---|---|---|
| Bitmap invalid source returned `SecurityException` | `CreatorBitmapService` caught `IOException` and `IllegalArgumentException`, but `CreatorFileService.resolveForRuntime` correctly rejected an out-of-root path with `SecurityException` | Catch `SecurityException` and convert it to typed `FAILED`, preserving the R1 boundary and preventing an exception leak |
| Camera capture with null environment threw NPE | `CreatorCameraService` dereferenced `environment` before checking host availability | Return typed `PERMISSION_REQUIRED` when the host Activity/environment is unavailable, before permission or intent access |
| Calendar invalid field assertion returned `SUCCEEDED` | The failing remote binary predates the current local calendar validation source; local source already rejects invalid fields. The local test is retained and passes in the full JVM/build validation | No fallback introduced; source/test contract remains typed `UNSUPPORTED_ARGUMENT` |
| Rerender fixture could not find `Increment` | `CreatorRuntimeSession` is an application singleton and survived preference clearing between instrumentation tests; fixture also used renderer-incompatible `seekbar` instead of R1 `slider` | Add a test lifecycle reset hook, call it in `@Before`, and change the fixture to the production `slider` type |

## Local evidence

The following local commands pass after the corrections:

```text
./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac --no-daemon
./gradlew test --no-daemon
```

The first command validates JVM tests, debug APK assembly, and androidTest Java compilation. The second command validates the full local JVM suite. A local emulator is not installed in the sandbox, so API 30/API 34 connected execution cannot be reproduced locally.

## Push gate

These corrections are committed locally but are **not pushed**. The remote branch remains at the previously authorized single-push commit until the user explicitly authorizes another push. The next remote validation must run the existing API 30/API 34 matrix against a commit containing this checkpoint.
