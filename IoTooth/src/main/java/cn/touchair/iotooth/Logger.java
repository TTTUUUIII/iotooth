/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.iotooth;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collection;

public final class Logger {
    private static final int TABLE_WIDTH = 50;
    private final String TAG;
    StringBuilder table = new StringBuilder();

    private Logger(@NonNull String tag){
        TAG = tag;
    }
    public <T> void debug(String title, Collection<T> info) {
        if (GlobalConfig.DEBUG) {
            table.append(title);
            for (int i = 0; i < TABLE_WIDTH - title.length(); ++i) {
                table.append(">");
            }
            table.append("<");
            for (T s : info) {
                table.append("\n*\t" + s);
            }
            table.append("\n");
            for (int i = 0; i < TABLE_WIDTH; ++i) {
                table.append(">");
            }
            table.append("<\n");
            Log.d(TAG, table.toString());
            table.delete(0, table.length());
        }
    }

    public static<T> Logger getLogger(Class<T> clazz) {
        Logger logger = new Logger(clazz.getSimpleName());
        return logger;
    }
}
