package com.letv.sarrsdesktop.blockcanaryex.jrt;

import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.BlockMonitor;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.LogWriter;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.WriteLogHandler;
import com.letv.sarrsdesktop.blockcanaryex.jrt.ui.DisplayActivity;
import com.letv.sarrsdesktop.blockcanaryex.jrt.ui.DisplayService;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

/**
 * add method sampler, base on BlockCanary
 * <p>
 * author: zhoulei date: 2017/2/28.
 */
public class BlockCanaryEx {

    private static Config sConfig;
    private static final DisplayService DISPLAY_SERVICE = new DisplayService();

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
        BlockMonitor.ensureMonitorInstalled();
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
        packageManager.setComponentEnabledSetting(component, newState, DONT_KILL_APP);
    }
}
