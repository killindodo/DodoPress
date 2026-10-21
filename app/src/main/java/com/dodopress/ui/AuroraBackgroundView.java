package com.dodopress.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * High-performance, ambient Aurora Background for the Aether Aurora theme.
 * Uses 2-3 slowly drifting, low-opacity soft radial gradient fields to create
 * a calming, atmospheric midnight aurora without consuming CPU or battery.
 */
public class AuroraBackgroundView extends View {

    private final Paint basePaint = new Paint();
    private final Paint auroraPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint auroraPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint auroraPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator driftAnimator;
    private float phase = 0f;
    private boolean isAnimating = false;

    // Palette tokens
    private static final int COLOR_MIDNIGHT = Color.parseColor("#0C0D1A");
    private static final int COLOR_VIOLET = Color.parseColor("#9A7CFF");
    private static final int COLOR_TEAL = Color.parseColor("#43DCC7");
    private static final int COLOR_DEEP_BLUE = Color.parseColor("#2E1065");

    public AuroraBackgroundView(Context context) {
        super(context);
        init();
    }

    public AuroraBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        basePaint.setColor(COLOR_MIDNIGHT);
        basePaint.setStyle(Paint.Style.FILL);

        driftAnimator = ValueAnimator.ofFloat(0f, 1f);
        driftAnimator.setDuration(22000); // 22 seconds for a peaceful, slow drift
        driftAnimator.setRepeatCount(ValueAnimator.INFINITE);
        driftAnimator.setRepeatMode(ValueAnimator.RESTART);
        driftAnimator.setInterpolator(new LinearInterpolator());
        driftAnimator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setActive(boolean active) {
        if (active) {
            setVisibility(View.VISIBLE);
            startAnimation();
        } else {
            stopAnimation();
            setVisibility(View.GONE);
        }
    }

    public void startAnimation() {
        if (driftAnimator != null && !driftAnimator.isStarted()) {
            driftAnimator.start();
            isAnimating = true;
        } else if (driftAnimator != null && driftAnimator.isPaused()) {
            driftAnimator.resume();
            isAnimating = true;
        }
    }

    public void pauseAnimation() {
        if (driftAnimator != null && driftAnimator.isRunning()) {
            driftAnimator.pause();
            isAnimating = false;
        }
    }

    public void resumeAnimation() {
        if (getVisibility() == View.VISIBLE && driftAnimator != null) {
            if (driftAnimator.isPaused()) {
                driftAnimator.resume();
                isAnimating = true;
            } else if (!driftAnimator.isStarted()) {
                driftAnimator.start();
                isAnimating = true;
            }
        }
    }

    public void stopAnimation() {
        if (driftAnimator != null) {
            driftAnimator.cancel();
            isAnimating = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Solid midnight base
        canvas.drawRect(0, 0, w, h, basePaint);

        double angle = phase * 2.0 * Math.PI;

        // 2. Violet aurora node (drifting around top 35% of screen)
        float cx1 = (float) (w * 0.40 + Math.sin(angle) * (w * 0.25));
        float cy1 = (float) (h * 0.30 + Math.cos(angle * 0.8) * (h * 0.15));
        float radius1 = Math.max(w, h) * 0.65f;
        RadialGradient g1 = new RadialGradient(
            cx1, cy1, radius1,
            new int[] { Color.argb(45, 154, 124, 255), Color.argb(18, 90, 40, 180), Color.TRANSPARENT },
            new float[] { 0.0f, 0.55f, 1.0f },
            Shader.TileMode.CLAMP
        );
        auroraPaint1.setShader(g1);
        canvas.drawRect(0, 0, w, h, auroraPaint1);

        // 3. Teal aurora node (drifting around lower 60% of screen)
        float cx2 = (float) (w * 0.65 + Math.cos(angle * 1.1) * (w * 0.22));
        float cy2 = (float) (h * 0.60 + Math.sin(angle * 0.9) * (h * 0.18));
        float radius2 = Math.max(w, h) * 0.60f;
        RadialGradient g2 = new RadialGradient(
            cx2, cy2, radius2,
            new int[] { Color.argb(38, 67, 220, 199), Color.argb(12, 30, 120, 140), Color.TRANSPARENT },
            new float[] { 0.0f, 0.50f, 1.0f },
            Shader.TileMode.CLAMP
        );
        auroraPaint2.setShader(g2);
        canvas.drawRect(0, 0, w, h, auroraPaint2);

        // 4. Subtle deep indigo undertone for atmospheric depth
        float cx3 = (float) (w * 0.20 + Math.cos(angle * 0.7) * (w * 0.15));
        float cy3 = (float) (h * 0.80 + Math.sin(angle * 0.6) * (h * 0.12));
        float radius3 = Math.max(w, h) * 0.55f;
        RadialGradient g3 = new RadialGradient(
            cx3, cy3, radius3,
            new int[] { Color.argb(40, 46, 16, 101), Color.TRANSPARENT },
            new float[] { 0.0f, 1.0f },
            Shader.TileMode.CLAMP
        );
        auroraPaint3.setShader(g3);
        canvas.drawRect(0, 0, w, h, auroraPaint3);
    }
}
