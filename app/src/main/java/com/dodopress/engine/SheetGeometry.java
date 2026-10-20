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
