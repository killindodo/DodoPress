package com.dodopress.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import com.dodopress.cups.CupsRasterWriter;
import com.dodopress.usb.UsbPrinterConnection;
import com.dynamixsoftware.drv.DrvRuntime;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import android.util.Log;

public class PrintEngine {

    public interface PrintProgressListener {
        void onProgress(int currentPage, int totalPages, String statusMessage);
        void onComplete();
        void onError(Exception e);
    }

    private final Context context;
    private final UsbPrinterConnection usbConnection;

    public PrintEngine(Context context, UsbPrinterConnection usbConnection) {
        this.context = context.getApplicationContext();
        this.usbConnection = usbConnection;
    }

    public void printPdf(File pdfFile, int darkness, int dpi, int copies, int startPage, int endPage, PrintProgressListener listener) {
        List<Integer> pages = new ArrayList<>();
        int from = Math.max(0, startPage);
        int to = Math.max(from, endPage);
        for (int i = from; i <= to; i++) {
            pages.add(i);
        }
        printPdf(pdfFile, darkness, dpi, copies, true, pages, SheetGeometry.PaperSize.A4, SheetGeometry.PlacementRegion.FULL_PAGE, listener);
    }

    public void printPdf(File pdfFile, int darkness, int dpi, int copies, boolean collate, List<Integer> pageIndices, PrintProgressListener listener) {
        printPdf(pdfFile, darkness, dpi, copies, collate, pageIndices, SheetGeometry.PaperSize.A4, SheetGeometry.PlacementRegion.FULL_PAGE, listener);
    }

    public void printPdf(File pdfFile, int darkness, int dpi, int copies, boolean collate, List<Integer> pageIndices,
                         SheetGeometry.PaperSize paperSize, SheetGeometry.PlacementRegion placementRegion,
                         PrintProgressListener listener) {
        printPdf(pdfFile, darkness, dpi, copies, collate, pageIndices, paperSize, placementRegion, false, null, 0f, listener);
    }

    public void printPdf(File pdfFile, int darkness, int dpi, int copies, boolean collate, List<Integer> pageIndices,
                         SheetGeometry.PaperSize paperSize, SheetGeometry.PlacementRegion placementRegion,
                         boolean invertDark, String watermark, float rotation,
                         PrintProgressListener listener) {
        printPdf(pdfFile, darkness, dpi, copies, collate, pageIndices, paperSize, placementRegion, invertDark, watermark, rotation, 0, listener);
    }

    public void printPdf(File pdfFile, int darkness, int dpi, int copies, boolean collate, List<Integer> pageIndices,
                         SheetGeometry.PaperSize paperSize, SheetGeometry.PlacementRegion placementRegion,
                         boolean invertDark, String watermark, float rotation, int marginMm,
                         PrintProgressListener listener) {
        new Thread(() -> {
            ParcelFileDescriptor pfd = null;
            PdfRenderer renderer = null;
            DrvRuntime.libhplip drv = null;
            long procHandle = 0;
            ParcelFileDescriptor stdinPfd = null;
            ParcelFileDescriptor stdoutPfd = null;
            ParcelFileDescriptor stderrPfd = null;
            OutputStream filterIn = null;
            InputStream filterOut = null;
            InputStream filterErr = null;
            Thread pumpThread = null;
            Thread errThread = null;
            AtomicBoolean pumpError = new AtomicBoolean(false);

            try {
                if (listener != null) listener.onProgress(0, 0, "Initializing USB printer interface...");

                // 1. Open USB printer connection
                usbConnection.open();
                OutputStream usbOut = usbConnection.getOutputStream();

                // 2. Prepare PPD asset
                File ppdFile = preparePpdAsset();

                // 3. Locate or extract libhplip.so
                File libFile = prepareNativeLibrary();

                if (listener != null) listener.onProgress(0, 0, "Starting hardware rasterization pipeline...");

                // 4. Set up JNI pipes and execute filter
                drv = new DrvRuntime.libhplip();
                int[] pipes = new int[3];
                String paperArg = (paperSize != null) ? paperSize.name() : "A4";
                String[] args = new String[] {
                    libFile.getAbsolutePath(),
                    "1", // Job ID
                    "android", // User
                    "DodoPress", // Title
                    "1", // Copies per page header
                    "PageSize=" + paperArg + " OutputMode=Normal Resolution=" + dpi + "dpi"
                };
                // env array MUST be alternating (key, value) pairs for setenv(key, val, 1)
                String[] env = new String[] {
                    "PPD", ppdFile.getAbsolutePath(),
                    "TMPDIR", context.getCacheDir().getAbsolutePath(),
                    "HOME", context.getCacheDir().getAbsolutePath(),
                    "DEVICE_URI", "hp:/usb/HP_LaserJet_M1005",
                    "ANDROID_ROOT", "/system",
                    "ANDROID_DATA", "/data"
                };

                procHandle = drv.procExec(args, env, context.getCacheDir().getAbsolutePath(), pipes);
                if (procHandle == 0 || procHandle == -1) {
                    throw new IOException("Failed to start printer raster filter process (procHandle=" + procHandle + ")");
                }

                stdinPfd = ParcelFileDescriptor.adoptFd(pipes[0]);
                stdoutPfd = ParcelFileDescriptor.adoptFd(pipes[1]);
                stderrPfd = ParcelFileDescriptor.adoptFd(pipes[2]);

                filterIn = new BufferedOutputStream(new ParcelFileDescriptor.AutoCloseOutputStream(stdinPfd), 65536);
                filterOut = new ParcelFileDescriptor.AutoCloseInputStream(stdoutPfd);
                filterErr = new ParcelFileDescriptor.AutoCloseInputStream(stderrPfd);

                // Background stderr drainer to prevent pipe buffer stalling and log filter diagnostics
                final InputStream fErr = filterErr;
                errThread = new Thread(() -> {
                    byte[] buf = new byte[1024];
                    int r;
                    try {
                        while ((r = fErr.read(buf)) != -1) {
                            String msg = new String(buf, 0, r, java.nio.charset.StandardCharsets.UTF_8).trim();
                            if (!msg.isEmpty()) {
                                Log.d("DodoPress-hpcups", msg);
                            }
                        }
                    } catch (Exception ignored) {}
                });
                errThread.start();

                // 5. Start background worker shoveling filter stdout -> USB bulk OUT
                final InputStream fOut = filterOut;
                pumpThread = new Thread(() -> {
                    byte[] buffer = new byte[16384];
                    int read;
                    long totalBytes = 0;
                    try {
                        while ((read = fOut.read(buffer)) != -1) {
                            usbOut.write(buffer, 0, read);
                            totalBytes += read;
                        }
                        usbOut.flush();
                        Log.d("DodoPress-Pump", "Pump complete: " + totalBytes + " bytes transferred to USB printer.");
                    } catch (IOException e) {
                        Log.e("DodoPress-Pump", "Error pumping raster to USB: " + e.getMessage());
                        pumpError.set(true);
                    }
                });
                pumpThread.start();

                // 6. Write CUPS Raster File Header
                CupsRasterWriter.writeFileHeader(filterIn);
                filterIn.flush();

                // 7. Render PDF pages with PdfRenderer
                pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(pfd);
                int docTotalPages = renderer.getPageCount();

                List<Integer> validPages = new ArrayList<>();
                for (int p : pageIndices) {
                    if (p >= 0 && p < docTotalPages) {
                        validPages.add(p);
                    }
                }
                if (validPages.isEmpty()) {
                    throw new IOException("No valid pages selected for printing.");
                }

                int numCopies = Math.max(1, copies);
                int totalSheets = validPages.size() * numCopies;
                int currentSheet = 0;
                int[] metrics = new int[4];

                if (collate || numCopies == 1) {
                    // Collate: Full document set (Copy 1), then full document set (Copy 2)...
                    for (int c = 1; c <= numCopies; c++) {
                        for (int pageIdx : validPages) {
                            currentSheet++;
                            String status = "Printing sheet " + currentSheet + " of " + totalSheets
                                + " (Page " + (pageIdx + 1) + (numCopies > 1 ? ", Copy " + c : "") + ")...";
                            if (listener != null) {
                                listener.onProgress(currentSheet, totalSheets, status);
                            }
                            Bitmap sheetBitmap = createPageOrSheetBitmap(renderer, pageIdx, dpi, paperSize, placementRegion, invertDark, watermark, rotation, marginMm, metrics);
                            CupsRasterWriter.writePageHeader(filterIn, metrics[0], metrics[1], dpi, metrics[2], metrics[3]);
                            filterIn.flush();
                            ToneEngine.streamBitmapTo1BitRaster(sheetBitmap, darkness, filterIn);
                            sheetBitmap.recycle();
                            System.gc();
                        }
                    }
                } else {
                    // Uncollated: Page 1 (Copy 1, Copy 2...), then Page 2 (Copy 1, Copy 2...)...
                    for (int pageIdx : validPages) {
                        Bitmap sheetBitmap = createPageOrSheetBitmap(renderer, pageIdx, dpi, paperSize, placementRegion, invertDark, watermark, rotation, marginMm, metrics);

                        for (int c = 1; c <= numCopies; c++) {
                            currentSheet++;
                            String status = "Printing sheet " + currentSheet + " of " + totalSheets
                                + " (Page " + (pageIdx + 1) + (numCopies > 1 ? ", Copy " + c : "") + ")...";
                            if (listener != null) {
                                listener.onProgress(currentSheet, totalSheets, status);
                            }
                            CupsRasterWriter.writePageHeader(filterIn, metrics[0], metrics[1], dpi, metrics[2], metrics[3]);
                            filterIn.flush();
                            ToneEngine.streamBitmapTo1BitRaster(sheetBitmap, darkness, filterIn);
                        }
                        sheetBitmap.recycle();
                        System.gc();
                    }
                }

                // 8. Close stdin to signal EOF to the filter
                if (filterIn != null) {
                    try { filterIn.flush(); } catch (Exception ignored) {}
                    try { filterIn.close(); } catch (Exception ignored) {}
                    filterIn = null;
                }
                if (stdinPfd != null) {
                    try { stdinPfd.close(); } catch (Exception ignored) {}
                    stdinPfd = null;
                }

                // 9. Wait for pump thread and filter completion
                if (listener != null) listener.onProgress(totalSheets, totalSheets, "Flushing printer buffer...");

                pumpThread.join(35000);
                if (pumpThread.isAlive()) {
                    Log.w("DodoPress-Pump", "Pump thread timed out after 35s; terminating filter.");
                    if (drv != null && procHandle != 0) {
                        try { drv.procDestroy(procHandle); } catch (Exception ignored) {}
                    }
                    pumpThread.interrupt();
                }

                int exitCode = drv.procWait(procHandle);
                procHandle = 0;

                if (exitCode != 0) {
                    Log.w("DodoPress-hpcups", "Filter exited with non-zero code: " + exitCode);
                }

                if (pumpError.get()) {
                    throw new IOException("Data transmission interrupted over USB.");
                }

                if (listener != null) listener.onComplete();

            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            } finally {
                try { if (renderer != null) renderer.close(); } catch (Exception ignored) {}
                try { if (pfd != null) pfd.close(); } catch (Exception ignored) {}
                try { if (filterIn != null) filterIn.close(); } catch (Exception ignored) {}
                try { if (filterOut != null) filterOut.close(); } catch (Exception ignored) {}
                try { if (filterErr != null) filterErr.close(); } catch (Exception ignored) {}
                try { if (stdinPfd != null) stdinPfd.close(); } catch (Exception ignored) {}
                try { if (stdoutPfd != null) stdoutPfd.close(); } catch (Exception ignored) {}
                try { if (stderrPfd != null) stderrPfd.close(); } catch (Exception ignored) {}
                if (drv != null && procHandle != 0) {
                    try { drv.procDestroy(procHandle); } catch (Exception ignored) {}
                    procHandle = 0;
                }
                usbConnection.close();
            }
        }).start();
    }

    private Bitmap createPageOrSheetBitmap(PdfRenderer renderer, int pageIdx, int dpi,
                                           SheetGeometry.PaperSize paperSize, SheetGeometry.PlacementRegion region,
                                           boolean invertDark, String watermark, float rotation, int marginMm,
                                           int[] outMetrics) {
        PdfRenderer.Page page = renderer.openPage(pageIdx);
        int pagePtsW = page.getWidth();
        int pagePtsH = page.getHeight();

        int marginPx = (marginMm > 0) ? (int) Math.round((marginMm / 25.4) * dpi) : 0;
        Bitmap finalBitmap;

        if (paperSize == null || region == null || region == SheetGeometry.PlacementRegion.FULL_PAGE) {
            if (marginMm <= 0) {
                // Borderless edge-to-edge
                int targetW = (int) Math.round((pagePtsW / 72.0) * dpi);
                int pixelW = (targetW + 31) & ~31;
                int pixelH = ((int) Math.round((pagePtsH / 72.0) * dpi)) & ~1;

                outMetrics[0] = pixelW;
                outMetrics[1] = pixelH;
                outMetrics[2] = pagePtsW;
                outMetrics[3] = pagePtsH;

                Bitmap pageBitmap = Bitmap.createBitmap(pixelW, pixelH, Bitmap.Config.ARGB_8888);
                pageBitmap.eraseColor(0xFFFFFFFF);
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                page.close();
                finalBitmap = pageBitmap;
            } else {
                // Full Page with Custom Physical Margin
                SheetGeometry.PaperSize targetPaper = (paperSize != null) ? paperSize : SheetGeometry.PaperSize.A4;
                int sheetPixelW = targetPaper.getPixelWidth(dpi);
                int sheetPixelH = targetPaper.getPixelHeight(dpi);

                outMetrics[0] = sheetPixelW;
                outMetrics[1] = sheetPixelH;
                outMetrics[2] = targetPaper.widthPts;
                outMetrics[3] = targetPaper.heightPts;

                Bitmap sheetBitmap = Bitmap.createBitmap(sheetPixelW, sheetPixelH, Bitmap.Config.ARGB_8888);
                sheetBitmap.eraseColor(0xFFFFFFFF);

                Rect targetBox = new Rect(0, 0, sheetPixelW, sheetPixelH);
                Rect destRect = SheetGeometry.calculateAspectFit(pagePtsW, pagePtsH, targetBox, marginPx);

                Bitmap docBitmap = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                docBitmap.eraseColor(0xFFFFFFFF);
                page.render(docBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                page.close();

                Canvas canvas = new Canvas(sheetBitmap);
                canvas.drawBitmap(docBitmap, destRect.left, destRect.top, null);
                docBitmap.recycle();
                finalBitmap = sheetBitmap;
            }
        } else {
            int sheetPixelW = paperSize.getPixelWidth(dpi);
            int sheetPixelH = paperSize.getPixelHeight(dpi);
            int sheetPtsW = paperSize.widthPts;
            int sheetPtsH = paperSize.heightPts;

            outMetrics[0] = sheetPixelW;
            outMetrics[1] = sheetPixelH;
            outMetrics[2] = sheetPtsW;
            outMetrics[3] = sheetPtsH;

            Bitmap sheetBitmap = Bitmap.createBitmap(sheetPixelW, sheetPixelH, Bitmap.Config.ARGB_8888);
            sheetBitmap.eraseColor(0xFFFFFFFF);

            Rect targetBox = SheetGeometry.getTargetBox(region, sheetPixelW, sheetPixelH);
            int effectiveMargin = Math.max(marginPx, (int) (dpi * 0.08f));
            Rect destRect = SheetGeometry.calculateAspectFit(pagePtsW, pagePtsH, targetBox, effectiveMargin);

            Bitmap docBitmap = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
            docBitmap.eraseColor(0xFFFFFFFF);
            page.render(docBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            page.close();

            Canvas canvas = new Canvas(sheetBitmap);
            canvas.drawBitmap(docBitmap, destRect.left, destRect.top, null);
            docBitmap.recycle();

            finalBitmap = sheetBitmap;
        }

        // Apply rotation if needed
        if (rotation != 0) {
            Bitmap rotated = ToneEngine.rotateBitmap(finalBitmap, rotation);
            if (rotated != finalBitmap) {
                finalBitmap.recycle();
                finalBitmap = rotated;
                outMetrics[0] = finalBitmap.getWidth();
                outMetrics[1] = finalBitmap.getHeight();
            }
        }

        // Apply dark-background inversion (ink shield)
        if (invertDark) {
            Bitmap inverted = ToneEngine.invertDarkDocument(finalBitmap);
            if (inverted != finalBitmap) {
                finalBitmap.recycle();
                finalBitmap = inverted;
            }
        }

        // Apply watermark stamp if provided
        if (watermark != null && !watermark.trim().isEmpty()) {
            Canvas c = new Canvas(finalBitmap);
            ToneEngine.applyWatermark(c, outMetrics[0], outMetrics[1], watermark);
        }

        return finalBitmap;
    }

    private File prepareNativeLibrary() throws IOException {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        File libFile = new File(nativeDir, "libhplip.so");
        if (libFile.exists() && libFile.canRead()) {
            return libFile;
        }

        File internalLib = new File(context.getFilesDir(), "libhplip.so");
        if (!internalLib.exists() || internalLib.length() == 0) {
            try (InputStream in = context.getAssets().open("libhplip.so");
                 FileOutputStream out = new FileOutputStream(internalLib)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            } catch (Exception e) {
                if (libFile.exists()) {
                    return libFile;
                }
            }
            internalLib.setExecutable(true, false);
            internalLib.setReadable(true, false);
        }
        return internalLib.exists() ? internalLib : libFile;
    }

    private File preparePpdAsset() throws IOException {
        File ppdFile = new File(context.getFilesDir(), "hp-laserjet_m1005.ppd");
        if (!ppdFile.exists() || ppdFile.length() == 0) {
            try (InputStream in = context.getAssets().open("hp-laserjet_m1005.ppd");
                 FileOutputStream out = new FileOutputStream(ppdFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        }
        ppdFile.setReadable(true, false);
        return ppdFile;
    }
}
