package com.letv.sarrsdesktop.blockcanaryex.demo;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.app.Application;

/**
 * author: zhoulei date: 2017/3/6.
 */
public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BlockCanaryEx.install(new Config(this) {
            @Override
            public boolean isBlock(long startTime, long endTime, long startThreadTime, long endThreadTime) {
                long costRealTime = endTime - startTime;
                return costRealTime > 100L;
            }

            @Override
            public boolean isHeavyMethod(com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo methodInfo) {
                return methodInfo.getCostRealTimeMs() > 2L || methodInfo.getCostThreadTime() > 1L;
            }

            @Override
            public boolean displayNotification() {
                return true;
            }
        });
    }
}
