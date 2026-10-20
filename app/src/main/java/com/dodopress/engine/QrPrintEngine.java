package com.dodopress.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import com.dodopress.engine.qr.QrCode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Universal High-Contrast 300 DPI QR Label & Sign Engine for DodoPress.
 * 100% offline, zero external APIs, zero dependencies.
 * Generates vector-sharp QR codes rendered directly to Android PdfDocument
 * for laser printing with micro-meter edge precision.
 */
public class QrPrintEngine {

    public enum LabelLayout {
        SINGLE_CARD("Single Placard / Sign (1 per page)", 1, 1),
        GRID_2X2("Quad Label Sheet (4 per page)", 2, 2),
        GRID_3X3("Sticker Sheet Medium (9 per page)", 3, 3),
        GRID_4X4("Compact Sticker Sheet (16 per page)", 4, 4);

        public final String title;
        public final int rows;
        public final int cols;

        LabelLayout(String title, int rows, int cols) {
            this.title = title;
            this.rows = rows;
            this.cols = cols;
        }
    }

    /**
     * Generates a high-contrast black/white Bitmap of the QR code with 4-module quiet zone.
     *
     * @param content Text / URL / Wi-Fi / UPI payload
     * @param sizePx Pixel width & height
     * @return Rendered Bitmap
     */
    public static Bitmap generateQrBitmap(String content, int sizePx) {
        return generateQrBitmap(content, sizePx, 4, QrCode.Ecc.MEDIUM);
    }

    /**
     * Generates a QR Bitmap with custom quiet zone and error correction level.
     */
    public static Bitmap generateQrBitmap(String content, int sizePx, int quietZoneModules, QrCode.Ecc ecc) {
        if (content == null || content.isEmpty()) {
            content = " ";
        }
        if (ecc == null) ecc = QrCode.Ecc.MEDIUM;
        QrCode qr = QrCode.encodeText(content, ecc);
        int qrSize = qr.size;
        int totalModules = qrSize + 2 * quietZoneModules;

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);

        Canvas canvas = new Canvas(bitmap);
        Paint blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        blackPaint.setStyle(Paint.Style.FILL);

        float moduleSize = (float) sizePx / totalModules;

        for (int y = 0; y < qrSize; y++) {
            for (int x = 0; x < qrSize; x++) {
                if (qr.getModule(x, y)) {
                    float left = (x + quietZoneModules) * moduleSize;
                    float top = (y + quietZoneModules) * moduleSize;
                    float right = (x + quietZoneModules + 1) * moduleSize;
                    float bottom = (y + quietZoneModules + 1) * moduleSize;
                    canvas.drawRect(left, top, right, bottom, blackPaint);
                }
            }
        }
        return bitmap;
    }

    /**
     * Formats a standard Wi-Fi configuration string compliant with ZXing / Android Wi-Fi QR spec.
     */
    public static String formatWifi(String ssid, String password, String security, boolean hidden) {
        String escSsid = escapeWifiString(ssid != null ? ssid : "");
        String escPass = escapeWifiString(password != null ? password : "");
        String sec = (security != null && !security.isEmpty()) ? security : (password == null || password.isEmpty() ? "nopass" : "WPA");

        StringBuilder sb = new StringBuilder("WIFI:");
        sb.append("T:").append(sec).append(";");
        sb.append("S:").append(escSsid).append(";");
        if (!"nopass".equalsIgnoreCase(sec) && !escPass.isEmpty()) {
            sb.append("P:").append(escPass).append(";");
        }
        if (hidden) {
            sb.append("H:true;");
        }
        sb.append(";");
        return sb.toString();
    }

    /**
     * Formats a standard UPI payment URI.
     */
    public static String formatUpi(String vpa, String payeeName, String amount, String note) {
        StringBuilder sb = new StringBuilder("upi://pay?");
        sb.append("pa=").append(Uri.encode(vpa != null ? vpa.trim() : ""));
        if (payeeName != null && !payeeName.trim().isEmpty()) {
            sb.append("&pn=").append(Uri.encode(payeeName.trim()));
        }
        if (amount != null && !amount.trim().isEmpty()) {
            sb.append("&am=").append(Uri.encode(amount.trim()));
            sb.append("&cu=INR");
        }
        if (note != null && !note.trim().isEmpty()) {
            sb.append("&tn=").append(Uri.encode(note.trim()));
        }
        return sb.toString();
    }

    private static String escapeWifiString(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace(":", "\\:")
                   .replace("\"", "\\\"");
    }

    /**
     * Renders a high-contrast vector QR label sheet directly onto an Android PdfDocument.
     * Uses pure vector rectangles for infinite sharpness at 300/600 DPI laser printing.
     */
    public static void renderQrSheet(PdfDocument doc, String content, String title, String subtitle,
                                     SheetGeometry.PaperSize paperSize, LabelLayout layout) {
        SheetGeometry.PaperSize targetPaper = (paperSize != null) ? paperSize : SheetGeometry.PaperSize.A4;
        LabelLayout targetLayout = (layout != null) ? layout : LabelLayout.SINGLE_CARD;

        int paperW = targetPaper.widthPts;
        int paperH = targetPaper.heightPts;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(paperW, paperH, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // White paper background
        canvas.drawColor(Color.WHITE);

        String payload = (content != null && !content.isEmpty()) ? content : " ";
        QrCode qr = QrCode.encodeText(payload, QrCode.Ecc.MEDIUM);

        Paint blackFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        blackFill.setColor(Color.BLACK);
        blackFill.setStyle(Paint.Style.FILL);

        Paint cutGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cutGuidePaint.setColor(0xFFBBBBBB);
        cutGuidePaint.setStyle(Paint.Style.STROKE);
        cutGuidePaint.setStrokeWidth(0.75f);
        cutGuidePaint.setPathEffect(new DashPathEffect(new float[]{4f, 4f}, 0f));

        Paint textTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTitlePaint.setColor(Color.BLACK);
        textTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textTitlePaint.setTextAlign(Paint.Align.CENTER);

        Paint textSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSubPaint.setColor(0xFF333333);
        textSubPaint.setTextAlign(Paint.Align.CENTER);

        Paint textSmallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSmallPaint.setColor(0xFF666666);
        textSmallPaint.setTextAlign(Paint.Align.CENTER);

        if (targetLayout == LabelLayout.SINGLE_CARD) {
            renderSinglePlacard(canvas, qr, payload, title, subtitle, paperW, paperH,
                               blackFill, textTitlePaint, textSubPaint, textSmallPaint);
        } else {
            renderGridLabels(canvas, qr, payload, title, subtitle, paperW, paperH,
                             targetLayout.rows, targetLayout.cols,
                             blackFill, cutGuidePaint, textTitlePaint, textSubPaint, textSmallPaint);
        }

        doc.finishPage(page);
    }

    private static void renderSinglePlacard(Canvas canvas, QrCode qr, String payload,
                                            String title, String subtitle, int paperW, int paperH,
                                            Paint blackFill, Paint textTitlePaint, Paint textSubPaint, Paint textSmallPaint) {
        float margin = 32f;
        RectF cardRect = new RectF(margin, margin, paperW - margin, paperH - margin);

        // Elegant card boundary with double hairline
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF222222);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(cardRect, 16f, 16f, borderPaint);

        borderPaint.setStrokeWidth(0.75f);
        RectF innerBorder = new RectF(margin + 5f, margin + 5f, paperW - margin - 5f, paperH - margin - 5f);
        canvas.drawRoundRect(innerBorder, 12f, 12f, borderPaint);

        // Header Title
        String displayTitle = (title != null && !title.isEmpty()) ? title : "SCAN QR CODE";
        textTitlePaint.setTextSize(26f);
        canvas.drawText(displayTitle, paperW / 2f, margin + 52f, textTitlePaint);

        // Subtitle / Instructions
        String displaySub = (subtitle != null && !subtitle.isEmpty()) ? subtitle : "Point phone camera or scanner to open";
        textSubPaint.setTextSize(13f);
        canvas.drawText(displaySub, paperW / 2f, margin + 76f, textSubPaint);

        // QR Code Size & Centering
        float qrBoxSize = Math.min(paperW - (margin * 2) - 80f, paperH - (margin * 2) - 220f);
        float qrLeft = (paperW - qrBoxSize) / 2f;
        float qrTop = margin + 110f + ((paperH - (margin * 2) - 220f - qrBoxSize) / 2f);

        drawVectorQr(canvas, qr, qrLeft, qrTop, qrBoxSize, blackFill);

        // Human-readable payload representation
        String snippet = payload;
        if (snippet.length() > 55) {
            snippet = snippet.substring(0, 52) + "...";
        }
        textSmallPaint.setTextSize(10f);
        textSmallPaint.setTypeface(Typeface.MONOSPACE);
        canvas.drawText(snippet, paperW / 2f, qrTop + qrBoxSize + 26f, textSmallPaint);

        // Footer Brand
        textSmallPaint.setTypeface(Typeface.DEFAULT);
        textSmallPaint.setTextSize(9f);
        textSmallPaint.setColor(0xFF888888);
        canvas.drawText("Generated with DodoPress • Offline Laser Print Engine", paperW / 2f, paperH - margin - 16f, textSmallPaint);
    }

    private static void renderGridLabels(Canvas canvas, QrCode qr, String payload,
                                         String title, String subtitle, int paperW, int paperH,
                                         int rows, int cols,
                                         Paint blackFill, Paint cutGuidePaint,
                                         Paint textTitlePaint, Paint textSubPaint, Paint textSmallPaint) {
        float margin = 20f;
        float availW = paperW - (2 * margin);
        float availH = paperH - (2 * margin);

        float cellW = availW / cols;
        float cellH = availH / rows;

        String displayTitle = (title != null && !title.isEmpty()) ? title : "SCAN ME";
        String displaySub = (subtitle != null && !subtitle.isEmpty()) ? subtitle : payload;
        if (displaySub.length() > 30) {
            displaySub = displaySub.substring(0, 27) + "...";
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cellLeft = margin + (c * cellW);
                float cellTop = margin + (r * cellH);
                float cellRight = cellLeft + cellW;
                float cellBottom = cellTop + cellH;

                // Cut dashed guide
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, cutGuidePaint);

                // Internal layout
                float pad = 8f;
                float titleH = (rows <= 2) ? 28f : ((rows == 3) ? 20f : 14f);
                float subH = (rows <= 2) ? 20f : ((rows == 3) ? 14f : 10f);

                float titleSize = (rows <= 2) ? 14f : ((rows == 3) ? 10f : 8f);
                float subSize = (rows <= 2) ? 9f : ((rows == 3) ? 8f : 6.5f);

                float qrAvailH = cellH - (pad * 2) - titleH - subH;
                float qrAvailW = cellW - (pad * 2);
                float qrBoxSize = Math.max(10f, Math.min(qrAvailW, qrAvailH));

                float qrLeft = cellLeft + (cellW - qrBoxSize) / 2f;
                float qrTop = cellTop + pad + titleH + (qrAvailH - qrBoxSize) / 2f;

                // Title
                textTitlePaint.setTextSize(titleSize);
                canvas.drawText(displayTitle, cellLeft + (cellW / 2f), cellTop + pad + (titleH * 0.7f), textTitlePaint);

                // Vector QR
                drawVectorQr(canvas, qr, qrLeft, qrTop, qrBoxSize, blackFill);

                // Subtitle
                textSubPaint.setTextSize(subSize);
                canvas.drawText(displaySub, cellLeft + (cellW / 2f), cellBottom - pad - 2f, textSubPaint);
            }
        }
    }

    private static void drawVectorQr(Canvas canvas, QrCode qr, float left, float top, float size, Paint blackFill) {
        int quietZone = 2; // In vector PDF cells, 2 modules provides ample scanning margin
        int totalModules = qr.size + (2 * quietZone);
        float moduleSize = size / totalModules;

        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    float ml = left + ((x + quietZone) * moduleSize);
                    float mt = top + ((y + quietZone) * moduleSize);
                    float mr = left + ((x + quietZone + 1) * moduleSize);
                    float mb = top + ((y + quietZone + 1) * moduleSize);
                    canvas.drawRect(ml, mt, mr, mb, blackFill);
                }
            }
        }
    }

    /**
     * Renders a QR label sheet into a temporary PDF file and returns the File.
     */
    public static File renderQrToPdfFile(Context context, String content, String title, String subtitle,
                                         SheetGeometry.PaperSize paperSize, LabelLayout layout) throws IOException {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        SheetGeometry.PaperSize targetPaper = (paperSize != null) ? paperSize : SheetGeometry.PaperSize.A4;
        PdfDocument doc = new PdfDocument();
        try {
            renderQrSheet(doc, content, title, subtitle, targetPaper, layout);
            File cacheDir = context.getCacheDir();
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File destPdf = new File(cacheDir, "qr_label_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(destPdf)) {
                doc.writeTo(fos);
            }
            return destPdf;
        } finally {
            doc.close();
        }
    }

    public static File renderQrToPdfFile(Context context, String content, String title, SheetGeometry.PaperSize paperSize) throws IOException {
        return renderQrToPdfFile(context, content, title, null, paperSize, LabelLayout.SINGLE_CARD);
    }

    public static File renderQrToPdfFile(Context context, String content, SheetGeometry.PaperSize paperSize) throws IOException {
        return renderQrToPdfFile(context, content, "SCAN QR CODE", null, paperSize, LabelLayout.SINGLE_CARD);
    }

    public static File renderQrToPdfFile(Context context, String content) throws IOException {
        return renderQrToPdfFile(context, content, "SCAN QR CODE", null, SheetGeometry.PaperSize.A4, LabelLayout.SINGLE_CARD);
    }
}
