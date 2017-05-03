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

import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.Serializable;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.SerializeException;

import java.io.File;
import java.util.List;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class BlockInfo implements Serializable {
    public static final String SEPARATOR = "\r\n";

    public static final String KEY_BlOCK_TIME = "blocked-time";
    public static final String KEY_BLOCK_THREAD_TIME = "blocked-thread-time";
    public static final String KEY_START_TIME = "time-start";
    public static final String KEY_END_TIME = "time-end";
    public static final String KEY_TOP_HEAVY_METHOD = "top-heavy-method";
    public static final String KEY_TOP_FREQUENT_METHOD = "top-frequent-heavy-method";
    public static final String KEY_TIMESTAMP = "time-stamp";

    public static final String KEY_PROCESS_NAME = "process-name";
    public static final String KEY_CPU_CORE_NUMBER = "cpu-core-number";
    public static final String KEY_CPU_RATE_INFO = "cpu-rate-info";
    public static final String KEY_CPU_BUSY = "cpu-busy";
    public static final String KEY_FREE_MEMORY = "free-memory";
    public static final String KEY_TOTAL_MEMORY = "total-memory";
    public static final String KEY_MODEL = "model";
    public static final String KEY_API_LEVEL = "api-level";
    public static final String KEY_NETWORK_TYPE = "network-type";
    public static final String KEY_QUALIFIER = "qualifier";
    public static final String KEY_UID = "uid";

    public static BlockInfo newInstance(long startTime, long blockRealTime, long blockThreadTime, List<MethodInfo> methodInfoList, String cpuRateInfo, boolean isCpuBusy, List gcInfos,
                                        List viewPerformanceInfos) {
        return null;
    }

    public long getTimestamp() {
        return 0;
    }

    public String getStartTime() {
        return null;
    }

    public String getEndTime() {
        return null;
    }

    public String getBlockRealTime() {
        return null;
    }

    public String getBlockThreadTime() {
        return null;
    }

    public String getTimeString() {
        return null;
    }

    public String getTopHeavyMethod() {
        return null;
    }

    public String getTopFrequentMethod() {
        return null;
    }

    public String getHeavyMethods() {
        return null;
    }

    public String getFrequentMethods() {
        return null;
    }

    public String getGcEvent() {
        return null;
    }

    public String getEnvInfo() {
        return null;
    }

    public String getViewPerformance() {
        return null;
    }

    @Override
    public String serialize() throws SerializeException {
        return null;
    }

    @Override
    public void deserialize(File file) throws SerializeException {
    }
}
