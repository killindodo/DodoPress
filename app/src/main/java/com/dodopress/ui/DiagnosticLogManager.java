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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiagnosticLogManager {

    private static final int MAX_LOGS = 120;
    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    static {
        log("SYSTEM", "DodoPress Pro Initialized. Native 300 DPI 1-bit Laser Engine ready.");
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
        sb.append("=== DodoPress Hardware Diagnostics & Event Log ===\n");
        sb.append("Printer: HP LaserJet M1005 MFP (USB 0x03F0:0x3B17)\n");
        sb.append("Driver: Native libhplip.so (CUPS_CSPACE_K 1-bit Bitonal Raster)\n");
        sb.append("Privacy: 100% Offline Local Architecture\n");
        sb.append("--------------------------------------------------\n");
        for (String line : logs) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clearLogs() {
        logs.clear();
        log("SYSTEM", "Diagnostics log cleared by user.");
    }

    public static void showDiagnosticDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A"));
        int pad = (int) (18 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // Header Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("⚡ Hardware Diagnostics & Telemetry");
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#38BDF8"));
        root.addView(tvTitle);

        // Subtitle / Hardware status
        TextView tvSub = new TextView(activity);
        tvSub.setText("HP LaserJet M1005 MFP • USB OTG Class 7 • 300 DPI 1-Bit Pipeline");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setPadding(0, 4, 0, 16);
        root.addView(tvSub);

        // Monospace Log Console Box
        ScrollView sv = new ScrollView(activity);
        LinearLayout.LayoutParams svParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (320 * activity.getResources().getDisplayMetrics().density)
        );
        sv.setLayoutParams(svParams);
        sv.setBackgroundColor(Color.parseColor("#020617"));
        sv.setPadding(12, 12, 12, 12);

        TextView tvLogs = new TextView(activity);
        tvLogs.setTypeface(Typeface.MONOSPACE);
        tvLogs.setTextSize(10);
        tvLogs.setTextColor(Color.parseColor("#A7F3D0"));
        tvLogs.setText(getAllLogsText());
        sv.addView(tvLogs);
        root.addView(sv);

        // Button Bar
        LinearLayout btnBar = new LinearLayout(activity);
        btnBar.setOrientation(LinearLayout.HORIZONTAL);
        btnBar.setGravity(Gravity.RIGHT);
        btnBar.setPadding(0, 16, 0, 0);

        Button btnClear = new Button(activity);
        btnClear.setText("🧹 Clear");
        btnClear.setTextColor(Color.parseColor("#94A3B8"));
        btnClear.setBackgroundColor(Color.TRANSPARENT);

        Button btnCopy = new Button(activity);
        btnCopy.setText("📋 Copy Report");
        btnCopy.setTextColor(Color.parseColor("#38BDF8"));
        btnCopy.setBackgroundColor(Color.TRANSPARENT);

        Button btnClose = new Button(activity);
        btnClose.setText("Close");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setBackgroundColor(Color.TRANSPARENT);

        btnBar.addView(btnClear);
        btnBar.addView(btnCopy);
        btnBar.addView(btnClose);
        root.addView(btnBar);

        builder.setView(root);
        AlertDialog dialog = builder.create();

        btnClear.setOnClickListener(v -> {
            clearLogs();
            tvLogs.setText(getAllLogsText());
            Toast.makeText(activity, "Logs cleared", Toast.LENGTH_SHORT).show();
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("DodoPress Diagnostics", getAllLogsText()));
                Toast.makeText(activity, "Copied telemetry to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
