package pro.sketchware.creator.runtime;

import android.content.Context;

/** Creates the Creator Runtime-native service set for a project. */
public final class CreatorRuntimePlugins {
    private CreatorRuntimePlugins() { }

    public static CreatorRuntimePluginDispatcher defaults(Context context, String projectId,
                                                          CreatorTimerPlugin.Listener timerListener) {
        return new CreatorRuntimePluginDispatcher()
                .register(new CreatorStoragePlugin(context, projectId))
                .register(new CreatorVibratorPlugin(context))
                .register(new CreatorCalendarService())
                .register(new CreatorTimerPlugin(timerListener));
    }
}
