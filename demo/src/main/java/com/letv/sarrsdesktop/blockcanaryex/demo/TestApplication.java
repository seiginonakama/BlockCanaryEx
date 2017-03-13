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
        if(BuildConfig.DEBUG) {
            //we don't suggest use BlockCanaryEx on release version
            //TODO add no-op version BlockCanaryEx
            BlockCanaryEx.install(new Config(this));
        }
    }
}
