package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.os.Looper;
import android.os.SystemClock;

/**
 * used by aop inject code
 *
 * author: zhoulei date: 2017/3/1.
 */
public class MethodSampler {
    private MethodSampler() {
    }

    /**
     * this method called by injected code, so do't delete this
     */
    public static boolean isConcernLooper() {
        Config config = BlockCanaryEx.getConfig();
        return config != null && Looper.myLooper() == config.provideWatchLooper();
    }

    /**
     * this method called by injected code, so do't delete this
     *
     */
    public static void onMethodExit(long startTimeNano, long startThreadTime, String cls, String method, String argTypes) {
        com.letv.sarrsdesktop.blockcanaryex.jrt.internal.BlockMonitor.ensureMonitorInstalled();

        com.letv.sarrsdesktop.blockcanaryex.jrt.internal.BlockMonitor.reportMethodProfile(cls, method, argTypes, startTimeNano, startThreadTime, System.nanoTime(), SystemClock.currentThreadTimeMillis());
    }
}
