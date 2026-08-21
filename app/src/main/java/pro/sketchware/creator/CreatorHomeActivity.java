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
import pro.sketchware.creator.runtime.CreatorEventBinding;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeBlock;
import pro.sketchware.creator.runtime.CreatorRuntimeDefaults;
import pro.sketchware.creator.runtime.CreatorWidget;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/** White, runtime-first home for an editable Creator project. */
public final class CreatorHomeActivity extends AppCompatActivity {
    private CreatorRuntimeSession session;
    private MaterialButton entryControl;
    private CreatorShakeRecovery shakeRecovery;
    private boolean showLiveSurfaceAfterEditor;
    private final CreatorRuntimeSession.Listener documentListener = document -> runOnUiThread(() -> renderDocument(document));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creator_home);
        session = CreatorRuntimeSession.get(this);
        entryControl = findViewById(R.id.creator_entry_control);
        entryControl.setOnClickListener(v -> openProject());
        shakeRecovery = new CreatorShakeRecovery(this, this::openProject);
    }

    @Override protected void onResume() {
        super.onResume();
        session.addListener(documentListener);
        renderDocument(session.getDocument());
        if (shakeRecovery != null) shakeRecovery.start();
        if (showLiveSurfaceAfterEditor) {
            showLiveSurfaceAfterEditor = false;
            startActivity(new Intent(this, CreatorProjectActivity.class)
                    .putExtra(CreatorProjectActivity.EXTRA_LIVE_ONLY, true));
        }
    }

    @Override protected void onPause() {
        if (shakeRecovery != null) shakeRecovery.stop();
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
        CreatorWidget editorEntry = findEditorEntryWidget(document);
        boolean visible = editorEntry != null && propertyBoolean(editorEntry, "visible", true);
        String fallbackLabel = document.getEntryControl().getLabel();
        String label = editorEntry == null ? fallbackLabel : propertyString(editorEntry, "text", fallbackLabel);
        entryControl.setTag(editorEntry == null ? null : editorEntry.getId());
        entryControl.setText(label);
        entryControl.setContentDescription(label);
        entryControl.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        applyEntryPlacement(editorEntry, document.getEntryControl().getPlacement());
    }

    private CreatorWidget findEditorEntryWidget(CreatorProjectDocument document) {
        if (document == null) return null;
        for (CreatorEventBinding binding : document.getEvents().values()) {
            if (binding == null || !"click".equals(binding.getEventName())) continue;
            if (!document.getWidgets().containsKey(binding.getTargetWidgetId())) continue;
            if (containsEditorIntent(binding.getBlocks())) {
                return document.getWidgets().get(binding.getTargetWidgetId());
            }
        }
        return null;
    }

    private boolean containsEditorIntent(java.util.List<CreatorRuntimeBlock> blocks) {
        if (blocks == null) return false;
        for (CreatorRuntimeBlock block : blocks) {
            if (block == null) continue;
            if (block.getType() == CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL) {
                Object rawArguments = block.getPayload().get("arguments");
                if (rawArguments instanceof java.util.Map) {
                    java.util.Map<?, ?> arguments = (java.util.Map<?, ?>) rawArguments;
                    if ("open_creator_editor".equals(String.valueOf(arguments.get("action")))
                            || CreatorRuntimeDefaults.EDITOR_INTENT_ID.equals(String.valueOf(arguments.get("intentId")))) {
                        return true;
                    }
                }
            }
            if (containsEditorIntent(block.getThenBlocks()) || containsEditorIntent(block.getElseBlocks())) return true;
        }
        return false;
    }

    private static String propertyString(CreatorWidget widget, String key, String fallback) {
        Object value = widget.getProperties().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean propertyBoolean(CreatorWidget widget, String key, boolean fallback) {
        Object value = widget.getProperties().get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private void applyEntryPlacement(CreatorWidget widget, String fallbackPlacement) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) entryControl.getLayoutParams();
        String placement = fallbackPlacement;
        if (widget != null) {
            Object rawGravity = widget.getProperties().get("legacyLayoutGravity");
            if (rawGravity instanceof Number) {
                int gravity = android.view.Gravity.getAbsoluteGravity(((Number) rawGravity).intValue(),
                        entryControl.getLayoutDirection());
                int horizontal = gravity & android.view.Gravity.HORIZONTAL_GRAVITY_MASK;
                int vertical = gravity & android.view.Gravity.VERTICAL_GRAVITY_MASK;
                if (vertical == android.view.Gravity.TOP && horizontal == android.view.Gravity.LEFT) placement = "top_start";
                else if (vertical == android.view.Gravity.TOP && horizontal == android.view.Gravity.RIGHT) placement = "top_end";
                else if (vertical == android.view.Gravity.TOP) placement = "top_end";
                else if (horizontal == android.view.Gravity.LEFT) placement = "bottom_start";
                else if (horizontal == android.view.Gravity.RIGHT) placement = "bottom_end";
            }
        }
        if ("bottom_start".equals(placement)) params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
        else if ("top_start".equals(placement)) params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        else if ("top_end".equals(placement)) params.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        else if ("center".equals(placement)) params.gravity = android.view.Gravity.CENTER;
        else params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        entryControl.setLayoutParams(params);
    }
}
