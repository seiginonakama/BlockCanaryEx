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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * author: zhoulei date: 2017/3/15.
 */
public class BlockSamplerService extends Service {
    private static final byte[] CPU_SAMPLER_LOCK = new byte[0];

    private ISamplerService.Stub mSamplerService = new ISamplerService.Stub() {
        @Override
        public void resetCpuSampler(int pid) throws RemoteException {
            synchronized (CPU_SAMPLER_LOCK) {
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
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mSamplerService;
    }
}
