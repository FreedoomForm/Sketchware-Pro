package pro.sketchware.creator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.button.MaterialButton;

import pro.sketchware.R;
import com.besome.sketch.design.DesignActivity;
import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeDefaults;
import pro.sketchware.creator.runtime.CreatorWidget;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/** White, runtime-first home for an editable Creator project. */
public final class CreatorHomeActivity extends AppCompatActivity {
    private CreatorRuntimeSession session;
    private MaterialButton entryControl;
    private boolean showLiveSurfaceAfterEditor;
    private final CreatorRuntimeSession.Listener documentListener = document -> runOnUiThread(() -> renderDocument(document));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creator_home);
        session = CreatorRuntimeSession.get(this);
        entryControl = findViewById(R.id.creator_entry_control);
        entryControl.setOnClickListener(v -> openProject());
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
        CreatorWidget continueWidget = document.getWidgets().get(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        boolean visible = continueWidget != null && propertyBoolean(continueWidget, "visible", true);
        String label = continueWidget == null ? document.getEntryControl().getLabel()
                : propertyString(continueWidget, "text", document.getEntryControl().getLabel());
        entryControl.setText(label);
        entryControl.setContentDescription(label);
        entryControl.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        applyEntryPlacement(document.getEntryControl().getPlacement());
    }

    private static String propertyString(CreatorWidget widget, String key, String fallback) {
        Object value = widget.getProperties().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean propertyBoolean(CreatorWidget widget, String key, boolean fallback) {
        Object value = widget.getProperties().get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
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
