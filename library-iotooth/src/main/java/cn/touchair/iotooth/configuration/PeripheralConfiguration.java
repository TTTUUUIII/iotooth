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
    public PeripheralConfiguration(@NonNull String serviceUuid) {
        this("TOOTH",
                UUID.fromString(serviceUuid));
    }

    public PeripheralConfiguration(@NonNull String localName, @NonNull String serviceUuid) {
        this(localName,
                UUID.fromString(serviceUuid));
    }

    public PeripheralConfiguration(@NonNull String localName ,@NonNull UUID serviceUuid) {
        this.serviceLocalName = localName;
        this.serviceUuid = serviceUuid;
    }
}
