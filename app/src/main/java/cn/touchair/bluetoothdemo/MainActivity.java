/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */
package cn.touchair.bluetoothdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import cn.touchair.bluetoothdemo.databinding.ActivityMainBinding;
import cn.touchair.iotooth.IoToothConfiguration;
import cn.touchair.iotooth.IoToothEventListener;
import cn.touchair.iotooth.IoToothPeripheral;
import cn.touchair.iotooth.PeripheralEvent;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, IoToothEventListener {
    private static final int REQ_CODE_ALL_PERMISSIONS = 0;
    private ActivityMainBinding binding;
    private boolean isAccessBluetoothPermission = false;
    private IoToothPeripheral mIoToothPeripheral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkPermissions();
        binding.stopAdvertingBtn.setOnClickListener(this::onClick);
        binding.sendBtn.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.stop_adverting_btn) {
            if (isAccessBluetoothPermission) {
                mIoToothPeripheral.shutdown();
            }
        }

        if (id == R.id.send_btn) {
            String sendMsg = binding.messageEdit.getText().toString().trim();
            if (sendMsg != null && !sendMsg.isEmpty()) {
                mIoToothPeripheral.send(sendMsg);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_ALL_PERMISSIONS:
                boolean isOk = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isOk = false;
                        break;
                    }
                }
                if (isOk) {
                    isAccessBluetoothPermission = true;
                    initIoToothPeripheral();
                }
                break;
            default:
        }
    }

    private void initIoToothPeripheral() {
        IoToothConfiguration ioToothConfiguration = new IoToothConfiguration(
                "e46ea779-33d2-4e4d-9081-cc1d81b48aeb",
                "1b3f1e30-0f15-4f98-8d69-d2b97f4cedd6",
                "2bc66748-4f33-4a6f-aeb0-14f3677c30fe",
                "ccb653e6-8006-d4c5-f215-6048075fae0f"
        );
        ioToothConfiguration.setAdvertTitle("Daisy");
        mIoToothPeripheral = new IoToothPeripheral(this, this);
        mIoToothPeripheral.startWithConfiguration(ioToothConfiguration);
    }

    public void checkPermissions() {
        final ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        boolean needRequest = false;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            requestPermissions(permissions.toArray(new String[0]), REQ_CODE_ALL_PERMISSIONS);
        } else {
            isAccessBluetoothPermission = true;
            initIoToothPeripheral();
        }
    }

    @Override
    public void onEvent(PeripheralEvent event, Object obj) {
        String newStateStr = "未定义";
        switch (event) {
            case ERROR:
                newStateStr = "错误：" + obj;
                break;
            case CONNECTED:
                newStateStr = "已连接";
                break;
            case CONNECTING:
                newStateStr = "连接中";
                break;
            case DISCONNECTED:
                newStateStr = "未连接";
                break;
            default:
        }
        binding.stateTextView.setText("状态：" + newStateStr);
    }

    @Override
    public void onNext(int offset, byte[] data) {
        appendMessage(new String(data, StandardCharsets.UTF_8));
    }

    private SimpleDateFormat mFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
    private void appendMessage(String newMessage) {
        binding.messageTextView.append(String.format("\n%s\t\t%s", mFormat.format(System.currentTimeMillis()), newMessage));
    }
}