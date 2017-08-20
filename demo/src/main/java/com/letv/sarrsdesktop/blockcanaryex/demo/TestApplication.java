package com.letv.sarrsdesktop.blockcanaryex.demo;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.app.Application;
import android.content.Context;

import java.util.Random;

/**
 * author: zhoulei date: 2017/3/6.
 */
public class TestApplication extends Application {
    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        boolean isInSamplerProcess = BlockCanaryEx.isInSamplerProcess(this);
        if(!isInSamplerProcess) {
            BlockCanaryEx.install(new Config(this));
        }
        if(!isInSamplerProcess) {
            doHeavyWork();
        }
    }

    private void doHeavyWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 300L) {
            random.nextInt(Integer.MAX_VALUE);
        }
    }
}
