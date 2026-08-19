package com.sketchware.ai.ui;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import com.sketchware.ai.ui.chat.ChatFragment;

/**
 * Native UI smoke test for the AI chat surface.
 *
 * <p>The test deliberately verifies the deterministic local part of the flow:
 * the fragment renders, accepts text, and immediately appends the user row.
 * Provider/network execution is asynchronous and is covered by JVM provider
 * tests; this instrumentation test does not require a real API key or network.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChatFragmentE2ETest {

    @Before public void clearProviderState() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("sketchware_ai_configs", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void chatInputSendAddsUserMessage() throws Throwable {
        FragmentScenario<ChatFragment> scenario =
                FragmentScenario.launchInContainer(
                        ChatFragment.class, null, R.style.Theme_SketchwarePro);

        // Drive the local fragment directly on the main thread rather than
        // relying on Espresso's focused-root picker while the container settles.
        scenario.onFragment(fragment -> {
            View root = fragment.requireView();
            EditText input = root.findViewById(R.id.input);
            input.setText("native smoke test");
            root.findViewById(R.id.btn_send).performClick();
        });

        AtomicReference<Integer> itemCount = new AtomicReference<>(0);
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            scenario.onFragment(fragment -> {
                View root = fragment.getView();
                if (root == null) return;
                RecyclerView recycler = root.findViewById(R.id.recycler);
                if (recycler != null && recycler.getAdapter() != null) {
                    itemCount.set(recycler.getAdapter().getItemCount());
                }
            });
            if (itemCount.get() >= 1) break;
            Thread.sleep(250L);
        }

        assertThat(itemCount.get()).isAtLeast(1);
        scenario.close();
    }
}
