# IO TOOTH

## 1. 关于

这是一个简易的低功耗蓝牙收发库，目前包含以下组件：

> `IoToothCentral`对应蓝牙中心设备;
>
> `IoToothPeripheral`对应蓝牙外设;
>
> `TransmitterController`传输控制，目前仅支持简单的文本传输。

## 2. 快速开始

### 2.1 申请权限
```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```
注意部分权限需要您[动态申请](https://developer.android.google.cn/training/permissions/requesting)。

### 2.2 声明依赖

`build.gradle[project]`
```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

`build.gradle[module]`

```groovy
dependencies {
    implementation 'com.github.TTTUUUIII:iotooth-for-android:v1.0.0'
}
```

### 2.3 使用

**A端作为蓝牙外设**

第一步：初始化
```java
private IoToothPeripheral mPeripheral;
@Override
protected void onCreate(Bundle savedInstanceState) {
    PeripheralConfiguration configuration = new PeripheralConfiguration("1b3f1e30-0f15-4f98-8d69-d2b97f4ceddf");
    // configuration.serviceLocalName = "Name"; /*Not recommended*/
    mPeripheral = new IoToothPeripheral.Builder(this, configuration)
            .build();
    mPeripheral.addEventListener(new PeriheralStateListener() {
            @Override
            public void onStateChanged(PeripheralState state, Object obj) {
                switch (state) {
                    case ADVERTISING:
                        break;
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        mConnectedDevices.add((String /*Address*/) obj);
                        break;
                    case DISCONNECTED:
                        mConnectedDevices.remove((String /*Address*/) obj);
                        break;
                    default:
                }
            }

            @Override
            public void onMessage(int offset, byte[] data) {
                /*Received Message*/
            }

            @Override
            public void onError(PeripheralErrorState errorState) {

            }
        });
}
```

第二步：启用外设
```java
mPeripheral.enable();    /* Wait for central device */
```

第三步：发消息
```java
if(connected) {
    mConnectedDevices.forEach(address -> mPeripheral.send(address, "hello".getBytes(StandardCharsets.UTF_8)));
}
```

**B端作为中心设备**

第一步：初始化
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
CentralConfiguration configuration = new CentralConfiguration("1b3f1e30-0f15-4f98-8d69-d2b97f4ceddf");
    mCentral = new IoToothCentral.Builder(this, configuration)
            .build();
    mCentral.addEventListener(new CentralStateListener() {
            @Override
            public void onStateChanged(CentralState state, @NonNull String address) {
                switch (state) {
                case OPENED_GATT:
                break;
                case CONNECTED:
                    mConnectedDevices.add(address);
                    break;
                case DISCONNECTED:
                    mConnectedDevices.remove(address);
                    break;
                case RSSI_REPORTER: /*Not impl*/
                default:
               }
            }

            @Override
            public void onMessage(int offset, byte[] data, @NonNull String address) {
                /*Received Message*/
            }

            @Override
            public void onError(CentralErrorState errorState) {

            }
        })
}
```

第二步：扫描并连接A端（外设）
```java
ScanResultCallback callback = new ScanResultCallback() {
    @Override
    public void onScanStarted() {

    }

    @Override
    public void onScanStopped() {

    }

    @Override
    public void onScanResult(ScanResult result) {
        if (result == my target) {
            mCentral.connect(result.getDevice());
        }
    }
};
ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString("1b3f1e30-0f15-4f98-8d69-d2b97f4ceddf"))
                .build();
mCentral.scanWithDuration(1000 * 10, callback, scanFilter);
```

第三步：发消息
```java
if(connected) {
    mConnectedDevices.forEach(address -> mCentral.send(address, "hello".getBytes(StandardCharsets.UTF_8)));
}
```

### 2.4 使用`TransmitterController`收发消息
如果直接使用IoToothCentral或IoToothPeripheral收发消息，默认情况下单次仅能传输20bytes的内容，使用`TransmitterController`则无此限制：
```java
TransmitterController controller = TransmitterController.create(mPeripheral /*Or mCentral*/, new TransmitterController.TransmitterCallback() {
    @Override
    public void onText(@Nullable String address, @NonNull String text) {
        /*Received Text Message*/
    }

    @Override
    public void onStream(@Nullable String address, float progress, byte dataType, byte[] raw, int offset, int len) {

    }
});

mConnectedDevices.forEach(address -> {
    mController.writeText(address, "Very long text message");
});
```

## 4. 接口说明

**`IoToothCentral`**

| 接口                                                       | 说明              | 备注 |
|:----------------------------------------------------------|:-----------------|:----|
| scanWithDuration(long mills, ScanResultCallback callback) | 扫描蓝牙外设       |      |
| connect(BluetoothDevice device)                           | 连接到指定设备      |     |
| disconnect(String address)                                | 从指定设备断开      |     |
| disconnectAll()                                           | 断开所有已连接的设备 |     |
| addEventListener(CentralStateListener listener)           | 注册事件处理器      |     |
| send(String address, byte[] data)                         | 发送消息           |     |
| send(String address, String text)                         | 发送消息           |     |

**`IoToothPeripheral`**

| 接口                                                | 说明                       | 备注 |
|:---------------------------------------------------|:---------------------------|:----|
| enable()                                           | 启用外设                    |      |
| enableWithPram(int key, byte[] advertiseData)      | 启用外设，并在广播中携带指定数据 |     |
| disable()                                          | 禁用外设                    |      |
| addEventListener(PeripheralStateListener listener) | 注册事件处理器                |     |
| send(String address, byte[] data)                  | 发送消息                    |      |

**`TransmitterController`**

| 接口                                                           | 说明              | 备注      |
|:--------------------------------------------------------------|:-----------------|:---------|
| create(TransmitterAble core, TransmitterCallback callback)    | 创建一个Controller |          |
| writeText(@Nullable String address, String text)              | 发送文本消息        |          |
| writeFile(@Nullable String address, File file, byte dataType) | 发送文件           | Not impl |
