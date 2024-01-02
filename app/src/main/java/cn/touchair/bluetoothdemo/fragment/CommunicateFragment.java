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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import cn.touchair.bluetoothdemo.CentralActivity;
import cn.touchair.bluetoothdemo.R;
import cn.touchair.bluetoothdemo.databinding.FragmentCommunicateBinding;
import cn.touchair.bluetoothdemo.entity.Card;
import cn.touchair.iotooth.central.CentralState;
import cn.touchair.iotooth.central.CentralStateListener;
import cn.touchair.iotooth.util.TransmitterController;
import cn.touchair.iotooth.util.TransmitterControllerImpl;
import cn.touchair.iotooth.util.TypeUtils;

public class CommunicateFragment extends Fragment implements CentralStateListener, View.OnClickListener, TransmitterController.TransmitterCallback {
    public static String ARG_KEY_CONNECT_TO = "connectTo";
    private FragmentCommunicateBinding binding;
    private BluetoothDevice mRemote;
    private CentralActivity mParent;

    private final SimpleDateFormat mFormatter = new SimpleDateFormat("MM/dd HH:ss");
    private final StringBuilder mMessageCache = new StringBuilder();
    private final Handler mH = new Handler(Looper.getMainLooper());
    private CentralState mState = CentralState.DISCONNECTED;
    private TransmitterController mController;

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
        mController = TransmitterController.create(mParent.getTransmitterCore(), this);
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
    public void onStateChanged(CentralState event, @NonNull String address) {
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
        /*Ignored*/
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
//                mController.writeText(address, msg);
                Card card = new Card.Builder("Google", "David")
                        .setPosition("Application engineer")
                        .setEmail("david@gmail.com")
                        .setMailingAddress("Longwood STHL 1ZZ, St Helena, Ascension and Tristan da Cunha.")
                        .setTelephone("+1-555-009-2937")
                        .build();
                mController.writeText(address, new Gson().toJson(card));
            }
        }
    }

    @Override
    public void onText(@Nullable String address, @NonNull String text) {
        mMessageCache.append(
                String.format("%s\t\t%s\n", mFormatter.format(System.currentTimeMillis()), text)
        );
        mH.post(this::updateUI);
    }

    @Override
    public void onStream(@Nullable String address, float progress, byte dataType, byte[] frame, int offset, int len) {
        /*Ignored*/
    }

    @Override
    public void onRxProgress(@Nullable String addr, int rxType, int total, int index) {
        int progress = (int) (((float) index / total) * 100);
        binding.mainLayout.rxProgressTextView.setText(String.format(Locale.US, "%d%%", progress));
        if (progress == 100) {
            mH.postDelayed(() -> {
                binding.mainLayout.rxProgressTextView.setText("-");
            }, 300);
        }
    }

    @Override
    public void onTxProgress(@Nullable String addr, int txType, int total, int index) {
        int progress = (int) (((float) index / total) * 100);
        binding.mainLayout.txProgressTextView.setText(String.format(Locale.US, "%d%%", progress));
        if (progress == 100) {
            mH.postDelayed(() -> {
                binding.mainLayout.txProgressTextView.setText("-");
            }, 300);
        }
    }
}
