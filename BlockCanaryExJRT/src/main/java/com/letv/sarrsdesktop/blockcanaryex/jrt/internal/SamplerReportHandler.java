package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * author: zhoulei date: 2017/2/28.
 */
public class SamplerReportHandler extends Handler {
    private static volatile SamplerReportHandler sInstance;
    private static HandlerThread sHandlerThread = new HandlerThread("BlockCanaryExSampler");

    private SamplerReportHandler(Looper looper) {
        super(looper);
    }

    public static SamplerReportHandler getInstance() {
        if(sInstance == null) {
            synchronized (SamplerReportHandler.class) {
                if(sInstance == null) {
                    sHandlerThread.start();
                    sInstance = new SamplerReportHandler(sHandlerThread.getLooper());
                }
            }
        }
        return sInstance;
    }
}
