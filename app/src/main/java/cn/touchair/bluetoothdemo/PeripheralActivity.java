/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cn.touchair.bluetoothdemo.databinding.ActivityPeripheralBinding;
import cn.touchair.bluetoothdemo.entity.Card;
import cn.touchair.iotooth.configuration.PeripheralConfiguration;
import cn.touchair.iotooth.periheral.IoToothPeripheral;
import cn.touchair.iotooth.periheral.PeriheralStateListener;
import cn.touchair.iotooth.periheral.PeripheralState;
import cn.touchair.iotooth.util.TransmitterController;

public class PeripheralActivity extends AppCompatActivity implements PeriheralStateListener, TransmitterController.TransmitterCallback {
    private IoToothPeripheral mPeripheral;
    private TransmitterController mController;
    private ActivityPeripheralBinding binding;
    private SimpleDateFormat mFormatter = new SimpleDateFormat("MM-dd HH:mm");
    private final StringBuilder mMessageCache = new StringBuilder();
    private PeripheralState mState = PeripheralState.DISCONNECTED;
    private Object mEventObj = null;
    private boolean isEnable = false;
    private final Handler mH = new Handler(Looper.getMainLooper());
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPeripheralBinding.inflate(getLayoutInflater());
        TempState.defaultButtonBackgroundTintList = binding.tggleBtn.getBackgroundTintList();
        setContentView(binding.getRoot());
        binding.mainLayout.messageEditText.setText("I'm David!");
        mPeripheral = new IoToothPeripheral.Builder(this, new PeripheralConfiguration("1b3f1e30-0f15-4f98-8d69-d2b97f4ceddf"))
                .setEventListener(this)
                .build();
        mController = TransmitterController.create(mPeripheral, this);
        binding.mainLayout.sendBtn.setOnClickListener(this::onAction);
        binding.tggleBtn.setOnClickListener(this::onAction);
    }

    public void onAction(View view) {
        int id = view.getId();
        if (id == R.id.tggle_btn) {
            isEnable = !isEnable;
            if (isEnable) {
                mPeripheral.enable();
            } else {
                mPeripheral.disable();
            }
            updateUI();
        }

        if (id == R.id.send_btn) {
            String sendMsg = binding.mainLayout.messageEditText.getText().toString().trim();
            if (!sendMsg.isEmpty()) {
//                mController.writeText(sendMsg);
                Card card = new Card.Builder("Google", "David")
                        .setPosition("Application engineer")
                        .setEmail("david@gmail.com")
                        .setMailingAddress("Longwood STHL 1ZZ, St Helena, Ascension and Tristan da Cunha.")
                        .setTelephone("+1-555-009-2937")
                        .build();
                mController.writeText(new Gson().toJson(card));
            }
        }
    }

    @Override
    public void onStateChanged(PeripheralState event, Object obj) {
        mState = event;
        mEventObj = obj;
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onMessage(int offset, byte[] data) {
        /*Ignored*/
    }

    private void updateMessage() {
        binding.mainLayout.messageShowTextView.setText(mMessageCache);
        binding.mainLayout.scrollView.fullScroll(View.FOCUS_DOWN);
    }

    private void updateUI() {
        binding.mainLayout.stateTextView.setText("状态：" + stateString());
        binding.mainLayout.sendBtn.setEnabled(mState == PeripheralState.CONNECTED);
        binding.tggleBtn.setText(isEnable ? "禁用" : "启用");
        binding.tggleBtn.setBackgroundTintList(
                isEnable ? ColorStateList.valueOf(0xFFFF0000) : TempState.defaultButtonBackgroundTintList
        );
    }

    private String stateString() {
        switch (mState) {
            case CONNECTED:
                return "已连接[" + mEventObj + "]";
            case DISCONNECTED:
                return "未连接";
            case CONNECTING:
                return "连接中";
            case ADVERTISING:
                return "广播中[" + mEventObj + "]";
            default:
        }
        return "未定义";
    }

    @Override
    public void onText(@Nullable String address, @NonNull String text) {
        mMessageCache.append(String.format("\n%s:\t\t%s", mFormatter.format(System.currentTimeMillis()), text));
        runOnUiThread(this::updateMessage);
    }

    @Override
    public void onStream(@Nullable String address, float progress, byte type, byte[] raw, int offset, int len) {
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

class TempState {
    static ColorStateList defaultButtonBackgroundTintList;
}