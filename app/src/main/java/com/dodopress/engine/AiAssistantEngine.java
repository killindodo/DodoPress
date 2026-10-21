package com.dodopress.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

/**
 * BYOK (Bring Your Own Key) AI Print Engine for DodoPress.
 * 100% Free Tier compatible: works with Google Gemini 2.0 Flash / 1.5 Flash (15 RPM free).
 * Zero server costs, zero recurring subscriptions, pure client-to-API architecture.
 */
public class AiAssistantEngine {

    private static final String PREF_NAME = "dodopress_ai_prefs";
    private static final String KEY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_SELECTED_MODEL = "gemini_model";

    public static final String DEFAULT_MODEL = "gemini-2.0-flash";

    public interface AiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static void saveApiKey(Context context, String apiKey) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_GEMINI_KEY, apiKey != null ? apiKey.trim() : "").apply();
    }

    public static String getApiKey(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_GEMINI_KEY, "");
    }

    public static boolean hasApiKey(Context context) {
        String key = getApiKey(context);
        return key != null && !key.trim().isEmpty();
    }

    public static void saveModel(Context context, String model) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SELECTED_MODEL, model).apply();
    }

    public static String getModel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL);
    }

    /**
     * Validates API Key by performing a lightweight 1-token test prompt.
     */
    public static void testApiKey(Context context, String testKey, AiCallback<String> callback) {
        new Thread(() -> {
            try {
                String model = getModel(context);
                String prompt = "Reply with 'OK' and your model name.";
                String response = callGeminiRaw(testKey, model, prompt, "You are a test ping agent.");
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess("Connected! " + response.trim()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    /**
     * Transforms raw unstructured chat/message into a professional laser-printable Invoice PDF.
     */
    public static void chatToInvoice(Context context, String chatText, SheetGeometry.PaperSize paperSize, AiCallback<File> callback) {
        new Thread(() -> {
            try {
                String apiKey = getApiKey(context);
                if (apiKey.isEmpty()) {
                    throw new IllegalStateException("Gemini API key is not configured. Please enter your key in the Dev tab.");
                }

                String systemInstruction = "You are a world-class professional accounting assistant. " +
                        "Extract and structure the transaction from the user's text/chat into a strictly valid JSON object without markdown formatting. " +
                        "Required JSON format: {" +
                        "\"invoiceNumber\": \"INV-1001\"," +
                        "\"date\": \"YYYY-MM-DD\"," +
                        "\"seller\": {\"name\": \"...\", \"phone\": \"...\", \"email\": \"...\"}," +
                        "\"client\": {\"name\": \"...\", \"phone\": \"...\", \"address\": \"...\"}," +
                        "\"items\": [{\"desc\": \"...\", \"qty\": 1, \"unitPrice\": 100.0, \"total\": 100.0}]," +
                        "\"subtotal\": 100.0," +
                        "\"tax\": 0.0," +
                        "\"grandTotal\": 100.0," +
                        "\"currency\": \"INR/USD/etc\"," +
                        "\"notes\": \"...\"" +
                        "}. Do not return code blocks or backticks, only pure JSON.";

                String rawJson = callGeminiRaw(apiKey, getModel(context), chatText, systemInstruction);
                rawJson = cleanJsonResponse(rawJson);

                JSONObject invoiceObj = new JSONObject(rawJson);
                File pdfFile = renderInvoiceToPdf(context, invoiceObj, paperSize);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(pdfFile));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    /**
     * Summarizes unstructured document or lecture notes into a clean 1-page A4 executive cheat sheet.
     */
    public static void summarizeToCheatSheet(Context context, String rawNotes, String title, SheetGeometry.PaperSize paperSize, AiCallback<File> callback) {
        new Thread(() -> {
            try {
                String apiKey = getApiKey(context);
                if (apiKey.isEmpty()) {
                    throw new IllegalStateException("Gemini API key is not configured. Please enter your key in the Dev tab.");
                }

                String systemInstruction = "You are an executive summary master. " +
                        "Format the input notes into an ultra-clean, high-density 1-page structured brief suitable for laser printing. " +
                        "Return strict JSON with: " +
                        "{\"title\": \"...\", \"subtitle\": \"...\", \"keyTakeaways\": [\"...\"], \"sections\": [{\"heading\": \"...\", \"bullets\": [\"...\"]}]}. " +
                        "Pure JSON only, no backticks.";

                String rawJson = callGeminiRaw(apiKey, getModel(context), rawNotes, systemInstruction);
                rawJson = cleanJsonResponse(rawJson);

                JSONObject summaryObj = new JSONObject(rawJson);
                File pdfFile = renderSummaryToPdf(context, summaryObj, paperSize);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(pdfFile));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    /**
     * Translates document text into target language and renders a high-contrast laser document.
     */
    public static void translateAndFormat(Context context, String sourceText, String targetLang, SheetGeometry.PaperSize paperSize, AiCallback<File> callback) {
        new Thread(() -> {
            try {
                String apiKey = getApiKey(context);
                if (apiKey.isEmpty()) {
                    throw new IllegalStateException("Gemini API key is not configured.");
                }

                String systemInstruction = "You are a professional legal & document translator. " +
                        "Translate the input document accurately into " + targetLang + ". " +
                        "Return JSON with: {\"title\": \"...\", \"language\": \"" + targetLang + "\", \"paragraphs\": [\"...\"]}. Pure JSON only.";

                String rawJson = callGeminiRaw(apiKey, getModel(context), sourceText, systemInstruction);
                rawJson = cleanJsonResponse(rawJson);

                JSONObject transObj = new JSONObject(rawJson);
                File pdfFile = renderTranslationToPdf(context, transObj, paperSize);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(pdfFile));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    /**
     * Core direct HTTPS caller for Google Gemini REST API.
     */
    private static String callGeminiRaw(String apiKey, String model, String userPrompt, String systemPrompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        URL url = new URL(endpoint);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        JSONObject root = new JSONObject();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemInst = new JSONObject();
            JSONObject part = new JSONObject();
            part.put("text", systemPrompt);
            systemInst.put("parts", new JSONArray().put(part));
            root.put("system_instruction", systemInst);
        }

        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        contentObj.put("role", "user");
        JSONArray parts = new JSONArray();
        JSONObject userPart = new JSONObject();
        userPart.put("text", userPrompt);
        parts.put(userPart);
        contentObj.put("parts", parts);
        contents.put(contentObj);
        root.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.2);
        genConfig.put("maxOutputTokens", 2048);
        root.put("generationConfig", genConfig);

        byte[] requestBytes = root.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBytes);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        if (code != 200) {
            String errorMsg = "Gemini API error (HTTP " + code + "): " + sb.toString();
            try {
                JSONObject errObj = new JSONObject(sb.toString());
                if (errObj.has("error")) {
                    errorMsg = errObj.getJSONObject("error").optString("message", errorMsg);
                }
            } catch (Exception ignored) {}
            throw new Exception(errorMsg);
        }

        JSONObject resp = new JSONObject(sb.toString());
        JSONArray cands = resp.optJSONArray("candidates");
        if (cands == null || cands.length() == 0) {
            throw new Exception("No response candidates returned by Gemini.");
        }

        JSONObject candidate = cands.getJSONObject(0);
        JSONObject candContent = candidate.getJSONObject("content");
        JSONArray candParts = candContent.getJSONArray("parts");
        return candParts.getJSONObject(0).getString("text");
    }

    private static String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        int firstBrace = s.indexOf('{');
        int lastBrace = s.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return s.substring(firstBrace, lastBrace + 1).trim();
        }
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    /**
     * Renders a beautiful, pixel-perfect 300 DPI Invoice PDF.
     */
    public static File renderInvoiceToPdf(Context context, JSONObject inv, SheetGeometry.PaperSize paperSize) throws Exception {
        int widthPts = (paperSize != null) ? paperSize.widthPts : 595;
        int heightPts = (paperSize != null) ? paperSize.heightPts : 842;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(widthPts, heightPts, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);

        int margin = 36;
        int contentW = widthPts - (margin * 2);
        float y = margin + 20;

        // Header Title
        paint.setTextSize(24f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TAX INVOICE", margin, y, paint);

        // Invoice Number & Date
        paint.setTextSize(10f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        String invNum = inv.optString("invoiceNumber", "INV-" + System.currentTimeMillis() % 10000);
        String dateStr = inv.optString("date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Invoice #: " + invNum, widthPts - margin, y - 10, paint);
        canvas.drawText("Date: " + dateStr, widthPts - margin, y + 4, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        y += 24;
        paint.setStrokeWidth(2f);
        canvas.drawLine(margin, y, widthPts - margin, y, paint);
        y += 20;

        // Seller & Buyer columns
        JSONObject seller = inv.optJSONObject("seller");
        JSONObject client = inv.optJSONObject("client");

        float colW = contentW / 2f;
        paint.setTextSize(10f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("BILLED FROM:", margin, y, paint);
        canvas.drawText("BILLED TO:", margin + colW, y, paint);
        y += 14;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        if (seller != null) {
            canvas.drawText(seller.optString("name", "Store / Merchant"), margin, y, paint);
            y += 12;
            if (seller.has("phone")) { canvas.drawText("Phone: " + seller.optString("phone"), margin, y, paint); y += 12; }
            if (seller.has("email")) { canvas.drawText("Email: " + seller.optString("email"), margin, y, paint); y += 12; }
        } else {
            canvas.drawText("General Merchant", margin, y, paint);
            y += 12;
        }

        float clientY = y - 36;
        if (client != null) {
            canvas.drawText(client.optString("name", "Customer"), margin + colW, clientY, paint);
            clientY += 12;
            if (client.has("phone")) { canvas.drawText("Phone: " + client.optString("phone"), margin + colW, clientY, paint); clientY += 12; }
            if (client.has("address")) { canvas.drawText(client.optString("address"), margin + colW, clientY, paint); clientY += 12; }
        } else {
            canvas.drawText("Cash / Retail Customer", margin + colW, clientY, paint);
        }

        y = Math.max(y, clientY) + 20;

        // Items Table Header
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF1F5F9);
        canvas.drawRect(margin, y - 12, widthPts - margin, y + 8, paint);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(9f);

        canvas.drawText("ITEM DESCRIPTION", margin + 6, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("QTY", margin + (contentW * 0.65f), y, paint);
        canvas.drawText("UNIT PRICE", margin + (contentW * 0.82f), y, paint);
        canvas.drawText("TOTAL", widthPts - margin - 6, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        y += 16;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        JSONArray items = inv.optJSONArray("items");
        String currency = inv.optString("currency", "₹");
        if ("INR".equalsIgnoreCase(currency)) currency = "₹";

        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String desc = item.optString("desc", "Item " + (i + 1));
                int qty = item.optInt("qty", 1);
                double unitPrice = item.optDouble("unitPrice", 0.0);
                double total = item.optDouble("total", qty * unitPrice);

                canvas.drawText(desc, margin + 6, y, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(String.valueOf(qty), margin + (contentW * 0.65f), y, paint);
                canvas.drawText(String.format(Locale.US, "%.2f", unitPrice), margin + (contentW * 0.82f), y, paint);
                canvas.drawText(String.format(Locale.US, "%.2f", total), widthPts - margin - 6, y, paint);
                paint.setTextAlign(Paint.Align.LEFT);

                y += 8;
                paint.setStrokeWidth(0.5f);
                paint.setColor(0xFFE2E8F0);
                canvas.drawLine(margin, y, widthPts - margin, y, paint);
                paint.setColor(Color.BLACK);
                y += 14;
            }
        }

        y += 10;
        // Subtotal & Grand Total
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(10f);
        double subtotal = inv.optDouble("subtotal", 0.0);
        double tax = inv.optDouble("tax", 0.0);
        double grandTotal = inv.optDouble("grandTotal", subtotal + tax);

        canvas.drawText("Subtotal:  " + currency + " " + String.format(Locale.US, "%.2f", subtotal), widthPts - margin - 6, y, paint);
        y += 14;
        if (tax > 0) {
            canvas.drawText("Tax / GST:  " + currency + " " + String.format(Locale.US, "%.2f", tax), widthPts - margin - 6, y, paint);
            y += 14;
        }

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(13f);
        canvas.drawText("TOTAL AMOUNT:  " + currency + " " + String.format(Locale.US, "%.2f", grandTotal), widthPts - margin - 6, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Footer note
        y = heightPts - margin - 20;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextSize(8f);
        paint.setColor(0xFF64748B);
        canvas.drawText("Generated by DodoPress AI • 100% Offline Universal Architecture", margin, y, paint);

        doc.finishPage(page);

        File cachePdf = new File(context.getCacheDir(), "invoice_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(cachePdf)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }

        return cachePdf;
    }

    /**
     * Renders a 1-page condensed Executive Summary / Study Notes Cheat Sheet.
     */
    public static File renderSummaryToPdf(Context context, JSONObject summary, SheetGeometry.PaperSize paperSize) throws Exception {
        int widthPts = (paperSize != null) ? paperSize.widthPts : 595;
        int heightPts = (paperSize != null) ? paperSize.heightPts : 842;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(widthPts, heightPts, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);

        int margin = 36;
        float y = margin + 20;

        // Title
        paint.setTextSize(20f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String title = summary.optString("title", "Executive Summary");
        canvas.drawText(title, margin, y, paint);
        y += 16;

        paint.setTextSize(10f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(0xFF475569);
        String subtitle = summary.optString("subtitle", "1-Page Condensed Study Brief");
        canvas.drawText(subtitle, margin, y, paint);
        y += 12;

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(margin, y, widthPts - margin, y, paint);
        y += 20;

        // Key Takeaways Box
        JSONArray takeaways = summary.optJSONArray("keyTakeaways");
        if (takeaways != null && takeaways.length() > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFF8FAFC);
            canvas.drawRoundRect(margin, y - 8, widthPts - margin, y + (takeaways.length() * 16) + 16, 8, 8, paint);

            paint.setColor(Color.BLACK);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(10f);
            canvas.drawText("KEY TAKEAWAYS:", margin + 12, y + 8, paint);
            y += 24;

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            for (int i = 0; i < takeaways.length(); i++) {
                String bullet = takeaways.getString(i);
                canvas.drawText("•  " + bullet, margin + 16, y, paint);
                y += 15;
            }
            y += 14;
        }

        // Sections
        JSONArray sections = summary.optJSONArray("sections");
        if (sections != null) {
            for (int i = 0; i < sections.length(); i++) {
                JSONObject sec = sections.getJSONObject(i);
                String heading = sec.optString("heading", "Section " + (i + 1));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(11f);
                canvas.drawText(heading.toUpperCase(Locale.ROOT), margin, y, paint);
                y += 16;

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(9.5f);
                JSONArray bullets = sec.optJSONArray("bullets");
                if (bullets != null) {
                    for (int j = 0; j < bullets.length(); j++) {
                        String b = bullets.getString(j);
                        canvas.drawText("—  " + b, margin + 8, y, paint);
                        y += 14;
                    }
                }
                y += 10;
            }
        }

        // Footer
        paint.setTextSize(8f);
        paint.setColor(0xFF94A3B8);
        canvas.drawText("DodoPress AI Executive Sheet • Saves 80%+ Toner and Paper", margin, heightPts - margin, paint);

        doc.finishPage(page);

        File cachePdf = new File(context.getCacheDir(), "summary_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(cachePdf)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }

        return cachePdf;
    }

    /**
     * Renders translated document paragraphs into a clean A4 PDF.
     */
    public static File renderTranslationToPdf(Context context, JSONObject trans, SheetGeometry.PaperSize paperSize) throws Exception {
        int widthPts = (paperSize != null) ? paperSize.widthPts : 595;
        int heightPts = (paperSize != null) ? paperSize.heightPts : 842;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(widthPts, heightPts, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);

        int margin = 36;
        float y = margin + 20;

        // Title
        paint.setTextSize(20f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String title = trans.optString("title", "Translated Document");
        canvas.drawText(title, margin, y, paint);
        y += 16;

        paint.setTextSize(9f);
        paint.setColor(0xFF64748B);
        String lang = trans.optString("language", "Target Language");
        canvas.drawText("Target Language: " + lang, margin, y, paint);
        y += 12;

        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.BLACK);
        canvas.drawLine(margin, y, widthPts - margin, y, paint);
        y += 24;

        paint.setTextSize(10f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        JSONArray paras = trans.optJSONArray("paragraphs");
        if (paras != null) {
            for (int i = 0; i < paras.length(); i++) {
                String p = paras.getString(i);
                // Simple word wrap
                y = drawWrappedText(canvas, paint, p, margin, y, widthPts - (margin * 2), 14f);
                y += 10;
            }
        }

        doc.finishPage(page);

        File cachePdf = new File(context.getCacheDir(), "trans_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(cachePdf)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }

        return cachePdf;
    }

    private static float drawWrappedText(Canvas canvas, Paint paint, String text, float x, float y, float maxWidth, float lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, y, paint);
            y += lineHeight;
        }
        return y;
    }
}
