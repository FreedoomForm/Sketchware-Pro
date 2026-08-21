package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import pro.sketchware.R;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

@RunWith(AndroidJUnit4.class)
public class CreatorRuntimeNavigationTest {
    private Context context;

    @Before public void clearRuntimeState() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("creator_runtime", Context.MODE_PRIVATE).edit().clear().commit();
        CreatorRuntimeSession.resetForTests();
    }

    @Test public void installedLauncherIsCreatorHomeWithSidebar() {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertThat(launchIntent).isNotNull();
        assertThat(launchIntent.getComponent()).isNotNull();
        assertThat(launchIntent.getComponent().getClassName())
                .isEqualTo(CreatorHomeActivity.class.getName());

        try (ActivityScenario<CreatorHomeActivity> scenario = ActivityScenario.launch(CreatorHomeActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat((Object) activity.findViewById(R.id.creator_home_drawer)).isInstanceOf(DrawerLayout.class);
                assertThat((Object) activity.findViewById(R.id.creator_entry_control))
                        .isInstanceOf(FloatingActionButton.class);
                assertThat(activity.findViewById(R.id.creator_entry_control).getVisibility())
                        .isEqualTo(View.VISIBLE);
                assertThat((Object) activity.findViewById(R.id.creator_open_legacy)).isNotNull();
            });
        }
    }

    @Test public void editorSidebarContainsMigratedMainScreenActions() {
        try (ActivityScenario<CreatorProjectActivity> scenario = ActivityScenario.launch(CreatorProjectActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat((Object) activity.findViewById(R.id.creator_project_drawer)).isInstanceOf(DrawerLayout.class);
                int[] actions = {
                        R.id.creator_sidebar_new_project, R.id.creator_sidebar_projects,
                        R.id.creator_sidebar_sketchub, R.id.creator_sidebar_about,
                        R.id.creator_sidebar_changelog, R.id.creator_sidebar_info,
                        R.id.creator_sidebar_keystore, R.id.creator_sidebar_settings,
                        R.id.creator_sidebar_swassist, R.id.creator_sidebar_discord,
                        R.id.creator_sidebar_telegram, R.id.creator_sidebar_github
                };
                for (int id : actions) {
                    assertThat((Object) activity.findViewById(id)).isNotNull();
                }
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
                assertThat((Object) activity.findViewById(R.id.creator_project_entry_control)).isNotNull();
                assertThat(activity.findViewById(R.id.creator_project_entry_control).getVisibility())
                        .isEqualTo(View.VISIBLE);
            });
        }
    }
}
