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
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.LogWriter;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.ProcessUtils;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.WriteLogHandler;
import com.letv.sarrsdesktop.blockcanaryex.jrt.ui.DisplayActivity;
import com.letv.sarrsdesktop.blockcanaryex.jrt.ui.DisplayService;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

/**
 * add method sampler, base on BlockCanary
 * <p>
 * author: zhoulei date: 2017/2/28.
 */
public class BlockCanaryEx {
    private static final String TAG = "BlockCanaryEx";
    private static Config sConfig;
    private static final DisplayService DISPLAY_SERVICE = new DisplayService();
    private static final String BLOCK_SAMPLER_SERVICE_NAME = "blockcanaryex";

    /**
     * begin block monitor
     *
     * @param config {@link Config}
     */
    public static void install(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config must not be null!");
        }
        if (isInstalled()) {
            throw new IllegalStateException("BlockCanaryEx installed");
        }
        sConfig = config;
        BlockMonitor.install(config);
        BlockMonitor.registerBlockObserver(sConfig);
        LogWriter.cleanObsolete();
        BlockMonitor.registerBlockObserver(DISPLAY_SERVICE);

        setEnabled(sConfig.getContext(), DisplayActivity.class, sConfig.displayNotification());
    }

    /**
     * whether block hunter is installed
     *
     * @return true if installed, else false
     */
    public static boolean isInstalled() {
        return sConfig != null;
    }

    /**
     * return the config we get whe {@link #install(Config)}
     *
     * @return config
     */
    public static Config getConfig() {
        return sConfig;
    }

    /**
     * whether current process is sampler process,
     * app should not do anything in sampler process
     *
     * @return true if it is, else false
     */
    public static boolean isInSamplerProcess(Context context) {
        String processName = ProcessUtils.myProcessName(context);
        return processName != null && processName.endsWith(":" + BLOCK_SAMPLER_SERVICE_NAME);
    }

    private static void setEnabled(final Context context, final Class<?> componentClass, final boolean enabled) {
        WriteLogHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                setEnabledBlocking(context, componentClass, enabled);
            }
        });
    }

    private static void setEnabledBlocking(Context appContext,
                                           Class<?> componentClass,
                                           boolean enabled) {
        ComponentName component = new ComponentName(appContext, componentClass);
        PackageManager packageManager = appContext.getPackageManager();
        int newState = enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
        // Blocks on IPC.
        try {
            //may throw exception if component don't exist
            packageManager.setComponentEnabledSetting(component, newState, DONT_KILL_APP);
        } catch (Throwable t) {
            Log.e(TAG, "setEnabledBlocking failed:" + t.toString());
        }
    }
}
