package com.sketchware.ai.release;

import android.content.Context;
import android.content.SharedPreferences;
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
 *
 * <p><b>Rate-limit guard:</b> the unauthenticated GitHub Releases endpoint
 * allows only 60 requests per hour per IP. Without throttling, a user who
 * opens the chat tab 60 times in an hour burns the entire budget and gets
 * rate-limited. {@link #checkAsync} persists a {@code last_check_ms}
 * timestamp and silently skips the network call if the previous check was
 * less than {@link #MIN_CHECK_INTERVAL_MS} ago (12 hours). The
 * {@link VersionsFragment} explicit "refresh" path uses
 * {@link #checkAsyncForce} to bypass the throttle.
 */
public final class UpdateChecker {

    /** Callbacks are always invoked on the main thread. */
    public interface Callback {
        void onUpdateAvailable(GitHubRelease latest);
        void onUpToDate();
        void onError(Exception error);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final String PREFS_NAME = "sketchware_ai_update_check";
    private static final String KEY_LAST_CHECK_MS = "last_check_ms";
    /** Minimum interval between automatic update checks (12 hours). */
    public static final long MIN_CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000;

    private UpdateChecker() {}

    /**
     * Trigger an asynchronous update check, subject to the 12-hour
     * rate-limit gate. If a check was performed recently, the callback is
     * invoked with {@link Callback#onUpToDate()} without hitting the network.
     */
    public static void checkAsync(Context context, Callback callback) {
        if (shouldThrottle(context)) {
            // Silently skip — the user opened the chat tab again within 12h.
            new Handler(Looper.getMainLooper()).post(callback::onUpToDate);
            return;
        }
        checkAsyncForce(context, callback);
    }

    /**
     * Force an update check that bypasses the rate-limit gate. Used by the
     * explicit "Refresh" action in {@code VersionsFragment} where the user
     * is asking for fresh data right now.
     */
    public static void checkAsyncForce(Context context, Callback callback) {
        final int installedCode = getInstalledVersionCode(context);
        final Handler ui = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            try {
                List<GitHubRelease> releases = ReleasesFetcher.fetchReleases();
                // Stamp the last-check timestamp regardless of outcome — a
                // failed fetch still counts against the rate-limit budget on
                // GitHub's side, so we should not immediately retry.
                stampLastCheck(context);
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

    /** Returns true if an update check was performed within the throttle window. */
    private static boolean shouldThrottle(Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long last = prefs.getLong(KEY_LAST_CHECK_MS, 0);
            if (last <= 0) return false;
            return (System.currentTimeMillis() - last) < MIN_CHECK_INTERVAL_MS;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void stampLastCheck(Context context) {
        try {
            context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                    .apply();
        } catch (Throwable t) {
            // Best-effort — throttle is an optimization, not a correctness requirement.
        }
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
