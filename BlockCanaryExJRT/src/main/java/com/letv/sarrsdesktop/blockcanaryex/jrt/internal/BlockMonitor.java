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
import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;
import com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.R;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

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
    private static final long CPU_SAMPLER_INTERVAL = 1000L;

    private static final int MY_PID = Process.myPid();

    private static final LooperMonitor.BlockListener BLOCK_LISTENER = new LooperMonitor.BlockListener() {
        @Override
        public void onStart() {
            long currentTime = SystemClock.uptimeMillis();
            if (currentTime - mLastResetCpuSamplerTime > CPU_SAMPLER_INTERVAL) {
                ISamplerService samplerService = getServiceSyncMayNull();
                if (samplerService != null) {
                    try {
                        samplerService.resetCpuSampler(MY_PID);
                        mLastResetCpuSamplerTime = currentTime;
                    } catch (RemoteException e) {
                        Log.d(TAG, "resetCpuSampler failed.", e);
                    }
                }
            }
        }

        @Override
        public void onBlockEvent(long realStartTime, long realTimeEnd, long threadTimeStart, long threadTimeEnd) {
            List<MethodInfo> methodInfoList = MethodInfoPool.getAllUsed();
            MethodInfoPool.reset();

            String cpuRate = "";
            boolean isBusy = false;
            ISamplerService samplerService = getServiceSyncMayNull();
            if (samplerService != null) {
                try {
                    CpuInfo cpuInfo = samplerService.getCurrentCpuInfo(realStartTime, realTimeEnd);
                    cpuRate = cpuInfo.cpuRate;
                    isBusy = cpuInfo.isBusy;
                } catch (RemoteException e) {
                    Log.d(TAG, "getCurrentCpuInfo() failed.", e);
                }
            }
            final BlockInfo blockInfo = BlockInfo.newInstance(realStartTime, realTimeEnd - realStartTime, threadTimeEnd - threadTimeStart,
                    methodInfoList, cpuRate, isBusy);
            notifyBlocked(blockInfo);
        }

        @Override
        public void onNoBlock() {
            MethodInfoPool.reset();
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

    public static void reportMethodProfile(final String cls, final String method, final String paramTypes, final long startTimeNano, final long startThreadTime,
                                           final long endTimeNano, final long endThreadTime) {
        SamplerReportHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                MethodInfo methodInfo = MethodInfoPool.obtain();
                methodInfo.setCls(cls);
                methodInfo.setMethod(method);
                methodInfo.setParamTypes(paramTypes);
                methodInfo.setCostRealTimeNano(endTimeNano - startTimeNano);
                methodInfo.setCostThreadTime(endThreadTime - startThreadTime);
            }
        });
    }

    public static void registerBlockObserver(BlockObserver blockObserver) {
        sBlockObservers.add(new WeakReference<>(blockObserver));
    }

    public static void install(Config config) {
        Context context = config.getContext();
        MethodInfoPool.setMaxBuffer(context.getResources().getInteger(R.integer.block_canary_max_method_info_buffer));
        ensureMonitorInstalled();
        connectServiceIfNot();
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
