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
     * Builds a 256-entry lookup table for tone preview mapping.
     */
    public static int[] buildLut(int darkness) {
        int[] lut = new int[256];
        int threshold = 64 + (int) ((darkness / 100.0) * 166);
        for (int i = 0; i < 256; i++) {
            lut[i] = (i <= threshold) ? 0 : 255;
        }
        return lut;
    }

    /**
     * Applies tone curve to preview thumbnail bitmap.
     * Accurately reflects 1-bit laser toner binarization in real time.
     */
    public static Bitmap processPreviewBitmap(Bitmap src, int darkness) {
        if (src == null || src.isRecycled()) return null;

        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        int threshold = 64 + (int) ((darkness / 100.0) * 166);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int a = (c >>> 24);
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;

            int luma = (r * 306 + g * 601 + b * 117) >> 10;
            int val = (luma <= threshold) ? 0 : 255;

            pixels[i] = (a << 24) | (val << 16) | (val << 8) | val;
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height);
        return output;
    }

    /**
     * Converts a page bitmap directly into a 1-bit bitonal CUPS_CSPACE_K raster stream.
     * 
     * In CUPS_CSPACE_K:
     *   Bit = 1 -> Pure Black (100% full intensity toner laser dot).
     *   Bit = 0 -> Pure White (Blank paper, laser off).
     *
     * This BYPASSES hpcups internal RGB-to-halftone dithering matrix!
     * Because no halftoning is applied by hpcups, the text is printed JET BLACK,
     * solid, and sharp, exactly like a Windows desktop driver.
     *
     * Darkness control:
     *   darkness = 25  (Draft)  -> threshold = 105 (thin, toner-saving text)
     *   darkness = 50  (Normal) -> threshold = 147 (standard laser text)
     *   darkness = 75  (Deep)   -> threshold = 188 (thick, bold, rich dark text)
     *   darkness = 100 (Ultra)  -> threshold = 230 (maximum ink density, pins all anti-aliasing to 100% black)
     */
    public static void streamBitmapTo1BitRaster(Bitmap bitmap, int darkness, OutputStream rasterOut) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bytesPerLine = width / 8; // width is guaranteed multiple of 32

        int threshold = 64 + (int) ((darkness / 100.0) * 166);

        int batchRows = 64;
        int[] blockPixels = new int[width * batchRows];
        byte[] lineBytes = new byte[bytesPerLine * batchRows];

        for (int y = 0; y < height; y += batchRows) {
            int currentBatch = Math.min(batchRows, height - y);
            bitmap.getPixels(blockPixels, 0, width, 0, y, width, currentBatch);

            int lineByteOffset = 0;
            for (int row = 0; row < currentBatch; row++) {
                int rowPixelOffset = row * width;
                int currentByte = 0;
                int bitCount = 0;

                for (int col = 0; col < width; col++) {
                    int c = blockPixels[rowPixelOffset + col];
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    int luma = (r * 306 + g * 601 + b * 117) >> 10;

                    // 1-bit CUPS_CSPACE_K: 1 = Black, 0 = White
                    if (luma <= threshold) {
                        currentByte |= (1 << (7 - bitCount));
                    }

                    bitCount++;
                    if (bitCount == 8) {
                        lineBytes[lineByteOffset++] = (byte) currentByte;
                        currentByte = 0;
                        bitCount = 0;
                    }
                }
            }
            rasterOut.write(lineBytes, 0, lineByteOffset);
        }
        rasterOut.flush();
    }
}
