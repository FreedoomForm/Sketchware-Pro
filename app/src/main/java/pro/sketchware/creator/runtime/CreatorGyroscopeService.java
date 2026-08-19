package pro.sketchware.creator.runtime;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.Map;

/** Runtime-native gyroscope subscription service with explicit start/stop lifecycle. */
public final class CreatorGyroscopeService implements CreatorRuntimeService, SensorEventListener {
    private final CreatorRuntimeEnvironment environment;
    private final SensorManager sensorManager;
    private final Sensor gyroscope;
    private boolean listening;

    public CreatorGyroscopeService(CreatorRuntimeEnvironment environment) {
        this.environment = environment;
        this.sensorManager = (SensorManager) environment.getContext().getSystemService(Context.SENSOR_SERVICE);
        this.gyroscope = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override public String getId() { return "gyroscope"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("start".equals(action)) {
            if (sensorManager == null || gyroscope == null) {
                return CreatorRuntimeServiceArguments.failed("This device does not provide a gyroscope sensor.");
            }
            int delay = (int) CreatorRuntimeServiceArguments.longValue(arguments, "delay", SensorManager.SENSOR_DELAY_NORMAL);
            listening = sensorManager.registerListener(this, gyroscope, delay);
            return listening ? CreatorRuntimeServiceArguments.succeeded("listening", true)
                    : CreatorRuntimeServiceArguments.failed("Gyroscope listener could not start.");
        }
        if ("stop".equals(action)) {
            if (sensorManager != null) sensorManager.unregisterListener(this);
            listening = false;
            return CreatorRuntimeServiceArguments.succeeded("listening", false);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported gyroscope action: " + action);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!listening || event == null || event.values == null || event.values.length < 3) return;
        environment.publish(getId(), "changed", CreatorRuntimeServiceArguments.output(
                "x", event.values[0], "y", event.values[1], "z", event.values[2],
                "timestamp", event.timestamp));
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        environment.publish(getId(), "accuracy_changed", CreatorRuntimeServiceArguments.output("accuracy", accuracy));
    }
}
