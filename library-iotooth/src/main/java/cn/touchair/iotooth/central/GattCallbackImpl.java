/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.central;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import cn.touchair.iotooth.GlobalConfig;
import cn.touchair.iotooth.configuration.CentralConfiguration;

public class GattCallbackImpl extends BluetoothGattCallback {

    private static final String TAG = GattCallbackImpl.class.getSimpleName();
    private static final String NOTIFY_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private CentralConfiguration mConfiguration;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mReadonlyCharacteristic;
    private BluetoothGattCharacteristic mWritableCharacteristic;
    private CentralState mState = CentralState.OPENED_GATT;
    private CentralStateListener mListener;
    private boolean isConnected = false;
    public GattCallbackImpl(@NonNull CentralConfiguration configuration, @NonNull CentralStateListener listener) {
        mConfiguration = configuration;
        mListener = listener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String address = gatt.getDevice().getAddress();
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                isConnected = true;
                mBluetoothGatt = gatt;
                gatt.discoverServices();
                break;
            case BluetoothGatt.STATE_CONNECTING:
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                isConnected = false;
                handleState(CentralState.DISCONNECTED, address);
                mBluetoothGatt = null;
                break;
            default:
                /*do nothing*/
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (GlobalConfig.DEBUG) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.d(TAG, "SERVICE_LIST>>>>>>>>>>>>>>>>>>>>>>>>>>>>><");
            for (BluetoothGattService service : services) {
                Log.d(TAG, "*\t" + service.getUuid().toString());
            }
            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><");
        }
        BluetoothGattService service = gatt.getService(mConfiguration.serviceUuid);
        if (service == null) {
            Log.e(TAG, "Service " + mConfiguration.serviceUuid + " not found!");
            close();
            return;
        }
        mReadonlyCharacteristic = service.getCharacteristic(mConfiguration.readonlyUuid);
        mBluetoothGatt.setCharacteristicNotification(mReadonlyCharacteristic, true);
        if (mReadonlyCharacteristic != null) {
            BluetoothGattDescriptor descriptor = mReadonlyCharacteristic.getDescriptor(UUID.fromString(NOTIFY_DESCRIPTOR_UUID));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            } else {
                Log.w(TAG, "Not found readonly characteristic's descriptor!");
            }
        } else {
            Log.d(TAG, "Readonly characteristic " + mConfiguration.readonlyUuid + " not found!");
        }
        mWritableCharacteristic = service.getCharacteristic(mConfiguration.writableUuid);
        if (mWritableCharacteristic == null) {
            Log.w(TAG, "Writable characteristic " + mConfiguration.writableUuid + " not found!");
        }

        if (mWritableCharacteristic != null && mReadonlyCharacteristic != null) {
            handleState(CentralState.CONNECTED, gatt.getDevice().getAddress());
        }
    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        super.onCharacteristicRead(gatt, characteristic, value, status);
        Log.d(TAG, "onCharacteristicRead");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.d(TAG, "onCharacteristicWrite");
    }

    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, new String(value));
        }
        mListener.onMessage(0, value);
    }

    @Override
    public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
        super.onDescriptorRead(gatt, descriptor, status, value);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "Write descriptor success");
        } else {
            Log.w(TAG, "Write descriptor failed.");
        }
    }

    @Override
    public void onServiceChanged(@NonNull BluetoothGatt gatt) {
        super.onServiceChanged(gatt);
        Log.i(TAG, "onServiceChanged");
    }

    @SuppressLint("MissingPermission")
    public void send(@NonNull String msg) {
        if (mBluetoothGatt != null && mWritableCharacteristic != null) {
            mWritableCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mWritableCharacteristic.setValue(msg);
            mBluetoothGatt.writeCharacteristic(mWritableCharacteristic);
        }
    }

    @SuppressLint("MissingPermission")
    public void send(@NonNull byte[] bytes) {
        if (mBluetoothGatt != null && mWritableCharacteristic != null) {
            mWritableCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mWritableCharacteristic.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mWritableCharacteristic);
        }
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (isConnected) {
            handleState(CentralState.DISCONNECTED, mBluetoothGatt.getDevice().getAddress());
            mBluetoothGatt.close();
            isConnected = false;
        }
        mListener = null;
        mBluetoothGatt = null;
    }

    private void handleState(@NonNull CentralState newState, @Nullable Object obj) {
        mState = newState;
        mListener.onEvent(mState, obj);
    }

    public interface GattStateListener {
        void onEvent(CentralState state, @Nullable Object obj);
    }
}
