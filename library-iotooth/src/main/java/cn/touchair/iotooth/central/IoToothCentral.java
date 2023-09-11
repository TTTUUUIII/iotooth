/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import cn.touchair.iotooth.configuration.CentralConfiguration;

public class IoToothCentral extends ScanCallback implements GattCallbackImpl.GattStateListener {
    private static final String TAG = IoToothCentral.class.getSimpleName();
    private Context mContext;
    private CentralConfiguration mConfiguration;
    private CentralStateListener mListener;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mLeScanner;
    private Handler mH = new Handler(Looper.myLooper());

    private HashMap<String, GattCallbackImpl> mGattHandlersMap = new HashMap<>();
    private BluetoothDevice mRemote;

    public IoToothCentral(@NonNull Context context, @NonNull CentralStateListener listener) {
        mContext = context;
        mListener = listener;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mLeScanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
    }

    public void startWithConfiguration(@NonNull CentralConfiguration configuration) {
        mConfiguration = configuration;
        scanService();
    }

    public void stop() {
        mGattHandlersMap.forEach((address, handler) -> {
            handler.close();
        });
    }

    public void send(@NonNull String address, String msg) {
        GattCallbackImpl handler = mGattHandlersMap.get(address);
        if (handler != null) {
            handler.send(msg);
        }
    }

    public void send(@NonNull String address, byte[] data) {
        GattCallbackImpl handler = mGattHandlersMap.get(address);
        if (handler != null) {
            handler.send(data);
        }
    }

    @SuppressLint("MissingPermission")
    private void scanService() {
        mListener.onEvent(CentralState.SCANNING, null);
        ArrayList<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(mConfiguration.serviceUuid))
                .build();
        filters.add(filter);
        ScanSettings settings = new ScanSettings.Builder().build();
        mLeScanner.startScan(filters, settings, this);
        mH.postDelayed(() -> {
            stopScanService();
            if (mRemote == null) {
                Log.w(TAG, "Remote service not found!");
            }
        }, 5000);
    }

    @SuppressLint("MissingPermission")
    private void stopScanService() {
        mLeScanner.stopScan(this);
    }

    @SuppressLint("MissingPermission")
    private void openGatt() {
        assert mRemote != null : "Bluetooth gatt is null!";
        GattCallbackImpl handler = new GattCallbackImpl(mConfiguration, mListener);
        BluetoothGatt gatt = mRemote.connectGatt(mContext, false, handler);
        if (gatt != null) {
            mGattHandlersMap.put(mRemote.getAddress(), handler);
            mListener.onEvent(CentralState.OPENED_GATT, gatt.getDevice().getAddress());
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        mRemote = result.getDevice();
        stopScanService();
        mH.postDelayed(this::openGatt, 3000);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.e(TAG, "Scan failed. errorCode=" + errorCode);
    }

    @Override
    public void onEvent(@NonNull CentralState event, @Nullable Object obj) {
        mListener.onEvent(event, obj);
    }
}
