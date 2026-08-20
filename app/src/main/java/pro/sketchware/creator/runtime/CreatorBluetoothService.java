package pro.sketchware.creator.runtime;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime-native Bluetooth availability and enable-request service. */
public final class CreatorBluetoothService implements CreatorRuntimeService {
    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final CreatorRuntimeEnvironment environment;
    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private final Map<String, BluetoothSocket> sockets = new ConcurrentHashMap<>();
    private final Map<String, BluetoothServerSocket> servers = new ConcurrentHashMap<>();

    public CreatorBluetoothService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "bluetooth"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action == null) return CreatorRuntimeServiceArguments.invalid("Bluetooth action is required.");
        if ("random_uuid".equals(action)) return CreatorRuntimeServiceArguments.succeeded("uuid", UUID.randomUUID().toString());
        if (!("status".equals(action) || "request_enable".equals(action) || "paired_devices".equals(action)
                || "ready_connection".equals(action) || "start_connection".equals(action)
                || "stop_connection".equals(action) || "send_data".equals(action))) {
            return CreatorRuntimeServiceArguments.invalid("Unsupported Bluetooth action: " + action);
        }
        String tag = CreatorRuntimeServiceArguments.string(arguments, "tag");
        if ("ready_connection".equals(action) && tag == null) {
            return CreatorRuntimeServiceArguments.invalid("ready_connection requires tag.");
        }
        if ("start_connection".equals(action)
                && (tag == null || CreatorRuntimeServiceArguments.string(arguments, "address") == null)) {
            return CreatorRuntimeServiceArguments.invalid("start_connection requires address and tag.");
        }
        if ("stop_connection".equals(action) && tag == null) {
            return CreatorRuntimeServiceArguments.invalid("stop_connection requires tag.");
        }
        if ("send_data".equals(action)
                && (tag == null || CreatorRuntimeServiceArguments.string(arguments, "data") == null)) {
            return CreatorRuntimeServiceArguments.invalid("send_data requires data and tag.");
        }
        if ("status".equals(action) && adapter == null) {
            return CreatorRuntimeServiceArguments.succeeded("activated", false, "enabled", false, "name", "");
        }
        if (adapter == null) return CreatorRuntimeServiceArguments.failed("This device does not provide Bluetooth.");
        if (Build.VERSION.SDK_INT >= 31 && !environment.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            environment.requestPermission(getId(), Manifest.permission.BLUETOOTH_CONNECT);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Bluetooth connection permission was requested.");
        }
        if ("status".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("activated", true, "enabled", adapter.isEnabled(), "name", adapter.getName());
        }
        if ("request_enable".equals(action)) {
            if (adapter.isEnabled()) return CreatorRuntimeServiceArguments.succeeded("enabled", true);
            environment.launchForResult(getId(), "enable_result", new android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return CreatorRuntimeServiceArguments.succeeded("started", true);
        }
        if ("paired_devices".equals(action)) {
            ArrayList<Object> devices = new ArrayList<>();
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("name", device.getName());
                descriptor.put("address", device.getAddress());
                devices.add(descriptor);
            }
            return CreatorRuntimeServiceArguments.succeeded("devices", devices);
        }
        if ("ready_connection".equals(action)) {
            UUID uuid = uuid(arguments);
            startServer(tag, uuid);
            return CreatorRuntimeServiceArguments.succeeded("tag", tag, "listening", true);
        }
        if ("start_connection".equals(action)) {
            String address = CreatorRuntimeServiceArguments.string(arguments, "address");
            UUID uuid = uuid(arguments);
            startClient(tag, address, uuid);
            return CreatorRuntimeServiceArguments.succeeded("tag", tag, "connecting", true);
        }
        if ("stop_connection".equals(action)) {
            stop(tag, true);
            return CreatorRuntimeServiceArguments.succeeded("tag", tag, "stopped", true);
        }
        if ("send_data".equals(action)) {
            String data = CreatorRuntimeServiceArguments.string(arguments, "data");
            BluetoothSocket socket = sockets.get(tag);
            if (socket == null || !socket.isConnected()) {
                publish("error", tag, "not_connected", "Bluetooth is not connected yet.");
                return CreatorRuntimeServiceArguments.failed("Bluetooth is not connected yet.");
            }
            try {
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                socket.getOutputStream().write(bytes);
                socket.getOutputStream().flush();
                environment.publish(getId(), "data_sent", CreatorRuntimeServiceArguments.output("tag", tag, "data", data));
                return CreatorRuntimeServiceArguments.succeeded("tag", tag, "sent", true);
            } catch (IOException error) {
                publish("error", tag, "io_error", message(error));
                return CreatorRuntimeServiceArguments.failed(message(error));
            }
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported Bluetooth action: " + action);
    }

    private UUID uuid(Map<String, Object> arguments) {
        String raw = CreatorRuntimeServiceArguments.string(arguments, "uuid");
        if (raw == null) return DEFAULT_UUID;
        try { return UUID.fromString(raw); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Bluetooth UUID is invalid."); }
    }

    private void startServer(final String tag, final UUID uuid) {
        stop(tag, false);
        new Thread(() -> {
            try {
                BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord(tag, uuid);
                servers.put(tag, server);
                BluetoothSocket socket = server.accept();
                servers.remove(tag);
                close(server);
                attach(tag, socket);
            } catch (IOException error) {
                if (servers.remove(tag) != null) publish("error", tag, "listen_error", message(error));
            }
        }, "creator-bluetooth-listen").start();
    }

    private void startClient(final String tag, final String address, final UUID uuid) {
        stop(tag, false);
        new Thread(() -> {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(address);
                adapter.cancelDiscovery();
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                attach(tag, socket);
            } catch (IllegalArgumentException | IOException error) {
                publish("error", tag, "connect_error", message(error));
            }
        }, "creator-bluetooth-connect").start();
    }

    private void attach(final String tag, final BluetoothSocket socket) throws IOException {
        sockets.put(tag, socket);
        BluetoothDevice device = socket.getRemoteDevice();
        environment.publish(getId(), "connected", CreatorRuntimeServiceArguments.output("tag", tag,
                "name", device == null ? null : device.getName(), "address", device == null ? null : device.getAddress()));
        new Thread(() -> receive(tag, socket), "creator-bluetooth-read").start();
    }

    private void receive(String tag, BluetoothSocket socket) {
        try {
            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                String data = new String(buffer, 0, count, StandardCharsets.UTF_8);
                environment.publish(getId(), "data_received", CreatorRuntimeServiceArguments.output("tag", tag, "data", data, "bytes", count));
            }
            stop(tag, true);
        } catch (IOException error) {
            if (sockets.remove(tag, socket)) publish("error", tag, "read_error", message(error));
            close(socket);
        }
    }

    private void stop(String tag, boolean notify) {
        BluetoothSocket socket = sockets.remove(tag);
        BluetoothServerSocket server = servers.remove(tag);
        close(socket);
        close(server);
        if (notify) environment.publish(getId(), "stopped", CreatorRuntimeServiceArguments.output("tag", tag));
    }

    private void publish(String event, String tag, String state, String detail) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output("tag", tag, "state", state, "message", detail));
    }

    private static String message(Exception error) {
        return error.getMessage() == null ? "Bluetooth operation failed." : error.getMessage();
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable == null) return;
        try { closeable.close(); } catch (IOException ignored) { }
    }
}
