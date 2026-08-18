package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable allow-list dispatcher; project data cannot load arbitrary Android code. */
public final class CreatorRuntimePluginDispatcher {
    private final Map<String, CreatorRuntimePlugin> plugins = new LinkedHashMap<>();

    public CreatorRuntimePluginDispatcher register(CreatorRuntimePlugin plugin) {
        if (plugin == null || plugin.getId() == null || plugin.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("plugin");
        }
        plugins.put(plugin.getId(), plugin);
        return this;
    }

    public CreatorRuntimePlugin.Result dispatch(String id, Map<String, Object> arguments) {
        CreatorRuntimePlugin plugin = plugins.get(id);
        if (plugin == null) {
            return new CreatorRuntimePlugin.Result(CreatorRuntimePlugin.Status.UNSUPPORTED_ARGUMENT,
                    Collections.<String, Object>emptyMap(), "No reviewed runtime plugin is registered for " + id + ".");
        }
        return plugin.execute(arguments == null ? Collections.<String, Object>emptyMap() : arguments);
    }
}
