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
        TOP_AND_BOTTOM("Top & Bottom Split"),
        FOUR_UP("4-Up Grid Multi-Card");

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
        public SheetGeometry.PaperSize paperSize = SheetGeometry.PaperSize.A4;
        public SheetGeometry.Orientation orientation = SheetGeometry.Orientation.PORTRAIT;
        public float marginMm = 12.0f;
        public int darkness = ToneEngine.PRESET_NORMAL;
        public String watermarkText = "";
        public int copies = 1;
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
        if (config == null) config = new IdCardConfig();
        float sheetWidthMm = config.paperSize.getWidthMm(config.orientation);
        float sheetHeightMm = config.paperSize.getHeightMm(config.orientation);
        return composeIdCardSheet(id1Front, id1Back, id2Front, id2Back, config, sheetW, sheetH, sheetWidthMm, sheetHeightMm);
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
        dashedGuidePaint.setPathEffect(new DashPathEffect(new float[]{10f, 10f}, 0));

        Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        placeholderPaint.setColor(0xFFF1F5F9); // Light slate fill
        placeholderPaint.setStyle(Paint.Style.FILL);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF0F172A); // Slate 900
        textPaint.setTextSize(Math.max(16f, sheetW / 80f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(0xFF64748B);
        subTextPaint.setTextSize(Math.max(12f, sheetW / 115f));
        subTextPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate px per mm based on actual sheet width
        float pxPerMm = (float) sheetW / Math.max(1.0f, sheetWidthMm);

        // 1. Margin and Hardware Drum Boundary
        int marginPx = Math.round(config.marginMm * pxPerMm);
        int hwDrumPx = Math.round(SheetGeometry.HARDWARE_MIN_MARGIN_MM * pxPerMm);

        // Draw laser drum 4.5mm hardware non-printable indicator if margin is zero or close to hardware limit
        if (config.marginMm < SheetGeometry.HARDWARE_MIN_MARGIN_MM) {
            Paint drumHatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            drumHatchPaint.setColor(0x18EF4444); // Red warning tint on non-printable edge
            drumHatchPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, sheetW, hwDrumPx, drumHatchPaint);
            canvas.drawRect(0, sheetH - hwDrumPx, sheetW, sheetH, drumHatchPaint);
            canvas.drawRect(0, 0, hwDrumPx, sheetH, drumHatchPaint);
            canvas.drawRect(sheetW - hwDrumPx, 0, sheetW, sheetH, drumHatchPaint);

            Paint drumAlertPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            drumAlertPaint.setColor(0xFFEF4444);
            drumAlertPaint.setTextSize(Math.max(11f, sheetW / 65f));
            drumAlertPaint.setTextAlign(Paint.Align.CENTER);
            drumAlertPaint.setFakeBoldText(true);
            canvas.drawText("⚠️ 4.5mm DRUM EDGE NON-PRINTABLE", sheetW / 2f, hwDrumPx - 4, drumAlertPaint);
        }

        // Draw printable margin outline
        if (config.marginMm > 0) {
            Paint marginLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            marginLinePaint.setColor(0xFFCBD5E1); // Slate 300 subtle margin boundary
            marginLinePaint.setStyle(Paint.Style.STROKE);
            marginLinePaint.setStrokeWidth(Math.max(1f, sheetW / 1200f));
            marginLinePaint.setPathEffect(new DashPathEffect(new float[]{6f, 6f}, 0));
            canvas.drawRect(marginPx, marginPx, sheetW - marginPx, sheetH - marginPx, marginLinePaint);
        }

        // Printable area bounds
        int printableW = sheetW - (2 * marginPx);
        int printableH = sheetH - (2 * marginPx);

        int cardW = Math.round(STANDARD_CARD_WIDTH_MM * pxPerMm * config.scale);
        int cardH = Math.round(STANDARD_CARD_HEIGHT_MM * pxPerMm * config.scale);
        int gapPx = Math.round(config.spacingMm * pxPerMm);

        // Ensure cards fit within printable area
        if ((cardW * 2 + gapPx) > printableW) {
            float maxAvailW = printableW - gapPx;
            cardW = (int) (maxAvailW / 2f);
            cardH = (int) (cardW * (STANDARD_CARD_HEIGHT_MM / STANDARD_CARD_WIDTH_MM));
        }

        int rowWidth = (cardW * 2) + gapPx;
        int startX = marginPx + (printableW - rowWidth) / 2;

        if (config.idCount == 1) {
            // SINGLE ID
            if (config.placement == IdPlacement.FOUR_UP) {
                // 4-Up Grid: 2 sets of ID #1 (Top Row + Bottom Row duplicate)
                int topHalfCenterY = marginPx + (printableH / 4);
                int bottomHalfCenterY = marginPx + (printableH * 3 / 4);

                int row1Y = topHalfCenterY - (cardH / 2);
                int row2Y = bottomHalfCenterY - (cardH / 2);

                Rect id1FrontSlot1 = new Rect(startX, row1Y, startX + cardW, row1Y + cardH);
                Rect id1BackSlot1 = new Rect(startX + cardW + gapPx, row1Y, startX + rowWidth, row1Y + cardH);

                renderSlot(canvas, id1Front, id1FrontSlot1, "ID #1 : FRONT", "Set 1 (Top)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
                renderSlot(canvas, id1Back, id1BackSlot1, "ID #1 : BACK", "Set 1 (Top)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

                if (config.showCutGuides) {
                    float lineY = marginPx + (printableH / 2f);
                    canvas.drawLine(marginPx + (10 * pxPerMm), lineY, sheetW - marginPx - (10 * pxPerMm), lineY, dashedGuidePaint);
                    canvas.drawText("✂ HORIZONTAL CUT LINE", sheetW / 2f, lineY + 16 * pxPerMm, subTextPaint);
                }

                Rect id1FrontSlot2 = new Rect(startX, row2Y, startX + cardW, row2Y + cardH);
                Rect id1BackSlot2 = new Rect(startX + cardW + gapPx, row2Y, startX + rowWidth, row2Y + cardH);

                renderSlot(canvas, id1Front, id1FrontSlot2, "ID #1 : FRONT", "Set 2 (Copy)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
                renderSlot(canvas, id1Back, id1BackSlot2, "ID #1 : BACK", "Set 2 (Copy)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

            } else if (config.placement == IdPlacement.TOP_AND_BOTTOM) {
                // Front in Top Half, Back in Bottom Half (Both Centered)
                int singleCardStartX = marginPx + (printableW - cardW) / 2;
                int topHalfCenterY = marginPx + (printableH / 4);
                int bottomHalfCenterY = marginPx + (printableH * 3 / 4);

                int slot1Y = topHalfCenterY - (cardH / 2);
                int slot2Y = bottomHalfCenterY - (cardH / 2);

                Rect frontSlot = new Rect(singleCardStartX, slot1Y, singleCardStartX + cardW, slot1Y + cardH);
                Rect backSlot = new Rect(singleCardStartX, slot2Y, singleCardStartX + cardW, slot2Y + cardH);

                renderSlot(canvas, id1Front, frontSlot, "ID #1 : FRONT", "Top Half", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

                if (config.showCutGuides) {
                    float lineY = marginPx + (printableH / 2f);
                    canvas.drawLine(marginPx + (10 * pxPerMm), lineY, sheetW - marginPx - (10 * pxPerMm), lineY, dashedGuidePaint);
                    canvas.drawText("✂ FOLD OR CUT LINE", sheetW / 2f, lineY + 16 * pxPerMm, subTextPaint);
                }

                renderSlot(canvas, id1Back, backSlot, "ID #1 : BACK", "Bottom Half", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

            } else if (config.placement == IdPlacement.CENTERED) {
                int startY = marginPx + (printableH - cardH) / 2;
                Rect frontSlot = new Rect(startX, startY, startX + cardW, startY + cardH);
                Rect backSlot = new Rect(startX + cardW + gapPx, startY, startX + rowWidth, startY + cardH);

                renderSlot(canvas, id1Front, frontSlot, "ID #1 : FRONT", "85.6 × 54 mm", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
                renderSlot(canvas, id1Back, backSlot, "ID #1 : BACK", "85.6 × 54 mm", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

            } else {
                // TOP_HALF: centered inside top half of printable area
                int topHalfH = printableH / 2;
                int startY = marginPx + (topHalfH - cardH) / 2;

                if (config.showCutGuides) {
                    float lineY = marginPx + topHalfH;
                    canvas.drawLine(marginPx + (10 * pxPerMm), lineY, sheetW - marginPx - (10 * pxPerMm), lineY, dashedGuidePaint);
                    canvas.drawText("✂ FOLD OR CUT LINE (LOWER HALF AVAILABLE FOR REUSE)", sheetW / 2f, lineY + 16 * pxPerMm, subTextPaint);
                }

                Rect frontSlot = new Rect(startX, startY, startX + cardW, startY + cardH);
                Rect backSlot = new Rect(startX + cardW + gapPx, startY, startX + rowWidth, startY + cardH);

                renderSlot(canvas, id1Front, frontSlot, "ID #1 : FRONT", "85.6 × 54 mm", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
                renderSlot(canvas, id1Back, backSlot, "ID #1 : BACK", "85.6 × 54 mm", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
            }

        } else {
            // DUAL ID (ID #1 Front+Back and ID #2 Front+Back)
            int topHalfCenterY = marginPx + (printableH / 4);
            int bottomHalfCenterY = marginPx + (printableH * 3 / 4);

            int row1Y = topHalfCenterY - (cardH / 2);
            int row2Y = bottomHalfCenterY - (cardH / 2);

            // Row 1: ID #1
            Rect id1FrontSlot = new Rect(startX, row1Y, startX + cardW, row1Y + cardH);
            Rect id1BackSlot = new Rect(startX + cardW + gapPx, row1Y, startX + rowWidth, row1Y + cardH);

            renderSlot(canvas, id1Front, id1FrontSlot, "ID #1 : FRONT", "Card 1 (Top)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
            renderSlot(canvas, id1Back, id1BackSlot, "ID #1 : BACK", "Card 1 (Top)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);

            // Center dividing cut guide
            if (config.showCutGuides) {
                float lineY = marginPx + (printableH / 2f);
                canvas.drawLine(marginPx + (10 * pxPerMm), lineY, sheetW - marginPx - (10 * pxPerMm), lineY, dashedGuidePaint);
                canvas.drawText("✂ HORIZONTAL CUT LINE", sheetW / 2f, lineY + 16 * pxPerMm, subTextPaint);
            }

            // Row 2: ID #2
            Rect id2FrontSlot = new Rect(startX, row2Y, startX + cardW, row2Y + cardH);
            Rect id2BackSlot = new Rect(startX + cardW + gapPx, row2Y, startX + rowWidth, row2Y + cardH);

            renderSlot(canvas, id2Front, id2FrontSlot, "ID #2 : FRONT", "Card 2 (Bottom)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
            renderSlot(canvas, id2Back, id2BackSlot, "ID #2 : BACK", "Card 2 (Bottom)", config.darkness, borderPaint, placeholderPaint, textPaint, subTextPaint);
        }

        // 3. Watermark Stamp if requested
        if (config.watermarkText != null && !config.watermarkText.trim().isEmpty()) {
            Paint wmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wmPaint.setColor(0x3864748B); // Slate transparent watermark
            wmPaint.setTextSize(Math.max(28f, sheetW / 16f));
            wmPaint.setTextAlign(Paint.Align.CENTER);
            wmPaint.setFakeBoldText(true);
            wmPaint.setStyle(Paint.Style.STROKE);
            wmPaint.setStrokeWidth(2f);

            canvas.save();
            canvas.rotate(-35f, sheetW / 2f, sheetH / 2f);
            canvas.drawText(config.watermarkText.trim(), sheetW / 2f, sheetH / 2f, wmPaint);
            canvas.restore();
        }

        return sheet;
    }

    private static void renderSlot(
            Canvas canvas, Bitmap bm, Rect slot,
            String label, String sublabel, int darkness,
            Paint borderPaint, Paint bgPaint,
            Paint textPaint, Paint subTextPaint
    ) {
        float cornerRadius = 12f;

        if (bm != null && !bm.isRecycled()) {
            Rect fitRect = SheetGeometry.calculateAspectFit(bm.getWidth(), bm.getHeight(), slot, 4);
            canvas.drawRoundRect(new RectF(fitRect), cornerRadius, cornerRadius, bgPaint);

            Bitmap renderBm = bm;
            if (darkness != 50) {
                Bitmap toneProcessed = ToneEngine.processPreviewBitmap(bm, darkness);
                if (toneProcessed != null) {
                    renderBm = toneProcessed;
                }
            }

            canvas.drawBitmap(renderBm, null, fitRect, null);
            canvas.drawRoundRect(new RectF(fitRect), cornerRadius, cornerRadius, borderPaint);
            canvas.drawText(label, fitRect.centerX(), fitRect.bottom + textPaint.getTextSize() * 1.15f, textPaint);
        } else {
            // Styled technical card blueprint placeholder
            canvas.drawRoundRect(new RectF(slot), cornerRadius, cornerRadius, bgPaint);
            canvas.drawRoundRect(new RectF(slot), cornerRadius, cornerRadius, borderPaint);

            float centerY = slot.centerY();
            canvas.drawText("🪪 " + label, slot.centerX(), centerY - 6f, textPaint);
            canvas.drawText("[ Tap Below to Scan / Select ]", slot.centerX(), centerY + subTextPaint.getTextSize() * 1.25f, subTextPaint);
            canvas.drawText("85.6 × 54.0 mm", slot.centerX(), slot.bottom - 8f, subTextPaint);
        }
    }
}
