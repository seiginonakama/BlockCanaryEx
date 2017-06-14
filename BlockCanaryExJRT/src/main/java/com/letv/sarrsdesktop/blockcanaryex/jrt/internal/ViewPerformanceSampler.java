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

import android.os.Build;
import android.os.Trace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * only called on main thread
 * <p>
 * author: zhoulei date: 2017/5/3.
 */
public class ViewPerformanceSampler {
    private static final long TRACE_TAG_VIEW = 1L << 3;

    private static boolean installed = false;

    private static final Stack<TracePoint> TRACE_POINTS = new Stack<>();
    private static final List<ViewPerformanceInfo> VIEW_PERFORMANCE_INFOS = new ArrayList<>();

    static void install() {
        if (!installed) {
            installed = true;
            if (isSupported()) {
                try {
                    Method traceBegin = Trace.class.getDeclaredMethod("traceBegin", long.class, String.class);
                    Method traceEnd = Trace.class.getDeclaredMethod("traceEnd", long.class);

                    Hook.hook(traceBegin, ViewPerformanceSampler.class.getDeclaredMethod("traceBegin", long.class, String.class));
                    Hook.hook(traceEnd, ViewPerformanceSampler.class.getDeclaredMethod("traceEnd", long.class));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 20 && Build.VERSION.SDK_INT < 26;
    }

    public static void traceBegin(long traceTag, String methodName) {
        if (traceTag == TRACE_TAG_VIEW) {
            TRACE_POINTS.push(TracePoint.obtain(methodName, System.currentTimeMillis()));
        }
    }

    public static void traceEnd(long traceTag) {
        if (traceTag == TRACE_TAG_VIEW) {
            if (!TRACE_POINTS.isEmpty()) {
                long endTime = System.currentTimeMillis();
                TracePoint tracePoint = TRACE_POINTS.pop();
                int type = ViewPerformanceInfo.TYPE_UNKNOWN;
                switch (tracePoint.mMethodName) {
                    case "input":
                        type = ViewPerformanceInfo.TYPE_INPUT;
                        break;
                    case "animation":
                        type = ViewPerformanceInfo.TYPE_ANIMATION;
                        break;
                    case "inflate":
                        type = ViewPerformanceInfo.TYPE_INFLATE;
                        break;
                    case "measure":
                        type = ViewPerformanceInfo.TYPE_MEASURE;
                        break;
                    case "layout":
                        type = ViewPerformanceInfo.TYPE_LAYOUT;
                        break;
                    case "draw":
                        type = ViewPerformanceInfo.TYPE_DRAW;
                        break;
                    case "commit":
                        type = ViewPerformanceInfo.TYPE_COMMIT;
                        break;
                }

                if (type != ViewPerformanceInfo.TYPE_UNKNOWN) {
                    ViewPerformanceInfo info = new ViewPerformanceInfo();
                    info.setType(type);
                    info.setStartTimeMs(tracePoint.mStartTime);
                    info.setEndTimeMs(endTime);
                    synchronized (VIEW_PERFORMANCE_INFOS) {
                        VIEW_PERFORMANCE_INFOS.add(info);
                    }
                }

                tracePoint.recycle();
            }
        }
    }

    static List<ViewPerformanceInfo> popPerformanceInfos() {
        List<ViewPerformanceInfo> infos;
        synchronized (VIEW_PERFORMANCE_INFOS) {
            infos = new ArrayList<>(VIEW_PERFORMANCE_INFOS.size());
            infos.addAll(VIEW_PERFORMANCE_INFOS);
        }
        clearPerformanceInfo();
        return infos;
    }

    static void clearPerformanceInfo() {
        synchronized (VIEW_PERFORMANCE_INFOS) {
            VIEW_PERFORMANCE_INFOS.clear();
        }
    }

    private static class TracePoint {
        private static final List<TracePoint> sPool = new LinkedList<>();

        String mMethodName;
        long mStartTime;

        static TracePoint obtain(String methodName, long startTime) {
            TracePoint tracePoint;
            synchronized (sPool) {
                if (sPool.isEmpty()) {
                    tracePoint = new TracePoint();
                } else {
                    tracePoint = sPool.remove(0);
                }
            }

            tracePoint.mMethodName = methodName;
            tracePoint.mStartTime = startTime;
            return tracePoint;
        }

        void recycle() {
            mMethodName = null;
            mStartTime = 0L;
            synchronized (sPool) {
                sPool.add(this);
            }
        }
    }
}
