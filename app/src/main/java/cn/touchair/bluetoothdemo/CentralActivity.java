/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Objects;

import cn.touchair.bluetoothdemo.databinding.ActivityCentralBinding;
import cn.touchair.bluetoothdemo.fragment.FindRemoteFragment;
import cn.touchair.iotooth.central.CentralState;
import cn.touchair.iotooth.central.CentralStateListener;
import cn.touchair.iotooth.central.IoToothCentral;
import cn.touchair.iotooth.central.ScanResultCallback;
import cn.touchair.iotooth.configuration.CentralConfiguration;
import cn.touchair.iotooth.util.TransmitterAble;
import cn.touchair.iotooth.util.TransmitterController;

public class CentralActivity extends AppCompatActivity implements  CentralStateListener {
    private static final String TAG = CentralActivity.class.getSimpleName();
    private IoToothCentral mCentral;
    private ActivityCentralBinding binding;
    private FragmentManager mFragmentManager;

    private HashMap<String, CentralStateListener> mObservers = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCentralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mFragmentManager = getSupportFragmentManager();
        mCentral = new IoToothCentral.Builder(this, new CentralConfiguration("1b3f1e30-0f15-4f98-8d69-d2b97f4ceddf"))
                .setEventListener(this)
                .build();
        if (Objects.isNull(savedInstanceState)) {
            FindRemoteFragment findRemoteFragment = FindRemoteFragment.newInstance();
            mFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, findRemoteFragment, FindRemoteFragment.class.getCanonicalName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void connect(@NonNull BluetoothDevice remote, CentralStateListener listener) {
        String address = remote.getAddress();
        if (mObservers.get(address) != null) return;
        mObservers.put(address, listener);
        mCentral.connect(remote);
    }

    public void disconnect(BluetoothDevice remote) {
        mCentral.disconnect(remote.getAddress());
    }

    private void disconnectAll() {
        mCentral.disconnectAll();
    }

    public void send(@NonNull String address, @NonNull String sendMsg) {
        if (sendMsg.isEmpty()) return;
        mCentral.send(address, sendMsg);
    }

    @Override
    public void onEvent(CentralState event, @NonNull String address) {
        CentralStateListener listener = mObservers.get(address);
        if (Objects.nonNull(listener)) {
            listener.onEvent(event, address);
        }
    }

    @Override
    public void onMessage(int offset, byte[] data, @NonNull String address) {
        CentralStateListener listener = mObservers.get(address);
        if (Objects.nonNull(listener)) {
            listener.onMessage(offset, data, address);
        }
    }

    public void startScan(@NonNull ScanResultCallback callback) {
        mCentral.scanWithDuration(1000 * 10, callback, new ScanFilter.Builder().setDeviceName("TOOTH").build());
    }

    public TransmitterAble getTransmitterCore() {
        return mCentral;
    }

    public void startFragment(@NonNull Fragment fragment) {
        String tag = fragment.getClass().getCanonicalName();
        mFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }
}