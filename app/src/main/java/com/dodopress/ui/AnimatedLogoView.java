package com.dodopress.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AnimatedLogoView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF bounds = new RectF();
    private final RectF innerBounds = new RectF();
    private final Path iconPath = new Path();

    private float rotationAngle = 0f;
    private ValueAnimator rotationAnimator;
    private int primaryColor = 0xFF38BDF8;
    private int accentColor = 0xFF818CF8;

    private boolean isSpinningFast = false;

    public AnimatedLogoView(Context context) {
        super(context);
        init();
    }

    public AnimatedLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedLogoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(1.5f);

        innerCirclePaint.setStyle(Paint.Style.FILL);

        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.WHITE);

        startContinuousRotation(24000); // Gentle ambient rotation: 24s per revolution
    }

    public void setThemeColors(int primary, int accent) {
        this.primaryColor = primary;
        this.accentColor = accent;
        ringPaint.setColor(primary);
        glowPaint.setColor(accent);
        invalidate();
    }

    public void setPrintingActive(boolean active) {
        if (active) {
            startContinuousRotation(2500); // Fast active spin during printing
        } else {
            startContinuousRotation(24000); // Ambient idle rotation
        }
    }

    private void startContinuousRotation(long duration) {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        rotationAnimator = ValueAnimator.ofFloat(rotationAngle, rotationAngle + 360f);
        rotationAnimator.setDuration(duration);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.addUpdateListener(anim -> {
            rotationAngle = (float) anim.getAnimatedValue() % 360f;
            invalidate();
        });
        rotationAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            animate().scaleX(0.88f).scaleY(0.88f).setDuration(120).start();
            // Temporary spin burst
            startContinuousRotation(1200);
            isSpinningFast = true;
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start();
            postDelayed(() -> {
                if (isSpinningFast) {
                    isSpinningFast = false;
                    startContinuousRotation(24000);
                }
            }, 3000);
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = Math.min(w, h) * 0.08f;
        bounds.set(pad, pad, w - pad, h - pad);
        float innerPad = Math.min(w, h) * 0.22f;
        innerBounds.set(innerPad, innerPad, w - innerPad, h - innerPad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - ringPaint.getStrokeWidth() * 2f;

        if (radius <= 0) return;

        // 1. Draw Inner Glowing Hub
        innerCirclePaint.setColor(Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));
        canvas.drawCircle(cx, cy, radius * 0.76f, innerCirclePaint);

        // 2. Draw Rotating Outer Technical Ring & Cogs
        canvas.save();
        canvas.rotate(rotationAngle, cx, cy);

        ringPaint.setColor(primaryColor);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        // Draw 8 orbital tech notches around circumference
        float notchLen = radius * 0.14f;
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4.0);
            float x1 = cx + (radius - notchLen) * (float) Math.cos(angle);
            float y1 = cy + (radius - notchLen) * (float) Math.sin(angle);
            float x2 = cx + (radius + 2f) * (float) Math.cos(angle);
            float y2 = cy + (radius + 2f) * (float) Math.sin(angle);
            canvas.drawLine(x1, y1, x2, y2, ringPaint);
        }
        canvas.restore();

        // 3. Draw Opposite Rotating Subtle Inner Ring
        canvas.save();
        canvas.rotate(-rotationAngle * 0.5f, cx, cy);
        glowPaint.setColor(Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        canvas.drawCircle(cx, cy, radius * 0.82f, glowPaint);
        canvas.restore();

        // 4. Draw Core Iconic Printer / Dodo Symbol (Center Static for clarity)
        float iconSize = radius * 0.52f;
        iconPaint.setColor(primaryColor);

        // Stylized Printer Base
        RectF printerBody = new RectF(cx - iconSize * 0.75f, cy - iconSize * 0.25f, cx + iconSize * 0.75f, cy + iconSize * 0.55f);
        canvas.drawRoundRect(printerBody, 4f, 4f, iconPaint);

        // Paper Feed Top
        Paint paperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paperPaint.setColor(Color.WHITE);
        RectF paperTop = new RectF(cx - iconSize * 0.45f, cy - iconSize * 0.75f, cx + iconSize * 0.45f, cy - iconSize * 0.15f);
        canvas.drawRoundRect(paperTop, 3f, 3f, paperPaint);

        // Paper Output Tray
        RectF paperOut = new RectF(cx - iconSize * 0.50f, cy + iconSize * 0.15f, cx + iconSize * 0.50f, cy + iconSize * 0.75f);
        canvas.drawRoundRect(paperOut, 3f, 3f, paperPaint);

        // Laser Output Line
        Paint laserLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserLine.setColor(accentColor);
        laserLine.setStrokeWidth(2.5f);
        canvas.drawLine(cx - iconSize * 0.35f, cy + iconSize * 0.45f, cx + iconSize * 0.35f, cy + iconSize * 0.45f, laserLine);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }
}
