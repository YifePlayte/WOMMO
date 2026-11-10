package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.wave;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Keep;

import java.util.function.Consumer;

// Adapted from Mi 10 China MIUI V13.0.8.0.SJBCNXM SystemUI's WaveChargeView.
// Kept as a reflective bridge for hooks reloaded while an older renderer is still attached.
@Keep
public final class WaveChargeView extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    private final View mCarrier;
    private final TimeInterpolator mQuartOutInterpolator;
    private final WaveView mWaveView;

    public WaveChargeView(Context context, View carrier, TimeInterpolator interpolator,
                          Point screenSize, Consumer<Throwable> onDitherError) {
        super(context);
        mCarrier = carrier;
        mQuartOutInterpolator = interpolator;
        mWaveView = new WaveView(context, onDitherError);
        mWaveView.setWaveViewWidth(screenSize.x);
        mWaveView.setWaveViewHeight(screenSize.y);
        setBackgroundColor(Color.BLACK);
        addView(mWaveView, new LayoutParams(-1, -1));
        setComponentTransparent(true);
    }

    public void setProgress(int progress) {
        mWaveView.setProgress(progress);
    }

    public void startAnimationOnChildView() {
        mWaveView.startAnim();
    }

    public AnimatorSet initAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 1);
        animator.setInterpolator(mQuartOutInterpolator);
        animator.setDuration(800L);
        animator.addUpdateListener((ValueAnimator.AnimatorUpdateListener) mCarrier);
        animator.addUpdateListener(this);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animator);
        return animatorSet;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        setScaleX(1.0f);
        setScaleY(1.0f);
        setAlpha(animator.getAnimatedFraction());
    }

    public void startDismiss(String reason, AnimatorSet animatorSet) {
        ObjectAnimator carrierAnimator = ObjectAnimator.ofPropertyValuesHolder(
                mCarrier, PropertyValuesHolder.ofFloat(View.ALPHA, mCarrier.getAlpha(), 0.0f)
        ).setDuration(600L);
        ObjectAnimator contentAnimator = ObjectAnimator.ofPropertyValuesHolder(
                this, PropertyValuesHolder.ofFloat(View.ALPHA, getAlpha(), 0.0f)
        ).setDuration(600L);
        animatorSet.setInterpolator(mQuartOutInterpolator);
        animatorSet.playTogether(contentAnimator);
        if (!"dismiss_for_timeout".equals(reason)) {
            animatorSet.play(carrierAnimator).with(contentAnimator);
        }
        animatorSet.start();
    }

    public void stopChildAnimation() {
        mWaveView.stopAnim();
    }

    public void setComponentTransparent(boolean transparent) {
        float alpha = transparent ? 0.0f : 1.0f;
        mCarrier.setAlpha(alpha);
        setAlpha(alpha);
    }
}
