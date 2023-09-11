/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import cn.touchair.iotooth.central.CentralState;

public interface CentralStateListener {
    void onEvent(CentralState event, Object obj);
    void  onMessage(int offset, byte[] data);
}
