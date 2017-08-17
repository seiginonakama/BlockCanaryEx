/*
 * Copyright (C) 2017 seiginonakama (https://github.com/seiginonakama).
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

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;
import com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * author: zhoulei date: 2017/3/2.
 */
public class BlockMonitor {
    private static final String TAG = "BlockMonitor";

    public interface BlockObserver {
        void onBlock(BlockInfo blockInfo);
    }

    private static ConnectServiceFuture sConnectServiceFuture;

    private static long mLastResetCpuSamplerTime = 0;
    private static final long RESET_SAMPLER_INTERVAL = 1000L;

    private static final int MY_PID = Process.myPid();

    private static final long INSTALLED_TIME = System.currentTimeMillis();
    private static final long INSTALLED_THREAD_TIME = SystemClock.currentThreadTimeMillis();

    private static ArrayPool<MethodInfo> sMethodInfoPool;

    private static final LooperMonitor.BlockListener BLOCK_LISTENER = new LooperMonitor.BlockListener() {
        @Override
        public void beforeFirstStart(long firstStartTime, long firstStartThreadTime, String creatingActivity) {
            if (BlockCanaryEx.getConfig().isBlock(firstStartTime - INSTALLED_TIME, firstStartThreadTime - INSTALLED_THREAD_TIME,
                    creatingActivity, true, 0L)) {
                onBlockEvent(INSTALLED_TIME, firstStartTime, INSTALLED_THREAD_TIME, firstStartThreadTime, null);
            }
        }

        @Override
        public void onStart(long startTime) {
            long currentTime = SystemClock.uptimeMillis();
            if (currentTime - mLastResetCpuSamplerTime > RESET_SAMPLER_INTERVAL) {
                ISamplerService samplerService = getServiceSyncMayNull();
                if (samplerService != null) {
                    try {
                        samplerService.resetSampler(MY_PID, startTime);
                        mLastResetCpuSamplerTime = currentTime;
                    } catch (RemoteException e) {
                        Log.d(TAG, "resetCpuSampler failed.", e);
                    }
                }
            }
        }

        @Override
        public void onBlockEvent(long realStartTime, long realEndTime, long threadTimeStart, long threadTimeEnd,
                                 List<ViewPerformanceInfo> viewPerformanceInfos) {
            List<MethodInfo> methodInfoList = sMethodInfoPool.getAllUsed();
            sMethodInfoPool.reset();

            String cpuRate = "";
            boolean isBusy = false;
            List<GcInfo> gcInfos = null;
            ISamplerService samplerService = getServiceSyncMayNull();
            if (samplerService != null) {
                try {
                    CpuInfo cpuInfo = samplerService.getCurrentCpuInfo(realStartTime, realEndTime);
                    cpuRate = cpuInfo.cpuRate;
                    isBusy = cpuInfo.isBusy;
                    gcInfos = samplerService.popGcInfoBetween(realStartTime, realEndTime);
                } catch (RemoteException e) {
                    Log.d(TAG, "get CpuInfo or GcInfo failed.", e);
                }
            }
            long blockRealTime = realEndTime - realStartTime;
            //ignore the block from block canary
            if (!isBlockCanaryExBlocked(methodInfoList)) {
                final BlockInfo blockInfo = BlockInfo.newInstance(realStartTime, blockRealTime, threadTimeEnd - threadTimeStart,
                        methodInfoList, cpuRate, isBusy, gcInfos, viewPerformanceInfos);
                notifyBlocked(blockInfo);
            }
        }

        @Override
        public void onNoBlock() {
            sMethodInfoPool.reset();
        }
    };

    private static final LogWriter.LogListener LOG_LISTENER = new LogWriter.LogListener() {
        @Override
        public void onNewLog(File log) {
            ISamplerService samplerService = getServiceSyncMayNull();
            if (samplerService != null) {
                try {
                    samplerService.notifyNewLog(BlockCanaryEx.getConfig().provideLogPath(), log.getAbsolutePath());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static final LooperMonitor LOOPER_MONITOR = new LooperMonitor(BLOCK_LISTENER);
    private static final List<WeakReference<BlockObserver>> sBlockObservers = new ArrayList<>();

    private static boolean isBlockCanaryExBlocked(List<MethodInfo> methodInfos) {
        if (methodInfos == null) {
            return false;
        }
        MethodInfo top = null;
        for (MethodInfo methodInfo : methodInfos) {
            if (top == null || top.getCostRealTimeMs() < methodInfo.getCostRealTimeMs()) {
                top = methodInfo;
            }
        }
        return top != null && top.getClassName().startsWith("com.letv.sarrsdesktop.blockcanaryex.jrt");
    }

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

    static void reportMethodProfile(final String cls, final String method, final String paramTypes, final long startTimeNano, final long startThreadTime,
                                    final long endTimeNano, final long endThreadTime) {
        SamplerReportHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if(sMethodInfoPool == null) {
                    //if somebody has not call install(), the sMethodInfoPool is null
                    sMethodInfoPool = new ArrayPool<>(1024, MethodInfo.class);
                }
                MethodInfo methodInfo = sMethodInfoPool.obtain();
                methodInfo.setCls(cls);
                methodInfo.setMethod(method);
                methodInfo.setParamTypes(paramTypes);
                methodInfo.setCostRealTimeNano(endTimeNano - startTimeNano);
                methodInfo.setCostThreadTime(endThreadTime - startThreadTime);
            }
        });
    }

    static void reportActivityCreated(final String activityClass) {
        LOOPER_MONITOR.currentCreatingActivity = activityClass;
    }

    public static void registerBlockObserver(BlockObserver blockObserver) {
        sBlockObservers.add(new WeakReference<>(blockObserver));
    }

    public static void install(Config config) {
        Context context = config.getContext();
        sMethodInfoPool = new ArrayPool<>(context.getResources().getInteger(R.integer.block_canary_ex_max_method_info_buffer),
                MethodInfo.class);
        ensureMonitorInstalled();
        connectServiceIfNot();
        LogWriter.registerLogListener(LOG_LISTENER);
    }

    //only running on main thread
    public static void ensureMonitorInstalled() {
        Looper.getMainLooper().setMessageLogging(LOOPER_MONITOR);
    }

    private static ISamplerService getServiceSyncMayNull() {
        if (sConnectServiceFuture == null) {
            return null;
        } else {
            try {
                return sConnectServiceFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.d(TAG, "connect service failed.", e);
            }
            return null;
        }
    }

    private static void connectServiceIfNot() {
        if (sConnectServiceFuture == null) {
            sConnectServiceFuture = new ConnectServiceFuture();
            Config config = BlockCanaryEx.getConfig();
            if (config != null) {
                Context context = config.getContext();
                Intent intent = new Intent(context, BlockSamplerService.class);
                context.bindService(intent, sConnectServiceFuture, Context.BIND_AUTO_CREATE);
            }
        }
    }
}
