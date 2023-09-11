/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.configuration;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.UUID;

public class PeripheralConfiguration extends ToothConfiguration {

    public String serviceLocalName = Build.PRODUCT;
    public PeripheralConfiguration(@NonNull String serviceUuid, @NonNull String readonlyUuid, @NonNull String writableUuid) {
        this(UUID.fromString(serviceUuid),
                UUID.fromString(readonlyUuid),
                UUID.fromString(writableUuid));
    }

    public PeripheralConfiguration(@NonNull UUID serviceUuid, @NonNull UUID readableUuid, @NonNull UUID writableUuid) {
        this.serviceUuid = serviceUuid;
        this.readonlyUuid = readableUuid;
        this.writableUuid = writableUuid;
    }
}
