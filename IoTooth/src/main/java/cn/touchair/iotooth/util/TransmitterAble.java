/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.util;

import androidx.annotation.Nullable;

public interface TransmitterAble {
    void send(@Nullable String address, byte[] data);
    void setRxFrameListener(@Nullable RxFrameListener listener);
}
