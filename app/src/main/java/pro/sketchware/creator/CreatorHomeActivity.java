package pro.sketchware.creator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import pro.sketchware.R;
import com.besome.sketch.design.DesignActivity;
import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/** White, runtime-first home for an editable Creator project. */
public final class CreatorHomeActivity extends AppCompatActivity {
    private CreatorRuntimeSession session;
    private TextView previewTitle;
    private TextView previewDetail;
    private FloatingActionButton entryControl;
    private boolean showLiveSurfaceAfterEditor;
    private final CreatorRuntimeSession.Listener documentListener = document -> runOnUiThread(() -> renderDocument(document));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creator_home);
        session = CreatorRuntimeSession.get(this);
        previewTitle = findViewById(R.id.creator_preview_title);
        previewDetail = findViewById(R.id.creator_preview_detail);
        entryControl = findViewById(R.id.creator_entry_control);
        entryControl.setOnClickListener(v -> openProject());
        findViewById(R.id.creator_home_body).setOnClickListener(v -> openProject());
    }

    @Override protected void onResume() {
        super.onResume();
        session.addListener(documentListener);
        renderDocument(session.getDocument());
        if (showLiveSurfaceAfterEditor) {
            showLiveSurfaceAfterEditor = false;
            startActivity(new Intent(this, CreatorProjectActivity.class)
                    .putExtra(CreatorProjectActivity.EXTRA_LIVE_ONLY, true));
        }
    }

    @Override protected void onPause() {
        session.removeListener(documentListener);
        super.onPause();
    }

    private void openProject() {
        showLiveSurfaceAfterEditor = true;
        CreatorProjectDocument document = session.getDocument();
        String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
        Intent intent = new Intent(this, DesignActivity.class)
                .putExtra("sc_id", legacyScId)
                .putExtra("creator_runtime_project_id", document.getProjectId());
        startActivity(intent);
    }

    private void renderDocument(CreatorProjectDocument document) {
        previewTitle.setText(document.getName());
        if (document.getScreens().isEmpty()) {
            previewDetail.setText(R.string.creator_home_empty_project);
        } else {
            previewDetail.setText(getString(R.string.creator_home_project_detail,
                    document.getRevision(), document.getScreens().size(), document.getWidgets().size()));
        }
        entryControl.setContentDescription(document.getEntryControl().getLabel());
        entryControl.setVisibility(document.getEntryControl().isVisible() ? View.VISIBLE : View.INVISIBLE);
        applyEntryPlacement(document.getEntryControl().getPlacement());
    }

    private void applyEntryPlacement(String placement) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) entryControl.getLayoutParams();
        if ("bottom_start".equals(placement)) params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
        else if ("top_start".equals(placement)) params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        else if ("top_end".equals(placement)) params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        else if ("center".equals(placement)) params.gravity = android.view.Gravity.CENTER;
        else params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        entryControl.setLayoutParams(params);
    }
}
