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
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cn.touchair.iotooth.GlobalConfig;
import cn.touchair.iotooth.central.IoToothCentral;
import cn.touchair.iotooth.configuration.PeripheralConfiguration;
import cn.touchair.iotooth.configuration.ToothConfiguration;
import cn.touchair.iotooth.util.RxFrameListener;
import cn.touchair.iotooth.util.TransmitterAble;

public class IoToothPeripheral extends AdvertiseCallback implements TransmitterAble {
    private static final String TAG = IoToothPeripheral.class.getSimpleName();
    private Context mContext;
    private BluetoothAdapter mAdapter;
    private List<PeriheralStateListener> mListeners = new ArrayList<>();
    private PeripheralConfiguration mConfiguration;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;

    private BluetoothDevice mConnectedDevice;
    private BluetoothGattCharacteristic mReadonlyCharacteristic;
    private BluetoothGattCharacteristic mWritableCharacteristic;

    private RxFrameListener mRxFrameListener;
    private boolean mIsAdverting = false;

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case BluetoothGattServer.STATE_CONNECTED:
                    mConnectedDevice = device;
                    dispatchState(PeripheralState.CONNECTED, device.getAddress());
                    stopAdvertising();
                    break;
                case BluetoothGattServer.STATE_DISCONNECTED:
                    mConnectedDevice = null;
                    dispatchState(PeripheralState.DISCONNECTED, null);
                    startAdverting();
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
                Log.d(TAG, "onDescriptorWriteRequest: " + value);
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
    private   IoToothPeripheral(@NonNull Context ctx, @Nullable PeriheralStateListener listener, @NonNull PeripheralConfiguration configuration) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(configuration);
        mContext = ctx;
        mBluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        if (Objects.nonNull(listener)) {
            mListeners.add(listener);
        }
        mConfiguration = configuration;
        mAdapter = mBluetoothManager.getAdapter();
        if (!mAdapter.isEnabled() && !mAdapter.enable()) {
            Log.d(TAG, "Bluetooth not enable.");
        }
    }

    public void addEventListener(@NonNull PeriheralStateListener listener) {
        if (mListeners.contains(listener)) return;
        mListeners.add(listener);
    }

    public void removeEventListener(@NonNull PeriheralStateListener listener) {
        mListeners.remove(listener);
    }

    @SuppressLint("MissingPermission")
    public void enable() {
        startAdverting();
    }

    public void enableWithPram(int menu,byte [] prm) {
        startAdvertingWithPram(menu,prm);
    }

    @SuppressLint("MissingPermission")
    public void disable() {
        stopAdvertising();
        if (Objects.nonNull(mGattServer) && Objects.nonNull(mConnectedDevice)) {;
            send(ToothConfiguration.PERIPHERAL_CLOSED);
            mGattServer.cancelConnection(mConnectedDevice);
        }
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
    private void startAdverting() {
        if (mIsAdverting) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(mConfiguration.serviceUuid))
                .setIncludeTxPowerLevel(true);
        if (mConfiguration != null) {
            dataBuilder.setIncludeDeviceName(true);
            mAdapter.setName(mConfiguration.serviceLocalName);
        }
        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(settings, dataBuilder.build(), this);
    }

    @SuppressLint("MissingPermission")
    private void startAdvertingWithPram(int menu,byte [] pra) {
        if (mIsAdverting) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(mConfiguration.serviceUuid))
                .addManufacturerData(menu,pra)
                .setIncludeTxPowerLevel(true);
//        if (mConfiguration != null) {
//            dataBuilder.setIncludeDeviceName(true);
//            mAdapter.setName(mConfiguration.serviceLocalName);
//        }
        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(settings, dataBuilder.build(), this);
    }

    @SuppressLint("MissingPermission")
    public void send(byte[] bytes) {
        send(null, bytes);
    }

    @SuppressLint("MissingPermission")
    public void send(@Nullable String address, byte[] bytes) {
        if (Objects.nonNull(mGattServer) && Objects.nonNull(mReadonlyCharacteristic) && Objects.nonNull(mConnectedDevice)) {
            mReadonlyCharacteristic.setValue(bytes);
            boolean indicate = (mReadonlyCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
            mGattServer.notifyCharacteristicChanged(mConnectedDevice, mReadonlyCharacteristic, indicate);
        }
    }

    @Override
    public void setRxFrameListener(@Nullable RxFrameListener listener) {
        mRxFrameListener = listener;
    }

    public void send(String msg) {
        send(msg.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
        mIsAdverting = true;
        dispatchState(PeripheralState.ADVERTISING, mAdapter.getAddress());
        if (Objects.isNull(mGattServer)) {
            mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        }
        mGattServer.clearServices();
        BluetoothGattService gattService = new BluetoothGattService(mConfiguration.serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mReadonlyCharacteristic = new BluetoothGattCharacteristic(mConfiguration.readonlyUuid,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        mReadonlyCharacteristic.addDescriptor(ToothConfiguration.getClientCharacteristicConfigurationDescriptor());
        mWritableCharacteristic = new BluetoothGattCharacteristic(mConfiguration.writableUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        gattService.addCharacteristic(mReadonlyCharacteristic);
        gattService.addCharacteristic(mWritableCharacteristic);
        mGattServer.addService(gattService);
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
        Log.e(TAG, "Failed to start advertising, errorCode=" + errorCode);
    }

    private void dispatchState(@NonNull PeripheralState event, Object obj) {
        mListeners.forEach(listener -> {
            try {
                listener.onStateChanged(event, obj);
            } catch (Exception e) {
                Log.w(TAG, "Listener dead.");
            }
        });
    }

    private void dispatchMessage(int offset, byte[] data) {
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, "dispatchMessage: " + Arrays.toString(data));
        }
        if (Objects.nonNull(mRxFrameListener)) {
            mRxFrameListener.onFrame(offset, data, null);
        }
        mListeners.forEach(listener -> {
            try {
                listener.onMessage(offset, data);
            } catch (Exception e) {
                Log.w(TAG, "Listener dead.");
            }
        });
    }

    public static class Builder {
        private Context context;
        private PeripheralConfiguration configuration;
        private PeriheralStateListener listener;

        public  Builder(@NonNull Context ctx, @NonNull PeripheralConfiguration configuration) {
            context = ctx;
            this.configuration = configuration;
        }

        public Builder setEventListener(@Nullable PeriheralStateListener listener) {
            this.listener = listener;
            return this;
        }

        public IoToothPeripheral build() {
            return new IoToothPeripheral(context, listener, configuration);
        }
    }
}
