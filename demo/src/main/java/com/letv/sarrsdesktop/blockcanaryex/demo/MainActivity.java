package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("test", Context.MODE_PRIVATE);
        mSharedPreferences.edit().clear();

        doHeavyWork();

        for (int i = 0; i < 50; i++) {
            doLightWork();
        }
    }

    private void doHeavyWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 100L) {
            mSharedPreferences.edit().putString("test" + random.nextInt(Integer.MAX_VALUE), String.valueOf(hashCode())).commit();
        }
    }

    private void doLightWork() {
        long startTime = System.currentTimeMillis();
        Random random = new Random();
        while (System.currentTimeMillis() - startTime < 2L) {
            mSharedPreferences.edit().putString("test" + random.nextInt(Integer.MAX_VALUE), String.valueOf(hashCode())).commit();
        }
    }
}
