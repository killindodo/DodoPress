package com.dodopress.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class IdCardEngine {

    /**
     * Composes Front and Back ID card images side-by-side onto the TOP HALF of an A4 sheet at 300 DPI.
     * Dimensions for A4 @ 300 DPI: 2480 x 3508.
     * Top half available: 2480 x 1754.
     * Standard ID Card physical size: 85.6 mm x 53.98 mm (~1011 x 638 px @ 300 DPI).
     */
    public static Bitmap composeIdCardSheet(Bitmap frontBm, Bitmap backBm, int sheetW, int sheetH) {
        Bitmap sheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888);
        sheet.eraseColor(0xFFFFFFFF);

        Canvas canvas = new Canvas(sheet);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF888888);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF444444);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        int topHalfH = sheetH / 2;
        int cardSlotW = (sheetW - 120) / 2; // Two slots side by side with padding
        int cardSlotH = (int) (cardSlotW * (54.0f / 85.6f)); // Standard ID card ratio (1.58 : 1)

        // Slot 1: Front
        int frontLeft = 40;
        int frontTop = (topHalfH - cardSlotH) / 2;
        Rect frontSlot = new Rect(frontLeft, frontTop, frontLeft + cardSlotW, frontTop + cardSlotH);

        // Slot 2: Back
        int backLeft = frontLeft + cardSlotW + 40;
        int backTop = frontTop;
        Rect backSlot = new Rect(backLeft, backTop, backLeft + cardSlotW, backTop + cardSlotH);

        if (frontBm != null && !frontBm.isRecycled()) {
            Rect fitRect = SheetGeometry.calculateAspectFit(frontBm.getWidth(), frontBm.getHeight(), frontSlot, 4);
            canvas.drawBitmap(frontBm, null, fitRect, null);
            canvas.drawRoundRect(new RectF(fitRect), 12f, 12f, borderPaint);
            canvas.drawText("FRONT", fitRect.centerX(), fitRect.bottom + 42f, textPaint);
        } else {
            canvas.drawRoundRect(new RectF(frontSlot), 12f, 12f, borderPaint);
            canvas.drawText("[ Front of ID Card ]", frontSlot.centerX(), frontSlot.centerY(), textPaint);
        }

        if (backBm != null && !backBm.isRecycled()) {
            Rect fitRect = SheetGeometry.calculateAspectFit(backBm.getWidth(), backBm.getHeight(), backSlot, 4);
            canvas.drawBitmap(backBm, null, fitRect, null);
            canvas.drawRoundRect(new RectF(fitRect), 12f, 12f, borderPaint);
            canvas.drawText("BACK", fitRect.centerX(), fitRect.bottom + 42f, textPaint);
        } else {
            canvas.drawRoundRect(new RectF(backSlot), 12f, 12f, borderPaint);
            canvas.drawText("[ Back of ID Card ]", backSlot.centerX(), backSlot.centerY(), textPaint);
        }

        return sheet;
    }
}
