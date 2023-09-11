/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.configuration;

import androidx.annotation.NonNull;

import java.util.UUID;

public class CentralConfiguration extends ToothConfiguration {
    public CentralConfiguration(@NonNull String serviceUuid, @NonNull String readonlyUuid, @NonNull String writableUuid) {
        this(UUID.fromString(serviceUuid),
                UUID.fromString(readonlyUuid),
                UUID.fromString(writableUuid));
    }

    public CentralConfiguration(@NonNull UUID serviceUuid, @NonNull UUID readableUuid, @NonNull UUID writableUuid) {
        this.serviceUuid = serviceUuid;
        this.readonlyUuid = readableUuid;
        this.writableUuid = writableUuid;
    }
}
