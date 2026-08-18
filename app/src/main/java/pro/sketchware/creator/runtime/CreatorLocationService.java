package pro.sketchware.creator.runtime;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import java.util.Collections;
import java.util.Map;

/** Runtime-native LocationManager component with start/stop updates and explicit permission state. */
public final class CreatorLocationService implements CreatorRuntimeService, LocationListener {
    private final CreatorRuntimeEnvironment environment;
    private final LocationManager locationManager;
    private boolean listening;

    public CreatorLocationService(CreatorRuntimeEnvironment environment) {
        this.environment = environment;
        this.locationManager = (LocationManager) environment.getContext().getSystemService(Context.LOCATION_SERVICE);
    }
    @Override public String getId() { return "location"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (locationManager == null) return CreatorRuntimeServiceArguments.failed("Location service is unavailable.");
        if (!environment.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            environment.requestPermission(getId(), Manifest.permission.ACCESS_FINE_LOCATION);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Precise location permission was requested.");
        }
        if ("start".equals(action)) {
            String provider = CreatorRuntimeServiceArguments.string(arguments, "provider");
            if (provider == null) provider = LocationManager.GPS_PROVIDER;
            if (!locationManager.isProviderEnabled(provider)) {
                return CreatorRuntimeServiceArguments.failed("Requested location provider is disabled: " + provider);
            }
            long interval = CreatorRuntimeServiceArguments.longValue(arguments, "intervalMs", 1000L);
            float distance = CreatorRuntimeServiceArguments.floatValue(arguments, "distanceMeters", 0f);
            locationManager.requestLocationUpdates(provider, interval, distance, this);
            listening = true;
            return CreatorRuntimeServiceArguments.succeeded("listening", true, "provider", provider);
        }
        if ("last_known".equals(action)) {
            String provider = CreatorRuntimeServiceArguments.string(arguments, "provider");
            if (provider == null) provider = LocationManager.GPS_PROVIDER;
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) return CreatorRuntimeServiceArguments.failed("No last known location is available.");
            return CreatorRuntimeServiceArguments.succeeded("latitude", location.getLatitude(),
                    "longitude", location.getLongitude(), "accuracy", location.getAccuracy(), "provider", provider);
        }
        if ("stop".equals(action)) {
            locationManager.removeUpdates(this);
            listening = false;
            return CreatorRuntimeServiceArguments.succeeded("listening", false);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported location action: " + action);
    }

    @Override public void onLocationChanged(Location location) {
        if (!listening || location == null) return;
        environment.publish(getId(), "changed", CreatorRuntimeServiceArguments.output(
                "latitude", location.getLatitude(), "longitude", location.getLongitude(),
                "accuracy", location.getAccuracy(), "provider", location.getProvider(), "time", location.getTime()));
    }
}
