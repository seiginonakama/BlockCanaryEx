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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.List;

/**
 * author: zhoulei date: 2017/3/15.
 */
public class BlockSamplerService extends Service {
    private static final byte[] CPU_SAMPLER_LOCK = new byte[0];
    private final RemoteCallbackList<INewLogListener> mNewLogListeners = new RemoteCallbackList<>();

    private ISamplerService.Stub mSamplerService = new ISamplerService.Stub() {
        @Override
        public void resetSampler(int pid, long startTime) throws RemoteException {
            synchronized (CPU_SAMPLER_LOCK) {
                GcSampler.startIfNot(pid);
                GcSampler.clearGcInfoBefore(startTime);
                CpuSampler.getInstance().resetSampler(pid);
            }
        }

        @Override
        public CpuInfo getCurrentCpuInfo(long startTime, long endTime) throws RemoteException {
            CpuInfo cpuInfo = new CpuInfo();
            synchronized (CPU_SAMPLER_LOCK) {
                CpuSampler.getInstance().recordSample();
                cpuInfo.cpuRate = CpuSampler.getInstance().getCpuRateInfo();
                cpuInfo.isBusy = CpuSampler.getInstance().isCpuBusy(startTime, endTime);
            }
            return cpuInfo;
        }

        @Override
        public List<GcInfo> popGcInfoBetween(long startTime, long endTime) throws RemoteException {
            return GcSampler.popGcInfoBetween(startTime, endTime);
        }

        @Override
        public void notifyNewLog(String logRootDir, String newLogPath) throws RemoteException {
            LogWriter.initIfNotInited(getApplicationContext(), logRootDir, false);

            int count = mNewLogListeners.beginBroadcast();
            for (int i = 0; i < count; i++) {
                INewLogListener listener = mNewLogListeners.getBroadcastItem(i);
                listener.onNewLog(newLogPath);
            }
            mNewLogListeners.finishBroadcast();
        }

        @Override
        public void registerNewLogListener(INewLogListener newLogListener) throws RemoteException {
            mNewLogListeners.register(newLogListener);
        }

        @Override
        public void unregisterNewLogListener(INewLogListener newLogListener) throws RemoteException {
            mNewLogListeners.unregister(newLogListener);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mSamplerService;
    }
}
