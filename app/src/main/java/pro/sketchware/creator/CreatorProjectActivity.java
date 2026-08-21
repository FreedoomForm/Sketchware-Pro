package pro.sketchware.creator;

import android.content.Intent;
import android.net.Uri;
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

import com.google.android.gms.maps.MapView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.besome.sketch.help.ProgramInfoActivity;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.tools.NewKeyStoreActivity;
import mod.hilal.saif.activities.tools.AppSettings;
import pro.sketchware.R;
import pro.sketchware.activities.about.AboutActivity;
import pro.sketchware.creator.runtime.CreatorApplyResult;
import pro.sketchware.creator.runtime.CreatorCompatibilityReport;
import pro.sketchware.creator.runtime.CreatorCompatibilityTier;
import pro.sketchware.creator.runtime.CreatorDrawerService;
import pro.sketchware.creator.runtime.CreatorEventBinding;
import pro.sketchware.creator.runtime.CreatorLegacyArtifactImporter;
import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorMapService;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectOperation;
import pro.sketchware.creator.runtime.CreatorRuntimeCompatibilityInspector;
import pro.sketchware.creator.runtime.CreatorRuntimeEnvironment;
import pro.sketchware.creator.runtime.CreatorRuntimeExecutor;
import pro.sketchware.creator.runtime.CreatorRuntimeService;
import pro.sketchware.creator.runtime.CreatorRuntimeServiceDispatcher;
import pro.sketchware.creator.runtime.CreatorRuntimeResourceResolver;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;
import pro.sketchware.creator.runtime.CreatorRuntimeServices;
import pro.sketchware.creator.runtime.CreatorWidget;

/**
 * Creator Runtime editor and live native application surface.
 *
 * <p>All controls submit typed operations to {@link CreatorRuntimeSession}; no
 * UI control writes project data directly and no Save or Compile button exists
 * in the live edit path.
 */
public final class CreatorProjectActivity extends AppCompatActivity {
    public static final String EXTRA_LIVE_ONLY = "creator_live_only";
    private CreatorRuntimeSession session;
    private boolean liveOnly;
    private LinearLayout previewCanvas;
    private TextView revisionLabel;
    private CreatorShakeRecovery shakeRecovery;
    private CreatorRuntimeEnvironment runtimeEnvironment;
    private CreatorRuntimeExecutor runtimeExecutor;
    private CreatorRuntimeServiceDispatcher runtimeServices;
    private String activeScreenId;
    private final Map<String, MapView> liveMapViews = new LinkedHashMap<>();
    private DrawerLayout editorDrawer;
    private DrawerLayout liveDrawerLayout;
    private boolean rendering;
    private boolean renderPending;
    private final CreatorRuntimeSession.Listener documentListener = document -> runOnUiThread(this::render);

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creator_project);
        liveOnly = getIntent().getBooleanExtra(EXTRA_LIVE_ONLY, false);
        configurePresentationMode();
        session = CreatorRuntimeSession.get(this);
        runtimeEnvironment = new CreatorRuntimeEnvironment(this, (serviceId, eventName, payload) ->
                runOnUiThread(() -> handleRuntimeServiceEvent(serviceId, eventName, payload)));
        previewCanvas = findViewById(R.id.creator_preview_canvas);
        revisionLabel = findViewById(R.id.creator_revision_label);
        editorDrawer = findViewById(R.id.creator_project_drawer);
        wireEditorSidebar();
        runtimeServices = CreatorRuntimeServices.defaults(this,
                session.getDocument().getProjectId(), runtimeEnvironment,
                timerId -> runtimeEnvironment.publish("timer", "tick",
                        java.util.Collections.<String, Object>singletonMap("timerId", timerId)));
        runtimeExecutor = new CreatorRuntimeExecutor(runtimeServices, session);
        findViewById(R.id.creator_back).setOnClickListener(v -> leaveEditorToLiveSurface());
        findViewById(R.id.creator_add_text).setOnClickListener(v -> addWidget("text", "New text"));
        findViewById(R.id.creator_add_button).setOnClickListener(v -> addWidget("button", "Button"));
        findViewById(R.id.creator_add_input).setOnClickListener(v -> addWidget("input", ""));
        findViewById(R.id.creator_add_toggle).setOnClickListener(v -> addWidget("switch", "Toggle"));
        findViewById(R.id.creator_checkpoint).setOnClickListener(v -> createCheckpoint());
        findViewById(R.id.creator_history).setOnClickListener(v -> showHistoryInspector());
        findViewById(R.id.creator_compatibility).setOnClickListener(v -> showCompatibilityInspector());
        shakeRecovery = new CreatorShakeRecovery(this, () -> {
            if (liveOnly) openEditor();
        });
        ensureStarterScreen();
        render();
        dispatchLifecycleEvent("create");
    }

    @Override protected void onResume() {
        super.onResume();
        session.addListener(documentListener);
        for (MapView map : liveMapViews.values()) map.onResume();
        if (shakeRecovery != null) shakeRecovery.start();
        render();
        dispatchLifecycleEvent("resume");
    }

    @Override protected void onPause() {
        dispatchLifecycleEvent("pause");
        for (MapView map : liveMapViews.values()) map.onPause();
        session.removeListener(documentListener);
        if (shakeRecovery != null) shakeRecovery.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        dispatchLifecycleEvent("destroy");
        disposeMapViews();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (editorDrawer != null && editorDrawer.isDrawerOpen(GravityCompat.START)) {
            editorDrawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (liveDrawerLayout != null && liveDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            liveDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (liveOnly) {
            finish();
        } else {
            leaveEditorToLiveSurface();
        }
    }

    private void wireEditorSidebar() {
        View menu = findViewById(R.id.creator_sidebar_open);
        if (menu != null) menu.setOnClickListener(v -> {
            if (editorDrawer != null) editorDrawer.openDrawer(GravityCompat.END);
        });
        bindSidebar(R.id.creator_sidebar_about, v -> openActivity(AboutActivity.class));
        bindSidebar(R.id.creator_sidebar_changelog, v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            intent.putExtra("select", "changelog");
            startActivity(intent);
        });
        bindSidebar(R.id.creator_sidebar_info, v -> startActivityForResult(
                new Intent(this, ProgramInfoActivity.class), 105));
        bindSidebar(R.id.creator_sidebar_keystore, v -> openActivity(NewKeyStoreActivity.class));
        bindSidebar(R.id.creator_sidebar_settings, v -> openActivity(AppSettings.class));
        bindSidebar(R.id.creator_sidebar_swassist, v -> openExternal(R.string.link_sw_assist));
        bindSidebar(R.id.creator_sidebar_discord, v -> openExternal(R.string.link_discord_invite));
        bindSidebar(R.id.creator_sidebar_telegram, v -> openExternal(R.string.link_telegram_invite));
        bindSidebar(R.id.creator_sidebar_github, v -> openExternal(R.string.link_github_url));
    }

    private void bindSidebar(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(listener);
    }

    private void closeEditorSidebar() {
        if (editorDrawer != null) editorDrawer.closeDrawer(GravityCompat.END);
    }

    private void openActivity(Class<?> type) {
        closeEditorSidebar();
        startActivity(new Intent(this, type));
    }

    private void openExternal(int urlRes) {
        closeEditorSidebar();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlRes))));
        } catch (android.content.ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.creator_external_link_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void configurePresentationMode() {
        View header = findViewById(R.id.creator_editor_header);
        View controls = findViewById(R.id.creator_editor_controls);
        if (header != null) header.setVisibility(liveOnly ? View.GONE : View.VISIBLE);
        if (controls != null) controls.setVisibility(liveOnly ? View.GONE : View.VISIBLE);
    }

    private void leaveEditorToLiveSurface() {
        if (liveOnly) {
            finish();
            return;
        }
        startActivity(new android.content.Intent(this, CreatorProjectActivity.class)
                .putExtra(EXTRA_LIVE_ONLY, true));
        finish();
    }

    private void openEditor() {
        if (liveOnly) {
            CreatorProjectDocument document = session.getDocument();
            String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
            Intent intent = new Intent(this, DesignActivity.class)
                    .putExtra("sc_id", legacyScId)
                    .putExtra("creator_runtime_project_id", document.getProjectId());
            // Keep the live surface underneath the original editor so Back
            // returns directly to the updated runtime application.
            startActivity(intent);
            return;
        }
        startActivity(new Intent(this, CreatorProjectActivity.class));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (runtimeEnvironment != null && runtimeEnvironment.handleActivityResult(requestCode, resultCode, data)) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (runtimeEnvironment != null && runtimeEnvironment.handlePermissionResult(requestCode, permissions, grantResults)) return;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        if (rendering) {
            renderPending = true;
            return;
        }
        rendering = true;
        try {
            renderNow();
        } finally {
            rendering = false;
            if (renderPending) {
                renderPending = false;
                render();
            }
        }
    }

    private void renderNow() {
        CreatorProjectDocument document = session.getDocument();
        boolean drawerWasOpen = liveDrawerLayout != null
                && liveDrawerLayout.isDrawerOpen(GravityCompat.START);
        runtimeEnvironment.clearWidgets();
        disposeMapViews();
        disposeDrawerLayout();
        revisionLabel.setText(getString(R.string.creator_revision_label, document.getRevision()));
        previewCanvas.removeAllViews();
        if (document.getScreens().isEmpty()) return;
        String screenId = activeScreenId != null && document.getScreens().containsKey(activeScreenId)
                ? activeScreenId : document.getEntryScreenId();
        String rootId = document.getScreens().get(screenId).getRootWidgetId();
        View root = renderWidget(document, document.getWidgets().get(rootId));
        if (root != null) {
            View shell = renderScreenShell(document, screenId, root);
            if (shell instanceof DrawerLayout) {
                previewCanvas.addView(shell, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(600)));
            } else {
                previewCanvas.addView(shell);
            }
            if (drawerWasOpen && liveDrawerLayout != null) {
                liveDrawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }

    private View renderScreenShell(CreatorProjectDocument document, String screenId, View mainContent) {
        if (!hasDrawer(document, screenId)) return mainContent;
        DrawerLayout drawer = new DrawerLayout(this);
        drawer.addView(mainContent, new DrawerLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout drawerPane = new LinearLayout(this);
        drawerPane.setOrientation(LinearLayout.VERTICAL);
        drawerPane.setBackgroundColor(0xFFFFFFFF);
        String drawerScreenId = "_drawer_" + screenId;
        if (document.getScreens().containsKey(drawerScreenId)) {
            View drawerContent = renderWidget(document,
                    document.getWidgets().get(document.getScreens().get(drawerScreenId).getRootWidgetId()));
            if (drawerContent != null) drawerPane.addView(drawerContent);
        }
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(dp(304),
                ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.START);
        drawer.addView(drawerPane, drawerParams);
        liveDrawerLayout = drawer;
        CreatorRuntimeService service = runtimeServices == null ? null : runtimeServices.registered().get("drawer");
        if (service instanceof CreatorDrawerService) ((CreatorDrawerService) service).register(drawer);
        return drawer;
    }

    @SuppressWarnings("unchecked")
    private boolean hasDrawer(CreatorProjectDocument document, String screenId) {
        Object rawIndex = document.getState().get("legacy.projectFileIndex");
        if (!(rawIndex instanceof Map)) return false;
        Object rawDescriptor = ((Map<?, ?>) rawIndex).get(screenId);
        if (!(rawDescriptor instanceof Map)) return false;
        Object enabled = ((Map<String, Object>) rawDescriptor).get("hasDrawer");
        return enabled instanceof Boolean && (Boolean) enabled;
    }

    private void disposeDrawerLayout() {
        liveDrawerLayout = null;
        CreatorRuntimeService service = runtimeServices == null ? null : runtimeServices.registered().get("drawer");
        if (service instanceof CreatorDrawerService) ((CreatorDrawerService) service).clear();
    }

    private void disposeMapViews() {
        for (MapView map : liveMapViews.values()) {
            map.onPause();
            map.onDestroy();
        }
        liveMapViews.clear();
    }

    private View renderWidget(CreatorProjectDocument document, CreatorWidget widget) {
        if (widget == null) return null;
        if ("text".equals(widget.getType())) {
            TextView text = new TextView(this);
            text.setText(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "text", "Text"), resourceVariant()));
            text.setTextSize(propertyInt(widget, "textSize", 18));
            text.setPadding(dp(16), dp(12), dp(16), dp(12));
            return registerRuntimeWidget(widget, text);
        }
        if ("button".equals(widget.getType())) {
            MaterialButton button = new MaterialButton(this);
            button.setTag(widget.getId());
            button.setText(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "text", "Button"), resourceVariant()));
            button.setOnClickListener(v -> dispatchRuntimeEvent(widget.getId(), "click"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(16), dp(8), dp(16), dp(8));
            button.setLayoutParams(params);
            return registerRuntimeWidget(widget, button);
        }
        if ("input".equals(widget.getType())) {
            EditText input = new EditText(this);
            input.setText(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "text", ""), resourceVariant()));
            input.setHint(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "hint", "Type here"), resourceVariant()));
            input.setSingleLine(propertyBoolean(widget, "singleLine", false));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(16), dp(8), dp(16), dp(8));
            input.setLayoutParams(params);
            return registerRuntimeWidget(widget, input);
        }
        if ("checkbox".equals(widget.getType())) {
            MaterialCheckBox checkbox = new MaterialCheckBox(this);
            checkbox.setText(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "text", "Checkbox"), resourceVariant()));
            checkbox.setChecked(propertyBoolean(widget, "checked", false));
            checkbox.setOnCheckedChangeListener((button, checked) -> dispatchRuntimeEvent(widget.getId(), "change"));
            checkbox.setPadding(dp(12), dp(8), dp(12), dp(8));
            return registerRuntimeWidget(widget, checkbox);
        }
        if ("switch".equals(widget.getType())) {
            SwitchCompat toggle = new SwitchCompat(this);
            toggle.setText(pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveString(document,
                    propertyString(widget, "text", "Switch"), resourceVariant()));
            toggle.setChecked(propertyBoolean(widget, "checked", false));
            toggle.setOnCheckedChangeListener((button, checked) -> dispatchRuntimeEvent(widget.getId(), "change"));
            toggle.setPadding(dp(16), dp(8), dp(16), dp(8));
            return registerRuntimeWidget(widget, toggle);
        }
        if ("image".equals(widget.getType())) {
            ImageView image = new ImageView(this);
            applyImageProperties(image, document, widget);
            image.setContentDescription(propertyString(widget, "contentDescription", "Image"));
            image.setPadding(dp(28), dp(28), dp(28), dp(28));
            return registerRuntimeWidget(widget, image);
        }
        if ("pager".equals(widget.getType())) {
            ViewPager pager = new ViewPager(this);
            pager.setId(View.generateViewId());
            java.util.List<?> customItems = runtimeItems(document, widget);
            pager.setAdapter(new PagerAdapter() {
                @Override public int getCount() { return customItems.isEmpty() ? widget.getChildren().size() : customItems.size(); }
                @Override public boolean isViewFromObject(View view, Object object) { return view == object; }
                @Override public Object instantiateItem(ViewGroup parent, int position) {
                    View child;
                    if (!customItems.isEmpty()) {
                        TextView row = new TextView(CreatorProjectActivity.this);
                        row.setText(runtimeItemText(customItems.get(position)));
                        row.setPadding(dp(16), dp(12), dp(16), dp(12));
                        child = row;
                    } else {
                        String childId = widget.getChildren().get(position);
                        child = renderWidget(document, document.getWidgets().get(childId));
                    }
                    if (child == null) child = new View(CreatorProjectActivity.this);
                    parent.addView(child);
                    return child;
                }
                @Override public void destroyItem(ViewGroup parent, int position, Object object) {
                    parent.removeView((View) object);
                }
            });
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override public void onPageSelected(int position) {
                    dispatchRuntimeEvent(widget.getId(), "page_selected");
                }
            });
            return registerRuntimeWidget(widget, pager);
        }
        if ("hscroll".equals(widget.getType())) {
            android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
            LinearLayout childRow = new LinearLayout(this);
            childRow.setOrientation(LinearLayout.HORIZONTAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) childRow.addView(child);
            }
            scroll.addView(childRow);
            return registerRuntimeWidget(widget, scroll);
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
            return registerRuntimeWidget(widget, scroll);
        }
        if ("progress".equals(widget.getType())) {
            android.widget.ProgressBar progress = new android.widget.ProgressBar(this, null,
                    android.R.attr.progressBarStyleHorizontal);
            progress.setMax(Math.max(1, propertyInt(widget, "max", 100)));
            progress.setProgress(Math.max(0, propertyInt(widget, "progress", 0)));
            return registerRuntimeWidget(widget, progress);
        }
        if ("spinner".equals(widget.getType())) {
            android.widget.Spinner spinner = new android.widget.Spinner(this);
            java.util.List<String> entries = new java.util.ArrayList<>();
            for (Object item : runtimeItems(document, widget)) entries.add(runtimeItemText(item));
            if (entries.isEmpty()) entries.add(propertyString(widget, "text", "Item"));
            spinner.setAdapter(new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, entries));
            final boolean[] initializing = {true};
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (!initializing[0]) dispatchRuntimeEvent(widget.getId(), "item_selected");
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            initializing[0] = false;
            return registerRuntimeWidget(widget, spinner);
        }
        if ("slider".equals(widget.getType())) {
            android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
            seekBar.setMax(Math.max(1, propertyInt(widget, "max", 100)));
            seekBar.setProgress(Math.max(0, propertyInt(widget, "progress", 0)));
            applySeekBarResources(seekBar, document, widget);
            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(android.widget.SeekBar view, int progress, boolean fromUser) {
                    if (fromUser) dispatchRuntimeEvent(widget.getId(), "change");
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar view) { }
                @Override public void onStopTrackingTouch(android.widget.SeekBar view) { }
            });
            return registerRuntimeWidget(widget, seekBar);
        }
        if ("calendar_view".equals(widget.getType())) {
            android.widget.CalendarView calendar = new android.widget.CalendarView(this);
            calendar.setOnDateChangeListener((view, year, month, day) -> dispatchRuntimeEvent(widget.getId(), "date_selected"));
            return registerRuntimeWidget(widget, calendar);
        }
        if ("fab".equals(widget.getType())) {
            FloatingActionButton fab = new FloatingActionButton(this);
            fab.setImageResource(android.R.drawable.ic_input_add);
            fab.setContentDescription(propertyString(widget, "contentDescription", "Action"));
            fab.setOnClickListener(view -> dispatchRuntimeEvent(widget.getId(), "click"));
            return registerRuntimeWidget(widget, fab);
        }
        if ("radio".equals(widget.getType())) {
            RadioButton radio = new RadioButton(this);
            radio.setText(propertyString(widget, "text", "Option"));
            radio.setChecked(propertyBoolean(widget, "checked", false));
            radio.setOnCheckedChangeListener((button, checked) -> dispatchRuntimeEvent(widget.getId(), "change"));
            return registerRuntimeWidget(widget, radio);
        }
        if ("rating".equals(widget.getType())) {
            android.widget.RatingBar rating = new android.widget.RatingBar(this);
            rating.setNumStars(Math.max(1, propertyInt(widget, "max", 5)));
            rating.setRating(Math.max(0, propertyInt(widget, "progress", 0)));
            rating.setOnRatingBarChangeListener((bar, value, fromUser) -> {
                if (fromUser) dispatchRuntimeEvent(widget.getId(), "change");
            });
            return registerRuntimeWidget(widget, rating);
        }
        if ("search".equals(widget.getType())) {
            androidx.appcompat.widget.SearchView search = new androidx.appcompat.widget.SearchView(this);
            search.setQueryHint(propertyString(widget, "hint", "Search"));
            String initialQuery = propertyString(widget, "query", "");
            if (!initialQuery.isEmpty()) search.setQuery(initialQuery, false);
            search.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) { dispatchRuntimeEvent(widget.getId(), "submit"); return false; }
                @Override public boolean onQueryTextChange(String value) { dispatchRuntimeEvent(widget.getId(), "change"); return false; }
            });
            return registerRuntimeWidget(widget, search);
        }
        if ("autocomplete".equals(widget.getType())) {
            android.widget.AutoCompleteTextView input = new android.widget.AutoCompleteTextView(this);
            input.setText(propertyString(widget, "text", ""));
            input.setHint(propertyString(widget, "hint", "Type here"));
            java.util.List<String> suggestions = new java.util.ArrayList<>();
            for (Object item : runtimeItems(document, widget)) suggestions.add(runtimeItemText(item));
            input.setAdapter(new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, suggestions));
            input.setThreshold(Math.max(0, propertyInt(widget, "threshold", 1)));
            input.setOnItemClickListener((parent, view, position, id) -> dispatchRuntimeEvent(widget.getId(), "item_selected"));
            return registerRuntimeWidget(widget, input);
        }
        if ("list".equals(widget.getType())) {
            android.widget.ListView list = new android.widget.ListView(this);
            java.util.List<String> entries = new java.util.ArrayList<>();
            for (Object item : runtimeItems(document, widget)) entries.add(runtimeItemText(item));
            if (entries.isEmpty()) entries.add(propertyString(widget, "text", "Item"));
            int choiceMode = propertyInt(widget, "choiceMode", android.widget.ListView.CHOICE_MODE_NONE);
            list.setChoiceMode(choiceMode);
            int rowLayout = choiceMode == android.widget.ListView.CHOICE_MODE_MULTIPLE
                    ? android.R.layout.simple_list_item_multiple_choice
                    : choiceMode == android.widget.ListView.CHOICE_MODE_SINGLE
                    ? android.R.layout.simple_list_item_single_choice : android.R.layout.simple_list_item_1;
            list.setAdapter(new android.widget.ArrayAdapter<>(this, rowLayout, entries));
            // ListView does not have a stable WRAP_CONTENT measurement inside the
            // live preview ScrollView. Bound it to the visible rows so measurement
            // cannot request a second layout pass forever on the emulator.
            int rowHeight = dp(48);
            int visibleRows = Math.max(1, Math.min(entries.size(), 6));
            list.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowHeight * visibleRows));
            list.setNestedScrollingEnabled(false);
            list.setOnItemClickListener((parent, view, position, id) -> dispatchRuntimeEvent(widget.getId(), "item_click"));
            return registerRuntimeWidget(widget, list);
        }
        if ("grid".equals(widget.getType())) {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation("grid".equals(widget.getType()) ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            java.util.List<?> items = runtimeItems(document, widget);
            if (items.isEmpty()) items = java.util.Collections.singletonList(propertyString(widget, "text", "Item"));
            for (Object item : items) {
                TextView row = new TextView(this);
                row.setText(runtimeItemText(item));
                row.setPadding(dp(12), dp(10), dp(12), dp(10));
                row.setOnClickListener(view -> dispatchRuntimeEvent(widget.getId(), "item_click"));
                list.addView(row);
            }
            return registerRuntimeWidget(widget, list);
        }
        if ("clock".equals(widget.getType())) {
            android.widget.TextClock clock = new android.widget.TextClock(this);
            clock.setFormat12Hour(propertyString(widget, "format12", "h:mm a"));
            clock.setFormat24Hour(propertyString(widget, "format24", "HH:mm"));
            return registerRuntimeWidget(widget, clock);
        }
        if ("date_picker".equals(widget.getType())) {
            android.widget.DatePicker picker = new android.widget.DatePicker(this);
            final boolean[] initializing = {true};
            picker.init(picker.getYear(), picker.getMonth(), picker.getDayOfMonth(),
                    (view, year, month, day) -> {
                        if (!initializing[0]) dispatchRuntimeEvent(widget.getId(), "date_selected");
                    });
            initializing[0] = false;
            return registerRuntimeWidget(widget, picker);
        }
        if ("time_picker".equals(widget.getType())) {
            android.widget.TimePicker picker = new android.widget.TimePicker(this);
            picker.setOnTimeChangedListener((view, hour, minute) -> dispatchRuntimeEvent(widget.getId(), "time_selected"));
            return registerRuntimeWidget(widget, picker);
        }
        if ("web".equals(widget.getType())) {
            android.webkit.WebView web = new android.webkit.WebView(this);
            web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            web.setWebViewClient(new android.webkit.WebViewClient() {
                @Override public boolean onRenderProcessGone(android.webkit.WebView view,
                        android.webkit.RenderProcessGoneDetail detail) {
                    android.view.ViewParent parent = view.getParent();
                    if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(view);
                    view.destroy();
                    return true;
                }
            });
            web.getSettings().setJavaScriptEnabled(true);
            String url = propertyString(widget, "url", propertyString(widget, "text", "about:blank"));
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "about:blank";
            web.loadUrl(url);
            return registerRuntimeWidget(widget, web);
        }
        if ("map".equals(widget.getType())) {
            MapView map = new MapView(this);
            map.onCreate(null);
            map.onResume();
            liveMapViews.put(widget.getId(), map);
            CreatorRuntimeService service = runtimeServices == null ? null : runtimeServices.registered().get("map");
            if (service instanceof CreatorMapService) ((CreatorMapService) service).register(widget.getId(), map);
            return registerRuntimeWidget(widget, map);
        }
        if ("video".equals(widget.getType())) {
            android.widget.VideoView video = new android.widget.VideoView(this);
            String source = propertyString(widget, "url", propertyString(widget, "text", ""));
            if (!source.isEmpty()) video.setVideoURI(android.net.Uri.parse(source));
            video.setOnCompletionListener(player -> dispatchRuntimeEvent(widget.getId(), "completed"));
            return registerRuntimeWidget(widget, video);
        }
        if ("lottie".equals(widget.getType())) {
            com.airbnb.lottie.LottieAnimationView animation = new com.airbnb.lottie.LottieAnimationView(this);
            String source = propertyString(widget, "url", propertyString(widget, "text", ""));
            if (!source.isEmpty()) animation.setAnimationFromUrl(source);
            animation.setRepeatCount(propertyBoolean(widget, "loop", true) ? com.airbnb.lottie.LottieDrawable.INFINITE : 0);
            animation.playAnimation();
            return registerRuntimeWidget(widget, animation);
        }
        if ("ad_banner".equals(widget.getType())) {
            com.google.android.gms.ads.AdView ad = new com.google.android.gms.ads.AdView(this);
            String unitId = propertyString(widget, "adUnitId", "");
            if (!unitId.isEmpty()) {
                ad.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
                ad.setAdUnitId(unitId);
                ad.loadAd(new com.google.android.gms.ads.AdRequest.Builder().build());
            }
            return registerRuntimeWidget(widget, ad);
        }
        if ("tabs".equals(widget.getType())) {
            com.google.android.material.tabs.TabLayout tabs = new com.google.android.material.tabs.TabLayout(this);
            java.util.List<?> items = widget.getProperties().get("items") instanceof java.util.List
                    ? (java.util.List<?>) widget.getProperties().get("items")
                    : java.util.Collections.singletonList(propertyString(widget, "text", "Tab"));
            for (Object item : items) tabs.addTab(tabs.newTab().setText(String.valueOf(item)));
            tabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) { dispatchRuntimeEvent(widget.getId(), "tab_selected"); }
                @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
                @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
            });
            return registerRuntimeWidget(widget, tabs);
        }
        if ("bottom_navigation".equals(widget.getType())) {
            com.google.android.material.bottomnavigation.BottomNavigationView navigation =
                    new com.google.android.material.bottomnavigation.BottomNavigationView(this);
            java.util.List<?> items = widget.getProperties().get("items") instanceof java.util.List
                    ? (java.util.List<?>) widget.getProperties().get("items")
                    : java.util.Collections.singletonList(propertyString(widget, "text", "Home"));
            int id = 1;
            for (Object item : items) navigation.getMenu().add(0, id++, 0, String.valueOf(item))
                    .setIcon(android.R.drawable.ic_menu_view);
            navigation.setOnItemSelectedListener(item -> { dispatchRuntimeEvent(widget.getId(), "item_selected"); return true; });
            return registerRuntimeWidget(widget, navigation);
        }
        if ("badge".equals(widget.getType())) {
            TextView badge = new TextView(this);
            badge.setText(propertyString(widget, "text", "0"));
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(8), dp(4), dp(8), dp(4));
            badge.setBackgroundColor(0xFFE0E0E0);
            return registerRuntimeWidget(widget, badge);
        }
        if ("pattern".equals(widget.getType())) {
            android.widget.GridLayout pattern = new android.widget.GridLayout(this);
            pattern.setColumnCount(3);
            for (int i = 0; i < 9; i++) {
                MaterialButton dot = new MaterialButton(this);
                dot.setText("•");
                dot.setOnClickListener(view -> dispatchRuntimeEvent(widget.getId(), "pattern_changed"));
                pattern.addView(dot, new android.widget.GridLayout.LayoutParams());
            }
            return registerRuntimeWidget(widget, pattern);
        }
        if ("sidebar".equals(widget.getType())) {
            LinearLayout sidebar = new LinearLayout(this);
            sidebar.setOrientation(LinearLayout.VERTICAL);
            for (char letter = 'A'; letter <= 'Z'; letter++) {
                TextView index = new TextView(this);
                index.setText(String.valueOf(letter));
                index.setGravity(Gravity.CENTER);
                index.setOnClickListener(view -> dispatchRuntimeEvent(widget.getId(), "letter_selected"));
                sidebar.addView(index);
            }
            return registerRuntimeWidget(widget, sidebar);
        }
        if ("card".equals(widget.getType())) {
            androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
            LinearLayout childColumn = new LinearLayout(this);
            childColumn.setOrientation(LinearLayout.VERTICAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) childColumn.addView(child);
            }
            card.addView(childColumn);
            return registerRuntimeWidget(widget, card);
        }
        if ("collapsing".equals(widget.getType())) {
            com.google.android.material.appbar.AppBarLayout appBar = new com.google.android.material.appbar.AppBarLayout(this);
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) content.addView(child);
            }
            appBar.addView(content);
            return registerRuntimeWidget(widget, appBar);
        }
        if ("text_input".equals(widget.getType())) {
            com.google.android.material.textfield.TextInputLayout layout = new com.google.android.material.textfield.TextInputLayout(this);
            EditText input = new EditText(this);
            input.setText(propertyString(widget, "text", ""));
            layout.setHint(propertyString(widget, "hint", "Input"));
            layout.addView(input);
            input.setOnFocusChangeListener((view, focused) -> { if (!focused) dispatchRuntimeEvent(widget.getId(), "change"); });
            return registerRuntimeWidget(widget, layout);
        }
        if ("swipe_refresh".equals(widget.getType())) {
            androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh = new androidx.swiperefreshlayout.widget.SwipeRefreshLayout(this);
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) content.addView(child);
            }
            refresh.addView(content);
            refresh.setOnRefreshListener(() -> { refresh.setRefreshing(false); dispatchRuntimeEvent(widget.getId(), "refresh"); });
            return registerRuntimeWidget(widget, refresh);
        }
        if ("radio_group".equals(widget.getType())) {
            RadioGroup group = new RadioGroup(this);
            group.setOrientation(LinearLayout.VERTICAL);
            for (String childId : widget.getChildren()) {
                View child = renderWidget(document, document.getWidgets().get(childId));
                if (child != null) group.addView(child);
            }
            group.setOnCheckedChangeListener((view, checkedId) -> dispatchRuntimeEvent(widget.getId(), "change"));
            return registerRuntimeWidget(widget, group);
        }
        if ("sign_in".equals(widget.getType())) {
            com.google.android.gms.common.SignInButton signIn = new com.google.android.gms.common.SignInButton(this);
            signIn.setOnClickListener(view -> dispatchRuntimeEvent(widget.getId(), "click"));
            return registerRuntimeWidget(widget, signIn);
        }
        if ("circle_image".equals(widget.getType())) {
            de.hdodenhof.circleimageview.CircleImageView image = new de.hdodenhof.circleimageview.CircleImageView(this);
            applyImageProperties(image, document, widget);
            image.setContentDescription(propertyString(widget, "contentDescription", "Image"));
            return registerRuntimeWidget(widget, image);
        }
        if ("otp".equals(widget.getType())) {
            EditText otp = new EditText(this);
            otp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            otp.setHint(propertyString(widget, "hint", "One-time password"));
            otp.setSingleLine(true);
            return registerRuntimeWidget(widget, otp);
        }
        if ("code".equals(widget.getType())) {
            EditText code = new EditText(this);
            code.setText(propertyString(widget, "text", ""));
            code.setHint(propertyString(widget, "hint", "Code"));
            code.setTypeface(android.graphics.Typeface.MONOSPACE);
            code.setSingleLine(false);
            return registerRuntimeWidget(widget, code);
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
        return registerRuntimeWidget(widget, container);
    }

    private View registerRuntimeWidget(CreatorWidget widget, View view) {
        applyCommonViewProperties(widget, view);
        runtimeEnvironment.registerWidget(widget.getId(), view);
        if (hasRuntimeClickBinding(widget.getId())) view.setOnClickListener(v -> dispatchRuntimeEvent(widget.getId(), "click"));
        return view;
    }

    private boolean hasRuntimeClickBinding(String widgetId) {
        if (session == null || widgetId == null) return false;
        for (CreatorEventBinding binding : session.getDocument().getEvents().values()) {
            if (widgetId.equals(binding.getTargetWidgetId()) && "click".equals(binding.getEventName())) return true;
        }
        return false;
    }

    private void applyCommonViewProperties(CreatorWidget widget, View view) {
        CreatorProjectDocument document = session == null ? null : session.getDocument();
        view.setEnabled(propertyBoolean(widget, "enabled", true));
        view.setClickable(propertyBoolean(widget, "clickable", view.isClickable()));
        view.setVisibility(propertyBoolean(widget, "visible", true) ? View.VISIBLE : View.GONE);
        view.setAlpha(propertyFloat(widget, "alpha", 1f));
        view.setRotation(propertyFloat(widget, "rotation", view.getRotation()));
        view.setTranslationX(propertyFloat(widget, "translationX", 0f));
        view.setTranslationY(propertyFloat(widget, "translationY", 0f));
        view.setScaleX(propertyFloat(widget, "scaleX", 1f));
        view.setScaleY(propertyFloat(widget, "scaleY", 1f));
        applyLegacyLayoutProperties(widget, view);
        String backgroundColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
                propertyString(widget, "backgroundColor", ""), resourceVariant());
        if (!backgroundColor.isEmpty()) {
            try { view.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor)); }
            catch (IllegalArgumentException ignored) { }
        }
        String backgroundResource = propertyString(widget, "backgroundResource", "");
        if (!backgroundResource.isEmpty()) {
            android.graphics.drawable.Drawable drawable = resolveProjectDrawable(document, backgroundResource);
            if (drawable != null) view.setBackground(drawable);
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            applyRuntimeTypeface(document, text, widget);
            String textColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
                    propertyString(widget, "textColor", ""), resourceVariant());
            if (!textColor.isEmpty()) {
                try { text.setTextColor(android.graphics.Color.parseColor(textColor)); }
                catch (IllegalArgumentException ignored) { }
            }
            text.setTextSize(propertyFloat(widget, "textSize", text.getTextSize() / getResources().getDisplayMetrics().scaledDensity));
        }
        if (view instanceof EditText) {
            String hintColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
                    propertyString(widget, "hintTextColor", ""), resourceVariant());
            if (!hintColor.isEmpty()) {
                try { ((EditText) view).setHintTextColor(android.graphics.Color.parseColor(hintColor)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private void applyLegacyLayoutProperties(CreatorWidget widget, View view) {
        int uniformPadding = propertyInt(widget, "padding", 0);
        view.setPadding(dp(propertyInt(widget, "paddingLeft", uniformPadding)), dp(propertyInt(widget, "paddingTop", uniformPadding)),
                dp(propertyInt(widget, "paddingRight", uniformPadding)), dp(propertyInt(widget, "paddingBottom", uniformPadding)));
        int width = runtimeLayoutDimension(propertyInt(widget, "legacyWidth", ViewGroup.LayoutParams.WRAP_CONTENT));
        int height = runtimeLayoutDimension(propertyInt(widget, "legacyHeight", ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams parameters = new LinearLayout.LayoutParams(width, height,
                Math.max(0f, propertyFloat(widget, "legacyWeight", 0f)));
        parameters.setMargins(dp(propertyInt(widget, "marginLeft", 0)), dp(propertyInt(widget, "marginTop", 0)),
                dp(propertyInt(widget, "marginRight", 0)), dp(propertyInt(widget, "marginBottom", 0)));
        int gravity = propertyInt(widget, "legacyLayoutGravity", Gravity.NO_GRAVITY);
        if (gravity != Gravity.NO_GRAVITY) parameters.gravity = gravity;
        view.setLayoutParams(parameters);
        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            int contentGravity = propertyInt(widget, "legacyGravity", Gravity.NO_GRAVITY);
            if (contentGravity != Gravity.NO_GRAVITY) layout.setGravity(contentGravity);
            int weightSum = propertyInt(widget, "legacyWeightSum", 0);
            if (weightSum > 0) layout.setWeightSum(weightSum);
        }
    }

    private int runtimeLayoutDimension(int value) {
        if (value == ViewGroup.LayoutParams.MATCH_PARENT || value == ViewGroup.LayoutParams.WRAP_CONTENT) return value;
        return value > 0 ? dp(value) : ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void applyRuntimeTypeface(CreatorProjectDocument document, TextView text, CreatorWidget widget) {
        int style = propertyInt(widget, "textType", android.graphics.Typeface.NORMAL);
        String fontName = propertyString(widget, "textFont", "default_font");
        Object rawTypeface = widget.getProperties().get("typeface");
        if (rawTypeface instanceof Map) {
            Map<?, ?> typeface = (Map<?, ?>) rawTypeface;
            Object font = typeface.get("font");
            if (font != null) fontName = String.valueOf(font);
            Object namedStyle = typeface.get("style");
            if (namedStyle != null) style = typefaceStyle(String.valueOf(namedStyle), style);
        }
        if ("default_font".equals(fontName) || fontName.trim().isEmpty()) {
            text.setTypeface(android.graphics.Typeface.DEFAULT, style);
            return;
        }
        java.io.File font = resolveProjectFont(document, fontName);
        if (font == null) {
            text.setTypeface(android.graphics.Typeface.DEFAULT, style);
            return;
        }
        try {
            text.setTypeface(android.graphics.Typeface.createFromFile(font), style);
        } catch (RuntimeException ignored) {
            text.setTypeface(android.graphics.Typeface.DEFAULT, style);
        }
    }

    private static int typefaceStyle(String value, int fallback) {
        String style = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (style.contains("BOLD") && style.contains("ITALIC")) return android.graphics.Typeface.BOLD_ITALIC;
        if (style.contains("BOLD")) return android.graphics.Typeface.BOLD;
        if (style.contains("ITALIC")) return android.graphics.Typeface.ITALIC;
        if (style.contains("NORMAL")) return android.graphics.Typeface.NORMAL;
        return fallback;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String resourceVariant() {
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ? "-night" : "";
    }

    private String propertyString(CreatorWidget widget, String key, String fallback) {
        Object value = widget.getProperties().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int propertyInt(CreatorWidget widget, String key, int fallback) {
        Object value = widget.getProperties().get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private boolean propertyBoolean(CreatorWidget widget, String key, boolean fallback) {
        Object value = widget.getProperties().get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String && ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }

    private float propertyFloat(CreatorWidget widget, String key, float fallback) {
        Object value = widget.getProperties().get(key);
        if (value instanceof Number) return ((Number) value).floatValue();
        try { return value == null ? fallback : Float.parseFloat(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private void applyImageProperties(ImageView image, CreatorProjectDocument document, CreatorWidget widget) {
        String resourceName = propertyString(widget, "resourceName", "");
        int resourceId = resourceName.isEmpty() ? 0
                : getResources().getIdentifier(resourceName, "drawable", getPackageName());
        java.io.File projectImage = resolveProjectImage(document, resourceName);
        String filePath = propertyString(widget, "filePath", "");
        java.io.File importedFile = filePath.isEmpty() ? null
                : CreatorRuntimeResourceResolver.resolveProjectImage(document.getProjectId(), filePath);
        String url = propertyString(widget, "url", "");
        if (importedFile != null) image.setImageURI(android.net.Uri.fromFile(importedFile));
        else if (!url.isEmpty() && (url.startsWith("https://") || url.startsWith("http://"))) {
            com.bumptech.glide.Glide.with(this).load(url).into(image);
        }
        else if (projectImage != null) image.setImageURI(android.net.Uri.fromFile(projectImage));
        else image.setImageResource(resourceId == 0 ? R.drawable.ic_mtrl_image : resourceId);
        try {
            image.setScaleType(ImageView.ScaleType.valueOf(propertyString(widget, "scaleType", "CENTER")));
        } catch (IllegalArgumentException ignored) {
            image.setScaleType(ImageView.ScaleType.CENTER);
        }
    }

    private void applySeekBarResources(android.widget.SeekBar seekBar, CreatorProjectDocument document, CreatorWidget widget) {
        android.graphics.drawable.Drawable thumb = resolveProjectDrawable(document,
                propertyString(widget, "thumbResource", ""));
        if (thumb != null) seekBar.setThumb(thumb);
        android.graphics.drawable.Drawable track = resolveProjectDrawable(document,
                propertyString(widget, "trackResource", ""));
        if (track != null) seekBar.setProgressDrawable(track);
    }

    private java.util.List<?> runtimeItems(CreatorProjectDocument document, CreatorWidget widget) {
        Object stateId = widget.getProperties().get("customDataStateId");
        Object rawItems = stateId == null ? widget.getProperties().get("items")
                : document.getState().get(String.valueOf(stateId));
        return rawItems instanceof java.util.List ? (java.util.List<?>) rawItems : java.util.Collections.emptyList();
    }

    private String runtimeItemText(Object item) {
        if (!(item instanceof Map)) return String.valueOf(item);
        Map<?, ?> row = (Map<?, ?>) item;
        for (String preferred : new String[]{"text", "title", "name"}) {
            Object value = row.get(preferred);
            if (value != null) return String.valueOf(value);
        }
        StringBuilder text = new StringBuilder();
        for (Object value : row.values()) {
            if (text.length() > 0) text.append(" · ");
            text.append(String.valueOf(value));
        }
        return text.toString();
    }

    private android.graphics.drawable.Drawable resolveProjectDrawable(CreatorProjectDocument document, String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) return null;
        String normalized = resourceName.replace(".9", "").toLowerCase(java.util.Locale.ROOT);
        java.io.File file = resolveProjectImage(document, normalized);
        if (file == null && !normalized.equals(resourceName)) file = resolveProjectImage(document, resourceName);
        if (file != null) return android.graphics.drawable.Drawable.createFromPath(file.getPath());
        int resourceId = getResources().getIdentifier(normalized, "drawable", getPackageName());
        return resourceId == 0 ? null : getResources().getDrawable(resourceId, getTheme());
    }

    private java.io.File resolveProjectImage(CreatorProjectDocument document, String resourceName) {
        Object rawResources = document.getState().get("legacy.resources");
        if (!(rawResources instanceof java.util.List)) return null;
        for (Object raw : (java.util.List<?>) rawResources) {
            if (!(raw instanceof Map)) continue;
            Map<?, ?> resource = (Map<?, ?>) raw;
            if (!resourceName.equals(String.valueOf(resource.get("name")))) continue;
            Object source = resource.get("source");
            return source == null ? null : CreatorRuntimeResourceResolver.resolveProjectImage(
                    document.getProjectId(), String.valueOf(source));
        }
        return null;
    }

    private java.io.File resolveProjectFont(CreatorProjectDocument document, String fontName) {
        if (document == null) return null;
        Object rawFonts = document.getState().get("legacy.fontResources");
        if (!(rawFonts instanceof Map)) return null;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawFonts).entrySet()) {
            if (!(entry.getValue() instanceof Map)) continue;
            Map<?, ?> descriptor = (Map<?, ?>) entry.getValue();
            Object source = descriptor.get("source");
            String name = String.valueOf(entry.getKey());
            String baseName = name.replaceFirst("\\.[^.]+$", "");
            if (!fontName.equals(name) && !fontName.equals(baseName)) continue;
            return source == null ? null : CreatorRuntimeResourceResolver.resolveProjectImage(
                    document.getProjectId(), String.valueOf(source));
        }
        return null;
    }

    private void dispatchRuntimeEvent(String widgetId, String eventName) {
        java.util.List<CreatorRuntimeExecutor.Effect> effects = runtimeExecutor.dispatch(session.getEngine(), widgetId, eventName);
        if (effects != null && !effects.isEmpty()) renderEffects(effects);
        render();
    }

    private void dispatchLifecycleEvent(String eventName) {
        if (runtimeExecutor == null || session == null) return;
        renderEffects(runtimeExecutor.dispatch(session.getEngine(),
                CreatorLegacyArtifactImporter.ACTIVITY_EVENT_TARGET, eventName));
    }

    private void handleRuntimeServiceEvent(String serviceId, String eventName, Map<String, Object> payload) {
        if ("creator_runtime".equals(serviceId) && "open_editor".equals(eventName)) {
            openEditor();
            return;
        }
        if ("intent".equals(serviceId) && "navigate".equals(eventName) && payload.get("screenId") != null) {
            activeScreenId = String.valueOf(payload.get("screenId"));
        }
        if ("timer".equals(serviceId) && payload.get("timerId") != null) {
            renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(payload.get("timerId")), eventName));
        }
        if ("firebase".equals(serviceId) && "children".equals(eventName)) {
            Object resultStateId = payload.get("resultStateId");
            Object rows = payload.get("rows");
            if (resultStateId != null && rows instanceof List) {
                Map<String, Object> statePayload = new LinkedHashMap<>();
                statePayload.put("stateId", String.valueOf(resultStateId));
                statePayload.put("value", rows);
                CreatorProjectDocument document = session.getDocument();
                CreatorProjectOperation operation = new CreatorProjectOperation("runtime-firebase-children-"
                        + UUID.randomUUID(), document.getProjectId(), document.getRevision(),
                        CreatorProjectOperation.ActorKind.SYSTEM, CreatorProjectOperation.Type.STATE_SET,
                        statePayload, System.currentTimeMillis());
                session.apply(operation);
            }
            Object callbackTargetId = payload.get("callbackTargetId");
            if (callbackTargetId != null && !String.valueOf(callbackTargetId).trim().isEmpty()) {
                renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(callbackTargetId), "children"));
            }
        }
        if ("dialog".equals(serviceId) && "button".equals(eventName)) {
            Object callbackTargetId = payload.get("callbackTargetId");
            if (callbackTargetId != null && !String.valueOf(callbackTargetId).trim().isEmpty()) {
                renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(callbackTargetId), "button"));
            }
        }
        Object rawComponents = session.getDocument().getState().get("legacy.components");
        if (rawComponents instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawComponents).entrySet()) {
                if (!(entry.getValue() instanceof Map)) continue;
                Object boundService = ((Map<?, ?>) entry.getValue()).get("serviceId");
                if (!serviceId.equals(boundService)) continue;
                renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(entry.getKey()), eventName));
            }
        }
        String summary = serviceId + " · " + eventName;
        if ("error".equals(eventName) && payload.get("message") != null) {
            summary += ": " + payload.get("message");
        }
        Toast.makeText(this, summary, Toast.LENGTH_SHORT).show();
        render();
    }

    private void renderEffects(java.util.List<CreatorRuntimeExecutor.Effect> effects) {
        for (CreatorRuntimeExecutor.Effect effect : effects) {
            if ("message".equals(effect.getType())) Toast.makeText(this, effect.getValue(), Toast.LENGTH_SHORT).show();
            else if ("navigate".equals(effect.getType())) {
                Toast.makeText(this, getString(R.string.creator_navigation_effect, effect.getValue()), Toast.LENGTH_SHORT).show();
            }
        }
    }


}
