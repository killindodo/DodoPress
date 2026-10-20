package com.dodopress.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.dodopress.engine.PrintEngine;
import com.dodopress.engine.ToneEngine;
import com.dodopress.usb.UsbPrinterConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_PDF = 2001;

    private UsbPrinterConnection usbPrinterConnection;
    private PrintEngine printEngine;
    private BroadcastReceiver usbHotplugReceiver;

    private TextView tvPrinterStatus;
    private Button btnPickFile;
    private TextView tvFileInfo;
    private LinearLayout layoutPreviewContainer;
    private ImageView ivPagePreview;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageIndicator;
    private SeekBar seekDarkness;
    private TextView tvDarknessLabel;
    private TextView tvDarknessPct;

    private Button btnPresetDraft;
    private Button btnPresetNormal;
    private Button btnPresetDeep;
    private Button btnPresetUltra;

    private RadioGroup rgDpi;
    private RadioButton rbDpi600;
    private RadioGroup rgPages;
    private RadioButton rbPagesAll;
    private RadioButton rbPagesCurrent;

    private Button btnCopiesMinus;
    private Button btnCopiesPlus;
    private TextView tvCopiesCount;

    private ProgressBar pbPrintProgress;
    private TextView tvPrintStatus;
    private Button btnPrintNow;

    private File currentPdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfPfd;
    private int totalPages = 0;
    private int currentPageIndex = 0;
    private Bitmap rawPageBitmap;
    private int copies = 1;

    // Background thread for real-time darkness rendering
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger pendingDarkness = new AtomicInteger(-1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.dodopress.R.layout.activity_main);

        usbPrinterConnection = new UsbPrinterConnection(this);
        printEngine = new PrintEngine(this, usbPrinterConnection);

        initViews();
        setupListeners();
        registerUsbHotplugReceiver();

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            loadPdfFromUri(intent.getData());
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                loadPdfFromUri(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterUsbHotplugReceiver();
        closePdf();
        usbPrinterConnection.close();
        previewExecutor.shutdown();
    }

    private void initViews() {
        tvPrinterStatus = findViewById(com.dodopress.R.id.tv_printer_status);
        btnPickFile = findViewById(com.dodopress.R.id.btn_pick_file);
        tvFileInfo = findViewById(com.dodopress.R.id.tv_file_info);
        layoutPreviewContainer = findViewById(com.dodopress.R.id.layout_preview_container);
        ivPagePreview = findViewById(com.dodopress.R.id.iv_page_preview);
        btnPrevPage = findViewById(com.dodopress.R.id.btn_prev_page);
        btnNextPage = findViewById(com.dodopress.R.id.btn_next_page);
        tvPageIndicator = findViewById(com.dodopress.R.id.tv_page_indicator);
        seekDarkness = findViewById(com.dodopress.R.id.seek_darkness);
        tvDarknessLabel = findViewById(com.dodopress.R.id.tv_darkness_label);
        tvDarknessPct = findViewById(com.dodopress.R.id.tv_darkness_pct);

        btnPresetDraft = findViewById(com.dodopress.R.id.btn_preset_draft);
        btnPresetNormal = findViewById(com.dodopress.R.id.btn_preset_normal);
        btnPresetDeep = findViewById(com.dodopress.R.id.btn_preset_deep);
        btnPresetUltra = findViewById(com.dodopress.R.id.btn_preset_ultra);

        rgDpi = findViewById(com.dodopress.R.id.rg_dpi);
        rbDpi600 = findViewById(com.dodopress.R.id.rb_dpi_600);
        rgPages = findViewById(com.dodopress.R.id.rg_pages);
        rbPagesAll = findViewById(com.dodopress.R.id.rb_pages_all);
        rbPagesCurrent = findViewById(com.dodopress.R.id.rb_pages_current);

        btnCopiesMinus = findViewById(com.dodopress.R.id.btn_copies_minus);
        btnCopiesPlus = findViewById(com.dodopress.R.id.btn_copies_plus);
        tvCopiesCount = findViewById(com.dodopress.R.id.tv_copies_count);

        pbPrintProgress = findViewById(com.dodopress.R.id.pb_print_progress);
        tvPrintStatus = findViewById(com.dodopress.R.id.tv_print_status);
        btnPrintNow = findViewById(com.dodopress.R.id.btn_print_now);
    }

    private void setupListeners() {
        btnPickFile.setOnClickListener(v -> pickPdf());

        btnPrevPage.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                renderPageThumb();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++;
                renderPageThumb();
            }
        });

        seekDarkness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDarknessDisplay(progress);
                scheduleDarknessPreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPresetDraft.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_DRAFT));
        btnPresetNormal.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_NORMAL));
        btnPresetDeep.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_DEEP));
        btnPresetUltra.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_ULTRA));

        btnCopiesMinus.setOnClickListener(v -> {
            if (copies > 1) {
                copies--;
                tvCopiesCount.setText(String.valueOf(copies));
            }
        });

        btnCopiesPlus.setOnClickListener(v -> {
            if (copies < 99) {
                copies++;
                tvCopiesCount.setText(String.valueOf(copies));
            }
        });

        btnPrintNow.setOnClickListener(v -> startPrinting());
    }

    private void setDarkness(int value) {
        seekDarkness.setProgress(value);
        updatePresetChipHighlight(value);
    }

    private void updatePresetChipHighlight(int progress) {
        btnPresetDraft.setBackgroundResource(progress == ToneEngine.PRESET_DRAFT ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPresetNormal.setBackgroundResource(progress == ToneEngine.PRESET_NORMAL ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPresetDeep.setBackgroundResource(progress == ToneEngine.PRESET_DEEP ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPresetUltra.setBackgroundResource(progress == ToneEngine.PRESET_ULTRA ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
    }

    private void scheduleDarknessPreview(int progress) {
        if (rawPageBitmap == null || rawPageBitmap.isRecycled()) return;
        pendingDarkness.set(progress);

        previewExecutor.execute(() -> {
            int target = pendingDarkness.getAndSet(-1);
            if (target != -1 && rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                Bitmap toneAdjusted = ToneEngine.processPreviewBitmap(rawPageBitmap, target);
                if (toneAdjusted != null) {
                    mainHandler.post(() -> ivPagePreview.setImageBitmap(toneAdjusted));
                }
            }
        });
    }

    private void registerUsbHotplugReceiver() {
        usbHotplugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                    UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    refreshPrinterStatus();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbHotplugReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(usbHotplugReceiver, filter);
        }
    }

    private void unregisterUsbHotplugReceiver() {
        if (usbHotplugReceiver != null) {
            try {
                unregisterReceiver(usbHotplugReceiver);
            } catch (Exception ignored) {}
            usbHotplugReceiver = null;
        }
    }

    private void refreshPrinterStatus() {
        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer != null) {
            String displayName = usbPrinterConnection.getPrinterDisplayName(printer);
            tvPrinterStatus.setText("● " + displayName);
            tvPrinterStatus.setBackgroundResource(com.dodopress.R.drawable.status_badge_connected);
            tvPrinterStatus.setTextColor(getResources().getColor(com.dodopress.R.color.status_connected_text));
        } else {
            tvPrinterStatus.setText("○ Connect USB OTG");
            tvPrinterStatus.setBackgroundResource(com.dodopress.R.drawable.status_badge_waiting);
            tvPrinterStatus.setTextColor(getResources().getColor(com.dodopress.R.color.status_waiting_text));
        }
    }

    private void updateDarknessDisplay(int progress) {
        tvDarknessPct.setText(progress + "%");
        String desc;
        if (progress <= 35) {
            desc = progress + "% — Light / Draft (Toner Saver)";
        } else if (progress <= 60) {
            desc = progress + "% — Normal (Standard)";
        } else if (progress <= 85) {
            desc = progress + "% — Deep Black (PC Match)";
        } else {
            desc = progress + "% — Ultra Deep Black";
        }
        tvDarknessLabel.setText(desc);
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PDF && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadPdfFromUri(uri);
            }
        }
    }

    private void loadPdfFromUri(Uri uri) {
        try {
            closePdf();

            currentPdfFile = new File(getCacheDir(), "active_print_job.pdf");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(currentPdfFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }

            pdfPfd = ParcelFileDescriptor.open(currentPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pdfPfd);
            totalPages = pdfRenderer.getPageCount();
            currentPageIndex = 0;

            long sizeKb = currentPdfFile.length() / 1024;
            tvFileInfo.setText(totalPages + " Page" + (totalPages > 1 ? "s" : "") + " (" + sizeKb + " KB)");
            layoutPreviewContainer.setVisibility(View.VISIBLE);

            renderPageThumb();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to read PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPageThumb() {
        if (pdfRenderer == null || totalPages == 0) return;

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(currentPageIndex);
            int pw = page.getWidth();
            int ph = page.getHeight();
            float scale = 400.0f / (float) pw;
            int previewW = 400;
            int previewH = (int) (ph * scale);

            if (rawPageBitmap != null) {
                rawPageBitmap.recycle();
            }
            rawPageBitmap = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888);
            rawPageBitmap.eraseColor(0xFFFFFFFF);

            page.render(rawPageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            tvPageIndicator.setText("Page " + (currentPageIndex + 1) + " of " + totalPages);
            btnPrevPage.setEnabled(currentPageIndex > 0);
            btnNextPage.setEnabled(currentPageIndex < totalPages - 1);

            scheduleDarknessPreview(seekDarkness.getProgress());

        } catch (Exception e) {
            Toast.makeText(this, "Render thumbnail error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startPrinting() {
        if (currentPdfFile == null || !currentPdfFile.exists()) {
            Toast.makeText(this, "Please select a PDF document first.", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer == null) {
            Toast.makeText(this, "No USB printer detected. Connect printer via USB OTG cable.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!usbPrinterConnection.hasPermission(printer)) {
            usbPrinterConnection.requestPermission(printer, this::executePrintJob, () -> {
                Toast.makeText(this, "USB permission was denied.", Toast.LENGTH_SHORT).show();
            });
        } else {
            executePrintJob();
        }
    }

    private void executePrintJob() {
        int darkness = seekDarkness.getProgress();
        int dpi = rbDpi600.isChecked() ? 600 : 300;

        int fromPage = 0;
        int toPage = totalPages - 1;
        if (rbPagesCurrent.isChecked()) {
            fromPage = currentPageIndex;
            toPage = currentPageIndex;
        }

        btnPrintNow.setEnabled(false);
        pbPrintProgress.setVisibility(View.VISIBLE);
        tvPrintStatus.setVisibility(View.VISIBLE);
        pbPrintProgress.setProgress(0);

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, fromPage, toPage, new PrintEngine.PrintProgressListener() {
            @Override
            public void onProgress(int currentPage, int total, String statusMessage) {
                runOnUiThread(() -> {
                    tvPrintStatus.setText(statusMessage);
                    if (total > 0) {
                        pbPrintProgress.setMax(total);
                        pbPrintProgress.setProgress(currentPage);
                    }
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Print job finished successfully!");
                    Toast.makeText(MainActivity.this, "Document printed!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Printing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void closePdf() {
        try {
            if (rawPageBitmap != null) {
                rawPageBitmap.recycle();
                rawPageBitmap = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (pdfPfd != null) {
                pdfPfd.close();
                pdfPfd = null;
            }
        } catch (Exception ignored) {}
    }
}
