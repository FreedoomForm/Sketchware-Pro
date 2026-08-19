package com.sketchware.ai.release;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;

/**
 * RecyclerView adapter that renders a list of {@link GitHubRelease} items
 * as cards. Each card shows the release name, publication date, release
 * notes (truncated), and a "Download APK" button that opens the APK
 * asset URL in the system browser (which kicks off a download).
 *
 * <p>The currently-installed release (if any) is badged "INSTALLED" and
 * the latest release is badged "LATEST". Both flags are computed by the
 * caller and passed in via {@link #setReleases(List, int, int)}.
 */
public final class ReleasesAdapter extends RecyclerView.Adapter<ReleasesAdapter.VH> {

    private final List<GitHubRelease> releases = new ArrayList<>();
    private int installedVersionCode = -1;
    private int latestVersionCode    = -1;

    /** Replace the current list and re-render. */
    public void setReleases(List<GitHubRelease> items, int installedCode, int latestCode) {
        releases.clear();
        if (items != null) releases.addAll(items);
        this.installedVersionCode = installedCode;
        this.latestVersionCode    = latestCode;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_release, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        GitHubRelease r = releases.get(position);
        Context ctx = h.itemView.getContext();

        h.name.setText(r.displayName());

        // Date
        String dateText = formatRelativeDate(ctx, r.published_at);
        if (dateText != null) {
            h.date.setVisibility(View.VISIBLE);
            h.date.setText(dateText);
        } else {
            h.date.setVisibility(View.GONE);
        }

        // Notes
        String notes = r.body != null ? r.body.trim() : "";
        if (notes.isEmpty()) {
            h.notes.setVisibility(View.GONE);
        } else {
            h.notes.setVisibility(View.VISIBLE);
            h.notes.setText(notes);
        }

        // Badge
        int code = r.extractVersionCode();
        boolean isInstalled = code > 0 && code == installedVersionCode;
        boolean isLatest    = position == 0 && latestVersionCode > 0 && code == latestVersionCode;
        if (isInstalled) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(ctx.getString(R.string.ai_versions_installed_badge));
        } else if (isLatest) {
            h.badge.setVisibility(View.VISIBLE);
            h.badge.setText(ctx.getString(R.string.ai_versions_latest_badge));
        } else {
            h.badge.setVisibility(View.GONE);
        }

        // Download button — opens the APK URL in the browser, which
        // triggers a download. We deliberately do NOT auto-install
        // because non-Play-Store apps cannot self-install without
        // REQUEST_INSTALL_PACKAGES + a system prompt.
        GitHubRelease.Asset apk = r.firstApkAsset();
        if (apk != null && apk.browser_download_url != null) {
            h.download.setVisibility(View.VISIBLE);
            h.download.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(apk.browser_download_url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(i); } catch (Exception ignored) {}
            });
        } else {
            h.download.setVisibility(View.GONE);
        }

        // Open the release page when the card itself is tapped.
        if (r.html_url != null) {
            h.card.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(r.html_url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(i); } catch (Exception ignored) {}
            });
        } else {
            h.card.setOnClickListener(null);
        }
    }

    @Override public int getItemCount() {
        return releases.size();
    }

    static String formatRelativeDate(Context ctx, String isoTs) {
        if (isoTs == null || isoTs.isEmpty()) return null;
        try {
            // Parse ISO-8601 like "2026-08-12T09:30:00Z"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(isoTs);
            if (d == null) return null;
            return DateUtils.getRelativeTimeSpanString(
                    d.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        } catch (Exception e) {
            return null;
        }
    }

    static final class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView name;
        final TextView date;
        final TextView notes;
        final TextView badge;
        final MaterialButton download;

        VH(@NonNull View v) {
            super(v);
            card     = v.findViewById(R.id.release_card);
            name     = v.findViewById(R.id.tv_release_name);
            date     = v.findViewById(R.id.tv_release_date);
            notes    = v.findViewById(R.id.tv_release_notes);
            badge    = v.findViewById(R.id.tv_release_badge);
            download = v.findViewById(R.id.btn_download);
        }
    }
}
