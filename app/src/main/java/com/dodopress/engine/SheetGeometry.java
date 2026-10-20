package com.dodopress.engine;

import android.graphics.Rect;

public class SheetGeometry {

    public enum PaperSize {
        A4("A4 (210×297 mm)", 595, 842),
        LETTER("Letter (8.5×11 in)", 612, 792),
        LEGAL("Legal (8.5×14 in)", 612, 1008),
        A5("A5 (148×210 mm)", 420, 595);

        public final String title;
        public final int widthPts;
        public final int heightPts;

        PaperSize(String title, int widthPts, int heightPts) {
            this.title = title;
            this.widthPts = widthPts;
            this.heightPts = heightPts;
        }

        public int getPixelWidth(int dpi) {
            int targetW = (int) Math.round((widthPts / 72.0) * dpi);
            return (targetW + 31) & ~31; // Word-aligned for raster
        }

        public int getPixelHeight(int dpi) {
            return ((int) Math.round((heightPts / 72.0) * dpi)) & ~1;
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
     * Calculates the target bounding box on the sheet for the selected region.
     */
    public static Rect getTargetBox(PlacementRegion region, int sheetW, int sheetH) {
        int x = (int) (region.relX * sheetW);
        int y = (int) (region.relY * sheetH);
        int w = (int) (region.relW * sheetW);
        int h = (int) (region.relH * sheetH);
        return new Rect(x, y, x + w, y + h);
    }

    /**
     * Calculates tile bounding boxes for N-Up multi-page printing.
     * ONE_UP: Single full-page bounding box inside sheet margins.
     * TWO_UP: Two stacked boxes (portrait) or side-by-side (landscape) with 2mm gutter.
     * FOUR_UP: 2x2 grid of 4 bounding boxes with 2mm gutter.
     */
    public static Rect[] calculateNUpBoxes(TilingMode mode, int sheetW, int sheetH, int marginPx) {
        if (mode == null || mode == TilingMode.ONE_UP) {
            int left = Math.min(marginPx, sheetW / 2);
            int top = Math.min(marginPx, sheetH / 2);
            int right = Math.max(left + 1, sheetW - marginPx);
            int bottom = Math.max(top + 1, sheetH - marginPx);
            return new Rect[] { new Rect(left, top, right, bottom) };
        }

        // 2mm physical gutter scaled to sheet resolution (based on standard 210mm width for portrait)
        float sheetBaseDim = (sheetH >= sheetW) ? 210.0f : 297.0f;
        float pxPerMm = (float) sheetW / sheetBaseDim;
        int gutterPx = Math.max(2, Math.round(pxPerMm * 2.0f));

        int effectiveMargin = Math.max(0, marginPx);
        int availW = Math.max(1, sheetW - (effectiveMargin * 2));
        int availH = Math.max(1, sheetH - (effectiveMargin * 2));

        if (mode == TilingMode.TWO_UP) {
            if (sheetH >= sheetW) {
                // Stacked vertically: Top Half and Bottom Half
                int slotH = Math.max(1, (availH - gutterPx) / 2);
                int top0 = effectiveMargin;
                int bottom0 = top0 + slotH;
                Rect box0 = new Rect(effectiveMargin, top0, sheetW - effectiveMargin, bottom0);

                int top1 = bottom0 + gutterPx;
                int bottom1 = Math.min(sheetH - effectiveMargin, top1 + slotH);
                Rect box1 = new Rect(effectiveMargin, top1, sheetW - effectiveMargin, bottom1);

                return new Rect[] { box0, box1 };
            } else {
                // Side-by-side: Left Half and Right Half
                int slotW = Math.max(1, (availW - gutterPx) / 2);
                int left0 = effectiveMargin;
                int right0 = left0 + slotW;
                Rect box0 = new Rect(left0, effectiveMargin, right0, sheetH - effectiveMargin);

                int left1 = right0 + gutterPx;
                int right1 = Math.min(sheetW - effectiveMargin, left1 + slotW);
                Rect box1 = new Rect(left1, effectiveMargin, right1, sheetH - effectiveMargin);

                return new Rect[] { box0, box1 };
            }
        }

        if (mode == TilingMode.FOUR_UP) {
            // 2x2 Grid of 4 boxes
            int slotW = Math.max(1, (availW - gutterPx) / 2);
            int slotH = Math.max(1, (availH - gutterPx) / 2);

            int left0 = effectiveMargin;
            int right0 = left0 + slotW;
            int left1 = right0 + gutterPx;
            int right1 = Math.min(sheetW - effectiveMargin, left1 + slotW);

            int top0 = effectiveMargin;
            int bottom0 = top0 + slotH;
            int top1 = bottom0 + gutterPx;
            int bottom1 = Math.min(sheetH - effectiveMargin, top1 + slotH);

            return new Rect[] {
                new Rect(left0, top0, right0, bottom0), // Tile 0: Top-Left
                new Rect(left1, top0, right1, bottom0), // Tile 1: Top-Right
                new Rect(left0, top1, right0, bottom1), // Tile 2: Bottom-Left
                new Rect(left1, top1, right1, bottom1)  // Tile 3: Bottom-Right
            };
        }

        return new Rect[] { new Rect(effectiveMargin, effectiveMargin, sheetW - effectiveMargin, sheetH - effectiveMargin) };
    }

    /**
     * Uniformly scales docW/docH to fit inside targetBox with aspect-ratio preservation (FIT_CENTER).
     * Guarantees ZERO distortion, stretching, or compression.
     */
    public static Rect calculateAspectFit(int docW, int docH, Rect targetBox, int marginPx) {
        int availW = Math.max(1, targetBox.width() - (marginPx * 2));
        int availH = Math.max(1, targetBox.height() - (marginPx * 2));

        float scale = Math.min((float) availW / (float) docW, (float) availH / (float) docH);
        int drawW = Math.max(1, (int) (docW * scale));
        int drawH = Math.max(1, (int) (docH * scale));

        int left = targetBox.left + marginPx + (availW - drawW) / 2;
        int top = targetBox.top + marginPx + (availH - drawH) / 2;

        return new Rect(left, top, left + drawW, top + drawH);
    }
}
