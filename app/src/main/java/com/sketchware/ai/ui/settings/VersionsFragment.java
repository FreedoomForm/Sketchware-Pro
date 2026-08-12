package com.sketchware.ai.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.sketchware.ai.release.GitHubRelease;
import com.sketchware.ai.release.ReleasesAdapter;
import com.sketchware.ai.release.ReleasesFetcher;
import com.sketchware.ai.release.UpdateChecker;

import java.util.List;

import pro.sketchware.R;

/**
 * "Versions" fragment — lists all published GitHub Releases for the
 * Sketchware-Pro fork, with the latest release at the top.
 *
 * <p>Each card shows:
 * <ul>
 *   <li>Release name + tag</li>
 *   <li>Publication timestamp (relative)</li>
 *   <li>Markdown release notes (truncated to 8 lines)</li>
 *   <li>Download APK button (opens browser)</li>
 *   <li>INSTALLED badge on the release matching the running versionCode</li>
 *   <li>LATEST badge on the newest release</li>
 * </ul>
 *
 * <p>Loading happens on a background thread via {@link ReleasesFetcher}.
 * Network errors show a retry button instead of leaving the user with
 * an empty screen.
 */
public final class VersionsFragment extends Fragment {

    private ReleasesAdapter adapter;
    private ProgressBar     progress;
    private TextView        errorText;
    private MaterialButton  retryBtn;
    private RecyclerView    rv;
    private int             installedVersionCode = -1;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        installedVersionCode = UpdateChecker.getInstalledVersionCode(ctx);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_versions, container, false);

        rv        = root.findViewById(R.id.rv_releases);
        progress  = root.findViewById(R.id.progress);
        errorText = root.findViewById(R.id.tv_error);
        retryBtn  = root.findViewById(R.id.btn_retry);

        adapter = new ReleasesAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        retryBtn.setOnClickListener(v -> loadReleases());

        loadReleases();
        return root;
    }

    private void loadReleases() {
        progress.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        retryBtn.setVisibility(View.GONE);
        rv.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<GitHubRelease> releases = ReleasesFetcher.fetchReleases();
                int latestCode = UpdateChecker.pickLatestStable(releases) != null
                        ? UpdateChecker.pickLatestStable(releases).extractVersionCode() : -1;
                final int finalLatest = latestCode;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (releases.isEmpty()) {
                        errorText.setText(R.string.ai_versions_no_releases);
                        errorText.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setReleases(releases, installedVersionCode, finalLatest);
                        rv.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    errorText.setText(R.string.ai_versions_error);
                    errorText.setVisibility(View.VISIBLE);
                    retryBtn.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }
}
