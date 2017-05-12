package com.letv.sarrsdesktop.blockcanaryex.demo.library;

import android.content.pm.IPackageDataObserver;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.Random;

/**
 * author: zhoulei date: 2017/5/12.
 */
public class TestUtil {
    public static void testProvide() {
        IPackageDataObserver packageDataObserver = new IPackageDataObserver() {
            @Override
            public void onRemoveCompleted(String s, boolean b) throws RemoteException {
                doHeavyWork();
                doHeavyWork();
            }

            @Override
            public IBinder asBinder() {
                return null;
            }
        };
        try {
            packageDataObserver.onRemoveCompleted("haha", false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        doHeavyWork();
    }

    private static void doHeavyWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 150L) {
            random.nextInt(Integer.MAX_VALUE);
        }
    }
}
