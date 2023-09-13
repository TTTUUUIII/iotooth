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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cn.touchair.iotooth.configuration.PeripheralConfiguration;

public class IoToothPeripheral extends AdvertiseCallback {
    private static final String TAG = IoToothPeripheral.class.getSimpleName();
    private static final int ERROR_CODE_BLUETOOTH_DISABLED = -1;
    private static final int ERROR_CODE_FAILED_TO_ADVERTISING = -2;
    private Context mContext;
    private BluetoothAdapter mAdapter;
    private PeriheralStateListener mListener;
    private PeripheralConfiguration mConfiguration;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;

    private BluetoothDevice mConnectedDevice;
    private BluetoothGattCharacteristic mReadonlyCharacteristic;
    private BluetoothGattCharacteristic mWritableCharacteristic;
    private boolean mIsAdverting = false;

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            switch (newState) {
                case BluetoothGattServer.STATE_CONNECTED:
                    mConnectedDevice = device;
                    mListener.onEvent(PeripheralState.CONNECTED, null);
                    stopAdvertising();
                    break;
                case BluetoothGattServer.STATE_DISCONNECTED:
                    mConnectedDevice = null;
                    mListener.onEvent(PeripheralState.DISCONNECTED, null);
                    startAdverting();
                    break;
                case BluetoothGattServer.STATE_CONNECTING:
                    mListener.onEvent(PeripheralState.CONNECTING, null);
                    break;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(TAG, "onServiceAdded called");
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "onCharacteristicReadRequest called");
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            mListener.onMessage(offset, value);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }
    };

    public  IoToothPeripheral(@NonNull Context ctx, @NonNull PeriheralStateListener listener) {
        mContext = ctx;
        mBluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        mListener = listener;
        mAdapter = mBluetoothManager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    public void startWithConfiguration(PeripheralConfiguration configuration) {
        if (!mAdapter.isEnabled() && !mAdapter.enable()) {
            Log.d(TAG, "Bluetooth not enable.");
            return;
        }
        mConfiguration = configuration;
        startAdverting();
    }

    @SuppressLint("MissingPermission")
    public void stopAdvertising() {
        if (!mIsAdverting) return;
        mIsAdverting = false;
        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();
        advertiser.stopAdvertising(this);
    }

    @SuppressLint("MissingPermission")
    public void startAdverting() {
        if (mIsAdverting) return;
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
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
    public void shutdown() {
        stopAdvertising();
        if (Objects.nonNull(mGattServer)) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }
        mListener.onEvent(PeripheralState.DISCONNECTED, null);
    }

    @SuppressLint("MissingPermission")
    public void send(byte[] bytes) {
        if (Objects.nonNull(mGattServer) && Objects.nonNull(mReadonlyCharacteristic)) {
            mReadonlyCharacteristic.setValue(bytes);
            mGattServer.notifyCharacteristicChanged(mConnectedDevice, mReadonlyCharacteristic, true);
        }
    }

    public void send(String msg) {
        send(msg.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
        mIsAdverting = true;
        mListener.onEvent(PeripheralState.ADVERTISING, null);
        if (Objects.isNull(mGattServer)) {
            mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        }
        mGattServer.clearServices();
        BluetoothGattService gattService = new BluetoothGattService(mConfiguration.serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mReadonlyCharacteristic = new BluetoothGattCharacteristic(mConfiguration.readonlyUuid,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        mWritableCharacteristic = new BluetoothGattCharacteristic(mConfiguration.writableUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
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
}
