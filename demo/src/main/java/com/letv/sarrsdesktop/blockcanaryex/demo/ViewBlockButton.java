package com.letv.sarrsdesktop.blockcanaryex.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import java.util.Random;

/**
 * author: zhoulei date: 2017/5/3.
 */
public class ViewBlockButton extends android.support.v7.widget.AppCompatButton {
    private boolean invokeBlock = false;

    private long measureBlockTime = 0;
    private long layoutBlockTime = 0;
    private long drawBlockTime = 0;

    public ViewBlockButton(Context context) {
        super(context);
    }

    public ViewBlockButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final Random mRandom = new Random();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(invokeBlock) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < measureBlockTime) {
                mRandom.nextInt(Integer.MAX_VALUE);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(invokeBlock) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < layoutBlockTime) {
                mRandom.nextInt(Integer.MAX_VALUE);
            }
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(invokeBlock) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < drawBlockTime) {
                mRandom.nextInt(Integer.MAX_VALUE);
            }
            invokeBlock = false;
        }
        super.onDraw(canvas);
    }

    public void invokeBlock(long measureTime, long layoutTime, long drawTime) {
        invokeBlock = true;
        measureBlockTime = measureTime;
        layoutBlockTime = layoutTime;
        drawBlockTime = drawTime;
        requestLayout();
    }
}
