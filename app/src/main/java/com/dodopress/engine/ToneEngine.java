package com.dodopress.engine;

import android.graphics.Bitmap;
import java.io.IOException;
import java.io.OutputStream;

public class ToneEngine {

    public static final int PRESET_DRAFT = 25;
    public static final int PRESET_NORMAL = 50;
    public static final int PRESET_DEEP = 75;
    public static final int PRESET_ULTRA = 100;

    /**
     * Builds a 256-entry lookup table for grayscale tone mapping.
     * darkness: 0 (lightest/draft) to 100 (maximum deep black).
     */
    public static int[] buildLut(int darkness) {
        int[] lut = new int[256];
        double gamma;
        int threshold;

        if (darkness <= 50) {
            // 0 -> gamma 0.6, 50 -> gamma 1.0
            gamma = 0.6 + (darkness / 50.0) * 0.4;
            threshold = 0;
        } else {
            // 50 -> gamma 1.0, 100 -> gamma 2.8
            double factor = (darkness - 50) / 50.0;
            gamma = 1.0 + factor * 1.8;
            threshold = (int) (factor * 140); // threshold anti-aliasing edges
        }

        for (int i = 0; i < 256; i++) {
            if (i <= threshold) {
                lut[i] = 0; // Solid black
            } else {
                double normalized = (double) i / 255.0;
                double curved = Math.pow(normalized, gamma);
                int val = (int) Math.round(curved * 255.0);
                lut[i] = Math.min(255, Math.max(0, val));
            }
        }
        return lut;
    }

    /**
     * Applies tone curve to preview bitmap quickly.
     */
    public static Bitmap processPreviewBitmap(Bitmap src, int darkness) {
        if (src == null || src.isRecycled()) return null;

        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] lut = buildLut(darkness);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int a = (c >>> 24);
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            int luma = (r * 306 + g * 601 + b * 117) >> 10;
            int adjusted = lut[luma];

            pixels[i] = (a << 24) | (adjusted << 16) | (adjusted << 8) | adjusted;
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height);
        return output;
    }

    /**
     * Converts a page bitmap to RGB CUPS raster lines, applying tone curve on the fly.
     * Uses vertical strip batching (64 rows per batch) for fast processing and minimal JNI overhead.
     */
    public static void streamBitmapToRaster(Bitmap bitmap, int darkness, OutputStream rasterOut) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] lut = buildLut(darkness);

        int batchRows = 64;
        int[] blockPixels = new int[width * batchRows];
        byte[] rgbBytes = new byte[width * 3 * batchRows];

        for (int y = 0; y < height; y += batchRows) {
            int currentBatch = Math.min(batchRows, height - y);
            bitmap.getPixels(blockPixels, 0, width, 0, y, width, currentBatch);
            int totalPixels = width * currentBatch;
            int byteIdx = 0;
            for (int i = 0; i < totalPixels; i++) {
                int c = blockPixels[i];
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;

                int luma = (r * 306 + g * 601 + b * 117) >> 10;
                int val = lut[luma];

                rgbBytes[byteIdx++] = (byte) val; // R
                rgbBytes[byteIdx++] = (byte) val; // G
                rgbBytes[byteIdx++] = (byte) val; // B
            }
            rasterOut.write(rgbBytes, 0, byteIdx);
        }
        rasterOut.flush();
    }
}
