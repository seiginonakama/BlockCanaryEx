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
package com.letv.sarrsdesktop.blockcanaryex.jrt;

/**
 * author: zhoulei date: 2017/3/3.
 */
public class FrequentMethodInfo {
    private MethodInfo methodInfo;
    private int calledTimes;
    private long totalCostRealTimeMs;

    FrequentMethodInfo(MethodInfo methodInfo, int calledTimes, long totalCostRealTimeMs) {
        this.methodInfo = methodInfo;
        this.calledTimes = calledTimes;
        this.totalCostRealTimeMs = totalCostRealTimeMs;
    }

    public String getClassName() {
        return methodInfo.getClassName();
    }

    public String getMethodName() {
        return methodInfo.getMethodName();
    }

    public String getParamTypes() {
        return methodInfo.getParamTypes();
    }

    public int getCalledTimes() {
        return calledTimes;
    }

    public long getTotalCostRealTimeMs() {
        return totalCostRealTimeMs;
    }

    @Override
    public String toString() {
        return methodInfo.generateMethodInfo(true) + " called " + calledTimes + " times" + ", " + "total block " + totalCostRealTimeMs + "ms";
    }
}
