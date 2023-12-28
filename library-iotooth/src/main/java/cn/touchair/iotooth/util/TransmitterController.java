/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public abstract class TransmitterController {

    protected final String TAG = getClass().getSimpleName();
    protected static final int MAX_FRAME_SIZE = 20;
    protected static final byte MAX_DATA_FRAME_SIZE = 14;

    protected static final byte FRAME_TYPE_HEADER = 0x01;
    protected static final byte FRAME_TYPE_DATA = 0x02;

    protected static final byte DATA_TYPE_TEXT = 0x01;

    public static final byte DATA_TYPE_IMG_JPEG = 0x02;
    public static final byte DATA_TYPE_IMG_PNG = 0x03;
    public static final byte DATA_TYPE_TXT = 0x04;

    protected final TransmitterCallback callback;
    protected final TransmitterAble core;

    protected TransmitterController(TransmitterAble core, TransmitterCallback callback) {
        this.callback = callback;
        this.core = core;
    }

    public static TransmitterController create(TransmitterAble core, TransmitterCallback callback) {
        return new TransmitterControllerImpl(core, callback);
    }

    public void writeText(String text) {
        writeText(null, text);
    }
    public abstract void writeText(@Nullable String address, String text);
    public void writeFile(File file, byte dataType) {
        writeFile(null, file, dataType);
    }
    public abstract void writeFile(@Nullable String address, File file, byte dataType);

    public interface TransmitterCallback {
        void onText(@Nullable String address, @NonNull String text);

        /**
         * 当发送方调用writeFile时，接收方可通过此回调接收数据
         * @param address 发送方蓝牙地址
         * @param progress 进度 0.0f-1.0f
         * @param dataType
         * @param raw 核心接收到的数据帧
         * @param offset 指示有效数据在此帧的偏移
         * @param len 有效数据的长度
         */
        void onStream(@Nullable String address, float progress, byte dataType, byte[] raw, int offset, int len);


        default void onRxProgress(@Nullable String addr, int rxType, int total, int index) {
            /*Do Nothing*/
        }

        default void onTxProgress(@Nullable String addr,  int txType, int total, int index) {
            /*Do Nothing*/
        }
    }
}
