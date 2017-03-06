package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * author: zhoulei date: 2017/2/28.
 */
public class WriteLogHandler extends Handler {
    private static volatile WriteLogHandler sInstance;
    private static HandlerThread sHandlerThread = new HandlerThread("BlockCanaryExLogWriter");

    private WriteLogHandler(Looper looper) {
        super(looper);
    }

    public static WriteLogHandler getInstance() {
        if(sInstance == null) {
            synchronized (WriteLogHandler.class) {
                if(sInstance == null) {
                    sHandlerThread.start();
                    sInstance = new WriteLogHandler(sHandlerThread.getLooper());
                }
            }
        }
        return sInstance;
    }
}
