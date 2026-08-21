package pro.sketchware.creator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** Host-owned shake detector; project widgets cannot disable this recovery path. */
final class CreatorShakeRecovery implements SensorEventListener {
    interface Listener { void onShake(); }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Listener listener;
    private final CreatorShakeDetector detector = new CreatorShakeDetector();

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
        if (detector.onSample(event.values[0], event.values[1], event.values[2],
                System.currentTimeMillis())) listener.onShake();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
