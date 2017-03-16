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

import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.PerformanceUtils;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.ProcessUtils;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.Serializable;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.SerializeException;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.TimeUtils;

import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class BlockInfo implements Serializable {
    private static final String TAG = "BlockInfo";

    private static final String MODEL = Build.MODEL;
    private static final int API_LEVEL = Build.VERSION.SDK_INT;

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

    private static final String KEY_HEAVY_METHOD = "---------------heavy-method-start---------------";
    private static final String KEY_HEAVY_METHOD_END = "---------------heavy-method-end---------------";
    private static final String KEY_FREQUENT_METHOD = "---------------frequent-method-start---------------";
    private static final String KEY_FREQUENT_METHOD_END = "---------------frequent-method-end---------------";
    private static final String KEY_ENVIRONMENT = "---------------environment-start---------------";
    private static final String KEY_ENVIRONMENT_END = "---------------environment-end---------------";

    private static final String KV = " = ";
    private static final String MS = "ms";
    private static final String KB = "KB";
    private static final Comparator<MethodInfo> COMPARATOR = new Comparator<MethodInfo>() {
        @Override
        public int compare(MethodInfo l, MethodInfo r) {
            int result = -compare(l.getCostRealTimeNano(), r.getCostRealTimeNano());
            if(result == 0) {
                result = -compare(l.getCostThreadTime(), r.getCostThreadTime());
            }
            return result;
        }

        private int compare(long x, long y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    };

    private static final Comparator<FrequentMethodInfo> FREQUENT_COMPARATOR = new Comparator<FrequentMethodInfo>() {
        @Override
        public int compare(FrequentMethodInfo lhs, FrequentMethodInfo rhs) {
            long l = lhs.getTotalCostRealTimeMs();
            long r = rhs.getTotalCostRealTimeMs();
            return l < r ? 1 : (l == r ? 0 : -1);
        }
    };

    private long timestamp;

    private String startTime;
    private String endTime;
    private String blockRealTime;
    private String blockThreadTime;
    private String topHeavyMethod;
    private String heavyMethods;
    private String topFrequentMethod;
    private String frequentMethods;
    private String envInfo;

    public static BlockInfo newInstance(long startTime, long blockRealTime, long blockThreadTime, List<MethodInfo> methodInfoList, String cpuRateInfo, boolean isCpuBusy) {
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.timestamp = startTime + blockRealTime;
        blockInfo.startTime = TimeUtils.format(startTime);
        blockInfo.blockRealTime = blockRealTime + MS;
        blockInfo.endTime = TimeUtils.format(blockInfo.timestamp);
        blockInfo.blockThreadTime = blockThreadTime + MS;
        blockInfo.envInfo = generateEnvInfo(cpuRateInfo, isCpuBusy);
        blockInfo.topHeavyMethod = generateTopHeavyMethod(methodInfoList);
        blockInfo.heavyMethods = generateHeavyMethod(methodInfoList);
        blockInfo.calculateFrequentMethods(methodInfoList);
        return blockInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getBlockRealTime() {
        return blockRealTime;
    }

    public String getBlockThreadTime() {
        return blockThreadTime;
    }

    public String getTimeString() {
        return KEY_BlOCK_TIME + KV + blockRealTime + SEPARATOR +
                KEY_BLOCK_THREAD_TIME + KV + blockThreadTime + SEPARATOR +
                KEY_START_TIME + KV + startTime + SEPARATOR +
                KEY_END_TIME + KV + endTime + SEPARATOR;
    }

    public String getTopHeavyMethod() {
        return topHeavyMethod;
    }

    public String getTopFrequentMethod() {
        return topFrequentMethod;
    }

    public String getHeavyMethods() {
        return heavyMethods;
    }

    public String getFrequentMethods() {
        return frequentMethods;
    }

    public String getEnvInfo() {
        return envInfo;
    }

    private static String generateEnvInfo(String cpuRateInfo, boolean isCpuBusy) {
        Config config = BlockCanaryEx.getConfig();

        return KEY_PROCESS_NAME + KV + ProcessUtils.myProcessName(config.getContext()) + SEPARATOR +
                KEY_QUALIFIER + KV + config.provideQualifier() + SEPARATOR +
                KEY_CPU_CORE_NUMBER + KV + PerformanceUtils.getNumCores() + SEPARATOR +
                KEY_CPU_BUSY + KV + isCpuBusy + SEPARATOR +
                KEY_FREE_MEMORY + KV + PerformanceUtils.getFreeMemory() + KB + SEPARATOR +
                KEY_TOTAL_MEMORY + KV + PerformanceUtils.getTotalMemory() + KB + SEPARATOR +
                KEY_CPU_RATE_INFO + KV + SEPARATOR + cpuRateInfo + SEPARATOR +
                KEY_MODEL + KV + MODEL + SEPARATOR +
                KEY_API_LEVEL + KV + API_LEVEL + SEPARATOR +
                KEY_NETWORK_TYPE + KV + config.provideNetworkType() + SEPARATOR +
                KEY_UID + KV + config.provideUid() + SEPARATOR;
    }

    private static String generateHeavyMethod(List<MethodInfo> methodInfoList) {
        StringBuilder heavyMethodBuilder = new StringBuilder();
        List<MethodInfo> tmp = new ArrayList<>(methodInfoList.size());
        tmp.addAll(methodInfoList);
        Collections.sort(tmp, COMPARATOR);
        for (MethodInfo methodInfo : tmp) {
            if (BlockCanaryEx.getConfig().isHeavyMethod(methodInfo)) {
                heavyMethodBuilder.append(methodInfo.toString()).append(SEPARATOR).append(SEPARATOR);
            }
        }
        return heavyMethodBuilder.toString();
    }

    private static String generateTopHeavyMethod(List<MethodInfo> methodInfoList) {
        if (methodInfoList.size() == 0) {
            return "none method be sampled";
        } else {
            MethodInfo mostHeavyMethod = null;
            for (MethodInfo methodInfo : methodInfoList) {
                if (mostHeavyMethod == null || mostHeavyMethod.getCostThreadTime() < methodInfo.getCostThreadTime()) {
                    mostHeavyMethod = methodInfo;
                }
            }
            return mostHeavyMethod.getClassSimpleName() + "." + mostHeavyMethod.getMethodName() + "()" + " cost " + mostHeavyMethod.getCostRealTimeMs() + "ms";
        }
    }

    private void calculateFrequentMethods(List<MethodInfo> methodInfoList) {
        if (methodInfoList == null || methodInfoList.size() == 0) {
            topFrequentMethod = "";
            frequentMethods = "";
            return;
        }

        Map<String, List<MethodInfo>> methodMap = new HashMap<>();
        for (MethodInfo method : methodInfoList) {
            String key = method.generateMethodInfo(false);
            List<MethodInfo> sameMethodList = methodMap.get(key);
            if (sameMethodList == null) {
                sameMethodList = new ArrayList<>();
                methodMap.put(key, sameMethodList);
            }
            sameMethodList.add(method);
        }

        List<FrequentMethodInfo> frequentMethodInfos = new ArrayList<>();
        for (List<MethodInfo> sameMethodList : methodMap.values()) {
            long totalCostNanos = 0;
            int calledTimes = sameMethodList.size();
            MethodInfo currentMethod = null;
            for (MethodInfo method : sameMethodList) {
                if (currentMethod == null) {
                    currentMethod = method;
                }
                totalCostNanos += method.getCostRealTimeNano();
            }
            FrequentMethodInfo frequentMethodInfo = new FrequentMethodInfo(currentMethod, calledTimes, TimeUnit.NANOSECONDS.toMillis(totalCostNanos));
            if (BlockCanaryEx.getConfig().isFrequentMethod(frequentMethodInfo)) {
                frequentMethodInfos.add(frequentMethodInfo);
            }
        }

        StringBuilder frequentMethodBuilder = new StringBuilder();
        Collections.sort(frequentMethodInfos, FREQUENT_COMPARATOR);
        for (int i = 0; i < frequentMethodInfos.size(); i++) {
            FrequentMethodInfo frequentMethodInfo = frequentMethodInfos.get(i);
            if(i == 0) {
                topFrequentMethod = frequentMethodInfo.toString();
                frequentMethodBuilder.append(topFrequentMethod).append(SEPARATOR).append(SEPARATOR);
            } else {
                frequentMethodBuilder.append(frequentMethodInfo.toString()).append(SEPARATOR).append(SEPARATOR);
            }
        }
        if(topFrequentMethod == null) {
            topFrequentMethod = "";
        }
        frequentMethods = frequentMethodBuilder.toString();
    }

    @Override
    public String serialize() throws SerializeException {
        StringBuilder sb = new StringBuilder();
        sb.append(KEY_START_TIME).append(KV).append(startTime).append(SEPARATOR);
        sb.append(KEY_END_TIME).append(KV).append(endTime).append(SEPARATOR);
        sb.append(KEY_BlOCK_TIME).append(KV).append(blockRealTime).append(SEPARATOR);
        sb.append(KEY_BLOCK_THREAD_TIME).append(KV).append(blockThreadTime).append(SEPARATOR);
        sb.append(KEY_TOP_HEAVY_METHOD).append(KV).append(topHeavyMethod).append(SEPARATOR);
        sb.append(KEY_TOP_FREQUENT_METHOD).append(KV).append(topFrequentMethod).append(SEPARATOR);
        sb.append(KEY_TIMESTAMP).append(KV).append(timestamp).append(SEPARATOR);

        sb.append(KEY_ENVIRONMENT).append(SEPARATOR);
        sb.append(envInfo);
        sb.append(KEY_ENVIRONMENT_END).append(SEPARATOR);

        sb.append(KEY_HEAVY_METHOD).append(SEPARATOR);
        sb.append(heavyMethods);
        sb.append(KEY_HEAVY_METHOD_END).append(SEPARATOR);

        sb.append(KEY_FREQUENT_METHOD).append(SEPARATOR);
        sb.append(frequentMethods);
        sb.append(KEY_FREQUENT_METHOD_END).append(SEPARATOR);
        return sb.toString();
    }

    @Override
    public void deserialize(File file) throws SerializeException {
        BufferedReader reader = null;
        Throwable closeException = null;

        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
            reader = new BufferedReader(in);
            boolean heavyMethodsStart = false;
            boolean frequentMethodsStart = false;
            boolean envInfoStart = false;
            StringBuilder heavyMethodsBuilder = new StringBuilder();
            StringBuilder frequentMethodsBuilder = new StringBuilder();
            StringBuilder envInfoBuilder = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (heavyMethodsStart) {
                    if (line.equals(KEY_HEAVY_METHOD_END)) {
                        heavyMethodsStart = false;
                    } else {
                        heavyMethodsBuilder.append(line).append(SEPARATOR);
                    }
                    continue;
                }
                if (frequentMethodsStart) {
                    if (line.equals(KEY_FREQUENT_METHOD_END)) {
                        frequentMethodsStart = false;
                    } else {
                        frequentMethodsBuilder.append(line).append(SEPARATOR);
                    }
                    continue;
                }
                if (envInfoStart) {
                    if (line.equals(KEY_ENVIRONMENT_END)) {
                        envInfoStart = false;
                    } else {
                        envInfoBuilder.append(line).append(SEPARATOR);
                    }
                    continue;
                }
                String prefix = KEY_START_TIME + KV;
                if (line.startsWith(prefix)) {
                    startTime = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_END_TIME + KV;
                if (line.startsWith(prefix)) {
                    endTime = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_BlOCK_TIME + KV;
                if (line.startsWith(prefix)) {
                    blockRealTime = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_BLOCK_THREAD_TIME + KV;
                if (line.startsWith(prefix)) {
                    blockThreadTime = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_TOP_HEAVY_METHOD + KV;
                if (line.startsWith(prefix)) {
                    topHeavyMethod = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_TOP_FREQUENT_METHOD + KV;
                if (line.startsWith(prefix)) {
                    topFrequentMethod = line.substring(prefix.length());
                    continue;
                }
                prefix = KEY_TIMESTAMP + KV;
                if(line.startsWith(prefix)) {
                    timestamp = Long.valueOf(line.substring(prefix.length()));
                    continue;
                }
                prefix = KEY_HEAVY_METHOD;
                if (line.startsWith(prefix)) {
                    heavyMethodsStart = true;
                    continue;
                }
                prefix = KEY_FREQUENT_METHOD;
                if (line.startsWith(prefix)) {
                    frequentMethodsStart = true;
                }
                prefix = KEY_ENVIRONMENT;
                if(line.startsWith(prefix)) {
                    envInfoStart = true;
                }
            }
            heavyMethods = heavyMethodsBuilder.toString();
            frequentMethods = frequentMethodsBuilder.toString();
            envInfo = envInfoBuilder.toString();
        } catch (Throwable e) {
            throw new SerializeException("BlockInfo deserialize failed", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable e) {
                closeException = e;
            }
        }

        if(closeException != null) {
            throw new SerializeException("BlockInfo file io close deserialize failed", closeException);
        }
    }
}
