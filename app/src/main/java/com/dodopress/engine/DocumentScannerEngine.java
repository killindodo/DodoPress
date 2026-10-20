package com.dodopress.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DocumentScannerEngine {

    /**
     * Enhances a camera photo of a physical document:
     * - Removes uneven lighting and shadows (background flattening)
     * - Sharpens text and line work
     * - Converts to clean, crisp high-contrast document bitmap suitable for laser printing
     */
    public static Bitmap enhanceDocument(Bitmap src) {
        if (src == null) return null;

        int width = src.getWidth();
        int height = src.getHeight();

        // 1. Convert to grayscale luminance array
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] luma = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            luma[i] = (r * 299 + g * 587 + b * 114) / 1000;
        }

        // 2. Block-based background illumination estimation (32x32 blocks)
        int blockSize = 32;
        int blocksX = (width + blockSize - 1) / blockSize;
        int blocksY = (height + blockSize - 1) / blockSize;
        int[] bgMap = new int[blocksX * blocksY];

        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                int startX = bx * blockSize;
                int endX = Math.min(startX + blockSize, width);
                int startY = by * blockSize;
                int endY = Math.min(startY + blockSize, height);

                // Find top 85th percentile brightness in this block to estimate paper background color
                int maxLuma = 0;
                int sum = 0;
                int count = 0;
                for (int y = startY; y < endY; y += 2) {
                    for (int x = startX; x < endX; x += 2) {
                        int v = luma[y * width + x];
                        sum += v;
                        if (v > maxLuma) maxLuma = v;
                        count++;
                    }
                }
                int avg = (count > 0) ? (sum / count) : 128;
                // Background estimate is weighted towards brighter pixels in block
                bgMap[by * blocksX + bx] = Math.max(avg, (avg + maxLuma) / 2);
            }
        }

        // 3. Normalize pixels against estimated local background
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] outPixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int by = Math.min(y / blockSize, blocksY - 1);
            int rowIdx = y * width;
            int bgRowIdx = by * blocksX;

            for (int x = 0; x < width; x++) {
                int bx = Math.min(x / blockSize, blocksX - 1);
                int localBg = Math.max(bgMap[bgRowIdx + bx], 60);

                int val = luma[rowIdx + x];

                // Scale value relative to background: normalized = val * 255 / localBg
                int norm = (val * 255) / localBg;

                // Non-linear contrast curve (S-curve): boost text contrast
                int enhanced;
                if (norm < 160) {
                    // Dark text gets pushed deeper toward 0
                    enhanced = Math.max(0, (norm * norm) / 190);
                } else if (norm > 215) {
                    // Paper background becomes pure white
                    enhanced = 255;
                } else {
                    // Transition zone
                    enhanced = Math.min(255, 160 + ((norm - 160) * 95) / 55);
                }

                outPixels[rowIdx + x] = 0xFF000000 | (enhanced << 16) | (enhanced << 8) | enhanced;
            }
        }

        out.setPixels(outPixels, 0, width, 0, 0, width, height);
        return out;
    }

    /**
     * Wraps an enhanced document bitmap into a standard A4 PDF document ready for printing.
     */
    public static File saveBitmapAsA4Pdf(Bitmap docBitmap, File destPdfFile) throws IOException {
        // Standard A4 in PDF points: 595 x 842 pt
        int a4PtsW = 595;
        int a4PtsH = 842;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(a4PtsW, a4PtsH, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        // Aspect fit inside 20pt margin
        int margin = 24;
        Rect availBounds = new Rect(margin, margin, a4PtsW - margin, a4PtsH - margin);
        Rect destRect = SheetGeometry.calculateAspectFit(docBitmap.getWidth(), docBitmap.getHeight(), availBounds, 0);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(docBitmap, null, destRect, paint);

        doc.finishPage(page);

        if (destPdfFile.getParentFile() != null && !destPdfFile.getParentFile().exists()) {
            destPdfFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(destPdfFile)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }

        return destPdfFile;
    }
}
