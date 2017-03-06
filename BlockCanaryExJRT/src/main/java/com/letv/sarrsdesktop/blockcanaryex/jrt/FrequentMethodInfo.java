package com.letv.sarrsdesktop.blockcanaryex.jrt;

/**
 * author: zhoulei date: 2017/3/3.
 */
public class FrequentMethodInfo {
    private com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo methodInfo;
    private int calledTimes;
    private long totalCostRealTimeMs;

    FrequentMethodInfo(com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo methodInfo, int calledTimes, long totalCostRealTimeMs) {
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
