package com.sketchware.ai.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import pro.sketchware.R;

/**
 * AI Settings root activity. Mirrors the layout of {@code SettingsActivity}
 * from Sketchware-Pro: a thin shell that switches fragments by tag.
 *
 * <p>Each fragment corresponds to one of the 4 Kilo Code-style settings
 * pages: API Configuration / Advanced / Auto-Approve / Experimental.
 */
public final class AISettingsActivity extends AppCompatActivity {

    public static final String EXTRA_FRAGMENT = "fragment";
    public static final String FRAGMENT_PROVIDER = "provider";
    public static final String FRAGMENT_ADVANCED = "advanced";
    public static final String FRAGMENT_AUTO_APPROVE = "auto_approve";
    public static final String FRAGMENT_EXPERIMENTAL = "experimental";

    public static Intent newIntent(Context ctx, String fragmentTag) {
        Intent i = new Intent(ctx, AISettingsActivity.class);
        if (fragmentTag != null) i.putExtra(EXTRA_FRAGMENT, fragmentTag);
        return i;
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            DrawerLayout drawer = findViewById(R.id.drawer);
            if (drawer != null && drawer.isDrawerOpen(findViewById(R.id.nav))) {
                drawer.closeDrawer(findViewById(R.id.nav));
            } else {
                finish();
            }
        });

        NavigationView nav = findViewById(R.id.nav);
        nav.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            String tag;
            if (id == R.id.nav_ai_provider) tag = FRAGMENT_PROVIDER;
            else if (id == R.id.nav_ai_advanced) tag = FRAGMENT_ADVANCED;
            else if (id == R.id.nav_ai_auto_approve) tag = FRAGMENT_AUTO_APPROVE;
            else if (id == R.id.nav_ai_experimental) tag = FRAGMENT_EXPERIMENTAL;
            else tag = FRAGMENT_PROVIDER;
            switchTo(tag);
            DrawerLayout drawer = findViewById(R.id.drawer);
            if (drawer != null) drawer.closeDrawer(nav);
            return true;
        });

        // Default fragment
        String initial = getIntent().getStringExtra(EXTRA_FRAGMENT);
        if (initial == null) initial = FRAGMENT_PROVIDER;
        switchTo(initial);
    }

    private void switchTo(String tag) {
        androidx.fragment.app.Fragment f;
        switch (tag) {
            case FRAGMENT_ADVANCED:     f = new AdvancedSettingsFragment(); break;
            case FRAGMENT_AUTO_APPROVE:  f = new AutoApproveFragment(); break;
            case FRAGMENT_EXPERIMENTAL:  f = new ExperimentalFragment(); break;
            case FRAGMENT_PROVIDER:
            default:                     f = new ApiConfigurationFragment(); break;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, f)
                .commit();
    }
}
