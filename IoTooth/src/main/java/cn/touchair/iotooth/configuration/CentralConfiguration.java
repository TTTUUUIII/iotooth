/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.configuration;

import androidx.annotation.NonNull;

import java.util.UUID;

public class CentralConfiguration extends ToothConfiguration {
    public CentralConfiguration(@NonNull String serviceUuid) {
        this(UUID.fromString(serviceUuid));
    }

    public CentralConfiguration(@NonNull UUID serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
}
