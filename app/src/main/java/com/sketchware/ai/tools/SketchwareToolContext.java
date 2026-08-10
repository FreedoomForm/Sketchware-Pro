package com.sketchware.ai.tools;

import android.app.Activity;
import android.content.Context;

import com.besome.sketch.design.DesignActivity;

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
    private final String currentJavaName; // active layout/file, e.g. "main"
    private final ToolPermissionGate permissionGate;
    private final Runnable viewRefreshCallback;
    private final Runnable logicRefreshCallback;
    private final Runnable eventRefreshCallback;
    private final Runnable componentRefreshCallback;

    public SketchwareToolContext(Context context,
                                 String scId,
                                 String currentJavaName,
                                 ToolPermissionGate permissionGate,
                                 Runnable viewRefreshCallback,
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

    public Activity getActivity() {
        if (context instanceof Activity) return (Activity) context;
        return null;
    }

    public void runOnUiThread(Runnable r) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(r);
    }

    /** Refresh the View editor canvas so changes are visible in real time. */
    public void refreshViewEditor() {
        if (viewRefreshCallback != null) {
            runOnUiThread(viewRefreshCallback);
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
