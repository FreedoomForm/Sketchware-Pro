# Native Bluetooth UUID Batch Plan

## Scope

This incremental batch adds native emulator evidence for the hardware-independent Bluetooth UUID reporter path while leaving hardware-dependent Bluetooth status, permission, RFCOMM transport, and paired-device behavior explicitly open. The action is an allow-listed `CreatorBluetoothService` call and does not require generated Java, plugin execution, or R2/R3 fallback.

| Capability | Typed action | Acceptance evidence |
|---|---|---|
| Bluetooth random UUID | `random_uuid` | Native emulator dispatches the real production event binding and verifies a non-empty UUID in Creator Runtime state |
| Legacy reporter | `bluetoothConnectGetRandomUuid` | Existing executor/importer path remains typed and JVM-covered |
| Hardware status | `status` | Separate device gate remains open because adapter availability and Android permission state vary by emulator/device |
| RFCOMM transport | `ready_connection`, `start_connection`, `send_data`, `stop_connection` | Separate device gate remains open pending Bluetooth hardware/permission evidence |

## Native acceptance test

`CreatorRuntimeNativeWidgetTest` persists a fixture through the `creator_runtime` production store, launches `CreatorProjectActivity`, and invokes a real button bound to the `bluetooth/random_uuid` service. The assertion checks the resulting runtime state rather than a generated source artifact.

## Fallback policy

Unavailable Bluetooth hardware must remain an explicit typed service result. This slice does not add a fallback transport or hidden host execution path. `addSourceDirectly` remains a visible R0 blocked exception and R2/R3 execution tiers remain prohibited.
