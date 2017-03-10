package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
