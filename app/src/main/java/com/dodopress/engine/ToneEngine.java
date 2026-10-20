package com.dodopress.engine;

import android.graphics.Bitmap;
import java.io.IOException;
import java.io.OutputStream;

public class ToneEngine {

    public static final int PRESET_DRAFT = 25;
    public static final int PRESET_NORMAL = 50;
    public static final int PRESET_DEEP = 75; // Locked PC-Match Default
    public static final int PRESET_ULTRA = 100; // Max Deep (Safe & Protected)

    // Standard high-frequency Bayer 8x8 dither threshold matrix (values 0..252 in steps of 4)
    public static final int[][] BAYER8 = {
        {   0, 128,  32, 160,   8, 136,  40, 168 },
        { 192,  64, 224,  96, 200,  72, 232, 104 },
        {  48, 176,  16, 144,  56, 184,  24, 152 },
        { 240, 112, 208,  80, 248, 120, 216,  88 },
        {  12, 140,  44, 172,   4, 132,  36, 164 },
        { 204,  76, 236, 108, 196,  68, 228, 100 },
        {  60, 188,  28, 156,  52, 180,  20, 148 },
        { 252, 124, 220,  92, 244, 116, 212,  84 }
    };

    private static final int PAPER_CUTOFF = 244;
    private static final int DARK_CORE_REQ = 90;
    private static final int DELTA_REQ = 45;

    /**
     * Builds a 256-entry lookup table for tone mapping operations.
     */
    public static int[] buildLut(int darkness) {
        int[] lut = new int[256];
        int threshold = calculateThreshold(darkness);
        for (int i = 0; i < 256; i++) {
            lut[i] = (i <= threshold) ? 0 : 255;
        }
        return lut;
    }

    public static int calculateThreshold(int darkness) {
        if (darkness <= 50) {
            return 90 + (int) ((darkness / 50.0) * 50); // 90..140
        } else {
            return 140 + (int) (((darkness - 50) / 50.0) * 55); // 140..195
        }
    }

    private static int getCoreBlackLimit(int darkness) {
        return 85 + (int) ((Math.min(100, Math.max(0, darkness)) / 100.0) * 65); // 85..150
    }

    private static int getEdgeLimit(int darkness) {
        if (darkness <= 50) {
            return 120 + (int) ((darkness / 50.0) * 55); // 120..175
        } else {
            // Strictly capped at 215 so backgrounds (>= 220) are NEVER treated as text edges!
            return 175 + (int) (((darkness - 50) / 50.0) * 40); // 175..215
        }
    }

    /**
     * Generates a bit-exact preview of the physical laser print output.
     * Accurately reproduces deep black text strokes and fine-dithered shading.
     */
    public static Bitmap processPreviewBitmap(Bitmap src, int darkness) {
        if (src == null || src.isRecycled()) return null;

        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] srcPixels = new int[width * height];
        src.getPixels(srcPixels, 0, width, 0, 0, width, height);

        // Precompute luminance grid for preview
        byte[] luma = new byte[width * height];
        for (int i = 0; i < srcPixels.length; i++) {
            int c = srcPixels[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            luma[i] = (byte) ((r * 306 + g * 601 + b * 117) >> 10);
        }

        int coreLimit = getCoreBlackLimit(darkness);
        int edgeLimit = getEdgeLimit(darkness);

        int[] outPixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            int prevRowOffset = (y > 0) ? (y - 1) * width : rowOffset;
            int nextRowOffset = (y < height - 1) ? (y + 1) * width : rowOffset;
            int[] bayerRow = BAYER8[y & 7];

            for (int x = 0; x < width; x++) {
                int l = luma[rowOffset + x] & 0xFF;
                boolean isBlack;

                if (l >= PAPER_CUTOFF) {
                    isBlack = false;
                } else if (l <= coreLimit) {
                    isBlack = true;
                } else {
                    int min3 = l;
                    int max3 = l;
                    int x0 = (x > 0) ? x - 1 : 0;
                    int x1 = (x < width - 1) ? x + 1 : width - 1;

                    for (int nx = x0; nx <= x1; nx++) {
                        int v0 = luma[prevRowOffset + nx] & 0xFF;
                        if (v0 < min3) min3 = v0;
                        if (v0 > max3) max3 = v0;

                        int v1 = luma[rowOffset + nx] & 0xFF;
                        if (v1 < min3) min3 = v1;
                        if (v1 > max3) max3 = v1;

                        int v2 = luma[nextRowOffset + nx] & 0xFF;
                        if (v2 < min3) min3 = v2;
                        if (v2 > max3) max3 = v2;
                    }

                    int delta = max3 - min3;
                    if (l <= edgeLimit && min3 <= DARK_CORE_REQ && delta >= DELTA_REQ) {
                        isBlack = true;
                    } else {
                        int blackness = 255 - l;
                        isBlack = (blackness > bayerRow[x & 7]);
                    }
                }

                outPixels[rowOffset + x] = isBlack ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height);
        return output;
    }

    /**
     * Converts a page bitmap directly into a 1-bit bitonal CUPS_CSPACE_K raster stream.
     * Uses streaming multi-row buffers to ensure near-zero memory footprint and ultra-fast throughput.
     */
    public static void streamBitmapTo1BitRaster(Bitmap bitmap, int darkness, OutputStream rasterOut) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bytesPerLine = width / 8; // Guaranteed multiple of 32

        int coreLimit = getCoreBlackLimit(darkness);
        int edgeLimit = getEdgeLimit(darkness);

        // Circular 3-row luma buffers
        int[] lumaPrev = new int[width];
        int[] lumaCurr = new int[width];
        int[] lumaNext = new int[width];

        // Batch buffer for reading from bitmap
        int batchSize = 64;
        int[] batchPixels = new int[width * batchSize];
        byte[] lineBytes = new byte[bytesPerLine];

        // Helper ring for rotating
        int[][] ring = new int[][] { lumaPrev, lumaCurr, lumaNext };

        // Preload row 0 into lumaCurr
        readRowLuma(bitmap, 0, width, batchPixels, ring[1]);
        // Duplicate row 0 into lumaPrev for y=0 boundary
        System.arraycopy(ring[1], 0, ring[0], 0, width);

        int batchStartRow = -1;

        for (int y = 0; y < height; y++) {
            // Load row y + 1 into ring[2] (lumaNext)
            if (y + 1 < height) {
                int nextY = y + 1;
                if (batchStartRow < 0 || nextY < batchStartRow || nextY >= batchStartRow + batchSize) {
                    batchStartRow = nextY;
                    int count = Math.min(batchSize, height - batchStartRow);
                    bitmap.getPixels(batchPixels, 0, width, 0, batchStartRow, width, count);
                }
                int offsetInBatch = (nextY - batchStartRow) * width;
                for (int x = 0; x < width; x++) {
                    int c = batchPixels[offsetInBatch + x];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    ring[2][x] = (r * 306 + g * 601 + b * 117) >> 10;
                }
            } else {
                // Bottom boundary: duplicate curr into next
                System.arraycopy(ring[1], 0, ring[2], 0, width);
            }

            int[] prev = ring[0];
            int[] curr = ring[1];
            int[] next = ring[2];
            int[] bayerRow = BAYER8[y & 7];

            int currentByte = 0;
            int bitCount = 0;
            int byteIndex = 0;

            for (int x = 0; x < width; x++) {
                int l = curr[x];
                boolean isBlack;

                if (l >= PAPER_CUTOFF) {
                    isBlack = false;
                } else if (l <= coreLimit) {
                    isBlack = true;
                } else {
                    int min3 = l;
                    int max3 = l;
                    int x0 = (x > 0) ? x - 1 : 0;
                    int x1 = (x < width - 1) ? x + 1 : width - 1;

                    for (int nx = x0; nx <= x1; nx++) {
                        int v0 = prev[nx];
                        if (v0 < min3) min3 = v0;
                        if (v0 > max3) max3 = v0;

                        int v1 = curr[nx];
                        if (v1 < min3) min3 = v1;
                        if (v1 > max3) max3 = v1;

                        int v2 = next[nx];
                        if (v2 < min3) min3 = v2;
                        if (v2 > max3) max3 = v2;
                    }

                    int delta = max3 - min3;
                    if (l <= edgeLimit && min3 <= DARK_CORE_REQ && delta >= DELTA_REQ) {
                        isBlack = true;
                    } else {
                        int blackness = 255 - l;
                        isBlack = (blackness > bayerRow[x & 7]);
                    }
                }

                currentByte = (currentByte << 1) | (isBlack ? 1 : 0);
                bitCount++;
                if (bitCount == 8) {
                    lineBytes[byteIndex++] = (byte) currentByte;
                    currentByte = 0;
                    bitCount = 0;
                }
            }

            rasterOut.write(lineBytes, 0, bytesPerLine);

            // Rotate ring buffers for next scanline
            int[] temp = ring[0];
            ring[0] = ring[1];
            ring[1] = ring[2];
            ring[2] = temp;
        }

        rasterOut.flush();
    }

    private static void readRowLuma(Bitmap bitmap, int y, int width, int[] rawBuf, int[] lumaBuf) {
        bitmap.getPixels(rawBuf, 0, width, 0, y, width, 1);
        for (int x = 0; x < width; x++) {
            int c = rawBuf[x];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            lumaBuf[x] = (r * 306 + g * 601 + b * 117) >> 10;
        }
    }

    /**
     * Inverts a dark-mode PDF/document so black backgrounds become pure white paper
     * and white text becomes deep black text, saving 80%+ toner.
     */
    public static Bitmap invertDarkDocument(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            pixels[i] = (0xFF << 24) | ((255 - r) << 16) | ((255 - g) << 8) | (255 - b);
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    /**
     * Applies a subtle watermark across a document page.
     */
    public static void applyWatermark(android.graphics.Canvas canvas, int w, int h, String watermarkText) {
        if (watermarkText == null || watermarkText.trim().isEmpty()) return;
        android.graphics.Paint wmPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        wmPaint.setColor(0x28000000); // 16% opacity light gray watermark
        wmPaint.setTextSize(Math.max(48f, w * 0.08f));
        wmPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        wmPaint.setFakeBoldText(true);

        canvas.save();
        canvas.rotate(-35f, w / 2f, h / 2f);
        canvas.drawText(watermarkText.toUpperCase(), w / 2f, h / 2f, wmPaint);
        canvas.restore();
    }

    /**
     * Checks if a page is effectively blank (>99% white/blank pixels).
     */
    public static boolean isPageBlank(Bitmap bm) {
        if (bm == null) return true;
        int w = bm.getWidth();
        int h = bm.getHeight();
        int nonWhite = 0;
        for (int y = 0; y < h; y += 12) {
            for (int x = 0; x < w; x += 12) {
                int c = bm.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                int lum = (r * 299 + g * 587 + b * 114) / 1000;
                if (lum < 240) {
                    nonWhite++;
                    if (nonWhite > 30) return false;
                }
            }
        }
        return nonWhite <= 30;
    }

    /**
     * Rotates a bitmap by a given angle (90, 180, 270 degrees).
     */
    public static Bitmap rotateBitmap(Bitmap src, float angle) {
        if (src == null || angle == 0) return src;
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
}
