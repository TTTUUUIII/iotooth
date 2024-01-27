/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.util;

import cn.touchair.iotooth.central.CentralState;
import cn.touchair.iotooth.periheral.PeripheralState;

public final class ToothUtils {
    private ToothUtils() {}

    public static String stateToString(CentralState state) {
        switch (state) {
            case CONNECTED:
                return "CONNECTED";
            case OPENED_GATT:
                return "OPEN_GATT";
            case DISCONNECTED:
                return "DISCONNECTED";
            case RSSI_REPORTER:
                return "RSSI_REPORTER";
            default:
                return "UNKNOWN";
        }
    }

    public static String stateToString(PeripheralState state) {
        switch (state) {
            case CONNECTED:
                return "CONNECTED";
            case CONNECTING:
                return "CONNECTING";
            case DISCONNECTED:
                return "DISCONNECTED";
            case ADVERTISING:
                return "ADVERTISING";
            default:
                return "UNKNOWN";
        }
    }
}
