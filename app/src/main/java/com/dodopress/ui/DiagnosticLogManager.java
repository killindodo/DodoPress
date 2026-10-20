package com.dodopress.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Precision Hardware Diagnostics & Telemetry Manager for DodoPress Pro.
 * Provides real-time USB hardware register status and cutaway visualization.
 */
public class DiagnosticLogManager {

    private static final int MAX_LOGS = 150;
    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    static {
        log("SYSTEM", "DodoPress Pro 300 DPI Laser Engine Initialized.");
        log("DEVICE", "Target Printer: HP LaserJet M1005 MFP (USB 0x03F0:0x3B17)");
        log("RASTER", "Driver: Native libhplip.so (CUPS_CSPACE_K 1-bit Bitonal Raster Streamer)");
        log("PRIVACY", "Security: 100% Offline Local Pipeline (Zero External Telemetry)");
    }

    public static synchronized void log(String tag, String message) {
        String timestamp = sdf.format(new Date());
        String entry = "[" + timestamp + "] [" + tag + "] " + message;
        logs.add(entry);
        while (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }

    public static synchronized void logError(String tag, String message, Throwable t) {
        String err = message + (t != null ? " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")" : "");
        log("ERROR/" + tag, err);
    }

    public static synchronized String getAllLogsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DodoPress Pro Hardware Telemetry & Diagnostics ===\n");
        sb.append("Timestamp: ").append(new Date().toString()).append("\n");
        sb.append("Printer: HP LaserJet M1005 MFP (Vendor 0x03F0, Product 0x3B17)\n");
        sb.append("Pipeline: Native ARM64 libhplip.so 1-bit Halftone Rasterizer\n");
        sb.append("Resolution: 300 x 300 DPI Bitonal Laser Mode\n");
        sb.append("Architecture: Local USB OTG Host Direct Stream (Zero Cloud)\n");
        sb.append("------------------------------------------------------\n");
        for (String line : logs) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clearLogs() {
        logs.clear();
        log("SYSTEM", "Diagnostics log buffer reset by operator.");
    }

    public static void showDiagnosticDialog(Activity activity) {
        showDiagnosticDialog(activity, PrinterCutawayView.PrinterState.READY);
    }

    public static void showDiagnosticDialog(Activity activity, PrinterCutawayView.PrinterState printerState) {
        if (activity == null || activity.isFinishing()) return;

        ThemeManager.AppTheme currentTheme = ThemeManager.getTheme(activity);
        ThemeManager.ThemePalette palette = ThemeManager.ThemePalette.forTheme(currentTheme);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ScrollView outerScroll = new ScrollView(activity);
        outerScroll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(palette.bgColor);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        outerScroll.addView(root);

        // Header Pill
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, (int) (6 * activity.getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("⚡ HARDWARE TELEMETRY & CUTAWAY");
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvTitle.setTextColor(palette.brandPrimaryColor);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(tvTitle);

        TextView tvModel = new TextView(activity);
        tvModel.setText("HP M1005");
        tvModel.setTextSize(10);
        tvModel.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvModel.setTextColor(palette.accentColor);
        headerRow.addView(tvModel);
        root.addView(headerRow);

        // Subtitle / Hardware status
        TextView tvSub = new TextView(activity);
        tvSub.setText("Live USB 0x03F0:0x3B17 Status Registers • 300 DPI Native");
        tvSub.setTextSize(10);
        tvSub.setTypeface(Typeface.MONOSPACE);
        tvSub.setTextColor(palette.textMedColor);
        tvSub.setPadding(0, 0, 0, (int) (8 * activity.getResources().getDisplayMetrics().density));
        root.addView(tvSub);

        // Visual Cutaway View
        PrinterCutawayView cutaway = new PrinterCutawayView(activity);
        cutaway.setPrinterState(printerState != null ? printerState : PrinterCutawayView.PrinterState.OFFLINE);
        LinearLayout.LayoutParams cutawayParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (130 * activity.getResources().getDisplayMetrics().density)
        );
        cutawayParams.setMargins(0, 0, 0, (int) (10 * activity.getResources().getDisplayMetrics().density));
        cutaway.setLayoutParams(cutawayParams);
        root.addView(cutaway);

        // Monospace Log Console Box with Horizontal + Vertical Scroll
        ScrollView sv = new ScrollView(activity);
        LinearLayout.LayoutParams svParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (180 * activity.getResources().getDisplayMetrics().density)
        );
        sv.setLayoutParams(svParams);
        sv.setBackgroundColor(palette.cardBgColor);
        sv.setPadding(10, 10, 10, 10);

        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvLogs = new TextView(activity);
        tvLogs.setTypeface(Typeface.MONOSPACE);
        tvLogs.setTextSize(9.5f);
        tvLogs.setTextColor(palette.textHighColor);
        tvLogs.setText(getAllLogsText());
        hsv.addView(tvLogs);
        sv.addView(hsv);
        root.addView(sv);

        // Button Bar
        LinearLayout btnBar = new LinearLayout(activity);
        btnBar.setOrientation(LinearLayout.HORIZONTAL);
        btnBar.setGravity(Gravity.RIGHT);
        btnBar.setPadding(0, (int) (12 * activity.getResources().getDisplayMetrics().density), 0, 0);

        Button btnClear = new Button(activity);
        btnClear.setText("🧹 Clear");
        btnClear.setTextSize(11);
        btnClear.setTextColor(palette.textMutedColor);
        btnClear.setBackgroundColor(Color.TRANSPARENT);

        Button btnCopy = new Button(activity);
        btnCopy.setText("📋 Copy Log");
        btnCopy.setTextSize(11);
        btnCopy.setTextColor(palette.brandPrimaryColor);
        btnCopy.setBackgroundColor(Color.TRANSPARENT);

        Button btnClose = new Button(activity);
        btnClose.setText("Close");
        btnClose.setTextSize(11);
        btnClose.setTextColor(palette.textHighColor);
        btnClose.setBackgroundColor(Color.TRANSPARENT);

        btnBar.addView(btnClear);
        btnBar.addView(btnCopy);
        btnBar.addView(btnClose);
        root.addView(btnBar);

        builder.setView(outerScroll);
        AlertDialog dialog = builder.create();

        btnClear.setOnClickListener(v -> {
            clearLogs();
            tvLogs.setText(getAllLogsText());
            Toast.makeText(activity, "Diagnostics log reset", Toast.LENGTH_SHORT).show();
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("DodoPress Diagnostics", getAllLogsText()));
                Toast.makeText(activity, "Telemetry copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
