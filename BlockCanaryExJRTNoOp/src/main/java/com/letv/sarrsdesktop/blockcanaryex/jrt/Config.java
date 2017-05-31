/*
 * Copyright (C) 2016 MarkZhai (http://zhaiyifan.cn).
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
package com.letv.sarrsdesktop.blockcanaryex.jrt;

import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.BlockMonitor;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Looper;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class Config implements BlockMonitor.BlockObserver {
    public Config(Context context) {
    }

    public final Context getContext() {
        return null;
    }

    public final Looper provideWatchLooper() {
        return null;
    }

    public boolean displayNotification() {
        return false;
    }

    public boolean isBlock(long costRealTimeMs, long costThreadTimeMs,
                           String creatingActivity, boolean isApplicationCreating, long inflateCostTimeMs) {
        return false;
    }

    public boolean isHeavyMethod(MethodInfo methodInfo) {
        return false;
    }

    public boolean isFrequentMethod(FrequentMethodInfo frequentMethodInfo) {
        return false;
    }

    public boolean enableSaveLog() {
        return false;
    }

    public String provideLogPath() {
        return null;
    }

    public String provideNetworkType() {
        return null;
    }

    public String provideUid() {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    public String provideQualifier() {
        return null;
    }

    @Override
    public void onBlock(BlockInfo blockInfo) {
    }
}
