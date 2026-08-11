package com.sketchware.ai.tools;

import android.app.Activity;
import android.content.Context;

import com.besome.sketch.design.DesignActivity;

import java.util.function.Consumer;

/**
 * Context object passed to every tool execution. Encapsulates everything a
 * tool needs to act on the current project.
 *
 * <p>The {@code sc_id} and {@code projectFile} come from the currently
 * active project in {@link DesignActivity}. The activity reference is used
 * for UI-thread dispatch (refreshing the canvas after a tool call).
 */
public class SketchwareToolContext {

    private final Context context;
    private final String scId;
    // Mutable: tools like view_manage_layout(switch_active) and view_manage_layout(create)
    // update this so subsequent view_add_widget / view_set_property calls operate
    // on the newly active layout. Previously this was final, which caused
    // view_add_widget to keep adding widgets to the OLD layout (e.g. 'main')
    // even after the assistant had explicitly switched to a new one.
    private String currentJavaName;
    private final ToolPermissionGate permissionGate;
    /**
     * Accepts the xml layout name the AI just modified. If it differs from
     * what the editor is currently showing, the editor will switch to it.
     * This is the fix for "в окне view не видно то что он сделал": the AI
     * was creating 'calculator' but the editor was still showing 'main'.
     */
    private final Consumer<String> viewRefreshCallback;
    private final Runnable logicRefreshCallback;
    private final Runnable eventRefreshCallback;
    private final Runnable componentRefreshCallback;

    public SketchwareToolContext(Context context,
                                 String scId,
                                 String currentJavaName,
                                 ToolPermissionGate permissionGate,
                                 Consumer<String> viewRefreshCallback,
                                 Runnable logicRefreshCallback,
                                 Runnable eventRefreshCallback,
                                 Runnable componentRefreshCallback) {
        this.context = context;
        this.scId = scId;
        this.currentJavaName = currentJavaName;
        this.permissionGate = permissionGate;
        this.viewRefreshCallback = viewRefreshCallback;
        this.logicRefreshCallback = logicRefreshCallback;
        this.eventRefreshCallback = eventRefreshCallback;
        this.componentRefreshCallback = componentRefreshCallback;
    }

    public Context getContext() { return context; }
    public String getScId() { return scId; }
    public String getCurrentJavaName() { return currentJavaName; }
    public ToolPermissionGate getPermissionGate() { return permissionGate; }

    /**
     * Update the active layout/file name. Called by view_manage_layout when
     * the user/assistant switches or creates a layout, so subsequent tool
     * calls operate on the new active layout.
     */
    public void setCurrentJavaName(String javaName) {
        if (javaName != null && !javaName.isEmpty()) {
            this.currentJavaName = javaName;
        }
    }

    public Activity getActivity() {
        if (context instanceof Activity) return (Activity) context;
        return null;
    }

    public void runOnUiThread(Runnable r) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(r);
    }

    /**
     * Refresh the View editor canvas so changes are visible in real time.
     * Passes the AI's current layout name so DesignActivity can switch the
     * editor to it if the user is viewing a different layout.
     */
    public void refreshViewEditor() {
        if (viewRefreshCallback != null) {
            final String layout = currentJavaName;
            runOnUiThread(() -> viewRefreshCallback.accept(layout));
        }
    }

    /** Refresh the Logic editor canvas. */
    public void refreshLogicEditor() {
        if (logicRefreshCallback != null) {
            runOnUiThread(logicRefreshCallback);
        }
    }

    /** Refresh the Event list. */
    public void refreshEventList() {
        if (eventRefreshCallback != null) {
            runOnUiThread(eventRefreshCallback);
        }
    }

    /** Refresh the Component list. */
    public void refreshComponentList() {
        if (componentRefreshCallback != null) {
            runOnUiThread(componentRefreshCallback);
        }
    }

    /** Refresh all known editors. */
    public void refreshAllEditors() {
        refreshViewEditor();
        refreshLogicEditor();
        refreshEventList();
        refreshComponentList();
    }
}
