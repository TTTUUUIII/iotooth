/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.periheral;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.touchair.iotooth.GlobalConfig;
import cn.touchair.iotooth.configuration.ToothConfiguration;
import cn.touchair.iotooth.util.RxFrameListener;
import cn.touchair.iotooth.util.TransmitterAble;
import kotlin.text.Charsets;

public class IoToothPeripheral extends AdvertiseCallback implements TransmitterAble {
    private static final String TAG = IoToothPeripheral.class.getSimpleName();
    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final List<PeripheralStateListener> mListeners = new CopyOnWriteArrayList<>();
    private final BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private BluetoothGattCharacteristic mReadonlyCharacteristic;
    private BluetoothGattCharacteristic mWritableCharacteristic;

    private RxFrameListener mRxFrameListener;
    private boolean mIsAdverting = false;
    private final Map<String, BluetoothDevice> mConnectedDevices = new ConcurrentHashMap<>();

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case BluetoothGattServer.STATE_CONNECTED:
                    mConnectedDevices.put(device.getAddress(), device);
                    dispatchState(PeripheralState.CONNECTED, device.getAddress());
                    stopAdvertising();
                    break;
                case BluetoothGattServer.STATE_DISCONNECTED:
                    mConnectedDevices.remove(device.getAddress());
                    dispatchState(PeripheralState.DISCONNECTED, null);
                    break;
                case BluetoothGattServer.STATE_CONNECTING:
                    dispatchState(PeripheralState.CONNECTING, null);
                    break;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "onServiceAdded called");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "onCharacteristicReadRequest called");
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            mListeners.forEach(peripheralStateListener -> peripheralStateListener.onMtuChanged(mtu));
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            dispatchMessage(offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "onDescriptorWriteRequest: " + Arrays.toString(value));
            }
            if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                descriptor.setValue(value);
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "onDescriptorReadRequest");
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "onExecuteWrite");
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    };

    @SuppressLint("MissingPermission")
    private   IoToothPeripheral(@NonNull Context ctx, @Nullable PeripheralStateListener listener) {
        Objects.requireNonNull(ctx);
        mContext = ctx;
        mBluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        if (Objects.nonNull(listener)) {
            mListeners.add(listener);
        }
        mAdapter = mBluetoothManager.getAdapter();
        if (!mAdapter.isEnabled() && !mAdapter.enable()) {
            Log.d(TAG, "Bluetooth not enable.");
        }
    }

    public void addEventListener(@NonNull PeripheralStateListener listener) {
        if (mListeners.contains(listener)) return;
        mListeners.add(listener);
    }

    public void removeEventListener(@NonNull PeripheralStateListener listener) {
        mListeners.remove(listener);
    }

    private AdvertiseData mCurrentAdvertiseData;
//    @SuppressLint("MissingPermission")
//    public void enable() {
//        enable(null);
//    }

    public void enable(@Nullable AdvertiseData advertiseData) {
        startAdverting(advertiseData);
        mCurrentAdvertiseData = advertiseData;
    }

    @SuppressLint("MissingPermission")
    public void disable() {
        if (Objects.nonNull(mGattServer)) {;
            mConnectedDevices.forEach((address, device) -> {
                send(address, ToothConfiguration.PERIPHERAL_CLOSED);
            });
            mGattServer.clearServices();
        }
        stopAdvertising();
        dispatchState(PeripheralState.DISCONNECTED, null);
    }

    @SuppressLint("MissingPermission")
    private void stopAdvertising() {
        if (!mIsAdverting) return;
        mIsAdverting = false;
        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();
        advertiser.stopAdvertising(this);
    }

    @SuppressLint("MissingPermission")
    private void startAdverting(AdvertiseData advertiseData) {
        if (mIsAdverting) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();
        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(settings, advertiseData, this);
    }

    @SuppressLint("MissingPermission")
    public void send(@Nullable String address, byte[] bytes) {
        if (Objects.nonNull(mGattServer) && Objects.nonNull(mReadonlyCharacteristic)) {
            BluetoothDevice device = mConnectedDevices.get(address);
            if (device != null) {
                mReadonlyCharacteristic.setValue(bytes);
                boolean indicate = (mReadonlyCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
                mGattServer.notifyCharacteristicChanged(device, mReadonlyCharacteristic, indicate);
            } else {
                Log.w(TAG, "Couldn't, send msg device " + address + " not exist.");
            }
        }
    }

    public void send(@Nullable String address, String msg) {
        send(address, msg.getBytes(Charsets.UTF_8));
    }

    @Override
    public void setRxFrameListener(@Nullable RxFrameListener listener) {
        mRxFrameListener = listener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
        mIsAdverting = true;
        dispatchState(PeripheralState.ADVERTISING, "Nana");
        if (Objects.isNull(mGattServer)) {
            mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        }
        mGattServer.clearServices();
        if (mCurrentAdvertiseData != null) {
            mCurrentAdvertiseData.getServiceUuids().forEach(this::addGattService);
        }
    }

    @SuppressLint("MissingPermission")
    private void addGattService(ParcelUuid serviceUuid) {
        BluetoothGattService gattService = new BluetoothGattService(serviceUuid.getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mReadonlyCharacteristic = new BluetoothGattCharacteristic(ToothConfiguration.readonlyUuid,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        mReadonlyCharacteristic.addDescriptor(ToothConfiguration.getClientCharacteristicConfigurationDescriptor());
        mWritableCharacteristic = new BluetoothGattCharacteristic(ToothConfiguration.writableUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        gattService.addCharacteristic(mReadonlyCharacteristic);
        gattService.addCharacteristic(mWritableCharacteristic);
        mGattServer.addService(gattService);
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
        dispatchErrorState(PeripheralErrorState.ERROR_ADVERTISE);
        mCurrentAdvertiseData = null;
        Log.e(TAG, "Failed to start advertising, errorCode=" + errorCode);
    }

    private void dispatchState(@NonNull PeripheralState event, Object obj) {
        ArrayList<PeripheralStateListener> deadListeners = new ArrayList<>();
        mListeners.forEach(listener -> {
            try {
                listener.onStateChanged(event, obj);
            } catch (Exception e) {
                Log.w(TAG, "Listener dead.");
                deadListeners.add(listener);
            }
        });
        deadListeners.forEach(mListeners::remove);
    }

    private void dispatchErrorState(PeripheralErrorState errorState) {
        ArrayList<PeripheralStateListener> deadListeners = new ArrayList<>();
        mListeners.forEach(listener -> {
            try {
                listener.onError(errorState);
            } catch (Exception e) {
                Log.w(TAG, "Listener dead.");
                deadListeners.add(listener);
            }
        });
        deadListeners.forEach(deadListener -> {
            mListeners.remove(deadListener);
        });
    }

    private void dispatchMessage(int offset, byte[] data) {
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, "dispatchMessage: " + Arrays.toString(data));
        }
        if (Objects.nonNull(mRxFrameListener)) {
            mRxFrameListener.onFrame(offset, data, null);
        }
        ArrayList<PeripheralStateListener> deadListeners = new ArrayList<>();
        mListeners.forEach(listener -> {
            try {
                listener.onMessage(offset, data);
            } catch (Exception e) {
                Log.w(TAG, "Listener dead.");
                deadListeners.add(listener);
            }
        });
        deadListeners.forEach(mListeners::remove);
    }

    public static class Builder {
        private Context context;
//        private PeripheralConfiguration configuration;
        private PeripheralStateListener listener;

        public  Builder(@NonNull Context ctx) {
            context = ctx;
//            this.configuration = configuration;
        }

        public Builder setEventListener(@Nullable PeripheralStateListener listener) {
            this.listener = listener;
            return this;
        }

        public IoToothPeripheral build() {
            return new IoToothPeripheral(context, listener);
        }
    }

    public static boolean hasSystemFeature(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }
}
