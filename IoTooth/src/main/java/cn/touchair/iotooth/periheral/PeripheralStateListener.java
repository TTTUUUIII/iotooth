/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.periheral;

public interface PeripheralStateListener {
    void onStateChanged(PeripheralState state, Object obj);
    void  onMessage(int offset, byte[] data);

    void onError(PeripheralErrorState errorState);
}
