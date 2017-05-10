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

    private boolean mFirstStart = true;

    private final Runnable mNotifyActionNoBlock = new Runnable() {
        @Override
        public void run() {
            notifyNoBlock();
        }
    };

    String currentCreatingActivity = null;

    //running on SamplerReportThread
    interface BlockListener {
        void beforeFirstStart(long firstStartTime, long firstStartThreadTime);

        void onStart(long startTime);

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
        if(mFirstStart) {
            mFirstStart = false;
            final long currentTime = System.currentTimeMillis();
            final long currentThreadTime = SystemClock.currentThreadTimeMillis();
            SamplerReportHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    notifyBeforeFirstStart(currentTime, currentThreadTime);
                }
            });
        }

        if(!mPrintingStarted) {
            mPrintingStarted = true;
            currentCreatingActivity = null;
            mStartTimestamp = System.currentTimeMillis();
            mStartThreadTimestamp = SystemClock.currentThreadTimeMillis();
            final long startTime = mStartThreadTimestamp;
            SamplerReportHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    notifyStart(startTime);
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
            if(config.isBlock(startTime, endTime, startThreadTime, endThreadTime,
                    currentCreatingActivity, false)) {
                SamplerReportHandler.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        notifyBlockEvent(startTime, endTime, startThreadTime, endThreadTime);
                    }
                });
            } else {
                SamplerReportHandler.getInstance().post(mNotifyActionNoBlock);
            }
            currentCreatingActivity = null;
        }
    }

    private void notifyBeforeFirstStart(long currentTime, long currentThreadTime) {
        mBlockListener.beforeFirstStart(currentTime, currentThreadTime);
    }

    private void notifyStart(long startTime) {
        mBlockListener.onStart(startTime);
    }

    private void notifyBlockEvent(long startTime, long endTime, long startThreadTime, final long endThreadTime) {
        mBlockListener.onBlockEvent(startTime, endTime, startThreadTime, endThreadTime);
    }

    private void notifyNoBlock() {
        mBlockListener.onNoBlock();
    }
}