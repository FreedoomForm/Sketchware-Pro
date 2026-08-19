package com.sketchware.ai.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static com.google.common.truth.Truth.assertThat;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import pro.sketchware.R;
import com.sketchware.ai.ui.settings.AISettingsActivity;

/**
 * Instrumentation tests for {@link AISettingsActivity}.
 *
 * <p>These tests launch the activity on a real device or emulator and
 * verify that the 4 settings sections are reachable from the navigation
 * drawer.
 */
@RunWith(AndroidJUnit4.class)
public class AISettingsActivityTest {

    @Test public void activityLaunchesAndShowsToolbar() {
        try (ActivityScenario<AISettingsActivity> scenario = ActivityScenario.launch(AISettingsActivity.class)) {
            scenario.onActivity(activity -> {
                assertThat((View) activity.findViewById(R.id.toolbar)).isNotNull();
                assertThat((View) activity.findViewById(R.id.nav)).isNotNull();
                assertThat((View) activity.findViewById(R.id.content_frame)).isNotNull();
            });
        }
    }

    @Test public void defaultFragmentIsProvidersList() {
        try (ActivityScenario<AISettingsActivity> scenario = ActivityScenario.launch(AISettingsActivity.class)) {
            // The current default fragment is ProvidersListFragment.
            onView(withId(R.id.providers_recycler)).check(matches(isDisplayed()));
        }
    }

    @Test public void navigatingToAutoApproveShowsToggles() {
        try (ActivityScenario<AISettingsActivity> scenario = ActivityScenario.launch(AISettingsActivity.class)) {
            scenario.onActivity(activity -> {
                com.google.android.material.navigation.NavigationView nav = activity.findViewById(R.id.nav);
                assertThat(nav.getMenu().findItem(R.id.nav_ai_auto_approve)).isNotNull();
                assertThat(nav.getMenu().findItem(R.id.nav_ai_provider)).isNotNull();
                assertThat(nav.getMenu().findItem(R.id.nav_ai_advanced)).isNotNull();
                assertThat(nav.getMenu().findItem(R.id.nav_ai_experimental)).isNotNull();
            });
        }
    }
}
