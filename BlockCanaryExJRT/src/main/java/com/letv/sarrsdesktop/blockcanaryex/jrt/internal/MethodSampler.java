/*
 * Copyright (C) 2017 lqcandqq13 (https://github.com/lqcandqq13).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        BlockMonitor.ensureMonitorInstalled();

        BlockMonitor.reportMethodProfile(cls, method, argTypes, startTimeNano, startThreadTime, System.nanoTime(), SystemClock.currentThreadTimeMillis());
    }
}
