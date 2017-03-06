package com.letv.sarrsdesktop.blockcanaryex.jrt;

import java.util.concurrent.TimeUnit;

/**
 * author: zhoulei date: 2017/2/28.
 */
public class MethodInfo {
    private String cls;
    private String method;
    private String paramTypes;
    private long costRealTimeNano;
    private long costThreadTime;

    public long getCostRealTimeMs() {
        return TimeUnit.NANOSECONDS.toMillis(costRealTimeNano);
    }

    public long getCostRealTimeNano() {
        return this.costRealTimeNano;
    }

    public void setCostRealTimeNano(long costRealTimeNano) {
        this.costRealTimeNano = costRealTimeNano;
    }

    public long getCostThreadTime() {
        return costThreadTime;
    }

    public void setCostThreadTime(long costThreadTime) {
        this.costThreadTime = costThreadTime;
    }

    public String getClassName() {
        return cls;
    }

    public String getClassSimpleName() {
        return getClassSimpleName(cls);
    }

    public void setCls(String cls) {
        this.cls = cls;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParamTypes(String paramTypes) {
        this.paramTypes = paramTypes;
    }

    private String getClassSimpleName(String clazz) {
        int dot = clazz.lastIndexOf('.');
        if (dot != -1) {
            return clazz.substring(dot + 1);
        }

        return clazz;
    }

    public String getMethodName() {
        return method;
    }

    public String getSimpleParamTypes() {
        if (paramTypes != null && paramTypes.length() > 0) {
            String[] types = paramTypes.split(",");
            StringBuilder simpleParamTypesBuilder = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                simpleParamTypesBuilder.append(getClassSimpleName(types[i]));
                if (i != types.length - 1) {
                    simpleParamTypesBuilder.append(",");
                }
            }
            return simpleParamTypesBuilder.toString();
        } else {
            return "";
        }
    }

    public String getParamTypes() {
        return paramTypes;
    }

    public boolean isConstructor() {
        return getClassSimpleName().equals(getMethodName());
    }

    public boolean isClassInitializer() {
        return "<clinit>".equals(method);
    }

    public String generateMethodInfo(boolean simpleParamType) {
        StringBuilder methodInfoBuilder = new StringBuilder();
        methodInfoBuilder.append(cls)
                .append(".")
                .append(method)
                .append("(");
        if (paramTypes != null) {
            methodInfoBuilder.append(simpleParamType ? getSimpleParamTypes() : paramTypes);
        }
        return methodInfoBuilder.append(")").toString();
    }

    @Override
    public String toString() {
        return generateMethodInfo(true) + " costRealTime:" + getCostRealTimeMs() + "ms" + " costThreadTime:" + getCostThreadTime() + "ms";
    }
}
