package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.app.ActivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

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

        final ViewBlockButton viewBlockButton = (ViewBlockButton) findViewById(R.id.view_block_button);
        final EditText measureBlockInput = (EditText) findViewById(R.id.measure_block_time);
        final EditText layoutBlockInput = (EditText) findViewById(R.id.layout_block_time);
        final EditText drawBlockInput = (EditText) findViewById(R.id.draw_block_time);
        viewBlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewBlockButton.invokeBlock(getViewBlockTime(measureBlockInput.getText().toString()),
                        getViewBlockTime(layoutBlockInput.getText().toString()),
                        getViewBlockTime(drawBlockInput.getText().toString()));
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

    private long getViewBlockTime(String input) {
        if(TextUtils.isEmpty(input)) {
            return 500L;
        }
        return Long.valueOf(input);
    }
}
