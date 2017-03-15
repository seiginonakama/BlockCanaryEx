/*
 * Copyright (C) 2016 MarkZhai (http://zhaiyifan.cn).
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

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx;
import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.Config;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Log writer which runs in standalone thread.
 */
public class LogWriter {

    private static final String TAG = "LogWriter";

    private static final Object SAVE_DELETE_LOCK = new Object();
    private static final SimpleDateFormat FILE_NAME_FORMATTER
            = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.US);
    private static final long OBSOLETE_DURATION = 2 * 24 * 3600 * 1000L;

    private static final List<WeakReference<LogListener>> LOG_LISTENERS = new ArrayList<>();

    private static final BlockMonitor.BlockObserver BLOCK_OBSERVER = new BlockMonitor.BlockObserver() {
        @Override
        public void onBlock(BlockInfo blockInfo) {
            Config config = BlockCanaryEx.getConfig();
            if(config != null && config.enableSaveLog()) {
                save(blockInfo);
            }
        }
    };

    static {
        BlockMonitor.registerBlockObserver(BLOCK_OBSERVER);
    }

    public interface LogListener {
        /**
         * running on log writer thread
         *
         * @param log the log file
         */
        void onNewLog(File log);
    }

    public static void registerLogListener(LogListener logListener) {
        if(logListener != null) {
            LOG_LISTENERS.add(new WeakReference<>(logListener));
        }
    }

    private static void notifyNewLog(File logFile) {
        for(WeakReference<LogListener> listenerWeakRef : LOG_LISTENERS) {
            LogListener logListener = listenerWeakRef.get();
            if(logListener != null) {
                logListener.onNewLog(logFile);
            }
        }
    }

    private LogWriter() {
        throw new InstantiationError("Must not instantiate this class");
    }

    /**
     * Save log to file
     *
     * @param serializable block info
     */
    private static void save(final Serializable serializable) {
        WriteLogHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                save("looper", serializable);
            }
        });
    }

    /**
     * get log files
     *
     * @param count if less than 0, return all log files, if larger than 0, return files orderBy modified time
     * @return File[] log files
     */
    public static File[] getLogFiles(int count) {
        File f = detectedBlockDirectory();
        if (f.exists() && f.isDirectory()) {
            if(count <= 0) {
                return f.listFiles(new BlockLogFileFilter());
            } else {
                File[] array = f.listFiles(new BlockLogFileFilter());
                if(array == null) {
                    return null;
                }
                if(count > array.length) {
                    count = array.length;
                }
                if(count == array.length) {
                    return array;
                }
                File[] re = Arrays.copyOfRange(array, array.length - count, array.length);
                Arrays.sort(re,
                        new Comparator<File>() {
                            @Override
                            public int compare(File lhs, File rhs) {
                                long l = lhs.lastModified();
                                long r = rhs.lastModified();
                                return l < r ? -1 : (l == r ? 0 : 1);
                            }
                        });
                return re;
            }
        }
        return null;
    }

    /**
     * Delete obsolete log files, which is by default 2 days.
     */
    public static void cleanObsolete() {
        WriteLogHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                File[] f = getLogFiles(-1);
                if (f != null && f.length > 0) {
                    synchronized (SAVE_DELETE_LOCK) {
                        for (File aF : f) {
                            if (now - aF.lastModified() > OBSOLETE_DURATION) {
                                aF.delete();
                            }
                        }
                    }
                }
            }
        });
    }

    public static void deleteAll() {
        synchronized (SAVE_DELETE_LOCK) {
            try {
                File[] files = getLogFiles(-1);
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "deleteAll: ", e);
            }
        }
    }

    private static String save(String logFileName, Serializable serializable) {
        String path = "";
        BufferedWriter writer = null;
        try {
            File file = detectedBlockDirectory();
            long time = System.currentTimeMillis();
            path = file.getAbsolutePath() + "/"
                    + logFileName + "-"
                    + FILE_NAME_FORMATTER.format(time) + ".log";
            File target = new File(path);

            OutputStreamWriter out =
                    new OutputStreamWriter(new FileOutputStream(target, true), "UTF-8");
            writer = new BufferedWriter(out);
            writer.write(serializable.serialize());
            writer.flush();
            writer.close();

            notifyNewLog(target);
        } catch (Throwable t) {
            Log.e(TAG, "save: ", t);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "save: ", e);
            }
        }
        return path;
    }

    private static String getPath() {
        String state = Environment.getExternalStorageState();
        Config config = BlockCanaryEx.getConfig();
        String logPath = config
                == null ? "" : config.provideLogPath();

        if (Environment.MEDIA_MOUNTED.equals(state)
                && Environment.getExternalStorageDirectory().canWrite()) {
            return Environment.getExternalStorageDirectory().getPath() + logPath;
        }
        Context context = BlockCanaryEx.getConfig().getContext();
        return context.getFilesDir().getAbsolutePath() + logPath;
    }

    private static File detectedBlockDirectory() {
        File directory = new File(getPath());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private static class BlockLogFileFilter implements FilenameFilter {

        private String TYPE = ".log";

        BlockLogFileFilter() {

        }

        @Override
        public boolean accept(File dir, String filename) {
            return filename.endsWith(TYPE);
        }
    }
}