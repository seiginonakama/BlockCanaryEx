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
package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.os.SystemClock;
import android.util.Printer;

class LooperMonitor implements Printer {
    private long mStartTimestamp = 0;
    private long mStartThreadTimestamp = 0;
    private BlockListener mBlockListener = null;
    private boolean mPrintingStarted = false;

    //running on SamplerReportThread
    interface BlockListener {
        void onStart();

        void onBlockEvent(long realStartTime,
                          long realTimeEnd,
                          long threadTimeStart,
                          long threadTimeEnd);

        void onNoBlock();
    }

    LooperMonitor(BlockListener blockListener) {
        if (blockListener == null) {
            throw new IllegalArgumentException("blockListener should not be null.");
        }
        this.mBlockListener = blockListener;
    }

    long getStartTimestamp() {
        return mStartTimestamp;
    }

    long getStartThreadTimestamp() {
        return mStartThreadTimestamp;
    }

    @Override
    public void println(String x) {
        if(!mPrintingStarted) {
            mPrintingStarted = true;
            mStartTimestamp = System.currentTimeMillis();
            mStartThreadTimestamp = SystemClock.currentThreadTimeMillis();
            SamplerReportHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    notifyStart();
                }
            });
        } else {
            mPrintingStarted = false;
            final Config config = BlockCanaryEx.getConfig();
            if(config == null) {
                return;
            }
            final long startTime = mStartTimestamp;
            final long endTime = System.currentTimeMillis();
            final long startThreadTime = mStartThreadTimestamp;
            final long endThreadTime = SystemClock.currentThreadTimeMillis();
            SamplerReportHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    if (config.isBlock(startTime, endTime, startThreadTime, endThreadTime)) {
                        notifyBlockEvent(startTime, endTime, startThreadTime, endThreadTime);
                    } else {
                        notifyNoBlock();
                    }
                }
            });
        }
    }

    private void notifyStart() {
        mBlockListener.onStart();
    }

    private void notifyBlockEvent(long startTime, long endTime, long startThreadTime, final long endThreadTime) {
        mBlockListener.onBlockEvent(startTime, endTime, startThreadTime, endThreadTime);
    }

    private void notifyNoBlock() {
        mBlockListener.onNoBlock();
    }
}