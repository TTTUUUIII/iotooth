package cn.touchair.iotooth.util;

import java.nio.ByteBuffer;

public class TypeUtils {

    public static byte[] asByteArray(int src) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(src).array();
    }

    public static int asInt(byte[] src) {
        return ByteBuffer.wrap(src).getInt();
    }
}
