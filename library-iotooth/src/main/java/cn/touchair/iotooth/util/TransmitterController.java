/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth.util;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.touchair.iotooth.GlobalConfig;

public class TransmitterController implements RxFrameListener {
    private static final String TAG = TransmitterController.class.getSimpleName();

    private static final int WRITE_WAIT_TIME_MILLISECOND = 200;
    private static final int MAX_FRAME_SIZE = 20;
    private static final byte MAX_DATA_FRAME_SIZE = 14;

    private static final byte FRAME_TYPE_HEADER = 0x01;
    private static final byte FRAME_TYPE_DATA = 0x02;

    private static final byte DATA_TYPE_TEXT = 0x01;
    private static final byte DATA_TYPE_IMG_JPEG = 0x02;
    private static final byte DATA_TYPE_IMG_PNG = 0x03;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private final TransmitterAble mCore;
    private final TransmitterCallback mCallback;
    private final HashMap<String, RxPacket> mRxContextMap = new HashMap<>();

    public TransmitterController(@NonNull TransmitterAble core, TransmitterCallback callback) {
        mCore = core;
        mCore.setRxFrameListener(this);
        mCallback = callback;
        mRxContextMap.put("default", new RxPacket());
    }

    public void writeText(String text) {
        writeText(null, text);
    }

    public void writeText(@Nullable String address, String text) {
        byte[] src = text.getBytes(Charset.defaultCharset());
        int frameCount = (int) Math.ceil((float) src.length / MAX_DATA_FRAME_SIZE); /*数据帧数量*/
        mExecutor.submit(() -> {
            byte[] header = generateHeaderFrame(DATA_TYPE_TEXT, frameCount);
            mCore.send(address, header);
            if (GlobalConfig.DEBUG) {
                Log.d(TAG, "writeFrame: " + Arrays.toString(header));
            }
            SystemClock.sleep(WRITE_WAIT_TIME_MILLISECOND);
            int position = 0;
            byte validLen = 0; /*当前帧携带的有效数据个数*/
            byte[] data = new byte[MAX_DATA_FRAME_SIZE];
            for (int i = 0; i < frameCount; ++i) {
                Arrays.fill(data, (byte) 0);
                if (position + MAX_DATA_FRAME_SIZE > src.length) {
                    validLen = (byte) (src.length - position);
                } else {
                    validLen = MAX_DATA_FRAME_SIZE;
                }
                System.arraycopy(src, position, data, 0, validLen);
                if (GlobalConfig.DEBUG) {
                    Log.d(TAG, "writeFrame: " + Arrays.toString(data));
                }
                mCore.send(address, generateDataFrame(i, data, validLen));
                mCallback.onTxProgress(address, DATA_TYPE_TEXT, frameCount, i);
                SystemClock.sleep(WRITE_WAIT_TIME_MILLISECOND);
                position += validLen;
            }
        });
    }

    /*

        |frame type-1byte|frame index-4byte|data len-1byte|data-14byte|

     */

    private byte[] generateDataFrame(int frameIndex, byte[] data, byte len) {
        if (data.length != MAX_DATA_FRAME_SIZE) throw new RuntimeException("Data size must is" + MAX_DATA_FRAME_SIZE);
        byte[] src = new byte[MAX_FRAME_SIZE];
        src[0] = FRAME_TYPE_DATA;
        byte[] frameIndexSrc = TypeUtils.asByteArray(frameIndex);
        System.arraycopy(frameIndexSrc, 0, src, 1, frameIndexSrc.length);
        src[5] = len;
        System.arraycopy(data, 0, src, 6, MAX_DATA_FRAME_SIZE);
        return src;
    }


    /*

        |frame type-1byte|data type-1byte|frame count-4byte|reserved|

     */
    private byte[] generateHeaderFrame(byte dataType, int frameCount) {
        byte[] src = new byte[MAX_FRAME_SIZE];
        Arrays.fill(src, (byte) 0);
        src[0] = FRAME_TYPE_HEADER;
        src[1] = dataType;
        byte[] frameLength = TypeUtils.asByteArray(frameCount);
        System.arraycopy(frameLength, 0, src, 2, frameLength.length);
        return src;
    }

    @Override
    public void onFrame(int offset, byte[] frame, @Nullable String addr) {

        RxPacket packet;
        if (addr == null) {
            packet = mRxContextMap.get("default");
        } else {
            packet = mRxContextMap.get(addr);
            if (packet == null) {
                packet = new RxPacket();
                mRxContextMap.put(addr, packet);
            }
        }

        assert packet != null;

        packet.putFrame(addr, frame);
        if ((packet.type & DATA_TYPE_TEXT) == DATA_TYPE_TEXT) {
            if (packet.isEnd()) {
                mCallback.onText(addr, packet.toText());
            }
        }
    }

    private class RxPacket {
        int frameCount;
        int frameIndex = -1;

        byte type;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        void putFrame(@Nullable String addr, byte[] frame) {
            byte[] temp = new byte[4];
            switch (frame[0]) {
                case FRAME_TYPE_HEADER:
                    type = frame[1];
                    System.arraycopy(frame, 2, temp, 0, temp.length);
                    frameCount = TypeUtils.asInt(temp);
                    frameIndex = -1;
                    buffer.reset();
                    break;
                case FRAME_TYPE_DATA:
                    System.arraycopy(frame, 1, temp, 0, temp.length);
                    frameIndex = TypeUtils.asInt(temp);
                    int validLen = frame[5];
                    buffer.write(frame, 6, validLen);
                    try {
                        buffer.flush();
                    } catch (IOException ignored) {}
                    mCallback.onRxProgress(addr, type, frameCount, frameIndex + 1);
                    break;
                default:
            }
        }

        boolean isEnd() {
            return frameCount == frameIndex + 1;
        }

        byte[] toRaw() {
            return buffer.toByteArray();
        }

        String toText() {
            return buffer.toString();
        }
    }

    public interface TransmitterCallback {
        void onText(@Nullable String address, @NonNull String text);
        void onStream(@Nullable String address, float progress, byte[] raw);


        default void onRxProgress(@Nullable String addr, int rxType, int total, int index) {
            /*Do Nothing*/
        }

        default void onTxProgress(@Nullable String addr,  int txType, int total, int index) {
            /*Do Nothing*/
        }
    }
}
