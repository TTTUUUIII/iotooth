/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.util;

import androidx.annotation.Nullable;

public interface RxFrameListener {
    void onFrame(int offset, byte[] frame, @Nullable String addr);
}
