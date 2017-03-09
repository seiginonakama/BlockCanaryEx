package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo;

import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class BlockMonitor {
    public interface BlockObserver {
        void onBlock(BlockInfo blockInfo);
    }

    private static MethodInfo[] sMethodInfoPool = new MethodInfo[512];
    private static int sPoolLastIndex = -1;

    private static final LooperMonitor.BlockListener BLOCK_LISTENER = new LooperMonitor.BlockListener() {
        @Override
        public void onStart() {
            CpuSampler.getInstance().resetSampleIfNoFresh();
        }

        @Override
        public void onBlockEvent(long realStartTime, long realTimeEnd, long threadTimeStart, long threadTimeEnd) {
            List<MethodInfo> methodInfoList;
            if(sPoolLastIndex < 0) {
                methodInfoList = Collections.EMPTY_LIST;
            } else {
                methodInfoList = new ArrayList<>(sPoolLastIndex + 1);
                for(int i = 0; i <= sPoolLastIndex; i++) {
                    methodInfoList.add(sMethodInfoPool[i]);
                }
            }
            resetMethodInfoPool();

            CpuSampler cpuSampler = CpuSampler.getInstance();
            cpuSampler.recordSample();
            final BlockInfo blockInfo = BlockInfo.newInstance(realStartTime, realTimeEnd - realStartTime, threadTimeEnd - threadTimeStart,
                    methodInfoList, cpuSampler.getCpuRateInfo(), cpuSampler.isCpuBusy(realStartTime, realTimeEnd));
            notifyBlocked(blockInfo);
        }

        @Override
        public void onNoBlock() {
            resetMethodInfoPool();
        }
    };

    private static final LooperMonitor LOOPER_MONITOR = new LooperMonitor(BLOCK_LISTENER);
    private static final List<WeakReference<BlockObserver>> sBlockObservers = new ArrayList<>();

    private static void notifyBlocked(BlockInfo blockInfo) {
        Iterator<WeakReference<BlockObserver>> iterator = sBlockObservers.iterator();
        while (iterator.hasNext()) {
            WeakReference<BlockObserver> observerWeakReference = iterator.next();
            BlockObserver observer = observerWeakReference.get();
            if (observer == null) {
                iterator.remove();
            } else {
                observer.onBlock(blockInfo);
            }
        }
    }

    public static void registerBlockObserver(BlockObserver blockObserver) {
        sBlockObservers.add(new WeakReference<>(blockObserver));
    }

    public static void ensureMonitorInstalled() {
        Looper.getMainLooper().setMessageLogging(LOOPER_MONITOR);
    }

    public static void reportMethodProfile(final String cls, final String method, final String paramTypes, final long startTimeNano, final long startThreadTime,
                                           final long endTimeNano, final long endThreadTime) {
        SamplerReportHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                MethodInfo methodInfo = obtainMethodInfo();
                methodInfo.setCls(cls);
                methodInfo.setMethod(method);
                methodInfo.setParamTypes(paramTypes);
                methodInfo.setCostRealTimeNano(endTimeNano - startTimeNano);
                methodInfo.setCostThreadTime(endThreadTime - startThreadTime);
            }
        });
    }

    public static long getLoopStartTime() {
        return LOOPER_MONITOR.getStartTimestamp();
    }

    public static long getLoopStartThreadTime() {
        return LOOPER_MONITOR.getStartThreadTimestamp();
    }

    private static void resetMethodInfoPool() {
        sPoolLastIndex = -1;
    }

    private static MethodInfo obtainMethodInfo() {
        sPoolLastIndex++;

        if(sPoolLastIndex > sMethodInfoPool.length - 1) {
            MethodInfo[] tmp = new MethodInfo[sMethodInfoPool.length * 2];
            System.arraycopy(sMethodInfoPool, 0, tmp, 0, sMethodInfoPool.length);
            sMethodInfoPool = tmp;
        }

        MethodInfo methodInfo = sMethodInfoPool[sPoolLastIndex];
        if(methodInfo == null) {
            methodInfo = new MethodInfo();
            sMethodInfoPool[sPoolLastIndex] = methodInfo;
        }
        return methodInfo;
    }
}
