# AdMob Startup Regression

## Failure observed

The application crashed during process startup with `java.lang.IllegalStateException: Missing application ID` from `com.google.android.gms.ads.MobileAdsInitProvider`. This provider is initialized by the Google Mobile Ads SDK before the first Activity is created, so the failure appears as a system crash report rather than an editor/runtime error.

## Why earlier tests missed it

The existing JVM suite validates pure runtime and tool logic. JVM tests do not install the Android application and therefore cannot execute manifest providers. The existing Android tests focus on Creator Home, editor navigation, widgets, and runtime services after instrumentation startup. They did not contain an explicit assertion that the merged application manifest declares `com.google.android.gms.ads.APPLICATION_ID`.

In addition, the local sandbox did not have a connected emulator for full cold-start instrumentation. The GitHub Actions native matrix was still running its emulator step when the failure was reported; the build/JVM job being successful did not prove that the application cold-started successfully on API 30/API 34.

## Fix

The host Manifest now declares `com.google.android.gms.ads.APPLICATION_ID` using the official Google development sample App ID resource `google_mobile_ads_app_id`. The resource is deliberately isolated so a publisher build can replace it with the real `ca-app-pub-...~...` App ID. The duplicate debug-variant metadata was removed so the main host Manifest remains the single source of truth.

## Regression evidence

`CreatorRuntimeNavigationTest.hostManifestDeclaresMobileAdsApplicationId` reads the installed application metadata and checks that the value is present, has the expected `ca-app-pub-...~...` shape, and matches the resource used by the host. The existing launcher ActivityScenario test also now acts as a cold-start smoke test: if the provider throws before Activity creation, the test cannot reach its assertions.

The local validation contract is:

| Layer | Coverage |
|---|---|
| JVM | Runtime/tool logic and existing 300+ unit tests |
| Manifest merge | Debug and release manifests contain the Ads App ID metadata |
| Android instrumentation | Metadata assertion plus Creator Home/editor ActivityScenario startup |
| CI | API 30 and API 34 connected Android matrix |
