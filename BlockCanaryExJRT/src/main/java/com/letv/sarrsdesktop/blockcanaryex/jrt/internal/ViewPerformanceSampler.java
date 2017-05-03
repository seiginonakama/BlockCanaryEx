package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import android.os.Build;
import android.os.Trace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * author: zhoulei date: 2017/5/3.
 */
class ViewPerformanceSampler {
    private static final long TRACE_TAG_VIEW = 1L << 3;

    private static boolean installed = false;

    private static final Stack<TracePoint> TRACE_POINTS = new Stack<>();
    private static final List<ViewPerformanceInfo> VIEW_PERFORMANCE_INFOS = new LinkedList<>();

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
                }
                if (info != null) {
                    synchronized (VIEW_PERFORMANCE_INFOS) {
                        VIEW_PERFORMANCE_INFOS.add(info);
                    }
                }
            }
        }
    }

    static List<ViewPerformanceInfo> popPerformanceInfoBetween(long startTimeMs, long endTimeMs) {
        List<ViewPerformanceInfo> result = new ArrayList<>();
        synchronized (VIEW_PERFORMANCE_INFOS) {
            for (ViewPerformanceInfo info : VIEW_PERFORMANCE_INFOS) {
                if (info.getStartTimeMs() >= startTimeMs && info.getEndTimeMs() <= endTimeMs) {
                    result.add(info);
                }
            }
            VIEW_PERFORMANCE_INFOS.clear();
        }
        return result;
    }

    static void clearPerformanceInfoBefore(long time) {
        synchronized (VIEW_PERFORMANCE_INFOS) {
            if (!VIEW_PERFORMANCE_INFOS.isEmpty()) {
                Iterator<ViewPerformanceInfo> iterator = VIEW_PERFORMANCE_INFOS.iterator();
                while (iterator.hasNext()) {
                    ViewPerformanceInfo viewPerformanceInfo = iterator.next();
                    if (viewPerformanceInfo.getStartTimeMs() < time) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
        }
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
