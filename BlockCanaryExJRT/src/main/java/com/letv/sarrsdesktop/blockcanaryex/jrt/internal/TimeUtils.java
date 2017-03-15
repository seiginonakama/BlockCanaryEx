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
