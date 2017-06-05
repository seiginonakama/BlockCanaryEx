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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * author: zhoulei date: 2017/3/15.
 */
class ConnectServiceFuture implements Future<ISamplerService>, ServiceConnection {
    private boolean mResultReceived = false;
    private ISamplerService mResult;
    private boolean enterWaitting;

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public ISamplerService get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public ISamplerService get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(TimeUnit.MILLISECONDS.convert(timeout, unit));
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        mResultReceived = true;
        mResult = ISamplerService.Stub.asInterface(service);
        notifyAll();
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        mResultReceived = true;
        mResult = null;
        if (enterWaitting) {
            notifyAll();
        }
    }

    private synchronized ISamplerService doGet(Long timeoutMs)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (mResultReceived) {
            return mResult;
        }

        enterWaitting = true;
        if (timeoutMs == null) {
            wait(0);
        } else if (timeoutMs > 0) {
            wait(timeoutMs);
        }
        enterWaitting = false;

        if (!mResultReceived) {
            throw new TimeoutException();
        }

        return mResult;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return mResultReceived || isCancelled();
    }

}