/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */
package cn.touchair.bluetoothdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.touchair.bluetoothdemo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final int REQ_CODE_PERMISSION = 0;
    private final List<String> ALL_PERMISSIONS = new ArrayList<>();

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH_CONNECT);
            ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH_CONNECT);
        ALL_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        ALL_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        ALL_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }
    private boolean mIsPermissionGranted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.centralBtn.setOnClickListener(this::route);
        binding.peripheralBtn.setOnClickListener(this::route);
        checkPermissions();
    }

    public void route(View v) {
        int id = v.getId();
        Intent intent = new Intent();
        if (!mIsPermissionGranted) {
            Toast.makeText(getApplicationContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (id == R.id.peripheral_btn) {
            intent.setClass(getApplicationContext(), PeripheralActivity.class);
        } else {
            intent.setClass(getApplicationContext(), CentralActivity.class);
        }
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_PERMISSION:
                boolean isOk = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        isOk = false;
                        break;
                    }
                }
                mIsPermissionGranted = isOk;
                break;
            default:
                /*do nothing*/
        }
    }

    public void checkPermissions() {
        List<String> needRequest = new ArrayList<>();
        for (String permission : ALL_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest.add(permission);
            }
        }
        if (!needRequest.isEmpty()) {
            requestPermissions(needRequest.toArray(new String[0]), REQ_CODE_PERMISSION);
        } else {
            mIsPermissionGranted = true;
        }
    }
}