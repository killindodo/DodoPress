package com.dodopress.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Universal Image Ingestion Engine for DodoPress.
 * Converts any camera photo or gallery image (JPEG, PNG, WebP, HEIF, BMP)
 * into a high-fidelity standard PDF document ready for laser rasterization.
 * 100% offline, zero external APIs, zero dependencies.
 */
public class ImagePrintEngine {

    private static final int MAX_DECODE_DIMENSION = 4096;
    private static final int DEFAULT_MARGIN_PTS = 20; // ~7mm standard laser printer margin

    /**
     * Converts an image from a content/file URI into an Android PdfDocument on the specified paper size.
     * Automatically handles EXIF rotation, uniform aspect ratio fit (FIT_CENTER), and caches the PDF.
     *
     * @param context Application context
     * @param imageUri URI of the image to convert
     * @param paperSize Target paper size (A4, LETTER, LEGAL, A5)
     * @return Generated PDF File located in context.getCacheDir()
     * @throws IOException If image loading or PDF writing fails
     */
    public static File convertImageToPdf(Context context, Uri imageUri, SheetGeometry.PaperSize paperSize) throws IOException {
        if (context == null || imageUri == null) {
            throw new IllegalArgumentException("Context and imageUri cannot be null");
        }

        SheetGeometry.PaperSize targetPaper = (paperSize != null) ? paperSize : SheetGeometry.PaperSize.A4;

        // 1. Read EXIF Orientation
        int exifOrientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Throwable ignored) {
            // Some images (e.g. PNG/WebP) do not have EXIF data; continue gracefully
        }

        // 2. Decode Image Dimensions to prevent Out-Of-Memory on massive camera photos
        BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
            if (in == null) {
                throw new IOException("Cannot open input stream for image URI: " + imageUri);
            }
            BitmapFactory.decodeStream(in, null, boundsOpts);
        }

        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
            throw new IOException("Failed to decode image dimensions from URI: " + imageUri);
        }

        int maxDim = Math.max(boundsOpts.outWidth, boundsOpts.outHeight);
        int sampleSize = 1;
        while (maxDim / sampleSize > MAX_DECODE_DIMENSION) {
            sampleSize *= 2;
        }

        // 3. Decode Full Bitmap with optimal inSampleSize
        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
        decodeOpts.inSampleSize = sampleSize;
        decodeOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap rawBitmap;
        try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
            if (in == null) {
                throw new IOException("Cannot open input stream for image URI: " + imageUri);
            }
            rawBitmap = BitmapFactory.decodeStream(in, null, decodeOpts);
        }

        if (rawBitmap == null) {
            throw new IOException("BitmapFactory returned null for URI: " + imageUri);
        }

        Bitmap orientedBitmap = null;
        try {
            // 4. Correct EXIF Orientation (Rotate / Flip)
            orientedBitmap = applyExifOrientation(rawBitmap, exifOrientation);

            // 5. Render onto Android PdfDocument
            int paperW = targetPaper.widthPts;
            int paperH = targetPaper.heightPts;

            PdfDocument doc = new PdfDocument();
            try {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(paperW, paperH, 1).create();
                PdfDocument.Page page = doc.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Crisp pure white paper background
                canvas.drawColor(Color.WHITE);

                // Calculate Uniform Aspect Fit inside printable margins
                Rect pageBox = new Rect(DEFAULT_MARGIN_PTS, DEFAULT_MARGIN_PTS, paperW - DEFAULT_MARGIN_PTS, paperH - DEFAULT_MARGIN_PTS);
                Rect destRect = SheetGeometry.calculateAspectFit(orientedBitmap.getWidth(), orientedBitmap.getHeight(), pageBox, 0);

                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
                canvas.drawBitmap(orientedBitmap, null, destRect, paint);

                doc.finishPage(page);

                // 6. Write to temporary cache file
                File cacheDir = context.getCacheDir();
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                File destPdf = new File(cacheDir, "img_converted_" + System.currentTimeMillis() + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(destPdf)) {
                    doc.writeTo(fos);
                }

                return destPdf;
            } finally {
                doc.close();
            }
        } finally {
            if (rawBitmap != null && !rawBitmap.isRecycled() && rawBitmap != orientedBitmap) {
                rawBitmap.recycle();
            }
            if (orientedBitmap != null && !orientedBitmap.isRecycled()) {
                orientedBitmap.recycle();
            }
        }
    }

    /**
     * Convenience overload converting an image to A4 PDF.
     */
    public static File convertImageToPdf(Context context, Uri imageUri) throws IOException {
        return convertImageToPdf(context, imageUri, SheetGeometry.PaperSize.A4);
    }

    /**
     * Converts an existing Bitmap directly into a standard PDF file.
     */
    public static File convertBitmapToPdf(Context context, Bitmap bitmap, SheetGeometry.PaperSize paperSize) throws IOException {
        if (context == null || bitmap == null || bitmap.isRecycled()) {
            throw new IllegalArgumentException("Context and valid bitmap are required");
        }

        SheetGeometry.PaperSize targetPaper = (paperSize != null) ? paperSize : SheetGeometry.PaperSize.A4;
        int paperW = targetPaper.widthPts;
        int paperH = targetPaper.heightPts;

        PdfDocument doc = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(paperW, paperH, 1).create();
            PdfDocument.Page page = doc.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.drawColor(Color.WHITE);

            Rect pageBox = new Rect(DEFAULT_MARGIN_PTS, DEFAULT_MARGIN_PTS, paperW - DEFAULT_MARGIN_PTS, paperH - DEFAULT_MARGIN_PTS);
            Rect destRect = SheetGeometry.calculateAspectFit(bitmap.getWidth(), bitmap.getHeight(), pageBox, 0);

            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            canvas.drawBitmap(bitmap, null, destRect, paint);

            doc.finishPage(page);

            File destPdf = new File(context.getCacheDir(), "bm_converted_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(destPdf)) {
                doc.writeTo(fos);
            }
            return destPdf;
        } finally {
            doc.close();
        }
    }

    /**
     * Corrects orientation by applying EXIF orientation matrix transformations.
     */
    public static Bitmap applyExifOrientation(Bitmap bitmap, int orientation) {
        if (bitmap == null || orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Throwable t) {
            return bitmap;
        }
    }
}
