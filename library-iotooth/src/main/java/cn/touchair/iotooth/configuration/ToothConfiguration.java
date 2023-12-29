/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */
package cn.touchair.iotooth.configuration;


import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

public abstract class ToothConfiguration {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final byte[] PERIPHERAL_CLOSED = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIG,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    public UUID serviceUuid;
    public UUID readonlyUuid = UUID.fromString("87ada2da-e829-4f2f-b884-acb3b67485e3");
    public UUID writableUuid = UUID.fromString("599ca5b0-5e81-4e8e-b404-1694001e52a6");
}
