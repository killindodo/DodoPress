package com.dodopress.engine;

import android.graphics.Rect;

public class SheetGeometry {

    // Standard Laser Mechanical Hardware Margin Limits
    public static final float HARDWARE_MIN_MARGIN_MM = 4.5f;   // Physical minimum unprintable margin (laser drum grip limit)
    public static final float HARDWARE_LEAD_TRAIL_MM = 4.5f;  // Lead & trail mechanical edge grip limit (~12.8 pt)
    public static final float HARDWARE_SIDES_MM = 4.23f;       // Lateral mechanical margins (~12.0 pt)

    public enum Orientation {
        PORTRAIT("Portrait"),
        LANDSCAPE("Landscape");

        public final String title;

        Orientation(String title) {
            this.title = title;
        }
    }

    public enum PaperSize {
        A4("A4 (210×297 mm)", 595, 842, 210.0f, 297.0f, 595.28f, 841.89f),
        LETTER("Letter (8.5×11 in)", 612, 792, 215.9f, 279.4f, 612.0f, 792.0f),
        LEGAL("Legal (8.5×14 in)", 612, 1008, 215.9f, 355.6f, 612.0f, 1008.0f),
        A5("A5 (148×210 mm)", 420, 595, 148.0f, 210.0f, 419.53f, 595.28f);

        public final String title;
        public final int widthPts;
        public final int heightPts;
        public final float widthMm;
        public final float heightMm;
        public final float widthPtsExact;
        public final float heightPtsExact;

        PaperSize(String title, int widthPts, int heightPts, float widthMm, float heightMm, float widthPtsExact, float heightPtsExact) {
            this.title = title;
            this.widthPts = widthPts;
            this.heightPts = heightPts;
            this.widthMm = widthMm;
            this.heightMm = heightMm;
            this.widthPtsExact = widthPtsExact;
            this.heightPtsExact = heightPtsExact;
        }

        PaperSize(String title, int widthPts, int heightPts, float widthMm, float heightMm) {
            this(title, widthPts, heightPts, widthMm, heightMm, (widthMm / 25.4f) * 72.0f, (heightMm / 25.4f) * 72.0f);
        }

        public float getWidthMm(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.max(widthMm, heightMm) : Math.min(widthMm, heightMm);
        }

        public float getHeightMm(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.min(widthMm, heightMm) : Math.max(widthMm, heightMm);
        }

        public int getWidthPts(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.max(widthPts, heightPts) : Math.min(widthPts, heightPts);
        }

        public int getHeightPts(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.min(widthPts, heightPts) : Math.max(widthPts, heightPts);
        }

        public float getWidthPtsExact(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.max(widthPtsExact, heightPtsExact) : Math.min(widthPtsExact, heightPtsExact);
        }

        public float getHeightPtsExact(Orientation orientation) {
            return (orientation == Orientation.LANDSCAPE) ? Math.min(widthPtsExact, heightPtsExact) : Math.max(widthPtsExact, heightPtsExact);
        }

        public int getPixelWidth(int dpi) {
            return getPixelWidth(dpi, Orientation.PORTRAIT);
        }

        public int getPixelHeight(int dpi) {
            return getPixelHeight(dpi, Orientation.PORTRAIT);
        }

        public int getRawPixelWidth(int dpi, Orientation orientation) {
            return Math.round((getWidthMm(orientation) / 25.4f) * dpi);
        }

        public int getRawPixelHeight(int dpi, Orientation orientation) {
            return Math.round((getHeightMm(orientation) / 25.4f) * dpi);
        }

        public int getPixelWidth(int dpi, Orientation orientation) {
            int targetW = Math.round((getWidthMm(orientation) / 25.4f) * dpi);
            return (targetW + 31) & ~31; // 32-bit word-aligned for raster scanlines
        }

        public int getPixelHeight(int dpi, Orientation orientation) {
            int targetH = Math.round((getHeightMm(orientation) / 25.4f) * dpi);
            return (targetH + 31) & ~31; // 32-bit word-aligned for raster scanlines (ensures exact dimension parity under rotation)
        }
    }

    public enum PlacementRegion {
        FULL_PAGE("Full Page (100%)", 0.0f, 0.0f, 1.0f, 1.0f),
        TOP_HALF("Top Half (50%)", 0.0f, 0.0f, 1.0f, 0.5f),
        BOTTOM_HALF("Bottom Half (50%)", 0.0f, 0.5f, 1.0f, 0.5f),
        LEFT_HALF("Left Half (50%)", 0.0f, 0.0f, 0.5f, 1.0f),
        RIGHT_HALF("Right Half (50%)", 0.5f, 0.0f, 0.5f, 1.0f),
        QUAD_TOP_LEFT("Top-Left (1/4)", 0.0f, 0.0f, 0.5f, 0.5f),
        QUAD_TOP_RIGHT("Top-Right (1/4)", 0.5f, 0.0f, 0.5f, 0.5f),
        QUAD_BOTTOM_LEFT("Bottom-Left (1/4)", 0.0f, 0.5f, 0.5f, 0.5f),
        QUAD_BOTTOM_RIGHT("Bottom-Right (1/4)", 0.5f, 0.5f, 0.5f, 0.5f);

        public final String title;
        public final float relX;
        public final float relY;
        public final float relW;
        public final float relH;

        PlacementRegion(String title, float relX, float relY, float relW, float relH) {
            this.title = title;
            this.relX = relX;
            this.relY = relY;
            this.relW = relW;
            this.relH = relH;
        }
    }

    public enum TilingMode {
        ONE_UP("1-Up (Standard)", 1),
        TWO_UP("2-Up (2-in-1)", 2),
        FOUR_UP("4-Up (4-in-1)", 4);

        public final String title;
        public final int pagesPerSheet;

        TilingMode(String title, int pagesPerSheet) {
            this.title = title;
            this.pagesPerSheet = pagesPerSheet;
        }
    }

    /**
     * Calculates the exact target bounding box on the sheet for the selected region,
     * taking into account independent horizontal and vertical margins.
     * Guarantees 100% mathematical parity between UI preview and CUPS raster generation.
     */
    public static Rect getPlacementBoundingBox(PlacementRegion region, int sheetW, int sheetH, int marginPxX, int marginPxY) {
        if (region == null || region == PlacementRegion.FULL_PAGE) {
            int left = Math.min(marginPxX, sheetW / 2);
            int top = Math.min(marginPxY, sheetH / 2);
            int right = Math.max(left + 1, sheetW - marginPxX);
            int bottom = Math.max(top + 1, sheetH - marginPxY);
            return new Rect(left, top, right, bottom);
        }

        int rawLeft = Math.round(region.relX * sheetW);
        int rawTop = Math.round(region.relY * sheetH);
        int rawW = Math.round(region.relW * sheetW);
        int rawH = Math.round(region.relH * sheetH);
        int rawRight = rawLeft + rawW;
        int rawBottom = rawTop + rawH;

        int left = rawLeft + marginPxX;
        int top = rawTop + marginPxY;
        int right = Math.max(left + 1, rawRight - marginPxX);
        int bottom = Math.max(top + 1, rawBottom - marginPxY);

        return new Rect(left, top, right, bottom);
    }

    public static Rect getPlacementBoundingBox(PlacementRegion region, int sheetW, int sheetH, int marginPx) {
        return getPlacementBoundingBox(region, sheetW, sheetH, marginPx, marginPx);
    }

    /**
     * Backward-compatible raw region bounding box without margins.
     */
    public static Rect getTargetBox(PlacementRegion region, int sheetW, int sheetH) {
        return getPlacementBoundingBox(region, sheetW, sheetH, 0, 0);
    }

    /**
     * Calculates tile bounding boxes for N-Up multi-page printing with physical mm gutters.
     * ONE_UP: Single full-page bounding box inside sheet margins.
     * TWO_UP: Two stacked boxes (portrait) or side-by-side (landscape) with exact 2mm physical gutter.
     * FOUR_UP: 2x2 grid of 4 bounding boxes with exact 2mm physical gutters.
     */
    public static Rect[] calculateNUpBoxes(TilingMode mode, int sheetW, int sheetH, int marginPxX, int marginPxY, float sheetWidthMm, float sheetHeightMm) {
        if (mode == null || mode == TilingMode.ONE_UP) {
            return new Rect[] { getPlacementBoundingBox(PlacementRegion.FULL_PAGE, sheetW, sheetH, marginPxX, marginPxY) };
        }

        float pxPerMmX = (float) sheetW / Math.max(1f, sheetWidthMm);
        float pxPerMmY = (float) sheetH / Math.max(1f, sheetHeightMm);
        int gutterPxX = Math.max(2, Math.round(pxPerMmX * 2.0f)); // Exact 2.0 mm physical gutter
        int gutterPxY = Math.max(2, Math.round(pxPerMmY * 2.0f));

        int effectiveMarginX = Math.max(0, marginPxX);
        int effectiveMarginY = Math.max(0, marginPxY);
        int availW = Math.max(1, sheetW - (effectiveMarginX * 2));
        int availH = Math.max(1, sheetH - (effectiveMarginY * 2));

        if (mode == TilingMode.TWO_UP) {
            if (sheetH >= sheetW) {
                // Stacked vertically: Top Half and Bottom Half with 2mm gutter
                int slotH = Math.max(1, (availH - gutterPxY) / 2);
                int top0 = effectiveMarginY;
                int bottom0 = top0 + slotH;
                Rect box0 = new Rect(effectiveMarginX, top0, sheetW - effectiveMarginX, bottom0);

                int top1 = bottom0 + gutterPxY;
                int bottom1 = Math.min(sheetH - effectiveMarginY, top1 + slotH);
                Rect box1 = new Rect(effectiveMarginX, top1, sheetW - effectiveMarginX, bottom1);

                return new Rect[] { box0, box1 };
            } else {
                // Side-by-side: Left Half and Right Half with 2mm gutter
                int slotW = Math.max(1, (availW - gutterPxX) / 2);
                int left0 = effectiveMarginX;
                int right0 = left0 + slotW;
                Rect box0 = new Rect(left0, effectiveMarginY, right0, sheetH - effectiveMarginY);

                int left1 = right0 + gutterPxX;
                int right1 = Math.min(sheetW - effectiveMarginX, left1 + slotW);
                Rect box1 = new Rect(left1, effectiveMarginY, right1, sheetH - effectiveMarginY);

                return new Rect[] { box0, box1 };
            }
        }

        if (mode == TilingMode.FOUR_UP) {
            // 2x2 Grid of 4 boxes with 2mm gutters
            int slotW = Math.max(1, (availW - gutterPxX) / 2);
            int slotH = Math.max(1, (availH - gutterPxY) / 2);

            int left0 = effectiveMarginX;
            int right0 = left0 + slotW;
            int left1 = right0 + gutterPxX;
            int right1 = Math.min(sheetW - effectiveMarginX, left1 + slotW);

            int top0 = effectiveMarginY;
            int bottom0 = top0 + slotH;
            int top1 = bottom0 + gutterPxY;
            int bottom1 = Math.min(sheetH - effectiveMarginY, top1 + slotH);

            return new Rect[] {
                new Rect(left0, top0, right0, bottom0), // Tile 0: Top-Left
                new Rect(left1, top0, right1, bottom0), // Tile 1: Top-Right
                new Rect(left0, top1, right0, bottom1), // Tile 2: Bottom-Left
                new Rect(left1, top1, right1, bottom1)  // Tile 3: Bottom-Right
            };
        }

        return new Rect[] { getPlacementBoundingBox(PlacementRegion.FULL_PAGE, sheetW, sheetH, marginPxX, marginPxY) };
    }

    public static Rect[] calculateNUpBoxes(TilingMode mode, int sheetW, int sheetH, int marginPx) {
        float baseW = (sheetH >= sheetW) ? 210.0f : 297.0f;
        float baseH = (sheetH >= sheetW) ? 297.0f : 210.0f;
        return calculateNUpBoxes(mode, sheetW, sheetH, marginPx, marginPx, baseW, baseH);
    }

    /**
     * Uniformly scales docW/docH to fit inside targetBox with aspect-ratio preservation (FIT_CENTER).
     * Guarantees ZERO distortion, stretching, or compression, and exact integer subpixel centering.
     */
    public static Rect calculateAspectFit(int docW, int docH, Rect targetBox, int marginPx) {
        if (docW <= 0 || docH <= 0 || targetBox == null) return new Rect(0, 0, 1, 1);
        int availW = Math.max(1, targetBox.width() - (marginPx * 2));
        int availH = Math.max(1, targetBox.height() - (marginPx * 2));

        float scale = Math.min((float) availW / (float) docW, (float) availH / (float) docH);
        int drawW = Math.max(1, Math.round(docW * scale));
        int drawH = Math.max(1, Math.round(docH * scale));

        int left = targetBox.left + marginPx + (availW - drawW) / 2;
        int top = targetBox.top + marginPx + (availH - drawH) / 2;

        return new Rect(left, top, left + drawW, top + drawH);
    }

    /**
     * Mathematical exact calculation of printable dimensions in millimeters.
     */
    public static float[] getPrintableDimensionsMm(PaperSize paper, Orientation orientation, float marginMm) {
        float sheetW = paper.getWidthMm(orientation);
        float sheetH = paper.getHeightMm(orientation);
        float printW = Math.max(0f, sheetW - (2f * marginMm));
        float printH = Math.max(0f, sheetH - (2f * marginMm));
        return new float[] { printW, printH };
    }

    /**
     * Mathematical exact calculation of printable dimensions in PostScript points.
     */
    public static float[] getPrintableDimensionsPts(PaperSize paper, Orientation orientation, float marginMm) {
        float[] dimsMm = getPrintableDimensionsMm(paper, orientation, marginMm);
        return new float[] {
            (dimsMm[0] / 25.4f) * 72.0f,
            (dimsMm[1] / 25.4f) * 72.0f
        };
    }

    /**
     * Checks if current margin setting encroaches on the mechanical unprintable hardware zone (< 4.5 mm).
     */
    public static boolean isHardwareClipped(float marginMm) {
        return marginMm < HARDWARE_MIN_MARGIN_MM;
    }

    /**
     * Calculates the exact mechanical cutoff depth in millimeters when margin < 4.5 mm.
     * Cutoff delta = 4.5 mm - user_margin. If user_margin >= 4.5 mm, delta is 0.0 mm.
     */
    public static float getHardwareCutoffDeltaMm(float marginMm) {
        return Math.max(0f, HARDWARE_MIN_MARGIN_MM - marginMm);
    }

    /**
     * Converts a millimeter dimension to horizontal pixels based on actual sheet width.
     */
    public static int mmToPixelsX(float mm, int sheetPixelW, float sheetWidthMm) {
        if (sheetWidthMm <= 0) return 0;
        return Math.round((mm / sheetWidthMm) * sheetPixelW);
    }

    /**
     * Converts a millimeter dimension to vertical pixels based on actual sheet height.
     */
    public static int mmToPixelsY(float mm, int sheetPixelH, float sheetHeightMm) {
        if (sheetHeightMm <= 0) return 0;
        return Math.round((mm / sheetHeightMm) * sheetPixelH);
    }
}
