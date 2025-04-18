/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.touchair.iotooth.GlobalConfig;
import cn.touchair.iotooth.configuration.CentralConfiguration;
import cn.touchair.iotooth.util.RxFrameListener;
import cn.touchair.iotooth.util.TransmitterAble;

public class IoToothCentral extends ScanCallback implements CentralStateListener, TransmitterAble {
    private static final String TAG = IoToothCentral.class.getSimpleName();
    private static final int MSG_WHAT_STOP_SCAN = 0;
    private final Context mContext;
    private final CentralConfiguration mConfiguration;
    private final List<CentralStateListener> mListeners = new ArrayList<>();
    private final BluetoothManager mBluetoothManager;
    private final BluetoothLeScanner mLeScanner;
    private ScanResultCallback mScanCallback;
    private RxFrameListener mRxFrameListener;
    private String mServiceUuid;

    private final Handler mH = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_WHAT_STOP_SCAN:
                    stopScanService(true);
                    break;
                default:
            }
        }
    };

    private final Map<String, GattCallbackImpl> mGattHandlersMap = new ConcurrentHashMap<>();

    @SuppressLint("MissingPermission")
    private IoToothCentral(@NonNull Context context, @Nullable CentralStateListener listener, @NonNull CentralConfiguration configuration) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(context);
        mContext = context;
        if (Objects.nonNull(listener)) {
            addEventListener(listener);
        }
        mConfiguration = configuration;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mLeScanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (!adapter.isEnabled() && !adapter.enable()) {
            Log.d(TAG, "Bluetooth not enable.");
        }
    }

    public synchronized void addEventListener(CentralStateListener listener) {
        if (mListeners.contains(listener)) return;
        mListeners.add(listener);
    }

    public synchronized void removeEventListener(CentralStateListener listener) {
        mListeners.remove(listener);
    }

    public void connect(@NonNull BluetoothDevice remote) {
        Objects.requireNonNull(remote);
        stopScanService(false);
        openGatt(remote);
    }

    public void requestMtu(String addr, int mtu) {
        GattCallbackImpl handler = mGattHandlersMap.get(addr);
        if (handler != null) {
            handler.requestMtu(mtu);
        }
    }

    public void scanWithDuration(long mills, @NonNull ScanResultCallback callback, @NonNull String serviceUuid) {
        scan(callback, serviceUuid);
        mH.sendEmptyMessageDelayed(MSG_WHAT_STOP_SCAN,
                mills);
    }

    @TestOnly
    public void scan( ScanResultCallback callback, @NonNull String serviceUUid) {
        mScanCallback = callback;
        mServiceUuid = serviceUUid;
        scanService(new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(serviceUUid))
                .build());
    }

    public void stopScan() {
        stopScanService(true);
        mScanCallback = null;
    }

    public void disconnect(@NonNull String addr) {
        GattCallbackImpl handler = mGattHandlersMap.remove(addr);
        if (handler != null) {
            handler.close();
        }
    }

    public void disconnectAll() {
        mGattHandlersMap.forEach((address, handler) -> {
            handler.close();
        });
        mGattHandlersMap.clear();
        mListeners.clear();
    }

    public void send(@NonNull String address, String msg) {
        GattCallbackImpl handler = mGattHandlersMap.get(address);
        if (handler != null) {
            handler.send(msg);
        }
    }

    @Override
    public void send(@Nullable String address, byte[] data) {
        GattCallbackImpl handler = mGattHandlersMap.get(address);
        if (handler != null) {
            handler.send(data);
        }
    }

    @Override
    public void setRxFrameListener(@Nullable RxFrameListener listener) {
        mRxFrameListener = listener;
    }

    private synchronized void dispatchState(CentralState event, String addr) {
        mListeners.forEach(listener -> {
            try {
                listener.onStateChanged(event, addr);
            } catch (Exception exception) {
                Log.w(TAG, "Listener dead.");
            }
        });
    }

    private synchronized void dispatchMessage(int offset, byte[] data, String addr) {
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, "dispatchMessage: " + Arrays.toString(data));
        }
        mListeners.forEach(listener -> {
            try {
                listener.onMessage(offset, data, addr);
                if (Objects.nonNull(mRxFrameListener)) {
                    mRxFrameListener.onFrame(offset, data, addr);
                }
            } catch (Exception exception) {
                Log.w(TAG, "Listener dead.");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void scanService(@Nullable ScanFilter filter) {
        mScanCallback.onScanStarted();
        if (Objects.isNull(filter)) {
            mLeScanner.startScan(this);
        } else {
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mLeScanner.startScan(filters, settings, this);
        }
    }

    @SuppressLint("MissingPermission")
    private void stopScanService(boolean notify) {
        mLeScanner.stopScan(this);
        if (notify) {
            mScanCallback.onScanStopped();
        }
    }

    @SuppressLint("MissingPermission")
    private void openGatt(@NonNull BluetoothDevice device) {
        GattCallbackImpl handler = new GattCallbackImpl(mServiceUuid, this);
        BluetoothGatt gatt = device.connectGatt(mContext, false, handler);
        if (gatt != null) {
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, String.format("BluetoothDevice: {name: %s, address: %s} open gatt => ok.", device.getName(), device.getAddress()));
            }
            mGattHandlersMap.put(device.getAddress(), handler);
            dispatchState(CentralState.OPENED_GATT, gatt.getDevice().getAddress());
        } else {
            Log.d(TAG, "Unable open gatt");
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        mScanCallback.onScanResult(result);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.e(TAG, "Scan failed. errorCode=" + errorCode);
    }

    @Override
    public void onStateChanged(CentralState event, @NonNull String address) {
        if (event == CentralState.DISCONNECTED) {
            GattCallbackImpl handler = mGattHandlersMap.get(address);
            if (handler != null) {
                mGattHandlersMap.remove(address);
            }
        }
        dispatchState(event, address);
    }

    @Override
    public void onMessage(int offset, byte[] data, @NonNull String address) {
        dispatchMessage(offset, data, address);
    }

    @Override
    public void onMtuChanged(int mtu) {
        mListeners.forEach(centralStateListener -> centralStateListener.onMtuChanged(mtu));
    }

    @Override
    public void onError(CentralErrorState errorState) {

    }

    public static class Builder {
        private Context context;
        private CentralConfiguration configuration;
        private CentralStateListener listener;

        public  Builder(@NonNull Context ctx, @NonNull CentralConfiguration configuration) {
            context = ctx;
            this.configuration = configuration;
        }

        public Builder setEventListener(CentralStateListener listener) {
            this.listener = listener;
            return this;
        }

        public IoToothCentral build() {
            return new IoToothCentral(context, listener, configuration);
        }
    }

    public static boolean hasSystemFeature(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }
}
