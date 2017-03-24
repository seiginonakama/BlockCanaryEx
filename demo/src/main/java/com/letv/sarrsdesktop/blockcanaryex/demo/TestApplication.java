package com.letv.sarrsdesktop.blockcanaryex.demo;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.app.Application;

import java.util.Random;

/**
 * author: zhoulei date: 2017/3/6.
 */
public class TestApplication extends Application {
    @Override
    public void onCreate() {
        if(!BlockCanaryEx.isInSamplerProcess(this)) {
            BlockCanaryEx.install(new Config(this));
        }
        super.onCreate();

        doHeavyWork();
    }

    private void doHeavyWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 300L) {
            random.nextInt(Integer.MAX_VALUE);
        }
    }
}
