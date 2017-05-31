package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

/**
 * author: zhoulei date: 2017/5/3.
 */
public class ViewPerformanceInfo {
    static final int TYPE_UNKNOWN = -1;

    static final int TYPE_INPUT = 0;
    static final int TYPE_ANIMATION = 1;
    static final int TYPE_INFLATE = 2;
    static final int TYPE_MEASURE = 3;
    static final int TYPE_LAYOUT = 4;
    static final int TYPE_DRAW = 5;
    static final int TYPE_COMMIT = 6;

    private int mType;
    private long mStartTimeMs;
    private long mEndTimeMs;

    ViewPerformanceInfo() {

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

    public void setType(int type) {
        mType = type;
    }

    public void setStartTimeMs(long startTimeMs) {
        mStartTimeMs = startTimeMs;
    }

    public void setEndTimeMs(long endTimeMs) {
        mEndTimeMs = endTimeMs;
    }

    @Override
    public String toString() {
        String type;
        switch (mType) {
            case TYPE_INPUT:
                type = "input";
                break;
            case TYPE_ANIMATION:
                type = "animation";
                break;
            case TYPE_INFLATE:
                type = "inflate";
                break;
            case TYPE_MEASURE:
                type = "measure";
                break;
            case TYPE_LAYOUT:
                type = "layout";
                break;
            case TYPE_DRAW:
                type = "draw";
                break;
            case TYPE_COMMIT:
                type = "commit";
                break;
            default:
                type = "unknown";
                break;
        }
        return type + ":" + getCostTimeMs() + "ms";
    }
}
