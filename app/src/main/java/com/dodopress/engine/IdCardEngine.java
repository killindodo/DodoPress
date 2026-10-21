package com.dodopress.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Precision ID Card Composition Engine for DodoPress Pro.
 * Formats Front and Back of 1 or 2 ID cards (Aadhaar, Driving License, PAN, Student IDs)
 * onto an A4 sheet with adjustable physical spacing (mm), scale size, and placement.
 */
public class IdCardEngine {

    public enum IdPlacement {
        TOP_HALF("Top Half (Save Lower Paper)"),
        CENTERED("Center of Page"),
        TOP_AND_BOTTOM("Top & Bottom Split");

        public final String title;
        IdPlacement(String title) {
            this.title = title;
        }
    }

    public static class IdCardConfig {
        public int idCount = 1; // 1 or 2 ID cards on the sheet
        public int spacingMm = 20; // Physical gap between Front and Back (e.g. 10mm, 20mm, 30mm, 40mm)
        public float scale = 1.0f; // Scale multiplier: 0.85f (85%), 1.0f (100%), 1.15f (115%), 1.30f (130%)
        public IdPlacement placement = IdPlacement.TOP_HALF;
        public boolean showCutGuides = true;
        public boolean showLabels = true;
    }

    // Standard ID-1 Dimensions: 85.60 mm × 53.98 mm
    public static final float STANDARD_CARD_WIDTH_MM = 85.60f;
    public static final float STANDARD_CARD_HEIGHT_MM = 53.98f;

    /**
     * Backward-compatible 2-card overload
     */
    public static Bitmap composeIdCardSheet(Bitmap frontBm, Bitmap backBm, int sheetW, int sheetH) {
        IdCardConfig cfg = new IdCardConfig();
        return composeIdCardSheet(frontBm, backBm, null, null, cfg, sheetW, sheetH);
    }

    /**
     * Master multi-ID composer supporting 1 or 2 IDs, spacing, scaling, and placement.
     */
    public static Bitmap composeIdCardSheet(
            Bitmap id1Front, Bitmap id1Back,
            Bitmap id2Front, Bitmap id2Back,
            IdCardConfig config,
            int sheetW, int sheetH
    ) {
        return composeIdCardSheet(id1Front, id1Back, id2Front, id2Back, config, sheetW, sheetH, 210.0f, 297.0f);
    }

    public static Bitmap composeIdCardSheet(
            Bitmap id1Front, Bitmap id1Back,
            Bitmap id2Front, Bitmap id2Back,
            IdCardConfig config,
            int sheetW, int sheetH,
            float sheetWidthMm, float sheetHeightMm
    ) {
        if (config == null) config = new IdCardConfig();

        Bitmap sheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888);
        sheet.eraseColor(0xFFFFFFFF); // Pure white physical sheet

        Canvas canvas = new Canvas(sheet);

        // Paints
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF334155); // Slate 700 border
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(2f, sheetW / 800f));

        Paint dashedGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashedGuidePaint.setColor(0xFF94A3B8); // Slate 400
        dashedGuidePaint.setStyle(Paint.Style.STROKE);
        dashedGuidePaint.setStrokeWidth(Math.max(1.5f, sheetW / 1200f));
        dashedGuidePaint.setPathEffect(new DashPathEffect(new float[]{12f, 12f}, 0));

        Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        placeholderPaint.setColor(0xFFF1F5F9); // Light slate fill
        placeholderPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF0F172A); // Slate 900
        textPaint.setTextSize(Math.max(20f, sheetW / 75f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(0xFF64748B);
        subTextPaint.setTextSize(Math.max(14f, sheetW / 110f));
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate px per mm based on actual sheet width
        float pxPerMm = (float) sheetW / Math.max(1.0f, sheetWidthMm);

        int cardW = Math.round(STANDARD_CARD_WIDTH_MM * pxPerMm * config.scale);
        int cardH = Math.round(STANDARD_CARD_HEIGHT_MM * pxPerMm * config.scale);
        int gapPx = Math.round(config.spacingMm * pxPerMm);

        // Ensure cards fit within sheet
        if ((cardW * 2 + gapPx) > (sheetW - (int)(20 * pxPerMm))) {
            float maxAvailW = sheetW - (20 * pxPerMm) - gapPx;
            cardW = (int) (maxAvailW / 2f);
            cardH = (int) (cardW * (STANDARD_CARD_HEIGHT_MM / STANDARD_CARD_WIDTH_MM));
        }

        int rowWidth = (cardW * 2) + gapPx;
        int startX = (sheetW - rowWidth) / 2;

        if (config.idCount == 1) {
            // SINGLE ID (Front + Back)
            int startY;
            if (config.placement == IdPlacement.CENTERED) {
                startY = (sheetH - cardH) / 2;
            } else {
                // TOP_HALF: centered inside the top half (0 to sheetH / 2)
                int topHalfH = sheetH / 2;
                startY = (topHalfH - cardH) / 2;

                // Draw subtle half-page cut/fold guide line
                if (config.showCutGuides) {
                    canvas.drawLine(30 * pxPerMm, topHalfH, sheetW - 30 * pxPerMm, topHalfH, dashedGuidePaint);
                    canvas.drawText("✂ FOLD OR CUT LINE (LOWER HALF AVAILABLE FOR REUSE)", sheetW / 2f, topHalfH + 18 * pxPerMm, subTextPaint);
                }
            }

            Rect frontSlot = new Rect(startX, startY, startX + cardW, startY + cardH);
            Rect backSlot = new Rect(startX + cardW + gapPx, startY, startX + rowWidth, startY + cardH);

            renderSlot(canvas, id1Front, frontSlot, "ID #1 : FRONT", "85.6 × 54 mm", borderPaint, placeholderPaint, textPaint, subTextPaint);
            renderSlot(canvas, id1Back, backSlot, "ID #1 : BACK", "85.6 × 54 mm", borderPaint, placeholderPaint, textPaint, subTextPaint);

        } else {
            // DUAL ID (ID #1 Front+Back and ID #2 Front+Back)
            int topHalfCenterY = (sheetH / 4);
            int bottomHalfCenterY = (sheetH * 3 / 4);

            int row1Y = topHalfCenterY - (cardH / 2);
            int row2Y = bottomHalfCenterY - (cardH / 2);

            // Row 1: ID #1
            Rect id1FrontSlot = new Rect(startX, row1Y, startX + cardW, row1Y + cardH);
            Rect id1BackSlot = new Rect(startX + cardW + gapPx, row1Y, startX + rowWidth, row1Y + cardH);

            renderSlot(canvas, id1Front, id1FrontSlot, "ID #1 : FRONT", "Card 1 (Top)", borderPaint, placeholderPaint, textPaint, subTextPaint);
            renderSlot(canvas, id1Back, id1BackSlot, "ID #1 : BACK", "Card 1 (Top)", borderPaint, placeholderPaint, textPaint, subTextPaint);

            // Center dividing cut guide
            if (config.showCutGuides) {
                canvas.drawLine(30 * pxPerMm, sheetH / 2f, sheetW - 30 * pxPerMm, sheetH / 2f, dashedGuidePaint);
                canvas.drawText("✂ HORIZONTAL CUT LINE", sheetW / 2f, (sheetH / 2f) + 16 * pxPerMm, subTextPaint);
            }

            // Row 2: ID #2
            Rect id2FrontSlot = new Rect(startX, row2Y, startX + cardW, row2Y + cardH);
            Rect id2BackSlot = new Rect(startX + cardW + gapPx, row2Y, startX + rowWidth, row2Y + cardH);

            renderSlot(canvas, id2Front, id2FrontSlot, "ID #2 : FRONT", "Card 2 (Bottom)", borderPaint, placeholderPaint, textPaint, subTextPaint);
            renderSlot(canvas, id2Back, id2BackSlot, "ID #2 : BACK", "Card 2 (Bottom)", borderPaint, placeholderPaint, textPaint, subTextPaint);
        }

        return sheet;
    }

    private static void renderSlot(
            Canvas canvas, Bitmap bm, Rect slot,
            String label, String sublabel,
            Paint borderPaint, Paint bgPaint,
            Paint textPaint, Paint subTextPaint
    ) {
        float cornerRadius = 14f;

        if (bm != null && !bm.isRecycled()) {
            Rect fitRect = SheetGeometry.calculateAspectFit(bm.getWidth(), bm.getHeight(), slot, 4);
            canvas.drawRoundRect(new RectF(fitRect), cornerRadius, cornerRadius, bgPaint);
            canvas.drawBitmap(bm, null, fitRect, null);
            canvas.drawRoundRect(new RectF(fitRect), cornerRadius, cornerRadius, borderPaint);
            canvas.drawText(label, fitRect.centerX(), fitRect.bottom + textPaint.getTextSize() * 1.15f, textPaint);
        } else {
            // Styled technical card placeholder
            canvas.drawRoundRect(new RectF(slot), cornerRadius, cornerRadius, bgPaint);
            canvas.drawRoundRect(new RectF(slot), cornerRadius, cornerRadius, borderPaint);

            float centerY = slot.centerY();
            canvas.drawText("🪪 " + label, slot.centerX(), centerY - 6f, textPaint);
            canvas.drawText("[ Tap to Choose Photo ]", slot.centerX(), centerY + subTextPaint.getTextSize() * 1.2f, subTextPaint);
        }
    }
}
