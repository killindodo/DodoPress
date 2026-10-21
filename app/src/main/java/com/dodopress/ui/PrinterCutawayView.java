package com.dodopress.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class PrinterCutawayView extends View {

    public enum PrinterState {
        READY("Printer Online • Ready to Print", 0xFF10B981),
        OFFLINE("USB OTG Disconnected • Plug Cable", 0xFFF59E0B),
        PAPER_EMPTY("Paper Tray Empty • Insert A4 Sheet", 0xFFEF4444),
        DOOR_OPEN("Toner Door Open • Close Cartridge Hatch", 0xFFF97316),
        PAPER_JAM("Paper Jam Detected • Clear Roller Path", 0xFFDC2626),
        PRINTING("Printing Sheet in Progress...", 0xFF3B82F6);

        public final String statusText;
        public final int statusColor;

        PrinterState(String statusText, int statusColor) {
            this.statusText = statusText;
            this.statusColor = statusColor;
        }
    }

    private PrinterState state = PrinterState.OFFLINE;

    private Paint chassisPaint;
    private Paint chamberPaint;
    private Paint highlightPaint;
    private Paint textPaint;
    private Paint laserPaint;

    private float animPhase = 0f;
    private ValueAnimator pulseAnimator;

    public PrinterCutawayView(Context context) {
        super(context);
        init();
    }

    public PrinterCutawayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        chassisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chassisPaint.setStyle(Paint.Style.FILL);

        chamberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chamberPaint.setStyle(Paint.Style.FILL);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeWidth(3f);

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            animPhase = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setPrinterState(PrinterState newState) {
        this.state = newState;
        invalidate();
    }

    public PrinterState getPrinterState() {
        return state;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;

        // Base chassis bounds
        float pad = 16 * density;
        float bodyLeft = pad;
        float bodyTop = pad + 24 * density;
        float bodyRight = w - pad;
        float bodyBottom = h - 36 * density;

        // 1. Scanner Flatbed Glass & Lid (Top)
        chassisPaint.setColor(0xFF1E293B);
        RectF lidRect = new RectF(bodyLeft + 10, bodyTop - 18 * density, bodyRight - 10, bodyTop);
        canvas.drawRoundRect(lidRect, 6 * density, 6 * density, chassisPaint);

        // Glass strip
        chamberPaint.setColor(0xFF38BDF8);
        RectF glassRect = new RectF(bodyLeft + 20, bodyTop - 6 * density, bodyRight - 20, bodyTop - 2 * density);
        canvas.drawRoundRect(glassRect, 2 * density, 2 * density, chamberPaint);

        // 2. Main Printer Chassis
        chassisPaint.setColor(0xFF0F172A);
        RectF chassisRect = new RectF(bodyLeft, bodyTop, bodyRight, bodyBottom);
        canvas.drawRoundRect(chassisRect, 14 * density, 14 * density, chassisPaint);

        highlightPaint.setColor(0xFF334155);
        canvas.drawRoundRect(chassisRect, 14 * density, 14 * density, highlightPaint);

        // 3. Middle Output Bin / Cartridge Hatch
        float hatchTop = bodyTop + 24 * density;
        float hatchBottom = hatchTop + 60 * density;
        RectF hatchRect = new RectF(bodyLeft + 16 * density, hatchTop, bodyRight - 16 * density, hatchBottom);

        if (state == PrinterState.DOOR_OPEN) {
            int glowAlpha = (int) (120 + 135 * animPhase);
            chamberPaint.setColor(Color.argb(glowAlpha, 249, 115, 22)); // Glowing orange
            canvas.drawRoundRect(hatchRect, 8 * density, 8 * density, chamberPaint);
            highlightPaint.setColor(0xFFF97316);
            canvas.drawRoundRect(hatchRect, 8 * density, 8 * density, highlightPaint);
        } else {
            chamberPaint.setColor(0xFF1E293B);
            canvas.drawRoundRect(hatchRect, 8 * density, 8 * density, chamberPaint);
            highlightPaint.setColor(0xFF475569);
            canvas.drawRoundRect(hatchRect, 8 * density, 8 * density, highlightPaint);
        }

        // Toner Cartridge Icon in middle
        chamberPaint.setColor(0xFF020617);
        float tonerLeft = hatchRect.centerX() - 40 * density;
        float tonerRight = hatchRect.centerX() + 40 * density;
        RectF tonerRect = new RectF(tonerLeft, hatchRect.centerY() - 14 * density, tonerRight, hatchRect.centerY() + 14 * density);
        canvas.drawRoundRect(tonerRect, 6 * density, 6 * density, chamberPaint);

        textPaint.setTextSize(10 * density);
        textPaint.setColor(0xFF94A3B8);
        canvas.drawText("HP Q2612A TONER", tonerRect.centerX(), tonerRect.centerY() + 4 * density, textPaint);

        // 4. Paper Path (Curved Guide)
        if (state == PrinterState.PRINTING) {
            laserPaint.setColor(0xFF38BDF8);
            laserPaint.setStrokeWidth(3 * density);
            float laserY = hatchTop + (animPhase * (hatchBottom - hatchTop));
            canvas.drawLine(hatchRect.left + 10, laserY, hatchRect.right - 10, laserY, laserPaint);
        } else if (state == PrinterState.PAPER_JAM) {
            int jamAlpha = (int) (140 + 115 * animPhase);
            highlightPaint.setColor(Color.argb(jamAlpha, 220, 38, 38));
            highlightPaint.setStrokeWidth(4 * density);
            canvas.drawRoundRect(hatchRect, 8 * density, 8 * density, highlightPaint);
        }

        // 5. Bottom Paper Input Tray (Tray 1)
        float trayTop = bodyBottom - 48 * density;
        RectF trayRect = new RectF(bodyLeft + 16 * density, trayTop, bodyRight - 16 * density, bodyBottom - 8 * density);

        if (state == PrinterState.PAPER_EMPTY) {
            int glowAlpha = (int) (120 + 135 * animPhase);
            chamberPaint.setColor(Color.argb(glowAlpha, 239, 68, 68)); // Glowing Red
            canvas.drawRoundRect(trayRect, 8 * density, 8 * density, chamberPaint);
            highlightPaint.setColor(0xFFEF4444);
            canvas.drawRoundRect(trayRect, 8 * density, 8 * density, highlightPaint);

            Paint emptyLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
            emptyLabel.setColor(0xFFFFFFFF);
            emptyLabel.setTextSize(8.5f * density);
            emptyLabel.setTextAlign(Paint.Align.CENTER);
            emptyLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("⚠️ LOAD PAPER IN TRAY 1", trayRect.centerX(), trayRect.centerY() + 3 * density, emptyLabel);
        } else {
            chamberPaint.setColor(0xFF1E293B);
            canvas.drawRoundRect(trayRect, 8 * density, 8 * density, chamberPaint);
            highlightPaint.setColor(0xFF475569);
            canvas.drawRoundRect(trayRect, 8 * density, 8 * density, highlightPaint);

            // Technical Paper Cassette Slot
            chamberPaint.setColor(0xFF0F172A);
            RectF paperSlot = new RectF(trayRect.left + 8 * density, trayRect.top + 6 * density, trayRect.right - 8 * density, trayRect.bottom - 6 * density);
            canvas.drawRoundRect(paperSlot, 4 * density, 4 * density, chamberPaint);

            // Realistic multi-sheet media edge highlight (no stark white placeholder)
            Paint paperEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
            paperEdge.setStyle(Paint.Style.STROKE);
            paperEdge.setStrokeWidth(1.2f * density);
            paperEdge.setColor(0xFF64748B);
            RectF paperSheet = new RectF(trayRect.left + 14 * density, trayRect.top + 9 * density, trayRect.right - 14 * density, trayRect.bottom - 9 * density);

            Paint paperBody = new Paint(Paint.ANTI_ALIAS_FLAG);
            paperBody.setColor(0xFF1E293B);
            canvas.drawRoundRect(paperSheet, 3 * density, 3 * density, paperBody);
            canvas.drawRoundRect(paperSheet, 3 * density, 3 * density, paperEdge);

            // Technical Cassette Monospace Label
            Paint trayLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
            trayLabel.setColor(0xFF94A3B8);
            trayLabel.setTextSize(8 * density);
            trayLabel.setTextAlign(Paint.Align.CENTER);
            trayLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("TRAY 1 : A4 / LTR MEDIA READY", trayRect.centerX(), trayRect.centerY() + 3 * density, trayLabel);
        }

        // 6. USB Status Indicator (Bottom right edge)
        float usbX = bodyRight - 8 * density;
        float usbY = bodyBottom - 26 * density;
        Paint usbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (state == PrinterState.OFFLINE) {
            int usbAlpha = (int) (120 + 135 * animPhase);
            usbPaint.setColor(Color.argb(usbAlpha, 245, 158, 11)); // Amber pulse
        } else {
            usbPaint.setColor(0xFF10B981); // Green
        }
        canvas.drawCircle(usbX, usbY, 5 * density, usbPaint);

        // 7. Status Banner Text at Bottom
        textPaint.setTextSize(12 * density);
        textPaint.setColor(state.statusColor);
        canvas.drawText(state.statusText, w / 2f, h - 10 * density, textPaint);
    }
}
