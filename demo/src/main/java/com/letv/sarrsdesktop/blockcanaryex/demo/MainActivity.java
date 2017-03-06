package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doHeavyWork();

        int i = 100;
        while (i-- > 0) {
            doLightWork();
        }
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
