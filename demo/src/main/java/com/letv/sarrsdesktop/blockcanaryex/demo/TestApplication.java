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
        if(!BlockCanaryEx.isInSamplerProcess(this)) {
            BlockCanaryEx.install(new Config(this));
        }
    }
}
