/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Objects;

import cn.touchair.bluetoothdemo.CentralActivity;
import cn.touchair.bluetoothdemo.R;
import cn.touchair.bluetoothdemo.databinding.FragmentCommunicateBinding;
import cn.touchair.iotooth.central.CentralState;
import cn.touchair.iotooth.central.CentralStateListener;

public class CommunicateFragment extends Fragment implements CentralStateListener, View.OnClickListener {
    public static String ARG_KEY_CONNECT_TO = "connectTo";
    private FragmentCommunicateBinding binding;
    private BluetoothDevice mRemote;
    private CentralActivity mParent;

    private final SimpleDateFormat mFormatter = new SimpleDateFormat("MM/dd HH:ss");
    private final StringBuilder mMessageCache = new StringBuilder();
    private final Handler mH = new Handler(Looper.getMainLooper());
    private CentralState mState = CentralState.DISCONNECTED;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentCommunicateBinding.inflate(inflater, container, false);
        binding.mainLayout.sendBtn.setOnClickListener(this::onClick);
        binding.mainLayout.messageEditText.setText("Hi, David!");
        connectTo();
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mParent = (CentralActivity) context;
    }

    private void connectTo() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mRemote = arguments.getParcelable(ARG_KEY_CONNECT_TO);
            ActionBar actionBar = mParent.getSupportActionBar();
            actionBar.setSubtitle(mRemote.getAddress());
            mParent.connect(mRemote, this);
        }
    }

    public static CommunicateFragment newInstance() {
        Bundle args = new Bundle();
        CommunicateFragment fragment = new CommunicateFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onEvent(CentralState event, @NonNull String address) {
        mState = event;
        mH.post(this::updateUI);
    }

    private void updateUI() {
        switch (mState) {
            case OPENED_GATT:
                binding.mainLayout.stateTextView.setText("状态：连接中");
                break;
            case CONNECTED:
                binding.mainLayout.stateTextView.setText("状态：已连接");
                binding.mainLayout.sendBtn.setEnabled(true);
                break;
            case DISCONNECTED:
                binding.mainLayout.stateTextView.setText("状态：未连接");
                binding.mainLayout.sendBtn.setEnabled(false);
                break;
            case RSSI_REPORTER:
        }
        binding.mainLayout.messageShowTextView.setText(mMessageCache.toString());
    }

    @Override
    public void onMessage(int offset, byte[] data, @NonNull String address) {
        String message = new String(data, StandardCharsets.UTF_8);
        mMessageCache.append(
                String.format("%s\t\t%s\n", mFormatter.format(System.currentTimeMillis()), message)
        );
        mH.post(this::updateUI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Objects.nonNull(mRemote)) {
            mParent.disconnect(mRemote);
            mRemote = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.send_btn) {
            final String msg = binding.mainLayout.messageEditText.getText().toString().trim();
            if (!msg.isEmpty() && Objects.nonNull(mRemote)) {
                String address = mRemote.getAddress();
                mParent.send(address, msg);
            }
        }
    }
}
