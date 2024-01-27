/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

public enum CentralState {
    OPENED_GATT, /*已连接到GATT*/
    CONNECTED, /*已连接*/
    DISCONNECTED, /*已断开*/
    RSSI_REPORTER
}
