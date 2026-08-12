package com.sketchware.ai.release;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Checks whether a newer GitHub Release exists than the currently installed
 * build, and notifies the caller on the main thread.
 *
 * <p>Comparison is by {@code versionCode} — the release workflow stamps
 * each release's {@code body} with a {@code **versionCode:** N} line,
 * which {@link GitHubRelease#extractVersionCode()} parses. The currently
 * installed versionCode comes from {@link PackageManager}.
 *
 * <p>If the latest non-prerelease release has a higher versionCode than
 * the installed build, {@link Callback#onUpdateAvailable(GitHubRelease)}
 * is invoked with that release. Otherwise
 * {@link Callback#onUpToDate()} is invoked. Network errors surface as
 * {@link Callback#onError(Exception)}.
 *
 * <p>The check is fire-and-forget — invoke from a UI entry point
 * (e.g. {@code Activity.onResume}) and let the callback update the UI.
 * A single-threaded executor is used so concurrent checks don't pile up.
 */
public final class UpdateChecker {

    /** Callbacks are always invoked on the main thread. */
    public interface Callback {
        void onUpdateAvailable(GitHubRelease latest);
        void onUpToDate();
        void onError(Exception error);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private UpdateChecker() {}

    /**
     * Trigger an asynchronous update check.
     *
     * @param context  any context (application context is extracted internally)
     * @param callback invoked on the main thread
     */
    public static void checkAsync(Context context, Callback callback) {
        final int installedCode = getInstalledVersionCode(context);
        final Handler ui = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            try {
                List<GitHubRelease> releases = ReleasesFetcher.fetchReleases();
                GitHubRelease latest = pickLatestStable(releases);
                if (latest == null) {
                    ui.post(callback::onUpToDate);
                    return;
                }
                int latestCode = latest.extractVersionCode();
                if (latestCode > 0 && latestCode > installedCode) {
                    ui.post(() -> callback.onUpdateAvailable(latest));
                } else {
                    ui.post(callback::onUpToDate);
                }
            } catch (Exception e) {
                final Exception ex = e;
                ui.post(() -> callback.onError(ex));
            }
        });
    }

    /** Pick the newest non-prerelease release, or null if the list is empty. */
    public static GitHubRelease pickLatestStable(List<GitHubRelease> releases) {
        if (releases == null || releases.isEmpty()) return null;
        for (GitHubRelease r : releases) {
            // The GitHub API returns releases newest-first, so the first
            // non-prerelease entry is the latest stable.
            if (!r.prerelease) return r;
        }
        // Fall back to the newest release even if it's a prerelease — better
        // to surface an available update than to silently skip.
        return releases.get(0);
    }

    /** Read the installed app's versionCode from the PackageManager. */
    public static int getInstalledVersionCode(Context context) {
        try {
            PackageManager pm = context.getApplicationContext().getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen — we're querying our own package.
            return 0;
        }
    }
}
