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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * author: zhoulei date: 2017/3/15.
 */
class CpuInfo implements Parcelable {
    String cpuRate;
    boolean isBusy;

    CpuInfo() {

    }

    private CpuInfo(Parcel in) {
        cpuRate = in.readString();
        isBusy = in.readByte() != 0;
    }

    public static final Creator<CpuInfo> CREATOR = new Creator<CpuInfo>() {
        @Override
        public CpuInfo createFromParcel(Parcel in) {
            return new CpuInfo(in);
        }

        @Override
        public CpuInfo[] newArray(int size) {
            return new CpuInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cpuRate);
        dest.writeByte((byte) (isBusy ? 1 : 0));
    }
}
