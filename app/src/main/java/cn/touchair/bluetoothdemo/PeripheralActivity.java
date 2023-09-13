/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import cn.touchair.bluetoothdemo.databinding.ActivityPeripheralBinding;
import cn.touchair.iotooth.configuration.PeripheralConfiguration;
import cn.touchair.iotooth.periheral.IoToothPeripheral;
import cn.touchair.iotooth.periheral.PeriheralStateListener;
import cn.touchair.iotooth.periheral.PeripheralState;

public class PeripheralActivity extends AppCompatActivity implements PeriheralStateListener {
    private IoToothPeripheral mPeripheral;
    private ActivityPeripheralBinding binding;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MM-dd HH:mm");
    private final StringBuilder mMessageCache = new StringBuilder();
    private PeripheralConfiguration mConfiguration = new PeripheralConfiguration(
            "1b3f1e30-0f15-4f98-8d69-d2b97f4cedd6",
            "2bc66748-4f33-4a6f-aeb0-14f3677c30fe",
            "ccb653e6-8006-d4c5-f215-6048075fae0f"
    );

    private PeripheralState mState = PeripheralState.DISCONNECTED;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfiguration.serviceLocalName = "followshot";
        binding = ActivityPeripheralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mPeripheral = new IoToothPeripheral(this, this);
        binding.mainLayout.sendBtn.setOnClickListener(this::onAction);
        binding.advertBtn.setOnClickListener(this::onAction);
    }

    public void onAction(View view) {
        int id = view.getId();
        if (id == R.id.advert_btn) {
            mPeripheral.startWithConfiguration(mConfiguration);
        }

        if (id == R.id.send_btn) {
            String sendMsg = binding.mainLayout.messageEditText.getText().toString().trim();
            if (sendMsg != null && !sendMsg.isEmpty()) {
                mPeripheral.send(sendMsg);
            }
        }
    }

    @Override
    public void onEvent(PeripheralState event, Object obj) {
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
        binding.advertBtn.setEnabled(mState != PeripheralState.CONNECTED);
        binding.mainLayout.sendBtn.setEnabled(mState == PeripheralState.CONNECTED);
    }

    private String stateString() {
        switch (mState) {
            case CONNECTED:
                return "已连接";
            case DISCONNECTED:
                return "未连接";
            case CONNECTING:
                return "连接中";
            case ADVERTISING:
                return "广播中";
            default:
        }
        return "未定义";
    }
}