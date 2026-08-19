package pro.sketchware.creator.runtime;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.Map;

/** Allow-listed DrawerLayout controls for an imported Sketchware activity drawer. */
public final class CreatorDrawerService implements CreatorRuntimeService {
    private DrawerLayout drawerLayout;

    @Override public String getId() { return "drawer"; }

    public void register(DrawerLayout layout) { drawerLayout = layout; }

    public void clear() { drawerLayout = null; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action == null || action.trim().isEmpty()) {
            return CreatorRuntimeServiceArguments.invalid("Drawer action requires action.");
        }
        if (drawerLayout == null) {
            return CreatorRuntimeServiceArguments.invalid("Runtime DrawerLayout is not available for the active screen.");
        }
        if ("open".equals(action)) {
            drawerLayout.openDrawer(GravityCompat.START);
            return CreatorRuntimeServiceArguments.succeeded("opened", true);
        }
        if ("close".equals(action)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return CreatorRuntimeServiceArguments.succeeded("closed", true);
        }
        if ("is_open".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("value", drawerLayout.isDrawerOpen(GravityCompat.START));
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported drawer action: " + action);
    }
}
