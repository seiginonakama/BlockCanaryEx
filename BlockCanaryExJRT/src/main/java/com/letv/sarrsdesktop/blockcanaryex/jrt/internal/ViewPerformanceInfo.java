package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

/**
 * author: zhoulei date: 2017/5/3.
 */
public class ViewPerformanceInfo {
    static final int TYPE_MEASURE = 0;
    static final int TYPE_LAYOUT = 1;
    static final int TYPE_DRAW = 2;

    private int mType;
    private long mStartTimeMs;
    private long mEndTimeMs;

    ViewPerformanceInfo(int type, long startTimeMs, long endTimeMs) {
        mType = type;
        mStartTimeMs = startTimeMs;
        mEndTimeMs = endTimeMs;
    }

    public int getType() {
        return mType;
    }

    public long getStartTimeMs() {
        return mStartTimeMs;
    }

    public long getEndTimeMs() {
        return mEndTimeMs;
    }

    public long getCostTimeMs() {
        return mEndTimeMs - mStartTimeMs;
    }

    @Override
    public String toString() {
        String type;
        switch (mType) {
            case TYPE_MEASURE:
                type = "measure";
                break;
            case TYPE_LAYOUT:
                type = "layout";
                break;
            case TYPE_DRAW:
                type = "draw";
                break;
            default:
                type = "unknown";
                break;
        }
        return type + ":" + getCostTimeMs() + "ms";
    }
}
