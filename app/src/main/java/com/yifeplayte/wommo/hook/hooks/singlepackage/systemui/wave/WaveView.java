package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.wave;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

// Adapted from Mi 10 China MIUI V13.0.8.0.SJBCNXM SystemUI's WaveView.
public final class WaveView extends View {
    private static final String stringWaveDitherShader =
            "uniform shader originalGradient;\n" +
            "uniform float ditherAmplitude;\n" +
            "half4 main(float2 coord) {\n" +
            "    half4 color = originalGradient.eval(coord);\n" +
            "    float2 pixel = floor(coord);\n" +
            "    float noise = fract(52.9829189 * fract(dot(pixel, float2(0.06711056, 0.00583715)))) - 0.5;\n" +
            "    float3 rgb = float3(color.rgb) + noise * ditherAmplitude * float(color.a);\n" +
            "    return half4(clamp(rgb, 0.0, float(color.a)), color.a);\n" +
            "}";

    private int mBubbleMaxHeight = 1380;
    private final int mBubbleMaxSize = 15;
    private final Paint mBubblePaint = new Paint();
    private final ArrayList<Bubble> mBubbles = new ArrayList<>();
    private final float mDamp = 0.95f;
    private final int mExtraHeightBetweenWaves = 10;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            if (message.what == 10001) {
                tryCreateBubble();
            }
        }
    };
    private final int mMinMargin;
    private int mProgress = 45;
    private final Random mRandom = new Random();
    private final int mWaterSpeed = -5;
    private int mWave1Dx;
    private int mWave2Dx;
    private AnimatorSet mWaveAnimatorSet;
    private final int[] mWaveEndColor = {
            Color.parseColor("#f1691e"), Color.parseColor("#1aabff"), Color.parseColor("#1ef1b8")
    };
    private final int mWaveHeight1 = 28;
    private final int mWaveHeight2 = 28;
    private final int mWaveLength1 = 1080;
    private final int mWaveLength2 = 1400;
    private final Paint mWavePaint = new Paint();
    private RuntimeShader mWaveDitherShader;
    private final Consumer<Throwable> mDitherErrorHandler;
    private final Path mWavePath1 = new Path();
    private final Path mWavePath2 = new Path();
    private final int[] mWaveStartColor = {
            Color.parseColor("#c33021"), Color.parseColor("#4c11e1"), Color.parseColor("#0e8f20")
    };
    private int mWaveViewHeight = 2250;
    private int mWaveViewWidth = 2048;
    private int mWaveY;
    private float mWaveYPercent = -0.1f;

    public WaveView(Context context, Consumer<Throwable> onDitherError) {
        super(context);
        mDitherErrorHandler = onDitherError;
        mWavePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWavePaint.setAlpha(100);
        mWavePaint.setDither(true);
        try {
            mWaveDitherShader = new RuntimeShader(stringWaveDitherShader);
            // Compensate for the original paint alpha; noise stays within one 8-bit output step.
            mWaveDitherShader.setFloatUniform("ditherAmplitude", 1.0f / mWavePaint.getAlpha());
        } catch (RuntimeException error) {
            mWaveDitherShader = null;
            mDitherErrorHandler.accept(error);
        }
        mBubblePaint.setColor(Color.WHITE);
        mMinMargin = (Math.max(mWaveHeight1, mWaveHeight2) / 2) + mExtraHeightBetweenWaves;
        updateWaveHeight();
        setWaveColor(mProgress);
    }

    public void setWaveViewWidth(int width) {
        if (width > 0) {
            mWaveViewWidth = width;
        }
    }

    public void setWaveViewHeight(int height) {
        if (height > 0) {
            mWaveViewHeight = height;
            updateWaveHeight();
            setWaveColor(mProgress);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        setWaveViewWidth(width);
        setWaveViewHeight(height);
    }

    private void updateWaveHeight() {
        mWaveY = (int) ((1.0d - (mProgress / 100.0d)) * (mWaveViewHeight - (mMinMargin * 2)))
                + mMinMargin;
        mBubbleMaxHeight = Math.max(1380, mWaveY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int waveY;
        if (mProgress < 100) {
            waveY = (int) ((1.0d - ((double) (mProgress * mWaveYPercent) / 100.0d))
                    * (mWaveViewHeight - (mMinMargin * 2))) + mMinMargin;
        } else {
            waveY = (int) ((1.0d - ((double) (mProgress * mWaveYPercent) / 100.0d))
                    * mWaveViewHeight) - mMinMargin;
        }

        mWavePath1.reset();
        int halfWaveLength1 = mWaveLength1 / 2;
        mWavePath1.moveTo(-mWaveLength1 + mWave1Dx, waveY);
        for (int x = -mWaveLength1; x <= mWaveLength1 + mWaveViewWidth; x += mWaveLength1) {
            mWavePath1.rQuadTo(halfWaveLength1 / 2, -mWaveHeight1, halfWaveLength1, 0.0f);
            mWavePath1.rQuadTo(halfWaveLength1 / 2, mWaveHeight1, halfWaveLength1, 0.0f);
        }
        mWavePath1.lineTo(mWaveViewWidth, mWaveViewHeight);
        mWavePath1.lineTo(0.0f, mWaveViewHeight);
        mWavePath1.close();
        canvas.drawPath(mWavePath1, mWavePaint);

        mWavePath2.reset();
        int halfWaveLength2 = mWaveLength2 / 2;
        mWavePath2.moveTo(-mWave2Dx, waveY + mExtraHeightBetweenWaves);
        for (int x = -mWaveLength2; x <= mWaveLength2 + mWaveViewWidth; x += mWaveLength2) {
            mWavePath2.rQuadTo(halfWaveLength2 / 2, -mWaveHeight2, halfWaveLength2, 0.0f);
            mWavePath2.rQuadTo(halfWaveLength2 / 2, mWaveHeight2, halfWaveLength2, 0.0f);
        }
        mWavePath2.lineTo(mWaveViewWidth, mWaveViewHeight);
        mWavePath2.lineTo(0.0f, mWaveViewHeight);
        mWavePath2.close();
        canvas.drawPath(mWavePath2, mWavePaint);
        drawBubble(canvas);
    }

    public void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            return;
        }
        mProgress = progress;
        setWaveColor(progress);
        updateWaveHeight();
        invalidate();
    }

    public void setWaveColor(int progress) {
        int color = progress >= 20 && progress < 60 ? 1 : progress >= 60 && progress <= 100 ? 2 : 0;
        Shader shaderGradient = new LinearGradient(
                0.0f, 0.0f, 0.0f, mWaveViewHeight,
                mWaveStartColor[color], mWaveEndColor[color], Shader.TileMode.CLAMP
        );
        if (mWaveDitherShader != null) {
            try {
                mWaveDitherShader.setInputShader("originalGradient", shaderGradient);
                mWavePaint.setShader(mWaveDitherShader);
                return;
            } catch (RuntimeException error) {
                mWaveDitherShader = null;
                mDitherErrorHandler.accept(error);
            }
        }
        mWavePaint.setShader(shaderGradient);
    }

    public void startAnim() {
        stopAnim();
        ValueAnimator waterAnimator = ValueAnimator.ofFloat(-0.1f, 1.0f);
        waterAnimator.setDuration(2000L);
        waterAnimator.setInterpolator(input -> input == 1.0f ? 1.0f
                : (float) ((-Math.pow(2.0d, input * (-10.0f))) + 1.0d));
        waterAnimator.addUpdateListener(animator -> {
            mWaveYPercent = (Float) animator.getAnimatedValue();
            postInvalidate();
        });

        ValueAnimator waveAnimator1 = ValueAnimator.ofInt(0, mWaveLength1);
        waveAnimator1.setDuration(1600L);
        waveAnimator1.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator1.setInterpolator(new LinearInterpolator());
        waveAnimator1.addUpdateListener(animator -> {
            int offset = (Integer) animator.getAnimatedValue();
            if (Math.abs(offset - mWave1Dx) > 1) {
                mWave1Dx = offset;
                postInvalidate();
            }
        });

        ValueAnimator waveAnimator2 = ValueAnimator.ofInt(0, mWaveLength2);
        waveAnimator2.setDuration(2400L);
        waveAnimator2.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator2.setInterpolator(new LinearInterpolator());
        waveAnimator2.addUpdateListener(animator -> mWave2Dx = (Integer) animator.getAnimatedValue());

        mWaveAnimatorSet = new AnimatorSet();
        mWaveAnimatorSet.setStartDelay(400L);
        mWaveAnimatorSet.play(waveAnimator1).with(waveAnimator2).with(waterAnimator);
        mWaveAnimatorSet.start();
        mHandler.sendEmptyMessageDelayed(10001, 1000L);
    }

    private void resetState() {
        mWaveYPercent = -0.1f;
        mWave1Dx = 0;
        mWave2Dx = 0;
        mBubbles.clear();
    }

    public void stopAnim() {
        mHandler.removeMessages(10001);
        if (mWaveAnimatorSet != null) {
            mWaveAnimatorSet.cancel();
            mWaveAnimatorSet = null;
        }
        resetState();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnim();
        super.onDetachedFromWindow();
    }

    private static final class Bubble {
        float Vx;
        float Vy;
        float alpha;
        float angle;
        float initAlpha;
        int radius;
        float scale;
        float sinRandom;
        int x;
        int y;
    }

    private void drawBubble(Canvas canvas) {
        refreshBubbles();
        for (Bubble bubble : mBubbles) {
            if (bubble != null) {
                mBubblePaint.setAlpha((int) (bubble.alpha * 255.0f));
                canvas.drawCircle(bubble.x, bubble.y, (int) (bubble.radius * bubble.scale), mBubblePaint);
            }
        }
    }

    private void tryCreateBubble() {
        if (mWaveAnimatorSet == null || !mWaveAnimatorSet.isStarted()) {
            return;
        }
        mHandler.sendEmptyMessageDelayed(10001,
                (long) (1000.0f / ((mRandom.nextFloat() * 4.0f) + 2.0f)));
        if (mBubbles.size() >= mBubbleMaxSize) {
            return;
        }
        Bubble bubble = new Bubble();
        bubble.radius = (int) ((mRandom.nextFloat() * 7.0f) + 7.0f);
        bubble.Vx = (mRandom.nextFloat() - 0.5f) * 12.0f;
        bubble.Vy = (-mRandom.nextFloat()) * 6.0f;
        bubble.x = mWaveViewWidth / 2;
        bubble.y = mWaveViewHeight;
        bubble.scale = (mRandom.nextFloat() * 0.4f) + 0.4f;
        bubble.initAlpha = (mRandom.nextFloat() * 0.7f) + 0.3f;
        bubble.alpha = bubble.initAlpha;
        bubble.angle = (float) Math.random() * 360.0f;
        bubble.sinRandom = (float) ((Math.random() * 2.0d) + 3.0d);
        mBubbles.add(bubble);
    }

    private void refreshBubbles() {
        Iterator<Bubble> iterator = mBubbles.iterator();
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            if (bubble.y + bubble.Vy <= mBubbleMaxHeight + 30) {
                iterator.remove();
            } else {
                int index = mBubbles.indexOf(bubble);
                double sin = Math.sin(((double) (bubble.angle
                        + ((bubble.y * bubble.sinRandom) / 2.5f))) * Math.PI / 180.0d);
                int y = bubble.y;
                float scale = bubble.scale;
                bubble.x = (int) (bubble.x + bubble.Vx
                        + (int) (((sin * ((y - 300) - mWaveViewHeight)) / 150.0d) * scale));
                bubble.y = (int) (y + bubble.Vy + (mWaterSpeed * (scale + 1.0f)));
                bubble.scale = Math.min(scale + ((mWaveViewHeight - bubble.y) / 1000.0f), 1.0f);
                bubble.Vx *= mDamp;
                bubble.Vy *= mDamp;
                bubble.alpha = Math.min(((bubble.y - mBubbleMaxHeight) - 20.0f) / 200.0f,
                        bubble.scale) * bubble.initAlpha;
                mBubbles.set(index, bubble);
            }
        }
    }
}
