package pro.sketchware.creator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** Host-owned shake detector; project widgets cannot disable this recovery path. */
final class CreatorShakeRecovery implements SensorEventListener {
    interface Listener { void onShake(); }

    private static final float SHAKE_THRESHOLD = 15f;
    private static final long DEBOUNCE_MS = 900L;
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Listener listener;
    private long lastShakeAt;

    CreatorShakeRecovery(Context context, Listener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
    }

    void start() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 3) return;
        float force = (float) Math.sqrt(event.values[0] * event.values[0]
                + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
        long now = System.currentTimeMillis();
        if (force >= SHAKE_THRESHOLD && now - lastShakeAt >= DEBOUNCE_MS) {
            lastShakeAt = now;
            listener.onShake();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
