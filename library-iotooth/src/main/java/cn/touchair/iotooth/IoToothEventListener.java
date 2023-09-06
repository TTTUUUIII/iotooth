/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth;

public interface IoToothEventListener {
    void onEvent(PeripheralEvent event, Object obj);
    void  onNext(int offset, byte[] data);
}
