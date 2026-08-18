package pro.sketchware.creator;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorApplyResult;
import pro.sketchware.creator.runtime.CreatorCompatibilityReport;
import pro.sketchware.creator.runtime.CreatorCompatibilityTier;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectOperation;
import pro.sketchware.creator.runtime.CreatorRuntimeCompatibilityInspector;
import pro.sketchware.creator.runtime.CreatorRuntimeExecutor;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;
import pro.sketchware.creator.runtime.CreatorWidget;

/**
 * First functional Creator Runtime editor and preview surface.
 *
 * <p>All controls submit typed operations to {@link CreatorRuntimeSession}; no
 * UI control writes project data directly and no Save or Compile button exists
 * in the live edit path.
 */
public final class CreatorProjectActivity extends AppCompatActivity {
    private CreatorRuntimeSession session;
    private LinearLayout previewCanvas;
    private TextView revisionLabel;
    private MaterialButton entryControl;
    private CreatorShakeRecovery shakeRecovery;
    private final CreatorRuntimeExecutor runtimeExecutor = new CreatorRuntimeExecutor();
    private final CreatorRuntimeSession.Listener documentListener = document -> runOnUiThread(this::render);
    private static final String[] ENTRY_PLACEMENTS = {
            "bottom_end", "bottom_start", "top_end", "top_start", "center"
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creator_project);
        session = CreatorRuntimeSession.get(this);
        previewCanvas = findViewById(R.id.creator_preview_canvas);
        revisionLabel = findViewById(R.id.creator_revision_label);
        entryControl = findViewById(R.id.creator_project_entry_control);
        findViewById(R.id.creator_back).setOnClickListener(v -> finish());
        findViewById(R.id.creator_add_text).setOnClickListener(v -> addWidget("text", "New text"));
        findViewById(R.id.creator_add_button).setOnClickListener(v -> addWidget("button", "Button"));
        findViewById(R.id.creator_add_input).setOnClickListener(v -> addWidget("input", ""));
        findViewById(R.id.creator_add_toggle).setOnClickListener(v -> addWidget("switch", "Toggle"));
        findViewById(R.id.creator_checkpoint).setOnClickListener(v -> createCheckpoint());
        findViewById(R.id.creator_history).setOnClickListener(v -> showHistoryInspector());
        findViewById(R.id.creator_compatibility).setOnClickListener(v -> showCompatibilityInspector());
        entryControl.setOnClickListener(v -> editEntryControl());
        shakeRecovery = new CreatorShakeRecovery(this, this::showRecoverySheet);
        ensureStarterScreen();
        render();
        showRecoveryOnboardingOnce();
    }

    @Override protected void onResume() {
        super.onResume();
        session.addListener(documentListener);
        if (shakeRecovery != null) shakeRecovery.start();
        render();
    }

    @Override protected void onPause() {
        session.removeListener(documentListener);
        if (shakeRecovery != null) shakeRecovery.stop();
        super.onPause();
    }

    private void ensureStarterScreen() {
        CreatorProjectDocument document = session.getDocument();
        if (!document.getScreens().isEmpty()) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", "home");
        payload.put("route", "/");
        payload.put("rootWidgetId", "root_home");
        payload.put("rootWidgetType", "column");
        apply(CreatorProjectOperation.Type.SCREEN_CREATE, payload);
    }

    private void addWidget(String widgetType, String defaultText) {
        CreatorProjectDocument document = session.getDocument();
        if (document.getScreens().isEmpty()) return;
        String rootId = document.getScreens().get(document.getEntryScreenId()).getRootWidgetId();
        String widgetId = widgetType + "_" + (document.getRevision() + 1);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("text", defaultText);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("widgetId", widgetId);
        payload.put("widgetType", widgetType);
        payload.put("parentId", rootId);
        payload.put("properties", properties);
        apply(CreatorProjectOperation.Type.WIDGET_ADD, payload);
    }

    private void editEntryControl() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(24);
        form.setPadding(padding, 0, padding, 0);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(session.getDocument().getEntryControl().getLabel());
        input.setSelectAllOnFocus(true);
        input.setHint(R.string.creator_entry_control_label_hint);
        form.addView(input);
        TextView placementTitle = new TextView(this);
        placementTitle.setText(R.string.creator_entry_control_position);
        placementTitle.setTextSize(14);
        placementTitle.setPadding(0, dp(16), 0, dp(4));
        form.addView(placementTitle);
        RadioGroup placements = new RadioGroup(this);
        int selected = placementIndex(session.getDocument().getEntryControl().getPlacement());
        for (int i = 0; i < ENTRY_PLACEMENTS.length; i++) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setText(entryPlacementLabel(i));
            option.setTag(ENTRY_PLACEMENTS[i]);
            option.setChecked(i == selected);
            placements.addView(option);
        }
        form.addView(placements);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_entry_control_title)
                .setMessage(R.string.creator_entry_control_message)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.creator_apply, (dialog, which) -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("label", input.getText().toString().trim());
                    View selectedView = placements.findViewById(placements.getCheckedRadioButtonId());
                    if (selectedView != null) payload.put("placement", String.valueOf(selectedView.getTag()));
                    apply(CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE, payload);
                }).show();
    }

    private void createCheckpoint() {
        String name = "revision-" + session.getDocument().getRevision();
        session.getEngine().checkpoint(name);
        Toast.makeText(this, getString(R.string.creator_checkpoint_created, name), Toast.LENGTH_SHORT).show();
    }

    private void showHistoryInspector() {
        List<Long> revisions = session.getEngine().getRevisionStore().getAvailableRevisions();
        String[] labels = new String[revisions.size()];
        long currentRevision = session.getDocument().getRevision();
        for (int i = 0; i < revisions.size(); i++) {
            long revision = revisions.get(i);
            labels[i] = getString(revision == currentRevision
                    ? R.string.creator_history_current_item : R.string.creator_history_item, revision);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_history_title)
                .setMessage(getString(R.string.creator_history_message,
                        session.getEngine().getRevisionStore().getCheckpoints().size()))
                .setNegativeButton(android.R.string.cancel, null)
                .setItems(labels, (dialog, which) -> {
                    long targetRevision = revisions.get(which);
                    if (targetRevision == currentRevision) return;
                    confirmRestoreRevision(targetRevision);
                })
                .show();
    }

    private void confirmRestoreRevision(long targetRevision) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_restore_title)
                .setMessage(getString(R.string.creator_restore_message, targetRevision))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.creator_restore_apply, (dialog, which) -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("targetRevision", targetRevision);
                    apply(CreatorProjectOperation.Type.REVISION_RESTORE, payload);
                })
                .show();
    }

    private void showCompatibilityInspector() {
        CreatorCompatibilityReport report = CreatorRuntimeCompatibilityInspector.inspect(session.getDocument());
        StringBuilder message = new StringBuilder();
        appendCompatibilitySection(message, report, CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                getString(R.string.creator_compatibility_r1));
        appendCompatibilitySection(message, report, CreatorCompatibilityTier.R2_RUNTIME_PLUGIN,
                getString(R.string.creator_compatibility_r2));
        appendCompatibilitySection(message, report, CreatorCompatibilityTier.R3_NATIVE_FALLBACK,
                getString(R.string.creator_compatibility_r3));
        appendCompatibilitySection(message, report, CreatorCompatibilityTier.R0_UNSUPPORTED,
                getString(R.string.creator_compatibility_r0));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_compatibility_title)
                .setMessage(message.toString().trim())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void appendCompatibilitySection(StringBuilder target, CreatorCompatibilityReport report,
                                            CreatorCompatibilityTier tier, String title) {
        int count = report.count(tier);
        if (count == 0) return;
        target.append(title).append(" · ").append(count).append('\n');
        for (CreatorCompatibilityReport.Item item : report.getItems()) {
            if (item.getTier() == tier) {
                target.append("• ").append(item.getSourceId()).append(": ")
                        .append(item.getMessage()).append('\n');
            }
        }
        target.append('\n');
    }

    private void showRecoverySheet() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_recovery_title)
                .setMessage(R.string.creator_recovery_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.creator_recovery_reset, (dialog, which) -> resetEntryControl())
                .setPositiveButton(R.string.creator_recovery_home, (dialog, which) -> finish())
                .show();
    }

    private void resetEntryControl() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("visible", true);
        payload.put("label", "Continue");
        payload.put("placement", "bottom_end");
        apply(CreatorProjectOperation.Type.ENTRY_CONTROL_UPDATE, payload);
    }

    private void apply(CreatorProjectOperation.Type type, Map<String, Object> payload) {
        CreatorProjectDocument document = session.getDocument();
        String operationId = "user-" + UUID.randomUUID() + "-" + type.name();
        CreatorProjectOperation operation = new CreatorProjectOperation(operationId,
                document.getProjectId(), document.getRevision(), CreatorProjectOperation.ActorKind.USER,
                type, payload, System.currentTimeMillis());
        CreatorApplyResult result = session.apply(operation);
        if (!result.isApplied()) {
            Toast.makeText(this, result.getValidation().getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        render();
    }

    private void render() {
        CreatorProjectDocument document = session.getDocument();
        revisionLabel.setText(getString(R.string.creator_revision_label, document.getRevision()));
        entryControl.setText(document.getEntryControl().getLabel());
        entryControl.setVisibility(document.getEntryControl().isVisible() ? View.VISIBLE : View.INVISIBLE);
        applyEntryPlacement(document.getEntryControl().getPlacement());
        previewCanvas.removeAllViews();
        if (document.getScreens().isEmpty()) return;
        String rootId = document.getScreens().get(document.getEntryScreenId()).getRootWidgetId();
        View root = renderWidget(document, document.getWidgets().get(rootId));
        if (root != null) previewCanvas.addView(root);
    }

    private View renderWidget(CreatorProjectDocument document, CreatorWidget widget) {
        if (widget == null) return null;
        if ("text".equals(widget.getType())) {
            TextView text = new TextView(this);
            text.setText(propertyString(widget, "text", "Text"));
            text.setTextSize(propertyInt(widget, "textSize", 18));
            text.setPadding(dp(16), dp(12), dp(16), dp(12));
            return text;
        }
        if ("button".equals(widget.getType())) {
            MaterialButton button = new MaterialButton(this);
            button.setText(propertyString(widget, "text", "Button"));
            button.setOnClickListener(v -> dispatchRuntimeEvent(widget.getId(), "click"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(16), dp(8), dp(16), dp(8));
            button.setLayoutParams(params);
            return button;
        }
        if ("input".equals(widget.getType())) {
            EditText input = new EditText(this);
            input.setText(propertyString(widget, "text", ""));
            input.setHint(propertyString(widget, "hint", "Type here"));
            input.setSingleLine(propertyBoolean(widget, "singleLine", false));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(16), dp(8), dp(16), dp(8));
            input.setLayoutParams(params);
            return input;
        }
        if ("checkbox".equals(widget.getType())) {
            MaterialCheckBox checkbox = new MaterialCheckBox(this);
            checkbox.setText(propertyString(widget, "text", "Checkbox"));
            checkbox.setChecked(propertyBoolean(widget, "checked", false));
            checkbox.setOnCheckedChangeListener((button, checked) -> dispatchRuntimeEvent(widget.getId(), "change"));
            checkbox.setPadding(dp(12), dp(8), dp(12), dp(8));
            return checkbox;
        }
        if ("switch".equals(widget.getType())) {
            SwitchCompat toggle = new SwitchCompat(this);
            toggle.setText(propertyString(widget, "text", "Switch"));
            toggle.setChecked(propertyBoolean(widget, "checked", false));
            toggle.setOnCheckedChangeListener((button, checked) -> dispatchRuntimeEvent(widget.getId(), "change"));
            toggle.setPadding(dp(16), dp(8), dp(16), dp(8));
            return toggle;
        }
        if ("image".equals(widget.getType())) {
            ImageView image = new ImageView(this);
            image.setImageResource(R.drawable.ic_mtrl_image);
            image.setContentDescription(propertyString(widget, "contentDescription", "Image"));
            image.setPadding(dp(28), dp(28), dp(28), dp(28));
            return image;
        }
        if ("scroll".equals(widget.getType())) {
            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            LinearLayout childColumn = new LinearLayout(this);
            childColumn.setOrientation(LinearLayout.VERTICAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) childColumn.addView(child);
            }
            scroll.addView(childColumn);
            return scroll;
        }
        LinearLayout container = new LinearLayout(this);
        container.setOrientation("row".equals(widget.getType())
                || "horizontal".equals(propertyString(widget, "orientation", "vertical"))
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(propertyInt(widget, "padding", 0), propertyInt(widget, "padding", 0),
                propertyInt(widget, "padding", 0), propertyInt(widget, "padding", 0));
        for (String childId : widget.getChildren()) {
            View child = renderWidget(document, document.getWidgets().get(childId));
            if (child != null) container.addView(child);
        }
        return container;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String propertyString(CreatorWidget widget, String key, String fallback) {
        Object value = widget.getProperties().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int propertyInt(CreatorWidget widget, String key, int fallback) {
        Object value = widget.getProperties().get(key);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private boolean propertyBoolean(CreatorWidget widget, String key, boolean fallback) {
        Object value = widget.getProperties().get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private void dispatchRuntimeEvent(String widgetId, String eventName) {
        java.util.List<CreatorRuntimeExecutor.Effect> effects = runtimeExecutor.dispatch(session.getEngine(), widgetId, eventName);
        for (CreatorRuntimeExecutor.Effect effect : effects) {
            if ("message".equals(effect.getType())) Toast.makeText(this, effect.getValue(), Toast.LENGTH_SHORT).show();
            else if ("navigate".equals(effect.getType())) {
                Toast.makeText(this, getString(R.string.creator_navigation_effect, effect.getValue()), Toast.LENGTH_SHORT).show();
            }
        }
        render();
    }

    private void applyEntryPlacement(String placement) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) entryControl.getLayoutParams();
        if ("bottom_start".equals(placement)) params.gravity = Gravity.BOTTOM | Gravity.START;
        else if ("top_start".equals(placement)) params.gravity = Gravity.TOP | Gravity.START;
        else if ("top_end".equals(placement)) params.gravity = Gravity.TOP | Gravity.END;
        else if ("center".equals(placement)) params.gravity = Gravity.CENTER;
        else params.gravity = Gravity.BOTTOM | Gravity.END;
        entryControl.setLayoutParams(params);
    }

    private int placementIndex(String placement) {
        for (int i = 0; i < ENTRY_PLACEMENTS.length; i++) {
            if (ENTRY_PLACEMENTS[i].equals(placement)) return i;
        }
        return 0;
    }

    private String entryPlacementLabel(int index) {
        switch (index) {
            case 1: return getString(R.string.creator_position_bottom_start);
            case 2: return getString(R.string.creator_position_top_end);
            case 3: return getString(R.string.creator_position_top_start);
            case 4: return getString(R.string.creator_position_center);
            default: return getString(R.string.creator_position_bottom_end);
        }
    }

    private void showRecoveryOnboardingOnce() {
        if (getSharedPreferences("creator_runtime", MODE_PRIVATE).getBoolean("recovery_onboarded", false)) return;
        getSharedPreferences("creator_runtime", MODE_PRIVATE).edit().putBoolean("recovery_onboarded", true).apply();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.creator_recovery_onboarding_title)
                .setMessage(R.string.creator_recovery_onboarding_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
