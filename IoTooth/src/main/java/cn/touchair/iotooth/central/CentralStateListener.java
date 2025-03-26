/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import androidx.annotation.NonNull;

public interface CentralStateListener {
    void onStateChanged(CentralState state, @NonNull String address);
    void  onMessage(int offset, byte[] data, @NonNull String address);

    void onMtuChanged(int mtu);

    void onError(CentralErrorState errorState);
}
