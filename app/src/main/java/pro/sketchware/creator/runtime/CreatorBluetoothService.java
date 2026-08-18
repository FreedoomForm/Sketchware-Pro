package pro.sketchware.creator.runtime;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import java.util.Collections;
import java.util.Map;

/** Runtime-native Bluetooth availability and enable-request service. */
public final class CreatorBluetoothService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    public CreatorBluetoothService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "bluetooth"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (adapter == null) return CreatorRuntimeServiceArguments.failed("This device does not provide Bluetooth.");
        if (Build.VERSION.SDK_INT >= 31 && !environment.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            environment.requestPermission(getId(), Manifest.permission.BLUETOOTH_CONNECT);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Bluetooth connection permission was requested.");
        }
        if ("status".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("enabled", adapter.isEnabled(), "name", adapter.getName());
        }
        if ("request_enable".equals(action)) {
            if (adapter.isEnabled()) return CreatorRuntimeServiceArguments.succeeded("enabled", true);
            environment.launchForResult(getId(), "enable_result", new android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return CreatorRuntimeServiceArguments.succeeded("started", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported Bluetooth action: " + action);
    }
}
