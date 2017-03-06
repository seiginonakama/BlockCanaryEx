package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class TimeUtils {
    private static SimpleDateFormat sSimpleDateFormat;

    public static String format(long mills) {
        if(sSimpleDateFormat == null) {
            Config config = BlockCanaryEx.getConfig();
            Locale locale;
            if(config != null) {
                locale = config.getContext().getResources().getConfiguration().locale;
            } else {
                locale = Locale.getDefault();
            }
            sSimpleDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", locale);
        }
        return sSimpleDateFormat.format(mills);
    }
}
