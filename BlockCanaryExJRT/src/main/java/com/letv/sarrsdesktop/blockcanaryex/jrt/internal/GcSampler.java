package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * author: zhoulei date: 2017/4/21.
 */
class GcSampler {
    private static final List<GcInfo> GC_INFO_LIST = new LinkedList<>();
    private static GcSamplerThread sSamplerThread;

    static void startIfNot(int pid) {
        if (sSamplerThread == null) {
            synchronized (GcSampler.class) {
                if (sSamplerThread == null) {
                    sSamplerThread = new GcSamplerThread(pid);
                    sSamplerThread.start();
                }
            }
        }
    }

    static void clearGcInfoBefore(long time) {
        synchronized (GC_INFO_LIST) {
            if(!GC_INFO_LIST.isEmpty()) {
                Iterator<GcInfo> iterator = GC_INFO_LIST.iterator();
                while (iterator.hasNext()) {
                    GcInfo gcInfo = iterator.next();
                    if (gcInfo.getHappenedTime() < time) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
        }
    }

    static List<GcInfo> popGcInfoBetween(long startTime, long endTime) {
        synchronized (GC_INFO_LIST) {
            if (GC_INFO_LIST.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<GcInfo> result = new ArrayList<>();
            for (GcInfo gcInfo : GC_INFO_LIST) {
                long happenedTime = gcInfo.getHappenedTime();
                if (happenedTime > startTime && happenedTime < endTime) {
                    result.add(gcInfo);
                }
            }
            GC_INFO_LIST.clear();
            return result;
        }
    }

    private static class GcSamplerThread extends Thread {
        private static final String CLEAR_LOGCAT_CMD = "logcat -c";
        private static final String UNFORMAT_LOGCAT_CMD = "logcat -v time %s:D *:S | grep '%d)'";
        private final String logcatCmd;
        private final boolean isArt;

        public GcSamplerThread(int pid) {
            super("GcSamplerThread");
            isArt = isArt();
            logcatCmd = String.format(Locale.getDefault(), UNFORMAT_LOGCAT_CMD, isArt ? "art" : "dalvikvm-heap:D dalvikvm", pid);
        }

        @Override
        public void run() {
            Runtime runtime = Runtime.getRuntime();
            Process process;
            InputStream inputStream;
            BufferedReader bufferedReader = null;
            for (;;){
                try {
                    runtime.exec(CLEAR_LOGCAT_CMD).waitFor();
                    process = runtime.exec(logcatCmd);
                    inputStream = process.getInputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String gcLog;
                    while ((gcLog = bufferedReader.readLine()) != null) {
                        if (!gcLog.contains("GC")) {
                            if (isArt) {
                                //most of useful art GC log contains 'GC', except '"art : Suspending all threads took: 12.627ms' "
                                if (!gcLog.contains("Suspending")) {
                                    continue;
                                }
                            } else if (!gcLog.contains("dalvikvm-heap")) {
                                //most of useful dalvikvm GC log contains 'GC', except 'Grow heap (frag case) to 130.931MB for 134217741-byte allocation"
                                continue;
                            }
                        }
                        recordGcInfo(System.currentTimeMillis(), gcLog);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static void recordGcInfo(long time, String gcLog) {
        synchronized (GC_INFO_LIST) {
            int size = GC_INFO_LIST.size();
            if(size >= 1) {
                //some system will print same gc log twice time!
                if(GC_INFO_LIST.get(size - 1).getGcLog().equals(gcLog)) {
                    return;
                }
            }
            GcInfo gcInfo = new GcInfo();
            gcInfo.setGcLog(gcLog);
            gcInfo.setHappenedTime(time);
            GC_INFO_LIST.add(gcInfo);
        }
    }

    private static boolean isArt() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return true;
        } else {
            final String vmVersion = System.getProperty("java.vm.version");
            return vmVersion != null && vmVersion.startsWith("2");
        }
    }
}
