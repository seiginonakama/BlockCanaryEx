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

import android.os.Build;
import android.os.Trace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * only called on main thread
 *
 * author: zhoulei date: 2017/5/3.
 */
class ViewPerformanceSampler {
    private static final long TRACE_TAG_VIEW = 1L << 3;

    private static boolean installed = false;

    private static final Stack<TracePoint> TRACE_POINTS = new Stack<>();
    private static final List<ViewPerformanceInfo> VIEW_PERFORMANCE_INFOS = new ArrayList<>();

    static void install() {
        if (!installed) {
            installed = true;
            if (Build.VERSION.SDK_INT >= 20) {
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

    public static void traceBegin(long traceTag, String methodName) {
        if (traceTag == TRACE_TAG_VIEW) {
            TRACE_POINTS.push(TracePoint.makePoint(methodName));
        }
    }

    public static void traceEnd(long traceTag) {
        if (traceTag == TRACE_TAG_VIEW) {
            if (!TRACE_POINTS.isEmpty()) {
                long endTime = System.currentTimeMillis();
                TracePoint tracePoint = TRACE_POINTS.pop();
                ViewPerformanceInfo info = null;
                switch (tracePoint.mMethodName) {
                    case "input":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_INPUT,
                                tracePoint.mStartTime, endTime);
                        break;
                    case "animation":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_ANIMATION,
                                tracePoint.mStartTime, endTime);
                        break;
                    case "measure":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_MEASURE,
                                tracePoint.mStartTime, endTime);
                        break;
                    case "layout":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_LAYOUT,
                                tracePoint.mStartTime, endTime);
                        break;
                    case "draw":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_DRAW,
                                tracePoint.mStartTime, endTime);
                        break;
                    case "commit":
                        info = new ViewPerformanceInfo(ViewPerformanceInfo.TYPE_COMMIT,
                                tracePoint.mStartTime, endTime);
                        break;
                }
                if (info != null) {
                    synchronized (VIEW_PERFORMANCE_INFOS) {
                        VIEW_PERFORMANCE_INFOS.add(info);
                    }
                }
            }
        }
    }

    static List<ViewPerformanceInfo> popPerformanceInfos() {
        List<ViewPerformanceInfo> infos = new ArrayList<>(VIEW_PERFORMANCE_INFOS.size());
        infos.addAll(VIEW_PERFORMANCE_INFOS);
        clearPerformanceInfo();
        return infos;
    }

    static void clearPerformanceInfo() {
        VIEW_PERFORMANCE_INFOS.clear();
    }

    private static class TracePoint {
        private String mMethodName;
        private long mStartTime;

        static TracePoint makePoint(String methodName) {
            long currentTime = System.currentTimeMillis();
            TracePoint tracePoint = new TracePoint();
            tracePoint.mMethodName = methodName;
            tracePoint.mStartTime = currentTime;
            return tracePoint;
        }
    }
}
