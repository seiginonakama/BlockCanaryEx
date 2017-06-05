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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * no thread safe
 *
 * author: zhoulei date: 2017/5/31.
 */
class ArrayPool<T> {
    private Object[] mPool;
    private int mLastIndex = -1;
    private final Constructor<T> mConstructor;
    private final int mMaxBuffer;

    ArrayPool(int maxBuffer, Class<T> cls) {
        mMaxBuffer = maxBuffer;
        mPool = new Object[maxBuffer];
        try {
            mConstructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("ObjArrayPool init failed!", e);
        }
    }

    public T obtain() {
        mLastIndex++;

        if (mLastIndex > mPool.length - 1) {
            Object[] tmp = new Object[mPool.length * 2];
            System.arraycopy(mPool, 0, tmp, 0, mPool.length);
            mPool = tmp;
        }

        T t = (T) mPool[mLastIndex];
        if (t == null) {
            try {
                t = mConstructor.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("construct item failed!", e);
            }
            mPool[mLastIndex] = t;
        }
        return t;
    }

    public List<T> getAllUsed() {
        List<T> usedList;
        if (mLastIndex < 0) {
            usedList = Collections.EMPTY_LIST;
        } else {
            usedList = new ArrayList<T>(mLastIndex + 1);
            for (int i = 0; i <= mLastIndex; i++) {
                usedList.add((T) mPool[i]);
            }
        }
        return usedList;
    }

    public void reset() {
        mLastIndex = -1;
        if(mPool.length > mMaxBuffer) {
            Object[] tmp = new Object[mMaxBuffer];
            System.arraycopy(mPool, 0, tmp, 0, mMaxBuffer);
            mPool = tmp;
        }
    }
}
