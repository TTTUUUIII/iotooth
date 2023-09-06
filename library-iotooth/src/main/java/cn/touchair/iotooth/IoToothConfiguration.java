/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */
package cn.touchair.iotooth;

import androidx.annotation.NonNull;

import java.util.UUID;

public class IoToothConfiguration {
    public final UUID advertUuid;
    public final UUID serviceUuid;
    public final UUID readonlyUuid;
    public final UUID writableUuid;

    public IoToothConfiguration(@NonNull String advertUuid, @NonNull String serviceUuid, @NonNull String readonlyUuid, @NonNull String writableUuid) {
        this(UUID.fromString(advertUuid),
                UUID.fromString(serviceUuid),
                UUID.fromString(readonlyUuid),
                UUID.fromString(writableUuid));
    }

    public IoToothConfiguration(@NonNull UUID advertUuid, @NonNull UUID serviceUuid, @NonNull UUID readableUuid, @NonNull UUID writableUuid) {
        this.advertUuid = advertUuid;
        this.serviceUuid = serviceUuid;
        this.readonlyUuid = readableUuid;
        this.writableUuid = writableUuid;
    }
}
