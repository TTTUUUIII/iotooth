### 外围设备大致流程如下：
#### 1. 启动广播，已让设备可以被发现
```java
startWithConfiguration(IoToothConfiguration)
```
#### 2. 若第一步成功，`onStartSuccess()`将被调用，反之来到`onStartFailure()`
若广播成功，则可添加service和特征，像这样：
```java
@Override
public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
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
```
#### 3. 在onConnectionStateChange回调中监听连接事件
像这样：
```java
public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
        switch (newState) {
        case BluetoothGattServer.STATE_CONNECTED:
        mConnectedDevice = device;
        mListener.onEvent(PeripheralEvent.CONNECTED, null);
        stopAdvertising();
        break;
        case BluetoothGattServer.STATE_DISCONNECTED:
        mConnectedDevice = null;
        mListener.onEvent(PeripheralEvent.DISCONNECTED, null);
        startAdverting();
        break;
        case BluetoothGattServer.STATE_CONNECTING:
        mListener.onEvent(PeripheralEvent.CONNECTING, null);
        break;
        }
        }
```