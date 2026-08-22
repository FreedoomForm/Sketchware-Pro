package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.design.DesignActivity;
import com.google.android.material.button.MaterialButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeDefaults;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/**
 * One intentionally small, high-value native smoke flow. This is not a
 * replacement for the broad regression suite; it is the fast gate that must
 * run on every push and directly exercises the failure reported on device.
 */
@RunWith(AndroidJUnit4.class)
public final class CreatorRuntimeSmokeTest {
    private Context context;

    @Before
    public void resetRuntimeState() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE)
                .edit().clear().commit();
        CreatorRuntimeSession.resetForTests();
    }

    @Test
    public void editorRoundTripPreservesUserButtonAndRunsItsClickBehavior() {
        Intent launch = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        assertThat(launch).isNotNull();
        assertThat(launch.getComponent()).isNotNull();
        assertThat(launch.getComponent().getClassName())
                .isEqualTo(DesignActivity.class.getName());

        CreatorProjectDocument initial = CreatorRuntimeSession.get(context).getDocument();
        String scId = CreatorLegacyProjectBridge.ensureLegacyProject(context, initial);
        String addedId = "smoke_added_button";
        Intent editorIntent = new Intent(context, DesignActivity.class)
                .putExtra("sc_id", scId)
                .putExtra("creator_runtime_project_id", initial.getProjectId());

        try (ActivityScenario<DesignActivity> editorScenario = ActivityScenario.launch(editorIntent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            ProjectFileBean main = a.a.a.jC.b(scId)
                    .b(ProjectFileBean.DEFAULT_XML_NAME);
            assertThat(main).isNotNull();
            ArrayList<ViewBean> before = awaitLegacyViews(scId, main.getXmlName(),
                    CreatorRuntimeDefaults.ENTRY_WIDGET_ID, 12000L);
            editorScenario.onActivity(activity -> {
                assertThat((Object) activity.findViewById(R.id.tab_layout)).isNotNull();
                assertThat((Object) activity.findViewById(R.id.viewpager)).isNotNull();
                assertThat(activity.findViewById(R.id.btn_options).getVisibility())
                        .isEqualTo(View.GONE);
                assertThat(findView(before, CreatorRuntimeDefaults.ENTRY_WIDGET_ID))
                        .isNotNull();

                ViewBean added = new ViewBean(addedId, ViewBean.VIEW_TYPE_WIDGET_BUTTON);
                added.parent = "root";
                added.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
                added.index = before.size();
                added.text.text = "Smoke button";
                added.layout.width = LayoutBean.LAYOUT_MATCH_PARENT;
                added.layout.height = LayoutBean.LAYOUT_WRAP_CONTENT;
                a.a.a.jC.a(scId).a(main.getXmlName(), added);

                EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW,
                        ViewBean.VIEW_TYPE_WIDGET_BUTTON, addedId, "onClick");
                BlockBean setText = new BlockBean("1", "", "", "setText");
                setText.parameters.add(addedId);
                setText.parameters.add("Smoke clicked");
                a.a.a.jC.a(scId).a(main.getJavaName(), click);
                a.a.a.jC.a(scId).a(main.getJavaName(), click.getEventKey(),
                        new ArrayList<>(Collections.singletonList(setText)));
                a.a.a.jC.a(scId).n(a.a.a.wq.b(scId) + File.separator + "view");

                assertThat(findView(a.a.a.jC.a(scId).d(main.getXmlName()), addedId))
                        .isNotNull();
                // Exercise the same lifecycle entry used by the Android Back key.
                activity.onBackPressed();
            });

            Activity live = awaitResumedActivity(CreatorProjectActivity.class, 12000L);
            assertThat(live).isNotNull();
            View canvas = live.findViewById(R.id.creator_preview_canvas);
            assertThat(canvas).isNotNull();
            View addedLive = canvas.findViewWithTag(addedId);
            assertThat(addedLive).isNotNull();
            assertThat(addedLive).isInstanceOf(MaterialButton.class);
            assertThat(((MaterialButton) addedLive).getText().toString())
                    .isEqualTo("Smoke button");
            addedLive.performClick();
            assertThat(((MaterialButton) addedLive).getText().toString())
                    .isEqualTo("Smoke clicked");

            try (ActivityScenario<DesignActivity> reopened = ActivityScenario.launch(editorIntent)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                reopened.onActivity(activity -> {
                    ArrayList<ViewBean> views = a.a.a.jC.a(scId).d("main.xml");
                    ViewBean persisted = findView(views, addedId);
                    assertThat(persisted).isNotNull();
                    assertThat(persisted.text).isNotNull();
                    assertThat(persisted.text.text).isEqualTo("Smoke button");
                    assertThat(findView(views, CreatorRuntimeDefaults.ENTRY_WIDGET_ID))
                            .isNotNull();
                });
            }
        }
    }

    private static ArrayList<ViewBean> awaitLegacyViews(String scId, String xmlName,
                                                         String requiredId, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        ArrayList<ViewBean> latest = null;
        while (System.currentTimeMillis() < deadline) {
            final AtomicReference<ArrayList<ViewBean>> snapshot = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    snapshot.set(a.a.a.jC.a(scId).d(xmlName)));
            latest = snapshot.get();
            if (findView(latest, requiredId) != null) return latest;
            try {
                Thread.sleep(50L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return latest;
            }
        }
        return latest;
    }

    private static ViewBean findView(ArrayList<ViewBean> views, String id) {
        if (views == null) return null;
        for (ViewBean view : views) {
            if (view != null && id.equals(view.id)) return view;
        }
        return null;
    }

    private static Activity awaitResumedActivity(Class<?> activityClass, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<Activity> resumed = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .forEach(candidate -> {
                                if (activityClass.isInstance(candidate)) resumed.set(candidate);
                            }));
            if (resumed.get() != null) return resumed.get();
            try {
                Thread.sleep(50L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
