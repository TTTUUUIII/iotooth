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

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import cn.touchair.iotooth.GlobalConfig;
import cn.touchair.iotooth.Logger;
import cn.touchair.iotooth.configuration.CentralConfiguration;
import cn.touchair.iotooth.configuration.ToothConfiguration;
import cn.touchair.iotooth.util.ToothUtils;

public class GattCallbackImpl extends BluetoothGattCallback {

    private static final String TAG = GattCallbackImpl.class.getSimpleName();

    private final byte[] mLock = new byte[0];
    private final CentralConfiguration mConfiguration;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mReadonlyCharacteristic;
    private BluetoothGattCharacteristic mWritableCharacteristic;
    private CentralStateListener mListener;

    private boolean mExecWriting = false;
    private volatile boolean mIsConnected = false;

    private final ConcurrentLinkedQueue<byte[]> mTxWaitQueue = new ConcurrentLinkedQueue<>();
    private final Logger mLogger = Logger.getLogger(GattCallbackImpl.class);

    public GattCallbackImpl(@NonNull CentralConfiguration configuration, @NonNull CentralStateListener listener) {
        mConfiguration = configuration;
        mListener = listener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        String address = gatt.getDevice().getAddress();
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, "onConnectionStateChange: newState=" + newState);
        }
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                mIsConnected = true;
                mBluetoothGatt = gatt;
                gatt.discoverServices();
                break;
            case BluetoothGatt.STATE_CONNECTING:
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                mIsConnected = false;
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
            Set<UUID> services = gatt.getServices()
                    .stream()
                    .map(service -> service.getUuid())
                    .collect(Collectors.toSet());
            mLogger.debug("SERVICE_LIST", services);
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
            if (GlobalConfig.DEBUG) {
                Set<UUID> descriptors = mReadonlyCharacteristic
                        .getDescriptors()
                        .stream()
                        .map(descriptor -> descriptor.getUuid())
                        .collect(Collectors.toSet());
                mLogger.debug("R_DESCRIPTOR_LIST", descriptors);
            }
            BluetoothGattDescriptor descriptor = mReadonlyCharacteristic.getDescriptor(ToothConfiguration.CLIENT_CHARACTERISTIC_CONFIG);
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
        if (mTxWaitQueue.isEmpty()) {
            synchronized (mLock) {
                mExecWriting = false;
            }
        } else {
            byte[] next = mTxWaitQueue.poll();
            if (next != null) {
                writeCharacteristic(next);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        super.onCharacteristicChanged(gatt, characteristic, value);
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, new String(value));
        }
//        mListener.onMessage(0, value);
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        if (GlobalConfig.DEBUG) {
            Log.d(TAG, "onReliableWriteCompleted");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        byte[] value = characteristic.getValue();
        if (isExitNotify(value)) {
            close();
        }
        if (mListener == null) return;
        String address = gatt.getDevice().getAddress();
        mListener.onMessage(0, value, address);
    }

    @Override
    public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
        super.onDescriptorRead(gatt, descriptor, status, value);
        Log.d(TAG, "onDescriptorRead");
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        String address = gatt.getDevice().getAddress();
        if (mListener == null) return;
        mListener.onStateChanged(CentralState.RSSI_REPORTER, String.format(Locale.US, "{address: \"%s\", rssi: \"%d\"}", address, rssi));
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

    @SuppressLint("MissingPermission")
    @Override
    public void onServiceChanged(@NonNull BluetoothGatt gatt) {
        gatt.disconnect();
        mIsConnected = false;
        mListener.onStateChanged(CentralState.DISCONNECTED, gatt.getDevice().getAddress());
        Log.i(TAG, "onServiceChanged");
    }

    @SuppressLint("MissingPermission")
    public void send(@NonNull String msg) {
        send(msg.getBytes(Charset.defaultCharset()));
    }

    @SuppressLint("MissingPermission")
    public void send(@NonNull byte[] bytes) {
        if (mBluetoothGatt != null && mWritableCharacteristic != null) {
            synchronized (mLock) {
                if (mExecWriting) {
                    mTxWaitQueue.add(bytes);
                } else {
                    mExecWriting = true;
                    writeCharacteristic(bytes);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mIsConnected) {
            handleState(CentralState.DISCONNECTED, mBluetoothGatt.getDevice().getAddress());
            mBluetoothGatt.close();
            mIsConnected = false;
        }
        mListener = null;
        mBluetoothGatt = null;
    }

    /**
     *
     * @hide
     */
    @SuppressLint("MissingPermission")
    private void writeCharacteristic(byte[] data) {
        mWritableCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mWritableCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mWritableCharacteristic);
    }

    private void handleState(@NonNull CentralState newState, @Nullable String address) {
        mListener.onStateChanged(newState, address);
        Log.d(TAG, "Notify new state => " + ToothUtils.stateToString(newState));
    }

    private boolean isExitNotify(byte[] value) {
        boolean ret = true;
        for (byte i : value) {
            if (i != -1) {
                ret = false;
                break;
            }
        }
        return ret;
    }
}
