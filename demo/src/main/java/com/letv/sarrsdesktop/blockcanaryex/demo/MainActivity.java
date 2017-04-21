package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.app.ActivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doHeavyWork();

        for (int i = 0; i < 50; i++) {
            doLightWork();
        }

        findViewById(R.id.gc_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doGc();
            }
        });
    }

    private void doGc() {
        long startTime = System.currentTimeMillis();
        int memoryClass = ((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
        long availableSize = memoryClass * 1024 * 1024;
        long allocSize = availableSize / 2 + 1;
        while (System.currentTimeMillis() - startTime < 100L) {
            byte[] tmp = new byte[(int) allocSize];
        }
    }

    private void doHeavyWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 100L) {
            random.nextInt(Integer.MAX_VALUE);
        }
    }

    private void doLightWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 1L) {
            random.nextInt(Integer.MAX_VALUE);
        }
    }
}
