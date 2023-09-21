/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.configuration;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.UUID;

public class PeripheralConfiguration extends ToothConfiguration {

    public String serviceLocalName = Build.PRODUCT;
    public PeripheralConfiguration(@NonNull String serviceUuid, @NonNull String readonlyUuid, @NonNull String writableUuid) {
        this("TOOTH",
                UUID.fromString(serviceUuid),
                UUID.fromString(readonlyUuid),
                UUID.fromString(writableUuid));
    }

    public PeripheralConfiguration(@NonNull String localName, @NonNull String serviceUuid, @NonNull String readonlyUuid, @NonNull String writableUuid) {
        this(localName,
                UUID.fromString(serviceUuid),
                UUID.fromString(readonlyUuid),
                UUID.fromString(writableUuid));
    }

    public PeripheralConfiguration(@NonNull String localName ,@NonNull UUID serviceUuid, @NonNull UUID readableUuid, @NonNull UUID writableUuid) {
        this.serviceLocalName = localName;
        this.serviceUuid = serviceUuid;
        this.readonlyUuid = readableUuid;
        this.writableUuid = writableUuid;
    }
}
