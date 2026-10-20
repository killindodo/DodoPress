package com.dodopress.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class LaserScanView extends View {

    private Paint beamPaint;
    private Paint tailPaint;
    private float scanY = -1f;
    private boolean isScanning = false;
    private ValueAnimator scanAnimator;
    private int laserColor = Color.parseColor("#38BDF8");

    public LaserScanView(Context context) {
        super(context);
        init();
    }

    public LaserScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LaserScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        beamPaint.setStrokeWidth(4f);

        tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setStyle(Paint.Style.FILL);
    }

    public void setLaserColor(int color) {
        this.laserColor = color;
    }

    public void triggerScan() {
        if (getHeight() == 0) {
            post(this::triggerScan);
            return;
        }

        if (scanAnimator != null) {
            scanAnimator.cancel();
        }

        isScanning = true;
        setVisibility(View.VISIBLE);
        setAlpha(1f);

        scanAnimator = ValueAnimator.ofFloat(0f, getHeight());
        scanAnimator.setDuration(850);
        scanAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scanAnimator.addUpdateListener(animation -> {
            scanY = (float) animation.getAnimatedValue();
            invalidate();
        });
        scanAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animate().alpha(0f).setDuration(250).withEndAction(() -> {
                    isScanning = false;
                    setVisibility(View.GONE);
                }).start();
            }
        });
        scanAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isScanning || scanY < 0) return;

        float w = getWidth();
        float tailHeight = 45f;
        float tailTop = Math.max(0, scanY - tailHeight);

        // Translucent laser glow tail
        LinearGradient tailGradient = new LinearGradient(
            0, tailTop, 0, scanY,
            Color.TRANSPARENT,
            Color.argb(70, Color.red(laserColor), Color.green(laserColor), Color.blue(laserColor)),
            Shader.TileMode.CLAMP
        );
        tailPaint.setShader(tailGradient);
        canvas.drawRect(0, tailTop, w, scanY, tailPaint);

        // Solid intense laser beam line
        beamPaint.setColor(laserColor);
        beamPaint.setAlpha(255);
        canvas.drawLine(0, scanY, w, scanY, beamPaint);

        // Center intense hot spot
        beamPaint.setColor(Color.WHITE);
        beamPaint.setAlpha(200);
        canvas.drawLine(w * 0.15f, scanY, w * 0.85f, scanY, beamPaint);
    }
}
