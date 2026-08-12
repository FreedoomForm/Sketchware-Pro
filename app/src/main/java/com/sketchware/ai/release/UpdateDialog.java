package com.sketchware.ai.release;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import pro.sketchware.R;

/**
 * Modal dialog shown to the user when a newer GitHub Release is available.
 *
 * <p>Displays the installed and available version labels, a short snippet
 * of the release notes, and two buttons:
 * <ul>
 *   <li><b>Download</b> — opens the APK asset URL in the browser, which
 *       triggers a download. The user then taps the downloaded file to
 *       install (Android's package installer takes over).</li>
 *   <li><b>Later</b> — dismisses the dialog without action. The check is
 *       run again on the next app launch.</li>
 * </ul>
 *
 * <p>The dialog is dismissed automatically on configuration change —
 * the host activity is responsible for re-invoking
 * {@link UpdateChecker#checkAsync} to re-show it if still relevant.
 */
public class UpdateDialog extends DialogFragment {

    private static final String ARG_NAME    = "release_name";
    private static final String ARG_URL     = "apk_url";
    private static final String ARG_NOTES   = "notes";
    private static final String ARG_HTML    = "html_url";

    public static UpdateDialog newInstance(GitHubRelease latest, Context ctx) {
        UpdateDialog d = new UpdateDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, latest.displayName());
        GitHubRelease.Asset apk = latest.firstApkAsset();
        args.putString(ARG_URL, apk != null ? apk.browser_download_url : null);
        args.putString(ARG_HTML, latest.html_url);
        args.putString(ARG_NOTES, latest.body);
        d.setArguments(args);
        return d;
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = requireContext();
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        String releaseName = args.getString(ARG_NAME, "");
        String apkUrl      = args.getString(ARG_URL);
        String htmlUrl     = args.getString(ARG_HTML);
        String notes       = args.getString(ARG_NOTES);

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(R.string.ai_update_title);

        View body = LayoutInflater.from(ctx).inflate(R.layout.dialog_update, null);
        TextView subtitle  = body.findViewById(R.id.tv_subtitle);
        TextView avail     = body.findViewById(R.id.tv_available);
        TextView current   = body.findViewById(R.id.tv_current);
        TextView notesView = body.findViewById(R.id.tv_notes);

        subtitle.setText(R.string.ai_update_subtitle);
        avail.setText(getString(R.string.ai_update_new_version, releaseName));
        current.setText(getString(R.string.ai_update_current_version, getInstalledVersionName(ctx)));

        if (!TextUtils.isEmpty(notes)) {
            notesView.setText(notes);
            notesView.setVisibility(View.VISIBLE);
        } else {
            notesView.setVisibility(View.GONE);
        }

        b.setView(body);
        b.setPositiveButton(R.string.ai_update_download, (d, w) -> openUrl(apkUrl != null ? apkUrl : htmlUrl));
        b.setNegativeButton(R.string.ai_update_later, null);
        return b.create();
    }

    private void openUrl(String url) {
        if (url == null) return;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { requireContext().startActivity(i); } catch (Exception ignored) {}
    }

    private static String getInstalledVersionName(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "?";
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
