package com.dodopress.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class PulsingBeaconView extends View {

    private Paint pulsePaint;
    private Paint dotPaint;
    private ValueAnimator animator;
    private float pulseProgress = 0f;
    private int beaconColor = Color.parseColor("#10B981"); // Default Emerald

    public PulsingBeaconView(Context context) {
        super(context);
        init();
    }

    public PulsingBeaconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PulsingBeaconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.STROKE);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            pulseProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setBeaconColor(int color) {
        this.beaconColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxRadius = Math.min(cx, cy);
        float dotRadius = maxRadius * 0.38f;

        // Wave 1
        float r1 = dotRadius + (maxRadius - dotRadius) * pulseProgress;
        int alpha1 = (int) (180 * (1f - pulseProgress));
        pulsePaint.setColor(beaconColor);
        pulsePaint.setAlpha(alpha1);
        pulsePaint.setStrokeWidth(maxRadius * 0.12f);
        canvas.drawCircle(cx, cy, r1, pulsePaint);

        // Wave 2 (offset by 0.5)
        float p2 = (pulseProgress + 0.5f) % 1.0f;
        float r2 = dotRadius + (maxRadius - dotRadius) * p2;
        int alpha2 = (int) (180 * (1f - p2));
        pulsePaint.setAlpha(alpha2);
        canvas.drawCircle(cx, cy, r2, pulsePaint);

        // Core Solid Dot
        dotPaint.setColor(beaconColor);
        canvas.drawCircle(cx, cy, dotRadius, dotPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}
