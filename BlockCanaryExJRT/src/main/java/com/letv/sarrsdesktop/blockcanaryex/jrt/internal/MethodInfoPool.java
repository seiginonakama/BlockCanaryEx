package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * non thread safe
 *
 * author: zhoulei date: 2017/3/16.
 */
class MethodInfoPool {
    private static int sMaxBuffer = 1024;
    private static MethodInfo[] sMethodInfoPool = new MethodInfo[sMaxBuffer];
    private static int sLastIndex = -1;

    static void setMaxBuffer(int maxBuffer) {
        if(maxBuffer > 0) {
            sMaxBuffer = maxBuffer;
        } else {
            throw new IllegalArgumentException("maxBuffer must > 0");
        }
    }

    static MethodInfo obtain() {
        sLastIndex++;

        if (sLastIndex > sMethodInfoPool.length - 1) {
            MethodInfo[] tmp = new MethodInfo[sMethodInfoPool.length * 2];
            System.arraycopy(sMethodInfoPool, 0, tmp, 0, sMethodInfoPool.length);
            sMethodInfoPool = tmp;
        }

        MethodInfo methodInfo = sMethodInfoPool[sLastIndex];
        if (methodInfo == null) {
            methodInfo = new MethodInfo();
            sMethodInfoPool[sLastIndex] = methodInfo;
        }
        return methodInfo;
    }

    static List<MethodInfo> getAllUsed() {
        List<MethodInfo> methodInfoList;
        if (sLastIndex < 0) {
            methodInfoList = Collections.EMPTY_LIST;
        } else {
            methodInfoList = new ArrayList<>(sLastIndex + 1);
            for (int i = 0; i <= sLastIndex; i++) {
                methodInfoList.add(sMethodInfoPool[i]);
            }
        }
        return methodInfoList;
    }

    static void reset() {
        sLastIndex = -1;
        if(sMethodInfoPool.length > sMaxBuffer) {
            MethodInfo[] tmp = new MethodInfo[sMaxBuffer];
            System.arraycopy(sMethodInfoPool, 0, tmp, 0, sMaxBuffer);
            sMethodInfoPool = tmp;
        }
    }
}
