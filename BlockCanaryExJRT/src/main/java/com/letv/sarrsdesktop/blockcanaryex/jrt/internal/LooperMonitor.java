package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.os.SystemClock;
import android.util.Printer;

class LooperMonitor implements Printer {
    private long mStartTimestamp = 0;
    private long mStartThreadTimestamp = 0;
    private BlockListener mBlockListener = null;

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
        if(x.startsWith(">")) {
            mStartTimestamp = System.currentTimeMillis();
            mStartThreadTimestamp = SystemClock.currentThreadTimeMillis();
            SamplerReportHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    notifyStart();
                }
            });
        } else {
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
        SamplerReportHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                mBlockListener.onStart();
            }
        });
    }

    private void notifyBlockEvent(long startTime, long endTime, long startThreadTime, final long endThreadTime) {
        mBlockListener.onBlockEvent(startTime, endTime, startThreadTime, endThreadTime);
    }

    private void notifyNoBlock() {
        mBlockListener.onNoBlock();
    }
}