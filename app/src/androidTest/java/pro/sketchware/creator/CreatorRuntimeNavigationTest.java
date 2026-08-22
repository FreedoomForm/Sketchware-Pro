package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;

import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.button.MaterialButton;
import com.besome.sketch.beans.ViewBean;
import mod.agus.jcoderz.beans.ViewBeans;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorApplyResult;
import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorProjectOperation;
import pro.sketchware.creator.runtime.CreatorRuntimeBlock;
import pro.sketchware.creator.runtime.CreatorRuntimeDefaults;
import pro.sketchware.creator.runtime.CreatorRuntimeExecutor;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

@RunWith(AndroidJUnit4.class)
public class CreatorRuntimeNavigationTest {
    private Context context;

    @Before public void clearRuntimeState() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit().clear().commit();
        CreatorRuntimeSession.resetForTests();
    }

    @Test public void hostManifestDeclaresMobileAdsApplicationId() throws Exception {
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                context.getPackageName(), PackageManager.GET_META_DATA);
        assertThat(info.metaData).isNotNull();
        String appId = info.metaData.getString("com.google.android.gms.ads.APPLICATION_ID");
        assertThat(appId).isEqualTo(context.getString(R.string.google_mobile_ads_app_id));
        assertThat(appId).startsWith("ca-app-pub-");
        assertThat(appId).contains("~");
    }

    @Test public void creatorActivitiesLaunchWithAppCompatPostSplashTheme() {
        try (ActivityScenario<CreatorProjectActivity> project = ActivityScenario.launch(CreatorProjectActivity.class)) {
            project.onActivity(activity -> assertThat(activity.hasWindowFocus() || !activity.isFinishing()).isTrue());
        }
    }

    @Test public void installedLauncherIsOriginalDesignActivityOnMainProject() {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertThat(launchIntent).isNotNull();
        assertThat(launchIntent.getComponent()).isNotNull();
        assertThat(launchIntent.getComponent().getClassName())
                .isEqualTo(DesignActivity.class.getName());

        try (ActivityScenario<DesignActivity> scenario = ActivityScenario.launch(DesignActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat(activity.getIntent().getStringExtra("creator_runtime_project_id"))
                        .isNotEmpty();
                assertThat((Object) activity.findViewById(R.id.tab_layout)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.viewpager)).isNotNull();
                assertThat(activity.findViewById(R.id.btn_options).getVisibility())
                        .isEqualTo(View.GONE);
            });
        }
    }

    @Test public void freshRuntimeProjectMatchesOriginalMainScreenContract() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        assertThat(document.getEntryScreenId()).isEqualTo("main");
        assertThat(document.getScreens()).containsKey("main");
        assertThat(document.getScreens()).containsKey(CreatorRuntimeDefaults.EDITOR_SCREEN_ID);
        assertThat(document.getScreens().get(CreatorRuntimeDefaults.EDITOR_SCREEN_ID).isLocked()).isTrue();
        assertThat(document.getScreens().get("main").getRootWidgetId()).isEqualTo("root_main");
        assertThat(document.getWidgets()).containsKey("root_main");
    }

    @Test public void starterSurfacePersistsContinueButtonAndEditorIntentBinding() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        assertThat(document.getWidgets()).containsKey(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(document.getWidgets().get(CreatorRuntimeDefaults.ENTRY_WIDGET_ID).getType())
                .isEqualTo("button");
        assertThat(document.getWidgets().get("root_main").getChildren())
                .contains(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        pro.sketchware.creator.runtime.CreatorEventBinding binding = document.getEvents()
                .get(CreatorRuntimeDefaults.ENTRY_CLICK_BINDING_ID);
        assertThat(binding).isNotNull();
        assertThat(binding.getTargetWidgetId()).isEqualTo(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
        assertThat(binding.getEventName()).isEqualTo("click");
        assertThat(binding.getBlocks()).hasSize(1);
        assertThat(binding.getBlocks().get(0).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL);
        assertThat(binding.getBlocks().get(0).getPayload().get("serviceId"))
                .isEqualTo("intent");
    }

    @Test public void legacyProvisioningPersistsDefaultMainProjectFile() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        com.besome.sketch.beans.ProjectFileBean main = a.a.a.jC.b(scId)
                .b(com.besome.sketch.beans.ProjectFileBean.DEFAULT_XML_NAME);
        assertThat(main).isNotNull();
        assertThat(main.fileName).isEqualTo("main");
        assertThat(main.fileType)
                .isEqualTo(com.besome.sketch.beans.ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY);
        com.besome.sketch.beans.ProjectFileBean editor = a.a.a.jC.b(scId).b("editor.xml");
        assertThat(editor).isNotNull();
        assertThat(editor.isActivityLocked()).isTrue();
        ArrayList<com.besome.sketch.beans.ComponentBean> components = a.a.a.jC.a(scId).e(main.getJavaName());
        boolean intentFound = false;
        for (com.besome.sketch.beans.ComponentBean component : components) {
            if (component != null && CreatorRuntimeDefaults.EDITOR_INTENT_ID.equals(component.componentId)) {
                intentFound = true;
            }
        }
        assertThat(intentFound).isTrue();
        ArrayList<com.besome.sketch.beans.EventBean> events = a.a.a.jC.a(scId).g(main.getJavaName());
        boolean continueEventFound = false;
        for (com.besome.sketch.beans.EventBean event : events) {
            if (event != null && CreatorRuntimeDefaults.ENTRY_WIDGET_ID.equals(event.targetId)
                    && "onClick".equals(event.eventName)) {
                continueEventFound = true;
                assertThat(a.a.a.jC.a(scId).a(main.getJavaName(), event.getEventKey())).hasSize(2);
            }
        }
        assertThat(continueEventFound).isTrue();
        ArrayList<ViewBean> views = a.a.a.jC.a(scId).d(main.getXmlName());
        boolean continueViewFound = false;
        for (ViewBean view : views) {
            if (view != null && CreatorRuntimeDefaults.ENTRY_WIDGET_ID.equals(view.id)) {
                continueViewFound = true;
                assertThat(view.type).isEqualTo(ViewBean.VIEW_TYPE_WIDGET_BUTTON);
                assertThat(view.parent).isEqualTo("root");
            }
        }
        assertThat(continueViewFound).isTrue();
    }

    @Test public void compatibilityHomeEntryRedirectsToOriginalSketchwareEditor() {
        try (ActivityScenario<CreatorHomeActivity> home = ActivityScenario.launch(CreatorHomeActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<Activity> resumed = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                            .forEach(resumed::set));
            assertThat(resumed.get()).isInstanceOf(DesignActivity.class);
            assertThat((Object) resumed.get().findViewById(R.id.tab_layout)).isNotNull();
            assertThat((Object) resumed.get().findViewById(R.id.viewpager)).isNotNull();
        }
    }

    @Test public void creatorRuntimeOpensOriginalSketchwareEditorSurface() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        Intent intent = new Intent(context, DesignActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("creator_runtime_project_id", document.getProjectId());
        try (ActivityScenario<DesignActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat((Object) activity.findViewById(R.id.toolbar)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.tab_layout)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.viewpager)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.btn_options)).isNotNull();
                assertThat(activity.findViewById(R.id.btn_options).getVisibility()).isEqualTo(View.GONE);
                DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
                assertThat(drawer.getDrawerLockMode(GravityCompat.END))
                        .isNotEqualTo(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                assertThat((Object) activity.findViewById(R.id.item_versions)).isNotNull();
            });
        }
    }

    @Test public void creatorRuntimeActivityManagerIsReachableAndContainsEditorActivity() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        Intent intent = new Intent(context, com.besome.sketch.editor.manage.view.ManageViewActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("creator_runtime_project_id", document.getProjectId());
        try (ActivityScenario<com.besome.sketch.editor.manage.view.ManageViewActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(activity.isFinishing()).isFalse();
                assertThat((Object) activity.findViewById(R.id.view_pager)).isNotNull();
                assertThat(a.a.a.jC.b(scId).b().size()).isAtLeast(2);
                assertThat(a.a.a.jC.b(scId).b().get(1).fileName).isEqualTo("editor");
                assertThat(a.a.a.jC.b(scId).b().get(1).isActivityLocked()).isTrue();
            });
        }
    }

    @Test public void creatorRuntimeCanOpenOriginalVisualBlockEditor() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        com.besome.sketch.beans.ProjectFileBean main = a.a.a.jC.b(scId)
                .b(com.besome.sketch.beans.ProjectFileBean.DEFAULT_XML_NAME);
        Intent intent = new Intent(context, LogicEditorActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("id", CreatorRuntimeDefaults.ENTRY_WIDGET_ID)
                .putExtra("event", "onClick")
                .putExtra("project_file", main)
                .putExtra("event_text", "Continue button click")
                .putExtra("creator_runtime_project_id", document.getProjectId());
        try (ActivityScenario<LogicEditorActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(activity.isFinishing()).isFalse();
                assertThat((Object) activity.findViewById(R.id.editor)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.palette_selector)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.fab_toggle_palette)).isNotNull();
            });
        }
    }

    @Test public void runtimeWidgetChangesAreProjectedIntoOriginalViewStore() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        Intent intent = new Intent(context, DesignActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("creator_runtime_project_id", document.getProjectId());
        AtomicReference<CreatorApplyResult> result = new AtomicReference<>();
        try (ActivityScenario<DesignActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("widgetId", "runtime_button");
                payload.put("widgetType", "button");
                payload.put("parentId", "root_main");
                payload.put("index", 0L);
                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("text", "Runtime button");
                payload.put("properties", properties);
                CreatorProjectOperation operation = new CreatorProjectOperation(
                        "instrumentation-runtime-widget", document.getProjectId(),
                        CreatorRuntimeSession.get(activity).getDocument().getRevision(),
                        CreatorProjectOperation.ActorKind.AI,
                        CreatorProjectOperation.Type.WIDGET_ADD, payload,
                        System.currentTimeMillis());
                result.set(CreatorRuntimeSession.get(activity).apply(operation));
            });
        }
        assertThat(result.get()).isNotNull();
        assertThat(result.get().isApplied()).isTrue();
        ArrayList<ViewBean> views = a.a.a.jC.a(scId).d("main.xml");
        boolean found = false;
        for (ViewBean view : views) {
            if (view != null && "runtime_button".equals(view.id)) {
                found = true;
                assertThat(view.type).isEqualTo(ViewBean.VIEW_TYPE_WIDGET_BUTTON);
                assertThat(view.text).isNotNull();
                assertThat(view.text.text).isEqualTo("Runtime button");
                assertThat(view.parent).isEqualTo("root");
            }
        }
        assertThat(found).isTrue();
    }

    @Test public void mainActivityBackReturnsToSamePersistedNativeSurface() {
        try (ActivityScenario<DesignActivity> editorScenario = ActivityScenario.launch(DesignActivity.class)) {
            editorScenario.onActivity(activity -> {
                assertThat(activity.getIntent().getStringExtra("creator_runtime_project_id")).isNotEmpty();
                assertThat(activity.findViewById(R.id.file_name).getVisibility()).isEqualTo(View.VISIBLE);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<Activity> editor = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                            .forEach(candidate -> {
                                if (candidate instanceof DesignActivity) editor.set(candidate);
                            }));
            assertThat(editor.get()).isNotNull();
            editor.get().finish();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<Activity> live = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                            .forEach(candidate -> {
                                if (candidate instanceof CreatorProjectActivity) live.set(candidate);
                            }));
            assertThat(live.get()).isNotNull();
            assertThat(live.get().findViewById(R.id.creator_editor_header).getVisibility())
                    .isEqualTo(View.GONE);
            assertThat((Object) live.get().findViewById(R.id.creator_preview_canvas)).isNotNull();
            assertThat((Object) live.get().findViewById(R.id.creator_preview_canvas)
                    .findViewWithTag(CreatorRuntimeDefaults.ENTRY_WIDGET_ID)).isNotNull();
        }
    }

    @Test public void extendedRuntimeWidgetTypesProjectToOriginalViewStore() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("widgetId", "runtime_code_widget");
        payload.put("widgetType", "code");
        payload.put("parentId", "root_main");
        payload.put("index", 0L);
        payload.put("properties", new LinkedHashMap<String, Object>());
        CreatorApplyResult result = CreatorRuntimeSession.get(context).apply(new CreatorProjectOperation(
                "instrumentation-runtime-code-widget", document.getProjectId(),
                CreatorRuntimeSession.get(context).getDocument().getRevision(),
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.WIDGET_ADD,
                payload, System.currentTimeMillis()));
        assertThat(result.isApplied()).isTrue();
        ArrayList<ViewBean> views = a.a.a.jC.a(scId).d("main.xml");
        boolean found = false;
        for (ViewBean view : views) {
            if (view != null && "runtime_code_widget".equals(view.id)) {
                found = true;
                assertThat(view.type).isEqualTo(ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW);
            }
        }
        assertThat(found).isTrue();
    }

    @Test public void blockClickMutationPersistsThroughSessionAndNotifiesLiveObservers() {
        CreatorRuntimeSession runtime = CreatorRuntimeSession.get(context);
        CreatorProjectDocument initial = runtime.getDocument();
        Map<String, Object> widgetPayload = new LinkedHashMap<>();
        widgetPayload.put("widgetId", "behavior_button");
        widgetPayload.put("widgetType", "button");
        widgetPayload.put("parentId", "root_main");
        widgetPayload.put("index", 0L);
        Map<String, Object> widgetProperties = new LinkedHashMap<>();
        widgetProperties.put("text", "Before");
        widgetPayload.put("properties", widgetProperties);
        CreatorApplyResult widgetResult = runtime.apply(new CreatorProjectOperation(
                "instrumentation-behavior-widget", initial.getProjectId(), initial.getRevision(),
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.WIDGET_ADD,
                widgetPayload, System.currentTimeMillis()));
        assertThat(widgetResult.isApplied()).isTrue();

        Map<String, Object> blockPayload = new LinkedHashMap<>();
        blockPayload.put("widgetId", "behavior_button");
        blockPayload.put("property", "text");
        blockPayload.put("value", "After");
        CreatorRuntimeBlock block = new CreatorRuntimeBlock(
                CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, blockPayload);
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("bindingId", "behavior_button_click");
        eventPayload.put("targetWidgetId", "behavior_button");
        eventPayload.put("eventName", "click");
        eventPayload.put("blocks", Collections.singletonList(block));
        CreatorProjectDocument beforeEvent = runtime.getDocument();
        CreatorApplyResult eventResult = runtime.apply(new CreatorProjectOperation(
                "instrumentation-behavior-event", beforeEvent.getProjectId(), beforeEvent.getRevision(),
                CreatorProjectOperation.ActorKind.USER, CreatorProjectOperation.Type.EVENT_ATTACH,
                eventPayload, System.currentTimeMillis()));
        assertThat(eventResult.isApplied()).isTrue();

        AtomicInteger notifications = new AtomicInteger();
        runtime.addListener(document -> notifications.incrementAndGet());
        new CreatorRuntimeExecutor(null, runtime).dispatch(runtime.getEngine(), "behavior_button", "click");
        Object text = runtime.getDocument().getWidgets().get("behavior_button").getProperties().get("text");
        assertThat(text).isEqualTo("After");
        assertThat(notifications.get()).isGreaterThan(0);
    }

    @Test public void originalEditorExposesAllTabsAndDrawerActionsInsideRuntimeHost() {
        CreatorProjectDocument document = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, document);
        Intent intent = new Intent(context, DesignActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("creator_runtime_project_id", document.getProjectId());
        try (ActivityScenario<DesignActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(((com.google.android.material.tabs.TabLayout) activity.findViewById(R.id.tab_layout)).getTabCount())
                        .isEqualTo(4);
                int[] actions = {
                        R.id.item_library_manager, R.id.item_view_manager,
                        R.id.item_image_manager, R.id.item_sound_manager,
                        R.id.item_font_manager, R.id.item_java_manager,
                        R.id.item_resource_manager, R.id.item_resource_editor,
                        R.id.item_assets_manager, R.id.item_permission_manager,
                        R.id.item_appcompat_manager, R.id.item_manifest_manager,
                        R.id.item_used_custom_blocks, R.id.item_code_shrinking_manager,
                        R.id.item_stringfog_manager, R.id.item_show_src,
                        R.id.item_xml_command_manager, R.id.item_logcat_reader,
                        R.id.item_collection_manager
                };
                for (int id : actions) assertThat((Object) activity.findViewById(id)).isNotNull();
            });
        }
    }

    @Test public void editorSidebarContainsMigratedMainScreenActions() {
        try (ActivityScenario<CreatorProjectActivity> scenario = ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat((Object) activity.findViewById(R.id.creator_project_drawer)).isInstanceOf(DrawerLayout.class);
                View sidebar = activity.findViewById(R.id.creator_project_sidebar);
                assertThat((Object) sidebar).isNotNull();
                assertThat(((DrawerLayout.LayoutParams) sidebar.getLayoutParams()).gravity)
                        .isEqualTo(android.view.Gravity.END);
                int[] actions = {
                        R.id.creator_sidebar_about, R.id.creator_sidebar_changelog,
                        R.id.creator_sidebar_info, R.id.creator_sidebar_keystore,
                        R.id.creator_sidebar_settings, R.id.creator_sidebar_swassist,
                        R.id.creator_sidebar_discord, R.id.creator_sidebar_telegram,
                        R.id.creator_sidebar_github
                };
                for (int id : actions) assertThat((Object) activity.findViewById(id)).isNotNull();
            });
        }
    }

    @Test public void liveOnlyEntryControlReturnsToOriginalSketchwareEditor() {
        try (ActivityScenario<CreatorProjectActivity> live = ActivityScenario.launch(
                new Intent(context, CreatorProjectActivity.class)
                        .putExtra(CreatorProjectActivity.EXTRA_LIVE_ONLY, true))) {
            live.onActivity(activity -> {
                View continueButton = activity.findViewById(R.id.creator_preview_canvas)
                        .findViewWithTag(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
                assertThat((Object) continueButton).isNotNull();
                continueButton.performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<Activity> resumed = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                            .forEach(resumed::set));
            assertThat(resumed.get()).isInstanceOf(DesignActivity.class);
            assertThat((Object) resumed.get().findViewById(R.id.tab_layout)).isNotNull();
        }
    }

    @Test public void liveOnlySurfaceRendersBlankScreenAndContinueWidget() {
        Intent intent = new Intent(context, CreatorProjectActivity.class)
                .putExtra(CreatorProjectActivity.EXTRA_LIVE_ONLY, true);
        try (ActivityScenario<CreatorProjectActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(((android.widget.LinearLayout) activity.findViewById(R.id.creator_preview_canvas))
                        .getChildCount()).isGreaterThan(0);
                View continueButton = activity.findViewById(R.id.creator_preview_canvas)
                        .findViewWithTag(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
                assertThat((Object) continueButton).isNotNull();
                assertThat(continueButton.getVisibility()).isEqualTo(View.VISIBLE);
                assertThat(((com.google.android.material.button.MaterialButton) continueButton)
                        .getText().toString()).isEqualTo("Continue");
            });
        }
    }

    @Test public void liveOnlyModeUsesNativeCanvasAndHidesEditorControls() {
        Intent intent = new Intent(context, CreatorProjectActivity.class)
                .putExtra(CreatorProjectActivity.EXTRA_LIVE_ONLY, true);
        try (ActivityScenario<CreatorProjectActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(activity.findViewById(R.id.creator_editor_header).getVisibility())
                        .isEqualTo(View.GONE);
                assertThat(activity.findViewById(R.id.creator_editor_controls).getVisibility())
                        .isEqualTo(View.GONE);
                assertThat((Object) activity.findViewById(R.id.creator_preview_canvas)).isNotNull();
                View continueButton = activity.findViewById(R.id.creator_preview_canvas)
                        .findViewWithTag(CreatorRuntimeDefaults.ENTRY_WIDGET_ID);
                assertThat((Object) continueButton).isNotNull();
                assertThat(continueButton.getVisibility()).isEqualTo(View.VISIBLE);
                assertThat(((MaterialButton) continueButton).getText().toString()).isEqualTo("Continue");
            });
        }
    }
}
