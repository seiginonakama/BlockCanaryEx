package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * author: zhoulei date: 2017/4/21.
 */
public class GcInfo implements Parcelable {
    private long happenedTime;
    private String gcLog;

    public GcInfo() {

    }

    public GcInfo(Parcel in) {
        happenedTime = in.readLong();
        gcLog = in.readString();
    }

    public long getHappenedTime() {
        return happenedTime;
    }

    public void setHappenedTime(long happenedTime) {
        this.happenedTime = happenedTime;
    }

    public String getGcLog() {
        return gcLog;
    }

    public void setGcLog(String gcLog) {
        this.gcLog = gcLog;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(happenedTime);
        dest.writeString(gcLog);
    }


    public static final Creator<GcInfo> CREATOR = new Creator<GcInfo>() {
        @Override
        public GcInfo createFromParcel(Parcel in) {
            return new GcInfo(in);
        }

        @Override
        public GcInfo[] newArray(int size) {
            return new GcInfo[size];
        }
    };
}
