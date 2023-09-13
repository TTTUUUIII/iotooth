/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import cn.touchair.bluetoothdemo.databinding.ActivityCentralBinding;
import cn.touchair.iotooth.central.CentralState;
import cn.touchair.iotooth.central.CentralStateListener;
import cn.touchair.iotooth.central.IoToothCentral;
import cn.touchair.iotooth.configuration.CentralConfiguration;

public class CentralActivity extends AppCompatActivity implements View.OnClickListener, CentralStateListener {
    private static final String TAG = CentralActivity.class.getSimpleName();
    private IoToothCentral mCentral;
    private CentralConfiguration mConfiguration;
    private ActivityCentralBinding binding;
    private String mRemoteAddress;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MM-dd HH:mm");
    private CentralState mState = CentralState.SCANNING;
    private final StringBuilder mMessageCache = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCentralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mCentral = new IoToothCentral(this, this);
        mConfiguration = new CentralConfiguration(
                "1b3f1e30-0f15-4f98-8d69-d2b97f4cedd6",
                "2bc66748-4f33-4a6f-aeb0-14f3677c30fe",
                "ccb653e6-8006-d4c5-f215-6048075fae0f"
        );
        binding.connectBtn.setOnClickListener(this::onClick);
        binding.disconnectBtn.setOnClickListener(this::onClick);
        binding.mainLayout.sendBtn.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.connect_btn) connect();
        if (id == R.id.disconnect_btn) disconnect();
        if (id == R.id.send_btn) send();
    }

    private void connect() {
        mCentral.startWithConfiguration(mConfiguration);
    }

    private void disconnect() {
        mCentral.stop();
    }

    private void send() {
        if (mRemoteAddress != null) {
            String sendMsg = binding.mainLayout.messageEditText.getText().toString();
            if (sendMsg.isEmpty()) return;
            mCentral.send(mRemoteAddress, sendMsg);
        }
    }

    @Override
    public void onEvent(CentralState event, Object obj) {
        Log.d(TAG, String.format("CentralEvent: {%s, %s}", event, obj));
        switch (event) {
            case CONNECTED:
                mRemoteAddress = (String) obj;
                break;
            case DISCONNECTED:
                mRemoteAddress = null;
                break;
            default:
        }
        mState = event;
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onMessage(int offset, byte[] data) {
        String newMessage = new String(data, StandardCharsets.UTF_8);
        mMessageCache.append(String.format("\n%s:\t\t%s", mFormatter.format(System.currentTimeMillis()), newMessage));
        runOnUiThread(this::updateMessage);
    }

    private void updateMessage() {
        binding.mainLayout.messageShowTextView.setText(mMessageCache);
    }
    private void updateUI() {
        binding.mainLayout.stateTextView.setText("状态：" + stateString());
        binding.connectBtn.setEnabled(mState != CentralState.CONNECTED);
        binding.disconnectBtn.setEnabled(mState == CentralState.CONNECTED);
        binding.mainLayout.sendBtn.setEnabled(mState == CentralState.CONNECTED);
    }

    private String stateString() {
        switch (mState) {
            case CONNECTED:
                return "已连接";
            case DISCONNECTED:
                return "未连接";
            case SCANNING:
                return "扫描中";
            case OPENED_GATT:
                return "已接入GATT";
            default:
        }
        return "未定义";
    }
}