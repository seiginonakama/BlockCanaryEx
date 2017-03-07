package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sHandler.post(new Runnable() {
            @Override
            public void run() {
                doHeavyWork();

                for (int i = 0; i < 100; i++) {
                    doLightWork();
                }
            }
        });
    }

    private void doHeavyWork() {
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doLightWork() {
        try {
            Thread.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
