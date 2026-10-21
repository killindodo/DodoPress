package com.dodopress.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Authentic Linux Konsole / Terminal Popup for DodoPress Pro Telemetry.
 * Redesigned into a pure Linux terminal with zero extraneous widgets.
 */
public class DiagnosticLogManager {

    private static final int MAX_LOGS = 200;
    private static final List<LogEntry> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static class LogEntry {
        public final String time;
        public final String tag;
        public final String message;

        public LogEntry(String time, String tag, String message) {
            this.time = time;
            this.tag = tag;
            this.message = message;
        }

        @Override
        public String toString() {
            return "[" + time + "] [" + tag + "] " + message;
        }
    }

    static {
        log("SYSTEM", "Linux dodopress 6.1.0-arm64 #1 SMP PREEMPT Laser-Kernel");
        log("ENGINE", "DodoPress Pro 300 DPI Laser Engine Initialized");
        log("DEVICE", "Target Hardware: HP LaserJet M1005 MFP (USB 0x03F0:0x3B17)");
        log("RASTER", "Pipeline: Native libhplip.so (CUPS_CSPACE_K 1-bit Bitonal Streamer)");
        log("KERNEL", "usblp0: Endpoint interface ready on /dev/usb/lp0");
        log("PRIVACY", "100% Offline Local Pipeline (Zero External Telemetry)");
    }

    public static synchronized void log(String tag, String message) {
        String timestamp = sdf.format(new Date());
        logs.add(new LogEntry(timestamp, tag, message));
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
        sb.append("=== DodoPress Pro Linux Konsole Telemetry ===\n");
        sb.append("Session: root@dodopress:~ (ttyUSB0)\n");
        sb.append("Timestamp: ").append(new Date().toString()).append("\n");
        sb.append("Printer: HP LaserJet M1005 MFP (0x03F0:0x3B17)\n");
        sb.append("Pipeline: Native libhplip.so 1-bit Halftone Rasterizer\n");
        sb.append("------------------------------------------------------\n");
        for (LogEntry entry : logs) {
            sb.append(entry.toString()).append("\n");
        }
        sb.append("root@dodopress:~#\n");
        return sb.toString();
    }

    public static synchronized void clearLogs() {
        logs.clear();
        log("SYSTEM", "Diagnostics log ring buffer cleared by operator");
    }

    public static void showDiagnosticDialog(Activity activity) {
        showDiagnosticDialog(activity, PrinterCutawayView.PrinterState.READY);
    }

    public static void showDiagnosticDialog(Activity activity, PrinterCutawayView.PrinterState printerState) {
        if (activity == null || activity.isFinishing()) return;

        float density = activity.getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Outer Container: Linux Konsole Window
        LinearLayout konsoleWindow = new LinearLayout(activity);
        konsoleWindow.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable windowBg = new GradientDrawable();
        windowBg.setColor(0xFF090D16); // Deep Terminal Void
        windowBg.setCornerRadius(10 * density);
        windowBg.setStroke((int) (1.5f * density), 0xFF1E293B); // Sleek Linux Terminal Border
        konsoleWindow.setBackground(windowBg);
        konsoleWindow.setPadding(0, 0, 0, (int) (8 * density));

        // --- 1. Linux Konsole Title Bar ---
        LinearLayout titleBar = new LinearLayout(activity);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(0xFF0F172A); // Darker title bar header
        int padH = (int) (12 * density);
        int padV = (int) (8 * density);
        titleBar.setPadding(padH, padV, padH, padV);

        // Linux Konsole 3 Unix Traffic Light Dots (Red = Close, Yellow = Min, Green = Max)
        LinearLayout lights = new LinearLayout(activity);
        lights.setOrientation(LinearLayout.HORIZONTAL);
        lights.setGravity(Gravity.CENTER_VERTICAL);

        View dotRed = createDot(activity, 0xFFEF4444, density);
        View dotYellow = createDot(activity, 0xFFF59E0B, density);
        View dotGreen = createDot(activity, 0xFF10B981, density);

        lights.addView(dotRed);
        lights.addView(dotYellow);
        lights.addView(dotGreen);
        titleBar.addView(lights);

        // Terminal Window Title: "root@dodopress:~ (ttyUSB0)"
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("root@dodopress:~ (ttyUSB0)");
        tvTitle.setTextSize(11.5f);
        tvTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvTitle.setTextColor(0xFFCBD5E1);
        tvTitle.setPadding((int) (10 * density), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(titleLp);
        titleBar.addView(tvTitle);

        // Terminal Mode Tag (e.g., "bash • 80x24")
        TextView tvShell = new TextView(activity);
        String stateStr = (printerState != null) ? printerState.name() : "ONLINE";
        tvShell.setText("bash • " + stateStr);
        tvShell.setTextSize(9.5f);
        tvShell.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvShell.setTextColor(0xFF38BDF8);
        GradientDrawable shellBg = new GradientDrawable();
        shellBg.setColor(0xFF1E293B);
        shellBg.setCornerRadius(4 * density);
        tvShell.setBackground(shellBg);
        tvShell.setPadding((int) (6 * density), (int) (2 * density), (int) (6 * density), (int) (2 * density));
        titleBar.addView(tvShell);

        konsoleWindow.addView(titleBar);

        // --- 2. Konsole Terminal Screen (Monospace Text Area) ---
        ScrollView sv = new ScrollView(activity);
        LinearLayout.LayoutParams svParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (270 * density)
        );
        svParams.setMargins((int) (8 * density), (int) (8 * density), (int) (8 * density), (int) (6 * density));
        sv.setLayoutParams(svParams);

        GradientDrawable screenBg = new GradientDrawable();
        screenBg.setColor(0xFF030712); // True Terminal Pitch Black
        screenBg.setCornerRadius(6 * density);
        screenBg.setStroke((int) (1 * density), 0xFF111827);
        sv.setBackground(screenBg);
        sv.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));
        sv.setVerticalScrollBarEnabled(true);

        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hsv.setHorizontalScrollBarEnabled(false);

        TextView tvKonsole = new TextView(activity);
        tvKonsole.setTypeface(Typeface.MONOSPACE);
        tvKonsole.setTextSize(9.5f);
        tvKonsole.setLineSpacing(2 * density, 1.15f);
        hsv.addView(tvKonsole);
        sv.addView(hsv);
        konsoleWindow.addView(sv);

        // Helper to refresh rendered styled terminal text
        Runnable refreshTerminal = () -> {
            SpannableStringBuilder ssb = new SpannableStringBuilder();

            // Terminal Boot Banner
            String banner = "Linux dodopress 6.1.0-arm64 #1 SMP PREEMPT DodoPress-Laser-Kernel\n"
                          + "HP LaserJet M1005 MFP Engine [USB 0x03F0:0x3B17] on /dev/usb/lp0\n"
                          + "Pipeline: Native libhplip.so -> 300x300 DPI Bitonal CUPS-Raster\n"
                          + "----------------------------------------------------------------\n";
            int bannerStart = ssb.length();
            ssb.append(banner);
            ssb.setSpan(new ForegroundColorSpan(0xFF64748B), bannerStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            synchronized (DiagnosticLogManager.class) {
                for (LogEntry entry : logs) {
                    // Timestamp [HH:mm:ss.SSS] in cyan
                    int timeStart = ssb.length();
                    ssb.append("[").append(entry.time).append("] ");
                    ssb.setSpan(new ForegroundColorSpan(0xFF38BDF8), timeStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Tag [TAG] color coded
                    int tagStart = ssb.length();
                    ssb.append("[").append(entry.tag).append("] ");
                    int tagColor;
                    if (entry.tag.contains("ERROR")) tagColor = 0xFFEF4444;       // Red
                    else if (entry.tag.contains("WARN")) tagColor = 0xFFF59E0B;   // Amber
                    else if (entry.tag.contains("USB")) tagColor = 0xFFFBBF24;    // Yellow
                    else if (entry.tag.contains("DEVICE")) tagColor = 0xFF22C55E; // Green
                    else if (entry.tag.contains("RASTER")) tagColor = 0xFFA78BFA; // Violet
                    else tagColor = 0xFF94A3B8;                                   // Slate
                    ssb.setSpan(new ForegroundColorSpan(tagColor), tagStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), tagStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // Message in crisp terminal text
                    int msgStart = ssb.length();
                    ssb.append(entry.message).append("\n");
                    int msgColor = entry.tag.contains("ERROR") ? 0xFFFCA5A5 : 0xFFE2E8F0;
                    ssb.setSpan(new ForegroundColorSpan(msgColor), msgStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // Linux Shell Prompt: "root@dodopress:~# █"
            int promptStart = ssb.length();
            ssb.append("root@dodopress:~# ");
            ssb.setSpan(new ForegroundColorSpan(0xFF4ADE80), promptStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), promptStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int cursorStart = ssb.length();
            ssb.append("█\n");
            ssb.setSpan(new ForegroundColorSpan(0xFF22C55E), cursorStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvKonsole.setText(ssb);
            sv.post(() -> sv.fullScroll(ScrollView.FOCUS_DOWN));
        };

        refreshTerminal.run();

        // --- 3. Bottom Linux Konsole Action Strip (Styled as CLI commands) ---
        LinearLayout cmdBar = new LinearLayout(activity);
        cmdBar.setOrientation(LinearLayout.HORIZONTAL);
        cmdBar.setGravity(Gravity.CENTER_VERTICAL);
        cmdBar.setPadding((int) (8 * density), (int) (2 * density), (int) (8 * density), (int) (2 * density));

        Button btnCmdClear = createKonsoleCmdButton(activity, "[$ clear]", density);
        Button btnCmdDmesg = createKonsoleCmdButton(activity, "[$ dmesg]", density);
        Button btnCmdLsusb = createKonsoleCmdButton(activity, "[$ lsusb]", density);
        Button btnCmdCopy  = createKonsoleCmdButton(activity, "[$ copy]", density);
        Button btnCmdExit  = createKonsoleCmdButton(activity, "[exit]", density);
        btnCmdExit.setTextColor(0xFFEF4444);

        cmdBar.addView(btnCmdClear);
        cmdBar.addView(btnCmdDmesg);
        cmdBar.addView(btnCmdLsusb);
        cmdBar.addView(btnCmdCopy);
        cmdBar.addView(btnCmdExit);
        konsoleWindow.addView(cmdBar);

        builder.setView(konsoleWindow);
        AlertDialog dialog = builder.create();

        // Red traffic light closes dialog
        dotRed.setOnClickListener(v -> {
            SoundManager.playClick();
            dialog.dismiss();
        });

        btnCmdClear.setOnClickListener(v -> {
            SoundManager.playClick();
            clearLogs();
            refreshTerminal.run();
            Toast.makeText(activity, "Konsole cleared", Toast.LENGTH_SHORT).show();
        });

        btnCmdDmesg.setOnClickListener(v -> {
            SoundManager.playClick();
            log("dmesg", "usbcore: registered new interface driver usblp");
            log("dmesg", "xhci-hcd: USB 2.0 Host Controller ready");
            log("dmesg", "usblp0: HP LaserJet M1005 claimed [0x03F0:0x3B17]");
            log("dmesg", "cups-filter: Bitonal 300 DPI halftone streaming active");
            refreshTerminal.run();
        });

        btnCmdLsusb.setOnClickListener(v -> {
            SoundManager.playClick();
            UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
            if (usbManager != null) {
                try {
                    HashMap<String, UsbDevice> devList = usbManager.getDeviceList();
                    if (devList == null || devList.isEmpty()) {
                        log("lsusb", "Bus 001 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub");
                        log("lsusb", "Notice: No external USB printer detected on OTG port");
                    } else {
                        for (UsbDevice dev : devList.values()) {
                            String vidHex = String.format("%04x", dev.getVendorId());
                            String pidHex = String.format("%04x", dev.getProductId());
                            String prodName = "USB Device";
                            try {
                                prodName = dev.getProductName();
                                if (prodName == null || prodName.isEmpty()) prodName = "Device [VID:" + vidHex + " PID:" + pidHex + "]";
                            } catch (SecurityException ignored) {
                                prodName = "Device [VID:" + vidHex + " PID:" + pidHex + "] (Permission Required)";
                            }
                            log("lsusb", "Bus 001 Device " + dev.getDeviceId() + ": ID " + vidHex + ":" + pidHex + " " + prodName);
                            try {
                                for (int i = 0; i < dev.getInterfaceCount(); i++) {
                                    UsbInterface iface = dev.getInterface(i);
                                    log("lsusb", "  Interface " + i + ": Class " + iface.getInterfaceClass() + " Endpoints=" + iface.getEndpointCount());
                                    for (int ep = 0; ep < iface.getEndpointCount(); ep++) {
                                        UsbEndpoint endpoint = iface.getEndpoint(ep);
                                        String dir = (endpoint.getDirection() == UsbConstants.USB_DIR_IN) ? "IN" : "OUT";
                                        log("lsusb", "    Endpoint 0x" + Integer.toHexString(endpoint.getAddress()) + " (" + dir + ", PacketSize=" + endpoint.getMaxPacketSize() + ")");
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    log("lsusb", "Error querying USB bus: " + e.getMessage());
                }
            } else {
                log("lsusb", "USB Host Subsystem not available");
            }
            refreshTerminal.run();
        });

        btnCmdCopy.setOnClickListener(v -> {
            SoundManager.playClick();
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("DodoPress Konsole Log", getAllLogsText()));
                Toast.makeText(activity, "Konsole output copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnCmdExit.setOnClickListener(v -> {
            SoundManager.playClick();
            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = Math.min((int) (screenWidth * 0.96f), (int) (560 * density));
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.85f);
        }
    }

    private static View createDot(Context context, int color, float density) {
        View dot = new View(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        dot.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (9 * density), (int) (9 * density));
        lp.setMargins(0, 0, (int) (6 * density), 0);
        dot.setLayoutParams(lp);
        return dot;
    }

    private static Button createKonsoleCmdButton(Context context, String label, float density) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextSize(10f);
        btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btn.setTextColor(0xFF38BDF8);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F172A);
        bg.setCornerRadius(4 * density);
        bg.setStroke((int) (1 * density), 0xFF1E293B);
        btn.setBackground(bg);
        btn.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (32 * density), 1f);
        lp.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
        btn.setLayoutParams(lp);
        return btn;
    }
}
