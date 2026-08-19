package pro.sketchware.creator.runtime;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Allow-listed native Google Maps actions for rendered Creator Runtime MapView widgets. */
public final class CreatorMapService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public CreatorMapService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "map"; }

    public void register(String widgetId, MapView view) {
        if (widgetId == null || view == null) return;
        Entry entry = new Entry(widgetId, view);
        entries.put(widgetId, entry);
        view.getMapAsync(map -> {
            entry.map = map;
            List<Runnable> pending = new ArrayList<>(entry.pending);
            entry.pending.clear();
            for (Runnable action : pending) action.run();
            environment.publish(getId(), "ready", CreatorRuntimeServiceArguments.output("widgetId", widgetId));
        });
    }

    @Override public Result execute(Map<String, Object> arguments) {
        String widgetId = CreatorRuntimeServiceArguments.string(arguments, "widgetId");
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (widgetId == null || action == null) return CreatorRuntimeServiceArguments.invalid("Map action requires widgetId and action.");
        Entry entry = entries.get(widgetId);
        if (entry == null) return CreatorRuntimeServiceArguments.invalid("Runtime MapView is not available: " + widgetId);
        Runnable operation = operation(entry, action, arguments);
        if (operation == null) return CreatorRuntimeServiceArguments.invalid("Unsupported map action: " + action);
        if (entry.map == null) {
            entry.pending.add(operation);
            return CreatorRuntimeServiceArguments.succeeded("queued", true, "widgetId", widgetId, "action", action);
        }
        operation.run();
        return CreatorRuntimeServiceArguments.succeeded("updated", true, "widgetId", widgetId, "action", action);
    }

    private Runnable operation(Entry entry, String action, Map<String, Object> arguments) {
        if ("set_map_type".equals(action)) {
            final int type = mapType(CreatorRuntimeServiceArguments.string(arguments, "mapType"));
            return () -> entry.map.setMapType(type);
        }
        if ("move_camera".equals(action)) {
            final double lat = number(arguments.get("latitude"));
            final double lng = number(arguments.get("longitude"));
            return () -> entry.map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        }
        if ("zoom_to".equals(action)) {
            final float zoom = (float) number(arguments.get("zoom"));
            return () -> entry.map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
        }
        if ("zoom_in".equals(action)) return () -> entry.map.moveCamera(CameraUpdateFactory.zoomIn());
        if ("zoom_out".equals(action)) return () -> entry.map.moveCamera(CameraUpdateFactory.zoomOut());
        final String markerId = CreatorRuntimeServiceArguments.string(arguments, "markerId");
        if (markerId == null || markerId.trim().isEmpty()) return null;
        if ("add_marker".equals(action)) {
            final double lat = number(arguments.get("latitude"));
            final double lng = number(arguments.get("longitude"));
            return () -> {
                Marker existing = entry.markers.remove(markerId);
                if (existing != null) existing.remove();
                Marker marker = entry.map.addMarker(new MarkerOptions().position(new LatLng(lat, lng)));
                if (marker != null) marker.setTag(markerId);
                if (marker != null) entry.markers.put(markerId, marker);
            };
        }
        if ("set_marker_info".equals(action)) {
            final String title = CreatorRuntimeServiceArguments.string(arguments, "title");
            final String snippet = CreatorRuntimeServiceArguments.string(arguments, "snippet");
            return () -> withMarker(entry, markerId, marker -> { marker.setTitle(title); marker.setSnippet(snippet); });
        }
        if ("set_marker_position".equals(action)) {
            final double lat = number(arguments.get("latitude"));
            final double lng = number(arguments.get("longitude"));
            return () -> withMarker(entry, markerId, marker -> marker.setPosition(new LatLng(lat, lng)));
        }
        if ("set_marker_color".equals(action)) {
            final float color = (float) number(arguments.get("color"));
            final float alpha = (float) number(arguments.get("alpha"));
            return () -> withMarker(entry, markerId, marker -> { marker.setAlpha(alpha); marker.setIcon(BitmapDescriptorFactory.defaultMarker(color)); });
        }
        if ("set_marker_icon".equals(action)) {
            final String resourceName = CreatorRuntimeServiceArguments.string(arguments, "resourceName");
            return () -> {
                int resourceId = resourceName == null ? 0 : environment.getContext().getResources().getIdentifier(
                        resourceName.replace(".9", "").toLowerCase(java.util.Locale.ROOT), "drawable",
                        environment.getContext().getPackageName());
                if (resourceId != 0) withMarker(entry, markerId, marker -> marker.setIcon(BitmapDescriptorFactory.fromResource(resourceId)));
            };
        }
        if ("set_marker_visible".equals(action)) {
            final boolean visible = booleanValue(arguments.get("visible"));
            return () -> withMarker(entry, markerId, marker -> marker.setVisible(visible));
        }
        return null;
    }

    private static void withMarker(Entry entry, String id, MarkerAction action) {
        Marker marker = entry.markers.get(id);
        if (marker != null) action.run(marker);
    }

    private static int mapType(String type) {
        if ("MAP_TYPE_SATELLITE".equals(type)) return GoogleMap.MAP_TYPE_SATELLITE;
        if ("MAP_TYPE_TERRAIN".equals(type)) return GoogleMap.MAP_TYPE_TERRAIN;
        if ("MAP_TYPE_HYBRID".equals(type)) return GoogleMap.MAP_TYPE_HYBRID;
        if ("MAP_TYPE_NONE".equals(type)) return GoogleMap.MAP_TYPE_NONE;
        try { return type == null ? GoogleMap.MAP_TYPE_NORMAL : Integer.parseInt(type); }
        catch (NumberFormatException ignored) { return GoogleMap.MAP_TYPE_NORMAL; }
    }

    private static double number(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return value == null ? 0d : Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0d; }
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    private interface MarkerAction { void run(Marker marker); }
    private static final class Entry {
        final String widgetId;
        final MapView view;
        final Map<String, Marker> markers = new LinkedHashMap<>();
        final List<Runnable> pending = new ArrayList<>();
        GoogleMap map;
        Entry(String widgetId, MapView view) { this.widgetId = widgetId; this.view = view; }
    }
}
