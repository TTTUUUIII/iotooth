/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import android.bluetooth.le.ScanResult;

public interface ScanResultCallback {
    void onScanStarted();
    void onScanStopped();
    void onScanResult(ScanResult result);
}
