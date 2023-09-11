/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */
package cn.touchair.iotooth.configuration;


import java.util.UUID;

public abstract class ToothConfiguration {
    public UUID serviceUuid;
    public UUID readonlyUuid;
    public UUID writableUuid;
}
